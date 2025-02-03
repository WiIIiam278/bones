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

package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.Transaction;
import net.william278.backend.database.model.User;
import net.william278.backend.database.repository.TransactionRepository;
import net.william278.backend.exception.ErrorResponse;
import net.william278.backend.exception.NoPermission;
import net.william278.backend.exception.NotAuthenticated;
import net.william278.backend.service.PaypalNotificationService;
import net.william278.backend.service.StripeWebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@Log
@RestController
@Tags(value = @Tag(name = "Transactions"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class TransactionController {

    private final StripeWebhookService stripe;
    private final AppConfiguration config;
    private final PaypalNotificationService paypal;
    private final TransactionRepository transactions;

    @Autowired
    public TransactionController(StripeWebhookService stripe, AppConfiguration config, PaypalNotificationService paypal, TransactionRepository transactions) {
        this.stripe = stripe;
        this.config = config;
        this.paypal = paypal;
        this.transactions = transactions;
    }

    @PostMapping(
            value = "/v1/transactions/paypal/{secret}"
    )
    @CrossOrigin(
            value = "*", allowCredentials = "false"
    )
    public ResponseEntity<?> paypalNotificationWebhookEvent(
            @RequestBody PaypalNotificationService.NotificationBody notificationBody,

            @PathVariable String secret
    ) {
        if (!secret.equals(config.getPaypalWebhookSecret())) {
            log.warning("Got POST on /v1/transactions/paypal with invalid secret");
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).build();
        }
        return paypal.handleNotification(notificationBody)
                ? ResponseEntity.status(HttpServletResponse.SC_CREATED).build()
                : ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).build();
    }

    @PostMapping(
            value = "/v1/transactions/stripe/{secret}"
    )
    @CrossOrigin(
            value = "*", allowCredentials = "false"
    )
    public ResponseEntity<?> stripePostWebhookEvent(
            @RequestBody String body,

            @PathVariable String secret
    ) {
        if (!secret.equals(config.getStripePaymentWebhookSecret())) {
            log.warning("Got POST on /v1/transactions/stripe with invalid secret");
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).build();
        }
        CompletableFuture.runAsync(() -> stripe.handleWebhook(body));
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get a paginated list of all transactions",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @GetMapping(
            value = "/v1/transactions",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "The user is not an admin.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @CrossOrigin
    public Page<Transaction> findPaginated(
            @AuthenticationPrincipal User principal,

            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,

            @RequestParam(value = "emailSearch", defaultValue = "", required = false) String emailSearch
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }

        // Filter and return
        final PageRequest pageReq = PageRequest.of(page, size);
        if (emailSearch != null && !emailSearch.isBlank()) {
            return transactions.findAllByEmailContainingIgnoreCaseOrderByTimestampDesc(emailSearch, pageReq);
        }
        return transactions.findAllByOrderByTimestampDesc(pageReq);
    }

}