package net.william278.backend.database.repository;

import net.william278.backend.database.model.Project;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends CrudRepository<Project, String> {

    @NotNull
    Optional<Project> findById(@NotNull String id);

    @NotNull
    List<Project> findAll();

}
