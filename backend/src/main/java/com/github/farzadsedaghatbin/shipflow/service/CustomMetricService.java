package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.metrics.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.github.farzadsedaghatbin.shipflow.service.MetricFormulaParser.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing custom metrics and calculating their values
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomMetricService {

    private final CustomMetricRepository customMetricRepository;
    private final MetricDataPointRepository metricDataPointRepository;
    private final UserRepository userRepository;
    private final PitchRepository pitchRepository;
    private final CycleRepository cycleRepository;
    private final WorkLogRepository workLogRepository;
    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final MetricFormulaParser formulaParser;
    private final MessageService messageService;

    /**
     * Create a new custom metric
     */
    @Transactional
    public CustomMetricDTO createMetric(Long userId, CreateCustomMetricRequest request) {
        log.info("Creating custom metric '{}' for user {}", request.getName(), userId);

        // Validate formula
        ValidationResult validation = formulaParser.validateFormula(request.getFormula());
        if (!validation.isValid()) {
            throw new IllegalArgumentException(messageService.getMessage("error.metric.formula.invalid", validation.getErrorMessage()));
        }

        // Check for duplicate name
        if (customMetricRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new IllegalArgumentException(messageService.getMessage("error.metric.name.exists", request.getName()));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Resolve scope entities if provided
        Cycle cycle = null;
        if (request.getCycleId() != null) {
            cycle = cycleRepository.findById(request.getCycleId())
                    .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + request.getCycleId()));
        }

        Pitch pitch = null;
        if (request.getPitchId() != null) {
            pitch = pitchRepository.findById(request.getPitchId())
                    .orElseThrow(() -> new IllegalArgumentException("Pitch not found: " + request.getPitchId()));
        }

        Team team = null;
        if (request.getTeamId() != null) {
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("Team not found: " + request.getTeamId()));
        }

        CustomMetric metric = CustomMetric.builder()
                .user(user)
                .cycle(cycle)
                .pitch(pitch)
                .team(team)
                .name(request.getName())
                .description(request.getDescription())
                .formula(request.getFormula())
                .dataSource(request.getDataSource())
                .aggregationType(request.getAggregationType())
                .filters(request.getFilters())
                .displayFormat(request.getDisplayFormat() != null ? request.getDisplayFormat() : CustomMetric.DisplayFormat.NUMBER)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        CustomMetric saved = customMetricRepository.save(metric);
        log.info("Created custom metric with ID: {}", saved.getId());

        return toDTO(saved);
    }

    /**
     * Update an existing custom metric
     */
    @Transactional
    public CustomMetricDTO updateMetric(Long userId, Long metricId, UpdateCustomMetricRequest request) {
        log.info("Updating custom metric {} for user {}", metricId, userId);

        CustomMetric metric = customMetricRepository.findById(metricId)
                .orElseThrow(() -> new IllegalArgumentException("Metric not found: " + metricId));

        // Verify ownership
        if (!metric.getUser().getId().equals(userId)) {
            throw new SecurityException("Not authorized to update this metric");
        }

        // Update fields if provided
        if (request.getName() != null) {
            // Check for duplicate name (excluding current metric)
            if (!request.getName().equals(metric.getName()) &&
                    customMetricRepository.existsByUserIdAndName(userId, request.getName())) {
                throw new IllegalArgumentException(messageService.getMessage("error.metric.name.exists", request.getName()));
            }
            metric.setName(request.getName());
        }

        if (request.getDescription() != null) {
            metric.setDescription(request.getDescription());
        }

        if (request.getFormula() != null) {
            ValidationResult validation = formulaParser.validateFormula(request.getFormula());
            if (!validation.isValid()) {
                throw new IllegalArgumentException(messageService.getMessage("error.metric.formula.invalid", validation.getErrorMessage()));
            }
            metric.setFormula(request.getFormula());
        }

        if (request.getDataSource() != null) {
            metric.setDataSource(request.getDataSource());
        }

        if (request.getAggregationType() != null) {
            metric.setAggregationType(request.getAggregationType());
        }

        if (request.getFilters() != null) {
            metric.setFilters(request.getFilters());
        }

        if (request.getDisplayFormat() != null) {
            metric.setDisplayFormat(request.getDisplayFormat());
        }

        if (request.getIsActive() != null) {
            metric.setIsActive(request.getIsActive());
        }

        CustomMetric updated = customMetricRepository.save(metric);
        log.info("Updated custom metric: {}", updated.getId());

        return toDTO(updated);
    }

    /**
     * Delete a custom metric
     */
    @Transactional
    public void deleteMetric(Long userId, Long metricId) {
        log.info("Deleting custom metric {} for user {}", metricId, userId);

        CustomMetric metric = customMetricRepository.findById(metricId)
                .orElseThrow(() -> new IllegalArgumentException("Metric not found: " + metricId));

        // Verify ownership
        if (!metric.getUser().getId().equals(userId)) {
            throw new SecurityException("Not authorized to delete this metric");
        }

        customMetricRepository.delete(metric);
        log.info("Deleted custom metric: {}", metricId);
    }

    /**
     * Get all metrics for a user
     */
    public List<CustomMetricDTO> getUserMetrics(Long userId) {
        return customMetricRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific metric
     */
    public CustomMetricDTO getMetric(Long userId, Long metricId) {
        CustomMetric metric = customMetricRepository.findById(metricId)
                .orElseThrow(() -> new IllegalArgumentException("Metric not found: " + metricId));

        if (!metric.getUser().getId().equals(userId)) {
            throw new SecurityException("Not authorized to view this metric");
        }

        return toDTO(metric);
    }

    /**
     * Calculate the current value of a metric
     */
    @Transactional
    public MetricValueDTO calculateMetricValue(Long userId, Long metricId) {
        log.info("Calculating value for metric {} (user: {})", metricId, userId);

        CustomMetric metric = customMetricRepository.findById(metricId)
                .orElseThrow(() -> new IllegalArgumentException("Metric not found: " + metricId));

        if (!metric.getUser().getId().equals(userId)) {
            throw new SecurityException("Not authorized to calculate this metric");
        }

        // Build data context based on data source
        Map<String, Object> dataContext = buildDataContext(metric);

        // Evaluate formula
        BigDecimal value = formulaParser.evaluateFormula(metric.getFormula(), dataContext);

        // Save data point
        MetricDataPoint dataPoint = MetricDataPoint.builder()
                .metric(metric)
                .calculatedValue(value)
                .calculationDate(LocalDateTime.now())
                .metadata(buildMetadata(dataContext))
                .build();

        metricDataPointRepository.save(dataPoint);

        return MetricValueDTO.builder()
                .metricId(metric.getId())
                .metricName(metric.getName())
                .value(value)
                .displayValue(formatValue(value, metric.getDisplayFormat()))
                .calculatedAt(LocalDateTime.now())
                .metadata(buildMetadata(dataContext))
                .build();
    }

    /**
     * Get historical values for a metric
     */
    public List<MetricValueDTO> getMetricHistory(Long userId, Long metricId, int limit) {
        CustomMetric metric = customMetricRepository.findById(metricId)
                .orElseThrow(() -> new IllegalArgumentException("Metric not found: " + metricId));

        if (!metric.getUser().getId().equals(userId)) {
            throw new SecurityException("Not authorized to view this metric history");
        }

        return metricDataPointRepository.findTopNByMetricId(metricId, limit).stream()
                .map(dp -> MetricValueDTO.builder()
                        .metricId(metric.getId())
                        .metricName(metric.getName())
                        .value(dp.getCalculatedValue())
                        .displayValue(formatValue(dp.getCalculatedValue(), metric.getDisplayFormat()))
                        .calculatedAt(dp.getCalculationDate())
                        .metadata(dp.getMetadata())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Validate a formula without saving
     */
    public ValidationResult validateFormula(String formula) {
        return formulaParser.validateFormula(formula);
    }

    private Map<String, Object> buildDataContext(CustomMetric metric) {
        Map<String, Object> context = new HashMap<>();

        switch (metric.getDataSource()) {
            case PITCH:
                List<Pitch> pitches = pitchRepository.findAll();
                context.put("totalPitches", pitches.size());
                context.put("completedPitches", pitches.stream().filter(p -> p.getStatus() == PitchStatus.DONE).count());
                context.put("inProgressPitches", pitches.stream().filter(p -> p.getStatus() == PitchStatus.IN_PROGRESS).count());
                break;

            case CYCLE:
                List<Cycle> cycles = cycleRepository.findAll();
                context.put("totalCycles", cycles.size());
                context.put("activeCycles", cycles.stream().filter(Cycle::getIsActive).count());
                break;

            case WORK_LOG:
                List<WorkLog> workLogs = workLogRepository.findAll();
                double totalHours = workLogs.stream()
                        .mapToDouble(wl -> wl.getHoursSpent().doubleValue())
                        .sum();
                context.put("totalWorkLogHours", totalHours);
                context.put("totalWorkLogs", workLogs.size());
                break;

            case TASK:
                List<Task> tasks = taskRepository.findAll();
                context.put("totalTasks", tasks.size());
                context.put("completedTasks", tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count());
                break;

            default:
                // CUSTOM or other sources
                break;
        }

        return context;
    }

    private String buildMetadata(Map<String, Object> dataContext) {
        // Convert data context to JSON-like string
        return dataContext.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":" + e.getValue())
                .collect(Collectors.joining(",", "{", "}"));
    }

    private String formatValue(BigDecimal value, CustomMetric.DisplayFormat format) {
        if (value == null) return "N/A";

        DecimalFormat df;
        switch (format) {
            case PERCENTAGE:
                df = new DecimalFormat("#,##0.00");
                return df.format(value) + "%";
            case CURRENCY:
                df = new DecimalFormat("$#,##0.00");
                return df.format(value);
            case DURATION:
                df = new DecimalFormat("#,##0.00");
                return df.format(value) + "h";
            case DECIMAL:
                df = new DecimalFormat("#,##0.00");
                return df.format(value);
            case NUMBER:
            default:
                df = new DecimalFormat("#,##0");
                return df.format(value);
        }
    }

    private CustomMetricDTO toDTO(CustomMetric metric) {
        return CustomMetricDTO.builder()
                .id(metric.getId())
                .name(metric.getName())
                .description(metric.getDescription())
                .formula(metric.getFormula())
                .dataSource(metric.getDataSource())
                .aggregationType(metric.getAggregationType())
                .filters(metric.getFilters())
                .displayFormat(metric.getDisplayFormat())
                .isActive(metric.getIsActive())
                .cycleId(metric.getCycle() != null ? metric.getCycle().getId() : null)
                .cycleName(metric.getCycle() != null ? metric.getCycle().getName() : null)
                .pitchId(metric.getPitch() != null ? metric.getPitch().getId() : null)
                .pitchName(metric.getPitch() != null ? metric.getPitch().getTitle() : null)
                .teamId(metric.getTeam() != null ? metric.getTeam().getId() : null)
                .teamName(metric.getTeam() != null ? metric.getTeam().getName() : null)
                .createdAt(metric.getCreatedAt())
                .updatedAt(metric.getUpdatedAt())
                .build();
    }
}
