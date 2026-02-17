package com.github.farzadsedaghatbin.shipflow.service.wisearchitecture;

import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.*;
import com.github.farzadsedaghatbin.shipflow.service.wisearchitecture.WiseArchitectureJob.JobStatus;
import com.github.farzadsedaghatbin.shipflow.service.wisearchitecture.WiseArchitectureJob.JobType;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Async service for WISE Architecture operations.
 * Handles long-running AI and MCP calls asynchronously to prevent HTTP timeouts.
 * 
 * Flow:
 * 1. Client calls start*Async() - receives jobId immediately
 * 2. Job executes in background on AI thread pool
 * 3. Client polls getJobStatus() for progress updates
 * 4. When complete, client calls get*Result() to retrieve result
 * 
 * Features:
 * - Request deduplication (same request returns existing jobId if active)
 * - Automatic job cleanup (30-minute TTL)
 * - Progress tracking with human-readable messages
 */
@Service
@Slf4j
public class AsyncWiseArchitectureService {

    private final WiseArchitectureExecutor executor;
    
    // In-memory job storage
    private final Map<String, WiseArchitectureJob<?>> jobs = new ConcurrentHashMap<>();
    
    // Job TTL in minutes
    private static final int JOB_TTL_MINUTES = 30;

    public AsyncWiseArchitectureService(WiseArchitectureExecutor executor) {
        this.executor = executor;
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
        private Long pitchId;
        private Integer progress;
        private String progressMessage;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        private String errorMessage;
        private boolean hasResult;
    }

    // ===========================================
    // DETECT STACKS (ASYNC)
    // ===========================================

    /**
     * Start async stack detection.
     * Includes request deduplication - returns existing job if active.
     * 
     * @param request The detection request
     * @return jobId for polling
     */
    public String startDetectStacks(DetectStacksRequestDTO request) {
        String requestHash = computeDetectStacksHash(request);
        
        // Deduplication: Check if a job with same parameters is already active
        Optional<String> existingJobId = findExistingJob(JobType.DETECT_STACKS, request.getPitchId(), requestHash);
        if (existingJobId.isPresent()) {
            log.info("Reusing existing detect-stacks job {} for pitch {}", 
                existingJobId.get(), request.getPitchId());
            return existingJobId.get();
        }
        
        String jobId = generateJobId();
        
        WiseArchitectureJob<DetectStacksResponseDTO> job = WiseArchitectureJob.<DetectStacksResponseDTO>builder()
            .jobId(jobId)
            .jobType(JobType.DETECT_STACKS)
            .status(JobStatus.PENDING)
            .progress(0)
            .progressMessage("Initializing...")
            .pitchId(request.getPitchId())
            .requestHash(requestHash)
            .createdAt(LocalDateTime.now())
            .build();
        
        jobs.put(jobId, job);
        
        // Execute async via separate bean (ensures @Async proxy works)
        executor.executeDetectStacksAsync(jobId, request, jobs);
        
        log.info("Started async detect-stacks job {} for pitch {}", jobId, request.getPitchId());
        return jobId;
    }

    /**
     * Get detect stacks result if completed
     */
    public Optional<DetectStacksResponseDTO> getDetectStacksResult(String jobId) {
        @SuppressWarnings("unchecked")
        WiseArchitectureJob<DetectStacksResponseDTO> job = 
            (WiseArchitectureJob<DetectStacksResponseDTO>) jobs.get(jobId);
        
        if (job == null || job.getStatus() != JobStatus.COMPLETED) {
            return Optional.empty();
        }
        return Optional.ofNullable(job.getResult());
    }

    // ===========================================
    // ANALYZE SOLUTION (ASYNC)
    // ===========================================

    /**
     * Start async solution analysis.
     * Includes request deduplication - returns existing job if active.
     * 
     * @param request The analysis request
     * @return jobId for polling
     */
    public String startAnalyze(WiseArchitectureRequestDTO request) {
        String requestHash = computeAnalyzeHash(request);
        
        // Deduplication: Check if a job with same parameters is already active
        Optional<String> existingJobId = findExistingJob(JobType.ANALYZE_SOLUTION, request.getPitchId(), requestHash);
        if (existingJobId.isPresent()) {
            log.info("Reusing existing analyze job {} for pitch {}", 
                existingJobId.get(), request.getPitchId());
            return existingJobId.get();
        }
        
        String jobId = generateJobId();
        
        WiseArchitectureJob<WiseArchitectureResponseDTO> job = WiseArchitectureJob.<WiseArchitectureResponseDTO>builder()
            .jobId(jobId)
            .jobType(JobType.ANALYZE_SOLUTION)
            .status(JobStatus.PENDING)
            .progress(0)
            .progressMessage("Initializing...")
            .pitchId(request.getPitchId())
            .requestHash(requestHash)
            .createdAt(LocalDateTime.now())
            .build();
        
        jobs.put(jobId, job);
        
        // Execute async via separate bean (ensures @Async proxy works)
        executor.executeAnalyzeAsync(jobId, request, jobs);
        
        log.info("Started async analyze job {} for pitch {} with {} stacks", 
            jobId, request.getPitchId(), request.getSelectedStacks().size());
        return jobId;
    }

    /**
     * Get analyze result if completed
     */
    public Optional<WiseArchitectureResponseDTO> getAnalyzeResult(String jobId) {
        @SuppressWarnings("unchecked")
        WiseArchitectureJob<WiseArchitectureResponseDTO> job = 
            (WiseArchitectureJob<WiseArchitectureResponseDTO>) jobs.get(jobId);
        
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
        WiseArchitectureJob<?> job = jobs.get(jobId);
        if (job == null) {
            return Optional.empty();
        }
        
        return Optional.of(JobStatusResponse.builder()
            .jobId(job.getJobId())
            .status(job.getStatus())
            .jobType(job.getJobType().getValue())
            .pitchId(job.getPitchId())
            .progress(job.getProgress())
            .progressMessage(job.getProgressMessage())
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
        WiseArchitectureJob<?> job = jobs.get(jobId);
        if (job == null) {
            return false;
        }
        if (job.isActive()) {
            job.cancel();
            log.info("Cancelled job {}", jobId);
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
        
        // Collect keys to remove first, then remove them
        List<String> keysToRemove = jobs.entrySet().stream()
            .filter(e -> e.getValue().getCompletedAt() != null)
            .filter(e -> e.getValue().getCompletedAt().isBefore(cutoff))
            .map(Map.Entry::getKey)
            .toList();
        
        keysToRemove.forEach(jobs::remove);
        
        if (!keysToRemove.isEmpty()) {
            log.info("Cleaned up {} old WISE Architecture jobs", keysToRemove.size());
        }
    }

    /**
     * Get current job statistics
     */
    public Map<String, Object> getJobStats() {
        // Collect all statistics in a single pass for consistency and efficiency
        Map<JobStatus, Long> statusCounts = jobs.values().stream()
            .collect(Collectors.groupingBy(WiseArchitectureJob::getStatus, Collectors.counting()));
        
        return Map.of(
            "total", jobs.size(),
            "pending", statusCounts.getOrDefault(JobStatus.PENDING, 0L),
            "processing", statusCounts.getOrDefault(JobStatus.PROCESSING, 0L),
            "completed", statusCounts.getOrDefault(JobStatus.COMPLETED, 0L),
            "failed", statusCounts.getOrDefault(JobStatus.FAILED, 0L)
        );
    }

    // ===========================================
    // HELPER METHODS
    // ===========================================

    private String generateJobId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Find an existing active job with matching type and parameters.
     * Used for request deduplication.
     */
    private Optional<String> findExistingJob(JobType jobType, Long pitchId, String requestHash) {
        return jobs.entrySet().stream()
            .filter(e -> e.getValue().getJobType() == jobType)
            .filter(e -> pitchId.equals(e.getValue().getPitchId()))
            .filter(e -> requestHash.equals(e.getValue().getRequestHash()))
            .filter(e -> e.getValue().isActive())
            .map(Map.Entry::getKey)
            .findFirst();
    }

    /**
     * Compute hash for detect-stacks request for deduplication
     */
    private String computeDetectStacksHash(DetectStacksRequestDTO request) {
        return String.format("ds_%d_%s_%s", 
            request.getPitchId(),
            request.getRepositoryIds().toString(),
            request.getRepositoryBranches() != null ? request.getRepositoryBranches().toString() : "default");
    }

    /**
     * Compute hash for analyze request for deduplication
     */
    private String computeAnalyzeHash(WiseArchitectureRequestDTO request) {
        return String.format("an_%d_%s_%s_%s", 
            request.getPitchId(),
            request.getRepositoryIds().toString(),
            request.getSelectedStacks().toString(),
            request.getRepositoryBranches() != null ? request.getRepositoryBranches().toString() : "default");
    }

    /**
     * Get the jobs map (for executor access)
     */
    public Map<String, WiseArchitectureJob<?>> getJobs() {
        return jobs;
    }
}
