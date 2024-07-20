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
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.User;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.database.repository.UsersRepository;
import net.william278.backend.exception.ErrorResponse;
import net.william278.backend.exception.NoPermission;
import net.william278.backend.exception.NotAuthenticated;
import net.william278.backend.exception.UserNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@Tags(value = @Tag(name = "Users"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private final UsersRepository users;
    private final ProjectRepository projects;

    @Autowired
    public UserController(UsersRepository users, ProjectRepository projects) {
        this.users = users;
        this.projects = projects;
    }

    @Operation(
            summary = "Get a paginated list of all users.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @GetMapping(
            value = "/v1/users",
            produces = {MediaType.APPLICATION_JSON_VALUE}
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
    @CrossOrigin
    public Page<User> findPaginated(
            @AuthenticationPrincipal User principal,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            @RequestParam(value = "nameSearch", defaultValue = "") String nameSearch
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        if (nameSearch != null && !nameSearch.isBlank()) {
            return users.findAllByNameContainingIgnoreCase(nameSearch, PageRequest.of(page, size));
        }
        return users.findAll(PageRequest.of(page, size));
    }

    @Operation(
            summary = "Get a specific user by their ID.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The user"
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
            description = "The user was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping(
            value = "/v1/users/{userId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    public User getUser(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The ID of the user to get.")
            @PathVariable String userId
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        return users.findById(userId).orElseThrow(UserNotFound::new);
    }

    @Operation(
            summary = "Delete a specific user by their ID.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The user that was deleted."
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
            description = "The user was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @DeleteMapping(
            value = "/v1/users/{userId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public User deleteUser(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The ID of the user to delete.")
            @PathVariable String userId
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        final User user = users.findById(userId).orElseThrow(UserNotFound::new);
        if (principal.getId().equals(user.getId())) {
            throw new NoPermission();
        }
        if (user.isAdmin()) {
            throw new NoPermission();
        }
        users.deleteById(userId);
        return user;
    }

    @Operation(
            summary = "Set the projects a user has purchased.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The user with their updated list of purchased projects."
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "The user is not a staff member or admin.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "The user was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @PutMapping(
            value = "/v1/users/{userId}/purchases",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    public User setUserPurchases(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The ID of the user to get.")
            @PathVariable String userId,

            @RequestBody List<String> projects
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isStaff()) {
            throw new NoPermission();
        }
        final User user = users.findById(userId).orElseThrow(UserNotFound::new);

        // Get the projects from the project repository
        final Set<Project> updatedProjects = new HashSet<>();
        this.projects.findAllById(projects).forEach(updatedProjects::add);
        if (user.setPurchases(updatedProjects)) {
            return users.save(user);
        }
        return user;
    }

    @Operation(
            summary = "Set the role of a user.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The user with their updated permission flags"
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
            description = "The user was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @PutMapping(
            value = "/v1/users/{userId}/role",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    public User setUserRole(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The ID of the user to get.")
            @PathVariable String userId,

            @RequestBody User.Role role
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }

        // Prevent self- or illegal-updates
        final User user = users.findById(userId).orElseThrow(UserNotFound::new);
        if (user.equals(principal) || user.isAdmin()) {
            throw new NoPermission();
        }

        // Set the role & save
        user.setRole(role);
        return users.save(user);
    }

    @Operation(
            summary = "Get the currently logged-in user.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The currently logged-in user."
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping(
            value = "/v1/users/@me",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    public User getLoggedInUser(
            @AuthenticationPrincipal User principal
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        return principal;
    }

    @Operation(
            summary = "Delete the currently logged-in user.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The user that was deleted."
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @DeleteMapping(
            value = "/v1/users/@me",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public User deleteLoggedInUser(
            @AuthenticationPrincipal User principal
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        users.delete(principal);
        return principal;
    }

}
