package me.project.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class EmailRequest {
    private String recipient;
    private String subject;
    private String body;
}