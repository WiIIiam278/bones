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
    public static OkHttpClient createClient() {
        return new OkHttpClient().newBuilder().followRedirects(true).build();
    }

    @SneakyThrows
    public static OkHttpClient createCachingClient(@NotNull String name) {
        return new OkHttpClient().newBuilder().followRedirects(true).cache(new Cache(
                Files.createTempDirectory("%s.cache".formatted(name)).toFile(),
                10 * 1024 * 1024
        )).build();
    }

}
