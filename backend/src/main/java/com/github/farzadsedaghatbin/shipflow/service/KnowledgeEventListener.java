package com.github.farzadsedaghatbin.shipflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** Event listener that triggers knowledge ingestion when relevant entities are modified. */
@Component
@Slf4j
public class KnowledgeEventListener {

  @Value("${app.features.qa.enabled:false}")
  private boolean qaEnabled;

  private final KnowledgeIngestionService knowledgeIngestionService;

  @Autowired
  public KnowledgeEventListener(
      @Autowired(required = false) KnowledgeIngestionService knowledgeIngestionService) {
    this.knowledgeIngestionService = knowledgeIngestionService;
  }

  /** Handle pitch created/updated events. */
  @Async
  @EventListener
  public void handlePitchEvent(PitchKnowledgeEvent event) {
    if (!qaEnabled || knowledgeIngestionService == null) return;

    log.debug("Handling pitch event for pitch ID: {}", event.getPitchId());
    try {
      knowledgeIngestionService.ingestPitch(event.getPitchId());
    } catch (Exception e) {
      log.error("Failed to ingest pitch {}: {}", event.getPitchId(), e.getMessage());
    }
  }

  /** Handle meeting created/updated events. */
  @Async
  @EventListener
  public void handleMeetingEvent(MeetingKnowledgeEvent event) {
    if (!qaEnabled || knowledgeIngestionService == null) return;

    log.debug("Handling meeting event for meeting ID: {}", event.getMeetingId());
    try {
      knowledgeIngestionService.ingestMeeting(event.getMeetingId());
    } catch (Exception e) {
      log.error("Failed to ingest meeting {}: {}", event.getMeetingId(), e.getMessage());
    }
  }

  /** Handle work log created/updated events. */
  @Async
  @EventListener
  public void handleWorkLogEvent(WorkLogKnowledgeEvent event) {
    if (!qaEnabled || knowledgeIngestionService == null) return;

    log.debug("Handling work log event for work log ID: {}", event.getWorkLogId());
    try {
      knowledgeIngestionService.ingestWorkLog(event.getWorkLogId());
    } catch (Exception e) {
      log.error("Failed to ingest work log {}: {}", event.getWorkLogId(), e.getMessage());
    }
  }

  /** Handle evidence created/updated events. */
  @Async
  @EventListener
  public void handleEvidenceEvent(EvidenceKnowledgeEvent event) {
    if (!qaEnabled || knowledgeIngestionService == null) return;

    log.debug("Handling evidence event for evidence ID: {}", event.getEvidenceId());
    try {
      knowledgeIngestionService.ingestEvidence(event.getEvidenceId());
    } catch (Exception e) {
      log.error("Failed to ingest evidence {}: {}", event.getEvidenceId(), e.getMessage());
    }
  }

  // ===== Event classes =====

  public static class PitchKnowledgeEvent {
    private final Long pitchId;

    public PitchKnowledgeEvent(Long pitchId) {
      this.pitchId = pitchId;
    }

    public Long getPitchId() {
      return pitchId;
    }
  }

  public static class MeetingKnowledgeEvent {
    private final Long meetingId;

    public MeetingKnowledgeEvent(Long meetingId) {
      this.meetingId = meetingId;
    }

    public Long getMeetingId() {
      return meetingId;
    }
  }

  public static class WorkLogKnowledgeEvent {
    private final Long workLogId;

    public WorkLogKnowledgeEvent(Long workLogId) {
      this.workLogId = workLogId;
    }

    public Long getWorkLogId() {
      return workLogId;
    }
  }

  public static class EvidenceKnowledgeEvent {
    private final Long evidenceId;

    public EvidenceKnowledgeEvent(Long evidenceId) {
      this.evidenceId = evidenceId;
    }

    public Long getEvidenceId() {
      return evidenceId;
    }
  }
}
