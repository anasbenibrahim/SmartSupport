package me.project.authservice.service;

import lombok.RequiredArgsConstructor;
import me.project.authservice.client.NotificationServiceClient;
import me.project.authservice.client.UserServiceClient;
import me.project.authservice.dto.AuthResponse;
import me.project.authservice.dto.CreateSupportRequest;
import me.project.authservice.dto.EmailRequest;
import me.project.authservice.dto.LoginRequest;
import me.project.authservice.dto.RegisterRequest;
import me.project.authservice.dto.SupportCreatedResponse;
import me.project.authservice.dto.UserDTO;
import me.project.authservice.dto.UserSyncRequest;
import me.project.authservice.entity.Role;
import me.project.authservice.entity.User;
import me.project.authservice.exception.CustomException;
import me.project.authservice.repository.UserRepository;
import me.project.authservice.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.JwtException;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserServiceClient userServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@$!%*?&";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("Email already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        try {
            userServiceClient.syncUser(UserSyncRequest.builder()
                    .id(savedUser.getId())
                    .firstName(savedUser.getFirstName())
                    .lastName(savedUser.getLastName())
                    .email(savedUser.getEmail())
                    .role(savedUser.getRole().name())
                    .active(savedUser.isEnabled())
                    .build());
        } catch (Exception ignored) {
            // Safety: auth registration still succeeds even if user-service sync is temporarily unavailable.
        }

        String accessToken = jwtService.generateToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        return buildAuthResponse(savedUser, accessToken, refreshToken);
    }

    @Transactional
    public SupportCreatedResponse createSupport(CreateSupportRequest request, String internalSecret) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("Email already registered", HttpStatus.CONFLICT);
        }

        String generatedPassword = generatePassword();
        User support = userRepository.save(User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(generatedPassword))
                .role(Role.SUPPORT_AGENT)
                .enabled(true)
                .build());

        try {
            userServiceClient.syncUser(UserSyncRequest.builder()
                    .id(support.getId()).firstName(support.getFirstName()).lastName(support.getLastName())
                    .email(support.getEmail()).role(Role.SUPPORT_AGENT.name()).active(true).build());
        } catch (Exception exception) {
            throw new CustomException("Support account created but user profile synchronization failed", HttpStatus.SERVICE_UNAVAILABLE);
        }

        boolean emailSent = true;
        try {
            notificationServiceClient.sendEmail(internalSecret, new EmailRequest(
                    support.getEmail(), "Your SmartSupport support account",
                    "Hello " + support.getFirstName() + ",\n\n"
                            + "Your SmartSupport support account has been created by an administrator.\n\n"
                            + "Email: " + support.getEmail() + "\n"
                            + "Temporary password: " + generatedPassword + "\n\n"
                            + "Please change this password after your first login.\n\nSmartSupport Team"));
        } catch (Exception exception) {
            emailSent = false;
        }

        return SupportCreatedResponse.builder().id(support.getId()).firstName(support.getFirstName())
                .lastName(support.getLastName()).email(support.getEmail()).role(Role.SUPPORT_AGENT.name())
                .emailSent(emailSent).build();
    }

    private String generatePassword() {
        StringBuilder password = new StringBuilder(14);
        for (int index = 0; index < 14; index++) {
            password.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    public AuthResponse login(LoginRequest request) {
        // Authentifier l'utilisateur
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Récupérer l'utilisateur
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        // Générer les tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException("Refresh token is required", HttpStatus.BAD_REQUEST);
        }

        // Extraire l'email du refresh token
        String email;
        try {
            email = jwtService.extractUsername(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new CustomException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
        }

        // Récupérer l'utilisateur
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        // Vérifier si le refresh token est valide
        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new CustomException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
        }

        // Générer de nouveaux tokens
        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    public boolean isAccessTokenValid(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }

        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            return false;
        }

        try {
            String email = jwtService.extractUsername(token);
            User user = userRepository.findByEmail(email).orElse(null);
            return user != null && jwtService.isTokenValid(token, user);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L) // 1 heure en secondes
                .user(userDTO)
                .build();
    }
}