package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateHillChartPointRequest;
import com.github.farzadsedaghatbin.shipflow.dto.HillChartPointDTO;
import com.github.farzadsedaghatbin.shipflow.dto.UpdateHillChartPointRequest;
import com.github.farzadsedaghatbin.shipflow.dto.hillchart.ConfidenceAnalysisDTO;
import com.github.farzadsedaghatbin.shipflow.dto.hillchart.HillChartHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.hillchart.UpdateHillChartPositionRequest;
import com.github.farzadsedaghatbin.shipflow.entity.HillChartHistory;
import com.github.farzadsedaghatbin.shipflow.entity.HillChartPoint;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.HillChartHistoryRepository;
import com.github.farzadsedaghatbin.shipflow.repository.HillChartPointRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HillChartService {

    private static final double HOURS_PER_DAY = 8.0;

    private final HillChartPointRepository hillChartPointRepository;
    private final HillChartHistoryRepository hillChartHistoryRepository;
    private final PitchRepository pitchRepository;
    private final UserRepository userRepository;
    private final WorkLogRepository workLogRepository;

    public List<HillChartPointDTO> getAllHillChartPoints() {
        return hillChartPointRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<HillChartPointDTO> getHillChartPointsByPitch(Long pitchId) {
        return hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(pitchId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public HillChartPointDTO getHillChartPointById(Long id) {
        HillChartPoint point = hillChartPointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hill chart point not found with id: " + id));
        return toDTO(point);
    }

    public HillChartPointDTO createHillChartPoint(CreateHillChartPointRequest request) {
        Pitch pitch = pitchRepository.findById(request.getPitchId())
                .orElseThrow(() -> new RuntimeException("Pitch not found with id: " + request.getPitchId()));

        HillChartPoint point = HillChartPoint.builder()
                .pitch(pitch)
                .scope(request.getScope())
                .description(request.getDescription())
                .position(request.getPosition())
                .build();

        HillChartPoint saved = hillChartPointRepository.save(point);
        return toDTO(saved);
    }

    public HillChartPointDTO updateHillChartPoint(Long id, UpdateHillChartPointRequest request) {
        HillChartPoint point = hillChartPointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hill chart point not found with id: " + id));

        if (request.getScope() != null) {
            point.setScope(request.getScope());
        }
        if (request.getDescription() != null) {
            point.setDescription(request.getDescription());
        }
        if (request.getPosition() != null) {
            point.setPosition(request.getPosition());
        }

        HillChartPoint saved = hillChartPointRepository.save(point);
        return toDTO(saved);
    }

    public void deleteHillChartPoint(Long id) {
        if (!hillChartPointRepository.existsById(id)) {
            throw new RuntimeException("Hill chart point not found with id: " + id);
        }
        hillChartPointRepository.deleteById(id);
    }

    /**
     * Update hill chart point position with history tracking.
     * Stores the move for confidence analysis.
     */
    public HillChartPointDTO updatePositionWithHistory(Long pointId, UpdateHillChartPositionRequest request, Long userId) {
        HillChartPoint point = hillChartPointRepository.findById(pointId)
                .orElseThrow(() -> new RuntimeException("Hill chart point not found with id: " + pointId));

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        int previousPosition = point.getPosition();
        
        // Get current progress metrics
        Pitch pitch = point.getPitch();
        Double totalHours = workLogRepository.getTotalHoursByPitchId(pitch.getId());
        if (totalHours == null) totalHours = 0.0;
        double appetiteHours = pitch.getAppetiteDays() * HOURS_PER_DAY;
        double progress = appetiteHours > 0 ? (totalHours / appetiteHours) * 100 : 0;

        // Create history entry
        HillChartHistory history = HillChartHistory.builder()
                .hillChartPoint(point)
                .pitch(pitch)
                .movedBy(user)
                .previousPosition(previousPosition)
                .newPosition(request.getNewPosition())
                .confidenceLevel(request.getConfidenceLevel())
                .note(request.getNote())
                .actualHoursAtMove(totalHours)
                .progressAtMove(progress)
                .build();
        
        hillChartHistoryRepository.save(history);
        log.info("Hill chart position updated: point {} moved from {} to {} by user {}", 
                pointId, previousPosition, request.getNewPosition(), userId);

        // Update the point position
        point.setPosition(request.getNewPosition());
        HillChartPoint saved = hillChartPointRepository.save(point);
        
        return toDTO(saved);
    }

    /**
     * Get position history for a hill chart point.
     */
    @Transactional(readOnly = true)
    public List<HillChartHistoryDTO> getPointHistory(Long pointId) {
        return hillChartHistoryRepository.findByHillChartPointIdOrderByCreatedAtDesc(pointId).stream()
                .map(this::toHistoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all position history for a pitch.
     */
    @Transactional(readOnly = true)
    public List<HillChartHistoryDTO> getPitchHistory(Long pitchId) {
        return hillChartHistoryRepository.findByPitchIdOrderByCreatedAtDesc(pitchId).stream()
                .map(this::toHistoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get confidence analysis for a pitch.
     * Compares user confidence vs actual progress.
     */
    @Transactional(readOnly = true)
    public ConfidenceAnalysisDTO getConfidenceAnalysis(Long pitchId) {
        Pitch pitch = pitchRepository.findById(pitchId)
                .orElseThrow(() -> new RuntimeException("Pitch not found with id: " + pitchId));

        // Get average confidence from history
        Double avgConfidence = hillChartHistoryRepository.getAverageConfidenceByPitch(pitchId);
        if (avgConfidence == null) avgConfidence = 50.0;

        // Calculate actual progress
        Double totalHours = workLogRepository.getTotalHoursByPitchId(pitchId);
        if (totalHours == null) totalHours = 0.0;
        double appetiteHours = pitch.getAppetiteDays() * HOURS_PER_DAY;
        double actualProgress = appetiteHours > 0 ? (totalHours / appetiteHours) * 100 : 0;

        // Calculate confidence delta
        double confidenceDelta = avgConfidence - actualProgress;

        // Determine accuracy assessment
        String assessment;
        if (Math.abs(confidenceDelta) < 10) {
            assessment = "ACCURATE";
        } else if (confidenceDelta > 20) {
            assessment = "OVERCONFIDENT";
        } else if (confidenceDelta < -20) {
            assessment = "UNDERCONFIDENT";
        } else if (confidenceDelta > 0) {
            assessment = "SLIGHTLY_OVERCONFIDENT";
        } else {
            assessment = "SLIGHTLY_UNDERCONFIDENT";
        }

        // Get history
        List<HillChartHistoryDTO> history = getPitchHistory(pitchId);

        // Generate insights
        List<String> insights = generateConfidenceInsights(avgConfidence, actualProgress, confidenceDelta, history);

        return ConfidenceAnalysisDTO.builder()
                .pitchId(pitchId)
                .pitchTitle(pitch.getTitle())
                .averageConfidence(Math.round(avgConfidence * 10.0) / 10.0)
                .actualProgress(Math.round(actualProgress * 10.0) / 10.0)
                .confidenceDelta(Math.round(confidenceDelta * 10.0) / 10.0)
                .accuracyAssessment(assessment)
                .history(history)
                .insights(insights)
                .build();
    }

    private List<String> generateConfidenceInsights(double avgConfidence, double actualProgress, 
                                                     double confidenceDelta, List<HillChartHistoryDTO> history) {
        List<String> insights = new ArrayList<>();

        if (Math.abs(confidenceDelta) < 10) {
            insights.add("Team confidence aligns well with actual progress");
        } else if (confidenceDelta > 20) {
            insights.add("Team may be overconfident - actual progress is lower than perceived");
            insights.add("Consider reviewing scope or identifying blockers");
        } else if (confidenceDelta < -20) {
            insights.add("Team may be underconfident - progress is better than perceived");
            insights.add("This could indicate good risk management or conservative estimates");
        }

        if (history.size() > 5) {
            long frequentMoves = history.stream()
                    .filter(h -> Math.abs(h.getNewPosition() - h.getPreviousPosition()) > 10)
                    .count();
            if (frequentMoves > history.size() / 2) {
                insights.add("Frequent large position changes may indicate uncertainty in scope");
            }
        }

        if (actualProgress > 80 && avgConfidence < 60) {
            insights.add("High progress with low confidence - team may be more accomplished than they realize");
        }

        return insights;
    }

    private HillChartHistoryDTO toHistoryDTO(HillChartHistory history) {
        return HillChartHistoryDTO.builder()
                .id(history.getId())
                .hillChartPointId(history.getHillChartPoint().getId())
                .scope(history.getHillChartPoint().getScope())
                .pitchId(history.getPitch().getId())
                .pitchTitle(history.getPitch().getTitle())
                .userId(history.getMovedBy() != null ? history.getMovedBy().getId() : null)
                .userName(history.getMovedBy() != null ? history.getMovedBy().getUsername() : null)
                .previousPosition(history.getPreviousPosition())
                .newPosition(history.getNewPosition())
                .confidenceLevel(history.getConfidenceLevel())
                .note(history.getNote())
                .actualHoursAtMove(history.getActualHoursAtMove())
                .progressAtMove(history.getProgressAtMove())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private HillChartPointDTO toDTO(HillChartPoint point) {
        return HillChartPointDTO.builder()
                .id(point.getId())
                .pitchId(point.getPitch().getId())
                .pitchTitle(point.getPitch().getTitle())
                .cycleId(point.getPitch().getCycle() != null ? point.getPitch().getCycle().getId() : null)
                .cycleName(point.getPitch().getCycle() != null ? point.getPitch().getCycle().getName() : null)
                .projectId(point.getPitch().getCycle() != null && point.getPitch().getCycle().getProject() != null ? point.getPitch().getCycle().getProject().getId() : null)
                .projectName(point.getPitch().getCycle() != null && point.getPitch().getCycle().getProject() != null ? point.getPitch().getCycle().getProject().getName() : null)
                .projectKey(point.getPitch().getCycle() != null && point.getPitch().getCycle().getProject() != null ? point.getPitch().getCycle().getProject().getProjectKey() : null)
                .scope(point.getScope())
                .description(point.getDescription())
                .position(point.getPosition())
                .createdAt(point.getCreatedAt())
                .updatedAt(point.getUpdatedAt())
                .build();
    }
}
