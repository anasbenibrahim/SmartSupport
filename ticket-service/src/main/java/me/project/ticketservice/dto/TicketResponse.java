package me.project.ticketservice.dto;

import lombok.Builder;
import lombok.Value;
import me.project.ticketservice.entity.*;
import java.time.LocalDateTime;

@Value @Builder
public class TicketResponse {
    Long id; String title; String description; TicketStatus status; TicketPriority priority;
    TicketCategory category; Long customerId; String customerName; String customerEmail;
    Long assignedAgentId; Double aiConfidence; String aiSuggestion; String resolution;
    LocalDateTime createdAt; LocalDateTime updatedAt; LocalDateTime resolvedAt;
}