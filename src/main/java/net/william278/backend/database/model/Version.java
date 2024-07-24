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

package net.william278.backend.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.exception.DownloadNotFound;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Schema(
        name = "Version",
        description = "A version of a project on a channel, for a distribution target platform."
)
@Entity
@Table(name = "versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Version {

    public static final String PATTERN = "[a-zA-Z0-9\\\\+._-]+";
    public static final String DEFAULT_CHANGELOG = "No changelog provided.";

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JsonIgnore
    private Project project;

    @ManyToOne
    @JsonIgnore
    private Channel channel;

    @Schema(
            name = "version",
            description = "Name/tag of the version.",
            pattern = PATTERN,
            example = "4.7"
    )
    private String name;

    @Schema(
            name = "changelog",
            description = "Changelog for the version."
    )
    @Builder.Default
    @Column(length = Integer.MAX_VALUE)
    private String changelog = DEFAULT_CHANGELOG;

    @Schema(
            name = "timestamp",
            description = "Timestamp of the version's release.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Schema(
            name = "downloads",
            description = "The downloads associated with this version.",
            minLength = 1
    )
    @OneToMany
    private List<Download> downloads;

    @Schema(
            name = "downloadCount",
            description = "The number of times this download has been accessed."
    )
    private long downloadCount;

    @JsonIgnore
    public boolean isRestricted() {
        return project.isRestricted();
    }

    public boolean canDownload(@NotNull User user) {
        return !isRestricted() || user.hasProjectPermission(project);
    }

    @NotNull
    @JsonIgnore
    public List<Distribution> getDistributions() {
        return downloads.stream().map(Download::getDistribution).toList();
    }

    public void incrementDownloadCount() {
        downloadCount++;
    }

    public boolean hasDistribution(@NotNull Distribution distribution) {
        return getDistributions().contains(distribution);
    }

    @NotNull
    public Download getDownloadFor(@NotNull Distribution distribution) {
        final Optional<Download> download = downloads.stream().filter(d -> d.getDistribution().equals(distribution)).findFirst();
        return download.orElseThrow(DownloadNotFound::new);
    }

    @NotNull
    public Path getDownloadPathFor(@NotNull Distribution distribution, @NotNull AppConfiguration config) {
        return config.getStoragePath()
                .resolve(project.getSlug())
                .resolve(channel.getName())
                .resolve(name)
                .resolve(distribution.getName())
                .resolve(getDownloadFor(distribution).getName());
    }

}
