package me.project.notificationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.project.notificationservice.dto.EmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class InternalEmailController {
    private final JavaMailSender mailSender;

    @Value("${notification.internal-secret}")
    private String internalSecret;

    @PostMapping("/internal/email")
    public void sendEmail(@RequestHeader(value = "X-Internal-Secret", required = false) String secret,
                          @Valid @RequestBody EmailRequest request) {
        if (secret == null || !secret.equals(internalSecret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal secret");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getRecipient());
        message.setSubject(request.getSubject());
        message.setText(request.getBody());
        mailSender.send(message);
    }
}