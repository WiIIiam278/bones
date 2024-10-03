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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@Schema(
        name = "Distribution",
        description = "A platform distribution of a project."
)
@Entity
@Table(name = "distributions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Distribution {

    public static final String PATTERN = "[a-z0-9._-]+";

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JsonIgnore
    private Project project;

    @Schema(
            name = "name",
            pattern = PATTERN,
            description = "The distribution name.",
            example = "fabric-1.20.1"
    )
    private String name;

    @Schema(
            name = "groupName",
            description = "The distribution group name.",
            example = "fabric"
    )
    @Builder.Default
    private String groupName = "fabric";

    @Schema(
            name = "description",
            description = "Text to specify the distribution target.",
            example = "Fabric 1.20.1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Nullable
    @Builder.Default
    private String description = null;

    @Schema(
            name = "statusLabel",
            description = "A label indicating the status of the distribution.",
            example = "Active",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Builder.Default
    private String statusLabel = "Active";

    @Schema(
            name = "archived",
            description = "Whether the distribution is archived and no longer active.",
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Builder.Default
    private Boolean archived = false;

    @SuppressWarnings("unused")
    public boolean isArchived() {
        return archived != null && archived;
    }

}
