package com.paridhi.resume_screener.controller;

import com.paridhi.resume_screener.dto.ScreeningRequest;
import com.paridhi.resume_screener.dto.ScreeningResponse;
import com.paridhi.resume_screener.service.ScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// Add these imports at the top
import com.paridhi.resume_screener.service.GeminiService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.paridhi.resume_screener.dto.AIScreeningResponse;

@RestController
@RequestMapping("/api/v1/screen")
@RequiredArgsConstructor
public class ScreeningController {

    private final ScreeningService screeningService;
    private final GeminiService geminiService;

    @PostMapping
    public ResponseEntity<ScreeningResponse> screenResume(
            @RequestBody ScreeningRequest request) {
        return ResponseEntity.ok(screeningService.screenResume(request));
    }


    @PostMapping("/ai")
    public ResponseEntity<AIScreeningResponse> aiScreenResume(
            @RequestBody ScreeningRequest request) {
        try {
            String rawResponse = geminiService.analyzeResume(
                    request.getResumeText(),
                    request.getJobDescription()
            );
            System.out.println("=== GEMINI RAW RESPONSE ===");
            System.out.println(rawResponse);
            System.out.println("=== END RESPONSE ===");

            // Parse Gemini response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(rawResponse);

            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new RuntimeException("Gemini returned no candidates: " + rawResponse);
            }
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new RuntimeException("Gemini returned no parts: " + rawResponse);
            }
            String aiText = parts.get(0).path("text").asText();

            JsonNode aiJson = mapper.readTree(aiText);

            AIScreeningResponse response = AIScreeningResponse.builder()
                    .overallScore(aiJson.path("overallScore").asInt())
                    .feedback(aiJson.path("feedback").asText())
                    .matchedSkills(aiJson.path("matchedSkills").asText())
                    .missingSkills(aiJson.path("missingSkills").asText())
                    .experienceMatch(aiJson.path("experienceMatch").asText())
                    .recommendation(aiJson.path("recommendation").asText())
                    .interviewTips(aiJson.path("interviewTips").asText())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            AIScreeningResponse errorResponse = AIScreeningResponse.builder()
                    .overallScore(0)
                    .feedback("Error: " + e.getMessage())
                    .matchedSkills("")
                    .missingSkills("")
                    .experienceMatch("")
                    .recommendation("ERROR")
                    .interviewTips("")
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Resume Screener API is running!");
    }
}