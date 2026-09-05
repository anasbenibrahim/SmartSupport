package me.project.ticketservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.project.ticketservice.dto.*;
import me.project.ticketservice.entity.*;
import me.project.ticketservice.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/tickets") @RequiredArgsConstructor
public class TicketController {
    private final TicketService service;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<TicketResponse>> create(@Valid @RequestBody TicketRequest request) {
        TicketResponse ticket = service.create(request);
        service.analyze(ticket.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(ticket, "Ticket created successfully"));
    }
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ApiResponse<List<TicketResponse>> all() { return ApiResponse.success(service.all(), "Tickets retrieved successfully"); }
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ApiResponse<Map<String, Long>> statistics() { return ApiResponse.success(service.statistics(), "Statistics retrieved successfully"); }
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ApiResponse<List<TicketResponse>> search(@RequestParam String query) { return ApiResponse.success(service.search(query), "Search results retrieved successfully"); }
    @GetMapping("/unassigned")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ApiResponse<List<TicketResponse>> unassigned() { return ApiResponse.success(service.unassigned(), "Unassigned tickets retrieved successfully"); }
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'SUPPORT_AGENT')")
    public ApiResponse<List<TicketResponse>> byCustomer(@PathVariable Long customerId) { return ApiResponse.success(service.byCustomer(customerId), "Customer tickets retrieved successfully"); }
    @GetMapping("/status/{status}") public ApiResponse<List<TicketResponse>> byStatus(@PathVariable TicketStatus status) { return ApiResponse.success(service.byStatus(status), "Tickets retrieved successfully"); }
    @GetMapping("/priority/{priority}") public ApiResponse<List<TicketResponse>> byPriority(@PathVariable TicketPriority priority) { return ApiResponse.success(service.byPriority(priority), "Tickets retrieved successfully"); }
    @GetMapping("/category/{category}") public ApiResponse<List<TicketResponse>> byCategory(@PathVariable TicketCategory category) { return ApiResponse.success(service.byCategory(category), "Tickets retrieved successfully"); }
    @GetMapping("/agent/{agentId}") public ApiResponse<List<TicketResponse>> byAgent(@PathVariable Long agentId) { return ApiResponse.success(service.byAgent(agentId), "Agent tickets retrieved successfully"); }
    @GetMapping("/{id}") public ApiResponse<TicketResponse> get(@PathVariable Long id) { return ApiResponse.success(service.get(id), "Ticket retrieved successfully"); }
    @PutMapping("/{id}") public ApiResponse<TicketResponse> update(@PathVariable Long id, @Valid @RequestBody TicketRequest request) { return ApiResponse.success(service.update(id, request), "Ticket updated successfully"); }
    @PatchMapping("/{id}/assign/{agentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ApiResponse<TicketResponse> assign(@PathVariable Long id, @PathVariable Long agentId) { return ApiResponse.success(service.assign(id, agentId), "Ticket assigned successfully"); }
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ApiResponse<TicketResponse> status(@PathVariable Long id, @Valid @RequestBody StatusRequest request) { return ApiResponse.success(service.status(id, request.getStatus()), "Ticket status updated successfully"); }
    @PatchMapping("/{id}/resolution")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ApiResponse<TicketResponse> resolve(@PathVariable Long id, @Valid @RequestBody ResolutionRequest request) {
        return ApiResponse.success(service.resolve(id, request.getResolution()), "Response sent to customer successfully");
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) { service.delete(id); return ApiResponse.success(null, "Ticket deleted successfully"); }
}