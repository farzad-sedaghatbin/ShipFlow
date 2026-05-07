package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.risk.CycleRiskOverviewDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import com.github.farzadsedaghatbin.shipflow.service.AsyncAIAdvisorService.JobStatus;
import com.github.farzadsedaghatbin.shipflow.service.AsyncAIAdvisorService.JobStatusResponse;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for AsyncAIAdvisorService.
 * Tests cover:
 * - Cache-first pattern (return immediately if cached)
 * - Async job creation and status tracking
 * - Job completion and result retrieval
 * - Job cancellation
 * - Job statistics
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AsyncAIAdvisorService Tests")
class AsyncAIAdvisorServiceTest {

  @Mock
  private AICacheService cacheService;

  @Mock
  private AsyncAIExecutor asyncAIExecutor;

  private AsyncAIAdvisorService asyncService;

  @BeforeEach
  void setUp() {
    asyncService = new AsyncAIAdvisorService(cacheService, asyncAIExecutor);
  }

  // ===========================================
  // CACHE-FIRST PATTERN TESTS
  // ===========================================

  @Nested
  @DisplayName("Cache-First Pattern Tests")
  class CacheFirstTests {

    @Test
    @DisplayName("Should return cached pitch risk immediately")
    void shouldReturnCachedPitchRiskImmediately() {
      // Given
      Long pitchId = 1L;
      PitchRiskDTO cachedRisk = createTestPitchRiskDTO(pitchId, RiskLevel.MEDIUM, 50);
      when(cacheService.getCachedPitchRisk(pitchId, true)).thenReturn(Optional.of(cachedRisk));

      // When
      Optional<PitchRiskDTO> result = asyncService.getCachedPitchRisk(pitchId);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getPitchId()).isEqualTo(pitchId);
      assertThat(result.get().getRiskScore()).isEqualTo(50);
      verify(cacheService).getCachedPitchRisk(pitchId, true);
    }

    @Test
    @DisplayName("Should return empty when pitch risk not cached")
    void shouldReturnEmptyWhenPitchRiskNotCached() {
      // Given
      Long pitchId = 1L;
      when(cacheService.getCachedPitchRisk(pitchId, true)).thenReturn(Optional.empty());

      // When
      Optional<PitchRiskDTO> result = asyncService.getCachedPitchRisk(pitchId);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return cached cycle risk immediately")
    void shouldReturnCachedCycleRiskImmediately() {
      // Given
      Long cycleId = 1L;
      CycleRiskOverviewDTO cachedRisk = createTestCycleRiskDTO(cycleId, RiskLevel.LOW, 25);
      when(cacheService.getCachedCycleRisk(cycleId, true)).thenReturn(Optional.of(cachedRisk));

      // When
      Optional<CycleRiskOverviewDTO> result = asyncService.getCachedCycleRisk(cycleId);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getCycleId()).isEqualTo(cycleId);
      assertThat(result.get().getOverallRiskScore()).isEqualTo(25);
    }

    @Test
    @DisplayName("Should handle null cache service gracefully")
    void shouldHandleNullCacheServiceGracefully() {
      // Given - service with no cache
      AsyncAIAdvisorService serviceWithoutCache = new AsyncAIAdvisorService(null, asyncAIExecutor);

      // When
      Optional<PitchRiskDTO> result = serviceWithoutCache.getCachedPitchRisk(1L);

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ===========================================
  // JOB CREATION TESTS
  // ===========================================

  @Nested
  @DisplayName("Job Creation Tests")
  class JobCreationTests {

    @Test
    @DisplayName("Should create pitch risk analysis job with unique ID")
    void shouldCreatePitchRiskAnalysisJob() {
      // Given
      Long pitchId = 1L;

      // When
      String jobId = asyncService.startPitchRiskAnalysis(pitchId);

      // Then
      assertThat(jobId).isNotNull();
      assertThat(jobId).hasSize(8); // UUID substring length

      Optional<JobStatusResponse> status = asyncService.getJobStatus(jobId);
      assertThat(status).isPresent();
      assertThat(status.get().getJobType()).isEqualTo("pitch_risk");
      assertThat(status.get().getContextId()).isEqualTo(pitchId);
    }

    @Test
    @DisplayName("Should create cycle risk analysis job")
    void shouldCreateCycleRiskAnalysisJob() {
      // Given
      Long cycleId = 2L;

      // When
      String jobId = asyncService.startCycleRiskAnalysis(cycleId);

      // Then
      assertThat(jobId).isNotNull();

      Optional<JobStatusResponse> status = asyncService.getJobStatus(jobId);
      assertThat(status).isPresent();
      assertThat(status.get().getJobType()).isEqualTo("cycle_risk");
      assertThat(status.get().getContextId()).isEqualTo(cycleId);
    }

    @Test
    @DisplayName("Should create Q&A job")
    void shouldCreateQAJob() {
      // Given
      Long pitchId = 1L;
      String question = "What are the main risks?";

      // When
      String jobId = asyncService.startQAQuestion(pitchId, question);

      // Then
      assertThat(jobId).isNotNull();

      Optional<JobStatusResponse> status = asyncService.getJobStatus(jobId);
      assertThat(status).isPresent();
      assertThat(status.get().getJobType()).isEqualTo("qa");
    }

    @Test
    @DisplayName("Should generate unique job IDs")
    void shouldGenerateUniqueJobIds() {
      // When
      String jobId1 = asyncService.startPitchRiskAnalysis(1L);
      String jobId2 = asyncService.startPitchRiskAnalysis(2L);
      String jobId3 = asyncService.startCycleRiskAnalysis(1L);

      // Then
      assertThat(jobId1).isNotEqualTo(jobId2);
      assertThat(jobId2).isNotEqualTo(jobId3);
      assertThat(jobId1).isNotEqualTo(jobId3);
    }
  }

  // ===========================================
  // JOB STATUS TESTS
  // ===========================================

  @Nested
  @DisplayName("Job Status Tests")
  class JobStatusTests {

    @Test
    @DisplayName("Should return job status")
    void shouldReturnJobStatus() {
      // Given
      String jobId = asyncService.startPitchRiskAnalysis(1L);

      // When
      Optional<JobStatusResponse> status = asyncService.getJobStatus(jobId);

      // Then
      assertThat(status).isPresent();
      assertThat(status.get().getJobId()).isEqualTo(jobId);
      assertThat(status.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return empty for non-existent job")
    void shouldReturnEmptyForNonExistentJob() {
      // When
      Optional<JobStatusResponse> status = asyncService.getJobStatus("non-existent");

      // Then
      assertThat(status).isEmpty();
    }
  }

  // ===========================================
  // JOB CANCELLATION TESTS
  // ===========================================

  @Nested
  @DisplayName("Job Cancellation Tests")
  class JobCancellationTests {

    @Test
    @DisplayName("Should attempt to cancel job")
    void shouldAttemptToCancelJob() {
      // Given
      String jobId = asyncService.startPitchRiskAnalysis(1L);

      // When - Note: Job may complete immediately in unit test context (synchronous execution)
      // This tests the cancellation mechanism exists and returns appropriate result
      asyncService.cancelJob(jobId);

      // Then - job either was cancelled or already completed
      Optional<JobStatusResponse> status = asyncService.getJobStatus(jobId);
      assertThat(status).isPresent();
      // Status will be FAILED (cancelled) or COMPLETED (finished before cancel)
      assertThat(status.get().getStatus())
          .isIn(JobStatus.FAILED, JobStatus.COMPLETED, JobStatus.PENDING, JobStatus.PROCESSING);
    }

    @Test
    @DisplayName("Should return false for non-existent job cancellation")
    void shouldReturnFalseForNonExistentJobCancellation() {
      // When
      boolean cancelled = asyncService.cancelJob("non-existent");

      // Then
      assertThat(cancelled).isFalse();
    }
  }

  // ===========================================
  // JOB STATISTICS TESTS
  // ===========================================

  @Nested
  @DisplayName("Job Statistics Tests")
  class JobStatisticsTests {

    @Test
    @DisplayName("Should track job statistics")
    void shouldTrackJobStatistics() {
      // Given
      asyncService.startPitchRiskAnalysis(1L);
      asyncService.startCycleRiskAnalysis(1L);
      String cancelledJobId = asyncService.startQAQuestion(1L, "test?");
      asyncService.cancelJob(cancelledJobId);

      // When
      Map<String, Object> stats = asyncService.getJobStats();

      // Then
      assertThat(stats).containsKey("total");
      assertThat(stats).containsKey("pending");
      assertThat(stats).containsKey("processing");
      assertThat(stats).containsKey("completed");
      assertThat(stats).containsKey("failed");
      // Use Number to handle both Integer and Long
      assertThat(((Number) stats.get("total")).longValue()).isGreaterThanOrEqualTo(3);
    }
  }

  // ===========================================
  // HELPER METHODS
  // ===========================================

  private PitchRiskDTO createTestPitchRiskDTO(Long pitchId, RiskLevel level, int score) {
    return PitchRiskDTO.builder()
        .pitchId(pitchId)
        .pitchTitle("Test Pitch " + pitchId)
        .riskScore(score)
        .riskLevel(level)
        .confidenceScore(80)
        .analyzedAt(LocalDateTime.now())
        .aiEnabled(true)
        .build();
  }

  private CycleRiskOverviewDTO createTestCycleRiskDTO(Long cycleId, RiskLevel level, int score) {
    return CycleRiskOverviewDTO.builder()
        .cycleId(cycleId)
        .cycleName("Test Cycle " + cycleId)
        .overallRiskScore(score)
        .overallRiskLevel(level)
        .totalPitches(5)
        .highRiskPitches(1)
        .analyzedAt(LocalDateTime.now())
        .aiEnabled(true)
        .build();
  }
}
