package me.project.ticketservice.client;

import me.project.ticketservice.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "AI-SERVICE", url = "${ai.service.url:http://ai-service:5000}")
public interface AIServiceClient {
    @PostMapping("/api/ai/analyze")
    AIAnalysisResponse analyze(@RequestBody AIAnalysisRequest request);
}