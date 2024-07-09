package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Pattern;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.Channel;
import net.william278.backend.database.model.Distribution;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.Version;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Schema(name = "Downloads")
@RestController
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

    @ApiResponse(
            responseCode = "200",
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
    @GetMapping(
            value = "/v1/projects/{project:" + Project.PATTERN
                    + "}/channels/{channel:" + Channel.PATTERN
                    + "}/distributions/{distribution:" + Distribution.PATTERN
                    + "}/versions/{version:" + Version.PATTERN + "}/download",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.ALL_VALUE
            }
    )
    @Operation(summary = "Downloads a specific project version on a certain channel.")
    public ResponseEntity<?> download(
            @Parameter(name = "project", description = "The project identifier.", example = "HuskHomes")
            @PathVariable("project")
            @Pattern(regexp = Project.PATTERN) //
            final String projectName,
            @Parameter(description = "The release channel to target.")
            @PathVariable("channel")
            @Pattern(regexp = Version.PATTERN) //
            final String channelName,
            @Parameter(description = "The distribution to target.")
            @PathVariable("distribution")
            @Pattern(regexp = Distribution.PATTERN) //
            final String distributionName,
            @Parameter(description = "The name of the version to download.")
            @PathVariable("version")
            @Pattern(regexp = Version.PATTERN) //
            final String versionName
    ) {
        final Project project = this.projects.findById(projectName).orElseThrow(ProjectNotFound::new);
        final Channel channel = this.channels.findChannelByName(channelName).orElseThrow(ChannelNotFound::new);
        final Distribution distribution = this.distributions.findDistributionByName(distributionName)
                .orElseThrow(DistributionNotFound::new);
        final Version version = this.versions.findByProjectAndChannelAndDistributionAndName(project, channel,
                distribution, versionName).orElseThrow(VersionNotFound::new);

        try {
            return new JavaArchive(
                    this.configuration.getStoragePath()
                            .resolve(project.getName())
                            .resolve(channel.getName())
                            .resolve(version.getDistribution().getName())
                            .resolve(version.getFileName()),
                    CACHE
            );
        } catch (Throwable e) {
            throw new DownloadFailed();
        }
    }

    private static class JavaArchive extends ResponseEntity<FileSystemResource> {
        JavaArchive(final Path path, final CacheControl cache) throws IOException {
            super(new FileSystemResource(path), headersFor(path, cache), HttpStatus.OK);
        }

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
