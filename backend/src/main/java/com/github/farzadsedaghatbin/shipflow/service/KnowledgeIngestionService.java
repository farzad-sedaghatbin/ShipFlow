package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.config.QAConfig;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.QAFeedbackType;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for ingesting knowledge from various entities into the vector store. */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.features.qa.enabled", havingValue = "true")
public class KnowledgeIngestionService {

  private final KnowledgeItemRepository knowledgeItemRepository;
  private final QAInteractionRepository qaInteractionRepository;
  private final EmbeddingModel embeddingModel;
  private final EmbeddingStore<TextSegment> embeddingStore;
  private final QAConfig qaConfig;

  // Repositories for fetching entity context
  private final PitchRepository pitchRepository;
  private final MeetingRepository meetingRepository;
  private final WorkLogRepository workLogRepository;
  private final TeamRepository teamRepository;
  private final CycleRepository cycleRepository;
  private final EvidenceRepository evidenceRepository;
  private final ManualNoteRepository manualNoteRepository;

  @Autowired
  public KnowledgeIngestionService(
      KnowledgeItemRepository knowledgeItemRepository,
      QAInteractionRepository qaInteractionRepository,
      @Autowired(required = false) EmbeddingModel embeddingModel,
      @Autowired(required = false) EmbeddingStore<TextSegment> embeddingStore,
      @Autowired(required = false) QAConfig qaConfig,
      PitchRepository pitchRepository,
      MeetingRepository meetingRepository,
      WorkLogRepository workLogRepository,
      TeamRepository teamRepository,
      CycleRepository cycleRepository,
      EvidenceRepository evidenceRepository,
      ManualNoteRepository manualNoteRepository) {
    this.knowledgeItemRepository = knowledgeItemRepository;
    this.qaInteractionRepository = qaInteractionRepository;
    this.embeddingModel = embeddingModel;
    this.embeddingStore = embeddingStore;
    this.qaConfig = qaConfig;
    this.pitchRepository = pitchRepository;
    this.meetingRepository = meetingRepository;
    this.workLogRepository = workLogRepository;
    this.teamRepository = teamRepository;
    this.cycleRepository = cycleRepository;
    this.evidenceRepository = evidenceRepository;
    this.manualNoteRepository = manualNoteRepository;
  }

  /** Ingest a pitch into the knowledge base. */
  @Transactional
  @Async
  public void ingestPitch(Long pitchId) {
    if (!isQAEnabled()) return;

    Optional<Pitch> pitchOpt = pitchRepository.findById(pitchId);
    if (pitchOpt.isEmpty()) {
      log.warn("Pitch not found for ingestion: {}", pitchId);
      return;
    }

    Pitch pitch = pitchOpt.get();
    String content = buildPitchContent(pitch);

    ingestEntity(
        KnowledgeEntityType.PITCH,
        pitch.getId(),
        pitch.getTitle(),
        content,
        pitch.getCycle().getId(),
        pitch.getTeam() != null ? pitch.getTeam().getId() : null,
        pitch.getId(),
        null);

    log.info("Ingested pitch: {} (ID: {})", pitch.getTitle(), pitch.getId());
  }

  /** Ingest a meeting into the knowledge base. */
  @Transactional
  @Async
  public void ingestMeeting(Long meetingId) {
    if (!isQAEnabled()) return;

    Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);
    if (meetingOpt.isEmpty()) {
      log.warn("Meeting not found for ingestion: {}", meetingId);
      return;
    }

    Meeting meeting = meetingOpt.get();
    String content = buildMeetingContent(meeting);

    Long cycleId = meeting.getPitch() != null ? meeting.getPitch().getCycle().getId() : null;
    Long teamId =
        meeting.getPitch() != null && meeting.getPitch().getTeam() != null
            ? meeting.getPitch().getTeam().getId()
            : null;
    Long pitchId = meeting.getPitch() != null ? meeting.getPitch().getId() : null;

    ingestEntity(
        KnowledgeEntityType.MEETING,
        meeting.getId(),
        "Meeting: " + meeting.getType() + " - " + meeting.getDateHeld(),
        content,
        cycleId,
        teamId,
        pitchId,
        null);

    log.info("Ingested meeting: {} (ID: {})", meeting.getType(), meeting.getId());
  }

  /** Ingest a work log into the knowledge base. */
  @Transactional
  @Async
  public void ingestWorkLog(Long workLogId) {
    if (!isQAEnabled()) return;

    Optional<WorkLog> workLogOpt = workLogRepository.findById(workLogId);
    if (workLogOpt.isEmpty()) {
      log.warn("WorkLog not found for ingestion: {}", workLogId);
      return;
    }

    WorkLog workLog = workLogOpt.get();

    // Only ingest work logs with notes
    if (workLog.getNote() == null || workLog.getNote().trim().isEmpty()) {
      log.debug("Skipping worklog without note: {}", workLogId);
      return;
    }

    String content = buildWorkLogContent(workLog);

    ingestEntity(
        KnowledgeEntityType.WORKLOG,
        workLog.getId(),
        "Work Log: " + workLog.getPerson().getName() + " - " + workLog.getDate(),
        content,
        workLog.getPitch().getCycle().getId(),
        workLog.getPitch().getTeam() != null ? workLog.getPitch().getTeam().getId() : null,
        workLog.getPitch().getId(),
        null);

    log.info("Ingested work log: ID: {}", workLog.getId());
  }

  /** Ingest evidence into the knowledge base. */
  @Transactional
  @Async
  public void ingestEvidence(Long evidenceId) {
    if (!isQAEnabled()) return;

    Optional<Evidence> evidenceOpt = evidenceRepository.findById(evidenceId);
    if (evidenceOpt.isEmpty()) {
      log.warn("Evidence not found for ingestion: {}", evidenceId);
      return;
    }

    Evidence evidence = evidenceOpt.get();
    String content = buildEvidenceContent(evidence);

    ingestEntity(
        KnowledgeEntityType.EVIDENCE,
        evidence.getId(),
        "Evidence: " + evidence.getDate(),
        content,
        evidence.getPitch().getCycle().getId(),
        evidence.getPitch().getTeam() != null ? evidence.getPitch().getTeam().getId() : null,
        evidence.getPitch().getId(),
        evidence.getPerson().getId());

    log.info("Ingested evidence: ID: {}", evidence.getId());
  }

  /** Ingest a manual note into the knowledge base. */
  @Transactional
  @Async
  public void ingestManualNote(Long noteId) {
    if (!isQAEnabled()) return;

    Optional<ManualNote> noteOpt = manualNoteRepository.findById(noteId);
    if (noteOpt.isEmpty()) {
      log.warn("Manual note not found for ingestion: {}", noteId);
      return;
    }

    ManualNote note = noteOpt.get();

    if (!note.getIncludeInKnowledge()) {
      log.debug("Skipping note not marked for knowledge inclusion: {}", noteId);
      return;
    }

    String content = buildManualNoteContent(note);

    ingestEntity(
        KnowledgeEntityType.MANUAL_NOTE,
        note.getId(),
        note.getTitle(),
        content,
        note.getCycleId(),
        note.getTeamId(),
        note.getPitchId(),
        note.getAuthorId());

    log.info("Ingested manual note: {} (ID: {})", note.getTitle(), note.getId());
  }

  /**
   * Ingest a reference document (like Shape Up methodology book) into the knowledge base. Reference
   * documents are external materials that provide context for the Q&A system.
   *
   * @param sourceId Unique identifier for the reference (e.g., "shape-up-methodology")
   * @param title Display title for the reference
   * @param content The full text content of the document
   * @return The number of chunks created
   */
  @Transactional
  public int ingestReferenceDocument(String sourceId, String title, String content) {
    if (!isQAEnabled()) {
      log.warn("Q&A feature is disabled, cannot ingest reference document");
      return 0;
    }

    if (content == null || content.trim().isEmpty()) {
      log.warn("Reference document has no content, skipping: {}", sourceId);
      return 0;
    }

    // Use a hash of the sourceId as a pseudo entity ID for consistency
    Long entityId = (long) sourceId.hashCode();

    String formattedContent = buildReferenceDocumentContent(sourceId, title, content);

    ingestEntity(
        KnowledgeEntityType.REFERENCE_DOCUMENT,
        entityId,
        title,
        formattedContent,
        null, // No cycle association
        null, // No team association
        null, // No pitch association
        null // No author
        );

    // Count how many chunks were created
    int chunkCount =
        knowledgeItemRepository
            .findByEntityTypeAndEntityId(KnowledgeEntityType.REFERENCE_DOCUMENT, entityId)
            .size();

    log.info("Ingested reference document: {} ({} chunks)", title, chunkCount);
    return chunkCount;
  }

  /** Build content for a reference document with metadata. */
  private String buildReferenceDocumentContent(String sourceId, String title, String content) {
    StringBuilder sb = new StringBuilder();
    sb.append("=== Reference Document ===\n");
    sb.append("Source: ").append(sourceId).append("\n");
    sb.append("Title: ").append(title).append("\n");
    sb.append("Type: Shape Up Methodology Reference\n\n");
    sb.append("=== Content ===\n");
    sb.append(content);
    return sb.toString();
  }

  /** Check if a reference document is already ingested. */
  public boolean isReferenceDocumentIngested(String sourceId) {
    Long entityId = (long) sourceId.hashCode();
    return !knowledgeItemRepository
        .findByEntityTypeAndEntityId(KnowledgeEntityType.REFERENCE_DOCUMENT, entityId)
        .isEmpty();
  }

  /**
   * Ingest an uploaded document into the knowledge base. Uses REQUIRES_NEW propagation to prevent
   * rollback of parent transaction if embedding fails.
   */
  @Transactional(
      propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW,
      noRollbackFor = Exception.class)
  public void ingestDocument(UploadedDocument document) {
    if (!isQAEnabled()) return;

    if (document.getExtractedText() == null || document.getExtractedText().isEmpty()) {
      log.warn("Document has no extracted text, skipping: {}", document.getId());
      return;
    }

    String content = buildDocumentContent(document);
    Long cycleId = null;
    Long teamId = null;
    Long pitchId = null;

    // Resolve entity associations
    if ("PITCH".equals(document.getEntityType()) && document.getEntityId() != null) {
      pitchRepository
          .findById(document.getEntityId())
          .ifPresent(
              pitch -> {
                // Can't set outer variables, using ingestEntity directly below
              });
      Optional<Pitch> pitchOpt = pitchRepository.findById(document.getEntityId());
      if (pitchOpt.isPresent()) {
        Pitch pitch = pitchOpt.get();
        cycleId = pitch.getCycle() != null ? pitch.getCycle().getId() : null;
        teamId = pitch.getTeam() != null ? pitch.getTeam().getId() : null;
        pitchId = pitch.getId();
      }
    } else if ("MEETING".equals(document.getEntityType()) && document.getEntityId() != null) {
      Optional<Meeting> meetingOpt = meetingRepository.findById(document.getEntityId());
      if (meetingOpt.isPresent()) {
        Meeting meeting = meetingOpt.get();
        if (meeting.getPitch() != null) {
          pitchId = meeting.getPitch().getId();
          cycleId =
              meeting.getPitch().getCycle() != null ? meeting.getPitch().getCycle().getId() : null;
          teamId =
              meeting.getPitch().getTeam() != null ? meeting.getPitch().getTeam().getId() : null;
        }
      }
    } else if ("CYCLE".equals(document.getEntityType()) && document.getEntityId() != null) {
      cycleId = document.getEntityId();
    }

    ingestEntity(
        KnowledgeEntityType.DOCUMENT,
        document.getId(),
        "Document: " + document.getOriginalFileName(),
        content,
        cycleId,
        teamId,
        pitchId,
        document.getUploaderId());

    log.info("Ingested document: {} (ID: {})", document.getOriginalFileName(), document.getId());
  }

  /**
   * Ingest a pitch document directly into the knowledge base. Used when extracting pitch data from
   * uploaded documents. This makes pitch documents searchable via Q&A.
   */
  @Transactional
  public void ingestPitchDocument(
      String fileName,
      String extractedText,
      Long pitchId,
      String pitchTitle,
      Long uploaderId,
      String uploaderUsername) {
    if (!isQAEnabled()) return;

    if (extractedText == null || extractedText.trim().isEmpty()) {
      log.warn("Pitch document has no extracted text, skipping: {}", fileName);
      return;
    }

    Long cycleId = null;
    Long teamId = null;

    // Resolve pitch associations
    if (pitchId != null) {
      Optional<Pitch> pitchOpt = pitchRepository.findById(pitchId);
      if (pitchOpt.isPresent()) {
        Pitch pitch = pitchOpt.get();
        cycleId = pitch.getCycle() != null ? pitch.getCycle().getId() : null;
        teamId = pitch.getTeam() != null ? pitch.getTeam().getId() : null;
      }
    }

    String content = buildPitchDocumentContent(fileName, pitchTitle, extractedText);

    ingestEntity(
        KnowledgeEntityType.DOCUMENT,
        null, // No document ID yet as it's directly uploaded for extraction
        "Pitch Document: " + (pitchTitle != null ? pitchTitle : fileName),
        content,
        cycleId,
        teamId,
        pitchId,
        uploaderId);

    log.info("Ingested pitch document to knowledge base: {} (Pitch: {})", fileName, pitchTitle);
  }

  private String buildPitchDocumentContent(
      String fileName, String pitchTitle, String extractedText) {
    StringBuilder sb = new StringBuilder();
    sb.append("Pitch Document: ").append(fileName).append("\n");
    if (pitchTitle != null) {
      sb.append("Pitch Title: ").append(pitchTitle).append("\n");
    }
    sb.append("Type: PITCH_DOCUMENT\n");
    sb.append("\n--- Pitch Content ---\n");
    sb.append(extractedText);

    return sb.toString();
  }

  private String buildDocumentContent(UploadedDocument document) {
    StringBuilder sb = new StringBuilder();
    sb.append("Document: ").append(document.getOriginalFileName()).append("\n");
    sb.append("Type: ").append(document.getFileType().toUpperCase()).append("\n");

    if (document.getEntityType() != null) {
      sb.append("Associated with: ").append(document.getEntityType());
      if (document.getEntityId() != null) {
        sb.append(" #").append(document.getEntityId());
      }
      sb.append("\n");
    }

    sb.append("\n--- Document Content ---\n");
    sb.append(document.getExtractedText());

    return sb.toString();
  }

  /** Ingest a validated Q&A interaction as knowledge. */
  @Transactional
  public void ingestValidatedQA(Long interactionId) {
    if (!isQAEnabled()) return;

    Optional<QAInteraction> interactionOpt = qaInteractionRepository.findById(interactionId);
    if (interactionOpt.isEmpty()) {
      log.warn("QA interaction not found for ingestion: {}", interactionId);
      return;
    }

    QAInteraction interaction = interactionOpt.get();

    // Only ingest validated (accurate or corrected) interactions
    if (interaction.getFeedbackType() != QAFeedbackType.ACCURATE
        && interaction.getFeedbackType() != QAFeedbackType.CORRECTED) {
      log.debug("Skipping non-validated QA interaction: {}", interactionId);
      return;
    }

    String content = buildValidatedQAContent(interaction);

    ingestEntity(
        KnowledgeEntityType.VALIDATED_QA,
        interaction.getId(),
        "Q&A: " + truncate(interaction.getQuestion(), 100),
        content,
        interaction.getCycleId(),
        interaction.getTeamId(),
        null,
        interaction.getUserId());

    // Mark as embedded
    interaction.setIsEmbedded(true);
    qaInteractionRepository.save(interaction);

    log.info("Ingested validated Q&A: ID: {}", interaction.getId());
  }

  /** Re-index all knowledge items. */
  @Transactional
  public void reindexAllKnowledge() {
    if (!isQAEnabled()) {
      log.warn("Q&A feature is disabled, cannot reindex");
      return;
    }

    log.info("Starting full knowledge reindexing...");

    // Reindex pitches
    pitchRepository
        .findAll()
        .forEach(
            pitch -> {
              try {
                ingestPitch(pitch.getId());
              } catch (Exception e) {
                log.error("Failed to reindex pitch {}: {}", pitch.getId(), e.getMessage());
              }
            });

    // Reindex meetings
    meetingRepository
        .findAll()
        .forEach(
            meeting -> {
              try {
                ingestMeeting(meeting.getId());
              } catch (Exception e) {
                log.error("Failed to reindex meeting {}: {}", meeting.getId(), e.getMessage());
              }
            });

    // Reindex work logs with notes
    workLogRepository
        .findAll()
        .forEach(
            workLog -> {
              try {
                ingestWorkLog(workLog.getId());
              } catch (Exception e) {
                log.error("Failed to reindex work log {}: {}", workLog.getId(), e.getMessage());
              }
            });

    // Reindex evidence
    evidenceRepository
        .findAll()
        .forEach(
            evidence -> {
              try {
                ingestEvidence(evidence.getId());
              } catch (Exception e) {
                log.error("Failed to reindex evidence {}: {}", evidence.getId(), e.getMessage());
              }
            });

    // Reindex manual notes
    manualNoteRepository
        .findByIncludeInKnowledgeTrue()
        .forEach(
            note -> {
              try {
                ingestManualNote(note.getId());
              } catch (Exception e) {
                log.error("Failed to reindex note {}: {}", note.getId(), e.getMessage());
              }
            });

    // Reindex validated Q&A
    qaInteractionRepository
        .findValidatedNotEmbedded(List.of(QAFeedbackType.ACCURATE, QAFeedbackType.CORRECTED))
        .forEach(
            qa -> {
              try {
                ingestValidatedQA(qa.getId());
              } catch (Exception e) {
                log.error("Failed to reindex QA {}: {}", qa.getId(), e.getMessage());
              }
            });

    log.info("Knowledge reindexing completed");
  }

  /** Process pending embeddings - embed knowledge items that haven't been embedded yet. */
  @Transactional
  public int processPendingEmbeddings() {
    if (!isQAEnabled()) return 0;

    List<KnowledgeItem> pending = knowledgeItemRepository.findByIsEmbeddedFalse();
    int processed = 0;

    for (KnowledgeItem item : pending) {
      try {
        embedKnowledgeItem(item);
        processed++;
      } catch (Exception e) {
        log.error("Failed to embed knowledge item {}: {}", item.getId(), e.getMessage());
      }
    }

    log.info("Processed {} pending embeddings", processed);
    return processed;
  }

  // ===== Private helper methods =====

  private void ingestEntity(
      KnowledgeEntityType entityType,
      Long entityId,
      String title,
      String content,
      Long cycleId,
      Long teamId,
      Long pitchId,
      Long authorId) {

    String contentHash = computeHash(content);

    // Check if we need to update existing knowledge
    List<KnowledgeItem> existing =
        knowledgeItemRepository.findByEntityTypeAndEntityId(entityType, entityId);

    // If content hasn't changed, skip
    if (!existing.isEmpty()
        && existing.get(0).getContentHash() != null
        && existing.get(0).getContentHash().equals(contentHash)) {
      log.debug("Content unchanged for {} {}, skipping", entityType, entityId);
      return;
    }

    // Remove old knowledge items and embeddings
    if (!existing.isEmpty()) {
      List<String> oldEmbeddingIds =
          knowledgeItemRepository.findEmbeddingIdsByEntity(entityType, entityId);
      for (String embeddingId : oldEmbeddingIds) {
        try {
          // Note: In-memory store doesn't support deletion by ID
          // ChromaDB supports it
          log.debug("Would remove embedding: {}", embeddingId);
        } catch (Exception e) {
          log.warn("Failed to remove old embedding: {}", embeddingId);
        }
      }
      knowledgeItemRepository.deleteByEntityTypeAndEntityId(entityType, entityId);
    }

    // Split content into chunks
    List<TextSegment> chunks = splitIntoChunks(content, title);

    // Create knowledge items for each chunk
    for (int i = 0; i < chunks.size(); i++) {
      KnowledgeItem item =
          KnowledgeItem.builder()
              .entityType(entityType)
              .entityId(entityId)
              .title(title)
              .content(chunks.get(i).text())
              .cycleId(cycleId)
              .teamId(teamId)
              .pitchId(pitchId)
              .authorId(authorId)
              .chunkIndex(i)
              .totalChunks(chunks.size())
              .contentHash(i == 0 ? contentHash : null) // Only store hash on first chunk
              .isEmbedded(false)
              .build();

      item.setEmbeddingId(item.generateEmbeddingId());
      knowledgeItemRepository.save(item);

      // Embed immediately
      embedKnowledgeItem(item);
    }
  }

  private void embedKnowledgeItem(KnowledgeItem item) {
    if (embeddingModel == null || embeddingStore == null) {
      log.warn("Embedding model or store not available");
      return;
    }

    try {
      // Create metadata for the embedding
      Metadata metadata = Metadata.from("knowledgeItemId", item.getId().toString());
      metadata.put("entityType", item.getEntityType().name());
      metadata.put("entityId", item.getEntityId().toString());
      if (item.getCycleId() != null) {
        metadata.put("cycleId", item.getCycleId().toString());
      }
      if (item.getTeamId() != null) {
        metadata.put("teamId", item.getTeamId().toString());
      }
      if (item.getPitchId() != null) {
        metadata.put("pitchId", item.getPitchId().toString());
      }
      if (item.getTitle() != null) {
        metadata.put("title", item.getTitle());
      }

      TextSegment segment = TextSegment.from(item.getContent(), metadata);
      Embedding embedding = embeddingModel.embed(segment).content();

      // Store embedding with ID
      embeddingStore.add(embedding, segment);

      item.setIsEmbedded(true);
      knowledgeItemRepository.save(item);

      log.debug("Embedded knowledge item: {}", item.getEmbeddingId());
    } catch (Exception e) {
      log.error("Failed to embed knowledge item {}: {}", item.getId(), e.getMessage());
      throw e;
    }
  }

  private List<TextSegment> splitIntoChunks(String content, String title) {
    if (content == null || content.trim().isEmpty()) {
      return List.of();
    }

    int maxTokens = qaConfig != null ? qaConfig.getMaxTokens() : 512;
    int overlap = qaConfig != null ? qaConfig.getChunkOverlap() : 50;

    // Use LangChain4j's document splitter
    Document document = Document.from(content);
    var splitter = DocumentSplitters.recursive(maxTokens, overlap);

    return splitter.split(document);
  }

  private boolean isQAEnabled() {
    return embeddingModel != null && embeddingStore != null;
  }

  private String computeHash(String content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      log.warn("SHA-256 not available, using content length as hash");
      return String.valueOf(content.length());
    }
  }

  private String truncate(String text, int maxLength) {
    if (text == null) return "";
    return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
  }

  // ===== Content building methods =====

  private String buildPitchContent(Pitch pitch) {
    StringBuilder sb = new StringBuilder();
    sb.append("Pitch: ").append(pitch.getTitle()).append("\n\n");

    if (pitch.getDescription() != null) {
      sb.append("Description:\n").append(pitch.getDescription()).append("\n\n");
    }

    sb.append("Status: ").append(pitch.getStatus()).append("\n");
    sb.append("Appetite: ").append(pitch.getAppetiteDays()).append(" days\n");

    if (pitch.getCycle() != null) {
      sb.append("Cycle: ").append(pitch.getCycle().getName()).append("\n");
    }

    if (pitch.getTeam() != null) {
      sb.append("Team: ").append(pitch.getTeam().getName()).append("\n");
    }

    sb.append("Created: ")
        .append(pitch.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
        .append("\n");
    sb.append("Last Updated: ")
        .append(pitch.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
        .append("\n");

    return sb.toString();
  }

  private String buildMeetingContent(Meeting meeting) {
    StringBuilder sb = new StringBuilder();
    sb.append("Meeting: ").append(meeting.getType()).append("\n");
    sb.append("Date: ").append(meeting.getDateHeld()).append("\n\n");

    if (meeting.getPitch() != null) {
      sb.append("Related Pitch: ").append(meeting.getPitch().getTitle()).append("\n");
    }

    sb.append("Definition of Ready (DoR): ")
        .append(meeting.getDorReady() ? "Ready" : "Not Ready")
        .append("\n");
    sb.append("Definition of Done (DoD): ")
        .append(meeting.getDodReady() ? "Ready" : "Not Ready")
        .append("\n");

    if (meeting.getNotes() != null && !meeting.getNotes().isEmpty()) {
      sb.append("\nMeeting Notes (Minutes of Meeting):\n");
      sb.append(meeting.getNotes()).append("\n");
    }

    return sb.toString();
  }

  private String buildWorkLogContent(WorkLog workLog) {
    StringBuilder sb = new StringBuilder();
    sb.append("Work Log Entry\n");
    sb.append("Date: ").append(workLog.getDate()).append("\n");
    sb.append("Person: ").append(workLog.getPerson().getName()).append("\n");
    sb.append("Hours Spent: ").append(workLog.getHoursSpent()).append("\n");
    sb.append("Pitch: ").append(workLog.getPitch().getTitle()).append("\n\n");

    if (workLog.getNote() != null) {
      sb.append("Notes:\n").append(workLog.getNote()).append("\n");
    }

    return sb.toString();
  }

  private String buildEvidenceContent(Evidence evidence) {
    StringBuilder sb = new StringBuilder();
    sb.append("Evidence Entry\n");
    sb.append("Date: ").append(evidence.getDate()).append("\n");
    sb.append("Added by: ").append(evidence.getPerson().getName()).append("\n");
    sb.append("Pitch: ").append(evidence.getPitch().getTitle()).append("\n\n");

    sb.append("Description:\n").append(evidence.getDescription()).append("\n");

    if (evidence.getFileUrl() != null) {
      sb.append("\nAttachment: ").append(evidence.getFileUrl()).append("\n");
    }

    return sb.toString();
  }

  private String buildManualNoteContent(ManualNote note) {
    StringBuilder sb = new StringBuilder();
    sb.append("Note: ").append(note.getTitle()).append("\n");
    sb.append("Context: ").append(note.getContextType()).append("\n");
    sb.append("Created: ")
        .append(note.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
        .append("\n\n");
    sb.append(note.getContent()).append("\n");
    return sb.toString();
  }

  private String buildValidatedQAContent(QAInteraction interaction) {
    StringBuilder sb = new StringBuilder();
    sb.append("Validated Q&A\n\n");
    sb.append("Question: ").append(interaction.getQuestion()).append("\n\n");

    if (interaction.getFeedbackType() == QAFeedbackType.CORRECTED
        && interaction.getFeedbackCorrection() != null) {
      sb.append("Corrected Answer:\n").append(interaction.getFeedbackCorrection()).append("\n");
    } else {
      sb.append("Answer:\n").append(interaction.getAnswer()).append("\n");
    }

    if (interaction.getContextType() != null) {
      sb.append("\nContext: ").append(interaction.getContextType());
      if (interaction.getContextId() != null) {
        sb.append(" (ID: ").append(interaction.getContextId()).append(")");
      }
      sb.append("\n");
    }

    return sb.toString();
  }
}
