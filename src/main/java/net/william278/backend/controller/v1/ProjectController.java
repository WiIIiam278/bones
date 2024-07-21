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
import jakarta.validation.constraints.Pattern;
import net.william278.backend.database.model.Channel;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.User;
import net.william278.backend.database.repository.ChannelRepository;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.exception.*;
import net.william278.backend.service.GitHubDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Tags(value = @Tag(name = "Projects"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ProjectController {

    private final ProjectRepository projects;
    private final ChannelRepository channels;
    private final GitHubDataService github;

    @Autowired
    public ProjectController(ProjectRepository projects, ChannelRepository channels, GitHubDataService github) {
        this.projects = projects;
        this.channels = channels;
        this.github = github;
    }

    @Operation(
            summary = "Get a list of all projects."
    )
    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin(value = "*", allowCredentials = "false")
    public List<Project> getProjects() {
        return projects.findAll();
    }

    @Operation(
            summary = "Get a specific project."
    )
    @ApiResponse(
            responseCode = "200"
    )
    @ApiResponse(
            responseCode = "404",
            description = "The project was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping(
            value = "/v1/projects/{projectSlug:" + Project.PATTERN + "}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin(value = "*", allowCredentials = "false")
    public Project getProject(
            @Parameter(description = "The slug of the project to get.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug
    ) {
        return projects.findById(projectSlug).orElseThrow(ProjectNotFound::new);
    }

    @Operation(
            summary = "Create or update a project.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200"
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "The user is not an admin.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "The project was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @PutMapping(
            value = "/v1/projects/{projectSlug:" + Project.PATTERN + "}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public Project putProject(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The slug of the project to create/update.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug,

            @RequestBody Project project
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        if (projectSlug.isBlank() || !projectSlug.matches(Project.PATTERN)) {
            throw new InvalidProject();
        }

        // Update release channels
        projects.findById(project.getSlug()).ifPresent(ex -> project.setReleaseChannels(ex.getReleaseChannels()
                .stream().map(r -> channels.findChannelByName(r).orElse(channels.save(new Channel(r))))
                .collect(Collectors.toSet())));

        // Update README
        if (project.getMetadata().isPullReadmeFromGithub()) {
            final Project.Metadata metadata = project.getMetadata();
            github.getReadme(project).ifPresent(metadata::setReadmeBody);
            project.setMetadata(metadata);
        }

        project.setSlug(projectSlug);
        return projects.save(project);
    }

    @Operation(
            summary = "Delete a project.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The project was deleted."
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "The user is not an admin.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "The project was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @DeleteMapping(
            value = "/v1/projects/{projectSlug:" + Project.PATTERN + "}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public Project deleteProject(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The slug of the project to delete.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        final Project project = projects.findById(projectSlug).orElseThrow(ProjectNotFound::new);
        projects.deleteById(projectSlug);
        return project;
    }

}
