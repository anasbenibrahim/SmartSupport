package me.project.authservice;

import me.project.authservice.client.UserServiceClient;
import me.project.authservice.dto.RegisterRequest;
import me.project.authservice.entity.Role;
import me.project.authservice.entity.User;
import me.project.authservice.repository.UserRepository;
import me.project.authservice.security.JwtService;
import me.project.authservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRoleSyncTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldKeepSupportAgentRoleAndSyncToUserService() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Support");
        request.setLastName("Agent");
        request.setEmail("support@example.com");
        request.setPassword("secret123");
        request.setRole(Role.SUPPORT_AGENT);

        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(userRepository.existsByEmail("support@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(999L);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh");
        when(userServiceClient.syncUser(any())).thenReturn(ResponseEntity.ok().build());

        var response = authService.register(request);

        assertEquals(Role.SUPPORT_AGENT, response.getUser().getRole());
        verify(userServiceClient).syncUser(any());
    }
}
