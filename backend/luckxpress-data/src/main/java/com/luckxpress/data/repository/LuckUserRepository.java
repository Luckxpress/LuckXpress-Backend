package com.luckxpress.data.repository;

import com.luckxpress.data.entity.LuckUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LuckUserRepository extends JpaRepository<LuckUser, Long> {

    Optional<LuckUser> findByUsername(String username);
    
    Optional<LuckUser> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    Page<LuckUser> findByStatus(LuckUser.Status status, Pageable pageable);
    
    Page<LuckUser> findByRole(LuckUser.Role role, Pageable pageable);
    
    @Query("SELECT u FROM LuckUser u WHERE u.createdAt >= :startDate")
    List<LuckUser> findRecentUsers(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(u) FROM LuckUser u WHERE u.status = :status")
    long countByStatus(@Param("status") LuckUser.Status status);
    
    @Query("SELECT u FROM LuckUser u WHERE " +
           "(:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
           "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:status IS NULL OR u.status = :status)")
    Page<LuckUser> findUsersWithFilters(
        @Param("username") String username,
        @Param("email") String email, 
        @Param("status") LuckUser.Status status,
        Pageable pageable
    );
}
