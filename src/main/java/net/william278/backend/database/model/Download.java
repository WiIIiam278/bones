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

@Schema(
        name = "Download",
        description = "A downloadable file, associated with a version."
)
@Entity
@Table(name = "downloads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Download {

    public static final String PATTERN = "[a-zA-Z0-9._-\\\\+]+\\.[a-zA-Z0-9._-]+";

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Schema(
            name = "distribution",
            description = "The distribution this download is associated with."
    )
    @ManyToOne(cascade = CascadeType.ALL)
    private Distribution distribution;

    @Schema(
            name = "name",
            pattern = PATTERN,
            example = "HuskHomes-Paper-4.7.jar"
    )
    @Column(length = 4096)
    private String name;

    @Setter
    @Schema(
            name = "md5",
            description = "The MD5 checksum of the file.",
            pattern = "[a-f0-9]{32}",
            example = "d41d8cd98f00b204e9800998ecf8427e"
    )
    @Column(length = 32)
    private String md5;

    @Schema(
            name = "fileSize",
            description = "The size of the file in bytes.",
            example = "1024"
    )
    private long fileSize;

}
