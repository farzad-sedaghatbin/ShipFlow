package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.risk.CycleRiskOverviewDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.RiskQuestionResponse;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Async service for AI advisory operations.
 * Handles long-running AI calls asynchronously to prevent blocking API responses.
 * 
 * IMPORTANT: This service checks cache first and returns immediately if cached.
 * Only uncached requests go through the async job flow.
 * 
 * ARCHITECTURE NOTE: Async execution is delegated to AsyncAIExecutor (separate bean)
 * to ensure Spring AOP properly intercepts @Async methods. Self-invocation within
 * the same bean bypasses the proxy and runs synchronously.
 * 
 * Usage pattern:
 * 1. Client calls check*Cache() - if cached, returns result immediately
 * 2. If not cached, client starts an async job and receives a jobId
 * 3. Client polls with jobId to check status
 * 4. When complete, client retrieves the result
 */
@Service
@Slf4j
public class AsyncAIAdvisorService {

  private final AICacheService cacheService;
  private final AsyncAIExecutor asyncAIExecutor;

  // In-memory job storage (consider Redis for production clustering)
  private final Map<String, AIJob<?>> jobs = new ConcurrentHashMap<>();
  
  // Job TTL in minutes (cleanup completed jobs after this time)
  private static final int JOB_TTL_MINUTES = 30;

  @Autowired
  public AsyncAIAdvisorService(
      @Autowired(required = false) AICacheService cacheService,
      AsyncAIExecutor asyncAIExecutor) {
    this.cacheService = cacheService;
    this.asyncAIExecutor = asyncAIExecutor;
  }

  /**
   * Job status enum
   */
  public enum JobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
  }

  /**
   * AI job wrapper containing status and result
   */
  @Data
  @Builder
  public static class AIJob<T> {
    private String jobId;
    private JobStatus status;
    private T result;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String jobType; // "pitch_risk", "cycle_risk", "qa"
    private Long contextId; // pitchId or cycleId
  }

  /**
   * DTO for job status response
   */
  @Data
  @Builder
  public static class JobStatusResponse {
    private String jobId;
    private JobStatus status;
    private String jobType;
    private Long contextId;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private boolean hasResult;
  }

  // ===========================================
  // CACHE CHECK METHODS (Return immediately if cached)
  // ===========================================

  /**
   * Check if pitch risk is cached - returns immediately if found.
   * Call this BEFORE starting async job to avoid unnecessary async work.
   */
  public Optional<PitchRiskDTO> getCachedPitchRisk(Long pitchId) {
    if (cacheService == null) {
      return Optional.empty();
    }
    Optional<PitchRiskDTO> cached = cacheService.getCachedPitchRisk(pitchId, true);
    if (cached.isPresent()) {
      log.debug("Cache HIT for pitch risk {}, returning immediately", pitchId);
    }
    return cached;
  }

  /**
   * Check if cycle risk is cached - returns immediately if found.
   * Call this BEFORE starting async job to avoid unnecessary async work.
   */
  public Optional<CycleRiskOverviewDTO> getCachedCycleRisk(Long cycleId) {
    if (cacheService == null) {
      return Optional.empty();
    }
    Optional<CycleRiskOverviewDTO> cached = cacheService.getCachedCycleRisk(cycleId, true);
    if (cached.isPresent()) {
      log.debug("Cache HIT for cycle risk {}, returning immediately", cycleId);
    }
    return cached;
  }

  // ===========================================
  // PITCH RISK ANALYSIS (ASYNC)
  // ===========================================

  /**
   * Start async pitch risk analysis with AI.
   * Includes request deduplication - returns existing job if one is already in progress.
   * @return jobId for polling
   */
  public String startPitchRiskAnalysis(Long pitchId) {
    return startPitchRiskAnalysis(pitchId, false);
  }

  /**
   * Start async pitch risk analysis with AI.
   * Includes request deduplication - returns existing job if one is already in progress.
   * @param force if true, evicts any cached result first so a fresh AI analysis is always run
   * @return jobId for polling
   */
  public String startPitchRiskAnalysis(Long pitchId, boolean force) {
    if (force && cacheService != null) {
      cacheService.invalidatePitchRiskCache(pitchId);
    }

    // Deduplication: Check if a job for this pitch is already pending/processing
    Optional<String> existingJobId = findExistingJob("pitch_risk", pitchId);
    if (existingJobId.isPresent()) {
      log.info("Reusing existing pitch risk analysis job {} for pitch {}", existingJobId.get(), pitchId);
      return existingJobId.get();
    }
    
    String jobId = generateJobId();
    
    AIJob<PitchRiskDTO> job = AIJob.<PitchRiskDTO>builder()
        .jobId(jobId)
        .status(JobStatus.PENDING)
        .createdAt(LocalDateTime.now())
        .jobType("pitch_risk")
        .contextId(pitchId)
        .build();
    
    jobs.put(jobId, job);
    
    // Execute async via separate bean (ensures @Async proxy works)
    asyncAIExecutor.executePitchRiskAnalysisAsync(jobId, pitchId, jobs);
    
    log.info("Started async pitch risk analysis job {} for pitch {}", jobId, pitchId);
    return jobId;
  }

  /**
   * Get pitch risk analysis result
   */
  public Optional<PitchRiskDTO> getPitchRiskResult(String jobId) {
    @SuppressWarnings("unchecked")
    AIJob<PitchRiskDTO> job = (AIJob<PitchRiskDTO>) jobs.get(jobId);
    if (job == null || job.getStatus() != JobStatus.COMPLETED) {
      return Optional.empty();
    }
    return Optional.ofNullable(job.getResult());
  }

  // ===========================================
  // CYCLE RISK ANALYSIS (ASYNC)
  // ===========================================

  /**
   * Start async cycle risk analysis with AI.
   * Includes request deduplication - returns existing job if one is already in progress.
   * @return jobId for polling
   */
  public String startCycleRiskAnalysis(Long cycleId) {
    return startCycleRiskAnalysis(cycleId, false);
  }

  /**
   * Start async cycle risk analysis with AI.
   * Includes request deduplication - returns existing job if one is already in progress.
   * @param force if true, evicts any cached result first so a fresh AI analysis is always run
   * @return jobId for polling
   */
  public String startCycleRiskAnalysis(Long cycleId, boolean force) {
    if (force && cacheService != null) {
      cacheService.invalidateCycleRiskCache(cycleId);
    }

    // Deduplication: Check if a job for this cycle is already pending/processing
    Optional<String> existingJobId = findExistingJob("cycle_risk", cycleId);
    if (existingJobId.isPresent()) {
      log.info("Reusing existing cycle risk analysis job {} for cycle {}", existingJobId.get(), cycleId);
      return existingJobId.get();
    }
    
    String jobId = generateJobId();
    
    AIJob<CycleRiskOverviewDTO> job = AIJob.<CycleRiskOverviewDTO>builder()
        .jobId(jobId)
        .status(JobStatus.PENDING)
        .createdAt(LocalDateTime.now())
        .jobType("cycle_risk")
        .contextId(cycleId)
        .build();
    
    jobs.put(jobId, job);
    
    // Execute async via separate bean (ensures @Async proxy works)
    asyncAIExecutor.executeCycleRiskAnalysisAsync(jobId, cycleId, jobs);
    
    log.info("Started async cycle risk analysis job {} for cycle {}", jobId, cycleId);
    return jobId;
  }

  /**
   * Get cycle risk analysis result
   */
  public Optional<CycleRiskOverviewDTO> getCycleRiskResult(String jobId) {
    @SuppressWarnings("unchecked")
    AIJob<CycleRiskOverviewDTO> job = (AIJob<CycleRiskOverviewDTO>) jobs.get(jobId);
    if (job == null || job.getStatus() != JobStatus.COMPLETED) {
      return Optional.empty();
    }
    return Optional.ofNullable(job.getResult());
  }

  // ===========================================
  // Q&A (ASYNC)
  // ===========================================

  /**
   * Start async Q&A with AI advisor.
   * Q&A is not deduplicated since each question is unique.
   * @return jobId for polling
   */
  public String startQAQuestion(Long pitchId, String question) {
    String jobId = generateJobId();
    
    AIJob<RiskQuestionResponse> job = AIJob.<RiskQuestionResponse>builder()
        .jobId(jobId)
        .status(JobStatus.PENDING)
        .createdAt(LocalDateTime.now())
        .jobType("qa")
        .contextId(pitchId)
        .build();
    
    jobs.put(jobId, job);
    
    // Execute async via separate bean (ensures @Async proxy works)
    asyncAIExecutor.executeQAAsync(jobId, pitchId, question, jobs);
    
    log.info("Started async Q&A job {} for pitch {}", jobId, pitchId);
    return jobId;
  }

  /**
   * Get Q&A result
   */
  public Optional<RiskQuestionResponse> getQAResult(String jobId) {
    @SuppressWarnings("unchecked")
    AIJob<RiskQuestionResponse> job = (AIJob<RiskQuestionResponse>) jobs.get(jobId);
    if (job == null || job.getStatus() != JobStatus.COMPLETED) {
      return Optional.empty();
    }
    return Optional.ofNullable(job.getResult());
  }

  // ===========================================
  // JOB STATUS & MANAGEMENT
  // ===========================================

  /**
   * Get job status
   */
  public Optional<JobStatusResponse> getJobStatus(String jobId) {
    AIJob<?> job = jobs.get(jobId);
    if (job == null) {
      return Optional.empty();
    }
    
    return Optional.of(JobStatusResponse.builder()
        .jobId(job.getJobId())
        .status(job.getStatus())
        .jobType(job.getJobType())
        .contextId(job.getContextId())
        .createdAt(job.getCreatedAt())
        .completedAt(job.getCompletedAt())
        .errorMessage(job.getErrorMessage())
        .hasResult(job.getStatus() == JobStatus.COMPLETED && job.getResult() != null)
        .build());
  }

  /**
   * Cancel a pending/processing job
   */
  public boolean cancelJob(String jobId) {
    AIJob<?> job = jobs.get(jobId);
    if (job == null) {
      return false;
    }
    if (job.getStatus() == JobStatus.PENDING || job.getStatus() == JobStatus.PROCESSING) {
      job.setStatus(JobStatus.FAILED);
      job.setErrorMessage("Job cancelled by user");
      job.setCompletedAt(LocalDateTime.now());
      return true;
    }
    return false;
  }

  /**
   * Cleanup old completed jobs (runs every 10 minutes)
   */
  @Scheduled(fixedRate = 600000)
  public void cleanupOldJobs() {
    LocalDateTime cutoff = LocalDateTime.now().minusMinutes(JOB_TTL_MINUTES);
    int removed = 0;
    
    for (Map.Entry<String, AIJob<?>> entry : jobs.entrySet()) {
      AIJob<?> job = entry.getValue();
      if (job.getCompletedAt() != null && job.getCompletedAt().isBefore(cutoff)) {
        jobs.remove(entry.getKey());
        removed++;
      }
    }
    
    if (removed > 0) {
      log.info("Cleaned up {} old AI jobs", removed);
    }
  }

  /**
   * Get current job statistics
   */
  public Map<String, Object> getJobStats() {
    long pending = jobs.values().stream().filter(j -> j.getStatus() == JobStatus.PENDING).count();
    long processing = jobs.values().stream().filter(j -> j.getStatus() == JobStatus.PROCESSING).count();
    long completed = jobs.values().stream().filter(j -> j.getStatus() == JobStatus.COMPLETED).count();
    long failed = jobs.values().stream().filter(j -> j.getStatus() == JobStatus.FAILED).count();
    
    return Map.of(
        "total", jobs.size(),
        "pending", pending,
        "processing", processing,
        "completed", completed,
        "failed", failed
    );
  }

  private String generateJobId() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  /**
   * Find an existing job with matching type and context that is still active (PENDING or PROCESSING).
   * Used for request deduplication to prevent parallel duplicate AI calls.
   */
  private Optional<String> findExistingJob(String jobType, Long contextId) {
    return jobs.entrySet().stream()
        .filter(e -> jobType.equals(e.getValue().getJobType()))
        .filter(e -> contextId.equals(e.getValue().getContextId()))
        .filter(e -> e.getValue().getStatus() == JobStatus.PENDING 
                  || e.getValue().getStatus() == JobStatus.PROCESSING)
        .map(Map.Entry::getKey)
        .findFirst();
  }

  /**
   * Get the jobs map (for AsyncAIExecutor access).
   */
  public Map<String, AIJob<?>> getJobs() {
    return jobs;
  }
}
