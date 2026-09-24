package com.paridhi.resume_screener.controller;

import com.paridhi.resume_screener.dto.AIScreeningResponse;
import com.paridhi.resume_screener.dto.ScreeningRequest;
import com.paridhi.resume_screener.dto.ScreeningResponse;
import com.paridhi.resume_screener.service.FileExtractionService;
import com.paridhi.resume_screener.service.GeminiService;
import com.paridhi.resume_screener.service.ResultFileService;
import com.paridhi.resume_screener.service.ScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/screen/upload")
@RequiredArgsConstructor
public class FileScreeningController {

    private final FileExtractionService fileExtractionService;
    private final ScreeningService screeningService;
    private final GeminiService geminiService;
    private final ResultFileService resultFileService;

    // Upload files → keyword screening → download .txt result
    @PostMapping("/keyword")
    public ResponseEntity<byte[]> keywordScreenUpload(
            @RequestParam("resume") MultipartFile resumeFile,
            @RequestParam("jd") MultipartFile jdFile) {
        try {
            String resumeText = fileExtractionService.extractText(resumeFile);
            String jdText = fileExtractionService.extractText(jdFile);

            ScreeningRequest request = new ScreeningRequest();
            request.setResumeText(resumeText);
            request.setJobDescription(jdText);

            ScreeningResponse response = screeningService.screenResume(request);
            String resultContent = resultFileService.generateKeywordResultFile(response);

            return buildFileResponse(resultContent, "screening_result_keyword.txt");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(("Error: " + e.getMessage()).getBytes());
        }
    }

    // Upload files → AI screening → download .txt result
    @PostMapping("/ai")
    public ResponseEntity<byte[]> aiScreenUpload(
            @RequestParam("resume") MultipartFile resumeFile,
            @RequestParam("jd") MultipartFile jdFile) {
        try {
            String resumeText = fileExtractionService.extractText(resumeFile);
            String jdText = fileExtractionService.extractText(jdFile);

            String rawResponse = geminiService.analyzeResume(resumeText, jdText);
            System.out.println("=== GEMINI RAW ===\n" + rawResponse + "\n=== END ===");

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

            String resultContent = resultFileService.generateAIResultFile(response);

            return buildFileResponse(resultContent, "screening_result_ai.txt");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(("Error: " + e.getMessage()).getBytes());
        }
    }

    private ResponseEntity<byte[]> buildFileResponse(String content, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.TEXT_PLAIN)
                .body(content.getBytes());
    }
}