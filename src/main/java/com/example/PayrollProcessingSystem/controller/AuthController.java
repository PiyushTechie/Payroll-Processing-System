package com.example.PayrollProcessingSystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Authentication", description = "Endpoints for authenticating users")
public class AuthController {

    @Operation(summary = "Login", description = "Login with username and password to get a session cookie.")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping(value = "/login", consumes = "application/x-www-form-urlencoded")
    public void login(
            @Parameter(description = "Username of the user", required = true) @RequestParam String username,
            @Parameter(description = "Password of the user", required = true) @RequestParam String password) {
        // This method is intentionally empty.
        // Spring Security's UsernamePasswordAuthenticationFilter intercepts the /login request.
        // We define this method solely so that springdoc-openapi (Swagger UI) can document it.
        throw new IllegalStateException("This method shouldn't be called. It's implemented by Spring Security.");
    }

    @Operation(summary = "Logout", description = "Logout and invalidate the session cookie.")
    @ApiResponse(responseCode = "200", description = "Logout successful")
    @PostMapping("/api/logout")
    public void logout() {
        // This method is intentionally empty.
        // Spring Security intercepts the /api/logout request.
        throw new IllegalStateException("This method shouldn't be called. It's implemented by Spring Security.");
    }
}
