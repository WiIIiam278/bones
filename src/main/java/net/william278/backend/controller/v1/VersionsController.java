package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.*;
import net.william278.backend.database.repository.ChannelRepository;
import net.william278.backend.database.repository.DistributionRepository;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.database.repository.VersionRepository;
import net.william278.backend.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class VersionsController {

    public static final String API_KEY_HEADER = "X-Api-Key";

    private final AppConfiguration config;
    private final ProjectRepository projects;
    private final ChannelRepository channels;
    private final VersionRepository versions;
    private final DistributionRepository distributions;

    @Autowired
    public VersionsController(AppConfiguration config, ProjectRepository projects, ChannelRepository channels,
                              VersionRepository versions, DistributionRepository distributions) {
        this.config = config;
        this.projects = projects;
        this.channels = channels;
        this.versions = versions;
        this.distributions = distributions;
    }

    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects/{project}/channels/{channel}/versions",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    @Operation(summary = "Get a project's versions on a specific channel.")
    public List<Version> getProjectChannelVersions(@PathVariable String project, @PathVariable String channel) {
        final Project foundProject = projects.findById(project).orElseThrow(ProjectNotFound::new);
        final Channel foundChannel = channels.findChannelByName(channel).orElseThrow(ChannelNotFound::new);
        return versions.getAllByProjectAndChannel(foundProject, foundChannel);
    }

    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects/{project}/channels/{channel}/versions/{version}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    @Operation(summary = "Get a specific version of a project.")
    public Version getProjectChannelVersion(@PathVariable String project, @PathVariable String channel,
                                            @PathVariable String version) {
        final Project foundProject = projects.findById(project).orElseThrow(ProjectNotFound::new);
        final Channel foundChannel = channels.findChannelByName(channel).orElseThrow(ChannelNotFound::new);
        return versions.findByProjectAndChannelAndName(foundProject, foundChannel, version).orElseThrow();
    }

    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects/{project}/channels/{channel}/distributions/{distribution}/versions",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    @Operation(summary = "Get a list of versions of a project on a specific channel and distribution.")
    public List<Version> getProjectChannelDistributionVersions(@PathVariable String project,
                                                               @PathVariable String channel,
                                                               @PathVariable String distribution) {
        final Project foundProject = projects.findById(project).orElseThrow(ProjectNotFound::new);
        final Channel foundChannel = channels.findChannelByName(channel).orElseThrow(ChannelNotFound::new);
        final Distribution foundDistribution = distributions.findDistributionByName(distribution).orElseThrow(DistributionNotFound::new);
        return versions.getAllByProjectAndChannelAndDistributionsIsContaining(foundProject, foundChannel, foundDistribution);
    }

    @Operation(
            summary = "Create a new version.",
            security = {
                    @SecurityRequirement(name = "Secret Key")
            }
    )
    @ApiResponse(
            responseCode = "200"
    )
    @PostMapping(
            value = "/v1/projects/{project}/channels/{channel}/versions",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    public void createVersion(
            @RequestHeader(API_KEY_HEADER) String secretKey,
            @PathVariable String project,
            @PathVariable String channel,
            @RequestBody Version version,
            @RequestParam("files") MultipartFile[] files
    ) {
        if (!config.getApiSecret().equals(secretKey)) {
            throw new NotAuthenticated();
        }
        final Project foundProject = projects.findById(project).orElseThrow(ProjectNotFound::new);
        final Channel foundChannel = channels.findChannelByName(channel).orElse(
                channels.save(new Channel(version.getChannel().getName()))
        );

        // Set version parameters
        version.setProject(foundProject);
        version.getDistributions().forEach(dist -> dist.setProject(foundProject));
        version.setChannel(foundChannel);

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

        versions.save(version);
    }

    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects/{project}/channels/{channel}/versions/latest",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    @Operation(summary = "Get the latest version of a project on a specific channel.")
    public Version getLatestProjectVersionOnChannel(@PathVariable String project, @PathVariable String channel) {
        final Project foundProject = projects.findById(project).orElseThrow(ProjectNotFound::new);
        final Channel foundChannel = channels.findChannelByName(channel).orElseThrow(ChannelNotFound::new);
        return versions.getTopByProjectAndChannelOrderByTimestamp(foundProject, foundChannel).orElseThrow(VersionNotFound::new);
    }


}
