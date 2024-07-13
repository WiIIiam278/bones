package net.william278.backend.database.repository;


import net.william278.backend.database.model.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<User, String> {

    @NotNull
    Optional<User> findById(@NotNull String id);

    @NotNull
    Page<User> findAllByNameContainingIgnoreCase(@NotNull String name, @NotNull Pageable pageable);

}
