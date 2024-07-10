package net.william278.backend.database.repository;


import net.william278.backend.database.model.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UsersRepository extends CrudRepository<User, String> {

    @NotNull
    Optional<User> findById(@NotNull String id);

    @Override
    void deleteById(@NotNull String integer);

}
