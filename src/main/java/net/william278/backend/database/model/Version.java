package net.william278.backend.database.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "versions")
public class Version {

    public static final String PATTERN = "[a-zA-Z0-9\\\\+._-]+";

    @Id
    private Integer id;
    private String name;
    private String changelog;
    private Instant timestamp;
    @ManyToOne
    private Project project;
    @ManyToOne
    private Distribution distribution;
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

    public boolean isRestricted() {
        return project.isRestricted();
    }

    public boolean canDownload(@NotNull User user) {
        return !isRestricted() || user.hasProjectPermission(project);
    }

}
