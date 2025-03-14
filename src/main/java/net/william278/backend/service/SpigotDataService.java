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

package net.william278.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.william278.backend.database.model.Project;
import net.william278.backend.util.HTTPUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Service
public class SpigotDataService implements StatsProvider {

    private static final String ENDPOINT_URL = "https://api.spiget.org/v2/resources/%s";

    private final OkHttpClient client = HTTPUtils.createCachingClient("spigot");
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Optional<Project.Stats> getStats(@NotNull Project project) {
        return getSpigotId(project).map(ENDPOINT_URL::formatted).flatMap(this::fetchSpigot).map(
                (spigot) -> Project.Stats.builder()
                        .downloadCount(spigot.downloads())
                        .interactions(spigot.likes())
                        .averageRating(spigot.rating().average())
                        .numberOfRatings(spigot.rating().count())
                        .build()
        );
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private Optional<String> getSpigotId(@NotNull Project project) {
        return project.getMetadata().getLinkUrlById("spigot")
                .map((url) -> {
                    final String id = url.substring(url.lastIndexOf('.') + 1);
                    if (id.endsWith("/")) {
                        return id.substring(0, id.length() - 1);
                    }
                    return id;
                });
    }

    private Optional<SpigotResource> fetchSpigot(@NotNull String url) {
        final Request request = new Request.Builder().cacheControl(CACHE_CONTROL).url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("Got {} fetching Spigot resource from URL {}", response.code(), url);
                return Optional.empty();
            }
            final ResponseBody body = response.body();
            if (body == null) {
                log.warn("Empty response body from Spigot resource URL {}", url);
                return Optional.empty();
            }
            // read value to SpigotResource, ignoring any unknown properties
            return Optional.of(mapper.readValue(body.string(), SpigotResource.class));
        } catch (IOException e) {
            log.warn("Exception fetching Spigot resource from URL {}", url, e);
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpigotResource(
            int likes,
            int downloads,
            @NotNull SpigotRating rating
    ) {
        record SpigotRating(
                int count,
                double average
        ) {
        }
    }

}
