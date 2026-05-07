package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.risk.CycleRiskOverviewDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.RiskQuestionResponse;
import com.github.farzadsedaghatbin.shipflow.service.AsyncAIAdvisorService.AIJob;
import com.github.farzadsedaghatbin.shipflow.service.AsyncAIAdvisorService.JobStatus;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Dedicated executor for async AI operations.
 * 
 * This class exists as a SEPARATE BEAN to ensure Spring AOP properly intercepts
 * @Async methods. When a bean calls its own @Async method (self-invocation),
 * the proxy is bypassed and the call executes synchronously on the caller's thread.
 * 
 * By extracting async execution to this separate component:
 * - AsyncAIAdvisorService calls AsyncAIExecutor (different bean = proxy works)
 * - @Async annotation is properly honored
 * - AI calls execute on the dedicated aiTaskExecutor thread pool
 * - Tomcat request threads are freed immediately
 */
@Component
@Slf4j
public class AsyncAIExecutor {

  private final RiskAnalysisService riskAnalysisService;

  public AsyncAIExecutor(RiskAnalysisService riskAnalysisService) {
    this.riskAnalysisService = riskAnalysisService;
  }

  /**
   * Execute pitch risk analysis asynchronously on the AI thread pool.
   * 
   * @param jobId The unique job identifier
   * @param pitchId The pitch to analyze
   * @param jobs The shared job map for status updates
   */
  @Async("aiTaskExecutor")
  public void executePitchRiskAnalysisAsync(String jobId, Long pitchId, Map<String, AIJob<?>> jobs) {
    log.debug("🚀 Executing pitch risk analysis on thread: {}", Thread.currentThread().getName());
    
    @SuppressWarnings("unchecked")
    AIJob<PitchRiskDTO> job = (AIJob<PitchRiskDTO>) jobs.get(jobId);
    if (job == null) {
      log.warn("Job {} not found, skipping execution", jobId);
      return;
    }

    try {
      job.setStatus(JobStatus.PROCESSING);
      
      // This is the potentially slow AI call (5-120+ seconds)
      PitchRiskDTO result = riskAnalysisService.analyzePitchRisk(pitchId, true);
      
      job.setResult(result);
      job.setStatus(JobStatus.COMPLETED);
      job.setCompletedAt(LocalDateTime.now());
      
      log.info("✅ Completed async pitch risk analysis job {} for pitch {} on thread {}", 
          jobId, pitchId, Thread.currentThread().getName());
    } catch (Exception e) {
      log.error("❌ Failed async pitch risk analysis job {} for pitch {}: {}", 
          jobId, pitchId, e.getMessage(), e);
      job.setStatus(JobStatus.FAILED);
      job.setErrorMessage(e.getMessage());
      job.setCompletedAt(LocalDateTime.now());
    }
  }

  /**
   * Execute cycle risk analysis asynchronously on the AI thread pool.
   * 
   * @param jobId The unique job identifier
   * @param cycleId The cycle to analyze
   * @param jobs The shared job map for status updates
   */
  @Async("aiTaskExecutor")
  public void executeCycleRiskAnalysisAsync(String jobId, Long cycleId, Map<String, AIJob<?>> jobs) {
    log.debug("🚀 Executing cycle risk analysis on thread: {}", Thread.currentThread().getName());
    
    @SuppressWarnings("unchecked")
    AIJob<CycleRiskOverviewDTO> job = (AIJob<CycleRiskOverviewDTO>) jobs.get(jobId);
    if (job == null) {
      log.warn("Job {} not found, skipping execution", jobId);
      return;
    }

    try {
      job.setStatus(JobStatus.PROCESSING);
      
      // This is the potentially slow AI call (can take 30-300+ seconds for cycles with many pitches)
      CycleRiskOverviewDTO result = riskAnalysisService.getCycleRiskOverview(cycleId, true);
      
      job.setResult(result);
      job.setStatus(JobStatus.COMPLETED);
      job.setCompletedAt(LocalDateTime.now());
      
      log.info("✅ Completed async cycle risk analysis job {} for cycle {} on thread {}", 
          jobId, cycleId, Thread.currentThread().getName());
    } catch (Exception e) {
      log.error("❌ Failed async cycle risk analysis job {} for cycle {}: {}", 
          jobId, cycleId, e.getMessage(), e);
      job.setStatus(JobStatus.FAILED);
      job.setErrorMessage(e.getMessage());
      job.setCompletedAt(LocalDateTime.now());
    }
  }

  /**
   * Execute Q&A asynchronously on the AI thread pool.
   * 
   * @param jobId The unique job identifier
   * @param pitchId The pitch context for the question
   * @param question The user's question
   * @param jobs The shared job map for status updates
   */
  @Async("aiTaskExecutor")
  public void executeQAAsync(String jobId, Long pitchId, String question, Map<String, AIJob<?>> jobs) {
    log.debug("🚀 Executing Q&A on thread: {}", Thread.currentThread().getName());
    
    @SuppressWarnings("unchecked")
    AIJob<RiskQuestionResponse> job = (AIJob<RiskQuestionResponse>) jobs.get(jobId);
    if (job == null) {
      log.warn("Job {} not found, skipping execution", jobId);
      return;
    }

    try {
      job.setStatus(JobStatus.PROCESSING);
      
      // This is the potentially slow AI call
      RiskQuestionResponse result = riskAnalysisService.answerRiskQuestion(pitchId, question);
      
      job.setResult(result);
      job.setStatus(JobStatus.COMPLETED);
      job.setCompletedAt(LocalDateTime.now());
      
      log.info("✅ Completed async Q&A job {} for pitch {} on thread {}", 
          jobId, pitchId, Thread.currentThread().getName());
    } catch (Exception e) {
      log.error("❌ Failed async Q&A job {} for pitch {}: {}", 
          jobId, pitchId, e.getMessage(), e);
      job.setStatus(JobStatus.FAILED);
      job.setErrorMessage(e.getMessage());
      job.setCompletedAt(LocalDateTime.now());
    }
  }
}
