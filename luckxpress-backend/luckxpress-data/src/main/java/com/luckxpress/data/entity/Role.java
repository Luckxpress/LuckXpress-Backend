package com.luckxpress.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Role entity representing user roles in the system
 * Used for role-based access control (RBAC)
 */
@Entity
@Table(name = "roles", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "name")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @NotBlank
    @Size(max = 50)
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Size(max = 255)
    @Column(name = "description")
    private String description;

    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

    /**
     * Predefined role constants
     */
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_MODERATOR = "ROLE_MODERATOR";
    public static final String ROLE_GUEST = "ROLE_GUEST";

    /**
     * Constructor with role name
     * @param name the role name
     */
    public Role(String name) {
        this.name = name;
    }

    /**
     * Constructor with role name and description
     * @param name the role name
     * @param description the role description
     */
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        if (!super.equals(o)) return false;
        
        Role role = (Role) o;
        return name != null && name.equals(role.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : super.hashCode();
    }

    @Override
    public String toString() {
        return "Role{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
