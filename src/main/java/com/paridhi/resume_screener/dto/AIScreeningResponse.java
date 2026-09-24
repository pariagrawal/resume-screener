package com.paridhi.resume_screener.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class AIScreeningResponse {
    private int overallScore;
    private String feedback;
    private String matchedSkills;
    private String missingSkills;
    private String experienceMatch;
    private String recommendation;
    private String interviewTips;
}