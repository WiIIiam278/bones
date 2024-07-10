package net.william278.backend.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Configuration
public class OpenAPIConfiguration {

    @Bean
    OpenAPI openAPI(final AppConfiguration configuration) {
        final OpenAPI api = new OpenAPI();
        api.info(
                new Info()
                        .title(configuration.getApiTitle())
                        .version(configuration.getApiVersion())
        );
        final URL apiBaseUrl = configuration.getApiBaseUrl();
        if (apiBaseUrl != null) {
            api.servers(List.of(new Server().url(apiBaseUrl.toExternalForm())));
        }
        return api;
    }

    // nothing we can do, the API exposes it raw
    @Bean
    @SuppressWarnings("rawtypes")
    OpenApiCustomizer sortSchemasAlphabetically() {
        return openApi -> {
            final Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            openApi.getComponents().setSchemas(new TreeMap<>(schemas));
        };
    }

}
