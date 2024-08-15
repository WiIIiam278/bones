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
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.validator.constraints.Length;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

@Schema(
        name = "Ticket",
        description = "A support ticket."
)
@Entity
@Table(name = "tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Schema(
            name = "id",
            description = "The ticket ID."
    )
    @Id
    private Long id;

    @Schema(
            name = "client",
            description = "The user who created the ticket"
    )
    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn
    @Nullable
    private User user;

    @Schema(
            name = "guildId",
            description = "Snowflake ID of the guild."
    )
    @Length(max = 22)
    private String guildId;

    @Schema(
            name = "channel",
            description = "Snowflake ID of the ticket channel."
    )
    @Length(max = 22)
    private String channelId;

    @Schema(
            name = "status",
            description = "Status code of the ticket.",
            example = "active"
    )
    @Length(max = 10)
    private String status;

    @Schema(
            name = "firstMessage",
            description = "Snowflake ID of the first ticket message."
    )
    @Length(max = 22)
    private String firstMessage;

    @Schema(
            name = "subject",
            description = "Short subject of the ticket."
    )
    @Length(max = 32)
    private String subject;

    @Schema(
            name = "description",
            description = "Details about the ticket case."
    )
    @Length(max = 512)
    private String description;

    @Schema(
            name = "openDate",
            description = "When the user created this ticket."
    )
    @Builder.Default
    private Instant openDate = Instant.now();

    @Schema(
            name = "closeDate",
            description = "When the user closed this ticket."
    )
    @Nullable
    private Instant closeDate;

}
