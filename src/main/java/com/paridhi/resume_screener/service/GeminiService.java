package com.paridhi.resume_screener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final WebClient webClient;
    private final String apiKey;

    public GeminiService(@Value("${gemini.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public String analyzeResume(String resumeText, String jobDescription) {
        String prompt = """
                You are an expert technical recruiter. Analyze this resume against the job description.
                
                RESUME:
                %s
                
                JOB DESCRIPTION:
                %s
                
                Respond in EXACTLY this JSON format (no markdown, no code blocks, just raw JSON):
                {
                    "overallScore": <0-100>,
                    "feedback": "<2-3 sentence overall assessment>",
                    "matchedSkills": "<comma-separated list of skills found in both resume and JD>",
                    "missingSkills": "<comma-separated list of skills in JD but not in resume>",
                    "experienceMatch": "<whether years of experience match the JD requirement>",
                    "recommendation": "<STRONG MATCH / MODERATE MATCH / WEAK MATCH — with one line reason>",
                    "interviewTips": "<2-3 areas the candidate should prepare for if interviewed>"
                }
                """.formatted(resumeText, jobDescription);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        try {
            String response = webClient.post()
                    .uri("/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return response;
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}