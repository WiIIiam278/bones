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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(
        name = "Transaction",
        description = "A monetary transaction."
)
@Entity
@Table(name = "transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Processor processor;

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Nullable
    private String email;

    private BigDecimal amount;

    private String currency;

    private String marketplace;

    private String transactionReference;

    @Builder.Default
    private boolean passedValidation = false;

    @Builder.Default
    private boolean refunded = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @Setter(AccessLevel.NONE)
    @JsonIgnore
    @Nullable
    private Project projectGrant;

    @SuppressWarnings("unused")
    @JsonSerialize
    @JsonAlias("projectGrant")
    @Nullable
    @Schema(
            name = "projectGrant",
            description = "The project this transaction grants"
    )
    public String getProjectEmailSubs() {
        return projectGrant != null ? projectGrant.getSlug() : null;
    }

    public enum Processor {
        PAYPAL,
        STRIPE
    }

}
