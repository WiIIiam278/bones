package net.william278.backend.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import masecla.modrinth4j.client.agent.UserAgent;
import masecla.modrinth4j.main.ModrinthAPI;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.controller.v1.StatsController;
import net.william278.backend.database.model.Project;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class ModrinthDataService implements StatsService {

    private final ModrinthAPI modrinth;

    @Autowired
    @SneakyThrows
    public ModrinthDataService(@NotNull AppConfiguration config) {
        this.modrinth = ModrinthAPI.rateLimited(
                UserAgent.builder()
                        .authorUsername("william278").contact("will27528@gmail.com")
                        .projectName("william278-backend").build(),
                config.getModrinthApiToken()
        );
    }

    @Override
    public Optional<StatsController.Stats> getStats(@NotNull Project project) {
        return getModrinthSlug(project)
                .flatMap(slug -> {
                    try {
                        return Optional.of(modrinth.projects().get(
                                modrinth.projects().getProjectIdBySlug(slug).join()
                        ).join());
                    } catch (Throwable e) {
                        log.warn("Exception fetching Modrinth project {}", slug, e);
                        return Optional.empty();
                    }
                })
                .map(p -> StatsController.Stats.builder().downloadCount(p.getDownloads()).build());
    }

    private Optional<String> getModrinthSlug(@NotNull Project project) {
        return project.getMetadata().getLinkUrlById("modrinth")
                .map((url) -> url.substring(url.lastIndexOf('/') + 1));
    }

}
