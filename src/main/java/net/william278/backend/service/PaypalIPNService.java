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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.java.Log;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.Transaction;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.database.repository.TransactionRepository;
import net.william278.backend.util.HTTPUtils;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Log
@Service
public class PaypalIPNService implements TransactionHandlerService {

    private static final String USER_AGENT = "William278-IPN-Responder";
    private static final String sandboxUrl = "https://ipnpb.sandbox.paypal.com/cgi-bin/webscr";
    private static final String liveUrl = "https://ipnpb.paypal.com/cgi-bin/webscr";

    private final boolean staging;
    private final OkHttpClient client;
    private final TransactionRepository transactions;
    private final ProjectRepository projects;

    @Getter
    private final EmailService emailService;

    @Autowired
    public PaypalIPNService(@NotNull Environment env, TransactionRepository transactionRepository, ProjectRepository projects, EmailService emailService) {
        this.staging = env.getActiveProfiles().length > 0 && env.getActiveProfiles()[0].equals("staging");
        this.client = HTTPUtils.createClient();
        this.transactions = transactionRepository;
        this.projects = projects;
        this.emailService = emailService;
    }

    public void handleNotification(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
        // Acknowledge PayPal
        response.setStatus(HttpServletResponse.SC_OK);

        // Handle the transaction
        CompletableFuture.runAsync(() -> {
            final Map<String, String[]> map = request.getParameterMap();
            final Transaction transaction = getTransaction(
                    verifyMessage(request.getParameterNames(), map),
                    map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]))
            );
            transactions.findByTransactionReference(transaction.getTransactionReference())
                    .ifPresent(found -> transaction.setId(found.getId()));
            transactions.save(transaction);
            sendTransactionEmail(transaction);
        });
    }

    @NotNull
    private Transaction getTransaction(boolean isValid, Map<String, String> params) {
        final String subject = params.getOrDefault("transaction_subject", "");
        final String marketplace = subject.startsWith("Polymart") ? "Polymart.org"
                : subject.startsWith("Purchase Resource: ") ? "SpigotMC.org" : "Unknown";
        final BigDecimal amount = new BigDecimal(params.getOrDefault("mc_gross", "0"));

        final Transaction.TransactionBuilder builder = Transaction.builder()
                .processor(Transaction.Processor.PAYPAL)
                .transactionReference(params.getOrDefault("txn_id", "Unknown"))
                .timestamp(Instant.now())
                .currency(params.getOrDefault("mc_currency", "GBP").toLowerCase(Locale.ENGLISH))
                .projectGrant(getProjectFromSubject(subject, marketplace).orElse(null))
                .marketplace(marketplace)
                .email(params.getOrDefault("payer_email", ""))
                .amount(amount)
                .refunded(amount.doubleValue() < 0.0d)
                .passedValidation(isValid);

        return builder.build();
    }

    private Optional<Project> getProjectFromSubject(@NotNull String subject, @NotNull String marketplace) {
        return switch (marketplace) {
            case "SpigotMC.org" -> projects.findById(subject.replace("Purchase Resource: ", "")
                    .split(" ")[0].toLowerCase(Locale.ENGLISH));
            case "Polymart.org" -> projects.findById(subject.split("\\|")[1].toLowerCase(Locale.ENGLISH).trim());
            default -> Optional.empty();
        };
    }

    private boolean verifyMessage(@NotNull Enumeration<String> keys, @NotNull Map<String, String[]> params) {
        final HttpUrl httpUrl = HttpUrl.parse(staging ? sandboxUrl : liveUrl);
        final HttpUrl.Builder builder = Objects.requireNonNull(httpUrl, "URL is null").newBuilder();
        builder.addQueryParameter("cmd", "_notify-validate");
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            builder.addQueryParameter(key, params.get(key)[0]);
        }

        final HttpUrl url = builder.build();
        final String query = Objects.requireNonNull(url.encodedQuery(), "Query is null");
        final Request request = new Request.Builder().url(url)
                .addHeader("User-Agent", USER_AGENT)
                .post(RequestBody.create(query.getBytes(StandardCharsets.UTF_8)))
                .build();
        log.info("Verifying IPN: %s".formatted(url));

        final String status;
        try (Response response = client.newCall(request).execute()) {
            status = Objects.requireNonNull(response.body(), "Body was null").string();
        } catch (IOException e) {
            log.severe("IOException while verifying IPN: %s".formatted(e.getMessage()));
            return false;
        }
        log.info("Response code: %s".formatted(status));
        return status.equals("VERIFIED");
    }

}
