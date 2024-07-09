package net.william278.backend.database.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "channels")
public class Channel {

    public static final String PATTERN = "[a-zA-Z0-9._-]+";

    @Id
    private Integer id;
    @Schema(
            name = "name",
            pattern = PATTERN,
            example = "stable"
    )
    private String name;

}
