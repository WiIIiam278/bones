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

import am.ik.webhook.annotation.WebhookPayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.validation.constraints.Pattern;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.exception.*;
import net.william278.backend.service.ProjectDocsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tags(value = @Tag(name = "Project Documentation"))
@RequestMapping(produces = MediaType.TEXT_PLAIN_VALUE)
public class DocsController {

    private final ProjectRepository projects;
    private final ProjectDocsService docs;

    public DocsController(ProjectRepository projects, ProjectDocsService docs) {
        this.projects = projects;
        this.docs = docs;
    }

    @Operation(
            summary = "Get a documentation page for a project."
    )
    @ApiResponse(
            responseCode = "200"
    )
    @ApiResponse(
            responseCode = "404",
            description = "The project or documentation page was not found.",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "The project does not have documentation",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE
            )
    )
    @GetMapping(
            value = "/v1/projects/{projectSlug:" + Project.PATTERN
                    + "}/docs/{pageSlug:" + ProjectDocsService.PATTERN + "}",
            produces = {MediaType.TEXT_PLAIN_VALUE}
    )
    @CrossOrigin("*")
    public String getProjectDocsPage(
            @Parameter(description = "The slug of the project to get docs for.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug,

            @Parameter(description = "The slug of the docs page.")
            @PathVariable String pageSlug
    ) {
        if (projectSlug.isBlank() || !projectSlug.matches(Project.PATTERN)) {
            throw new InvalidProject();
        }
        final Project project = projects.findById(projectSlug).orElseThrow(InvalidProject::new);
        if (!project.getMetadata().isDocumentation()) {
            throw new UndocumentedProject();
        }

        return docs.getPage(project, pageSlug).orElseThrow(DocsPageNotFound::new);
    }

    @Operation(
            summary = "Update the documentation for a project from a GitHub Payload.",
            security = {
                    @SecurityRequirement(name = "APIKey")
            }
    )
    @ApiResponse(
            responseCode = "200"
    )
    @ApiResponse(
            responseCode = "404",
            description = "The project was not found.",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "The project does not have documentation",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "The secret hash is incorrect.",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "The secret hash was not provided",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE
            )
    )
    @PostMapping(
            value = "/v1/projects/{projectSlug:" + Project.PATTERN + "}/docs"
    )
    @CrossOrigin("*")
    public ResponseEntity<?> updateProjectDocs(
            @Parameter(description = "The slug of the project to get docs for.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug,

            @Parameter(description = "Webhook contents.")
            @SuppressWarnings("unused")
            @WebhookPayload @RequestBody String payload
    ) {
        final Project project = projects.findById(projectSlug).orElseThrow(InvalidProject::new);
        if (!project.getMetadata().isDocumentation()) {
            throw new UndocumentedProject();
        }

        if (docs.updateWiki(project)) {
            return ResponseEntity.ok().build();
        }
        throw new FailedToUpdateDocs();
    }

}
