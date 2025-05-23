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

import net.william278.backend.database.model.Asset;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetsRepository extends JpaRepository<Asset, String> {

    @NotNull
    Optional<Asset> findByName(@NotNull String name);

    @NotNull
    Page<Asset> findAllByOrderByCreatedAtDesc(@NotNull Pageable pageable);

    @NotNull
    Page<Asset> findAllByContentTypeContainingIgnoreCaseOrderByCreatedAtDesc(@NotNull String contentType, @NotNull Pageable pageable);

    @NotNull
    Page<Asset> findAllByNameContainingIgnoreCaseOrderByCreatedAtDesc(@NotNull String name, @NotNull Pageable pageable);

    @NotNull
    Page<Asset> findAllByContentTypeContainingIgnoreCaseAndNameContainingIgnoreCaseOrderByCreatedAtDesc(@NotNull String contentType,
                                                                                    @NotNull String name,
                                                                                    @NotNull Pageable pageable);

}
