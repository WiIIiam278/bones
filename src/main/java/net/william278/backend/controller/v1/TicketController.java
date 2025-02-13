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
import net.william278.backend.service.S3Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Tags(value = @Tag(name = "Support Tickets"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class TicketController {

    private final TicketRepository tickets;
    private final UsersRepository users;
    private final S3Service s3;

    public TicketController(TicketRepository tickets, UsersRepository users, S3Service s3) {
        this.tickets = tickets;
        this.users = users;
        this.s3 = s3;
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
            description = "The user is not a staff member and is trying to access someone else's tickets.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @CrossOrigin
    public Page<Ticket> findPaginatedByUser(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The ID of the user to get tickets for.")
            @PathVariable String userId,

            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,

            @RequestParam(value = "idFilter", required = false) Long idFilter,
            @RequestParam(value = "statusFilter", required = false) Integer statusFilter
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }

        final User user = users.findById(userId).orElseThrow(UserNotFound::new);
        if (!user.equals(principal) && !principal.isStaff()) {
            throw new NoPermission();
        }

        // Filter and return
        final PageRequest pageReq = PageRequest.of(page, size);
        if (idFilter != null) {
            if (statusFilter != null) {
                return tickets.findAllByUserAndIdAndStatusOrderByIdDesc(user, idFilter, String.valueOf(statusFilter), pageReq);
            }
            return tickets.findAllByUserAndIdOrderByIdDesc(user, idFilter, pageReq);
        } else if (statusFilter != null) {
            return tickets.findAllByUserAndStatusOrderByIdDesc(user, String.valueOf(statusFilter), pageReq);
        }
        return tickets.findAllByUserOrderByIdDesc(user, pageReq);
    }

    @Operation(
            summary = "Get a paginated list of all tickets",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @GetMapping(
            value = "/v1/tickets",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "The user is not a member of staff.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @CrossOrigin
    public Page<Ticket> findPaginated(
            @AuthenticationPrincipal User principal,

            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,

            @RequestParam(value = "idFilter", required = false) Long idFilter,
            @RequestParam(value = "statusFilter", required = false) Integer statusFilter
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isStaff()) {
            throw new NoPermission();
        }

        // Filter and return
        final PageRequest pageReq = PageRequest.of(page, size);
        if (idFilter != null) {
            if (statusFilter != null) {
                return tickets.findAllByIdAndStatusOrderByIdDesc(idFilter, String.valueOf(statusFilter), pageReq);
            }
            return tickets.findAllByIdOrderByIdDesc(idFilter, pageReq);
        } else if (statusFilter != null) {
            return tickets.findAllByStatusOrderByIdDesc(String.valueOf(statusFilter), pageReq);
        }
        return tickets.findAllByOrderByIdDesc(PageRequest.of(page, size));
    }

    @Operation(
            summary = "Get a ticket",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @GetMapping(
            value = "/v1/tickets/{ticketNumber}"
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "The user isn't the ticket creator, or a staff member.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @CrossOrigin
    public Ticket getTicket(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The number of the ticket to get")
            @PathVariable String ticketNumber
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }

        final Ticket ticket = tickets.findById(ticketNumber).orElseThrow(TicketNotFound::new);
        if (!ticket.canUserAccess(principal)) {
            throw new NoPermission();
        }

        return ticket;
    }

    @Operation(
            summary = "Delete a closed ticket",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @DeleteMapping(
            value = "/v1/tickets/{ticketNumber}"
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "The ticket is not closed.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "The user isn't the ticket creator, or an admin.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @CrossOrigin
    public ResponseEntity<?> deleteTicket(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The number of the ticket to delete")
            @PathVariable String ticketNumber
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }

        final Ticket ticket = tickets.findById(ticketNumber).orElseThrow(TicketNotFound::new);
        if (!ticket.canUserAccess(principal)) {
            throw new NoPermission();
        }
        if (!ticket.getStatus().equals("2")) {
            throw new TicketNotClosed();
        }

        // Delete ticket transcript
        s3.deleteTranscript(ticket.getId());
        tickets.delete(ticket);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get the URL of a ticket transcript",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @GetMapping(
            value = "/v1/tickets/{ticketNumber}/transcript",
            produces = {MediaType.TEXT_PLAIN_VALUE}
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "The user isn't the ticket creator, or a staff member.",
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
        if (!ticket.canUserAccess(principal)) {
            throw new NoPermission();
        }

        return s3.getTranscriptUrl(ticket.getId()).orElseThrow(TicketNotFound::new);
    }


}
