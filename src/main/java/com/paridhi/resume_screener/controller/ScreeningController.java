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

            // Parse Gemini response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(rawResponse);
            String aiText = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // Clean up — remove markdown code blocks if present
            aiText = aiText.replace("```json", "").replace("```", "").trim();

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
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Resume Screener API is running!");
    }
}