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

        return users.save(discordRoles.updateMemberRoles(
                users.findById(id).map(u -> updateUser(u, oAuthUser)).orElseGet(() -> createUser(oAuthUser)),
                accessToken
        ));
    }

    @NotNull
    private User updateUser(User user, OAuth2User oAuth2User) {
        user.setName(oAuth2User.getAttribute("username"));
        user.setAvatar(oAuth2User.getAttribute("avatar"));
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
                .role(isAdmin ? User.Role.ADMIN : User.Role.USER)
                .purchases(new HashSet<>())
                .build();
    }

}
