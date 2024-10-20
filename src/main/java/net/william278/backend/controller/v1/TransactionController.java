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