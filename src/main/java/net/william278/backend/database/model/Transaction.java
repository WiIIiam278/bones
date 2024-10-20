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
