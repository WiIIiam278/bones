package net.william278.backend.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Schema(
        name = "Page",
        description = "A webpage for a project."
)
@Entity
@Table(name = "pages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Page {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JsonIgnore
    private Project project;

    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Column(length = Integer.MAX_VALUE)
    private String contents;

    @Schema(
            name = "contents",
            description = "The JSON contents of the page."
    )
    @JsonSerialize
    @SneakyThrows
    @NotNull
    public Page.Contents getContents() {
        final Contents deserialized = new ObjectMapper().readValue(contents, Page.Contents.class);
        deserialized.sort();
        return deserialized;
    }

    @SneakyThrows
    @SuppressWarnings("unused")
    public void setContents(@NotNull Page.Contents contents) {
        this.contents = new ObjectMapper().writeValueAsString(contents);
    }


    @AllArgsConstructor
    @NoArgsConstructor
    public static class Contents {

        private List<Section> sections = Lists.newArrayList();

        protected void sort() {
            Collections.sort(sections);
            sections.forEach(Section::sort);
        }

    }

    @AllArgsConstructor
    @NoArgsConstructor
    public static class Section implements Comparable<Section> {

        private int order;
        private String type;
        private @Nullable String title;
        private @Nullable String body;
        private @Nullable Map<String, String> properties;
        private @Nullable List<Section> sections;

        protected void sort() {
            if (sections == null) {
                return;
            }
            Collections.sort(sections);
            sections.forEach(Section::sort);
        }

        @Override
        public int compareTo(@NotNull Page.Section o) {
            return order - o.order;
        }

    }

}
