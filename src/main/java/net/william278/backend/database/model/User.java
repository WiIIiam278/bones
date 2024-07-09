package net.william278.backend.database.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User implements OAuth2User {

    @Id
    private String id;
    private String name;
    @ManyToMany
    private List<Project> projects;

    public boolean hasProjectPermission(@NotNull Project project) {
        return projects.contains(project);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of(
                "id", id,
                "name", name,
                "projects", projects
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                (GrantedAuthority) () -> "USER"
        );
    }

}
