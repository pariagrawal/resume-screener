package com.paridhi.resume_screener.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ScreeningResponse {
    private int overallScore;
    private String feedback;
    private String matchedSkills;
    private String missingSkills;
    private String recommendation;
}