package me.project.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailRequest {
    @NotBlank @Email private String recipient;
    @NotBlank private String subject;
    @NotBlank private String body;
}