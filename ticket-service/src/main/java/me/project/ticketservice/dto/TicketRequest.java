package me.project.ticketservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketRequest {
    @NotBlank @Size(min = 5, max = 255) private String title;
    @NotBlank @Size(min = 10) private String description;
    @NotNull private Long customerId;
    private String customerName;
    private String customerEmail;
}