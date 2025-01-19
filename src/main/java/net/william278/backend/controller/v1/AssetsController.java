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

package net.william278.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.validation.constraints.Pattern;
import net.william278.backend.database.model.*;
import net.william278.backend.database.repository.AssetsRepository;
import net.william278.backend.exception.ErrorResponse;
import net.william278.backend.exception.InvalidRole;
import net.william278.backend.exception.NoPermission;
import net.william278.backend.exception.NotAuthenticated;
import net.william278.backend.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@RestController
@Tags(value = @Tag(name = "Assets"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class AssetsController {

    private final AssetsRepository assets;
    private final S3Service s3;

    @Autowired
    public AssetsController(AssetsRepository assets, S3Service s3) {
        this.assets = assets;
        this.s3 = s3;
    }

    @Operation(
            summary = "Get a paginated list of all uploaded assets.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @GetMapping(
            value = "/v1/assets",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "The user is not a staff member.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @CrossOrigin
    public Page<Asset> findPaginated(
            @AuthenticationPrincipal User principal,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            @RequestParam(value = "fileNameSearch", defaultValue = "") String fileNameSearch
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isStaff()) {
            throw new NoPermission();
        }
        if (fileNameSearch != null && !fileNameSearch.isBlank()) {
            return assets.findAllByNameContainingIgnoreCaseOrderByCreatedAtAsc(fileNameSearch, PageRequest.of(page, size));
        }
        return assets.findAllByOrderByCreatedAtAsc(PageRequest.of(page, size));
    }

    @Operation(
            summary = "Upload or replace an asset.",
            security = {
                    @SecurityRequirement(name = "OAuth2")
            }
    )
    @ApiResponse(
            responseCode = "200",
            description = "The asset was created or updated successfully."
    )
    @ApiResponse(
            responseCode = "401",
            description = "Not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "Not authorized to create or upload assets.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @PutMapping(
            value = "/v1/assets/{fileName:" + Asset.PATTERN + "}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Asset putAsset(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The file name of the asset.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String fileName,

            @RequestParam("file")
            MultipartFile file
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isStaff()) {
            throw new NoPermission();
        }

        final Asset asset = assets.findByName(fileName).orElse(Asset.builder().name(fileName).build());
        asset.setCreatedBy(principal);
        asset.setFileSize(file.getSize());
        asset.setCreatedAt(Instant.now());
        asset.setContentType(file.getContentType());

        s3.uploadAsset(file, asset);
        return asset;
    }

    @Operation(
            summary = "Delete an asset.",
            security = {
                    @SecurityRequirement(name = "OAuth2")
            }
    )
    @ApiResponse(
            responseCode = "200",
            description = "The asset was deleted successfully."
    )
    @ApiResponse(
            responseCode = "401",
            description = "Not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "Not authorized to delete assets.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @DeleteMapping(
            value = "/v1/assets/{fileName:" + Asset.PATTERN + "}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Asset deleteAsset(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The file name of the asset.")
            @Pattern(regexp = Project.PATTERN)
            @PathVariable String fileName
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isStaff()) {
            throw new NoPermission();
        }

        final Asset asset = assets.findByName(fileName).orElseThrow(InvalidRole::new);
        s3.deleteAsset(asset);
        return asset;
    }

}
