package com.ticketbox.backend.worker.ai;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class PdfExtractionService {

    public String extractText(String filePath) {
        log.info("Extracting text from PDF: {}", filePath);
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("PDF file not found: " + filePath);
        }

        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().length() < 20) {
                log.warn("Extracted text is empty or too short. Possibly an image-heavy PDF.");
                throw new UnprocessablePdfException("PDF contains insufficient extractable text (image-only or empty)");
            }

            return text.trim();
        } catch (IOException e) {
            log.error("Failed to load or parse PDF file: {}", filePath, e);
            throw new UnprocessablePdfException("Failed to read PDF file", e);
        }
    }
}
