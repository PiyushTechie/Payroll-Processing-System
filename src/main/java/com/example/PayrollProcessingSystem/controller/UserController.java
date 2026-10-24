package com.example.PayrollProcessingSystem.controller;

import com.example.PayrollProcessingSystem.entity.User;
import com.example.PayrollProcessingSystem.repository.EmployeeRepository;
import com.example.PayrollProcessingSystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<User> getUserByUsername(@RequestParam String username) {
        return userRepository.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user, Authentication authentication) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body("Username is required");
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body("Username already taken");
        }
        if (user.getEmployee() == null || user.getEmployee().getEmployeeId() == null) {
            return ResponseEntity.badRequest().body("Employee reference is required");
        }
        Long employeeId = user.getEmployee().getEmployeeId();
        var employeeOpt = employeeRepository.findById(employeeId);
        if (employeeOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Employee with ID " + employeeId + " not found");
        }
        if (userRepository.existsByEmployeeEmployeeId(employeeId)) {
            return ResponseEntity.badRequest().body("This employee already has a user account");
        }
        user.setEmployee(employeeOpt.get());

        // Automatically hash plain-text password behind the scenes
        String rawPassword = user.getPassword();
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        } else if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            if (!user.getPasswordHash().startsWith("$2a$") && !user.getPasswordHash().startsWith("$2b$")) {
                user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
            }
        } else {
            return ResponseEntity.badRequest().body("Password is required");
        }

        // Account lock can ONLY be set by ADMIN
        if (user.getStatus() == User.UserStatus.LOCKED && !isAdmin(authentication)) {
            throw new AccessDeniedException("Only ADMIN users are authorized to set an account to LOCKED status.");
        }

        if (user.getStatus() == null) {
            user.setStatus(User.UserStatus.ACTIVE);
        }

        return ResponseEntity.ok(userRepository.save(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id, 
            @RequestBody User updated, 
            Authentication authentication) {

        // Enforce: Account lock can ONLY be performed by ADMIN
        if (updated.getStatus() == User.UserStatus.LOCKED && !isAdmin(authentication)) {
            throw new AccessDeniedException("Only ADMIN users are authorized to lock user accounts.");
        }

        return userRepository.findById(id).map(u -> {
            // Unlocking an already locked account also requires ADMIN
            if (u.getStatus() == User.UserStatus.LOCKED && updated.getStatus() != null 
                    && updated.getStatus() != User.UserStatus.LOCKED && !isAdmin(authentication)) {
                throw new AccessDeniedException("Only ADMIN users are authorized to unlock user accounts.");
            }

            if (updated.getUsername() != null && !updated.getUsername().isBlank()) {
                if (!u.getUsername().equalsIgnoreCase(updated.getUsername()) && userRepository.existsByUsername(updated.getUsername())) {
                    return ResponseEntity.badRequest().body("Username '" + updated.getUsername() + "' is already taken by another account");
                }
                u.setUsername(updated.getUsername());
            }
            if (updated.getRole() != null) {
                u.setRole(updated.getRole());
            }
            if (updated.getStatus() != null) {
                u.setStatus(updated.getStatus());
            }
            // Allow updating password seamlessly with plain text
            if (updated.getPassword() != null && !updated.getPassword().isBlank()) {
                u.setPasswordHash(passwordEncoder.encode(updated.getPassword()));
            } else if (updated.getPasswordHash() != null && !updated.getPasswordHash().isBlank()) {
                if (!updated.getPasswordHash().startsWith("$2a$") && !updated.getPasswordHash().startsWith("$2b$")) {
                    u.setPasswordHash(passwordEncoder.encode(updated.getPasswordHash()));
                } else {
                    u.setPasswordHash(updated.getPasswordHash());
                }
            }
            return ResponseEntity.ok(userRepository.save(u));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> lockUser(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setStatus(User.UserStatus.LOCKED);
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unlockUser(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setStatus(User.UserStatus.ACTIVE);
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}