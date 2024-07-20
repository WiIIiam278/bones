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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.*;
import net.william278.backend.database.repository.*;
import net.william278.backend.util.HTTPUtils;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@AllArgsConstructor
public class GitHubImportService {

    private final AppConfiguration config;
    private final GitHubDataService github;

    private final VersionRepository versions;
    private final DistributionRepository distributions;
    private final ChannelRepository channels;
    private final DownloadRepository downloads;
    private final ProjectRepository projects;

    public void importGithub(@NotNull Project project, @NotNull VersionSource source, @NotNull Channel channel,
                             @NotNull Map<String, Distribution> distributionMatchers) throws IllegalArgumentException {
        if (project.getMetadata().getGithub().isBlank()) {
            throw new IllegalArgumentException("Project does not have a GitHub repository.");
        }
        switch (source) {
            case RELEASE -> CompletableFuture.runAsync(() -> importReleases(project, channel, distributionMatchers));
//            case COMMIT -> importCommits(project, distributionMatchers);
        }
    }

    private void importReleases(@NotNull Project project, @NotNull Channel channel,
                                @NotNull Map<String, Distribution> distributionMatchers) {
        log.info("Importing releases for project {}", project.getSlug());

        // Prepare channel
        if (channels.findChannelByName(channel.getName()).isEmpty()) {
            channels.save(channel);
        }
        project.addReleaseChannel(channel);
        projects.save(project);

        // Import releases
        github.getReleases(project).stream()
                .filter((release) -> versions.findByProjectAndChannelAndName(
                        project, channel, release.getTagName()).isEmpty()
                )
                .map((release) -> releaseToVersion(project, channel, release, distributionMatchers))
                .forEach((ver -> {
                    try {
                        versions.save(ver);
                        log.info("Imported version {} for project {}", ver.getName(), project.getSlug());
                    } catch (Throwable e) {
                        log.warn("Exception saving version {} for project {}", ver.getName(), project.getSlug(), e);
                    }
                }));
        log.info("Imported all releases for project {}", project.getSlug());
    }

    private void importCommits(@NotNull Project project, @NotNull Map<String, Distribution> distributionMatchers) {
        log.info("Importing commits for project {}", project.getSlug());
        throw new UnsupportedOperationException("Commit import is not yet implemented.");
    }

    @SneakyThrows
    private Version releaseToVersion(@NotNull Project project, @NotNull Channel channel,
                                     @NotNull GHRelease release, @NotNull Map<String, Distribution> distributionMatchers) {
        return Version.builder()
                .project(project)
                .channel(channel)
                .name(release.getTagName())
                .changelog(release.getBody())
                .timestamp(release.getPublished_at().toInstant())
                .downloads(release.listAssets().toList().stream().map(r -> assetToDownload(
                        r, project, release.getTagName(), channel, distributionMatchers
                ).orElse(null)).filter(Objects::nonNull).toList())
                .build();
    }

    private Optional<Download> assetToDownload(@NotNull GHAsset asset, @NotNull Project project,
                                               @NotNull String versionName, @NotNull Channel channel,
                                               @NotNull Map<String, Distribution> distributionMatchers) {
        // Download from asset.getBrowserDownloadUrl() and place at getUploadPathFor() (making dirs if needed)
        final OkHttpClient client = HTTPUtils.createClient();
        final Request request = new Request.Builder()
                .url(asset.getBrowserDownloadUrl())
                .addHeader("Accept", "application/octet-stream")
                .build();
        final Call call = client.newCall(request);
        try (Response response = call.execute();
             InputStream inputStream = Objects.requireNonNull(response.body(), "Response body").byteStream()) {

            // Get the distribution for the asset
            final Optional<Distribution> distro = distributionMatchers.entrySet().stream()
                    .map((entry) -> asset.getName().matches(entry.getKey()) ? entry.getValue() : null)
                    .filter(Objects::nonNull).findFirst();
            if (distro.isEmpty()) {
                log.warn("No distribution matcher found for asset {}", asset.getName());
                return Optional.empty();
            }
            final Distribution distribution = distributions.findDistributionByNameAndProject(distro.get().getName(), project)
                    .orElseGet(() -> {
                        distro.get().setProject(project);
                        return distributions.save(distro.get());
                    });
            final Path path = getUploadPathFor(project, channel, distribution, versionName, asset.getName());

            // Copy the input stream to the file path
            log.info("Downloading asset {} from GitHub to {} ({}kb)", asset.getName(), path, asset.getSize() / 1024);
            final byte[] md5 = DigestUtils.md5Digest(inputStream);
            FileUtils.copyToFile(inputStream, path.toFile());
            log.info("Downloaded asset {}!", asset.getName());

            // Return the download object
            final Download download = Download.builder()
                    .name(asset.getName())
                    .distribution(distribution)
                    .fileSize(asset.getSize())
                    .build();
            download.setMd5(md5);
            return Optional.of(downloads.save(download));
        } catch (Throwable e) {
            log.warn("Exception downloading asset {} from GitHub", asset.getName(), e);
        }
        return Optional.empty();
    }

    @NotNull
    private Path getUploadPathFor(@NotNull Project project, @NotNull Channel channel,
                                  @NotNull Distribution distribution, @NotNull String versionName,
                                  @NotNull String fileName) {
        return config.getStoragePath()
                .resolve(project.getSlug())
                .resolve(channel.getName())
                .resolve(versionName)
                .resolve(distribution.getName())
                .resolve(fileName);
    }

    @Getter
    @Schema(description = "The source of versions to import.")
    public enum VersionSource {
        @Schema(description = "Import versions from GitHub releases.")
        RELEASE
//        @Schema(description = "Import versions from GitHub commits.")
//        COMMIT;

    }

}
