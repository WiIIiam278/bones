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
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.controller.v1.StatsController;
import net.william278.backend.database.model.Project;
import net.william278.backend.util.HTTPUtils;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.*;
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class GitHubDataService implements StatsService {

    private final static int PAGE_SIZE = 50;
    private final GitHub github;

    @Autowired
    @SneakyThrows
    public GitHubDataService(@NotNull AppConfiguration config) {
        this.github = new GitHubBuilder()
                .withOAuthToken(config.getGithubApiToken())
                .withConnector(new OkHttpGitHubConnector(HTTPUtils.createCachingClient("github")))
                .build();
    }

    @Override
    public Optional<StatsController.Stats> getStats(@NotNull Project project) {
        return getRepositoryId(project).map(id -> {
            try {
                return getGitHubStats(id);
            } catch (IOException e) {
                log.warn("Exception fetching GitHub stats for project {}", id, e);
                return StatsController.Stats.builder().build();
            }
        });
    }

    public Optional<String> getReadme(@NotNull Project project) {
        final String repoId = getRepositoryId(project).orElseThrow(IllegalArgumentException::new);
        try (InputStream readme = github.getRepository(repoId).getReadme().read()) {
            return Optional.of(new String(readme.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("Exception fetching GitHub README for project {}", repoId, e);
            return Optional.empty();
        }
    }

    private Optional<String> getRepositoryId(@NotNull Project project) {
        final String url = project.getMetadata().getGithub();
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(url.replaceAll("https://github.com/", ""));
    }

    @NotNull
    public StatsController.Stats getGitHubStats(@NotNull String repository) throws IOException {
        final AtomicLong downloads = new AtomicLong();
        final GHRepository repo = github.getRepository(repository);
        repo.listReleases()._iterator(PAGE_SIZE).forEachRemaining(
                release -> downloads.getAndAdd(getTotalReleaseAssetDownloads(release))
        );
        return StatsController.Stats.builder()
                .interactions(repo.getStargazersCount())
                .downloadCount(downloads.get())
                .build();
    }

    public long getTotalReleaseAssetDownloads(@NotNull GHRelease release) {
        try {
            final AtomicLong total = new AtomicLong();
            release.listAssets()._iterator(PAGE_SIZE)
                    .forEachRemaining(asset -> total.getAndAdd(asset.getDownloadCount()));
            return total.get();
        } catch (Throwable e) {
            log.warn("Exception fetching GitHub downloads for release {}", release.getTagName(), e);
            return 0L;
        }
    }

    @SneakyThrows
    List<GHRelease> getReleases(@NotNull Project project) {
        final String repoId = getRepositoryId(project).orElseThrow(IllegalArgumentException::new);
        return github.getRepository(repoId).listReleases().toList();
    }

    @SneakyThrows
    List<GHCommit> getCommits(@NotNull Project project) {
        final String repoId = getRepositoryId(project).orElseThrow(IllegalArgumentException::new);
        return github.getRepository(repoId).listCommits().toList();
    }



}
