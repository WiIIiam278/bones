package net.william278.backend.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HTTPUtils {

    public static <T> ResponseEntity<T> cachedOk(final T response, final CacheControl cache) {
        return ResponseEntity.ok().cacheControl(cache).body(response);
    }

    public static CacheControl sMaxAgePublicCache(final Duration sMaxAge) {
        return CacheControl.empty()
                .cachePublic()
                .sMaxAge(sMaxAge);
    }

    public static ContentDisposition attachmentDisposition(final Path filename) {
        return ContentDisposition.attachment().filename(filename.getFileName().toString(), StandardCharsets.UTF_8).build();
    }

    @SneakyThrows
    public static OkHttpClient createClient(@NotNull String name) {
        return new OkHttpClient().newBuilder().cache(new Cache(
                Files.createTempDirectory("%s.cache".formatted(name)).toFile(),
                10 * 1024 * 1024
        )).build();
    }

}
