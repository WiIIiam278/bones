package net.william278.backend.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(
        name = "Channel",
        description = "A release channel for versions of a project."
)
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "channels")
public class Channel {

    public static final String PATTERN = "[a-z0-9._-]+";

    @Id
    @Schema(
            name = "name",
            pattern = PATTERN,
            example = "release",
            description = "The lowercase channel name."
    )
    private String name;

}
