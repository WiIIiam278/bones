package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.*;
import net.william278.backend.database.repository.ChannelRepository;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.database.repository.VersionRepository;
import net.william278.backend.exception.*;
import net.william278.backend.service.GitHubImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.william278.backend.controller.RootController.CORS_FRONTEND_ORIGIN;

@RestController
@Tags(value = @Tag(name = "Project Versions"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class VersionController {

    public static final String API_KEY_HEADER = "X-Api-Key";

    private final AppConfiguration config;
    private final ProjectRepository projects;
    private final ChannelRepository channels;
    private final VersionRepository versions;
    private final GitHubImportService githubImporter;

    @Autowired
    public VersionController(AppConfiguration config, ProjectRepository projects, ChannelRepository channels,
                             VersionRepository versions, GitHubImportService gitHubImportService) {
        this.config = config;
        this.projects = projects;
        this.channels = channels;
        this.versions = versions;
        this.githubImporter = gitHubImportService;
    }

    @Operation(
            summary = "Get a project's versions on a specific channel."
    )
    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects/{projectSlug:" + Project.PATTERN
                    + "}/channels/{channelName:" + Channel.PATTERN + "}/versions",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    public Page<Version> getProjectVersions(
            @Parameter(description = "The slug of the project to get versions for.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug,

            @Parameter(description = "The name of the release channel to get versions on.")
            @Pattern(regexp = Channel.PATTERN)
            @PathVariable String channelName,

            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size
    ) {
        final Project project = projects.findById(projectSlug).orElseThrow(ProjectNotFound::new);
        final Channel channel = channels.findChannelByName(channelName).orElseThrow(ChannelNotFound::new);
        return versions.getAllByProjectAndChannelOrderByTimestampDesc(project, channel, PageRequest.of(page, size));
    }

    @Operation(
            summary = "Get a specific version of a project."
    )
    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects/{projectSlug:" + Project.PATTERN
                    + "}/channels/{channelName:" + Channel.PATTERN
                    + "}/versions/{versionName:" + Version.PATTERN + "}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    public Version getProjectChannelVersion(
            @Parameter(description = "The slug of the project the version is for.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug,

            @Parameter(description = "The name of the channel the version was released on.")
            @Pattern(regexp = Channel.PATTERN)
            @PathVariable String channelName,

            @Parameter(description = "The name of the version to get.")
            @Pattern(regexp = Version.PATTERN)
            @PathVariable String versionName
    ) {
        final Project project = projects.findById(projectSlug).orElseThrow(ProjectNotFound::new);
        final Channel channel = channels.findChannelByName(channelName).orElseThrow(ChannelNotFound::new);
        return versions.findByProjectAndChannelAndName(project, channel, versionName).orElseThrow(VersionNotFound::new);
    }

    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects/{projectSlug}/channels/{channelName}/versions/latest",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    @Operation(summary = "Get the latest version of a project on a specific channel.")
    public Version getLatestProjectVersion(@PathVariable String projectSlug, @PathVariable String channelName) {
        final Project foundProject = projects.findById(projectSlug).orElseThrow(ProjectNotFound::new);
        final Channel foundChannel = channels.findChannelByName(channelName).orElseThrow(ChannelNotFound::new);
        return versions.getTopByProjectAndChannelOrderByTimestampDesc(foundProject, foundChannel).orElseThrow(VersionNotFound::new);
    }

    @Operation(
            summary = "Create a new version.",
            security = @SecurityRequirement(name = "Secret Key")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The version was created successfully."
    )
    @ApiResponse(
            responseCode = "403",
            description = "Not authorized to create versions.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @PostMapping(
            value = "/v1/projects/{projectSlug:" + Project.PATTERN
                    + "}/channels/{channelName:" + Channel.PATTERN + "}/versions",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    public Version postVersion(
            @RequestHeader(API_KEY_HEADER) String secretKey,

            @Parameter(description = "The slug of the project to create a version for.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug,

            @Parameter(description = "The name of the channel to release the version on.")
            @Pattern(regexp = Channel.PATTERN)
            @PathVariable String channelName,

            @RequestBody Version version,
            @RequestParam("files") MultipartFile[] files
    ) {
        if (!config.getApiSecret().equals(secretKey)) {
            throw new NotAuthenticated();
        }
        final Project project = projects.findById(projectSlug).orElseThrow(ProjectNotFound::new);

        // Create the channel, add it to the project if it doesn't exist
        final Channel channel = channels.findChannelByName(channelName)
                .orElse(channels.save(new Channel(version.getChannel().getName())));
        if (project.addReleaseChannel(channel)) {
            projects.save(project);
        }

        // Set version parameters
        version.setProject(project);
        version.getDistributions().forEach(dist -> dist.setProject(project));
        version.setChannel(channel);

        // Save and correctly relocate the uploaded files
        try {
            for (MultipartFile file : files) {
                final String name = Objects.requireNonNull(file.getOriginalFilename(), "File name was null.");
                final Download download = version.getDownloadByFileName(name);
                final Path dest = version.getDownloadPathFor(download.getDistribution(), config).resolve(name);

                //noinspection ResultOfMethodCallIgnored
                dest.toFile().getParentFile().mkdirs();

                // Move file
                file.transferTo(dest);
                download.setMd5(DigestUtils.md5Digest(file.getInputStream()));
                download.setFileSize(file.getSize());
            }
        } catch (IOException e) {
            throw new UploadFailed();
        }

        return versions.save(version);
    }

    @Operation(
            summary = "Import versions from GitHub to a channel.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "Import started."
    )
    @ApiResponse(
            responseCode = "401",
            description = "Not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "Not authorized to import versions.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @PostMapping(
            value = "/v1/projects/{projectSlug:" + Project.PATTERN
                    + "}/channels/{channelName:" + Channel.PATTERN + "}/versions/import/github",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin(
            allowCredentials = "true", originPatterns = {CORS_FRONTEND_ORIGIN},
            methods = {RequestMethod.POST}
    )
    public VersionImportRequest importGitHubVersions(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The slug of the project to import versions for.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug,

            @Parameter(description = "The name of the channel to import versions to.")
            @Pattern(regexp = Channel.PATTERN)
            @PathVariable String channelName,

            @RequestBody VersionImportRequest request
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        final Project project = projects.findById(projectSlug).orElseThrow(ProjectNotFound::new);
        final Channel channel = channels.findChannelByName(channelName).orElse(new Channel(channelName));

        githubImporter.importGithub(project, request.source(), channel, request.distributionMatchersMap());
        return request;
    }

    @Schema(description = "Request to import versions from GitHub.")
    public record VersionImportRequest(
            @Schema(description = "The version source (import from releases or commits).")
            @NotNull GitHubImportService.VersionSource source,

            @Schema(description = "Matchers to resolve distributions from asset file names.")
            @NotNull List<DistributionMatcher> distributionMatchers
    ) {

        @Schema(description = "Matcher to resolve a distribution from an asset file name.")
        public record DistributionMatcher(
                @Schema(description = "Regex filter to match distributions from a file name.")
                @NotNull String match,
                @Schema(description = "The distribution")
                @NotNull Distribution distribution
        ) {
            public Map.Entry<String, Distribution> distributionMatchers() {
                return Map.entry(match, distribution);
            }
        }

        @NotNull
        public Map<String, Distribution> distributionMatchersMap() {
            return distributionMatchers.stream().map(DistributionMatcher::distributionMatchers)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

    }

}
