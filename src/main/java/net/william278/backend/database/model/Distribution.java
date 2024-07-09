package net.william278.backend.database.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "distributions")
public class Distribution {

    public static final String PATTERN = "[a-z0-9._-]+";

    @Id
    private Integer id;
    @ManyToOne
    private Project project;
    @Schema(
            name = "name",
            pattern = PATTERN,
            example = "fabric-1.20.1"
    )
    private String name;
    @Schema(
            name = "description",
            example = "Fabric 1.20.1"
    )
    private String description;
    private Boolean archived;

}
