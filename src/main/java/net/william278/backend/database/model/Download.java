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
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Schema(
        name = "Download",
        description = "A downloadable file, associated with a version."
)
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "downloads")
public class Download {

    public static final String PATTERN = "[a-zA-Z0-9._-\\\\+]+\\.[a-zA-Z0-9._-]+";

    @Id
    @JsonIgnore
    private Integer id;

    @Schema(
            name = "distribution",
            description = "The distribution this download is associated with."
    )
    @ManyToOne
    private Distribution distribution;

    @Schema(
            name = "name",
            pattern = PATTERN,
            example = "HuskHomes-Paper-4.7.jar"
    )
    private String name;

    @Schema(
            name = "md5",
            description = "The MD5 checksum of the file.",
            pattern = "[a-f0-9]{32}",
            example = "d41d8cd98f00b204e9800998ecf8427e"
    )
    private String md5;

    public void setMd5(byte[] md5) {
        this.md5 = new String(md5, StandardCharsets.UTF_8);
    }

}
