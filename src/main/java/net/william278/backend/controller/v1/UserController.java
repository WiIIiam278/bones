package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.User;
import net.william278.backend.database.repository.UsersRepository;
import net.william278.backend.exception.NotAuthenticated;
import net.william278.backend.exception.UserNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Schema(name = "Users")
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private final AppConfiguration configuration;
    private final UsersRepository users;

    @Autowired
    public UserController(AppConfiguration configuration, UsersRepository users) {
        this.configuration = configuration;
        this.users = users;
    }

    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/user",
            produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    public User test(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        return (User) principal;
    }


}
