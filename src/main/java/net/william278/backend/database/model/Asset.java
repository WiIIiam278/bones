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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

@Schema(
        name = "Asset",
        description = "A web asset."
)
@Entity
@Table(name = "assets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    public static final String PATTERN = "[a-z0-9\\\\+._-]+";

    @Schema(
            name = "name",
            pattern = PATTERN,
            example = "example-asset.png",
            description = "The asset's file name."
    )
    @Id
    @Length(min = 1, max = 255)
    private String name;

    @Schema(
            name = "contentType",
            description = "Content type of the asset."
    )
    @Length(max = 255)
    private String contentType;

    @Schema(
            name = "fileSize",
            description = "The size of the file in bytes.",
            example = "1024"
    )
    private long fileSize;

    @Schema(
            name = "createdBy",
            description = "User who created or last updated the asset."
    )
    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @Nullable
    @Builder.Default
    private User createdBy = null;

    @Schema(
            name = "createdAt",
            description = "When the asset was created or last updated",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Instant createdAt = Instant.now();

}
