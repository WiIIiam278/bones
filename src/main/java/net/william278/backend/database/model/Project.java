package net.william278.backend.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Schema(
        name = "Project",
        description = "A project."
)
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "projects")
public class Project {

    public static final String PATTERN = "[a-z0-9._-]+";

    @Schema(
            name = "slug",
            pattern = PATTERN,
            example = "huskhomes",
            description = "The project's unique slug ID."
    )
    @Id
    @Length(min = 1, max = 64)
    private String slug;

    @Schema(
            name = "restricted",
            description = "Whether the project is restricted to certain users."
    )
    private boolean restricted;

    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Column(length = Integer.MAX_VALUE)
    private String metadata;

    @Schema(
            name = "metadata",
            description = "JSON metadata for the project."
    )
    @JsonSerialize
    @SneakyThrows
    @NotNull
    public Metadata getMetadata() {
        return new ObjectMapper().readValue(metadata, Metadata.class);
    }

    @SneakyThrows
    public void setMetadata(@NotNull Metadata metadata) {
        this.metadata = new ObjectMapper().writeValueAsString(metadata);
    }

    @Schema(
            name = "Metadata",
            description = "Metadata for a project."
    )
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Metadata {
        @Schema(
                name = "name",
                description = "The project's name.",
                example = "HuskHomes"
        )
        @Length(min = 1, max = 255)
        private String name;

        @Schema(
                name = "tagline",
                description = "A brief description of the project.",
                example = "A lightweight, fast and feature-rich homes plugin.",
                maxLength = 32768
        )
        @Length(min = 1, max = 32768)
        @Column(length = 32768)
        private String tagline;

        @Schema(
                name = "license",
                description = "The project's license.",
                example = "MIT"
        )
        @Length(min = 1, max = 64)
        private String license;

        @Schema(
                name = "tags",
                description = "A set of tags associated with the project.",
                example = "[\"homes\", \"teleportation\"]"
        )
        private Set<String> tags;

        @Schema(
                name = "repository",
                description = "The project's GitHub repository URL.",
                example = "https://github.com/WiIIiam278/HuskHomes"
        )
        @Builder.Default
        @Length(max = 256)
        private String repository = null;

        @Schema(
                name = "readmePath",
                description = "Path to a README page for the project. If not set, will be pulled from the repo.",
                example = "/about-huskhomes"
        )
        @Builder.Default
        @Length(max = 256)
        private String readmePath = null;

        @Schema(
                name = "links",
                description = "A map of links associated with the project.",
                example = "{\"spigot\": \"https://www.spigotmc.org/resources/huskhomes.83767/\"}"
        )
        @Builder.Default
        private Map<String, String> links = new TreeMap<>();

        @Schema(
                name = "maintainers",
                description = "A set of maintainers for the project.",
                example = "[\"William278\"]"
        )
        @Builder.Default
        private Set<String> maintainers = new HashSet<>(Set.of("William278"));

        @Schema(
                name = "archived",
                description = "Whether the project has been archived.",
                example = "false"
        )
        @Builder.Default
        private boolean archived = false;

        @Schema(
                name = "documentation",
                description = "Whether the project has a GitHub Wiki to pull documentation from.",
                example = "true"
        )
        @Builder.Default
        private boolean documentation = false;

        @Schema(
                name = "hidden",
                description = "Whether the project should be hidden.",
                example = "false"
        )
        @Builder.Default
        private boolean hidden = false;

        @Schema(
                name = "icons",
                description = "A map of icons associated with the project.",
                example = "{\"SVG\": \"huskhomes.svg\", \"PNG\": \"huskhomes.png\"}"
        )
        @Builder.Default
        private Map<IconType, String> icons = new TreeMap<>();

        @Schema(
                name = "specialProperties",
                description = "A map of other metadata properties associated with the project.",
                example = "{\"special-property\": \"special-value\"}"
        )
        @Builder.Default
        private Map<String, String> specialProperties = new TreeMap<>();

        @Schema(
                name = "images",
                description = "A list of images associated with the project."
        )
        @Builder.Default
        private List<Image> images = new ArrayList<>();

        @Schema(
                name = "IconType",
                description = "The type of icon."
        )
        enum IconType {
            @Schema(description = "A scalable vector graphic icon.")
            SVG,
            @Schema(description = "A bitmap PNG icon.")
            PNG,
        }

        @Schema(
                name = "Image",
                description = "An image associated with the project."
        )
        record Image(
                @Schema(
                        name = "url",
                        description = "The URL of the image.",
                        example = "https://example.com/image.png"
                )
                @Length(max = 511)
                @NotNull String url,

                @Schema(
                        name = "description",
                        description = "A brief alt text description of the image.",
                        example = "A screenshot of the project in action."
                )
                @Length(max = 32768)
                @NotNull String description) {

        }
    }

}
