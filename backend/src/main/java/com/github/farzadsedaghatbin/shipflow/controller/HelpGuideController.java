package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.service.HelpGuideAIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Help Guide AI search feature.
 * Completely separate from QAController to avoid coupling with business-logic
 * Q&A.
 */
@RestController
@RequestMapping("/api/help")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Help Guide AI", description = "AI-powered search for ShipFlow help documentation")
public class HelpGuideController {

    private final HelpGuideAIService helpGuideAIService;

    @GetMapping("/ai/status")
    @Operation(summary = "Check Help AI status", description = "Returns whether the Help Guide AI is available")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of("available", helpGuideAIService.isAvailable()));
    }

    @PostMapping("/ai/ask")
    @PreAuthorize("@permissionService.hasPermission('AI_FEATURES', 'READ')")
    @Operation(summary = "Ask a help question", description = "Ask a how-to question about ShipFlow features. Answers are guardrailed to only cover ShipFlow documentation.")
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question is required"));
        }

        Map<String, Object> response = helpGuideAIService.ask(question.trim());
        return ResponseEntity.ok(response);
    }
}
