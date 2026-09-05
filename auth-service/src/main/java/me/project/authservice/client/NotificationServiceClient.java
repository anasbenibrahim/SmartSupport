package me.project.authservice.client;

import me.project.authservice.dto.EmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "NOTIFICATION-SERVICE")
public interface NotificationServiceClient {
    @PostMapping("/internal/email")
    void sendEmail(@RequestHeader("X-Internal-Secret") String secret, @RequestBody EmailRequest request);
}