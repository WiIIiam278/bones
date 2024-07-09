package net.william278.backend.database.repository;

import net.william278.backend.database.model.Channel;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ChannelRepository extends CrudRepository<Channel, Integer> {

    @NotNull
    Optional<Channel> findById(@NotNull Integer id);

    @NotNull
    Optional<Channel> findChannelByName(@NotNull String name);

}
