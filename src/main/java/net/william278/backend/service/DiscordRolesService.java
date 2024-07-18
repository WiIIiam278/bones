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

package net.william278.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.User;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.database.repository.UsersRepository;
import net.william278.backend.util.HTTPUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DiscordRolesService {

    private static final String API_URL = "https://discord.com/api/v10";
    private static final String ENDPOINT = "/users/@me/guilds/%s/member";

    private final OkHttpClient client = HTTPUtils.createClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String guildId;
    private final ProjectRepository projects;
    private final UsersRepository users;

    @Autowired
    public DiscordRolesService(AppConfiguration config, ProjectRepository projects, UsersRepository users) {
        this.guildId = config.getDiscordGuildId();
        this.projects = projects;
        this.users = users;
    }

    public void updateMemberRoles(@NotNull User user, @NotNull String accessToken) {
        final Request request = new Request.Builder()
                .url(API_URL + ENDPOINT.formatted(guildId))
                .header("Authorization", "Bearer %s".formatted(accessToken))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("HTTP {} fetching roles for {} - are they a guild member?", response.code(), user.getName());
                return;
            }

            // Get the member's roles
            final String body = Objects.requireNonNull(response.body(), "response body").string();
            final DiscordMember member = mapper.readValue(body, DiscordMember.class);

            // Get linked projects and update
            final List<Project> linkedProjects = getLinkedProjects(member.roles());
            if (user.setPurchases(linkedProjects)) {
                log.info("Updated roles for user {} to {}", user.getId(), user.getPurchases());
                users.save(user);
            }
        } catch (Throwable e) {
            log.error("Failed to update roles for user {}", user.getId(), e);
        }
    }

    @NotNull
    private List<Project> getLinkedProjects(@NotNull List<String> roles) {
        final Map<String, Project> projectRoles = getProjectRoles();
        return roles.stream()
                .filter(projectRoles::containsKey)
                .map(projectRoles::get)
                .toList();
    }

    @NotNull
    private Map<String, Project> getProjectRoles() {
        return projects.findAllByRestrictedTrue()
                .stream().filter(p -> p.getMetadata().getLinkedDiscordRole() != null)
                .collect(Collectors.toMap(p -> p.getMetadata().getLinkedDiscordRole(), p -> p));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DiscordMember(@NotNull List<String> roles) {
    }


}