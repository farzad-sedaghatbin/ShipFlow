package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.DocumentUploadResponse;
import com.github.farzadsedaghatbin.shipflow.dto.ExtractedPitchDataDTO;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.DocumentService;
import com.github.farzadsedaghatbin.shipflow.service.KnowledgeIngestionService;
import com.github.farzadsedaghatbin.shipflow.service.PitchShapingExtractorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST controller for document upload and management.
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Documents", description = "Document upload and management for knowledge extraction")
public class DocumentController {

    private final DocumentService documentService;
    private final UserRepository userRepository;
    private final PitchShapingExtractorService pitchShapingExtractorService;
    
    @Autowired(required = false)
    private KnowledgeIngestionService knowledgeIngestionService;

    /**
     * Upload a document and extract its text content.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload document", description = "Upload a document (PDF, DOCX, TXT) and extract its text for knowledge base")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        String username = userDetails.getUsername();

        DocumentUploadResponse response = documentService.uploadDocument(
                file, entityType, entityId, userId, username);

        if (response.getErrorMessage() != null) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Upload a document for a pitch.
     */
    @PostMapping(value = "/pitch/{pitchId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload document for pitch", description = "Upload a document for a pitch and extract text for Q&A")
    public ResponseEntity<DocumentUploadResponse> uploadDocumentForPitch(
            @PathVariable Long pitchId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        String username = userDetails.getUsername();

        DocumentUploadResponse response = documentService.uploadDocument(
                file, "PITCH", pitchId, userId, username);

        if (response.getErrorMessage() != null) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Upload a document for a meeting.
     */
    @PostMapping(value = "/meeting/{meetingId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload document for meeting", description = "Upload a document for a meeting (e.g., meeting notes) and extract text for Q&A")
    public ResponseEntity<DocumentUploadResponse> uploadDocumentForMeeting(
            @PathVariable Long meetingId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        String username = userDetails.getUsername();

        DocumentUploadResponse response = documentService.uploadDocument(
                file, "MEETING", meetingId, userId, username);

        if (response.getErrorMessage() != null) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Upload a document for a cycle.
     */
    @PostMapping(value = "/cycle/{cycleId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload document for cycle", description = "Upload a document for a cycle and extract text for Q&A")
    public ResponseEntity<DocumentUploadResponse> uploadDocumentForCycle(
            @PathVariable Long cycleId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        String username = userDetails.getUsername();

        DocumentUploadResponse response = documentService.uploadDocument(
                file, "CYCLE", cycleId, userId, username);

        if (response.getErrorMessage() != null) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get documents for an entity.
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get entity documents", description = "Get all documents for a specific entity")
    public ResponseEntity<List<UploadedDocument>> getDocumentsByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(documentService.getDocumentsByEntity(entityType.toUpperCase(), entityId));
    }

    /**
     * Get documents for a pitch.
     */
    @GetMapping("/pitch/{pitchId}")
    @Operation(summary = "Get pitch documents", description = "Get all documents for a pitch")
    public ResponseEntity<List<UploadedDocument>> getDocumentsForPitch(@PathVariable Long pitchId) {
        return ResponseEntity.ok(documentService.getDocumentsByEntity("PITCH", pitchId));
    }

    /**
     * Get documents for a meeting.
     */
    @GetMapping("/meeting/{meetingId}")
    @Operation(summary = "Get meeting documents", description = "Get all documents for a meeting")
    public ResponseEntity<List<UploadedDocument>> getDocumentsForMeeting(@PathVariable Long meetingId) {
        return ResponseEntity.ok(documentService.getDocumentsByEntity("MEETING", meetingId));
    }

    /**
     * Get a single document.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get document", description = "Get a document by ID")
    public ResponseEntity<UploadedDocument> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    /**
     * Delete a document.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete document", description = "Delete a document")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
    }

    /**
     * Index pending documents for Q&A.
     */
    @PostMapping("/index-pending")
    @Operation(summary = "Index pending documents", description = "Index documents that haven't been added to knowledge base yet")
    public ResponseEntity<Map<String, Object>> indexPendingDocuments() {
        int indexed = documentService.indexPendingDocuments();
        return ResponseEntity.ok(Map.of(
                "message", "Documents indexed successfully",
                "indexedCount", indexed
        ));
    }

    /**
     * Extract pitch shaping data from an uploaded document using AI.
     * This endpoint analyzes the document and extracts Shape Up methodology elements.
     * Also adds the document to the knowledge base for Q&A.
     */
    @PostMapping(value = "/extract-pitch-data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Extract pitch data from document", 
               description = "Upload a pitch document (PDF, DOCX, TXT) and extract Shape Up elements like problem statement, solution, rabbit holes, and risks using AI. Also adds to knowledge base for Q&A.")
    public ResponseEntity<ExtractedPitchDataDTO> extractPitchDataFromDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "pitchId", required = false) Long pitchId,
            @RequestParam(value = "addToKnowledgeBase", required = false, defaultValue = "true") boolean addToKnowledgeBase,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            // First extract text from the document
            String extractedText = documentService.extractTextFromFile(file);
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ExtractedPitchDataDTO.builder()
                        .extractionSuccessful(false)
                        .errorMessage("Could not extract text from document")
                        .build());
            }
            
            // Use AI to extract pitch data
            PitchShapingExtractorService.ExtractedPitchData extracted = 
                    pitchShapingExtractorService.extractFromDocument(extractedText);
            
            // Add to knowledge base if enabled and QA service is available
            if (addToKnowledgeBase && knowledgeIngestionService != null) {
                try {
                    String pitchTitle = extracted.title() != null ? extracted.title() : file.getOriginalFilename();
                    Long userId = userDetails != null ? getUserId(userDetails) : null;
                    String username = userDetails != null ? userDetails.getUsername() : "system";
                    
                    knowledgeIngestionService.ingestPitchDocument(
                        file.getOriginalFilename(),
                        extractedText,
                        pitchId,
                        pitchTitle,
                        userId,
                        username
                    );
                    
                    log.info("Added pitch document to knowledge base: {}", pitchTitle);
                } catch (Exception e) {
                    log.warn("Failed to add pitch document to knowledge base: {}", e.getMessage());
                    // Don't fail the extraction if knowledge base ingestion fails
                }
            }
            
            return ResponseEntity.ok(ExtractedPitchDataDTO.builder()
                    .title(extracted.title())
                    .problemStatement(extracted.problemStatement())
                    .solution(extracted.solution())
                    .rabbitHoles(extracted.rabbitHoles())
                    .risks(extracted.risks())
                    .noGos(extracted.noGos())
                    .appetiteDays(extracted.appetiteDays())
                    .wireframeLinks(extracted.wireframeLinks())
                    .extractionSuccessful(extracted.extractionSuccessful())
                    .errorMessage(extracted.errorMessage())
                    .build());
                    
        } catch (Exception e) {
            log.error("Error extracting pitch data: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ExtractedPitchDataDTO.builder()
                    .extractionSuccessful(false)
                    .errorMessage("Error processing document: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Check if AI pitch extraction is available.
     */
    @GetMapping("/extract-pitch-data/status")
    @Operation(summary = "Check extraction availability", description = "Check if AI-powered pitch data extraction is available")
    public ResponseEntity<Map<String, Object>> getExtractionStatus() {
        boolean available = pitchShapingExtractorService.isExtractionAvailable();
        return ResponseEntity.ok(Map.of(
                "available", available,
                "message", available ? "AI extraction is available" : "AI extraction is not configured. Please enable AI in application settings."
        ));
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
