package net.william278.backend.service;

import net.william278.backend.controller.v1.StatsController;
import net.william278.backend.database.model.Project;
import okhttp3.CacheControl;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface StatsService {

    @NotNull
    CacheControl CACHE_CONTROL = new CacheControl.Builder().maxAge(4, TimeUnit.HOURS).build();

    Optional<StatsController.Stats> getStats(@NotNull Project project);

}
