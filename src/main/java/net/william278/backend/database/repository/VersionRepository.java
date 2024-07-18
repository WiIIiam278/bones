/*
 * MIT License
 *
 * Copyright (c) 2024 William278
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.william278.backend.database.repository;

import net.william278.backend.database.model.Channel;
import net.william278.backend.database.model.Distribution;
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
    Page<Version> getAllByProjectAndChannelOrderByTimestampDesc(@NotNull Project project, @NotNull Channel channel,
                                                                @NotNull PageRequest pageRequest);

    @NotNull
    Page<Version> getAllByProjectAndChannelAndDownloadsDistributionOrderByTimestampDesc(@NotNull Project project,
                                                                                        @NotNull Channel channel,
                                                                                        @NotNull Distribution distribution,
                                                                                        @NotNull PageRequest pageRequest);

    @NotNull
    Optional<Version> getTopByProjectAndChannelOrderByTimestampDesc(@NotNull Project project, @NotNull Channel channel);

}
