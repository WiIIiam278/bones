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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User implements OAuth2User {

    private static final String CDN_URL = "https://cdn.discordapp.com";

    @Id
    @Schema(
            name = "id",
            description = "The user's unique ID."
    )
    private String id;

    @Schema(
            name = "createdAt",
            description = "When the user created their account."
    )
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
    private String email;

    @JsonIgnore
    @Nullable
    private String avatar;

    @Builder.Default
    @Schema(
            name = "admin",
            description = "Whether the user is an administrator"
    )
    @Getter(AccessLevel.NONE)
    private Boolean admin = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "purchases",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "project_slug")
    )
    @Setter(AccessLevel.NONE)
    @JsonIgnore
    private Set<Project> purchases;

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

    public boolean setPurchases(@NotNull List<Project> purchases) {
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

    public boolean isAdmin() {
        return admin != null && admin;
    }

    public boolean hasProjectPermission(@NotNull Project project) {
        return admin || purchases.contains(project);
    }

    @JsonIgnore
    @Override
    @Unmodifiable
    public Map<String, Object> getAttributes() {
        return Map.of(
                "id", id,
                "username", name,
                "email", email,
                "avatar", getAvatar(),
                "projects", purchases,
                "admin", admin
        );
    }

    @JsonIgnore
    @Override
    @Unmodifiable
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of((GrantedAuthority) () -> "DISCORD");
    }

}
