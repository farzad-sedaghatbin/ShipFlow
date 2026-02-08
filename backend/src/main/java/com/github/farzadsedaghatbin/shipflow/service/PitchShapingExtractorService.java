package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for extracting Shape Up pitch narrative elements from documents using
 * AI.
 *
 * <p>
 * This service uses LLM to analyze pitch documents and extract: - Problem
 * Statement: What problem is being solved - Solution: The proposed solution
 * (hatched solution / fat-marker sketches) - Rabbit Holes: Edge cases and areas
 * to avoid - Risks: Known risks and unknowns - No-Gos: Things explicitly out of
 * scope - Wireframe/Prototype links: Visual references
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PitchShapingExtractorService {

  @Autowired(required = false)
  private ChatLanguageModel chatLanguageModel;

  private final ObjectMapper objectMapper;

  private static final String EXTRACTION_PROMPT = """
      You are an expert at analyzing Shape Up methodology pitch documents.

      Analyze the following document and extract the key pitch elements according to Shape Up methodology.

      Extract the following elements (use null if not found in the document):

      1. **title**: A concise title for the pitch (extract or generate from content)
      2. **problemStatement**: The problem being solved. What is the pain point? Why does this matter?
      3. **solution**: The proposed solution. What is being built? Include any described approach or fat-marker sketch ideas.
      4. **rabbitHoles**: Areas to avoid, edge cases that should be out of scope, potential time sinks.
      5. **risks**: Known risks, technical challenges, unknowns, or dependencies.
      6. **noGos**: Things explicitly out of scope. What we are NOT building.
      7. **appetiteDays**: Estimated time appetite in days (if mentioned). Default to null if not specified.
      8. **wireframeLinks**: Any mentioned links to wireframes, prototypes, Figma, or visual references.

      DOCUMENT CONTENT:
      ---
      %s
      ---

      Respond ONLY with a valid JSON object in this exact format (no markdown, no explanation):
      {
          "title": "string or null",
          "problemStatement": "string or null",
          "solution": "string or null",
          "rabbitHoles": "string or null",
          "risks": "string or null",
          "noGos": "string or null",
          "appetiteDays": number or null,
          "wireframeLinks": "string or null"
      }
      """;

  /** DTO for extracted pitch data from documents. */
  public record ExtractedPitchData(String title, String problemStatement, String solution, String rabbitHoles,
      String risks, String noGos, Integer appetiteDays, String wireframeLinks, boolean extractionSuccessful,
      String errorMessage) {
    public static ExtractedPitchData empty() {
      return new ExtractedPitchData(null, null, null, null, null, null, null, null, false, "No data extracted");
    }

    public static ExtractedPitchData error(String message) {
      return new ExtractedPitchData(null, null, null, null, null, null, null, null, false, message);
    }
  }

  /**
   * Extract pitch shaping data from document text using AI.
   *
   * @param documentText
   *            The extracted text from the uploaded document
   * @return ExtractedPitchData containing the parsed pitch elements
   */
  public ExtractedPitchData extractFromDocument(String documentText) {
    if (chatLanguageModel == null) {
      log.warn("AI model not available - pitch extraction disabled");
      return ExtractedPitchData.error("AI extraction is not enabled. Please configure AI settings.");
    }

    if (documentText == null || documentText.trim().isEmpty()) {
      return ExtractedPitchData.error("Document text is empty");
    }

    try {
      // Truncate very long documents to avoid token limits
      String truncatedText = truncateText(documentText, 8000);

      String prompt = String.format(EXTRACTION_PROMPT, truncatedText);

      log.info("Extracting pitch data from document ({} chars)", documentText.length());

      String response = chatLanguageModel.generate(prompt);

      log.debug("AI response: {}", response);

      // Parse the JSON response
      return parseResponse(response);

    } catch (Exception e) {
      log.error("Error extracting pitch data from document: {}", e.getMessage(), e);
      return ExtractedPitchData.error("Failed to extract pitch data: " + e.getMessage());
    }
  }

  /**
   * Extract pitch data from multiple document texts. Combines content from all
   * documents before extraction.
   */
  public ExtractedPitchData extractFromMultipleDocuments(java.util.List<String> documentTexts) {
    if (documentTexts == null || documentTexts.isEmpty()) {
      return ExtractedPitchData.error("No documents provided");
    }

    // Combine all document texts
    StringBuilder combined = new StringBuilder();
    for (int i = 0; i < documentTexts.size(); i++) {
      combined.append("=== DOCUMENT ").append(i + 1).append(" ===\n");
      combined.append(documentTexts.get(i)).append("\n\n");
    }

    return extractFromDocument(combined.toString());
  }

  private ExtractedPitchData parseResponse(String response) {
    try {
      // Clean up the response - remove markdown code blocks if present
      String cleanedResponse = response.trim();
      if (cleanedResponse.startsWith("```json")) {
        cleanedResponse = cleanedResponse.substring(7);
      } else if (cleanedResponse.startsWith("```")) {
        cleanedResponse = cleanedResponse.substring(3);
      }
      if (cleanedResponse.endsWith("```")) {
        cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
      }
      cleanedResponse = cleanedResponse.trim();

      JsonNode node = objectMapper.readTree(cleanedResponse);

      return new ExtractedPitchData(getTextOrNull(node, "title"), getTextOrNull(node, "problemStatement"),
          getTextOrNull(node, "solution"), getTextOrNull(node, "rabbitHoles"), getTextOrNull(node, "risks"),
          getTextOrNull(node, "noGos"), getIntOrNull(node, "appetiteDays"),
          getTextOrNull(node, "wireframeLinks"), true, null);

    } catch (Exception e) {
      log.error("Failed to parse AI response: {}", e.getMessage());
      return ExtractedPitchData.error("Failed to parse extraction results: " + e.getMessage());
    }
  }

  private String getTextOrNull(JsonNode node, String field) {
    JsonNode fieldNode = node.get(field);
    if (fieldNode == null || fieldNode.isNull() || fieldNode.asText().equals("null")) {
      return null;
    }
    String text = fieldNode.asText().trim();
    return text.isEmpty() ? null : text;
  }

  private Integer getIntOrNull(JsonNode node, String field) {
    JsonNode fieldNode = node.get(field);
    if (fieldNode == null || fieldNode.isNull()) {
      return null;
    }
    if (fieldNode.isNumber()) {
      return fieldNode.asInt();
    }
    try {
      return Integer.parseInt(fieldNode.asText());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String truncateText(String text, int maxChars) {
    if (text.length() <= maxChars) {
      return text;
    }
    return text.substring(0, maxChars) + "\n\n[... content truncated ...]";
  }

  /** Check if AI extraction is available. */
  public boolean isExtractionAvailable() {
    return chatLanguageModel != null;
  }
}
