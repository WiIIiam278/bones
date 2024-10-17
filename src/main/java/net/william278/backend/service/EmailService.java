package net.william278.backend.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Resources;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.User;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final Cache<String, String> verificationCodes;
    private final AppConfiguration config;

    private final SendGrid sendGrid;
    private final Email sendFromEmail;
    private final Email replyToEmail;

    @Autowired
    public EmailService(@NotNull AppConfiguration config) {
        this.verificationCodes = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
        this.config = config;

        this.sendGrid = new SendGrid(config.getSendgridApiKey());
        this.sendFromEmail = new Email(config.getSendEmailFrom(), "William278.net");
        this.replyToEmail = new Email(config.getSendEmailReplyTo(), "William278.net");
    }

    public void sendVerificationCodeEmail(@NotNull User user) {
        final Map<String, String> map = verificationCodes.asMap();
        if (map.containsValue(user.getId())) {
            verificationCodes.invalidate(map.get(user.getId()));
            log.info("Replacing old verification code with new verification code for {}", user.getId());
        }

        final String code = createSecureCode();
        verificationCodes.put(code, user.getId());
        this.sendEmail(createVerificationEmail(user, code));
        log.info("Sending verification code {} to {}", user.getId(), user.getEmail());
    }

    private void sendEmail(@NotNull Mail mail) {
        final Request request = new Request();
        final Response response;
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            response = sendGrid.api(request);
        } catch (IOException e) {
            log.error("Failed to send verification email", e);
            return;
        }
        log.info("Sent email, got {}", response.getStatusCode());
    }

    public boolean verifyEmail(@NotNull String code, @NotNull String userId) {
        final Map<String, String> map = verificationCodes.asMap();
        if (map.containsKey(code) && map.get(code).equals(userId)) {
            log.info("Verified mail verification code {} for user {}", code, userId);
            verificationCodes.invalidate(code);
            return true;
        }
        log.warn("Invalid mail verification code {} for user {}", code, userId);
        return false;
    }

    @NotNull
    private String createSecureCode() {
        return new BigInteger(30, new SecureRandom()).toString(32).toUpperCase();
    }

    @NotNull
    private Mail createVerificationEmail(@NotNull User user, @NotNull String code) {
        final String codeUrl = "%s/v1/users/%s/email/%s".formatted(
                config.getApiBaseUrl(), user.getId(), code
        );
        final Mail message = new Mail(
                sendFromEmail,
                "Verify your email address for William278.net",
                new Email(user.getEmail(), user.getName()),
                new Content("text/html", MailTemplate.VERIFY_EMAIL.getTemplate(Map.of(
                        "%%_USERNAME_%%", user.getName(),
                        "%%_VERIFY_URL_%%", codeUrl,
                        "%%_VERIFY_CODE_%%", code
                )))
        );
        message.setReplyTo(replyToEmail);
        return message;
    }

    @AllArgsConstructor
    public enum MailTemplate {
        VERIFY_EMAIL("verify-email.html");

        private final String file;

        @SneakyThrows
        public String getTemplate(@NotNull Map<String, String> placeholders) {
            String template = Resources.toString(
                    Resources.getResource("emails/out/%s".formatted(file)),
                    StandardCharsets.UTF_8
            );
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                template = template.replace(entry.getKey(), entry.getValue());
            }
            return template;
        }
    }

}
