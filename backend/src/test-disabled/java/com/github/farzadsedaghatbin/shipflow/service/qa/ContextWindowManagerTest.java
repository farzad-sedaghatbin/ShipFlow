package com.github.farzadsedaghatbin.shipflow.service.qa;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContextWindowManagerTest {

    private ContextWindowManager manager;

    @BeforeEach
    void setUp() {
        manager = new ContextWindowManager();
    }

    @Test
    void testEstimateTokens_emptyString() {
        int tokens = manager.estimateTokens("");
        assertEquals(0, tokens);
    }

    @Test
    void testEstimateTokens_shortString() {
        String text = "Hello World"; // 11 characters = ~3 tokens
        int tokens = manager.estimateTokens(text);
        assertEquals(3, tokens);
    }

    @Test
    void testEstimateTokens_longerString() {
        String text = "A".repeat(400); // 400 characters = 100 tokens
        int tokens = manager.estimateTokens(text);
        assertEquals(100, tokens);
    }

    @Test
    void testTruncateToTokens_noTruncationNeeded() {
        String text = "Short text";
        String result = manager.truncateToTokens(text, 100);
        assertEquals(text, result);
    }

    @Test
    void testTruncateToTokens_truncatesLongText() {
        String text = "A".repeat(1000); // 1000 chars = 250 tokens
        String result = manager.truncateToTokens(text, 100); // Limit to 100 tokens = 400 chars
        
        assertEquals(400, result.length());
        assertTrue(result.endsWith("..."));
    }

    @Test
    void testBuildManagedContext_emptyMatches() {
        ContextWindowManager.ContextResult result = manager.buildManagedContext(
            List.of(), "test question", 4000
        );
        
        assertEquals("", result.getContext());
        assertFalse(result.isWasTruncated());
        assertEquals(0, result.getIncludedDocuments());
    }

    @Test
    void testBuildManagedContext_fitsWithinBudget() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch("Short document 1"),
            createMatch("Short document 2")
        );
        
        ContextWindowManager.ContextResult result = manager.buildManagedContext(
            matches, "test question", 4000
        );
        
        assertFalse(result.getContext().isEmpty());
        assertFalse(result.isWasTruncated());
        assertEquals(2, result.getIncludedDocuments());
        assertTrue(result.getContext().contains("Short document 1"));
        assertTrue(result.getContext().contains("Short document 2"));
    }

    @Test
    void testBuildManagedContext_exceedsBudget_dropsDocuments() {
        // Create documents that together exceed the budget
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch("A".repeat(4000)), // ~1000 tokens
            createMatch("B".repeat(4000)), // ~1000 tokens
            createMatch("C".repeat(4000))  // ~1000 tokens
        );
        
        ContextWindowManager.ContextResult result = manager.buildManagedContext(
            matches, "test question", 1500  // Small budget - only ~1 doc + question
        );
        
        assertTrue(result.isWasTruncated());
        assertTrue(result.getIncludedDocuments() < 3);
    }

    @Test
    void testBuildManagedContext_truncatesIndividualDocument() {
        // Create a single very long document
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch("A".repeat(20000)) // ~5000 tokens
        );
        
        ContextWindowManager.ContextResult result = manager.buildManagedContext(
            matches, "test question", 2000  // Limit to 2000 tokens
        );
        
        assertTrue(result.isWasTruncated());
        assertEquals(1, result.getIncludedDocuments());
        assertTrue(result.getContext().contains("..."));
    }

    @Test
    void testBuildManagedContext_includesSourceLabels() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch("Document content")
        );
        
        ContextWindowManager.ContextResult result = manager.buildManagedContext(
            matches, "test question", 4000
        );
        
        assertTrue(result.getContext().contains("[Source "));
        assertTrue(result.getContext().contains("Document content"));
    }

    @Test
    void testBuildManagedContext_handlesMissingEntityType() {
        TextSegment segment = TextSegment.from("No metadata");
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.8, "id1", segment, null);
        
        ContextWindowManager.ContextResult result = manager.buildManagedContext(
            List.of(match), "test question", 4000
        );
        
        assertFalse(result.getContext().isEmpty());
        assertTrue(result.getContext().contains("No metadata"));
    }

    @Test
    void testBuildManagedContext_reservesTokensForQuestion() {
        // Create documents that would fit if we didn't reserve space for the question
        String longQuestion = "A".repeat(2000); // ~500 tokens
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch("B".repeat(8000)) // ~2000 tokens
        );
        
        ContextWindowManager.ContextResult result = manager.buildManagedContext(
            matches, longQuestion, 2000  // Total budget 2000 tokens
        );
        
        // Should reserve space for question, so document should be truncated
        assertTrue(result.isWasTruncated());
    }

    @Test
    void testBuildManagedContext_prioritizesFirstDocuments() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch("First doc - should be included"),
            createMatch("Second doc - should be included"),
            createMatch("Third doc - might be dropped")
        );
        
        ContextWindowManager.ContextResult result = manager.buildManagedContext(
            matches, "test", 100  // Very small budget
        );
        
        // First doc should always be included
        assertTrue(result.getContext().contains("First doc"));
    }

    @Test
    void testBuildManagedContext_formatsWithEntityType() {
        TextSegment segment = TextSegment.from("Pitch content", Map.of(
            "entityType", "PITCH",
            "entityId", "123"
        ));
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.9, "id1", segment, null);
        
        ContextWindowManager.ContextResult result = manager.buildManagedContext(
            List.of(match), "test question", 4000
        );
        
        assertTrue(result.getContext().contains("PITCH"));
        assertTrue(result.getContext().contains("Pitch content"));
    }

    // Helper method
    private EmbeddingMatch<TextSegment> createMatch(String text) {
        TextSegment segment = TextSegment.from(text, Map.of(
            "entityType", "GENERIC",
            "entityId", "test-" + text.hashCode()
        ));
        return new EmbeddingMatch<>(0.8, "id-" + text.hashCode(), segment, null);
    }
}
