package net.william278.backend.database.repository;

import net.william278.backend.database.model.Channel;
import net.william278.backend.database.model.Distribution;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.Version;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface VersionRepository extends CrudRepository<Version, String> {

    @NotNull
    Optional<Version> findById(@NotNull Integer id);

    @NotNull
    Optional<Version> findByProjectAndChannelAndName(@NotNull Project project, @NotNull Channel channel,
                                                     @NotNull String version);

    @NotNull
    List<Version> getAllByProject(@NotNull Project project);

    @NotNull
    List<Version> getAllByProjectAndChannel(@NotNull Project project, @NotNull Channel channel);

    @NotNull
    List<Version> getAllByProjectAndChannelAndDistributionsIsContaining(@NotNull Project project,
                                                                        @NotNull Channel channel,
                                                                        @NotNull Distribution distribution);

    @NotNull
    Optional<Version> getTopByProjectAndChannelOrderByTimestamp(@NotNull Project project, @NotNull Channel channel);

}
