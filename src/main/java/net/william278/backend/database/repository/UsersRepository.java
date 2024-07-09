package net.william278.backend.database.repository;


import net.william278.backend.database.model.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UsersRepository extends CrudRepository<User, Integer> {

    @NotNull
    Optional<User> findById(@NotNull Integer id);

    @Override
    void deleteById(@NotNull Integer integer);

}
