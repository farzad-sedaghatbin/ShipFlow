package com.github.farzadsedaghatbin.shipflow.service.wisearchitecture;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Represents an async job for WISE Architecture operations.
 * Jobs are stored in-memory and cleaned up after TTL expires.
 *
 * @param <T> The result type (DetectStacksResponseDTO or WiseArchitectureResponseDTO)
 */
@Data
@Builder
public class WiseArchitectureJob<T> {
    
    /**
     * Unique job identifier (8 character UUID prefix)
     */
    private String jobId;
    
    /**
     * Type of job being executed
     */
    private JobType jobType;
    
    /**
     * Current status of the job
     */
    private JobStatus status;
    
    /**
     * Progress percentage (0-100)
     */
    private Integer progress;
    
    /**
     * Human-readable progress message
     */
    private String progressMessage;
    
    /**
     * The pitch ID associated with this job
     */
    private Long pitchId;
    
    /**
     * Request hash for deduplication
     */
    private String requestHash;
    
    /**
     * The result when job completes successfully
     */
    private T result;
    
    /**
     * Error message if job failed
     */
    private String errorMessage;
    
    /**
     * When the job was created
     */
    private LocalDateTime createdAt;
    
    /**
     * When the job completed (success or failure)
     */
    private LocalDateTime completedAt;
    
    /**
     * Job types for WISE Architecture
     */
    public enum JobType {
        DETECT_STACKS("detect_stacks"),
        ANALYZE_SOLUTION("analyze_solution");
        
        private final String value;
        
        JobType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    /**
     * Job status enum
     */
    public enum JobStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    /**
     * Update progress with message
     */
    public void updateProgress(int percent, String message) {
        this.progress = Math.min(percent, 100);
        this.progressMessage = message;
    }
    
    /**
     * Mark job as completed with result
     */
    public void complete(T result) {
        this.status = JobStatus.COMPLETED;
        this.result = result;
        this.progress = 100;
        this.progressMessage = "Completed";
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Mark job as failed with error
     */
    public void fail(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Mark job as cancelled
     */
    public void cancel() {
        this.status = JobStatus.CANCELLED;
        this.errorMessage = "Job cancelled by user";
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Check if job is still active (can be updated)
     */
    public boolean isActive() {
        return status == JobStatus.PENDING || status == JobStatus.PROCESSING;
    }
}
