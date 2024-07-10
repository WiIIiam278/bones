package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.william278.backend.database.model.User;
import net.william278.backend.database.repository.UsersRepository;
import net.william278.backend.exception.NotAuthenticated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static net.william278.backend.controller.RootController.CORS_FRONTEND_ORIGIN;

@Schema(name = "Users")
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private final UsersRepository users;

    @Autowired
    public UserController(UsersRepository users) {
        this.users = users;
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
            value = "/v1/user",
            produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    @CrossOrigin(
            allowCredentials = "true", originPatterns = { CORS_FRONTEND_ORIGIN },
            methods = { RequestMethod.GET }
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
            value = "/v1/user",
            produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    @CrossOrigin(
            allowCredentials = "true", originPatterns = { CORS_FRONTEND_ORIGIN },
            methods = { RequestMethod.DELETE }
    )
    public User deleteLoggedInUser(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        users.delete(principal);
        return principal;
    }

}
