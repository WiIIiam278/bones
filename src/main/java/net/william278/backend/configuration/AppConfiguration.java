package net.william278.backend.configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URL;
import java.nio.file.Path;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties(prefix = "app")
@Validated
public class AppConfiguration {

    private String defaultAdminDiscordId;

    private URL apiBaseUrl;
    private URL frontendBaseUrl;

    private String apiTitle;
    private String apiVersion;
    private String apiSecret;

    private @NotNull Path storagePath;

    private String modrinthApiToken;
    private String githubApiToken;

}
