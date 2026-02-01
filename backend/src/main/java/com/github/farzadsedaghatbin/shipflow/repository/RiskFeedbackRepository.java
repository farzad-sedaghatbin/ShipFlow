package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.dto.feedback.RiskFeedbackDTO.FeedbackRating;
import com.github.farzadsedaghatbin.shipflow.entity.RiskFeedback;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for risk feedback entities. */
@Repository
public interface RiskFeedbackRepository extends JpaRepository<RiskFeedback, Long> {

  List<RiskFeedback> findByPitchId(Long pitchId);

  List<RiskFeedback> findByUserId(Long userId);

  List<RiskFeedback> findByRating(FeedbackRating rating);

  @Query("SELECT COUNT(rf) FROM RiskFeedback rf WHERE rf.rating = :rating")
  Long countByRating(@Param("rating") FeedbackRating rating);

  @Query(
      "SELECT AVG(rf.originalRiskScore - rf.suggestedRiskScore) FROM RiskFeedback rf WHERE rf.suggestedRiskScore IS NOT NULL")
  Double getAverageScoreDelta();

  @Query(
      "SELECT rf FROM RiskFeedback rf WHERE rf.createdAt >= :startDate ORDER BY rf.createdAt DESC")
  List<RiskFeedback> findRecentFeedback(@Param("startDate") LocalDateTime startDate);

  @Query(
      "SELECT rf FROM RiskFeedback rf WHERE rf.pitch.cycle.id = :cycleId ORDER BY rf.createdAt DESC")
  List<RiskFeedback> findByCycleId(@Param("cycleId") Long cycleId);

  @Query(
      "SELECT rf.missedFactors FROM RiskFeedback rf WHERE rf.missedFactors IS NOT NULL AND rf.missedFactors <> ''")
  List<String> findAllMissedFactors();

  @Query(
      "SELECT FUNCTION('DATE_FORMAT', rf.createdAt, '%Y-%m') as month, COUNT(rf) "
          + "FROM RiskFeedback rf GROUP BY FUNCTION('DATE_FORMAT', rf.createdAt, '%Y-%m') ORDER BY month")
  List<Object[]> countByMonth();

  boolean existsByPitchIdAndUserId(Long pitchId, Long userId);
}
