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
import net.william278.backend.database.model.Ticket;
import net.william278.backend.database.model.User;
import net.william278.backend.database.repository.TicketRepository;
import net.william278.backend.database.repository.UsersRepository;
import net.william278.backend.exception.*;
import net.william278.backend.service.TicketTranscriptsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Tags(value = @Tag(name = "Support Tickets"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class TicketController {

    private final TicketRepository tickets;
    private final UsersRepository users;
    private final TicketTranscriptsService transcripts;

    public TicketController(TicketRepository tickets, UsersRepository users, TicketTranscriptsService transcripts) {
        this.tickets = tickets;
        this.users = users;
        this.transcripts = transcripts;
    }

    @Operation(
            summary = "Get a paginated list of the logged-in user's support tickets.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @GetMapping(
            value = "/v1/users/@me/tickets",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @CrossOrigin
    public Page<Ticket> findPaginated(
            @AuthenticationPrincipal User principal,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        return tickets.findAllByUserOrderByOpenDate(principal, PageRequest.of(page, size));
    }

    @Operation(
            summary = "Get a paginated list of a user by ID's support tickets.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @GetMapping(
            value = "/v1/users/{userId}/tickets",
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
    public Page<Ticket> findPaginatedForUserById(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The ID of the user to get tickets for.")
            @PathVariable String userId,

            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isStaff()) {
            throw new NoPermission();
        }
        final User user = users.findById(userId).orElseThrow(UserNotFound::new);
        return tickets.findAllByUserOrderByOpenDate(user, PageRequest.of(page, size));
    }

    @Operation(
            summary = "Get the URL of a ticket transcript",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @GetMapping(
            value = "/v1/tickets/{ticketNumber}",
            produces = {MediaType.TEXT_PLAIN_VALUE}
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "The user does not have access to that ticket.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @CrossOrigin
    public String getTranscriptUrl(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The number of the ticket to get a transcript for")
            @PathVariable String ticketNumber
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }

        final Ticket ticket = tickets.findById(ticketNumber).orElseThrow(TicketNotFound::new);
        if (!(ticket.getUser() != null && ticket.getUser().equals(principal)) || !principal.isStaff()) {
            throw new NoPermission();
        }

        return transcripts.getTranscriptUrl(ticket.getId()).orElseThrow(TicketNotFound::new);
    }


}
