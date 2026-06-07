package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.config.AIConfig;
import com.github.farzadsedaghatbin.shipflow.dto.narrative.CycleNarrativeDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.NarrativeType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.github.farzadsedaghatbin.shipflow.service.retro.RetroTestFixtures;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the retrospective summary feature of {@link CycleNarrativeService}.
 *
 * <p>Covers: with items + AI, with items + no LLM (template fallback),
 * empty retro (exception), no retro (exception).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RetroSummaryService — generateRetroSummary / getRetroSummary")
class RetroSummaryServiceTest {

  // ── repositories ────────────────────────────────────────────────────────
  @Mock private CycleRepository cycleRepository;
  @Mock private PitchRepository pitchRepository;
  @Mock private WorkLogRepository workLogRepository;
  @Mock private BettingDecisionRepository bettingDecisionRepository;
  @Mock private CycleNarrativeRepository narrativeRepository;
  @Mock private UserRepository userRepository;
  @Mock private RetroRepository retroRepository;
  @Mock private RetroItemRepository retroItemRepository;

  // ── services / config ───────────────────────────────────────────────────
  @Mock private AIConfig aiConfig;
  @Mock private CapacityConfigService capacityConfigService;
  @Mock private ChatLanguageModel chatLanguageModel;

  // ── system under test ───────────────────────────────────────────────────
  private CycleNarrativeService service;

  private static final Long CYCLE_ID = 42L;

  // ── test fixtures ────────────────────────────────────────────────────────
  private Project project;
  private Cycle cycle;
  private Retrospective retro;

  @BeforeEach
  void setUp() {
    project = RetroTestFixtures.aProject().withId(1L).withName("Alpha").build();
    cycle = RetroTestFixtures.aCycle()
        .withId(CYCLE_ID)
        .withName("Cycle 1")
        .withProject(project)
        .build();
    retro = RetroTestFixtures.aRetro()
        .withId(10L)
        .withCycle(cycle)
        .withProject(project)
        .withStatus(RetroStatus.CLOSED)
        .build();
  }

  /** Constructs the service; pass null chatLanguageModel to simulate no-LLM scenario. */
  private CycleNarrativeService buildService(ChatLanguageModel llm) {
    return new CycleNarrativeService(
        cycleRepository,
        pitchRepository,
        workLogRepository,
        bettingDecisionRepository,
        narrativeRepository,
        aiConfig,
        llm,
        userRepository,
        capacityConfigService,
        retroRepository,
        retroItemRepository);
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private List<RetroItem> threeItems() {
    RetroItem wentWell = RetroTestFixtures.aRetroItem()
        .withId(1L).withContent("Fast deployments").wentWell()
        .withRetrospective(retro).build();
    RetroItem blocker = RetroTestFixtures.aRetroItem()
        .withId(2L).withContent("Slow reviews").didntGoWell()
        .withRetrospective(retro).build();
    RetroItem action = RetroTestFixtures.aRetroItem()
        .withId(3L).withContent("Add code review SLA").action()
        .withRetrospective(retro).build();
    return List.of(wentWell, blocker, action);
  }

  private CycleNarrative savedNarrative(String content, boolean isAI) {
    return CycleNarrative.builder()
        .id(99L)
        .cycle(cycle)
        .narrativeType(NarrativeType.RETROSPECTIVE_SUMMARY)
        .content(content)
        .isAiGenerated(isAI)
        .generatedAt(LocalDateTime.now())
        .build();
  }

  // ══════════════════════════════════════════════════════════════════════════
  // generateRetroSummary
  // ══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("generateRetroSummary()")
  class GenerateRetroSummary {

    @Test
    @DisplayName("with items and AI enabled — calls LLM and saves AI narrative")
    void withItemsAndAI_savesAINarrative() {
      service = buildService(chatLanguageModel);

      when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
      when(retroRepository.findByCycleIdOrderByCreatedAtDesc(CYCLE_ID)).thenReturn(List.of(retro));
      when(retroItemRepository.findByRetrospectiveIdOrderByCreatedAtAsc(retro.getId()))
          .thenReturn(threeItems());
      when(narrativeRepository.findByCycleIdAndNarrativeType(CYCLE_ID, NarrativeType.RETROSPECTIVE_SUMMARY))
          .thenReturn(Optional.empty());
      when(aiConfig.isAiRiskAnalysisEnabled()).thenReturn(true);
      when(aiConfig.getModelName()).thenReturn("test-model");
      when(chatLanguageModel.generate(anyString())).thenReturn("AI-generated summary content");

      CycleNarrative narr = savedNarrative("AI-generated summary content", true);
      when(narrativeRepository.save(any(CycleNarrative.class))).thenReturn(narr);

      CycleNarrativeDTO result = service.generateRetroSummary(CYCLE_ID);

      assertThat(result).isNotNull();
      assertThat(result.getNarrativeType()).isEqualTo(NarrativeType.RETROSPECTIVE_SUMMARY);
      assertThat(result.getIsAiGenerated()).isTrue();

      ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
      verify(chatLanguageModel).generate(promptCaptor.capture());
      String prompt = promptCaptor.getValue();
      assertThat(prompt).contains("Cycle 1");
      assertThat(prompt).contains("WENT WELL");
      assertThat(prompt).contains("Fast deployments");
      assertThat(prompt).contains("ACTION ITEMS");

      verify(narrativeRepository).save(any(CycleNarrative.class));
    }

    @Test
    @DisplayName("with items and no LLM — falls back to template summary")
    void withItemsNoLLM_usesTemplate() {
      service = buildService(null); // no LLM wired

      when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
      when(retroRepository.findByCycleIdOrderByCreatedAtDesc(CYCLE_ID)).thenReturn(List.of(retro));
      when(retroItemRepository.findByRetrospectiveIdOrderByCreatedAtAsc(retro.getId()))
          .thenReturn(threeItems());
      when(narrativeRepository.findByCycleIdAndNarrativeType(CYCLE_ID, NarrativeType.RETROSPECTIVE_SUMMARY))
          .thenReturn(Optional.empty());

      ArgumentCaptor<CycleNarrative> savedCaptor = ArgumentCaptor.forClass(CycleNarrative.class);
      when(narrativeRepository.save(savedCaptor.capture())).thenAnswer(inv -> {
        CycleNarrative n = inv.getArgument(0);
        n.setId(99L);
        n.setGeneratedAt(LocalDateTime.now());
        n.setCreatedAt(LocalDateTime.now());
        return n;
      });

      CycleNarrativeDTO result = service.generateRetroSummary(CYCLE_ID);

      assertThat(result).isNotNull();
      assertThat(result.getIsAiGenerated()).isFalse();

      CycleNarrative saved = savedCaptor.getValue();
      assertThat(saved.getContent()).contains("Key Wins");
      assertThat(saved.getContent()).contains("Fast deployments");
      assertThat(saved.getContent()).contains("Action Items");
      assertThat(saved.getContent()).contains("Add code review SLA");

      verify(chatLanguageModel, never()).generate(anyString());
    }

    @Test
    @DisplayName("no retrospectives for cycle — throws IllegalStateException")
    void noRetros_throwsException() {
      service = buildService(null);

      when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
      when(retroRepository.findByCycleIdOrderByCreatedAtDesc(CYCLE_ID))
          .thenReturn(Collections.emptyList());

      assertThatThrownBy(() -> service.generateRetroSummary(CYCLE_ID))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("No retrospective data found for this cycle");
    }

    @Test
    @DisplayName("retrospective exists but has no items — throws IllegalStateException")
    void retroWithNoItems_throwsException() {
      service = buildService(null);

      when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
      when(retroRepository.findByCycleIdOrderByCreatedAtDesc(CYCLE_ID)).thenReturn(List.of(retro));
      when(retroItemRepository.findByRetrospectiveIdOrderByCreatedAtAsc(retro.getId()))
          .thenReturn(Collections.emptyList());

      assertThatThrownBy(() -> service.generateRetroSummary(CYCLE_ID))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("No retrospective data found for this cycle");
    }

    @Test
    @DisplayName("AI throws exception — falls back to template without re-throwing")
    void aiFailure_gracefulFallbackToTemplate() {
      service = buildService(chatLanguageModel);

      when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
      when(retroRepository.findByCycleIdOrderByCreatedAtDesc(CYCLE_ID)).thenReturn(List.of(retro));
      when(retroItemRepository.findByRetrospectiveIdOrderByCreatedAtAsc(retro.getId()))
          .thenReturn(threeItems());
      when(narrativeRepository.findByCycleIdAndNarrativeType(CYCLE_ID, NarrativeType.RETROSPECTIVE_SUMMARY))
          .thenReturn(Optional.empty());
      when(aiConfig.isAiRiskAnalysisEnabled()).thenReturn(true);
      when(chatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("LLM timeout"));

      ArgumentCaptor<CycleNarrative> savedCaptor = ArgumentCaptor.forClass(CycleNarrative.class);
      when(narrativeRepository.save(savedCaptor.capture())).thenAnswer(inv -> {
        CycleNarrative n = inv.getArgument(0);
        n.setId(99L);
        n.setGeneratedAt(LocalDateTime.now());
        n.setCreatedAt(LocalDateTime.now());
        return n;
      });

      // Should NOT throw — falls back to template
      CycleNarrativeDTO result = service.generateRetroSummary(CYCLE_ID);

      assertThat(result).isNotNull();
      assertThat(result.getIsAiGenerated()).isFalse();
      assertThat(savedCaptor.getValue().getContent()).contains("Key Wins");
    }

    @Test
    @DisplayName("existing narrative — updates it in place (upsert)")
    void existingNarrative_isUpdated() {
      service = buildService(null);

      CycleNarrative existing = savedNarrative("old content", false);
      when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
      when(retroRepository.findByCycleIdOrderByCreatedAtDesc(CYCLE_ID)).thenReturn(List.of(retro));
      when(retroItemRepository.findByRetrospectiveIdOrderByCreatedAtAsc(retro.getId()))
          .thenReturn(threeItems());
      when(narrativeRepository.findByCycleIdAndNarrativeType(CYCLE_ID, NarrativeType.RETROSPECTIVE_SUMMARY))
          .thenReturn(Optional.of(existing));
      when(narrativeRepository.save(any(CycleNarrative.class))).thenReturn(existing);

      service.generateRetroSummary(CYCLE_ID);

      // Save called exactly once (update path, not create)
      verify(narrativeRepository, times(1)).save(existing);
    }
  }

  // ══════════════════════════════════════════════════════════════════════════
  // getRetroSummary
  // ══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("getRetroSummary()")
  class GetRetroSummary {

    @Test
    @DisplayName("returns existing summary when present")
    void summaryPresent_returnsDTO() {
      service = buildService(null);

      CycleNarrative existing = savedNarrative("## Retrospective Summary", false);
      existing.setCycle(cycle);
      when(narrativeRepository.findByCycleIdAndNarrativeType(CYCLE_ID, NarrativeType.RETROSPECTIVE_SUMMARY))
          .thenReturn(Optional.of(existing));

      Optional<CycleNarrativeDTO> result = service.getRetroSummary(CYCLE_ID);

      assertThat(result).isPresent();
      assertThat(result.get().getNarrativeType()).isEqualTo(NarrativeType.RETROSPECTIVE_SUMMARY);
      assertThat(result.get().getContent()).contains("Retrospective Summary");
    }

    @Test
    @DisplayName("returns empty when no summary generated yet")
    void noSummary_returnsEmpty() {
      service = buildService(null);

      when(narrativeRepository.findByCycleIdAndNarrativeType(CYCLE_ID, NarrativeType.RETROSPECTIVE_SUMMARY))
          .thenReturn(Optional.empty());

      Optional<CycleNarrativeDTO> result = service.getRetroSummary(CYCLE_ID);

      assertThat(result).isEmpty();
    }
  }
}
