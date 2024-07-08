package net.william278.backend.database.repository;

import net.william278.backend.database.model.Channel;
import net.william278.backend.database.model.Project;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ChannelRepository extends CrudRepository<Channel, String> {

    @NotNull
    Optional<Channel> findById(@NotNull String id);

}
