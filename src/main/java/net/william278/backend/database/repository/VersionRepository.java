package net.william278.backend.database.repository;

import net.william278.backend.database.model.Channel;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.Version;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VersionRepository extends JpaRepository<Version, Integer> {

    @NotNull
    Optional<Version> findById(@NotNull Integer id);

    @NotNull
    Optional<Version> findByProjectAndChannelAndName(@NotNull Project project, @NotNull Channel channel,
                                                     @NotNull String version);

    @NotNull
    List<Version> getAllByProject(@NotNull Project project);

    @NotNull
    Page<Version> getAllByProjectAndChannelOrderByTimestamp(@NotNull Project project, @NotNull Channel channel,
                                                            @NotNull PageRequest pageRequest);

    @NotNull
    Optional<Version> getTopByProjectAndChannelOrderByTimestamp(@NotNull Project project, @NotNull Channel channel);

}
