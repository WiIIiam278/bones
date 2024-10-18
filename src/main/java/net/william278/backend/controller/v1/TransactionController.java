package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.service.PaypalIPNService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Log
@RestController
@Tags(value = @Tag(name = "Transactions"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class TransactionController {

    private final PaypalIPNService paypal;
    private final AppConfiguration config;

    @Autowired
    public TransactionController(PaypalIPNService paypal, AppConfiguration config) {
        this.paypal = paypal;
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

}