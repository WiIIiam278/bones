package net.william278.backend.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Integer id;

    @Schema(
            name = "project",
            description = "The project that the distribution is associated with."
    )
    @ManyToOne
    private Project project;

    @Schema(
            name = "name",
            pattern = PATTERN,
            description = "The distribution name.",
            example = "fabric-1.20.1"
    )
    private String name;

    @Schema(
            name = "description",
            description = "Text to specify the distribution target.",
            example = "Fabric 1.20.1"
    )
    private String description;

    @Schema(
            name = "archived",
            description = "Whether the distribution is archived and no longer active."
    )
    private Boolean archived;

}
