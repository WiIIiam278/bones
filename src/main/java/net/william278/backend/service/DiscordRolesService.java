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

    private final String guildId;
    private final UsersRepository users;
    private final ProjectRepository projectRepository;

    private final OkHttpClient client = HTTPUtils.createClient("discord");
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public DiscordRolesService(AppConfiguration config, UsersRepository users, ProjectRepository projectRepository) {
        this.guildId = config.getDiscordGuildId();
        this.users = users;
        this.projectRepository = projectRepository;
    }

    public void updateMemberRoles(@NotNull User user, @NotNull String accessToken) {
        final Request request = new Request.Builder()
                .url(API_URL + ENDPOINT.formatted(guildId))
                .header("Authorization", "Bearer %s".formatted(accessToken))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("Failed to update roles for user {}: HTTP {}", user.getId(), response.code());
                return;
            }
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
        return projectRepository.findAllByRestrictedTrue()
                .stream().filter(p -> p.getMetadata().getLinkedDiscordRole() != null)
                .collect(Collectors.toMap(p -> p.getMetadata().getLinkedDiscordRole(), p -> p));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DiscordMember(@NotNull List<String> roles) {
    }


}
