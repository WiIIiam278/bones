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

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.william278.backend.database.model.Project;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
public class StatsService {

    private static final long CACHE_TIME = 60 * 60 * 4; // 4 hours

    private final Set<StatsProvider> providers = Sets.newHashSet();
    private final Map<String, CachedStats> cache = new HashMap<>();


    @SneakyThrows
    @Autowired
    public StatsService(GitHubDataService github, ModrinthDataService modrinth, SpigotDataService spigot,
                        PolymartDataService polymart, HangarDataService hangar, BStatsDataService bStats,
                        LocalDataService local) {
        this.providers.addAll(Set.of(github, modrinth, spigot, polymart, hangar, bStats, local));
    }

    // Combine the stats from all services
    @NotNull
    public Project.Stats fetchStats(@NotNull Project project) {
        final String slug = project.getSlug();
        if (cache.containsKey(slug)) {
            final CachedStats cached = cache.get(slug);
            if (cached.isExpired()) {
                cached.setExpiry(Instant.now().plusSeconds(CACHE_TIME));
                CompletableFuture.runAsync(() -> cached.setStats(getStatsNow(project)));
            }
            return cached.getStats();
        }

        final Project.Stats newStats = new Project.Stats();
        cache.put(slug, new CachedStats(newStats, Instant.now().plusSeconds(CACHE_TIME)));
        CompletableFuture.runAsync(() -> cache.get(slug).setStats(getStatsNow(project)));
        return newStats;
    }

    // Get the stats from all services
    @NotNull
    private Project.Stats getStatsNow(@NotNull Project project) {
        return providers.stream()
                .filter(StatsProvider::isEnabled)
                .map((service) -> service.getStats(project))
                .filter(Optional::isPresent).map(Optional::get)
                .reduce(Project.Stats::combine)
                .orElse(new Project.Stats());
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class CachedStats {
        private Project.Stats stats;
        private Instant expiry;

        private boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
    }

}
