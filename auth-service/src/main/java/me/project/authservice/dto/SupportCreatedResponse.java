package me.project.authservice.dto;

import lombok.Builder;
import lombok.Value;

@Value @Builder
public class SupportCreatedResponse {
    Long id;
    String firstName;
    String lastName;
    String email;
    String role;
    boolean emailSent;
}