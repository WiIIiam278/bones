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

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.service.PaypalIPNService;
import net.william278.backend.service.StripeWebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@Log
@RestController
@Tags(value = @Tag(name = "Transactions"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class TransactionController {

    private final PaypalIPNService paypal;
    private final StripeWebhookService stripe;
    private final AppConfiguration config;

    @Autowired
    public TransactionController(PaypalIPNService paypal, StripeWebhookService stripe, AppConfiguration config) {
        this.paypal = paypal;
        this.stripe = stripe;
        this.config = config;
    }

    @PostMapping(
            value = "/v1/transactions/paypal/{secret}"
    )
    @CrossOrigin(
            value = "*", allowCredentials = "false"
    )
    public void paypalPostIpnEvent(
            HttpServletRequest request,
            HttpServletResponse response,

            @PathVariable String secret
    ) {
        if (!secret.equals(config.getPaypalIpnWebhookSecret())) {
            log.warning("Got POST on /v1/transactions/paypal with invalid secret");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        this.paypal.handleNotification(request, response);
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

}