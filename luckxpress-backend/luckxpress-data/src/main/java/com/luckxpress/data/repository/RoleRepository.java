package com.luckxpress.data.repository;

import com.luckxpress.data.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for Role entity
 * Provides database access operations for roles
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Find role by name
     * @param name the role name
     * @return Optional containing role if found
     */
    Optional<Role> findByName(String name);

    /**
     * Check if role exists by name
     * @param name the role name
     * @return true if exists
     */
    Boolean existsByName(String name);

    /**
     * Find roles by names
     * @param names set of role names
     * @return list of matching roles
     */
    List<Role> findByNameIn(Set<String> names);

    /**
     * Find all active roles
     * @return list of active roles
     */
    List<Role> findByIsActiveTrue();

    /**
     * Find roles containing keyword in name or description
     * @param keyword search keyword
     * @return list of matching roles
     */
    @Query("SELECT r FROM Role r WHERE " +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Role> searchRoles(@Param("keyword") String keyword);

    /**
     * Count users for a specific role
     * @param roleId the role ID
     * @return count of users with the role
     */
    @Query("SELECT COUNT(u) FROM Role r JOIN r.users u WHERE r.id = :roleId")
    Long countUsersForRole(@Param("roleId") Long roleId);

    /**
     * Find roles that have no users
     * @return list of unused roles
     */
    @Query("SELECT r FROM Role r WHERE r.users IS EMPTY")
    List<Role> findUnusedRoles();

    /**
     * Delete role by name
     * @param name the role name
     */
    void deleteByName(String name);
}
