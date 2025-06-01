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
import jakarta.validation.constraints.Email;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.User;
import net.william278.backend.database.repository.ProjectRepository;
import net.william278.backend.database.repository.UsersRepository;
import net.william278.backend.exception.*;
import net.william278.backend.service.EmailService;
import net.william278.backend.service.TransactionGrantsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.MessageDigest;
import java.util.*;

@RestController
@Tags(value = @Tag(name = "Users"))
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private final UsersRepository users;
    private final ProjectRepository projects;
    private final EmailService emailService;
    private final AppConfiguration config;
    private final TransactionGrantsService transactionGrants;

    @Autowired
    public UserController(UsersRepository users, ProjectRepository projects, EmailService emailService, AppConfiguration config, TransactionGrantsService transactionGrants) {
        this.users = users;
        this.projects = projects;
        this.emailService = emailService;
        this.config = config;
        this.transactionGrants = transactionGrants;
    }

    @Operation(
            summary = "Get a paginated list of all users.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @GetMapping(
            value = "/v1/users",
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
    public Page<User> findPaginated(
            @AuthenticationPrincipal User principal,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            @RequestParam(value = "nameSearch", defaultValue = "") String nameSearch
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isStaff()) {
            throw new NoPermission();
        }
        if (nameSearch != null && !nameSearch.isBlank()) {
            return users.findAllByNameContainingIgnoreCaseOrderByCreatedAtAsc(nameSearch, PageRequest.of(page, size));
        }
        return users.findAllByOrderByCreatedAtAsc(PageRequest.of(page, size));
    }

    @Operation(
            summary = "Get a specific user by their ID.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The user"
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
    @ApiResponse(
            responseCode = "404",
            description = "The user was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping(
            value = "/v1/users/{userId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    public User getUser(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The ID of the user to get.")
            @PathVariable String userId
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isStaff()) {
            throw new NoPermission();
        }
        return users.findById(userId).orElseThrow(UserNotFound::new);
    }

    @Operation(
            summary = "Get a specific user by their ID with an API key.",
            security = @SecurityRequirement(name = "APIKey")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The user"
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
    @ApiResponse(
            responseCode = "404",
            description = "The user was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping(
            value = "/v1/users/{userId}/api",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin(
            origins = "*", allowCredentials = "false",
            allowedHeaders = {"X-Api-Key", "Content-Type", "Accept"}
    )
    public User getUser(
            @RequestHeader("X-Api-Key") String apiKey,

            @Parameter(description = "The ID of the user to get.")
            @PathVariable String userId
    ) {
        if (config.getApiSecret() == null) {
            throw new IllegalStateException("API key is not set on server.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new NotAuthenticated();
        }
        if (!MessageDigest.isEqual(Utf8.encode(config.getApiSecret()), Utf8.encode(apiKey))) {
            throw new NoPermission();
        }
        return users.findById(userId).orElseThrow(UserNotFound::new);
    }

    @Operation(
            summary = "Delete a specific user by their ID.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The user that was deleted."
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
    @ApiResponse(
            responseCode = "404",
            description = "The user was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @DeleteMapping(
            value = "/v1/users/{userId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public User deleteUser(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The ID of the user to delete.")
            @PathVariable String userId
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }
        final User user = users.findById(userId).orElseThrow(UserNotFound::new);
        if (principal.getId().equals(user.getId())) {
            throw new NoPermission();
        }
        if (user.isAdmin()) {
            throw new NoPermission();
        }
        users.deleteById(userId);
        return user;
    }

    @Operation(
            summary = "Set the projects a user has purchased.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The user with their updated list of purchased projects."
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "The user is not a staff member or admin.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "The user was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @PutMapping(
            value = "/v1/users/{userId}/purchases",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    public User setUserPurchases(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The ID of the user to get.")
            @PathVariable String userId,

            @RequestBody List<String> projects
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isStaff()) {
            throw new NoPermission();
        }
        final User user = users.findById(userId).orElseThrow(UserNotFound::new);

        // Get the projects from the project repository
        final Set<Project> updatedProjects = new HashSet<>();
        this.projects.findAllById(projects).forEach(updatedProjects::add);
        if (user.setPurchases(updatedProjects)) {
            return users.save(user);
        }
        return user;
    }

    @Operation(
            summary = "Get the projects a user has purchased with an API key, as a map of purchases to Discord role IDs.",
            security = @SecurityRequirement(name = "APIKey")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The user with their updated list of purchased projects."
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "403",
            description = "The user is not a staff member or admin.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "The user was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping(
            value = "/v1/users/{userId}/purchases/api",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin(
            origins = "*", allowCredentials = "false",
            allowedHeaders = {"X-Api-Key", "Content-Type", "Accept"}
    )
    public Map<String, String> getUserPurchases(
            @RequestHeader("X-Api-Key") String apiKey,

            @Parameter(description = "The ID of the user to get.")
            @PathVariable String userId
    ) {
        if (config.getApiSecret() == null) {
            throw new IllegalStateException("API key is not set on server.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new NotAuthenticated();
        }
        if (!MessageDigest.isEqual(Utf8.encode(config.getApiSecret()), Utf8.encode(apiKey))) {
            throw new NoPermission();
        }

        // Refresh purchases
        final User user = transactionGrants.applyTransactionGrants(users.findById(userId)
                .orElseThrow(UserNotFound::new));

        // Calculate linked roles map
        final Map<String, String> purchasesToRoles = new HashMap<>();
        user.getPurchases().forEach(purchase -> projects.findById(purchase).ifPresent(project -> {
            final String linkedRole = project.getMetadata().getLinkedDiscordRole();
            if (linkedRole != null) {
                purchasesToRoles.put(purchase, linkedRole);
            }
        }));
        return purchasesToRoles;
    }

    @Operation(
            summary = "Set the role of a user.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The user with their updated permission flags"
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
    @ApiResponse(
            responseCode = "404",
            description = "The user was not found.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @PutMapping(
            value = "/v1/users/{userId}/role",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    public User setUserRole(
            @AuthenticationPrincipal User principal,

            @Parameter(description = "The ID of the user to get.")
            @PathVariable String userId,

            @RequestBody String roleName
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        if (!principal.isAdmin()) {
            throw new NoPermission();
        }

        // Prevent self- or illegal-updates
        final User.Role role = User.Role.findByName(roleName).orElseThrow(InvalidRole::new);
        final User user = users.findById(userId).orElseThrow(UserNotFound::new);
        if (user.equals(principal) || user.isAdmin()) {
            throw new NoPermission();
        }

        // Set the role & save
        user.setRole(role);
        return users.save(user);
    }

    @Operation(
            summary = "Get the currently logged-in user.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The currently logged-in user."
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping(
            value = "/v1/users/@me",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @CrossOrigin
    public User getLoggedInUser(
            @AuthenticationPrincipal User principal
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        return principal;
    }

    @Operation(
            summary = "Request an email verification code.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The user with their updated email."
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @PostMapping(
            value = "/v1/users/@me/email",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            consumes = {MediaType.APPLICATION_JSON_VALUE}
    )
    public User updateLoggedInUserEmail(
            @AuthenticationPrincipal User principal,

            @RequestBody
            @Email
            String email
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        principal.setEmail(email);
        principal.setEmailVerified(false);
        final User newUser = users.save(principal);
        emailService.sendVerificationCodeEmail(principal);
        return newUser;
    }

    @Operation(
            summary = "Request an email verification code for a user by ID.",
            security = @SecurityRequirement(name = "APIKey")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The user with their updated email."
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @PostMapping(
            value = "/v1/users/{userId}/email/api",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            consumes = {MediaType.TEXT_PLAIN_VALUE}
    )
    @CrossOrigin(
            origins = "*", allowCredentials = "false",
            allowedHeaders = {"X-Api-Key", "Content-Type", "Accept"}
    )
    public User updateUserEmailApi(
            @RequestHeader("X-Api-Key") String apiKey,

            @Parameter(description = "The ID of the user to request email verification for.")
            @PathVariable String userId,

            @RequestBody
            String email
    ) {
        if (config.getApiSecret() == null) {
            throw new IllegalStateException("API key is not set on server.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new NotAuthenticated();
        }
        if (!MessageDigest.isEqual(Utf8.encode(config.getApiSecret()), Utf8.encode(apiKey))) {
            throw new NoPermission();
        }

        final User user = users.findById(userId).orElseThrow(UserNotFound::new);
        user.setEmail(email);
        user.setEmailVerified(false);
        final User newUser = users.save(user);
        emailService.sendVerificationCodeEmail(user);
        return newUser;
    }

    @Operation(
            summary = "Verify a user's email address with a code."
    )
    @ApiResponse(
            responseCode = "302",
            description = "The email was verified. Redirecting accordingly..."
    )
    @ApiResponse(
            responseCode = "302",
            description = "The verification code is invalid. Redirecting accordingly..."
    )
    @ApiResponse(
            responseCode = "200",
            description = "The email was verified."
    )
    @ApiResponse(
            responseCode = "401",
            description = "The verification code is invalid",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping(
            value = "/v1/users/{userId}/email/{verificationCode}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<?> verifyUserEmail(
            @Parameter(description = "The ID of the user to verify.")
            @PathVariable String userId,

            @Parameter(description = "The verification code to verify the user with.")
            @PathVariable String verificationCode,

            @Parameter(description = "Whether to redirect to the user's account page after verifying")
            @RequestParam(value = "redirect", defaultValue = "true")
            boolean redirect
    ) {
        if (!emailService.verifyEmail(verificationCode, userId)) {
            if (!redirect) {
                throw new InvalidMailCode();
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("%s/account/email?status=expired"
                            .formatted(config.getFrontendBaseUrl().toString())))
                    .build();
        }
        final User user = users.findById(userId).orElseThrow(UserNotFound::new);
        user.setEmailVerified(true);
        users.save(user);

        if (!redirect) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("%s/account/email?status=verified"
                        .formatted(config.getFrontendBaseUrl().toString())))
                .build();
    }

    @Operation(
            summary = "Delete the currently logged-in user.",
            security = @SecurityRequirement(name = "OAuth2")
    )
    @ApiResponse(
            responseCode = "200",
            description = "The user that was deleted."
    )
    @ApiResponse(
            responseCode = "401",
            description = "The user is not logged in.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @DeleteMapping(
            value = "/v1/users/@me",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public User deleteLoggedInUser(
            @AuthenticationPrincipal User principal
    ) {
        if (principal == null) {
            throw new NotAuthenticated();
        }
        users.delete(principal);
        return principal;
    }

}
