package net.william278.backend.database.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "projects")
public class Project {

    public static final String PATTERN = "[a-zA-Z0-9._-]+";

    @Id
    private String id;
    @Schema(
            name = "name",
            pattern = PATTERN,
            example = "HuskHomes"
    )
    private String name;
    private Boolean restricted;
    @Nullable
    private Integer associatedRoleId;

}
