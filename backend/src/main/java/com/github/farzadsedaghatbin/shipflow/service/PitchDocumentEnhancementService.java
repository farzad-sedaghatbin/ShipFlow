package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.service.PitchShapingExtractorService.ExtractedPitchData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for enhancing pitch content from associated documents using AI extraction.
 *
 * <p>This service supports the Shape Up workflow by allowing pitches to be enriched
 * with structured content extracted from uploaded documents (PDFs, Word docs, etc.).
 * It's particularly useful during the shaping process (IDEA → DRAFT → SHAPED) where
 * product managers may upload pitch narratives, research documents, or design specs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PitchDocumentEnhancementService {

  private final PitchRepository pitchRepository;
  private final DocumentService documentService;
  private final PitchShapingExtractorService extractorService;

  /** DTO for enhancement results. */
  public record EnhancementResult(
      Long pitchId,
      String pitchTitle,
      boolean success,
      String message,
      int documentsProcessed,
      List<String> enhancedFields,
      PitchDTO updatedPitch) {

    public static EnhancementResult success(Long pitchId, String title, int docsProcessed,
        List<String> fields, PitchDTO pitch) {
      return new EnhancementResult(pitchId, title, true,
          "Successfully enhanced pitch from " + docsProcessed + " document(s)", docsProcessed, fields, pitch);
    }

    public static EnhancementResult error(Long pitchId, String title, String errorMessage) {
      return new EnhancementResult(pitchId, title, false, errorMessage, 0, List.of(), null);
    }

    public static EnhancementResult noDocuments(Long pitchId, String title) {
      return new EnhancementResult(pitchId, title, false,
          "No documents found associated with this pitch", 0, List.of(), null);
    }

    public static EnhancementResult noTextExtracted(Long pitchId, String title, int docsProcessed) {
      return new EnhancementResult(pitchId, title, false,
          "No extractable text found in " + docsProcessed + " document(s)", docsProcessed, List.of(), null);
    }
  }

  /** Options for controlling enhancement behavior. */
  public record EnhancementOptions(
      boolean overwriteExisting,
      boolean autoAdvanceStatus) {

    public static EnhancementOptions defaults() {
      return new EnhancementOptions(false, false);
    }

    public static EnhancementOptions overwrite() {
      return new EnhancementOptions(true, false);
    }

    public static EnhancementOptions overwriteAndAdvance() {
      return new EnhancementOptions(true, true);
    }
  }

  /**
   * Enhance a pitch by extracting content from its associated documents.
   *
   * @param pitchId The ID of the pitch to enhance
   * @param options Enhancement options controlling merge behavior
   * @return EnhancementResult with details of the operation
   */
  @Transactional
  public EnhancementResult enhancePitchFromDocuments(Long pitchId, EnhancementOptions options) {
    log.info("Starting pitch enhancement for pitch ID: {} with options: {}", pitchId, options);

    // Verify AI extraction is available
    if (!extractorService.isExtractionAvailable()) {
      return EnhancementResult.error(pitchId, null,
          "AI extraction is not available. Please configure AI settings.");
    }

    // Find the pitch
    Pitch pitch = pitchRepository.findById(pitchId)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + pitchId));

    // Get associated documents
    List<UploadedDocument> documents = documentService.getDocumentsByEntity("PITCH", pitchId);

    if (documents.isEmpty()) {
      log.info("No documents found for pitch ID: {}", pitchId);
      return EnhancementResult.noDocuments(pitchId, pitch.getTitle());
    }

    // Collect extracted text from all documents
    List<String> documentTexts = documents.stream()
        .filter(doc -> doc.isTextExtracted() && doc.getExtractedText() != null)
        .map(UploadedDocument::getExtractedText)
        .filter(text -> !text.trim().isEmpty())
        .collect(Collectors.toList());

    if (documentTexts.isEmpty()) {
      log.info("No extractable text found in {} documents for pitch ID: {}", documents.size(), pitchId);
      return EnhancementResult.noTextExtracted(pitchId, pitch.getTitle(), documents.size());
    }

    // Extract pitch data from documents using AI
    ExtractedPitchData extractedData = extractorService.extractFromMultipleDocuments(documentTexts);

    if (!extractedData.extractionSuccessful()) {
      log.warn("AI extraction failed for pitch ID: {} - {}", pitchId, extractedData.errorMessage());
      return EnhancementResult.error(pitchId, pitch.getTitle(),
          "AI extraction failed: " + extractedData.errorMessage());
    }

    // Apply extracted data to pitch
    List<String> enhancedFields = applyExtractedData(pitch, extractedData, options);

    if (enhancedFields.isEmpty()) {
      log.info("No fields enhanced for pitch ID: {} (all fields already populated)", pitchId);
      return new EnhancementResult(pitchId, pitch.getTitle(), true,
          "No new data extracted (all fields already populated)", documentTexts.size(), List.of(), toDTO(pitch));
    }

    // Optionally advance status during shaping
    if (options.autoAdvanceStatus() && pitch.getStatus() == PitchStatus.IDEA) {
      pitch.setStatus(PitchStatus.DRAFT);
      enhancedFields = new java.util.ArrayList<>(enhancedFields);
      enhancedFields.add("status");
      log.info("Auto-advanced pitch {} from IDEA to DRAFT", pitchId);
    }

    pitch.setUpdatedAt(LocalDateTime.now());
    Pitch saved = pitchRepository.save(pitch);

    log.info("Successfully enhanced pitch ID: {} - fields updated: {}", pitchId, enhancedFields);

    return EnhancementResult.success(pitchId, saved.getTitle(), documentTexts.size(), enhancedFields, toDTO(saved));
  }

  /**
   * Enhance a pitch using provided document text directly (without looking up stored documents).
   *
   * @param pitchId The ID of the pitch to enhance
   * @param documentText The document text to extract from
   * @param options Enhancement options
   * @return EnhancementResult
   */
  @Transactional
  public EnhancementResult enhancePitchFromText(Long pitchId, String documentText, EnhancementOptions options) {
    log.info("Starting pitch enhancement from direct text for pitch ID: {}", pitchId);

    if (!extractorService.isExtractionAvailable()) {
      return EnhancementResult.error(pitchId, null,
          "AI extraction is not available. Please configure AI settings.");
    }

    if (documentText == null || documentText.trim().isEmpty()) {
      return EnhancementResult.error(pitchId, null, "Document text is empty");
    }

    Pitch pitch = pitchRepository.findById(pitchId)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + pitchId));

    ExtractedPitchData extractedData = extractorService.extractFromDocument(documentText);

    if (!extractedData.extractionSuccessful()) {
      return EnhancementResult.error(pitchId, pitch.getTitle(),
          "AI extraction failed: " + extractedData.errorMessage());
    }

    List<String> enhancedFields = applyExtractedData(pitch, extractedData, options);

    if (enhancedFields.isEmpty()) {
      return new EnhancementResult(pitchId, pitch.getTitle(), true,
          "No new data extracted", 1, List.of(), toDTO(pitch));
    }

    if (options.autoAdvanceStatus() && pitch.getStatus() == PitchStatus.IDEA) {
      pitch.setStatus(PitchStatus.DRAFT);
      enhancedFields = new java.util.ArrayList<>(enhancedFields);
      enhancedFields.add("status");
    }

    pitch.setUpdatedAt(LocalDateTime.now());
    Pitch saved = pitchRepository.save(pitch);

    return EnhancementResult.success(pitchId, saved.getTitle(), 1, enhancedFields, toDTO(saved));
  }

  /**
   * Apply extracted data to pitch fields.
   *
   * @param pitch The pitch to update
   * @param data The extracted data
   * @param options Enhancement options controlling overwrite behavior
   * @return List of field names that were enhanced
   */
  private List<String> applyExtractedData(Pitch pitch, ExtractedPitchData data, EnhancementOptions options) {
    List<String> enhancedFields = new java.util.ArrayList<>();
    boolean overwrite = options.overwriteExisting();

    // Title - only update if overwrite or currently empty/generic
    if (data.title() != null && (overwrite || isEmpty(pitch.getTitle()))) {
      pitch.setTitle(data.title());
      enhancedFields.add("title");
    }

    // Problem Statement
    if (data.problemStatement() != null && (overwrite || isEmpty(pitch.getProblemStatement()))) {
      pitch.setProblemStatement(data.problemStatement());
      enhancedFields.add("problemStatement");
    }

    // Solution
    if (data.solution() != null && (overwrite || isEmpty(pitch.getSolution()))) {
      pitch.setSolution(data.solution());
      enhancedFields.add("solution");
    }

    // Rabbit Holes
    if (data.rabbitHoles() != null && (overwrite || isEmpty(pitch.getRabbitHoles()))) {
      pitch.setRabbitHoles(data.rabbitHoles());
      enhancedFields.add("rabbitHoles");
    }

    // Risks
    if (data.risks() != null && (overwrite || isEmpty(pitch.getRisks()))) {
      pitch.setRisks(data.risks());
      enhancedFields.add("risks");
    }

    // No-Gos
    if (data.noGos() != null && (overwrite || isEmpty(pitch.getNoGos()))) {
      pitch.setNoGos(data.noGos());
      enhancedFields.add("noGos");
    }

    // Appetite Days
    if (data.appetiteDays() != null && (overwrite || pitch.getAppetiteDays() == null)) {
      pitch.setAppetiteDays(data.appetiteDays());
      enhancedFields.add("appetiteDays");
    }

    // Wireframe Links
    if (data.wireframeLinks() != null && (overwrite || isEmpty(pitch.getWireframeLinks()))) {
      pitch.setWireframeLinks(data.wireframeLinks());
      enhancedFields.add("wireframeLinks");
    }

    return enhancedFields;
  }

  private boolean isEmpty(String value) {
    return value == null || value.trim().isEmpty();
  }

  /** Check if AI enhancement is available. */
  public boolean isEnhancementAvailable() {
    return extractorService.isExtractionAvailable();
  }

  /** Convert Pitch entity to DTO (simplified for enhancement result). */
  private PitchDTO toDTO(Pitch pitch) {
    return PitchDTO.builder()
        .id(pitch.getId())
        .title(pitch.getTitle())
        .description(pitch.getDescription())
        .status(pitch.getStatus())
        .appetiteDays(pitch.getAppetiteDays())
        .cycleId(pitch.getCycle() != null ? pitch.getCycle().getId() : null)
        .cycleName(pitch.getCycle() != null ? pitch.getCycle().getName() : null)
        .teamId(pitch.getTeam() != null ? pitch.getTeam().getId() : null)
        .teamName(pitch.getTeam() != null ? pitch.getTeam().getName() : null)
        .epicId(pitch.getEpic() != null ? pitch.getEpic().getId() : null)
        .epicName(pitch.getEpic() != null ? pitch.getEpic().getName() : null)
        .targetReleaseId(pitch.getTargetRelease() != null ? pitch.getTargetRelease().getId() : null)
        .targetReleaseName(pitch.getTargetRelease() != null ? pitch.getTargetRelease().getName() : null)
        .targetReleaseVersion(pitch.getTargetRelease() != null ? pitch.getTargetRelease().getVersion() : null)
        .problemStatement(pitch.getProblemStatement())
        .solution(pitch.getSolution())
        .rabbitHoles(pitch.getRabbitHoles())
        .risks(pitch.getRisks())
        .noGos(pitch.getNoGos())
        .wireframeLinks(pitch.getWireframeLinks())
        .isCircuitBreakerTriggered(pitch.getIsCircuitBreakerTriggered())
        .circuitBreakerReason(pitch.getCircuitBreakerReason())
        .circuitBreakerDate(pitch.getCircuitBreakerDate())
        .createdAt(pitch.getCreatedAt())
        .updatedAt(pitch.getUpdatedAt())
        .build();
  }
}
