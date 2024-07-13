package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.*;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.exception.ProjectNotFound;
import net.william278.backend.service.GitHubDataService;
import net.william278.backend.service.ModrinthDataService;
import net.william278.backend.service.SpigotDataService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Schema(name = "Stats")
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class StatsController {

    private static final long CACHE_TIME = 60 * 60 * 4; // 4 hours

    private final GitHubDataService github;
    private final ModrinthDataService modrinth;
    private final SpigotDataService spigot;
    private final ProjectRepository projects;
    private final Map<String, CachedStats> cache = new HashMap<>();


    @SneakyThrows
    @Autowired
    public StatsController(GitHubDataService github, ModrinthDataService modrinth, SpigotDataService spigot,
                           ProjectRepository projects) {
        this.github = github;
        this.modrinth = modrinth;
        this.spigot = spigot;
        this.projects = projects;
    }

    @Operation(
            summary = "Get a specific project."
    )
    @ApiResponse(
            responseCode = "200"
    )
    @GetMapping(
            value = "/v1/projects/{projectId}/stats",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    public Stats getProject(@PathVariable String projectId) {
        return fetchStats(projects.findById(projectId).orElseThrow(ProjectNotFound::new));
    }

    // Combine the stats from all services
    @NotNull
    private Stats fetchStats(@NotNull Project project) {
        final String slug = project.getSlug();
        if (cache.containsKey(slug)) {
            final CachedStats cached = cache.get(slug);
            if (cached.isExpired()) {
                cached.setExpiry(Instant.now().plusSeconds(CACHE_TIME));
                CompletableFuture.runAsync(() -> cached.setStats(getStatsNow(project)));
            }
            return cached.getStats();
        }

        final Stats newStats = new Stats();
        cache.put(slug, new CachedStats(newStats, Instant.now().plusSeconds(CACHE_TIME)));
        CompletableFuture.runAsync(() -> cache.get(slug).setStats(getStatsNow(project)));
        return newStats;
    }

    @NotNull
    private Stats getStatsNow(@NotNull Project project) {
        return Stream.of(github, modrinth, spigot)
                .map((service) -> service.getStats(project))
                .filter(Optional::isPresent).map(Optional::get)
                .reduce(Stats::combine)
                .orElse(new Stats());
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class CachedStats {
        private Stats stats;
        private Instant expiry;

        private boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(
            name = "Stats",
            description = "A collection of statistics for a project"
    )
    public static class Stats {

        @Schema(
                name = "downloadCount",
                description = "The number of times the project has been downloaded"
        )
        public long downloadCount;

        @Schema(
                name = "averageRating",
                description = "The average star rating of the project, out of 5"
        )
        public double averageRating;

        @Schema(
                name = "numberOfRatings",
                description = "The number of ratings the project has received"
        )
        public long numberOfRatings;

        @Schema(
                name = "interactions",
                description = "The number of positive interactions the project has received"
        )
        public long interactions;

        @NotNull
        public StatsController.Stats combine(@NotNull StatsController.Stats other) {
            return new Stats(
                    this.downloadCount + other.downloadCount,
                    other.numberOfRatings <= 0 ? this.numberOfRatings :
                            (this.averageRating * this.numberOfRatings + other.averageRating * other.numberOfRatings) /
                            (this.numberOfRatings + other.numberOfRatings),
                    this.numberOfRatings + other.numberOfRatings,
                    this.interactions + other.interactions
            );
        }

    }
}
