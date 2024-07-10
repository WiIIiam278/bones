package net.william278.backend.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;

import java.util.Objects;

public class OAuthUtils {

    public static final String DISCORD_USER_AGENT = "William278 (https://github.com/WiIIiam278/william278-backend)";

    @NotNull
    public static RequestEntity<?> withUserAgent(@Nullable RequestEntity<?> request) {
        Objects.requireNonNull(request, "RequestEntity must not be null");

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(request.getHeaders());
        headers.add(HttpHeaders.USER_AGENT, DISCORD_USER_AGENT);

        return new RequestEntity<>(request.getBody(), headers, request.getMethod(), request.getUrl());
    }

}
