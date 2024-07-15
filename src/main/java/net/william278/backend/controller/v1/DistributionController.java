package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.validation.constraints.Pattern;
import net.william278.backend.database.model.Distribution;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.repository.DistributionRepository;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.exception.ErrorResponse;
import net.william278.backend.exception.ProjectNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tags(value = @Tag(name = "Version Distributions"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class DistributionController {

    private final ProjectRepository projects;
    private final DistributionRepository distributions;

    @Autowired
    public DistributionController(ProjectRepository projects, DistributionRepository distributions) {
        this.projects = projects;
        this.distributions = distributions;
    }

    @Operation(
            summary = "Get the distributions a project has published versions for."
    )
    @ApiResponse(
            responseCode = "200",
            description = "The distributions for the project."
    )
    @ApiResponse(
            responseCode = "404",
            description = "The project was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping(
            value = "/v1/projects/{projectSlug:" + Project.PATTERN + "}/distributions",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    public List<Distribution> getProjectDistributions(
            @Parameter(description = "The slug of the project to get distributions for.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug
    ) {
        final Project project = projects.findById(projectSlug).orElseThrow(ProjectNotFound::new);
        return distributions.findDistributionsByProject(project);
    }

}
