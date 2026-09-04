package me.project.notificationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.project.notificationservice.dto.ApiResponse;
import me.project.notificationservice.dto.NotificationDTO;
import me.project.notificationservice.dto.NotificationRequest;
import me.project.notificationservice.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationDTO>> createNotification(@Valid @RequestBody NotificationRequest request) {
        NotificationDTO created = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Notification created successfully"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getUserNotifications(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationsForUser(userId), "Notifications retrieved successfully"));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getUnreadNotifications(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadNotificationsForUser(userId), "Unread notifications retrieved successfully"));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationDTO>> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markAsRead(id), "Notification marked as read"));
    }
}
