package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.PitchRiskHistory;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRiskHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for saving risk history records.
 * Separated to ensure proper transaction handling with REQUIRES_NEW propagation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RiskHistoryService {

    private final PitchRiskHistoryRepository riskHistoryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Save risk analysis result to history for trend tracking.
     * Uses REQUIRES_NEW to ensure this runs in its own transaction,
     * avoiding issues with parent transaction read-only mode.
     *
     * @param pitch The pitch being analyzed
     * @param riskDTO The risk analysis result
     * @param triggerType What triggered this snapshot
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRiskHistory(Pitch pitch, PitchRiskDTO riskDTO, PitchRiskHistory.TriggerType triggerType) {
        try {
            // Serialize risk factors to JSON
            String riskFactorsJson = objectMapper.writeValueAsString(riskDTO.getRiskFactors());

            PitchRiskHistory history = PitchRiskHistory.builder()
                    .pitch(pitch)
                    .riskScore(riskDTO.getRiskScore())
                    .riskLevel(riskDTO.getRiskLevel())
                    .riskFactorsJson(riskFactorsJson)
                    .recordedAt(LocalDateTime.now())
                    .triggerType(triggerType)
                    .build();

            riskHistoryRepository.save(history);
            log.debug("Saved risk history for pitch {} with score {} (trigger: {})",
                    pitch.getId(), riskDTO.getRiskScore(), triggerType);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize risk factors for pitch {}: {}", pitch.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to save risk history for pitch {}: {}", pitch.getId(), e.getMessage());
        }
    }
}
