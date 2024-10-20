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