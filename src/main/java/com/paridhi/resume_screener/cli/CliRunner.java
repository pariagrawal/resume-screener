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
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CliRunner implements CommandLineRunner {

    private final FileExtractionService fileExtractionService;
    private final ScreeningService screeningService;
    private final GeminiService geminiService;
    private final ResultFileService resultFileService;

    private static final String[] SUPPORTED_EXTENSIONS = {".pdf", ".docx", ".txt", ".csv"};

    @Override
    public void run(String... args) throws Exception {
        // No CLI args → normal server mode
        if (args.length == 0) {
            return;
        }

        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        String mode = args[0].toLowerCase();
        String resumePath = null;
        String jdPath = null;
        String outputPath = null;

        // Check if using named flags (e.g., -resumePath ~/resumes/)
        if (args.length > 1 && args[1].startsWith("-")) {
            // Named flag mode
            for (int i = 1; i < args.length - 1; i += 2) {
                String flag = args[i].toLowerCase();
                String value = args[i + 1];
                switch (flag) {
                    case "-resumepath": resumePath = value; break;
                    case "-jdpath":     jdPath = value;     break;
                    case "-outputpath": outputPath = value;  break;
                    default:
                        System.err.println("Error: Unknown flag '" + args[i] + "'");
                        printUsage();
                        System.exit(1);
                }
            }
            // Validate all required flags are present
            if (resumePath == null || jdPath == null || outputPath == null) {
                System.err.println("Error: Missing required flags. All three are needed: -resumePath, -JDPath, -outputPath");
                printUsage();
                System.exit(1);
            }
        } else {
            // Positional mode (backward compatible)
            if (args.length != 4) {
                printUsage();
                System.exit(1);
            }
            resumePath = args[1];
            jdPath = args[2];
            outputPath = args[3];
        }

        // Validate mode
        if (!mode.equals("keyword") && !mode.equals("ai")) {
            System.err.println("Error: Mode must be 'keyword' or 'ai'. You entered: '" + mode + "'");
            printUsage();
            System.exit(1);
        }

        // Validate JD file
        File jdFile = new File(jdPath);
        if (!jdFile.exists() || !jdFile.isFile()) {
            System.err.println("Error: Job description file not found: " + jdPath);
            System.exit(1);
        }

        // Determine if resume path is a directory or a single file
        File resumeInput = new File(resumePath);
        if (!resumeInput.exists()) {
            System.err.println("Error: Resume path not found: " + resumePath);
            System.exit(1);
        }

        // Read JD text
        String jdText = Files.readString(Path.of(jdPath));

        if (resumeInput.isDirectory()) {
            // Batch mode: process all resumes in the directory
            processBatch(mode, resumeInput, jdText, outputPath);
        } else {
            // Single file mode
            processSingle(mode, resumeInput, jdText, outputPath);
        }

        System.exit(0);
    }

    private void processSingle(String mode, File resumeFile, String jdText, String outputPath) {
        System.out.println();
        System.out.println("=== Resume Screener CLI ===");
        System.out.println("  Mode   : " + mode);
        System.out.println("  Resume : " + resumeFile.getAbsolutePath());
        System.out.println("  Output : " + outputPath);
        System.out.println("===========================");
        System.out.println();

        try {
            String resumeText = Files.readString(resumeFile.toPath());
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
                resultContent = runAIScreening(resumeText, jdText);
            }

            writeToFile(outputPath, resultContent);
            System.out.println("\nDone! Result saved to: " + outputPath);
            printPreview(resultContent);

        } catch (Exception e) {
            System.err.println("Error during screening: " + e.getMessage());
            System.exit(1);
        }
    }

    private void processBatch(String mode, File resumeDir, String jdText, String outputPath) {
        // Get all supported resume files from directory
        File[] resumeFiles = resumeDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return Arrays.stream(SUPPORTED_EXTENSIONS).anyMatch(lower::endsWith);
        });

        if (resumeFiles == null || resumeFiles.length == 0) {
            System.err.println("Error: No supported resume files found in: " + resumeDir.getAbsolutePath());
            System.err.println("Supported formats: PDF, DOCX, TXT, CSV");
            System.exit(1);
        }

        // Sort by filename for consistent ordering
        Arrays.sort(resumeFiles, Comparator.comparing(File::getName));

        System.out.println();
        System.out.println("=== Resume Screener CLI (Batch Mode) ===");
        System.out.println("  Mode    : " + mode);
        System.out.println("  Resumes : " + resumeDir.getAbsolutePath() + " (" + resumeFiles.length + " files)");
        System.out.println("  Output  : " + outputPath);
        System.out.println("========================================");
        System.out.println();

        // Ensure output directory exists
        File outputDir = new File(outputPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        List<CandidateResult> results = new ArrayList<>();
        int processed = 0;
        int failed = 0;

        for (File resumeFile : resumeFiles) {
            processed++;
            System.out.printf("[%d/%d] Screening: %s ...%n", processed, resumeFiles.length, resumeFile.getName());

            try {
                String resumeText = Files.readString(resumeFile.toPath());
                String candidateName = extractCandidateName(resumeText, resumeFile.getName());

                if (mode.equals("keyword")) {
                    ScreeningRequest request = new ScreeningRequest();
                    request.setResumeText(resumeText);
                    request.setJobDescription(jdText);
                    ScreeningResponse response = screeningService.screenResume(request);
                    String resultContent = resultFileService.generateKeywordResultFile(response);

                    // Save individual result
                    String individualFile = outputPath + File.separator + "result_" + sanitizeFilename(resumeFile.getName()) + ".txt";
                    writeToFile(individualFile, resultContent);

                    results.add(new CandidateResult(candidateName, resumeFile.getName(),
                            response.getOverallScore(), getRecommendation(response.getOverallScore()), resultContent));

                } else {
                    String resultContent = runAIScreening(resumeText, jdText);

                    // Parse score from AI result
                    int score = parseAIScore(resultContent);

                    // Save individual result
                    String individualFile = outputPath + File.separator + "result_" + sanitizeFilename(resumeFile.getName()) + ".txt";
                    writeToFile(individualFile, resultContent);

                    results.add(new CandidateResult(candidateName, resumeFile.getName(),
                            score, getRecommendation(score), resultContent));
                }

                System.out.println("       ✓ Done");

            } catch (Exception e) {
                failed++;
                System.out.println("       ✗ Failed: " + e.getMessage());
                results.add(new CandidateResult(
                        resumeFile.getName(), resumeFile.getName(), 0, "ERROR", "Error: " + e.getMessage()));
            }
        }

        // Sort by score descending (highest first)
        results.sort((a, b) -> Integer.compare(b.score, a.score));

        // Generate summary report
        String summaryReport = generateSummaryReport(results, mode, resumeFiles.length, failed);
        String summaryFile = outputPath + File.separator + "SUMMARY_RANKING.txt";
        writeToFile(summaryFile, summaryReport);

        // Print summary
        System.out.println();
        System.out.println(summaryReport);
        System.out.println("Summary saved to: " + summaryFile);
        System.out.println("Individual results saved to: " + outputPath);
        System.out.println();
    }

    private String runAIScreening(String resumeText, String jdText) throws Exception {
        String rawResponse = geminiService.analyzeResume(resumeText, jdText);

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

        return resultFileService.generateAIResultFile(response);
    }

    private String extractCandidateName(String resumeText, String fallbackFileName) {
        // Try to extract name from first non-empty line of the resume
        String[] lines = resumeText.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && trimmed.length() < 60 && !trimmed.contains("@") && !trimmed.contains("http")) {
                return trimmed;
            }
        }
        // Fallback to filename without extension
        return fallbackFileName.replaceFirst("[.][^.]+$", "");
    }

    private String getRecommendation(int score) {
        if (score >= 75) return "STRONG MATCH";
        if (score >= 50) return "MODERATE MATCH";
        if (score >= 25) return "WEAK MATCH";
        return "NO MATCH";
    }

    private int parseAIScore(String resultContent) {
        // Try to extract score from the AI result text
        try {
            for (String line : resultContent.split("\n")) {
                if (line.contains("Overall Score") || line.contains("overall score") || line.contains("Match Score")) {
                    String numbers = line.replaceAll("[^0-9]", "");
                    if (!numbers.isEmpty()) {
                        return Integer.parseInt(numbers.substring(0, Math.min(numbers.length(), 3)));
                    }
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private String generateSummaryReport(List<CandidateResult> results, String mode, int total, int failed) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================\n");
        sb.append("         RESUME SCREENING - CANDIDATE RANKING\n");
        sb.append("========================================================\n");
        sb.append(String.format("  Mode           : %s\n", mode.toUpperCase()));
        sb.append(String.format("  Total Resumes  : %d\n", total));
        sb.append(String.format("  Processed      : %d\n", total - failed));
        if (failed > 0) {
            sb.append(String.format("  Failed         : %d\n", failed));
        }
        sb.append("========================================================\n\n");

        sb.append(String.format("%-6s %-30s %-8s %-18s%n", "RANK", "CANDIDATE", "SCORE", "RECOMMENDATION"));
        sb.append("─".repeat(65)).append("\n");

        int rank = 1;
        for (CandidateResult result : results) {
            if (!result.recommendation.equals("ERROR")) {
                sb.append(String.format("%-6d %-30s %-8s %-18s%n",
                        rank++,
                        truncate(result.candidateName, 28),
                        result.score + "%",
                        result.recommendation));
            } else {
                sb.append(String.format("%-6s %-30s %-8s %-18s%n",
                        "-",
                        truncate(result.candidateName, 28),
                        "N/A",
                        "ERROR"));
            }
        }

        sb.append("─".repeat(65)).append("\n");
        sb.append("\nIndividual detailed results are in the output directory.\n");
        sb.append("========================================================\n");

        return sb.toString();
    }

    private String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 2) + "..";
    }

    private String sanitizeFilename(String name) {
        return name.replaceFirst("[.][^.]+$", "").replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private void writeToFile(String path, String content) {
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(content);
        } catch (Exception e) {
            System.err.println("Error writing to " + path + ": " + e.getMessage());
        }
    }

    private void printPreview(String content) {
        String[] lines = content.split("\n");
        int previewLines = Math.min(lines.length, 20);
        System.out.println("\n── Preview ──────────────────────────");
        for (int i = 0; i < previewLines; i++) {
            System.out.println(lines[i]);
        }
        System.out.println("─────────────────────────────────────");
    }

    private void printUsage() {
        System.out.println();
        System.out.println("=== Resume Screener CLI ===");
        System.out.println();
        System.out.println("Usage (with named flags — recommended):");
        System.out.println("  java -jar resume-screener.jar <mode> -resumePath <path> -JDPath <path> -outputPath <path>");
        System.out.println();
        System.out.println("Usage (positional — also supported):");
        System.out.println("  java -jar resume-screener.jar <mode> <resume_path> <jd_path> <output_path>");
        System.out.println();
        System.out.println("Mode:");
        System.out.println("  keyword       Fast keyword-based screening");
        System.out.println("  ai            AI-powered screening (uses Gemini)");
        System.out.println();
        System.out.println("Flags:");
        System.out.println("  -resumePath   Path to a single resume file, OR a directory of resumes");
        System.out.println("  -JDPath       Path to the job description file");
        System.out.println("  -outputPath   Single mode: path for result file");
        System.out.println("                Batch mode:  directory for results + ranking");
        System.out.println();
        System.out.println("Examples:");
        System.out.println();
        System.out.println("  Single Resume:");
        System.out.println("  java -jar resume-screener.jar keyword -resumePath ~/resume.pdf -JDPath ~/jd.txt -outputPath ~/result.txt");
        System.out.println();
        System.out.println("  Batch (Directory of Resumes):");
        System.out.println("  java -jar resume-screener.jar keyword -resumePath ~/resumes/ -JDPath ~/jd.txt -outputPath ~/results/");
        System.out.println();
        System.out.println("Supported formats: PDF, DOCX, TXT, CSV");
        System.out.println();
    }

    // Inner class to hold candidate screening result for ranking
    private static class CandidateResult {
        String candidateName;
        String fileName;
        int score;
        String recommendation;
        String content;

        CandidateResult(String candidateName, String fileName, int score, String recommendation, String content) {
            this.candidateName = candidateName;
            this.fileName = fileName;
            this.score = score;
            this.recommendation = recommendation;
            this.content = content;
        }
    }
}