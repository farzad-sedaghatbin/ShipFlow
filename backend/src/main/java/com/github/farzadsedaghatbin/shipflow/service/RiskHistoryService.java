package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.PitchRiskHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for saving risk history records. Separated to ensure proper
 * transaction handling with REQUIRES_NEW propagation.
 */
@Service
@Slf4j
public class RiskHistoryService {

  private final ObjectMapper objectMapper;

  @PersistenceContext
  private EntityManager entityManager;

  public RiskHistoryService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Save risk analysis result to history for trend tracking. Uses REQUIRES_NEW
   * with readOnly=false to ensure this runs in its own writable transaction,
   * avoiding issues with parent transaction read-only mode.
   *
   * @param pitch
   *            The pitch being analyzed
   * @param riskDTO
   *            The risk analysis result
   * @param triggerType
   *            What triggered this snapshot
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false)
  public void saveRiskHistory(Pitch pitch, PitchRiskDTO riskDTO, PitchRiskHistory.TriggerType triggerType) {
    try {
      log.debug("Saving risk history for pitch {} in new transaction (readOnly=false)", pitch.getId());

      // Re-fetch the pitch in this new transaction to avoid detached entity issues
      Pitch managedPitch = entityManager.find(Pitch.class, pitch.getId());
      if (managedPitch == null) {
        log.error("Pitch {} not found when trying to save risk history", pitch.getId());
        return;
      }

      // Serialize risk factors to JSON
      String riskFactorsJson = objectMapper.writeValueAsString(riskDTO.getRiskFactors());

      PitchRiskHistory history = PitchRiskHistory.builder().pitch(managedPitch).riskScore(riskDTO.getRiskScore())
          .riskLevel(riskDTO.getRiskLevel()).riskFactorsJson(riskFactorsJson).recordedAt(LocalDateTime.now())
          .triggerType(triggerType).build();

      // Use EntityManager directly to have more control
      entityManager.persist(history);
      entityManager.flush();

      log.debug("Saved risk history for pitch {} with score {} (trigger: {})", pitch.getId(),
          riskDTO.getRiskScore(), triggerType);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize risk factors for pitch {}: {}", pitch.getId(), e.getMessage());
    } catch (Exception e) {
      log.error("Failed to save risk history for pitch {}: {}", pitch.getId(), e.getMessage());
      throw new RuntimeException("Failed to save risk history: " + e.getMessage(), e);
    }
  }
}
