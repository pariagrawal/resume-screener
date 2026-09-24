package com.paridhi.resume_screener.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.opencsv.CSVReader;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

@Service
public class FileExtractionService {

    public String extractText(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename().toLowerCase();

        if (filename.endsWith(".pdf")) {
            return extractFromPdf(file);
        } else if (filename.endsWith(".csv")) {
            return extractFromCsv(file);
        } else if (filename.endsWith(".txt")) {
            return new String(file.getBytes());
        } else {
            throw new IllegalArgumentException("Unsupported file type. Use PDF, CSV, or TXT.");
        }
    }

    private String extractFromPdf(MultipartFile file) throws Exception {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractFromCsv(MultipartFile file) throws Exception {
        StringBuilder text = new StringBuilder();
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();
            for (String[] row : rows) {
                text.append(String.join(" ", row)).append("\n");
            }
        }
        return text.toString();
    }

}