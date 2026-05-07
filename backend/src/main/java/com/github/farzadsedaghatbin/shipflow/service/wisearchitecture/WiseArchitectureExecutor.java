package com.github.farzadsedaghatbin.shipflow.service.wisearchitecture;

import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.*;
import com.github.farzadsedaghatbin.shipflow.service.wisearchitecture.WiseArchitectureJob.JobStatus;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Dedicated executor for async WISE Architecture operations.
 * 
 * This class exists as a SEPARATE BEAN to ensure Spring AOP properly intercepts
 * @Async methods. When a bean calls its own @Async method (self-invocation),
 * the proxy is bypassed and the call executes synchronously on the caller's thread.
 * 
 * By extracting async execution to this separate component:
 * - AsyncWiseArchitectureService calls WiseArchitectureExecutor (different bean = proxy works)
 * - @Async annotation is properly honored
 * - Operations execute on the dedicated aiTaskExecutor thread pool
 * - HTTP request threads are freed immediately
 */
@Component
@Slf4j
public class WiseArchitectureExecutor {

    private final WiseArchitectureService wiseArchitectureService;

    public WiseArchitectureExecutor(WiseArchitectureService wiseArchitectureService) {
        this.wiseArchitectureService = wiseArchitectureService;
    }

    /**
     * Execute stack detection asynchronously on the AI thread pool.
     * 
     * @param jobId The unique job identifier
     * @param request The detection request
     * @param jobs The shared job map for status updates
     */
    @Async("aiTaskExecutor")
    public void executeDetectStacksAsync(
            String jobId, 
            DetectStacksRequestDTO request, 
            Map<String, WiseArchitectureJob<?>> jobs) {
        
        log.info("🚀 Executing detect-stacks on thread: {}", Thread.currentThread().getName());
        
        @SuppressWarnings("unchecked")
        WiseArchitectureJob<DetectStacksResponseDTO> job = 
            (WiseArchitectureJob<DetectStacksResponseDTO>) jobs.get(jobId);
        
        if (job == null) {
            log.warn("Job {} not found, skipping execution", jobId);
            return;
        }

        try {
            job.setStatus(JobStatus.PROCESSING);
            job.updateProgress(5, "Starting stack detection...");
            
            // Create a progress callback for the service
            ProgressCallback callback = (percent, message) -> {
                job.updateProgress(percent, message);
                log.debug("Job {} progress: {}% - {}", jobId, percent, message);
            };
            
            // This is the potentially slow operation (30-180+ seconds for large repos)
            DetectStacksResponseDTO result = wiseArchitectureService.detectStacksWithProgress(request, callback);
            
            job.complete(result);
            
            log.info("✅ Completed async detect-stacks job {} for pitch {} - found {} stacks", 
                jobId, request.getPitchId(), result.getDetectedStacks().size());
                
        } catch (Exception e) {
            log.error("❌ Failed async detect-stacks job {} for pitch {}: {}", 
                jobId, request.getPitchId(), e.getMessage(), e);
            job.fail(e.getMessage());
        }
    }

    /**
     * Execute solution analysis asynchronously on the AI thread pool.
     * 
     * @param jobId The unique job identifier
     * @param request The analysis request
     * @param jobs The shared job map for status updates
     */
    @Async("aiTaskExecutor")
    public void executeAnalyzeAsync(
            String jobId, 
            WiseArchitectureRequestDTO request, 
            Map<String, WiseArchitectureJob<?>> jobs) {
        
        log.info("🚀 Executing analyze on thread: {}", Thread.currentThread().getName());
        
        @SuppressWarnings("unchecked")
        WiseArchitectureJob<WiseArchitectureResponseDTO> job = 
            (WiseArchitectureJob<WiseArchitectureResponseDTO>) jobs.get(jobId);
        
        if (job == null) {
            log.warn("Job {} not found, skipping execution", jobId);
            return;
        }

        try {
            job.setStatus(JobStatus.PROCESSING);
            job.updateProgress(5, "Starting solution analysis...");
            
            // Create a progress callback for the service
            ProgressCallback callback = (percent, message) -> {
                job.updateProgress(percent, message);
                log.debug("Job {} progress: {}% - {}", jobId, percent, message);
            };
            
            // This is the potentially slow operation (60-300+ seconds)
            WiseArchitectureResponseDTO result = wiseArchitectureService.analyzeWithProgress(request, callback);
            
            job.complete(result);
            
            log.info("✅ Completed async analyze job {} for pitch {} on thread {}", 
                jobId, request.getPitchId(), Thread.currentThread().getName());
                
        } catch (Exception e) {
            log.error("❌ Failed async analyze job {} for pitch {}: {}", 
                jobId, request.getPitchId(), e.getMessage(), e);
            job.fail(e.getMessage());
        }
    }

    /**
     * Functional interface for progress callbacks
     */
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int percent, String message);
    }
}
