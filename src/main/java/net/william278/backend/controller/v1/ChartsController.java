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
import net.william278.backend.database.model.Asset;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.User;
import net.william278.backend.database.repository.AssetsRepository;
import net.william278.backend.exception.ErrorResponse;
import net.william278.backend.exception.InvalidRole;
import net.william278.backend.exception.NoPermission;
import net.william278.backend.exception.NotAuthenticated;
import net.william278.backend.service.ChartService;
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
@Tags(value = @Tag(name = "Charts"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ChartsController {

    private final ChartService charts;

    @Autowired
    public ChartsController(ChartService charts) {
        this.charts = charts;
    }

    @Operation(
            summary = "Get a chart for transactions.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @GetMapping(
            value = "/v1/charts/transactions",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "The user is not an admin.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @CrossOrigin
    public ChartService.Chart findPaginated(
            @AuthenticationPrincipal User principal,
            @RequestParam(value = "pastDays", defaultValue = "30") int pastDays,
            @RequestParam(value = "dayGrouping", defaultValue = "7") int dayGrouping
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        return charts.getTransactionsLineChart(pastDays, dayGrouping);
    }

}
