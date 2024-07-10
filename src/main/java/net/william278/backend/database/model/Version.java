package net.william278.backend.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "versions")
public class Version {

    public static final String PATTERN = "[a-zA-Z0-9\\\\+._-]+";

    @Id
    @JsonIgnore
    private Integer id;
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
    private String changelog;
    @Schema(
            name = "timestamp",
            description = "Timestamp of the version's release."
    )
    private Instant timestamp;
    @Schema(
            name = "project",
            description = "The project this version is associated with."
    )
    @ManyToOne
    private Project project;

    @Schema(
            name = "distribution",
            description = "The distribution this version is associated with."
    )
    @ManyToOne
    private Distribution distribution;

    @Schema(
            name = "channel",
            description = "The channel this version is associated with."
    )
    @ManyToOne
    private Channel channel;

    @Schema(
            name = "name",
            pattern = PATTERN,
            example = "HuskHomes-Paper-4.7.jar"
    )
    private String fileName;
    @Schema(
            name = "sha256",
            pattern = "[a-f0-9]{64}",
            example = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
    )
    private String fileHash;

    @JsonIgnore
    public boolean isRestricted() {
        return project.getRestricted();
    }

    public boolean canDownload(@NotNull User user) {
        return !isRestricted() || user.hasProjectPermission(project);
    }

}
