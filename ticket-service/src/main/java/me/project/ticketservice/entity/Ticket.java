package me.project.ticketservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_ticket_customer", columnList = "customer_id"),
        @Index(name = "idx_ticket_status", columnList = "status"),
        @Index(name = "idx_ticket_priority", columnList = "priority")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TicketStatus status;
    @Enumerated(EnumType.STRING) private TicketPriority priority;
    @Enumerated(EnumType.STRING) private TicketCategory category;
    @Column(name = "customer_id", nullable = false) private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long assignedAgentId;
    private Double aiConfidence;
    @Column(columnDefinition = "TEXT") private String aiSuggestion;
    @Column(columnDefinition = "TEXT") private String resolution;
    @CreationTimestamp @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    @PrePersist
    void initialize() {
        if (status == null) status = TicketStatus.OPEN;
        if (priority == null) priority = TicketPriority.MEDIUM;
    }

    @PreUpdate
    void markResolved() {
        if (status == TicketStatus.RESOLVED && resolvedAt == null) resolvedAt = LocalDateTime.now();
    }
}