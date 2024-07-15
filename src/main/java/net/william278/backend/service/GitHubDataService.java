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
                .withConnector(new OkHttpGitHubConnector(HTTPUtils.createClient("github")))
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
