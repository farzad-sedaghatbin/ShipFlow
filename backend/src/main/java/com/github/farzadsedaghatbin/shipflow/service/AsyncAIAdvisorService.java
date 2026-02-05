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
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Async service for AI advisory operations.
 * Handles long-running AI calls asynchronously to prevent blocking API responses.
 * 
 * IMPORTANT: This service checks cache first and returns immediately if cached.
 * Only uncached requests go through the async job flow.
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

  private final RiskAnalysisService riskAnalysisService;
  private final AICacheService cacheService;

  // In-memory job storage (consider Redis for production clustering)
  private final Map<String, AIJob<?>> jobs = new ConcurrentHashMap<>();
  
  // Job TTL in minutes (cleanup completed jobs after this time)
  private static final int JOB_TTL_MINUTES = 30;

  @Autowired
  public AsyncAIAdvisorService(RiskAnalysisService riskAnalysisService, 
      @Autowired(required = false) AICacheService cacheService) {
    this.riskAnalysisService = riskAnalysisService;
    this.cacheService = cacheService;
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
   * Start async pitch risk analysis with AI
   * @return jobId for polling
   */
  public String startPitchRiskAnalysis(Long pitchId) {
    String jobId = generateJobId();
    
    AIJob<PitchRiskDTO> job = AIJob.<PitchRiskDTO>builder()
        .jobId(jobId)
        .status(JobStatus.PENDING)
        .createdAt(LocalDateTime.now())
        .jobType("pitch_risk")
        .contextId(pitchId)
        .build();
    
    jobs.put(jobId, job);
    
    // Execute async
    executePitchRiskAnalysisAsync(jobId, pitchId);
    
    log.info("Started async pitch risk analysis job {} for pitch {}", jobId, pitchId);
    return jobId;
  }

  @Async("aiTaskExecutor")
  protected void executePitchRiskAnalysisAsync(String jobId, Long pitchId) {
    @SuppressWarnings("unchecked")
    AIJob<PitchRiskDTO> job = (AIJob<PitchRiskDTO>) jobs.get(jobId);
    if (job == null) return;

    try {
      job.setStatus(JobStatus.PROCESSING);
      
      // This is the potentially slow AI call
      PitchRiskDTO result = riskAnalysisService.analyzePitchRisk(pitchId, true);
      
      job.setResult(result);
      job.setStatus(JobStatus.COMPLETED);
      job.setCompletedAt(LocalDateTime.now());
      
      log.info("Completed async pitch risk analysis job {} for pitch {}", jobId, pitchId);
    } catch (Exception e) {
      log.error("Failed async pitch risk analysis job {} for pitch {}: {}", jobId, pitchId, e.getMessage());
      job.setStatus(JobStatus.FAILED);
      job.setErrorMessage(e.getMessage());
      job.setCompletedAt(LocalDateTime.now());
    }
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
   * Start async cycle risk analysis with AI
   * @return jobId for polling
   */
  public String startCycleRiskAnalysis(Long cycleId) {
    String jobId = generateJobId();
    
    AIJob<CycleRiskOverviewDTO> job = AIJob.<CycleRiskOverviewDTO>builder()
        .jobId(jobId)
        .status(JobStatus.PENDING)
        .createdAt(LocalDateTime.now())
        .jobType("cycle_risk")
        .contextId(cycleId)
        .build();
    
    jobs.put(jobId, job);
    
    // Execute async
    executeCycleRiskAnalysisAsync(jobId, cycleId);
    
    log.info("Started async cycle risk analysis job {} for cycle {}", jobId, cycleId);
    return jobId;
  }

  @Async("aiTaskExecutor")
  protected void executeCycleRiskAnalysisAsync(String jobId, Long cycleId) {
    @SuppressWarnings("unchecked")
    AIJob<CycleRiskOverviewDTO> job = (AIJob<CycleRiskOverviewDTO>) jobs.get(jobId);
    if (job == null) return;

    try {
      job.setStatus(JobStatus.PROCESSING);
      
      // This is the potentially slow AI call
      CycleRiskOverviewDTO result = riskAnalysisService.getCycleRiskOverview(cycleId, true);
      
      job.setResult(result);
      job.setStatus(JobStatus.COMPLETED);
      job.setCompletedAt(LocalDateTime.now());
      
      log.info("Completed async cycle risk analysis job {} for cycle {}", jobId, cycleId);
    } catch (Exception e) {
      log.error("Failed async cycle risk analysis job {} for cycle {}: {}", jobId, cycleId, e.getMessage());
      job.setStatus(JobStatus.FAILED);
      job.setErrorMessage(e.getMessage());
      job.setCompletedAt(LocalDateTime.now());
    }
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
   * Start async Q&A with AI advisor
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
    
    // Execute async
    executeQAAsync(jobId, pitchId, question);
    
    log.info("Started async Q&A job {} for pitch {}", jobId, pitchId);
    return jobId;
  }

  @Async("aiTaskExecutor")
  protected void executeQAAsync(String jobId, Long pitchId, String question) {
    @SuppressWarnings("unchecked")
    AIJob<RiskQuestionResponse> job = (AIJob<RiskQuestionResponse>) jobs.get(jobId);
    if (job == null) return;

    try {
      job.setStatus(JobStatus.PROCESSING);
      
      // This is the potentially slow AI call
      RiskQuestionResponse result = riskAnalysisService.answerRiskQuestion(pitchId, question);
      
      job.setResult(result);
      job.setStatus(JobStatus.COMPLETED);
      job.setCompletedAt(LocalDateTime.now());
      
      log.info("Completed async Q&A job {} for pitch {}", jobId, pitchId);
    } catch (Exception e) {
      log.error("Failed async Q&A job {} for pitch {}: {}", jobId, pitchId, e.getMessage());
      job.setStatus(JobStatus.FAILED);
      job.setErrorMessage(e.getMessage());
      job.setCompletedAt(LocalDateTime.now());
    }
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
}
