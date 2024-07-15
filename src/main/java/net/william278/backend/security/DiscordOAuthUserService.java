package net.william278.backend.security;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.User;
import net.william278.backend.database.repository.UsersRepository;
import net.william278.backend.service.DiscordRolesService;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordOAuthUserService extends DefaultOAuth2UserService {

    private final AppConfiguration config;
    private final UsersRepository users;
    private final DiscordRolesService discordRoles;

    @Override
    @SneakyThrows
    public OAuth2User loadUser(@NotNull OAuth2UserRequest oAuth2UserRequest) {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        return processOAuth2User(oAuth2User, oAuth2UserRequest.getAccessToken().getTokenValue());
    }

    // Create or update the user in the database
    @NotNull
    private OAuth2User processOAuth2User(@NotNull OAuth2User oAuthUser, @NotNull String accessToken) {
        final String id = oAuthUser.getAttribute("id");
        if (id == null) {
            log.error("Discord OAuth2 user ID is null, aborting user processing");
            return oAuthUser;
        }

        // Get/create user
        final User user = users.save(users.findById(id)
                .map(u -> updateUser(u, oAuthUser))
                .orElse(createUser(oAuthUser)));

        // Update roles
        discordRoles.updateMemberRoles(user, accessToken);
        return user;
    }

    @NotNull
    private User updateUser(User user, OAuth2User oAuth2User) {
        user.setName(oAuth2User.getAttribute("username"));
        user.setAvatar(oAuth2User.getAttribute("avatar"));
        user.setEmail(oAuth2User.getAttribute("email"));
        return user;
    }

    @NotNull
    private User createUser(@NotNull OAuth2User oAuth2User) {
        final String id = oAuth2User.getAttribute("id");
        final String username = oAuth2User.getAttribute("username");

        // Determine whether to make admin account
        final String adminId = config.getDefaultAdminDiscordId();
        final boolean isAdmin = adminId != null && adminId.equals(id);
        log.info("Creating new {} for {}", isAdmin ? "admin account" : "user account", username);

        // Create the user object
        return User.builder()
                .id(id)
                .name(username)
                .createdAt(Instant.now())
                .email(oAuth2User.getAttribute("email"))
                .avatar(oAuth2User.getAttribute("avatar"))
                .admin(isAdmin)
                .purchases(new HashSet<>())
                .build();
    }

}
