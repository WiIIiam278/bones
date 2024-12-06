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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.jetbrains.annotations.NotNull;

@Schema(
        name = "Channel",
        description = "A release channel for versions of a project."
)
@Entity
@Table(name = "channels")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Channel {

    public static final String DEFAULT_RELEASES_CHANNEL = "release";
    public static final String PATTERN = "[a-z0-9._-]+";

    @Id
    @Schema(
            name = "name",
            pattern = PATTERN,
            example = DEFAULT_RELEASES_CHANNEL,
            description = "The lowercase channel name."
    )
    private String name;

    @Schema(
            name = "emailNotifications",
            example = "true",
            description = "Whether email subscribers should be notified when releases are uploaded to this channel"
    )
    @Builder.Default
    private boolean emailNotifications = false;

    @Schema(
            name = "createPosts",
            example = "true",
            description = "Whether posts should be created when a version is published to this channel"
    )
    @Builder.Default
    private boolean createPosts = false;

    public Channel(@NotNull String name) {
        this.name = name;
        this.emailNotifications = name.equals(DEFAULT_RELEASES_CHANNEL);
        this.createPosts = name.equals(DEFAULT_RELEASES_CHANNEL);
    }

}
