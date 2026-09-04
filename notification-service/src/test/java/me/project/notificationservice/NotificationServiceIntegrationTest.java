package me.project.notificationservice;

import me.project.notificationservice.dto.NotificationRequest;
import me.project.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    void shouldCreateNotification() {
        NotificationRequest request = new NotificationRequest();
        request.setUserId("user-123");
        request.setTitle("Welcome");
        request.setMessage("Your account has been created successfully.");
        request.setType("INFO");

        var created = notificationService.createNotification(request);

        assertNotNull(created.getId());
        assertEquals("user-123", created.getUserId());
        assertEquals("Welcome", created.getTitle());
    }
}
