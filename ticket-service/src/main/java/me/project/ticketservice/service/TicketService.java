package me.project.ticketservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.project.ticketservice.client.AIServiceClient;
import me.project.ticketservice.dto.*;
import me.project.ticketservice.entity.*;
import me.project.ticketservice.repository.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@Service @RequiredArgsConstructor @Slf4j
public class TicketService {
    private final TicketRepository repository;
    private final AIServiceClient aiServiceClient;

    @Transactional
    public TicketResponse create(TicketRequest request) {
        Ticket ticket = Ticket.builder().title(request.getTitle()).description(request.getDescription())
                .customerId(request.getCustomerId()).customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail()).status(TicketStatus.OPEN).build();
        Ticket saved = repository.save(ticket);
        return toResponse(saved);
    }

    @Async
    public void analyze(Long ticketId) {
        try {
            Ticket ticket = repository.findById(ticketId).orElse(null);
            if (ticket == null) return;
            AIAnalysisResponse result = aiServiceClient.analyze(new AIAnalysisRequest(ticket.getTitle() + ". " + ticket.getDescription()));
            if (result == null) return;
            ticket.setCategory(parseCategory(result.getCategory()));
            ticket.setPriority(parsePriority(result.getPriority()));
            ticket.setAiConfidence(result.getConfidence());
            ticket.setAiSuggestion(result.getSuggestedResponse());
            repository.save(ticket);
        } catch (Exception exception) {
            log.warn("AI analysis unavailable for ticket {}: {}", ticketId, exception.getMessage());
        }
    }

    private TicketCategory parseCategory(String value) {
        try { return value == null ? TicketCategory.OTHER : TicketCategory.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException exception) { return TicketCategory.OTHER; }
    }

    private TicketPriority parsePriority(String value) {
        try { return value == null ? TicketPriority.MEDIUM : TicketPriority.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException exception) { return TicketPriority.MEDIUM; }
    }

    @Transactional(readOnly = true)
    public TicketResponse get(Long id) { return toResponse(find(id)); }
    @Transactional(readOnly = true)
    public List<TicketResponse> all() { return repository.findAll().stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public List<TicketResponse> byCustomer(Long id) { return repository.findByCustomerIdOrderByCreatedAtDesc(id).stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public List<TicketResponse> byStatus(TicketStatus status) { return repository.findByStatusOrderByCreatedAtDesc(status).stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public List<TicketResponse> byPriority(TicketPriority priority) { return repository.findByPriorityOrderByCreatedAtDesc(priority).stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public List<TicketResponse> byCategory(TicketCategory category) { return repository.findByCategoryOrderByCreatedAtDesc(category).stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public List<TicketResponse> byAgent(Long id) { return repository.findByAssignedAgentIdOrderByCreatedAtDesc(id).stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public List<TicketResponse> search(String query) { return repository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query).stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public List<TicketResponse> unassigned() { return repository.findByAssignedAgentIdIsNullAndStatusOrderByCreatedAtDesc(TicketStatus.OPEN).stream().map(this::toResponse).toList(); }

    @Transactional
    public TicketResponse update(Long id, TicketRequest request) {
        Ticket ticket = find(id);
        ticket.setTitle(request.getTitle()); ticket.setDescription(request.getDescription());
        return toResponse(repository.save(ticket));
    }

    @Transactional
    public TicketResponse assign(Long id, Long agentId) {
        Ticket ticket = find(id); ticket.setAssignedAgentId(agentId); ticket.setStatus(TicketStatus.IN_PROGRESS);
        return toResponse(repository.save(ticket));
    }

    @Transactional
    public TicketResponse status(Long id, TicketStatus status) {
        Ticket ticket = find(id); ticket.setStatus(status);
        return toResponse(repository.save(ticket));
    }

    @Transactional
    public TicketResponse resolve(Long id, String resolution) {
        Ticket ticket = find(id);
        ticket.setResolution(resolution);
        ticket.setStatus(TicketStatus.RESOLVED);
        return toResponse(repository.save(ticket));
    }

    @Transactional
    public void delete(Long id) { if (!repository.existsById(id)) throw notFound(id); repository.deleteById(id); }

    @Transactional(readOnly = true)
    public Map<String, Long> statistics() {
        return Map.of("total", repository.count(), "open", repository.countByStatus(TicketStatus.OPEN),
                "inProgress", repository.countByStatus(TicketStatus.IN_PROGRESS), "resolved", repository.countByStatus(TicketStatus.RESOLVED),
                "closed", repository.countByStatus(TicketStatus.CLOSED), "critical", repository.countByPriority(TicketPriority.CRITICAL),
                "high", repository.countByPriority(TicketPriority.HIGH), "medium", repository.countByPriority(TicketPriority.MEDIUM),
                "low", repository.countByPriority(TicketPriority.LOW));
    }

    private Ticket find(Long id) { return repository.findById(id).orElseThrow(() -> notFound(id)); }
    private ResponseStatusException notFound(Long id) { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found with id: " + id); }
    private TicketResponse toResponse(Ticket ticket) {
        return TicketResponse.builder().id(ticket.getId()).title(ticket.getTitle()).description(ticket.getDescription())
                .status(ticket.getStatus()).priority(ticket.getPriority()).category(ticket.getCategory()).customerId(ticket.getCustomerId())
                .customerName(ticket.getCustomerName()).customerEmail(ticket.getCustomerEmail()).assignedAgentId(ticket.getAssignedAgentId())
                .aiConfidence(ticket.getAiConfidence()).aiSuggestion(ticket.getAiSuggestion()).resolution(ticket.getResolution())
                .createdAt(ticket.getCreatedAt()).updatedAt(ticket.getUpdatedAt()).resolvedAt(ticket.getResolvedAt()).build();
    }
}