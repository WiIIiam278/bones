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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.io.BigDecimalParser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.Transaction;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.database.repository.TransactionRepository;
import org.hibernate.validator.constraints.Length;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class PaypalNotificationService implements TransactionHandlerService {

    private final ProjectRepository projects;
    private final @Getter EmailService emailService;
    private final TransactionRepository transactions;

    public boolean handleNotification(@NotNull PaypalNotificationService.NotificationBody notificationBody) {
        final Transaction transaction = notificationBody.toTransaction(projects);
        if (transaction == null) {
            return false;
        }
        transactions.findByTransactionReference(transaction.getTransactionReference())
                .ifPresent(found -> transaction.setId(found.getId()));
        transactions.save(transaction);
        sendTransactionEmail(transaction);
        return true;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NotificationBody(
            @NotNull @Length(max = 17, min = 17) String id,
            @NotNull @Email String email,
            @NotNull String amount,
            @NotNull String currency,
            @NotNull String marketplace,
            @NotNull String projectSlug
    ) {

        @Nullable
        public Transaction toTransaction(ProjectRepository projects) {
            final BigDecimal decimalAmount = BigDecimalParser.parse(amount);
            return Transaction.builder()
                    .processor(Transaction.Processor.PAYPAL)
                    .transactionReference(id)
                    .marketplace(marketplace)
                    .email(email)
                    .timestamp(Instant.now())
                    .projectGrant(getProjectGrant(projects, projectSlug).orElse(null))
                    .refunded(false)
                    .currency(getValidCurrency(currency).toUpperCase(Locale.ENGLISH))
                    .amount(decimalAmount).refunded(decimalAmount.doubleValue() < 0.0d)
                    .passedValidation(true)
                    .build();
        }

        private static Optional<Project> getProjectGrant(ProjectRepository projects, String projectSlug) {
            return projects.findById(projectSlug)
                    .flatMap(project -> project.isRestricted() ? Optional.of(project) : Optional.empty());
        }

        @NotNull
        private static String getValidCurrency(String currency) {
            try {
                final Currency parsed = Currency.getInstance(currency);
                return parsed.getCurrencyCode();
            } catch (IllegalArgumentException ignored) {
                return switch (currency) {
                    case "$" -> "USD";
                    case "€" -> "EUR";
                    default -> "GBP";
                };
            }
        }
    }

}
