package com.github.farzadsedaghatbin.shipflow.service.qa;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SecurityDocumentFilterTest {

    private SecurityDocumentFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SecurityDocumentFilter();
    }

    @Test
    void testFilterByUserAccess_emptyMatches() {
        List<EmbeddingMatch<TextSegment>> result = filter.filterByUserAccess(
            List.of(), 1L, Set.of(), Set.of()
        );
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testFilterByUserAccess_publicDocuments() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch("Public doc", "PUBLIC", null, null)
        );
        
        List<EmbeddingMatch<TextSegment>> result = filter.filterByUserAccess(
            matches, 1L, Set.of(), Set.of()
        );
        
        assertEquals(1, result.size());
    }

    @Test
    void testFilterByUserAccess_teamDocuments_userInTeam() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch("Team doc", "TEAM", 100L, null)
        );
        
        List<EmbeddingMatch<TextSegment>> result = filter.filterByUserAccess(
            matches, 1L, Set.of(100L), Set.of()
        );
        
        assertEquals(1, result.size());
    }

    @Test
    void testFilterByUserAccess_teamDocuments_userNotInTeam() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch("Team doc", "TEAM", 100L, null)
        );
        
        List<EmbeddingMatch<TextSegment>> result = filter.filterByUserAccess(
            matches, 1L, Set.of(200L), Set.of() // User in different team
        );
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testFilterByUserAccess_projectDocuments_userInProject() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch("Project doc", "PROJECT", null, 500L)
        );
        
        List<EmbeddingMatch<TextSegment>> result = filter.filterByUserAccess(
            matches, 1L, Set.of(), Set.of(500L)
        );
        
        assertEquals(1, result.size());
    }

    @Test
    void testFilterByUserAccess_projectDocuments_userNotInProject() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch("Project doc", "PROJECT", null, 500L)
        );
        
        List<EmbeddingMatch<TextSegment>> result = filter.filterByUserAccess(
            matches, 1L, Set.of(), Set.of(600L) // User in different project
        );
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testFilterByUserAccess_privateDocuments_filtered() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch("Private doc", "PRIVATE", null, null)
        );
        
        List<EmbeddingMatch<TextSegment>> result = filter.filterByUserAccess(
            matches, 1L, Set.of(), Set.of()
        );
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testFilterByUserAccess_mixedVisibility() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch("Public doc", "PUBLIC", null, null),
            createMatch("Team doc", "TEAM", 100L, null),
            createMatch("Private doc", "PRIVATE", null, null),
            createMatch("Project doc", "PROJECT", null, 500L)
        );
        
        List<EmbeddingMatch<TextSegment>> result = filter.filterByUserAccess(
            matches, 1L, Set.of(100L), Set.of(500L)
        );
        
        assertEquals(3, result.size()); // Public, Team, and Project docs
        assertTrue(result.stream().anyMatch(m -> m.embedded().text().contains("Public")));
        assertTrue(result.stream().anyMatch(m -> m.embedded().text().contains("Team")));
        assertTrue(result.stream().anyMatch(m -> m.embedded().text().contains("Project")));
        assertFalse(result.stream().anyMatch(m -> m.embedded().text().contains("Private")));
    }

    @Test
    void testFilterByUserAccess_missingMetadata_allowedByDefault() {
        TextSegment segment = TextSegment.from("No metadata doc");
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.8, "id1", segment, null);
        
        List<EmbeddingMatch<TextSegment>> result = filter.filterByUserAccess(
            List.of(match), 1L, Set.of(), Set.of()
        );
        
        assertEquals(1, result.size());
    }

    @Test
    void testFilterByUserAccess_multipleTeams() {
        List<EmbeddingMatch<TextSegment>> matches = List.of(
            createMatch("Team 1 doc", "TEAM", 100L, null),
            createMatch("Team 2 doc", "TEAM", 200L, null),
            createMatch("Team 3 doc", "TEAM", 300L, null)
        );
        
        List<EmbeddingMatch<TextSegment>> result = filter.filterByUserAccess(
            matches, 1L, Set.of(100L, 200L), Set.of()
        );
        
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(m -> m.embedded().text().contains("Team 1")));
        assertTrue(result.stream().anyMatch(m -> m.embedded().text().contains("Team 2")));
        assertFalse(result.stream().anyMatch(m -> m.embedded().text().contains("Team 3")));
    }

    @Test
    void testCanUserAccess_publicDocument() {
        assertTrue(filter.canUserAccess(
            createMatch("Public", "PUBLIC", null, null),
            1L, Set.of(), Set.of()
        ));
    }

    @Test
    void testCanUserAccess_teamDocument_withAccess() {
        assertTrue(filter.canUserAccess(
            createMatch("Team", "TEAM", 100L, null),
            1L, Set.of(100L), Set.of()
        ));
    }

    @Test
    void testCanUserAccess_teamDocument_noAccess() {
        assertFalse(filter.canUserAccess(
            createMatch("Team", "TEAM", 100L, null),
            1L, Set.of(200L), Set.of()
        ));
    }

    @Test
    void testCanUserAccess_projectDocument_withAccess() {
        assertTrue(filter.canUserAccess(
            createMatch("Project", "PROJECT", null, 500L),
            1L, Set.of(), Set.of(500L)
        ));
    }

    @Test
    void testCanUserAccess_projectDocument_noAccess() {
        assertFalse(filter.canUserAccess(
            createMatch("Project", "PROJECT", null, 500L),
            1L, Set.of(), Set.of(600L)
        ));
    }

    @Test
    void testCanUserAccess_privateDocument() {
        assertFalse(filter.canUserAccess(
            createMatch("Private", "PRIVATE", null, null),
            1L, Set.of(), Set.of()
        ));
    }

    @Test
    void testBuildSecurityFilter_noConstraints() {
        String filter = this.filter.buildSecurityFilter(Set.of(), Set.of());
        
        assertEquals("visibility:PUBLIC", filter);
    }

    @Test
    void testBuildSecurityFilter_withTeams() {
        String filter = this.filter.buildSecurityFilter(Set.of(100L, 200L), Set.of());
        
        assertTrue(filter.contains("visibility:PUBLIC"));
        assertTrue(filter.contains("teamId:100"));
        assertTrue(filter.contains("teamId:200"));
        assertTrue(filter.contains("OR"));
    }

    @Test
    void testBuildSecurityFilter_withProjects() {
        String filter = this.filter.buildSecurityFilter(Set.of(), Set.of(500L, 600L));
        
        assertTrue(filter.contains("visibility:PUBLIC"));
        assertTrue(filter.contains("projectId:500"));
        assertTrue(filter.contains("projectId:600"));
        assertTrue(filter.contains("OR"));
    }

    @Test
    void testBuildSecurityFilter_withTeamsAndProjects() {
        String filter = this.filter.buildSecurityFilter(Set.of(100L), Set.of(500L));
        
        assertTrue(filter.contains("visibility:PUBLIC"));
        assertTrue(filter.contains("teamId:100"));
        assertTrue(filter.contains("projectId:500"));
        assertTrue(filter.contains("OR"));
    }

    // Helper method
    private EmbeddingMatch<TextSegment> createMatch(String text, String visibility, 
            Long teamId, Long projectId) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("visibility", visibility);
        if (teamId != null) {
            metadata.put("teamId", teamId.toString());
        }
        if (projectId != null) {
            metadata.put("projectId", projectId.toString());
        }
        
        TextSegment segment = TextSegment.from(text, metadata);
        return new EmbeddingMatch<>(0.8, "id-" + text.hashCode(), segment, null);
    }
}
