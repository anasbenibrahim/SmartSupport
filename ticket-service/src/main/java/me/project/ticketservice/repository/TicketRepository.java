package me.project.ticketservice.repository;

import me.project.ticketservice.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status);
    List<Ticket> findByAssignedAgentIdOrderByCreatedAtDesc(Long agentId);
    List<Ticket> findByCategoryOrderByCreatedAtDesc(TicketCategory category);
    List<Ticket> findByPriorityOrderByCreatedAtDesc(TicketPriority priority);
    List<Ticket> findByAssignedAgentIdIsNullAndStatusOrderByCreatedAtDesc(TicketStatus status);
    List<Ticket> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);
    long countByStatus(TicketStatus status);
    long countByPriority(TicketPriority priority);
}