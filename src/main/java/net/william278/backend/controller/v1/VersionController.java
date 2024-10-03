/*
 * MIT License
 *
 * Copyright (c) 2024 William278
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
import net.william278.backend.database.repository.*;
import net.william278.backend.exception.*;
import net.william278.backend.service.GitHubImportService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Tags(value = @Tag(name = "Project Versions"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class VersionController {

    private static final Logger log = LoggerFactory.getLogger(VersionController.class);

    private final AppConfiguration config;
    private final ProjectRepository projects;
    private final ChannelRepository channels;
    private final VersionRepository versions;
    private final GitHubImportService githubImporter;
    private final DistributionRepository distributions;
    private final DownloadRepository downloads;

    @Autowired
    public VersionController(AppConfiguration config, ProjectRepository projects, ChannelRepository channels,
                             VersionRepository versions, GitHubImportService gitHubImportService, DistributionRepository distributions, DownloadRepository downloads) {
        this.config = config;
        this.projects = projects;
        this.channels = channels;
        this.versions = versions;
        this.githubImporter = gitHubImportService;
        this.distributions = distributions;
        this.downloads = downloads;
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
            summary = "Get a project's versions on a specific channel that contain a specific distribution."
    )
    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects/{projectSlug:" + Project.PATTERN
                    + "}/channels/{channelName:" + Channel.PATTERN
                    + "}/distributions/{distributionName:" + Distribution.PATTERN + "}/versions",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    public Page<Version> getProjectDistributionsVersions(
            @Parameter(description = "The slug of the project to get versions for.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug,

            @Parameter(description = "The name of the release channel to get versions on.")
            @Pattern(regexp = Channel.PATTERN)
            @PathVariable String channelName,

            @Parameter(description = "The name of the distribution to get versions for.")
            @Pattern(regexp = Distribution.PATTERN)
            @PathVariable String distributionName,

            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size
    ) {
        final Project project = projects.findById(projectSlug).orElseThrow(ProjectNotFound::new);
        final Channel channel = channels.findChannelByName(channelName).orElseThrow(ChannelNotFound::new);
        final Distribution distribution = distributions.findDistributionByNameAndProjectOrderBySortingWeightDesc(distributionName, project)
                .orElseThrow(DistributionNotFound::new);

        return versions.getAllByProjectAndChannelAndDownloadsDistributionOrderByTimestampDesc(
                project, channel, distribution, PageRequest.of(page, size)
        );
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

    @Operation(
            summary = "Get the latest version of a project on a specific channel."
    )
    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects/{projectSlug}/channels/{channelName}/versions/latest",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    public Version getLatestProjectVersion(@PathVariable String projectSlug, @PathVariable String channelName) {
        final Project foundProject = projects.findById(projectSlug).orElseThrow(ProjectNotFound::new);
        final Channel foundChannel = channels.findChannelByName(channelName).orElseThrow(ChannelNotFound::new);
        return versions.getTopByProjectAndChannelOrderByTimestampDesc(foundProject, foundChannel).orElseThrow(VersionNotFound::new);
    }

    @Operation(
            summary = "Create a new version as a logged-in user.",
            security = {
                    @SecurityRequirement(name = "OAuth2")
            }
    )
    @ApiResponse(
            responseCode = "200",
            description = "The version was created successfully."
    )
    @ApiResponse(
            responseCode = "401",
            description = "Not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "Not authorized to create versions.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @PostMapping(
            value = "/v1/projects/{projectSlug:" + Project.PATTERN
                    + "}/channels/{channelName:" + Channel.PATTERN + "}/versions",
            produces = MediaType.APPLICATION_JSON_VALUE,
            headers = {
                    "content-type=multipart/mixed", "content-type=multipart/form-data",
                    "content-type=application/octet-stream", "content-type=application/json"
            }
    )
    public Version postVersionOAuth(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The slug of the project to create a version for.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug,

            @Parameter(description = "The name of the channel to release the version on.")
            @Pattern(regexp = Channel.PATTERN)
            @PathVariable String channelName,

            @RequestPart("version")
            @Schema(description = "The version to create.", implementation = Version.class)
            Version version,

            @RequestPart("files")
            @Schema(description = "The files to upload for the version.")
            MultipartFile[] files
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        return createNewVersion(projectSlug, channelName, version, files);
    }

    @Operation(
            summary = "Create a new version with an API key",
            security = {
                    @SecurityRequirement(name = "APIKey")
            }
    )
    @ApiResponse(
            responseCode = "200",
            description = "The version was created successfully."
    )
    @ApiResponse(
            responseCode = "401",
            description = "No API key provided.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "Invalid API key.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @PostMapping(
            value = "/v1/projects/{projectSlug:" + Project.PATTERN
                    + "}/channels/{channelName:" + Channel.PATTERN + "}/versions/api",
            produces = MediaType.APPLICATION_JSON_VALUE,
            headers = {
                    "content-type=multipart/mixed", "content-type=multipart/form-data",
                    "content-type=application/octet-stream", "content-type=application/json"
            }
    )
    @CrossOrigin(
            origins = "*", allowCredentials = "false",
            allowedHeaders = {"X-Api-Key", "Content-Type", "Accept"}
    )
    public Version postVersionApiKey(
            @RequestHeader("X-Api-Key") String apiKey,

            @Parameter(description = "The slug of the project to create a version for.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug,

            @Parameter(description = "The name of the channel to release the version on.")
            @Pattern(regexp = Channel.PATTERN)
            @PathVariable String channelName,

            @RequestPart("version")
            @Schema(description = "The version to create.", implementation = Version.class)
            Version version,

            @RequestPart("files")
            @Schema(description = "The files to upload for the version.")
            MultipartFile[] files
    ) {
        if (config.getApiSecret() == null) {
            throw new IllegalStateException("API key is not set on server.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new NotAuthenticated();
        }
        if (!MessageDigest.isEqual(Utf8.encode(config.getApiSecret()), Utf8.encode(apiKey))) {
            throw new NoPermission();
        }
        return createNewVersion(projectSlug, channelName, version, files);
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

    @NotNull
    private Version createNewVersion(@NotNull String projectSlug, @NotNull String channelName,
                                     @NotNull Version version, @NotNull MultipartFile[] files) {
        final Project project = projects.findById(projectSlug).orElseThrow(ProjectNotFound::new);

        // Create the channel, add it to the project if it doesn't exist
        final Channel channel = channels.findChannelByName(channelName).orElse(channels.save(new Channel(channelName)));
        if (project.addReleaseChannel(channel)) {
            projects.save(project);
        }

        // Set version parameters
        version.setProject(project);
        version.setChannel(channel);
        if (version.getTimestamp() == null) {
            version.setTimestamp(Instant.now());
        }

        // Set dist
        version.getDownloads().forEach(d -> d.setDistribution(
                distributions.findDistributionByNameAndProjectOrderBySortingWeightDesc(d.getDistribution().getName(), project).orElseGet(() -> {
                    d.getDistribution().setProject(project);
                    return distributions.save(d.getDistribution());
                })
        ));

        // Save and correctly relocate the uploaded files
        try {
            for (int i = 0; i < files.length; i++) {
                final MultipartFile file = files[i];
                final Download download = version.getDownloads().get(i);
                final Path dest = version.getDownloadPathFor(download.getDistribution(), config);

                // Check size
                long size = file.getSize();
                if (size <= 0) {
                    throw new IllegalArgumentException("File size was 0");
                }
                download.setFileSize(size);

                // Move file
                try (InputStream copy = file.getInputStream()) {
                    FileUtils.copyInputStreamToFile(copy, dest.toFile());
                }
                try (InputStream copy = file.getInputStream()) {
                    download.setMd5(DigestUtils.md5Digest(copy));
                }
                downloads.save(download);
            }
        } catch (Throwable e) {
            log.warn("Failed to upload files for version {} of project {}", version.getName(), projectSlug, e);
            throw new UploadFailed();
        }

        return versions.save(version);
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
