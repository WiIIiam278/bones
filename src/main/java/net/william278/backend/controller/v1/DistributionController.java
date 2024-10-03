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
    @CrossOrigin(value = "*", allowCredentials = "false")
    public List<Distribution> getProjectDistributions(
            @Parameter(description = "The slug of the project to get distributions for.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug
    ) {
        final Project project = projects.findById(projectSlug).orElseThrow(ProjectNotFound::new);
        return distributions.findDistributionsByProjectOrderBySortingWeightDesc(project);
    }

}
