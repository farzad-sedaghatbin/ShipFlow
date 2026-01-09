package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.github.farzadsedaghatbin.shipflow.service.KnowledgeIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the knowledge base with sample data for Q&A feature.
 * Runs after SampleDataInitializer (Order 2) to ensure data exists.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after SampleDataInitializer
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class KnowledgeSeeder implements CommandLineRunner {

    private final PitchRepository pitchRepository;
    private final MeetingRepository meetingRepository;
    private final WorkLogRepository workLogRepository;
    private final EvidenceRepository evidenceRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final KnowledgeIngestionService knowledgeIngestionService;

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

            long totalItems = knowledgeItemRepository.count();
            log.info("Knowledge base seeded successfully with {} items!", totalItems);

        } catch (Exception e) {
            log.error("Failed to seed knowledge base: {}", e.getMessage(), e);
        }
    }
}
