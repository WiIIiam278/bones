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
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.*;
import net.william278.backend.database.repository.ChannelRepository;
import net.william278.backend.database.repository.DistributionRepository;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.database.repository.VersionRepository;
import net.william278.backend.exception.*;
import net.william278.backend.util.HTTPUtils;
import net.william278.backend.util.MediaTypeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@Tags(value = @Tag(name = "Downloads"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class DownloadController {

    private static final CacheControl CACHE = CacheControl.empty().cachePublic().sMaxAge(Duration.ofDays(7));
    private final AppConfiguration configuration;
    private final ProjectRepository projects;
    private final VersionRepository versions;
    private final ChannelRepository channels;
    private final DistributionRepository distributions;

    @Autowired
    public DownloadController(AppConfiguration configuration, ProjectRepository projects, VersionRepository versions,
                              ChannelRepository channels, DistributionRepository distributions) {
        this.configuration = configuration;
        this.projects = projects;
        this.versions = versions;
        this.channels = channels;
        this.distributions = distributions;
    }

    @Operation(
            summary = "Download a project's version."
    )
    @ApiResponse(
            responseCode = "200",
            description = "The requested version download.",
            headers = {
                    @Header(
                            name = "Content-Disposition",
                            description = "A header indicating that the content is expected to be displayed as an attachment, that is downloaded and saved locally.",
                            schema = @Schema(type = "string")
                    ),
                    @Header(
                            name = "ETag",
                            description = "An identifier for a specific version of a resource. It lets caches be more efficient and save bandwidth, as a web server does not need to resend a full response if the content has not changed.",
                            schema = @Schema(type = "string")
                    ),
                    @Header(
                            name = "Last-Modified",
                            description = "The date and time at which the origin server believes the resource was last modified.",
                            schema = @Schema(type = "string")
                    )
            }
    )
    @ApiResponse(
            responseCode = "403",
            description = "The version is restricted and the user is not authenticated."
    )
    @ApiResponse(
            responseCode = "404",
            description = "The project, channel, version, and/or distribution was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping(
            value = "/v1/projects/{projectSlug:" + Project.PATTERN
                    + "}/channels/{channelName:" + Channel.PATTERN
                    + "}/versions/{versionName:" + Version.PATTERN
                    + "}/distributions/{distributionName:" + Distribution.PATTERN + "}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.ALL_VALUE
            }
    )
    @CrossOrigin
    public ResponseEntity<?> download(
            @AuthenticationPrincipal User principal,

            @Parameter(name = "project", description = "The project identifier.", example = "HuskHomes")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug,

            @Parameter(description = "The release channel to target.")
            @Pattern(regexp = Channel.PATTERN)
            @PathVariable String channelName,

            @Parameter(description = "The distribution to target.")
            @Pattern(regexp = Distribution.PATTERN)
            @PathVariable String distributionName,

            @Parameter(description = "The name of the version to download.")
            @Pattern(regexp = Version.PATTERN)
            @PathVariable String versionName
    ) {
        final Project project = projects.findById(projectSlug).orElseThrow(ProjectNotFound::new);
        final Channel channel = channels.findChannelByName(channelName).orElseThrow(ChannelNotFound::new);
        final Version version = versions.findByProjectAndChannelAndName(project, channel, versionName)
                .orElseThrow(VersionNotFound::new);
        final Distribution distribution = distributions.findDistributionByNameAndProjectOrderBySortingWeightDesc(distributionName, project)
                .orElseThrow(DistributionNotFound::new);

        // Check the version has this distribution
        if (!version.hasDistribution(distribution)) {
            throw new DistributionNotFound();
        }

        // Restrict download if the version is restricted and the user is not authenticated
        if (version.isRestricted()) {
            if (principal == null) {
                throw new NotAuthenticated();
            }
            if (!version.canDownload(principal)) {
                throw new NoPermission();
            }
        }

        // Increment the download count for the version
        CompletableFuture.runAsync(() -> incrementVersionDownloads(version));

        try {
            return new DownloadArchive(version.getDownloadPathFor(distribution, this.configuration), CACHE);
        } catch (Throwable e) {
            log.warn("Failed to serve download version archive", e);
            throw new DownloadFailed();
        }
    }

    private void incrementVersionDownloads(@NotNull Version version) {
        version.incrementDownloadCount();
        versions.save(version);
    }

    private static class DownloadArchive extends ResponseEntity<FileSystemResource> {

        private DownloadArchive(final Path path, final CacheControl cache) throws IOException {
            super(new FileSystemResource(path), headersFor(path, cache), HttpStatus.OK);
        }

        @NotNull
        private static HttpHeaders headersFor(final Path path, final CacheControl cache) throws IOException {
            final HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl(cache);
            headers.setContentDisposition(HTTPUtils.attachmentDisposition(path));
            headers.setContentType(MediaTypeUtils.fromFileName(path.getFileName().toString()));
            headers.setLastModified(Files.getLastModifiedTime(path).toInstant());
            return headers;
        }
    }

}
