package com.paridhi.resume_screener.service;

import com.paridhi.resume_screener.dto.ScreeningRequest;
import com.paridhi.resume_screener.dto.ScreeningResponse;
import com.paridhi.resume_screener.model.ScreeningResult;
import com.paridhi.resume_screener.model.ScreeningResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScreeningService {

    private final ScreeningResultRepository repository;

    public ScreeningResponse screenResume(ScreeningRequest request) {

        // Step 1: Extract skills from resume and JD
        String resumeText = request.getResumeText().toLowerCase();
        String jdText = request.getJobDescription().toLowerCase();

        String[] commonSkills = {"java", "spring boot", "kafka", "rest api",
                "microservices", "sql", "python", "docker", "kubernetes",
                "redis", "postgresql", "mysql", "git", "ci/cd", "linux",
                "distributed systems", "system design", "agile", "jira",
                "multithreading", "react", "node.js", "golang", "aws",
                "gcp", "azure", "mongodb", "elasticsearch"};

        StringBuilder matched = new StringBuilder();
        StringBuilder missing = new StringBuilder();
        int matchCount = 0;
        int totalRequired = 0;

        for (String skill : commonSkills) {
            if (jdText.contains(skill)) {
                totalRequired++;
                if (resumeText.contains(skill)) {
                    matched.append(skill).append(", ");
                    matchCount++;
                } else {
                    missing.append(skill).append(", ");
                }
            }
        }

        // Step 2: Calculate score
        int score = totalRequired > 0 ? (matchCount * 100) / totalRequired : 0;

        // Step 3: Generate feedback
        String feedback;
        String recommendation;
        if (score >= 80) {
            feedback = "Strong match. Candidate covers most required skills.";
            recommendation = "RECOMMEND — proceed to interview";
        } else if (score >= 50) {
            feedback = "Moderate match. Some key skills are missing but fundamentals are strong.";
            recommendation = "MAYBE — review missing skills before deciding";
        } else {
            feedback = "Weak match. Significant skill gaps identified.";
            recommendation = "PASS — skill gap too large for this role";
        }

        // Step 4: Save to database
        ScreeningResult result = new ScreeningResult();
        result.setResumeText(request.getResumeText());
        result.setJobDescription(request.getJobDescription());
        result.setOverallScore(score);
        result.setFeedback(feedback);
        result.setMatchedSkills(matched.toString());
        result.setMissingSkills(missing.toString());
        repository.save(result);

        // Step 5: Return response
        return ScreeningResponse.builder()
                .overallScore(score)
                .feedback(feedback)
                .matchedSkills(matched.toString())
                .missingSkills(missing.toString())
                .recommendation(recommendation)
                .build();
    }
}