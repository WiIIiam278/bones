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
public class HangarDataService implements StatsProvider {

    private static final String ENDPOINT_URL = "https://hangar.papermc.io/api/v1/projects/%s";

    private final OkHttpClient client = HTTPUtils.createCachingClient("hangar");
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Optional<Project.Stats> getStats(@NotNull Project project) {
        return getHangarSlug(project).map(ENDPOINT_URL::formatted).flatMap(this::fetchHangar).map(
                (hangar) -> Project.Stats.builder()
                        .interactions(hangar.stats().stars())
                        .downloadCount(hangar.stats().downloads())
                        .build()
        );
    }

    private Optional<String> getHangarSlug(@NotNull Project project) {
        return project.getMetadata().getLinkUrlById("hangar")
                .map((url) -> {
                    final String id = url.substring(url.lastIndexOf('/') + 1);
                    if (id.endsWith("/")) {
                        return id.substring(0, id.length() - 1);
                    }
                    return id;
                });
    }

    private Optional<HangarResource> fetchHangar(@NotNull String url) {
        final Request request = new Request.Builder().cacheControl(CACHE_CONTROL).url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("Got {} fetching Hangar resource from URL {}", response.code(), url);
                return Optional.empty();
            }
            final ResponseBody body = response.body();
            if (body == null) {
                log.warn("Empty response body from Hangar resource URL {}", url);
                return Optional.empty();
            }
            return Optional.of(mapper.readValue(body.string(), HangarResource.class));
        } catch (IOException e) {
            log.warn("Exception fetching Hangar resource from URL {}", url, e);
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HangarResource(
            @NotNull HangarStats stats
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record HangarStats(
                int downloads,
                int stars
        ) {

        }

    }

}
