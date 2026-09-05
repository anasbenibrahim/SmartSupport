package me.project.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.project.authservice.dto.AuthResponse;
import me.project.authservice.dto.CreateSupportRequest;
import me.project.authservice.dto.SupportCreatedResponse;
import me.project.authservice.dto.LoginRequest;
import me.project.authservice.dto.RegisterRequest;
import me.project.authservice.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${notification.internal-secret:local-notification-secret}")
    private String notificationInternalSecret;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/admin/support")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupportCreatedResponse> createSupport(@Valid @RequestBody CreateSupportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.createSupport(request, notificationInternalSecret));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(Map.of("valid", authService.isAccessTokenValid(authorization)));
    }
}