package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.github.farzadsedaghatbin.shipflow.service.KnowledgeIngestionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;

/**
 * Seeds the knowledge base with sample data for Q&A feature.
 * Runs after SampleDataInitializer (Order 2) to ensure data exists.
 */
@Component
@Slf4j
@Order(2) // Run after SampleDataInitializer  
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class KnowledgeSeeder implements CommandLineRunner {

    private final PitchRepository pitchRepository;
    private final MeetingRepository meetingRepository;
    private final WorkLogRepository workLogRepository;
    private final EvidenceRepository evidenceRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;
    
    @Autowired(required = false)
    private KnowledgeIngestionService knowledgeIngestionService;

    public KnowledgeSeeder(
            PitchRepository pitchRepository,
            MeetingRepository meetingRepository,
            WorkLogRepository workLogRepository,
            EvidenceRepository evidenceRepository,
            KnowledgeItemRepository knowledgeItemRepository) {
        this.pitchRepository = pitchRepository;
        this.meetingRepository = meetingRepository;
        this.workLogRepository = workLogRepository;
        this.evidenceRepository = evidenceRepository;
        this.knowledgeItemRepository = knowledgeItemRepository;
    }

    @Override
    public void run(String... args) {
        // Skip if knowledge already seeded
        if (knowledgeItemRepository.count() > 0) {
            log.info("Knowledge base already seeded, skipping initialization");
            return;
        }

        // Check if ingestion service is available
        if (knowledgeIngestionService == null) {
            log.warn("KnowledgeIngestionService not available, skipping knowledge seeding");
            return;
        }

        log.info("Seeding knowledge base with sample data...");

        try {
            // Ingest all pitches
            var pitches = pitchRepository.findAll();
            log.info("Ingesting {} pitches...", pitches.size());
            for (var pitch : pitches) {
                try {
                    knowledgeIngestionService.ingestPitch(pitch.getId());
                } catch (Exception e) {
                    log.warn("Failed to ingest pitch {}: {}", pitch.getId(), e.getMessage());
                }
            }

            // Ingest all meetings
            var meetings = meetingRepository.findAll();
            log.info("Ingesting {} meetings...", meetings.size());
            for (var meeting : meetings) {
                try {
                    knowledgeIngestionService.ingestMeeting(meeting.getId());
                } catch (Exception e) {
                    log.warn("Failed to ingest meeting {}: {}", meeting.getId(), e.getMessage());
                }
            }

            // Ingest all work logs
            var workLogs = workLogRepository.findAll();
            log.info("Ingesting {} work logs...", workLogs.size());
            for (var workLog : workLogs) {
                try {
                    knowledgeIngestionService.ingestWorkLog(workLog.getId());
                } catch (Exception e) {
                    log.warn("Failed to ingest work log {}: {}", workLog.getId(), e.getMessage());
                }
            }

            // Ingest all evidences
            var evidences = evidenceRepository.findAll();
            log.info("Ingesting {} evidences...", evidences.size());
            for (var evidence : evidences) {
                try {
                    knowledgeIngestionService.ingestEvidence(evidence.getId());
                } catch (Exception e) {
                    log.warn("Failed to ingest evidence {}: {}", evidence.getId(), e.getMessage());
                }
            }

            // Ingest Shape Up methodology reference document
            ingestShapeUpMethodology();

            long totalItems = knowledgeItemRepository.count();
            log.info("Knowledge base seeded successfully with {} items!", totalItems);

        } catch (Exception e) {
            log.error("Failed to seed knowledge base: {}", e.getMessage(), e);
        }
    }

    /**
     * Ingest the Shape Up methodology PDF into the knowledge base.
     * This provides the AI Q&A system with authoritative knowledge about Shape Up practices.
     */
    private void ingestShapeUpMethodology() {
        final String SOURCE_ID = "shape-up-methodology";
        final String TITLE = "Shape Up: Stop Running in Circles and Ship Work that Matters";
        final String PDF_PATH = "knowledgebase/shape-up.pdf";

        // Check if already ingested
        if (knowledgeIngestionService.isReferenceDocumentIngested(SOURCE_ID)) {
            log.info("Shape Up methodology already ingested, skipping");
            return;
        }

        try {
            ClassPathResource resource = new ClassPathResource(PDF_PATH);
            if (!resource.exists()) {
                log.warn("Shape Up PDF not found at classpath:{}, skipping methodology ingestion", PDF_PATH);
                return;
            }

            log.info("Extracting text from Shape Up methodology PDF...");
            String extractedText = extractTextFromPdf(resource.getInputStream());

            if (extractedText == null || extractedText.trim().isEmpty()) {
                log.warn("Could not extract text from Shape Up PDF");
                return;
            }

            log.info("Ingesting Shape Up methodology ({} characters)...", extractedText.length());
            int chunkCount = knowledgeIngestionService.ingestReferenceDocument(
                    SOURCE_ID,
                    TITLE,
                    extractedText
            );

            log.info("Shape Up methodology ingested successfully ({} knowledge chunks)", chunkCount);

        } catch (IOException e) {
            log.error("Failed to read Shape Up PDF: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to ingest Shape Up methodology: {}", e.getMessage(), e);
        }
    }

    /**
     * Extract text content from a PDF file.
     * For memory efficiency with large PDFs, load bytes into memory first.
     */
    private String extractTextFromPdf(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        }
    }
}
