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
import net.william278.backend.database.model.Post;
import net.william278.backend.database.model.User;
import net.william278.backend.database.repository.PostRepository;
import net.william278.backend.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@Tags(value = @Tag(name = "Posts"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class PostController {

    private final PostRepository posts;

    @Autowired
    public PostController(PostRepository posts) {
        this.posts = posts;
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
            @AuthenticationPrincipal User principal,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "categoryFilter", required = false) String categoryFilter
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        if (categoryFilter != null) {
            return posts.findAllByCategoryOrderByTimestampDesc(PageRequest.of(page, size), categoryFilter);
        }
        return posts.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
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
            value = "/v1/posts/{postSlug}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public Post getPost(
            @Parameter(description = "The slug of the post to get.")
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
            value = "/v1/posts/{postSlug}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public Post deletePost(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The slug of the post to delete.")
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
            value = "/v1/posts/{postSlug}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    public ResponseEntity<Post> putPost(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The slug of the post to create or update.")
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
        existingPost.ifPresentOrElse(
                (found) -> post.setId(found.getId()),
                () -> post.setAuthor(principal)
        );
        posts.save(post);

        return ResponseEntity.status(existingPost.isEmpty() ? 201 : 200).body(post);
    }

}
