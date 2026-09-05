package me.project.ticketservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResolutionRequest {
    @NotBlank
    @Size(min = 5, max = 5000)
    private String resolution;
}