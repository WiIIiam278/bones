package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.Channel;
import net.william278.backend.database.model.Distribution;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.Version;
import net.william278.backend.database.repository.ChannelRepository;
import net.william278.backend.database.repository.DistributionRepository;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.database.repository.VersionRepository;
import net.william278.backend.exception.ProjectNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Schema(name = "Channels")
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ChannelsController {

    private final AppConfiguration configuration;
    private final ProjectRepository projects;
    private final ChannelRepository channels;
    private final VersionRepository versions;

    @Autowired
    public ChannelsController(AppConfiguration configuration, ProjectRepository projects,
                              ChannelRepository channels, VersionRepository versions) {
        this.configuration = configuration;
        this.projects = projects;
        this.channels = channels;
        this.versions = versions;
    }

    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects/{project}/channels",
            produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    @CrossOrigin
    public Iterable<Channel> getChannelsForProject(@PathVariable String project) {
        final Project found = projects.findById(project).orElseThrow(ProjectNotFound::new);
        return versions.getAllByProject(found).stream().map(Version::getChannel).toList();
    }

}
