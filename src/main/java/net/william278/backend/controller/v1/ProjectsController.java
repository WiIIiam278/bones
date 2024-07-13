package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.User;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.exception.InvalidProject;
import net.william278.backend.exception.NoPermission;
import net.william278.backend.exception.NotAuthenticated;
import net.william278.backend.exception.ProjectNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static net.william278.backend.controller.RootController.CORS_FRONTEND_ORIGIN;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ProjectsController {

    private final ProjectRepository projects;

    @Autowired
    public ProjectsController(ProjectRepository projects) {
        this.projects = projects;
    }

    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    @Operation(summary = "Get the list of all projects.")
    public List<Project> getProjects() {
        return projects.findAll();
    }

    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects/{projectId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    @Operation(summary = "Get a specific project.")
    public Project getProject(@PathVariable String projectId) {
        return projects.findById(projectId).orElseThrow(ProjectNotFound::new);
    }

    @Operation(summary = "Create or update a project.", security = {
            @SecurityRequirement(name = "OAuth2")
    })
    @ApiResponse(
            responseCode = "200"
    )
    @PutMapping(
            value = "/v1/projects/{projectId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin(
            allowCredentials = "true", originPatterns = {CORS_FRONTEND_ORIGIN},
            methods = {RequestMethod.PUT}
    )
    public Project putProject(@AuthenticationPrincipal User principal, @PathVariable String projectId,
                              @RequestBody Project project) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        if (projectId.isBlank() || !projectId.matches(Project.PATTERN)) {
            throw new InvalidProject();
        }
        project.setSlug(projectId);
        return projects.save(project);
    }

    @Operation(summary = "Delete a project.", security = {
            @SecurityRequirement(name = "OAuth2")
    })
    @ApiResponse(
            responseCode = "200"
    )
    @DeleteMapping(
            value = "/v1/projects/{projectId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin(
            allowCredentials = "true", originPatterns = {CORS_FRONTEND_ORIGIN},
            methods = {RequestMethod.DELETE}
    )
    public Project deleteProject(@AuthenticationPrincipal User principal, @PathVariable String projectId) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        final Project project = projects.findById(projectId).orElseThrow(ProjectNotFound::new);
        projects.delete(project);
        return project;
    }

}
