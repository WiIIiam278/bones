package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.User;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.database.repository.UsersRepository;
import net.william278.backend.exception.NoPermission;
import net.william278.backend.exception.NotAuthenticated;
import net.william278.backend.exception.UserNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static net.william278.backend.controller.RootController.CORS_FRONTEND_ORIGIN;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class UsersController {

    private final UsersRepository users;
    private final ProjectRepository projects;

    @Autowired
    public UsersController(UsersRepository users, ProjectRepository projects) {
        this.users = users;
        this.projects = projects;
    }

    @Operation(summary = "Paginate through all users.", security = {
            @SecurityRequirement(name = "OAuth2")
    })
    @GetMapping(
            value = "/v1/users",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            params = {"page", "size"}
    )
    @CrossOrigin(
            allowCredentials = "true", originPatterns = {CORS_FRONTEND_ORIGIN},
            methods = {RequestMethod.GET}
    )
    public Page<User> findPaginated(@AuthenticationPrincipal User principal,
                                    @RequestParam("page") int page, @RequestParam("size") int size) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        return users.findAll(PageRequest.of(page, size));
    }

    @Operation(summary = "Paginate through all users.", security = {
            @SecurityRequirement(name = "OAuth2")
    })
    @GetMapping(
            value = "/v1/users/search",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            params = {"page", "size", "search"}
    )
    @CrossOrigin(
            allowCredentials = "true", originPatterns = {CORS_FRONTEND_ORIGIN},
            methods = {RequestMethod.GET}
    )
    public Page<User> searchPaginated(@AuthenticationPrincipal User principal,
                                      @RequestParam("page") int page, @RequestParam("size") int size,
                                      @RequestParam("search") String search) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        return users.findAllByNameContainingIgnoreCase(search, PageRequest.of(page, size));
    }

    @Operation(summary = "Get a specific user by their ID.", security = {
            @SecurityRequirement(name = "OAuth2")
    })
    @ApiResponse(
            responseCode = "200",
            description = "The user",
            content = @Content(schema = @Schema(implementation = User.class))
    )
    @GetMapping(
            value = "/v1/users/{userId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin(
            allowCredentials = "true", originPatterns = {CORS_FRONTEND_ORIGIN},
            methods = {RequestMethod.GET}
    )
    public User getUser(@AuthenticationPrincipal User principal, @PathVariable String userId) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        return users.findById(userId).orElseThrow(UserNotFound::new);
    }

    @Operation(summary = "Delete a specific user by their ID.", security = {
            @SecurityRequirement(name = "OAuth2")
    })
    @ApiResponse(
            responseCode = "200",
            description = "The user that was deleted.",
            content = @Content(schema = @Schema(implementation = User.class))
    )
    @DeleteMapping(
            value = "/v1/users/{userId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin(
            allowCredentials = "true", originPatterns = {CORS_FRONTEND_ORIGIN},
            methods = {RequestMethod.DELETE}
    )
    public User deleteUser(@AuthenticationPrincipal User principal, @PathVariable String userId) {
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
        users.delete(user);
        return user;
    }

    @Operation(
            summary = "Set the projects a user is assigned to.",
            security = {@SecurityRequirement(name = "OAuth2")
    })
    @ApiResponse(
            responseCode = "200",
            description = "The user with their updated projects.",
            content = @Content(schema = @Schema(implementation = User.class))
    )
    @PutMapping(
            value = "/v1/users/{userId}/projects",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin(
            allowCredentials = "true", originPatterns = {CORS_FRONTEND_ORIGIN},
            methods = {RequestMethod.PUT}
    )
    public User setUserProjects(@AuthenticationPrincipal User principal, @PathVariable String userId,
                                @RequestBody List<String> projects) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        final User user = users.findById(userId).orElseThrow(UserNotFound::new);

        // Get the projects from the project repository
        final List<Project> projectList = new ArrayList<>();
        this.projects.findAllById(projects).forEach(projectList::add);
        user.setProjects(projectList);

        return users.save(user);
    }

    @Operation(summary = "Get the currently logged-in user.", security = {
            @SecurityRequirement(name = "OAuth2")
    })
    @ApiResponse(
            responseCode = "200",
            description = "The currently logged-in user.",
            content = @Content(schema = @Schema(implementation = User.class))
    )
    @GetMapping(
            value = "/v1/users/@me",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin(
            allowCredentials = "true", originPatterns = {CORS_FRONTEND_ORIGIN},
            methods = {RequestMethod.GET}
    )
    public User getLoggedInUser(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        return principal;
    }

    @Operation(summary = "Delete the account of the currently logged-in user.", security = {
            @SecurityRequirement(name = "OAuth2")
    })
    @ApiResponse(
            responseCode = "200",
            description = "The user that was deleted.",
            content = @Content(schema = @Schema(implementation = User.class))
    )
    @DeleteMapping(
            value = "/v1/users/@me",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin(
            allowCredentials = "true", originPatterns = {CORS_FRONTEND_ORIGIN},
            methods = {RequestMethod.DELETE}
    )
    public User deleteLoggedInUser(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        users.delete(principal);
        return principal;
    }

}
