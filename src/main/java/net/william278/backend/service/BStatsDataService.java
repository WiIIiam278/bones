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
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalDouble;

@Slf4j
@Service
public class BStatsDataService implements StatsProvider {

    private static final String ENDPOINT_URL = "https://bstats.org/api/v1/plugins/%s/charts/%s/data";

    private final OkHttpClient client = HTTPUtils.createCachingClient("bstats");
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Optional<Project.Stats> getStats(@NotNull Project project) {
        return getBStatsId(project).map(id -> {
            final Project.Stats.StatsBuilder builder = Project.Stats.builder();
            fetchAverage(ENDPOINT_URL.formatted(id, "players")).ifPresent(d -> builder.onlinePlayers((long) d));
            fetchAverage(ENDPOINT_URL.formatted(id, "servers")).ifPresent(d -> builder.onlineServers((long) d));
            return builder.build();
        });
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private Optional<String> getBStatsId(@NotNull Project project) {
        return project.getMetadata().getLinkUrlById("bstats")
                .map((url) -> {
                    final String id = url.substring(url.lastIndexOf('/') + 1);
                    if (id.endsWith("/")) {
                        return id.substring(0, id.length() - 1);
                    }
                    return id;
                });
    }

    @NotNull
    private OptionalDouble fetchAverage(@NotNull String url) {
        final Request request = new Request.Builder().cacheControl(CACHE_CONTROL).url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("Got {} fetching BStats chart from URL {}", response.code(), url);
                return OptionalDouble.empty();
            }
            final ResponseBody body = response.body();
            if (body == null) {
                log.warn("Empty response body from BStats chart URL {}", url);
                return OptionalDouble.empty();
            }

            final Long[][] chartData = mapper.readValue(body.string(), Long[][].class);
            return Arrays.stream(chartData).mapToLong(timedDataPair -> timedDataPair[1]).average();
        } catch (IOException e) {
            log.warn("Exception fetching BStats chart from URL {}", url, e);
            return OptionalDouble.empty();
        }
    }

}
