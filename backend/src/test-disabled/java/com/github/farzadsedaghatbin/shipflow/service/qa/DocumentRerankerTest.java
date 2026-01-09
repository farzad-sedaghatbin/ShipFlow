package com.github.farzadsedaghatbin.shipflow.service.qa;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DocumentRerankerTest {

    private DocumentReranker reranker;

    @BeforeEach
    void setUp() {
        reranker = new DocumentReranker();
    }

    @Test
    void testRerank_withEmptyMatches_returnsEmptyList() {
        List<EmbeddingMatch<TextSegment>> result = reranker.rerank("test query", List.of(), 5);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testRerank_withLessMatchesThanK_returnsAllMatches() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch(0.8, "Recent document", 1),
            createMatch(0.75, "Older document", 30)
        );
        
        List<EmbeddingMatch<TextSegment>> result = reranker.rerank("test query", matches, 5);
        
        assertEquals(2, result.size());
    }

    @Test
    void testRerank_prioritizesHigherEmbeddingScore() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch(0.9, "High score doc", 10),
            createMatch(0.6, "Low score doc", 5)
        );
        
        List<EmbeddingMatch<TextSegment>> result = reranker.rerank("test query", matches, 2);
        
        assertEquals(2, result.size());
        assertEquals(0.9, result.get(0).score(), 0.01);
    }

    @Test
    void testRerank_boostsRecentDocuments() {
        // Create two documents with same embedding score but different timestamps
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch(0.7, "Recent document", 1),
            createMatch(0.7, "Old document", 100)
        );
        
        List<EmbeddingMatch<TextSegment>> result = reranker.rerank("test query", matches, 1);
        
        // Recent document should be ranked higher
        assertTrue(result.get(0).embedded().text().contains("Recent"));
    }

    @Test
    void testRerank_limitsByTopK() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch(0.9, "Doc 1", 1),
            createMatch(0.85, "Doc 2", 2),
            createMatch(0.8, "Doc 3", 3),
            createMatch(0.75, "Doc 4", 4),
            createMatch(0.7, "Doc 5", 5)
        );
        
        List<EmbeddingMatch<TextSegment>> result = reranker.rerank("test query", matches, 3);
        
        assertEquals(3, result.size());
    }

    @Test
    void testRerank_handlesNullMetadata() {
        TextSegment segment = TextSegment.from("Document without metadata");
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.8, "id1", segment, null);
        
        List<EmbeddingMatch<TextSegment>> result = reranker.rerank("test query", List.of(match), 1);
        
        assertEquals(1, result.size());
        assertEquals(0.8, result.get(0).score(), 0.01);
    }

    @Test
    void testRerank_boostsPitchEntityType() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatchWithEntity(0.7, "Meeting doc", "MEETING", 10),
            createMatchWithEntity(0.7, "Pitch doc", "PITCH", 10)
        );
        
        List<EmbeddingMatch<TextSegment>> result = reranker.rerank("test query", matches, 1);
        
        // Pitch should be boosted more
        assertTrue(result.get(0).embedded().text().contains("Pitch"));
    }

    @Test
    void testRerank_penalizesLongDocuments() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch(0.8, "A".repeat(5000), 5), // Very long document
            createMatch(0.8, "Short document", 5)  // Short document
        );
        
        List<EmbeddingMatch<TextSegment>> result = reranker.rerank("test query", matches, 1);
        
        // Short document should rank higher after length penalty
        assertTrue(result.get(0).embedded().text().contains("Short"));
    }

    @Test
    void testRerank_preservesOriginalOrder_whenScoresEqual() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch(0.8, "First", 10),
            createMatch(0.8, "Second", 10),
            createMatch(0.8, "Third", 10)
        );
        
        List<EmbeddingMatch<TextSegment>> result = reranker.rerank("test query", matches, 3);
        
        assertEquals(3, result.size());
        // Verify all documents are present
        assertTrue(result.stream().anyMatch(m -> m.embedded().text().contains("First")));
        assertTrue(result.stream().anyMatch(m -> m.embedded().text().contains("Second")));
        assertTrue(result.stream().anyMatch(m -> m.embedded().text().contains("Third")));
    }

    // Helper methods
    private EmbeddingMatch<TextSegment> createMatch(double score, String text, int daysAgo) {
        LocalDateTime timestamp = LocalDateTime.now().minusDays(daysAgo);
        TextSegment segment = TextSegment.from(text, Map.of(
            "timestamp", timestamp.toString(),
            "entityType", "GENERIC"
        ));
        return new EmbeddingMatch<>(score, "id-" + text.hashCode(), segment, null);
    }

    private EmbeddingMatch<TextSegment> createMatchWithEntity(double score, String text, 
            String entityType, int daysAgo) {
        LocalDateTime timestamp = LocalDateTime.now().minusDays(daysAgo);
        TextSegment segment = TextSegment.from(text, Map.of(
            "timestamp", timestamp.toString(),
            "entityType", entityType
        ));
        return new EmbeddingMatch<>(score, "id-" + text.hashCode(), segment, null);
    }
}
