package com.paridhi.resume_screener.dto;

import lombok.Data;

@Data
public class ScreeningRequest {
    private String resumeText;
    private String jobDescription;
}