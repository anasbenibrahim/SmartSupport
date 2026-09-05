package me.project.ticketservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AIAnalysisResponse {
    private String category;
    private String priority;
    private Double confidence;
    @JsonProperty("suggested_response") private String suggestedResponse;
}