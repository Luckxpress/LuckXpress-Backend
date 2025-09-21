package com.luckxpress.controller;

import com.luckxpress.data.entity.LuckUser;
import com.luckxpress.data.repository.LuckUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserManagementController {

    @Autowired
    private LuckUserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getUsers(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(defaultValue = "id") String sort,
                                                        @RequestParam(defaultValue = "desc") String order,
                                                        @RequestParam(required = false) String username,
                                                        @RequestParam(required = false) String email,
                                                        @RequestParam(required = false) String status) {
        
        Sort.Direction direction = order.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        
        LuckUser.Status statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = LuckUser.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore filter
            }
        }
        
        Page<LuckUser> users = userRepository.findUsersWithFilters(username, email, statusEnum, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", users.getContent());
        response.put("total", users.getTotalElements());
        response.put("page", page);
        response.put("size", size);
        response.put("totalPages", users.getTotalPages());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable Long id) {
        Optional<LuckUser> user = userRepository.findById(id);
        if (user.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("data", user.get());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> userData) {
        try {
            LuckUser user = new LuckUser();
            user.setUsername((String) userData.get("username"));
            user.setEmail((String) userData.get("email"));
            user.setFirstName((String) userData.get("firstName"));
            user.setLastName((String) userData.get("lastName"));
            user.setPhoneNumber((String) userData.get("phoneNumber"));
            user.setCountry((String) userData.get("country"));
            
            if (userData.get("role") != null) {
                user.setRole(LuckUser.Role.valueOf(((String) userData.get("role")).toUpperCase()));
            }
            
            if (userData.get("status") != null) {
                user.setStatus(LuckUser.Status.valueOf(((String) userData.get("status")).toUpperCase()));
            }
            
            LuckUser savedUser = userRepository.save(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", savedUser);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to create user: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> userData) {
        Optional<LuckUser> existingUser = userRepository.findById(id);
        if (!existingUser.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            LuckUser user = existingUser.get();
            
            if (userData.containsKey("username")) {
                user.setUsername((String) userData.get("username"));
            }
            if (userData.containsKey("email")) {
                user.setEmail((String) userData.get("email"));
            }
            if (userData.containsKey("firstName")) {
                user.setFirstName((String) userData.get("firstName"));
            }
            if (userData.containsKey("lastName")) {
                user.setLastName((String) userData.get("lastName"));
            }
            if (userData.containsKey("phoneNumber")) {
                user.setPhoneNumber((String) userData.get("phoneNumber"));
            }
            if (userData.containsKey("country")) {
                user.setCountry((String) userData.get("country"));
            }
            if (userData.containsKey("role")) {
                user.setRole(LuckUser.Role.valueOf(((String) userData.get("role")).toUpperCase()));
            }
            if (userData.containsKey("status")) {
                user.setStatus(LuckUser.Status.valueOf(((String) userData.get("status")).toUpperCase()));
            }
            
            LuckUser savedUser = userRepository.save(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", savedUser);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to update user: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        userRepository.deleteById(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", Map.of("id", id));
        return ResponseEntity.ok(response);
    }

    // Initialize sample data
    @PostMapping("/init-sample-data")
    public ResponseEntity<Map<String, Object>> initSampleData() {
        if (userRepository.count() > 0) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Sample data already exists");
            return ResponseEntity.ok(response);
        }

        List<LuckUser> sampleUsers = Arrays.asList(
            new LuckUser("john_doe", "john@example.com", "John", "Doe"),
            new LuckUser("jane_smith", "jane@example.com", "Jane", "Smith"),
            new LuckUser("bob_wilson", "bob@example.com", "Bob", "Wilson"),
            new LuckUser("alice_brown", "alice@example.com", "Alice", "Brown"),
            new LuckUser("charlie_davis", "charlie@example.com", "Charlie", "Davis")
        );

        // Set additional properties
        sampleUsers.get(0).setRole(LuckUser.Role.VIP);
        sampleUsers.get(0).setBalance(new BigDecimal("1500.00"));
        sampleUsers.get(0).setCountry("USA");
        sampleUsers.get(0).setPhoneNumber("+1-555-0123");

        sampleUsers.get(1).setRole(LuckUser.Role.PLAYER);
        sampleUsers.get(1).setBalance(new BigDecimal("750.50"));
        sampleUsers.get(1).setCountry("Canada");
        sampleUsers.get(1).setPhoneNumber("+1-555-0124");

        sampleUsers.get(2).setRole(LuckUser.Role.PLAYER);
        sampleUsers.get(2).setBalance(new BigDecimal("0.00"));
        sampleUsers.get(2).setStatus(LuckUser.Status.INACTIVE);
        sampleUsers.get(2).setCountry("UK");

        sampleUsers.get(3).setRole(LuckUser.Role.ADMIN);
        sampleUsers.get(3).setBalance(new BigDecimal("0.00"));
        sampleUsers.get(3).setCountry("Australia");

        sampleUsers.get(4).setRole(LuckUser.Role.PLAYER);
        sampleUsers.get(4).setBalance(new BigDecimal("2250.75"));
        sampleUsers.get(4).setStatus(LuckUser.Status.SUSPENDED);
        sampleUsers.get(4).setCountry("Germany");

        userRepository.saveAll(sampleUsers);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sample data created successfully");
        response.put("count", sampleUsers.size());
        return ResponseEntity.ok(response);
    }
}
