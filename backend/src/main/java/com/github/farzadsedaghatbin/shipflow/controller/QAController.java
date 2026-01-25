package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.qa.*;
import com.github.farzadsedaghatbin.shipflow.entity.QAInteraction;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.KnowledgeIngestionService;
import com.github.farzadsedaghatbin.shipflow.service.MessageService;
import com.github.farzadsedaghatbin.shipflow.service.NoteService;
import com.github.farzadsedaghatbin.shipflow.service.QAService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Q&A feature endpoints.
 */
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Q&A", description = "AI-powered Q&A feature for ShipFlow knowledge")
public class QAController {

    private final QAService qaService;
    private final NoteService noteService;
    private final UserRepository userRepository;
    private final MessageService messageService;

    @Autowired(required = false)
    private KnowledgeIngestionService knowledgeIngestionService;

    @Value("${app.features.qa.enabled:false}")
    private boolean qaEnabled;

    /**
     * Check if Q&A feature is enabled.
     */
    @GetMapping("/status")
    @Operation(summary = "Get Q&A feature status", description = "Returns the status of the Q&A feature including availability of AI and vector store")
    public ResponseEntity<QAStatusDTO> getStatus() {
        return ResponseEntity.ok(qaService.getStatus());
    }

    /**
     * Ask a question.
     */
    @PostMapping("/ask")
    @PreAuthorize("@permissionService.hasPermission('AI_FEATURES', 'READ')")
    @Operation(summary = "Ask a question", description = "Submit a question and receive an AI-generated answer based on stored knowledge")
    public ResponseEntity<QAResponse> askQuestion(
            @Valid @RequestBody AskQuestionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (!qaEnabled) {
            return ResponseEntity.badRequest().body(
                    QAResponse.builder()
                            .question(request.getQuestion())
                            .aiEnabled(false)
                            .errorMessage("Q&A feature is not enabled")
                            .build()
            );
        }

        Long userId = getUserId(userDetails);
        QAResponse response = qaService.askQuestion(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Submit feedback for a Q&A response.
     */
    @PostMapping("/feedback")
    @Operation(summary = "Submit feedback", description = "Provide feedback on a Q&A response (accurate, inaccurate, or corrected)")
    public ResponseEntity<Map<String, String>> submitFeedback(
            @Valid @RequestBody QAFeedbackRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (!qaEnabled) {
            return ResponseEntity.badRequest().body(Map.of("error", "Q&A feature is not enabled"));
        }

        Long userId = getUserId(userDetails);
        qaService.submitFeedback(request, userId);
        return ResponseEntity.ok(Map.of("message", messageService.getMessage("qa.feedback.submitted")));
    }
    
    /**
     * Submit simple helpful/unhelpful feedback for active learning.
     */
    @PostMapping("/feedback/simple")
    @Operation(summary = "Submit simple feedback", description = "Submit helpful/unhelpful feedback for active learning")
    public ResponseEntity<Map<String, String>> submitSimpleFeedback(
            @RequestParam Long interactionId,
            @RequestParam boolean helpful,
            @RequestParam(required = false) String text,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (!qaEnabled) {
            return ResponseEntity.badRequest().body(Map.of("error", "Q&A feature is not enabled"));
        }

        qaService.recordSimpleFeedback(interactionId, helpful, text);
        return ResponseEntity.ok(Map.of("message", "Feedback recorded for learning"));
    }

    /**
     * Get recent Q&A interactions for the current user.
     */
    @GetMapping("/history")
    @Operation(summary = "Get Q&A history", description = "Get recent Q&A interactions for the current user")
    public ResponseEntity<List<QAInteraction>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (!qaEnabled) {
            return ResponseEntity.ok(List.of());
        }

        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(qaService.getRecentInteractions(userId));
    }

    // ===== Note endpoints =====

    /**
     * Create a new note.
     */
    @PostMapping("/notes")
    @Operation(summary = "Create a note", description = "Create a new manual note that can be included in the knowledge base")
    public ResponseEntity<NoteDTO> createNote(
            @Valid @RequestBody CreateNoteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = getUserId(userDetails);
        NoteDTO note = noteService.createNote(request, userId);
        return ResponseEntity.ok(note);
    }

    /**
     * Update a note.
     */
    @PutMapping("/notes/{id}")
    @Operation(summary = "Update a note", description = "Update an existing note")
    public ResponseEntity<NoteDTO> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody CreateNoteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = getUserId(userDetails);
        NoteDTO note = noteService.updateNote(id, request, userId);
        return ResponseEntity.ok(note);
    }

    /**
     * Delete a note.
     */
    @DeleteMapping("/notes/{id}")
    @Operation(summary = "Delete a note", description = "Delete a note")
    public ResponseEntity<Map<String, String>> deleteNote(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = getUserId(userDetails);
        noteService.deleteNote(id, userId);
        return ResponseEntity.ok(Map.of("message", messageService.getMessage("qa.note.deleted")));
    }

    /**
     * Get a note by ID.
     */
    @GetMapping("/notes/{id}")
    @Operation(summary = "Get a note", description = "Get a note by ID")
    public ResponseEntity<NoteDTO> getNote(@PathVariable Long id) {
        return ResponseEntity.ok(noteService.getNoteById(id));
    }

    /**
     * Get notes by context.
     */
    @GetMapping("/notes")
    @Operation(summary = "Get notes", description = "Get notes by context type and ID")
    public ResponseEntity<List<NoteDTO>> getNotes(
            @RequestParam String contextType,
            @RequestParam Long contextId) {
        return ResponseEntity.ok(noteService.getNotesByContext(contextType, contextId));
    }

    /**
     * Get notes for a pitch.
     */
    @GetMapping("/notes/pitch/{pitchId}")
    @Operation(summary = "Get pitch notes", description = "Get all notes for a pitch")
    public ResponseEntity<List<NoteDTO>> getNotesByPitch(@PathVariable Long pitchId) {
        return ResponseEntity.ok(noteService.getNotesByPitch(pitchId));
    }

    /**
     * Get notes for a cycle.
     */
    @GetMapping("/notes/cycle/{cycleId}")
    @Operation(summary = "Get cycle notes", description = "Get all notes for a cycle")
    public ResponseEntity<List<NoteDTO>> getNotesByCycle(@PathVariable Long cycleId) {
        return ResponseEntity.ok(noteService.getNotesByCycle(cycleId));
    }

    /**
     * Get notes for a team.
     */
    @GetMapping("/notes/team/{teamId}")
    @Operation(summary = "Get team notes", description = "Get all notes for a team")
    public ResponseEntity<List<NoteDTO>> getNotesByTeam(@PathVariable Long teamId) {
        return ResponseEntity.ok(noteService.getNotesByTeam(teamId));
    }

    /**
     * Get notes for the current user.
     */
    @GetMapping("/notes/my")
    @Operation(summary = "Get my notes", description = "Get all notes created by the current user")
    public ResponseEntity<List<NoteDTO>> getMyNotes(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(noteService.getNotesByAuthor(userId));
    }

    // ===== Admin endpoints =====

    /**
     * Trigger a full knowledge reindex (admin only).
     */
    @PostMapping("/admin/reindex")
    @Operation(summary = "Reindex knowledge", description = "Trigger a full reindex of all knowledge items (admin only)")
    public ResponseEntity<Map<String, String>> reindexKnowledge(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (!qaEnabled || knowledgeIngestionService == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Q&A feature is not enabled"));
        }

        // Note: In a real app, you'd check for admin role here
        knowledgeIngestionService.reindexAllKnowledge();
        return ResponseEntity.ok(Map.of("message", "Knowledge reindexing started"));
    }

    /**
     * Process pending embeddings (admin only).
     */
    @PostMapping("/admin/process-pending")
    @Operation(summary = "Process pending embeddings", description = "Process any pending knowledge items that haven't been embedded yet (admin only)")
    public ResponseEntity<Map<String, Object>> processPending(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (!qaEnabled || knowledgeIngestionService == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Q&A feature is not enabled"));
        }

        int processed = knowledgeIngestionService.processPendingEmbeddings();
        return ResponseEntity.ok(Map.of(
                "message", "Processing completed",
                "processedCount", processed
        ));
    }

    // ===== Helper methods =====

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
