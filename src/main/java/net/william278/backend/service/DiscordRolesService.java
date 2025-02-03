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
import net.william278.backend.util.HTTPUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DiscordRolesService {

    private static final String API_URL = "https://discord.com/api/v10";
    private static final String ENDPOINT = "/users/@me/guilds/%s/member";

    private final OkHttpClient client = HTTPUtils.createClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ProjectRepository projects;
    private final String rolesGuildId;

    @Autowired
    public DiscordRolesService(AppConfiguration config, ProjectRepository projects) {
        this.rolesGuildId = config.getDiscordGuildId();
        this.projects = projects;
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true, noRollbackFor = Exception.class)
    public User updateMemberRoles(@NotNull User user, @NotNull String accessToken) {
        final Request request = new Request.Builder()
                .url(API_URL + ENDPOINT.formatted(rolesGuildId))
                .header("Authorization", "Bearer %s".formatted(accessToken))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("HTTP {} fetching roles for {} - are they a guild member?", response.code(), user.getName());
                return user;
            }

            // Get the member's roles
            final String body = Objects.requireNonNull(response.body(), "response body").string();
            final DiscordMember member = mapper.readValue(body, DiscordMember.class);

            // Get linked projects and update
            final Set<Project> linkedProjects = getLinkedProjects(member.roles());
            if (user.addPurchases(linkedProjects)) {
                log.info("Applied project grants from Discord roles for {}: {}", user.getName(), user.getPurchases());
                return user;
            }
        } catch (Throwable e) {
            log.error("Failed to update project grants from Discord for {}", user.getName(), e);
        }
        return user;
    }

    @NotNull
    private Set<Project> getLinkedProjects(@NotNull List<String> roles) {
        final Map<String, Project> projectRoles = getProjectRoles();
        return roles.stream()
                .filter(projectRoles::containsKey)
                .map(projectRoles::get)
                .collect(Collectors.toSet());
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
