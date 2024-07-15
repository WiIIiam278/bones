package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.validation.constraints.Pattern;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.Channel;
import net.william278.backend.database.model.Download;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.Version;
import net.william278.backend.database.repository.ChannelRepository;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.database.repository.VersionRepository;
import net.william278.backend.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

@RestController
@Tags(value = @Tag(name = "Project Versions"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class VersionController {

    public static final String API_KEY_HEADER = "X-Api-Key";

    private final AppConfiguration config;
    private final ProjectRepository projects;
    private final ChannelRepository channels;
    private final VersionRepository versions;

    @Autowired
    public VersionController(AppConfiguration config, ProjectRepository projects, ChannelRepository channels,
                             VersionRepository versions) {
        this.config = config;
        this.projects = projects;
        this.channels = channels;
        this.versions = versions;
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
        return versions.getAllByProjectAndChannelOrderByTimestamp(project, channel, PageRequest.of(page, size));
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
        return versions.getTopByProjectAndChannelOrderByTimestamp(foundProject, foundChannel).orElseThrow(VersionNotFound::new);
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
            value = "/v1/projects/{projectSlug}/channels/{channelName}/versions",
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

            @RequestBody Version versionName,
            @RequestParam("files") MultipartFile[] files
    ) {
        if (!config.getApiSecret().equals(secretKey)) {
            throw new NotAuthenticated();
        }
        final Project project = projects.findById(projectSlug).orElseThrow(ProjectNotFound::new);

        // Create the channel, add it to the project if it doesn't exist
        final Channel channel = channels.findChannelByName(channelName)
                .orElse(channels.save(new Channel(versionName.getChannel().getName())));
        if (project.addReleaseChannel(channel)) {
            projects.save(project);
        }

        // Set version parameters
        versionName.setProject(project);
        versionName.getDistributions().forEach(dist -> dist.setProject(project));
        versionName.setChannel(channel);

        // Save and correctly relocate the uploaded files
        try {
            for (MultipartFile file : files) {
                final String name = Objects.requireNonNull(file.getOriginalFilename(), "File name was null.");
                final Download download = versionName.getDownloadByFileName(name);
                final Path dest = versionName.getDownloadPathFor(download.getDistribution(), config).resolve(name);

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

        return versions.save(versionName);
    }


}
