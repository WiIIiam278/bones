package net.william278.backend.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.controller.v1.StatsController;
import net.william278.backend.database.model.Project;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
                .withConnector(new OkHttpGitHubConnector(createClient("github")))
                .build();
    }

    @Override
    public Optional<StatsController.Stats> getStats(@NotNull Project project) {
        final String url = project.getMetadata().getGithub();
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(getGitHubStats(url));
        } catch (Throwable e) {
            log.warn("Exception fetching GitHub downloads for URL {}", url, e);
            return Optional.empty();
        }
    }

    @NotNull
    public StatsController.Stats getGitHubStats(@NotNull String repositoryUrl) throws IOException {
        final String repository = repositoryUrl.replaceAll("https://github.com/", "");
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
}
