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
import net.william278.backend.database.model.Post;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.User;
import net.william278.backend.database.repository.PostRepository;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@RestController
@Tags(value = @Tag(name = "Posts"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class PostController {

    private final PostRepository posts;
    private final ProjectRepository projects;

    @Autowired
    public PostController(PostRepository posts, ProjectRepository projects) {
        this.posts = posts;
        this.projects = projects;
    }

    @Operation(
            summary = "Get a paginated list of all posts.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @GetMapping(
            value = "/v1/posts",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public Page<Post> findPaginated(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "categoryFilter", required = false) String categoryFilter
    ) {
        if (categoryFilter != null) {
            return posts.findAllByCategoryOrderByTimestampDesc(PageRequest.of(page, size), categoryFilter);
        }
        return posts.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
    }

    @Operation(
            summary = "Get a paginated list of all posts related to a specified project.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @GetMapping(
            value = "/v1/projects/{projectSlug:" + Project.PATTERN + "}/posts",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public Page<Post> findProjectPaginated(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,

            @Parameter(description = "The slug of the project to get posts for.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String projectSlug
    ) {
        return posts.findAllByAssociatedProjectOrderByTimestampDesc(PageRequest.of(page, size),
                projects.findById(projectSlug).orElseThrow(ProjectNotFound::new));
    }

    @Operation(
            summary = "Get a specific post by its slug."
    )
    @ApiResponse(
            responseCode = "200",
            description = "The post"
    )
    @ApiResponse(
            responseCode = "404",
            description = "The post was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping(
            value = "/v1/posts/{postSlug:" + Post.PATTERN + "}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public Post getPost(
            @Parameter(description = "The slug of the post to get.")
            @Pattern(regexp = Post.PATTERN)
            @PathVariable String postSlug
    ) {
        return posts.findBySlug(postSlug).orElseThrow(PostNotFound::new);
    }

    @Operation(
            summary = "Delete a specific post by its slug.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The post that was deleted."
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
            description = "The post was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @DeleteMapping(
            value = "/v1/posts/{postSlug:" + Post.PATTERN + "}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public Post deletePost(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The slug of the post to delete.")
            @Pattern(regexp = Post.PATTERN)
            @PathVariable String postSlug
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }

        final Post post = posts.findBySlug(postSlug).orElseThrow(PostNotFound::new);
        posts.deleteById(post.getId());
        return post;
    }

    @Operation(
            summary = "Create or update a specific post by its slug.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "201",
            description = "The post was created."
    )
    @ApiResponse(
            responseCode = "200",
            description = "The post was updated."
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
            description = "The post was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @PutMapping(
            value = "/v1/posts/{postSlug:" + Post.PATTERN + "}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    public ResponseEntity<Post> putPost(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The slug of the post to create or update.")
            @Pattern(regexp = Post.PATTERN)
            @PathVariable String postSlug,

            @RequestBody Post post
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }

        // If the post exists, update it, otherwise make it & set author
        final Optional<Post> existingPost = posts.findBySlug(postSlug);
        existingPost.ifPresent((found) -> post.setId(found.getId()));
        post.setAuthor(principal);
        post.setTimestamp(Instant.now());
        posts.save(post);

        return ResponseEntity.status(existingPost.isEmpty() ? 201 : 200).body(post);
    }

}
