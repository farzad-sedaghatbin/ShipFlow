package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.DocumentUploadResponse;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
