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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import masecla.modrinth4j.client.agent.UserAgent;
import masecla.modrinth4j.main.ModrinthAPI;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class ModrinthDataService implements StatsProvider {

    private final @Nullable String modrinthApiToken;
    private ModrinthAPI modrinth;

    @Autowired
    @SneakyThrows
    public ModrinthDataService(@NotNull AppConfiguration config) {
        this.modrinthApiToken = config.getModrinthApiToken();
        if (modrinthApiToken == null || modrinthApiToken.isEmpty()) {
            return;
        }

        this.modrinth = ModrinthAPI.rateLimited(
                UserAgent.builder()
                        .authorUsername("william278").contact("will27528@gmail.com")
                        .projectName("william278-backend").build(),
                modrinthApiToken
        );
    }

    @Override
    public Optional<Project.Stats> getStats(@NotNull Project project) {
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
                .map(p -> Project.Stats.builder().downloadCount(p.getDownloads()).build());
    }

    @Override
    public boolean isEnabled() {
        return modrinth != null;
    }

    private Optional<String> getModrinthSlug(@NotNull Project project) {
        return project.getMetadata().getLinkUrlById("modrinth")
                .map((url) -> url.substring(url.lastIndexOf('/') + 1));
    }

}
