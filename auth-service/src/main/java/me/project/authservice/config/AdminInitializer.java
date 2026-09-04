package me.project.authservice.config;

import lombok.RequiredArgsConstructor;
import me.project.authservice.entity.Role;
import me.project.authservice.entity.User;
import me.project.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner createDefaultAdmin(
            @Value("${app.admin.email:admin@smartsupport.local}") String email,
            @Value("${app.admin.password:Admin@12345}") String password) {
        return args -> {
            if (userRepository.existsByEmail(email)) {
                return;
            }

            userRepository.save(User.builder()
                    .firstName("System")
                    .lastName("Administrator")
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build());
        };
    }
}