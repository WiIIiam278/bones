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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Locale;

@Schema(
        name = "Post",
        description = "A news post."
)
@Entity
@Table(name = "posts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    public static final String PATTERN = "[a-z0-9.-]+";
    public static final String NEWS_CATEGORY = "news";
    public static final String VERSION_UPDATES_CATEGORY = "changelogs";

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Schema(
            name = "slug",
            example = "news-post-dec-1-2024",
            description = "A slug for this news post."
    )
    @Pattern(regexp = PATTERN)
    @Length(min = 1, max = 255)
    @Column(unique = true)
    private String slug;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @Nullable
    @Builder.Default
    private User author = null;

    @Schema(
            name = "authorName",
            description = "The author of this post, if there is one",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @JsonSerialize
    @Nullable
    private String getAuthorName() {
        return author != null ? author.getName() : null;
    }

    @Schema(
            name = "authorAvatar",
            description = "Avatar of the author of the post",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @JsonSerialize
    @Nullable
    private String getAuthorAvatar() {
        return author != null ? author.getAvatar().toString() : null;
    }

    @Schema(
            name = "timestamp",
            description = "Timestamp of the version's release.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Schema(
            name = "category",
            description = "Category string of this post."
    )
    @Builder.Default
    public String category = NEWS_CATEGORY;

    @Schema(
            name = "imageUrl",
            description = "A URL for the main post image.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Nullable
    @Builder.Default
    public String imageUrl = null;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL)
    @Nullable
    @Builder.Default
    private Version associatedVersionUpdate = null;

    @Schema(
            name = "isVersionUpdate",
            description = "Whether this post is a version release post.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @JsonSerialize
    @NotNull
    private Boolean isVersionUpdate() {
        return associatedVersionUpdate != null;
    }

    private void setIsVersionUpdate(boolean isVersionUpdate) {
        this.associatedVersionUpdate = isVersionUpdate ? this.associatedVersionUpdate : null;
    }

    @Schema(
            name = "associatedProject",
            description = "The project associated with this post.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @SneakyThrows
    @JsonSerialize
    @Nullable
    private String getAssociatedProject() {
        return associatedVersionUpdate == null ? null : associatedVersionUpdate.getProject().getSlug();
    }

    @JsonIgnore
    @Nullable
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Column(length = Integer.MAX_VALUE)
    private String titleContent;

    @Schema(
            name = "title",
            description = "The title of this post",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @SneakyThrows
    @JsonSerialize
    @NotNull
    private String title() {
        return isVersionUpdate() ? "%s v%s".formatted(
                associatedVersionUpdate.getProject().getMetadata().getName(),
                associatedVersionUpdate.getName())
                : (titleContent != null ? titleContent : "");
    }

    private void setTitle(@NotNull String title) {
        this.titleContent = title;
    }

    @JsonIgnore
    @Nullable
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Column(length = Integer.MAX_VALUE)
    private String bodyContent;

    @Schema(
            name = "body",
            description = "The body of this post",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @SneakyThrows
    @JsonSerialize
    @NotNull
    private String body() {
        return isVersionUpdate() ? associatedVersionUpdate.getChangelog() : (bodyContent != null ? bodyContent : "");
    }

    private void setBody(@NotNull String body) {
        this.bodyContent = body;
    }

    @NotNull
    public static Post fromVersion(@NotNull Version version) {
        return Post.builder()
                .associatedVersionUpdate(version)
                .category(VERSION_UPDATES_CATEGORY)
                .slug("%s-%s".formatted(version.getProject().getSlug(), version.getName()).toLowerCase(Locale.ENGLISH))
                .build();
    }

}
