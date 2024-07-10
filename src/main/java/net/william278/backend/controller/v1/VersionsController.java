package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import net.william278.backend.database.model.Channel;
import net.william278.backend.database.model.Distribution;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.Version;
import net.william278.backend.database.repository.ChannelRepository;
import net.william278.backend.database.repository.DistributionRepository;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.database.repository.VersionRepository;
import net.william278.backend.exception.ChannelNotFound;
import net.william278.backend.exception.DistributionNotFound;
import net.william278.backend.exception.ProjectNotFound;
import net.william278.backend.exception.VersionNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class VersionsController {

    private final ProjectRepository projects;
    private final ChannelRepository channels;
    private final VersionRepository versions;
    private final DistributionRepository distributions;

    @Autowired
    public VersionsController(ProjectRepository projects, ChannelRepository channels, VersionRepository versions, DistributionRepository distributions) {
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
    public Iterable<Version> getProjectChannelVersions(@PathVariable String project, @PathVariable String channel) {
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
    public Iterable<Version> getProjectChannelDistributionVersions(@PathVariable String project,
                                                                   @PathVariable String channel,
                                                                   @PathVariable String distribution) {
        final Project foundProject = projects.findById(project).orElseThrow(ProjectNotFound::new);
        final Channel foundChannel = channels.findChannelByName(channel).orElseThrow(ChannelNotFound::new);
        final Distribution foundDistribution = distributions.findDistributionByName(distribution).orElseThrow(DistributionNotFound::new);
        return versions.getAllByProjectAndChannelAndDistributionsIsContaining(foundProject, foundChannel, foundDistribution);
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
