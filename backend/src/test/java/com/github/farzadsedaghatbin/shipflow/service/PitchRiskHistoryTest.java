package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.risk.RiskFactor;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for PitchRiskHistory entity and repository interactions. Tests cover: - Risk history
 * retrieval with date filtering - Risk factor serialization - Repository query verification
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pitch Risk History Tests")
class PitchRiskHistoryTest {

  @Mock private PitchRiskHistoryRepository riskHistoryRepository;

  private Pitch testPitch;
  private Cycle testCycle;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();

    // Create test cycle
    testCycle = new Cycle();
    testCycle.setId(1L);
    testCycle.setName("Test Cycle");
    testCycle.setStartDate(LocalDate.now().minusDays(14));
    testCycle.setEndDate(LocalDate.now().plusDays(14));
    testCycle.setIsActive(true);

    // Create test pitch
    testPitch = new Pitch();
    testPitch.setId(1L);
    testPitch.setTitle("Test Pitch");
    testPitch.setStatus(PitchStatus.IN_PROGRESS);
    testPitch.setCycle(testCycle);
    testPitch.setCreatedAt(LocalDateTime.now().minusDays(7));
  }

  @Nested
  @DisplayName("Risk History Repository Tests")
  class RepositoryTests {

    @Test
    @DisplayName("Should find history by pitch ID and date range")
    void shouldFindHistoryByPitchIdAndDateRange() {
      // Given
      Long pitchId = 1L;
      LocalDateTime startDate = LocalDateTime.now().minusDays(30);
      LocalDateTime endDate = LocalDateTime.now();
      List<PitchRiskHistory> historyRecords = createTestHistoryRecords(pitchId, 10);

      when(riskHistoryRepository.findByPitchIdAndDateRange(
              eq(pitchId), any(LocalDateTime.class), any(LocalDateTime.class)))
          .thenReturn(historyRecords);

      // When
      List<PitchRiskHistory> result =
          riskHistoryRepository.findByPitchIdAndDateRange(pitchId, startDate, endDate);

      // Then
      assertThat(result).hasSize(10);
      verify(riskHistoryRepository).findByPitchIdAndDateRange(pitchId, startDate, endDate);
    }

    @Test
    @DisplayName("Should return empty list when no history exists")
    void shouldReturnEmptyListWhenNoHistory() {
      // Given
      Long pitchId = 999L;

      when(riskHistoryRepository.findByPitchIdAndDateRange(
              eq(pitchId), any(LocalDateTime.class), any(LocalDateTime.class)))
          .thenReturn(Collections.emptyList());

      // When
      List<PitchRiskHistory> result =
          riskHistoryRepository.findByPitchIdAndDateRange(
              pitchId, LocalDateTime.now().minusDays(30), LocalDateTime.now());

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find pitch IDs with history today")
    void shouldFindPitchIdsWithHistoryToday() {
      // Given
      LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
      LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
      List<Long> pitchIdsWithHistory = Arrays.asList(1L, 2L, 3L);

      when(riskHistoryRepository.findPitchIdsWithHistoryToday(
              any(LocalDateTime.class), any(LocalDateTime.class)))
          .thenReturn(pitchIdsWithHistory);

      // When
      List<Long> result = riskHistoryRepository.findPitchIdsWithHistoryToday(startOfDay, endOfDay);

      // Then
      assertThat(result).containsExactly(1L, 2L, 3L);
    }
  }

  @Nested
  @DisplayName("PitchRiskHistory Entity Tests")
  class EntityTests {

    @Test
    @DisplayName("Should create history with all fields")
    void shouldCreateHistoryWithAllFields() {
      // Given & When
      PitchRiskHistory history =
          PitchRiskHistory.builder()
              .id(1L)
              .pitch(testPitch)
              .recordedAt(LocalDateTime.now())
              .riskScore(75)
              .riskLevel(RiskLevel.HIGH)
              .triggerType(PitchRiskHistory.TriggerType.MANUAL)
              .riskFactorsJson(
                  "[{\"category\":\"TIME_OVERRUN\",\"description\":\"Behind schedule\"}]")
              .build();

      // Then
      assertThat(history.getId()).isEqualTo(1L);
      assertThat(history.getPitch()).isEqualTo(testPitch);
      assertThat(history.getRiskScore()).isEqualTo(75);
      assertThat(history.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
      assertThat(history.getTriggerType()).isEqualTo(PitchRiskHistory.TriggerType.MANUAL);
    }

    @Test
    @DisplayName("Should support different trigger types")
    void shouldSupportDifferentTriggerTypes() {
      // Verify all trigger types exist
      assertThat(PitchRiskHistory.TriggerType.values())
          .containsExactlyInAnyOrder(
              PitchRiskHistory.TriggerType.MANUAL,
              PitchRiskHistory.TriggerType.SCHEDULED,
              PitchRiskHistory.TriggerType.STATUS_CHANGE,
              PitchRiskHistory.TriggerType.WORK_LOG_ADDED,
              PitchRiskHistory.TriggerType.CIRCUIT_BREAKER);
    }
  }

  @Nested
  @DisplayName("Risk Factor Serialization Tests")
  class RiskFactorSerializationTests {

    @Test
    @DisplayName("Should properly serialize risk factors to JSON")
    void shouldSerializeRiskFactors() throws JsonProcessingException {
      // Given
      List<RiskFactor> riskFactors =
          Arrays.asList(
              RiskFactor.builder()
                  .category(RiskFactor.RiskCategory.TIME_OVERRUN)
                  .description("Project is behind schedule")
                  .impactLevel(8)
                  .probability(7)
                  .build(),
              RiskFactor.builder()
                  .category(RiskFactor.RiskCategory.SCOPE_CREEP)
                  .description("Scope creep detected")
                  .impactLevel(6)
                  .probability(5)
                  .build());

      // When
      String json = objectMapper.writeValueAsString(riskFactors);

      // Then
      assertThat(json).contains("TIME_OVERRUN");
      assertThat(json).contains("SCOPE_CREEP");
      assertThat(json).contains("Project is behind schedule");
      assertThat(json).contains("impactLevel");
    }

    @Test
    @DisplayName("Should properly deserialize risk factors from JSON")
    void shouldDeserializeRiskFactors() throws JsonProcessingException {
      // Given
      String json =
          "[{\"category\":\"TIME_OVERRUN\",\"description\":\"Behind schedule\",\"impactLevel\":8,\"probability\":7}]";

      // When
      List<RiskFactor> factors =
          objectMapper.readValue(
              json,
              objectMapper.getTypeFactory().constructCollectionType(List.class, RiskFactor.class));

      // Then
      assertThat(factors).hasSize(1);
      assertThat(factors.get(0).getCategory()).isEqualTo(RiskFactor.RiskCategory.TIME_OVERRUN);
      assertThat(factors.get(0).getImpactLevel()).isEqualTo(8);
    }

    @Test
    @DisplayName("Should handle empty risk factors")
    void shouldHandleEmptyRiskFactors() throws JsonProcessingException {
      // Given
      String emptyJson = "[]";

      // When
      List<RiskFactor> factors =
          objectMapper.readValue(
              emptyJson,
              objectMapper.getTypeFactory().constructCollectionType(List.class, RiskFactor.class));

      // Then
      assertThat(factors).isEmpty();
    }
  }

  @Nested
  @DisplayName("Risk Level Tests")
  class RiskLevelTests {

    @Test
    @DisplayName("Should have all expected risk levels")
    void shouldHaveAllExpectedRiskLevels() {
      // Verify all risk levels exist
      assertThat(RiskLevel.values())
          .containsExactlyInAnyOrder(
              RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.CRITICAL);
    }

    @Test
    @DisplayName("Should create history with each risk level")
    void shouldCreateHistoryWithEachRiskLevel() {
      for (RiskLevel level : RiskLevel.values()) {
        PitchRiskHistory history =
            PitchRiskHistory.builder()
                .pitch(testPitch)
                .riskLevel(level)
                .riskScore(getScoreForLevel(level))
                .build();

        assertThat(history.getRiskLevel()).isEqualTo(level);
      }
    }
  }

  // Helper methods

  private List<PitchRiskHistory> createTestHistoryRecords(Long pitchId, int count) {
    List<PitchRiskHistory> records = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      PitchRiskHistory history =
          PitchRiskHistory.builder()
              .id((long) (i + 1))
              .pitch(testPitch)
              .recordedAt(LocalDateTime.now().minusDays(i))
              .riskScore(40 + i * 5) // Varying scores
              .riskLevel(i < 3 ? RiskLevel.MEDIUM : RiskLevel.HIGH)
              .triggerType(
                  i == 0
                      ? PitchRiskHistory.TriggerType.MANUAL
                      : PitchRiskHistory.TriggerType.SCHEDULED)
              .riskFactorsJson("[]")
              .build();
      records.add(history);
    }
    return records;
  }

  private int getScoreForLevel(RiskLevel level) {
    return switch (level) {
      case LOW -> 20;
      case MEDIUM -> 50;
      case HIGH -> 75;
      case CRITICAL -> 95;
    };
  }
}
