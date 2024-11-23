/*
 * MIT License
 *
 * Copyright (c) 2024 William278
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.william278.backend.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.compress.utils.Lists;
import org.hibernate.validator.constraints.Length;
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static abstract class SectionContainer {

        @Schema(
                name = "sections",
                description = "Ordered sections this page contains",
                example = """
                        [
                          {
                            "order": 1,
                            "type": "hero",
                            "properties": {
                              "color1": "#12345",
                              "color2": "#42312"
                            }
                          }
                        ]"""
        )
        @Length(min = 1, max = 255)
        private @Nullable List<Section> sections = Lists.newArrayList();

        protected void sort() {
            if (sections == null) {
                return;
            }
            Collections.sort(sections);
            sections.forEach(Section::sort);
        }

    }

    @Getter
    @Setter
    @Builder
    public static class Contents extends SectionContainer {

    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Section extends SectionContainer implements Comparable<Section> {

        @Schema(
                name = "order",
                description = "Weighted order of this section in the page",
                example = "1"
        )
        private int order;

        @Schema(
                name = "type",
                description = "Type of this page",
                example = "hero"
        )
        private String type;

        @Schema(
                name = "title",
                description = "Section title, as applicable",
                example = "HuskHomes"
        )
        private @Nullable String title;

        @Schema(
                name = "body",
                description = "Section body, as applicable",
                example = "A cross-server homes plugin"
        )
        private @Nullable String body;

        @Schema(
                name = "properties",
                description = "Section properties, as applicable",
                example = """
                        {
                          "property1": "foo",
                          "property2": "bar"
                        }
                        """
        )
        private @Nullable Map<String, String> properties;

        @Override
        public int compareTo(@NotNull Page.Section o) {
            return order - o.order;
        }

    }

}
