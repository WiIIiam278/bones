package net.william278.backend.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@Schema(
        name = "Distribution",
        description = "A platform distribution of a project."
)
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "distributions")
public class Distribution {

    public static final String PATTERN = "[a-z0-9._-]+";

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JsonIgnore
    private Project project;

    @Schema(
            name = "name",
            pattern = PATTERN,
            description = "The distribution name.",
            example = "fabric-1.20.1"
    )
    private String name;

    @Schema(
            name = "groupName",
            description = "The distribution group name.",
            example = "fabric"
    )
    @Builder.Default
    private String groupName = "fabric";

    @Schema(
            name = "description",
            description = "Text to specify the distribution target.",
            example = "Fabric 1.20.1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Nullable
    @Builder.Default
    private String description = null;

    @Schema(
            name = "archived",
            description = "Whether the distribution is archived and no longer active.",
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Builder.Default
    private Boolean archived = false;

    @SuppressWarnings("unused")
    public boolean isArchived() {
        return archived != null && archived;
    }

}
