package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.exception.ProjectNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Schema(name = "Projects")
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ProjectController {

    private final AppConfiguration configuration;
    private final ProjectRepository projects;

    @Autowired
    public ProjectController(AppConfiguration configuration, ProjectRepository projects) {
        this.configuration = configuration;
        this.projects = projects;
    }

    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects",
            produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    @CrossOrigin
    public Iterable<Project> getProjects() {
        return projects.findAll();
    }

    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects/{projectId}",
            produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    @CrossOrigin
    public Project getProject(@PathVariable String projectId) {
        return projects.findById(projectId).orElseThrow(ProjectNotFound::new);
    }

}
