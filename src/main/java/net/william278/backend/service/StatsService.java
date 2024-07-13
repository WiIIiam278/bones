package net.william278.backend.service;

import lombok.SneakyThrows;
import net.william278.backend.controller.v1.StatsController;
import net.william278.backend.database.model.Project;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface StatsService {

    @NotNull
    CacheControl CACHE_CONTROL = new CacheControl.Builder().maxAge(4, TimeUnit.HOURS).build();

    Optional<StatsController.Stats> getStats(@NotNull Project project);

    @SneakyThrows
    default OkHttpClient createClient(@NotNull String name) {
        return new OkHttpClient().newBuilder().cache(new Cache(
                Files.createTempDirectory("%s.cache".formatted(name)).toFile(),
                10 * 1024 * 1024
        )).build();
    }

}
