package net.william278.backend.service;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import net.william278.backend.database.model.Transaction;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.database.repository.TransactionRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

@Log
@Service
public class StripeWebhookService implements TransactionHandlerService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final TransactionRepository transactions;

    @Getter
    private final EmailService emailService;
    private final ProjectRepository projects;

    public StripeWebhookService(@NotNull TransactionRepository transactions, @NotNull EmailService emailService,
                                @NotNull ProjectRepository projects) {
        this.transactions = transactions;
        this.emailService = emailService;
        this.projects = projects;
    }

    public void handleWebhook(@NotNull String body) {
        try {
            final StripeBody data = mapper.readValue(body, StripeBody.class);
            final Transaction transaction = data.toTransaction(projects, mapper);
            transactions.findByTransactionReference(transaction.getTransactionReference())
                    .ifPresent(found -> transaction.setId(found.getId()));
            transactions.save(transaction);
            sendTransactionEmail(transaction);
        } catch (JsonProcessingException e) {
            log.warning("Got POST on /v1/transactions/stripe with invalid body: " + e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StripeBody(
            StripeData data
    ) {

        @NotNull
        public Transaction toTransaction(@NotNull ProjectRepository projects, @NotNull ObjectMapper mapper) {
            final StripeData.StripeCharge charge = data().object();
            final Transaction.TransactionBuilder builder = Transaction.builder();
            final BigDecimal amount = BigDecimal.valueOf((charge.amount() - charge.amountRefunded()) / 100d);
            return builder.processor(Transaction.Processor.STRIPE)
                    .transactionReference(charge.id())
                    .currency(charge.currency().toUpperCase(Locale.ENGLISH))
                    .amount(amount).refunded(amount.compareTo(BigDecimal.ZERO) > 0)
                    .email(charge.getEmail(mapper))
                    .marketplace(charge.description().split("\\|")[0].trim())
                    .projectGrant(projects.findById(charge.description().split("\\|")[1]
                            .trim().toLowerCase(Locale.ENGLISH)).orElse(null))
                    .passedValidation(true) // todo
                    .build();
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record StripeData(
                @NotNull StripeCharge object
        ) {
            @JsonIgnoreProperties(ignoreUnknown = true)
            record StripeCharge(
                    @NotNull String id,
                    int amount,
                    @JsonAlias("amount_refunded")
                    int amountRefunded,
                    @NotNull String currency,
                    @NotNull String description,
                    @NotNull Map<String, String> metadata
            ) {

                @JsonIgnoreProperties(ignoreUnknown = true)
                record BillingMetadata(
                        @Nullable String email
                ) {
                }

                @Nullable
                @SneakyThrows
                public String getEmail(@NotNull ObjectMapper mapper) {
                    return mapper.readValue(
                            metadata.getOrDefault("Billing Details", "{}"),
                            BillingMetadata.class
                    ).email();
                }

            }
        }
    }

}