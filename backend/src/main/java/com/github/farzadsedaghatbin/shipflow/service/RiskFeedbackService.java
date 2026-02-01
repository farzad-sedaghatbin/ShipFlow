package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.feedback.CreateRiskFeedbackRequest;
import com.github.farzadsedaghatbin.shipflow.dto.feedback.RiskFeedbackDTO;
import com.github.farzadsedaghatbin.shipflow.dto.feedback.RiskFeedbackDTO.FeedbackRating;
import com.github.farzadsedaghatbin.shipflow.dto.feedback.RiskFeedbackStatsDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.RiskFeedback;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RiskFeedbackRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing AI risk assessment feedback. Used to track accuracy and improve AI over
 * time.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RiskFeedbackService {

  private final RiskFeedbackRepository riskFeedbackRepository;
  private final PitchRepository pitchRepository;
  private final UserRepository userRepository;

  /** Submit feedback for an AI risk assessment. */
  public RiskFeedbackDTO submitFeedback(CreateRiskFeedbackRequest request, Long userId) {
    Pitch pitch =
        pitchRepository
            .findById(request.getPitchId())
            .orElseThrow(
                () -> new RuntimeException("Pitch not found with id: " + request.getPitchId()));

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

    RiskFeedback feedback =
        RiskFeedback.builder()
            .pitch(pitch)
            .user(user)
            .originalRiskScore(request.getOriginalRiskScore())
            .rating(request.getRating())
            .suggestedRiskScore(request.getSuggestedRiskScore())
            .notes(request.getNotes())
            .missedFactors(request.getMissedFactors())
            .build();

    RiskFeedback saved = riskFeedbackRepository.save(feedback);
    log.info("Risk feedback submitted by user {} for pitch {}", userId, request.getPitchId());

    return toDTO(saved);
  }

  /** Get all feedback for a pitch. */
  @Transactional(readOnly = true)
  public List<RiskFeedbackDTO> getFeedbackByPitch(Long pitchId) {
    return riskFeedbackRepository.findByPitchId(pitchId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /** Get all feedback by a user. */
  @Transactional(readOnly = true)
  public List<RiskFeedbackDTO> getFeedbackByUser(Long userId) {
    return riskFeedbackRepository.findByUserId(userId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /** Get feedback for a cycle. */
  @Transactional(readOnly = true)
  public List<RiskFeedbackDTO> getFeedbackByCycle(Long cycleId) {
    return riskFeedbackRepository.findByCycleId(cycleId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /** Get aggregated statistics for all feedback. */
  @Transactional(readOnly = true)
  public RiskFeedbackStatsDTO getFeedbackStats() {
    long total = riskFeedbackRepository.count();

    if (total == 0) {
      return RiskFeedbackStatsDTO.builder()
          .totalFeedback(0L)
          .accurateCount(0L)
          .partiallyAccurateCount(0L)
          .inaccurateCount(0L)
          .overlyPessimisticCount(0L)
          .overlyOptimisticCount(0L)
          .accuracyPercent(0.0)
          .averageScoreDelta(0.0)
          .commonMissedFactors(Collections.emptyList())
          .feedbackByMonth(Collections.emptyMap())
          .build();
    }

    Long accurateCount = riskFeedbackRepository.countByRating(FeedbackRating.ACCURATE);
    Long partialCount = riskFeedbackRepository.countByRating(FeedbackRating.PARTIALLY_ACCURATE);
    Long inaccurateCount = riskFeedbackRepository.countByRating(FeedbackRating.INACCURATE);
    Long pessimisticCount = riskFeedbackRepository.countByRating(FeedbackRating.OVERLY_PESSIMISTIC);
    Long optimisticCount = riskFeedbackRepository.countByRating(FeedbackRating.OVERLY_OPTIMISTIC);

    double accuracyPercent =
        total > 0 ? ((accurateCount + partialCount) / (double) total) * 100 : 0;

    Double avgDelta = riskFeedbackRepository.getAverageScoreDelta();

    // Analyze missed factors
    List<String> allMissedFactors = riskFeedbackRepository.findAllMissedFactors();
    List<String> commonMissedFactors = extractCommonFactors(allMissedFactors);

    // Get monthly breakdown
    Map<String, Long> feedbackByMonth = new LinkedHashMap<>();
    List<Object[]> monthlyData = riskFeedbackRepository.countByMonth();
    for (Object[] row : monthlyData) {
      feedbackByMonth.put((String) row[0], (Long) row[1]);
    }

    return RiskFeedbackStatsDTO.builder()
        .totalFeedback(total)
        .accurateCount(accurateCount != null ? accurateCount : 0L)
        .partiallyAccurateCount(partialCount != null ? partialCount : 0L)
        .inaccurateCount(inaccurateCount != null ? inaccurateCount : 0L)
        .overlyPessimisticCount(pessimisticCount != null ? pessimisticCount : 0L)
        .overlyOptimisticCount(optimisticCount != null ? optimisticCount : 0L)
        .accuracyPercent(Math.round(accuracyPercent * 10.0) / 10.0)
        .averageScoreDelta(avgDelta != null ? Math.round(avgDelta * 10.0) / 10.0 : 0.0)
        .commonMissedFactors(commonMissedFactors)
        .feedbackByMonth(feedbackByMonth)
        .build();
  }

  /** Check if user has already submitted feedback for a pitch. */
  @Transactional(readOnly = true)
  public boolean hasUserSubmittedFeedback(Long pitchId, Long userId) {
    return riskFeedbackRepository.existsByPitchIdAndUserId(pitchId, userId);
  }

  /** Get recent feedback (last 30 days). */
  @Transactional(readOnly = true)
  public List<RiskFeedbackDTO> getRecentFeedback() {
    LocalDateTime startDate = LocalDateTime.now().minusDays(30);
    return riskFeedbackRepository.findRecentFeedback(startDate).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  private RiskFeedbackDTO toDTO(RiskFeedback entity) {
    return RiskFeedbackDTO.builder()
        .id(entity.getId())
        .pitchId(entity.getPitch().getId())
        .pitchTitle(entity.getPitch().getTitle())
        .userId(entity.getUser().getId())
        .userName(entity.getUser().getUsername())
        .originalRiskScore(entity.getOriginalRiskScore())
        .rating(entity.getRating())
        .suggestedRiskScore(entity.getSuggestedRiskScore())
        .notes(entity.getNotes())
        .missedFactors(entity.getMissedFactors())
        .createdAt(entity.getCreatedAt())
        .build();
  }

  private List<String> extractCommonFactors(List<String> allMissedFactors) {
    // Simple word frequency analysis
    Map<String, Integer> wordCount = new HashMap<>();

    for (String factors : allMissedFactors) {
      if (factors != null && !factors.isEmpty()) {
        String[] words = factors.toLowerCase().replaceAll("[^a-zA-Z\\s]", "").split("\\s+");

        for (String word : words) {
          if (word.length() > 3) { // Filter short words
            wordCount.merge(word, 1, Integer::sum);
          }
        }
      }
    }

    // Return top 10 most common factors
    return wordCount.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(10)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }
}
