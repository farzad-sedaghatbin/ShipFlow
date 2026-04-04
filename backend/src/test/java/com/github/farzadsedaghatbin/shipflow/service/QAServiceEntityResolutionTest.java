package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.qa.AskQuestionRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.QAInteractionRepository;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for QAService entity resolution logic:
 * resolveEntityFromQuestion, extractEntityIdFromQuestion,
 * extractEntityNameFromQuestion.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QAService – entity resolution")
class QAServiceEntityResolutionTest {

    @Mock
    private KnowledgeItemRepository knowledgeItemRepository;

    @Mock
    private QAInteractionRepository qaInteractionRepository;

    @Mock
    private AICacheService cacheService;

    @Mock
    private MessageService messageService;

    @Mock
    private CycleRepository cycleRepository;

    @InjectMocks
    private QAService qaService;

    // ── private method handles ──────────────────────────────────────────────────

    private Method extractEntityIdMethod;
    private Method extractEntityNameMethod;
    private Method resolveEntityMethod;

    @BeforeEach
    void setUp() throws Exception {
        extractEntityIdMethod = QAService.class.getDeclaredMethod(
            "extractEntityIdFromQuestion", String.class, String.class);
        extractEntityIdMethod.setAccessible(true);

        extractEntityNameMethod = QAService.class.getDeclaredMethod(
            "extractEntityNameFromQuestion", String.class, String.class);
        extractEntityNameMethod.setAccessible(true);

        resolveEntityMethod = QAService.class.getDeclaredMethod(
            "resolveEntityFromQuestion", AskQuestionRequest.class);
        resolveEntityMethod.setAccessible(true);
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private Long invokeExtractId(String question, String contextType) throws Exception {
        return (Long) extractEntityIdMethod.invoke(qaService, question, contextType);
    }

    private String invokeExtractName(String question, String contextType) throws Exception {
        return (String) extractEntityNameMethod.invoke(qaService, question, contextType);
    }

    private void invokeResolveEntity(AskQuestionRequest request) throws Exception {
        resolveEntityMethod.invoke(qaService, request);
    }

    private Cycle cycle(Long id, String name) {
        Cycle c = new Cycle();
        c.setId(id);
        c.setName(name);
        return c;
    }

    // ── extractEntityIdFromQuestion ─────────────────────────────────────────────

    @Nested
    @DisplayName("extractEntityIdFromQuestion")
    class ExtractEntityId {

        @DisplayName("should extract numeric ID from 'contextType N' patterns")
        @ParameterizedTest(name = "''{0}'' with contextType=''{1}'' → {2}")
        @CsvSource({
            "What is in cycle 6?,         cycle,   6",
            "pitch 15 details,            pitch,   15",
            "team 2 members,              team,    2",
            "meeting 3 summary,           meeting, 3",
            "Tell me about cycle 100,     cycle,   100"
        })
        void shouldExtractIdFromDirectPattern(String question, String contextType, long expected) throws Exception {
            Long result = invokeExtractId(question.strip(), contextType.strip());
            assertThat(result).isEqualTo(expected);
        }

        @DisplayName("should extract numeric ID from prepositional patterns")
        @ParameterizedTest(name = "''{0}'' → {2}")
        @CsvSource({
            "What pitches are in cycle 4?,    cycle, 4",
            "Tasks for pitch 10,              pitch, 10",
            "About team 7,                    team,  7",
            "Notes from meeting 2,            meeting, 2"
        })
        void shouldExtractIdFromPrepositionalPattern(String question, String contextType, long expected)
            throws Exception {
            Long result = invokeExtractId(question.strip(), contextType.strip());
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should extract ID when question is only a number")
        void shouldExtractIdWhenQuestionIsJustANumber() throws Exception {
            Long result = invokeExtractId("42", "cycle");
            assertThat(result).isEqualTo(42L);
        }

        @Test
        @DisplayName("should return null when question has no numeric reference")
        void shouldReturnNullWhenNoNumericReference() throws Exception {
            Long result = invokeExtractId("Tell me about the Build Season cycle", "cycle");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for null question")
        void shouldReturnNullForNullQuestion() throws Exception {
            Long result = invokeExtractId(null, "cycle");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for null contextType")
        void shouldReturnNullForNullContextType() throws Exception {
            Long result = invokeExtractId("cycle 3", null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should not extract number from entity name containing digits (e.g. 'Q1 Sprint')")
        void shouldNotExtractNumberFromEntityNameWithDigits() throws Exception {
            // "Q1 Sprint cycle" – the 1 is embedded in the name, not as a standalone ID
            // The pattern requires "cycle <digits>" so "Q1" should not match "cycle Q1"
            Long result = invokeExtractId("What is in the Q1 Sprint cycle?", "cycle");
            assertThat(result).isNull();
        }
    }

    // ── extractEntityNameFromQuestion ───────────────────────────────────────────

    @Nested
    @DisplayName("extractEntityNameFromQuestion")
    class ExtractEntityName {

        @Test
        @DisplayName("should extract name from 'the XYZ cycle' pattern")
        void shouldExtractNameFromThePrefixPattern() throws Exception {
            String result = invokeExtractName("What pitches are in the Build Season cycle?", "cycle");
            assertThat(result).isEqualTo("Build Season");
        }

        @Test
        @DisplayName("should extract name from 'this XYZ cycle' pattern")
        void shouldExtractNameFromThisPrefixPattern() throws Exception {
            String result = invokeExtractName("Show me pitches in this Alpha cycle", "cycle");
            assertThat(result).isEqualTo("Alpha");
        }

        @Test
        @DisplayName("should extract name from 'in/for XYZ cycle' pattern")
        void shouldExtractNameFromPrepositionalPattern() throws Exception {
            String result = invokeExtractName("Pitches for Q1 Sprint cycle", "cycle");
            assertThat(result).isEqualTo("Q1 Sprint");
        }

        @Test
        @DisplayName("should extract non-numeric name from 'cycle XYZ' pattern")
        void shouldExtractNameFromContextTypeFirstPattern() throws Exception {
            String result = invokeExtractName("cycle Alpha details", "cycle");
            assertThat(result).isNotNull();
            assertThat(result).isEqualToIgnoringCase("Alpha details");
        }

        @Test
        @DisplayName("should return null when name would be a plain number")
        void shouldReturnNullWhenNameIsNumeric() throws Exception {
            String result = invokeExtractName("the 5 cycle", "cycle");
            // A numeric-only group should be rejected
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for null question")
        void shouldReturnNullForNullQuestion() throws Exception {
            String result = invokeExtractName(null, "cycle");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for null contextType")
        void shouldReturnNullForNullContextType() throws Exception {
            String result = invokeExtractName("the Build Season cycle", null);
            assertThat(result).isNull();
        }
    }

    // ── resolveEntityFromQuestion ───────────────────────────────────────────────

    @Nested
    @DisplayName("resolveEntityFromQuestion")
    class ResolveEntity {

        @Test
        @DisplayName("should set contextId and cycleId for 'cycle N' numeric reference")
        void shouldSetContextIdAndCycleIdFromNumericReference() throws Exception {
            AskQuestionRequest request = AskQuestionRequest.builder()
                .question("What pitches are in cycle 6?")
                .contextType("cycle")
                .build();

            when(cycleRepository.findById(6L)).thenReturn(Optional.of(cycle(6L, "Summer Cycle")));

            invokeResolveEntity(request);

            assertThat(request.getContextId()).isEqualTo(6L);
            assertThat(request.getCycleId()).isEqualTo(6L);
            assertThat(request.getContextName()).isEqualTo("Summer Cycle");
        }

        @Test
        @DisplayName("should set teamId for 'team N' numeric reference")
        void shouldSetTeamIdFromNumericReference() throws Exception {
            AskQuestionRequest request = AskQuestionRequest.builder()
                .question("Who is in team 3?")
                .contextType("team")
                .build();

            invokeResolveEntity(request);

            assertThat(request.getContextId()).isEqualTo(3L);
            assertThat(request.getTeamId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("should resolve cycle by name when single match is found")
        void shouldResolveCycleByNameWhenSingleMatch() throws Exception {
            Cycle buildSeason = cycle(42L, "Build Season");
            when(cycleRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(List.of(buildSeason));

            AskQuestionRequest request = AskQuestionRequest.builder()
                .question("What pitches are in the Build Season cycle?")
                .contextType("cycle")
                .build();

            invokeResolveEntity(request);

            assertThat(request.getContextId()).isEqualTo(42L);
            assertThat(request.getCycleId()).isEqualTo(42L);
            assertThat(request.getContextName()).isEqualTo("Build Season");
        }

        @Test
        @DisplayName("should set contextName only when multiple cycles match the name")
        void shouldSetContextNameOnlyWhenMultipleCyclesMatch() throws Exception {
            when(cycleRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(List.of(cycle(1L, "Sprint Alpha"), cycle(2L, "Sprint Beta")));

            AskQuestionRequest request = AskQuestionRequest.builder()
                .question("What is in the Sprint cycle?")
                .contextType("cycle")
                .build();

            invokeResolveEntity(request);

            // Multiple matches: no specific ID set, contextName used for vector filtering
            assertThat(request.getContextId()).isNull();
            assertThat(request.getCycleId()).isNull();
            assertThat(request.getContextName()).isNotNull();
        }

        @Test
        @DisplayName("should set contextName for semantic search when no cycle name matches")
        void shouldSetContextNameForSemanticSearchWhenNoMatch() throws Exception {
            when(cycleRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(List.of());

            AskQuestionRequest request = AskQuestionRequest.builder()
                .question("What is in the Winter Cycle cycle?")
                .contextType("cycle")
                .build();

            invokeResolveEntity(request);

            // No match: contextId stays null, contextName set so semantic search can use it
            assertThat(request.getContextId()).isNull();
            assertThat(request.getContextName()).isNotNull();
        }

        @Test
        @DisplayName("should not overwrite already-set cycleId when resolving by numeric ID")
        void shouldNotOverwriteExistingCycleId() throws Exception {
            AskQuestionRequest request = AskQuestionRequest.builder()
                .question("What pitches are in cycle 6?")
                .contextType("cycle")
                .cycleId(99L) // already set by caller
                .build();

            // Name lookup for "Cycle 6" returns nothing → falls back to numeric ID lookup.
            // cycleId guard (null-check) prevents overwriting the caller-supplied 99L.
            when(cycleRepository.findByNameContainingIgnoreCase("Cycle 6")).thenReturn(List.of());

            invokeResolveEntity(request);

            // contextId set from extracted numeric id, but existing cycleId preserved
            assertThat(request.getContextId()).isEqualTo(6L);
            assertThat(request.getCycleId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("named 'Cycle 5' is preferred over db row with id=5")
        void shouldPreferNamedCycleOverIdWhenBothExist() throws Exception {
            // DB has: id=5 → "Summer Sprint", id=12 → "Cycle 5"
            // User asks about "Cycle 5" meaning the one literally named "Cycle 5".
            Cycle namedCycle5 = cycle(12L, "Cycle 5");
            when(cycleRepository.findByNameContainingIgnoreCase("Cycle 5"))
                .thenReturn(List.of(namedCycle5));

            AskQuestionRequest request = AskQuestionRequest.builder()
                .question("What pitches are in cycle 5?")
                .contextType("cycle")
                .build();

            invokeResolveEntity(request);

            // Should resolve to the cycle NAMED "Cycle 5" (id=12), not id=5
            assertThat(request.getContextId()).isEqualTo(12L);
            assertThat(request.getCycleId()).isEqualTo(12L);
            assertThat(request.getContextName()).isEqualTo("Cycle 5");
            // findById(5) must never be called since name match took precedence
            verify(cycleRepository, never()).findById(5L);
        }

        @Test
        @DisplayName("'Cycle 5' must not match a cycle named 'Cycle 50'")
        void shouldNotMatchPartialCycleNameCycle50WhenAskedAboutCycle5() throws Exception {
            // DB only has "Cycle 50"; no cycle is named exactly "Cycle 5".
            Cycle cycle50 = cycle(50L, "Cycle 50");
            when(cycleRepository.findByNameContainingIgnoreCase("Cycle 5"))
                .thenReturn(List.of(cycle50));        // partial hit: "Cycle 50" contains "Cycle 5"
            // Numeric fallback: db row id=5 exists
            when(cycleRepository.findById(5L)).thenReturn(Optional.of(cycle(5L, "Summer Sprint")));

            AskQuestionRequest request = AskQuestionRequest.builder()
                .question("What pitches are in cycle 5?")
                .contextType("cycle")
                .build();

            invokeResolveEntity(request);

            // "Cycle 50" is NOT an exact match for "Cycle 5", so we fall back to id=5
            assertThat(request.getContextId()).isEqualTo(5L);
            assertThat(request.getCycleId()).isEqualTo(5L);
            assertThat(request.getContextName()).isEqualTo("Summer Sprint");
        }

        @Test
        @DisplayName("should not overwrite caller-supplied cycleId when resolving cycle by name")
        void shouldNotOverwriteCallerCycleIdWhenResolvingByName() throws Exception {
            Cycle buildSeason = cycle(42L, "Build Season");
            when(cycleRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(List.of(buildSeason));

            AskQuestionRequest request = AskQuestionRequest.builder()
                .question("What pitches are in the Build Season cycle?")
                .contextType("cycle")
                .cycleId(99L) // pre-set by caller
                .build();

            invokeResolveEntity(request);

            assertThat(request.getContextId()).isEqualTo(42L);
            assertThat(request.getCycleId()).isEqualTo(99L); // untouched
            assertThat(request.getContextName()).isEqualTo("Build Season");
        }

        @Test
        @DisplayName("should handle cycleRepository lookup failure gracefully")
        void shouldHandleCycleRepositoryExceptionGracefully() throws Exception {
            when(cycleRepository.findById(anyLong())).thenThrow(new RuntimeException("DB error"));

            AskQuestionRequest request = AskQuestionRequest.builder()
                .question("What is in cycle 5?")
                .contextType("cycle")
                .build();

            // Should not propagate exception; contextId is still set, contextName stays null
            invokeResolveEntity(request);

            assertThat(request.getContextId()).isEqualTo(5L);
            assertThat(request.getCycleId()).isEqualTo(5L);
            assertThat(request.getContextName()).isNull();
        }

        @Test
        @DisplayName("should handle name-based cycleRepository lookup failure gracefully")
        void shouldHandleNameBasedLookupExceptionGracefully() throws Exception {
            when(cycleRepository.findByNameContainingIgnoreCase(anyString()))
                .thenThrow(new RuntimeException("DB error"));

            AskQuestionRequest request = AskQuestionRequest.builder()
                .question("What is in the Build Season cycle?")
                .contextType("cycle")
                .build();

            // Should not propagate exception
            invokeResolveEntity(request);

            assertThat(request.getContextId()).isNull();
        }

        @Test
        @DisplayName("should not perform name lookup for non-cycle contextTypes")
        void shouldNotPerformNameLookupForNonCycleContext() throws Exception {
            AskQuestionRequest request = AskQuestionRequest.builder()
                .question("What is in the Frontend pitch?")
                .contextType("pitch")
                .build();

            invokeResolveEntity(request);

            // CycleRepository must never be consulted for non-cycle context
            verifyNoInteractions(cycleRepository);
        }
    }
}
