package com.paridhi.resume_screener.cli;

import com.paridhi.resume_screener.dto.AIScreeningResponse;
import com.paridhi.resume_screener.dto.ScreeningRequest;
import com.paridhi.resume_screener.dto.ScreeningResponse;
import com.paridhi.resume_screener.service.FileExtractionService;
import com.paridhi.resume_screener.service.GeminiService;
import com.paridhi.resume_screener.service.ResultFileService;
import com.paridhi.resume_screener.service.ScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class CliRunner implements CommandLineRunner {

    private final FileExtractionService fileExtractionService;
    private final ScreeningService screeningService;
    private final GeminiService geminiService;
    private final ResultFileService resultFileService;
    private final Environment environment;

    @Override
    public void run(String... args) throws Exception {
        // No CLI args → normal server mode, do nothing
        if (args.length == 0) {
            return;
        }

        // CLI mode: expects 4 args
        if (args.length != 4) {
            printUsage();
            System.exit(1);
        }

        String mode = args[0].toLowerCase();
        String resumePath = args[1];
        String jdPath = args[2];
        String outputPath = args[3];

        // Validate mode
        if (!mode.equals("keyword") && !mode.equals("ai")) {
            System.err.println("Error: Mode must be 'keyword' or 'ai'. You entered: '" + mode + "'");
            printUsage();
            System.exit(1);
        }

        // Validate input files
        if (!new File(resumePath).exists()) {
            System.err.println("Error: Resume file not found: " + resumePath);
            System.exit(1);
        }
        if (!new File(jdPath).exists()) {
            System.err.println("Error: Job description file not found: " + jdPath);
            System.exit(1);
        }

        System.out.println();
        System.out.println("=== Resume Screener CLI ===");
        System.out.println("  Mode   : " + mode);
        System.out.println("  Resume : " + resumePath);
        System.out.println("  JD     : " + jdPath);
        System.out.println("  Output : " + outputPath);
        System.out.println("===========================");
        System.out.println();

        try {
            // Extract text from files
            String resumeText = Files.readString(Path.of(resumePath));
            String jdText = Files.readString(Path.of(jdPath));

            String resultContent;

            if (mode.equals("keyword")) {
                System.out.println("Running keyword screening...");
                ScreeningRequest request = new ScreeningRequest();
                request.setResumeText(resumeText);
                request.setJobDescription(jdText);

                ScreeningResponse response = screeningService.screenResume(request);
                resultContent = resultFileService.generateKeywordResultFile(response);

            } else {
                System.out.println("Running AI screening (Gemini)...");
                String rawResponse = geminiService.analyzeResume(resumeText, jdText);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(rawResponse);

                JsonNode candidates = root.path("candidates");
                if (!candidates.isArray() || candidates.isEmpty()) {
                    System.err.println("Error: Gemini returned no candidates.");
                    System.err.println("Raw response: " + rawResponse);
                    System.exit(1);
                }

                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (!parts.isArray() || parts.isEmpty()) {
                    System.err.println("Error: Gemini returned no parts.");
                    System.err.println("Raw response: " + rawResponse);
                    System.exit(1);
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

                resultContent = resultFileService.generateAIResultFile(response);
            }

            // Write result to output file
            try (FileWriter writer = new FileWriter(outputPath)) {
                writer.write(resultContent);
            }

            System.out.println();
            System.out.println("Done! Result saved to: " + outputPath);
            System.out.println();

            // Print preview
            String[] lines = resultContent.split("\n");
            int previewLines = Math.min(lines.length, 20);
            System.out.println("── Preview ──────────────────────────");
            for (int i = 0; i < previewLines; i++) {
                System.out.println(lines[i]);
            }
            System.out.println("─────────────────────────────────────");

        } catch (Exception e) {
            System.err.println("Error during screening: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

    private void printUsage() {
        System.out.println();
        System.out.println("=== Resume Screener CLI ===");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar resume-screener.jar <mode> <resume> <jd> <output>");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  mode     'keyword' - Fast keyword-based screening");
        System.out.println("           'ai'      - AI-powered screening (Gemini)");
        System.out.println("  resume   Path to resume file (PDF, DOCX, or TXT)");
        System.out.println("  jd       Path to job description file");
        System.out.println("  output   Path where the result will be saved");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar resume-screener.jar keyword ~/resume.pdf ~/jd.txt ~/result.txt");
        System.out.println("  java -jar resume-screener.jar ai ~/Documents/resume.docx ~/Documents/jd.pdf ~/output.txt");
        System.out.println();
    }
}