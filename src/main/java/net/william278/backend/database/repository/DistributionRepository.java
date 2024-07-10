package net.william278.backend.database.repository;


import net.william278.backend.database.model.Distribution;
import net.william278.backend.database.model.Project;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface DistributionRepository extends CrudRepository<Distribution, Integer> {

    @NotNull
    Optional<Distribution> findById(@NotNull Integer id);

    @NotNull
    Optional<Distribution> findDistributionByName(@NotNull String name);

    @NotNull
    List<Distribution> findDistributionsByProject(@NotNull Project project);

}
