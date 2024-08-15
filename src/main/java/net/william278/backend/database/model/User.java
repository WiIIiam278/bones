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

package net.william278.backend.database.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements OAuth2User {

    private static final String CDN_URL = "https://cdn.discordapp.com";

    @Id
    @Schema(
            name = "id",
            description = "The user's Discord snowflake ID."
    )
    private String id;

    @Schema(
            name = "createdAt",
            description = "When the user created their account."
    )
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Schema(
            name = "name",
            description = "The user's account username. Not necessarily unique."
    )
    private String name;

    @Schema(
            name = "email",
            description = "The user's email address."
    )
    @Nullable
    private String email;

    @JsonIgnore
    @Nullable
    private String avatar;

    @Schema(
            name = "role",
            description = "The user's role"
    )
    @Builder.Default
    private Role role = Role.USER;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "purchases",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "project_slug")
    )
    @Setter(AccessLevel.NONE)
    @JsonIgnore
    private Set<Project> purchases = new HashSet<>();

    @SuppressWarnings("unused")
    @JsonSerialize
    @JsonAlias("purchases")
    @NotNull
    @Unmodifiable
    @Schema(
            name = "purchases",
            description = "List of projects, by slug, that the user has purchased"
    )
    public Set<String> getPurchases() {
        return purchases.stream().map(Project::getSlug).collect(Collectors.toSet());
    }

    public boolean setPurchases(@NotNull Set<Project> purchases) {
        boolean changed = !this.purchases.equals(purchases);
        this.purchases = new HashSet<>(purchases);
        return changed;
    }

    public boolean addPurchases(@NotNull Set<Project> purchases) {
        return this.purchases.addAll(purchases);
    }

    @JsonSerialize
    @JsonAlias("avatar")
    @NotNull
    @Schema(
            name = "avatar",
            description = "The user's avatar URL"
    )
    public URI getAvatar() {
        if (avatar == null) {
            final Long defaultAvatarIndex = (Long.parseLong(id.trim()) >> 22) % 6;
            return URI.create("%s/embed/avatars/%s.png".formatted(CDN_URL, defaultAvatarIndex));
        }
        return URI.create("%s/avatars/%s/%s.png".formatted(CDN_URL, id, avatar));
    }

    @JsonIgnore
    public boolean isAdmin() {
        return role.isAtLeast(Role.ADMIN);
    }

    @JsonIgnore
    public boolean isStaff() {
        return role.isAtLeast(Role.STAFF);
    }

    public boolean hasProjectPermission(@NotNull Project project) {
        return isStaff() || purchases.stream().anyMatch(p -> p.getSlug().equals(project.getSlug()));
    }

    @JsonIgnore
    @Override
    @Unmodifiable
    public Map<String, Object> getAttributes() {
        return Map.of(
                "id", id,
                "username", name,
                "email", email == null ? "" : email,
                "avatar", getAvatar(),
                "projects", purchases,
                "role", role
        );
    }

    @JsonIgnore
    @Override
    @Unmodifiable
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of((GrantedAuthority) () -> "DISCORD");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User other) {
            return id.equals(other.id);
        }
        return super.equals(obj);
    }

    @Schema(
            name = "Role",
            description = "Roles a user can have"
    )
    @AllArgsConstructor
    public enum Role {
        @Schema(description = "A standard user")
        USER(0),
        @Schema(description = "A user with staff permissions")
        STAFF(100),
        @Schema(description = "A user with administrative permissions")
        ADMIN(200);

        @JsonIgnore
        private final int weight;

        public boolean isAtLeast(@NotNull Role role) {
            return this.weight >= role.weight;
        }

        public static Optional<Role> findByName(@NotNull String name) {
            return Arrays.stream(values())
                    .filter(role -> role.name().equals(name.toUpperCase(Locale.ROOT)))
                    .findFirst();
        }
    }

}
