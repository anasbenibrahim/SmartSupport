package me.project.ticketservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import me.project.ticketservice.entity.TicketStatus;

@Data
public class StatusRequest { @NotNull private TicketStatus status; }