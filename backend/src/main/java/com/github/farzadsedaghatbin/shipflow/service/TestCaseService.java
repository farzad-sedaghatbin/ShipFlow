package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.qa.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing test cases.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final TestRunRepository testRunRepository;
    private final PitchRepository pitchRepository;
    private final CycleRepository cycleRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final HillChartPointRepository hillChartPointRepository;
    private final TaskRepository taskRepository;
    private final LocalizationService localizationService;

    @Value("${app.qa.test-management.enabled:true}")
    private boolean testManagementEnabled;

    /**
     * Create a new test case.
     */
    @Transactional
    public TestCaseDTO createTestCase(CreateTestCaseRequest request, Long userId) {
        checkFeatureEnabled();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        TestCase testCase = TestCase.builder()
                .testCaseKey(generateTestCaseKey())
                .title(request.getTitle())
                .description(request.getDescription())
                .preconditions(request.getPreconditions())
                .steps(request.getSteps())
                .expectedResult(request.getExpectedResult())
                .type(request.getType())
                .priority(request.getPriority())
                .status(request.getStatus() != null ? request.getStatus() : TestCaseStatus.DRAFT)
                .aiGenerated(request.getAiGenerated() != null ? request.getAiGenerated() : false)
                .tags(request.getTags() != null ? String.join(",", request.getTags()) : null)
                .estimatedMinutes(request.getEstimatedMinutes())
                .createdBy(user)
                .build();

        // Set relationships
        if (request.getPitchId() != null) {
            Pitch pitch = pitchRepository.findById(request.getPitchId())
                    .orElseThrow(() -> new RuntimeException("Pitch not found: " + request.getPitchId()));
            testCase.setPitch(pitch);
            testCase.setCycle(pitch.getCycle());
            testCase.setTeam(pitch.getTeam());
        } else {
            if (request.getCycleId() != null) {
                Cycle cycle = cycleRepository.findById(request.getCycleId())
                        .orElseThrow(() -> new RuntimeException("Cycle not found: " + request.getCycleId()));
                testCase.setCycle(cycle);
            }
            if (request.getTeamId() != null) {
                Team team = teamRepository.findById(request.getTeamId())
                        .orElseThrow(() -> new RuntimeException("Team not found: " + request.getTeamId()));
                testCase.setTeam(team);
            }
        }

        // Set scope if provided
        if (request.getScopeId() != null) {
            HillChartPoint scope = hillChartPointRepository.findById(request.getScopeId())
                    .orElseThrow(() -> new IllegalArgumentException("Scope not found: " + request.getScopeId()));
            testCase.setScope(scope);
        }

        // Set task if provided
        if (request.getTaskId() != null) {
            Task task = taskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + request.getTaskId()));
            testCase.setTask(task);
        }

        testCase = testCaseRepository.save(testCase);
        log.info("Created test case: {} by user: {}", testCase.getTestCaseKey(), userId);
        
        return toDTO(testCase);
    }

    /**
     * Update an existing test case.
     */
    @Transactional
    public TestCaseDTO updateTestCase(Long id, UpdateTestCaseRequest request, Long userId) {
        checkFeatureEnabled();

        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test case not found: " + id));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (request.getTitle() != null) testCase.setTitle(request.getTitle());
        if (request.getDescription() != null) testCase.setDescription(request.getDescription());
        if (request.getPreconditions() != null) testCase.setPreconditions(request.getPreconditions());
        if (request.getSteps() != null) testCase.setSteps(request.getSteps());
        if (request.getExpectedResult() != null) testCase.setExpectedResult(request.getExpectedResult());
        if (request.getType() != null) testCase.setType(request.getType());
        if (request.getPriority() != null) testCase.setPriority(request.getPriority());
        if (request.getStatus() != null) testCase.setStatus(request.getStatus());
        if (request.getTags() != null) testCase.setTags(String.join(",", request.getTags()));
        if (request.getEstimatedMinutes() != null) testCase.setEstimatedMinutes(request.getEstimatedMinutes());

        // Update relationships
        if (request.getPitchId() != null) {
            Pitch pitch = pitchRepository.findById(request.getPitchId())
                    .orElseThrow(() -> new RuntimeException("Pitch not found: " + request.getPitchId()));
            testCase.setPitch(pitch);
        }
        if (request.getCycleId() != null) {
            Cycle cycle = cycleRepository.findById(request.getCycleId())
                    .orElseThrow(() -> new RuntimeException("Cycle not found: " + request.getCycleId()));
            testCase.setCycle(cycle);
        }
        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new RuntimeException("Team not found: " + request.getTeamId()));
            testCase.setTeam(team);
        }

        // Update scope if provided
        if (request.getScopeId() != null) {
            HillChartPoint scope = hillChartPointRepository.findById(request.getScopeId())
                    .orElseThrow(() -> new RuntimeException("Scope not found: " + request.getScopeId()));
            testCase.setScope(scope);
        }

        // Update task if provided
        if (request.getTaskId() != null) {
            Task task = taskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + request.getTaskId()));
            testCase.setTask(task);
        }

        testCase.setUpdatedBy(user);
        testCase = testCaseRepository.save(testCase);
        
        log.info("Updated test case: {} by user: {}", testCase.getTestCaseKey(), userId);
        return toDTO(testCase);
    }

    /**
     * Delete a test case.
     */
    @Transactional
    public void deleteTestCase(Long id) {
        checkFeatureEnabled();
        
        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test case not found: " + id));
        
        testCaseRepository.delete(testCase);
        log.info("Deleted test case: {}", testCase.getTestCaseKey());
    }

    /**
     * Get a test case by ID.
     */
    @Transactional(readOnly = true)
    public TestCaseDTO getTestCaseById(Long id) {
        checkFeatureEnabled();
        
        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test case not found: " + id));
        
        return toDTO(testCase);
    }

    /**
     * Get a test case by key.
     */
    @Transactional(readOnly = true)
    public TestCaseDTO getTestCaseByKey(String key) {
        checkFeatureEnabled();
        
        TestCase testCase = testCaseRepository.findByTestCaseKey(key)
                .orElseThrow(() -> new RuntimeException("Test case not found: " + key));
        
        return toDTO(testCase);
    }

    /**
     * Get all test cases.
     */
    @Transactional(readOnly = true)
    public List<TestCaseDTO> getAllTestCases() {
        checkFeatureEnabled();
        
        return testCaseRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get test cases by pitch.
     */
    @Transactional(readOnly = true)
    public List<TestCaseDTO> getTestCasesByPitch(Long pitchId) {
        checkFeatureEnabled();
        
        return testCaseRepository.findByPitchId(pitchId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get test cases by cycle.
     */
    @Transactional(readOnly = true)
    public List<TestCaseDTO> getTestCasesByCycle(Long cycleId) {
        checkFeatureEnabled();
        
        return testCaseRepository.findByCycleId(cycleId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get test cases by status.
     */
    @Transactional(readOnly = true)
    public List<TestCaseDTO> getTestCasesByStatus(TestCaseStatus status) {
        checkFeatureEnabled();
        
        return testCaseRepository.findByStatus(status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get test cases with filters.
     */
    @Transactional(readOnly = true)
    public List<TestCaseDTO> getTestCasesWithFilters(
            Long cycleId,
            Long pitchId,
            List<TestCaseStatus> statuses,
            List<TestCaseType> types,
            List<TestCasePriority> priorities) {
        checkFeatureEnabled();
        
        // Convert empty lists to null for the query
        List<TestCaseStatus> statusList = (statuses != null && !statuses.isEmpty()) ? statuses : null;
        List<TestCaseType> typeList = (types != null && !types.isEmpty()) ? types : null;
        List<TestCasePriority> priorityList = (priorities != null && !priorities.isEmpty()) ? priorities : null;
        
        return testCaseRepository.findWithFilters(cycleId, pitchId, statusList, typeList, priorityList).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get AI-generated test cases for a pitch.
     */
    @Transactional(readOnly = true)
    public List<TestCaseDTO> getAiGeneratedTestCases(Long pitchId) {
        checkFeatureEnabled();
        
        return testCaseRepository.findAiGeneratedByPitchId(pitchId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Generate unique test case key.
     */
    private String generateTestCaseKey() {
        Integer maxNumber = testCaseRepository.findMaxTestCaseKeyNumber();
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("TC-%03d", nextNumber);
    }

    private void checkFeatureEnabled() {
        if (!testManagementEnabled) {
            throw new RuntimeException(localizationService.getMessage("qa.feature.disabled"));
        }
    }

    /**
     * Convert entity to DTO.
     */
    public TestCaseDTO toDTO(TestCase testCase) {
        long totalRuns = testRunRepository.countByTestCaseId(testCase.getId());
        long passedRuns = testRunRepository.countByPitchIdAndStatus(
                testCase.getPitch() != null ? testCase.getPitch().getId() : null, 
                TestRunStatus.PASSED);
        long failedRuns = testRunRepository.countByPitchIdAndStatus(
                testCase.getPitch() != null ? testCase.getPitch().getId() : null, 
                TestRunStatus.FAILED);

        return TestCaseDTO.builder()
                .id(testCase.getId())
                .testCaseKey(testCase.getTestCaseKey())
                .title(testCase.getTitle())
                .description(testCase.getDescription())
                .preconditions(testCase.getPreconditions())
                .steps(testCase.getSteps())
                .expectedResult(testCase.getExpectedResult())
                .pitchId(testCase.getPitch() != null ? testCase.getPitch().getId() : null)
                .pitchTitle(testCase.getPitch() != null ? testCase.getPitch().getTitle() : null)
                .cycleId(testCase.getCycle() != null ? testCase.getCycle().getId() : null)
                .cycleName(testCase.getCycle() != null ? testCase.getCycle().getName() : null)
                .teamId(testCase.getTeam() != null ? testCase.getTeam().getId() : null)
                .teamName(testCase.getTeam() != null ? testCase.getTeam().getName() : null)
                .scopeId(testCase.getScope() != null ? testCase.getScope().getId() : null)
                .scopeName(testCase.getScope() != null ? testCase.getScope().getScope() : null)
                .taskId(testCase.getTask() != null ? testCase.getTask().getId() : null)
                .taskTitle(testCase.getTask() != null ? testCase.getTask().getTitle() : null)
                .type(testCase.getType())
                .priority(testCase.getPriority())
                .status(testCase.getStatus())
                .aiGenerated(testCase.getAiGenerated())
                .tags(testCase.getTags())
                .tagList(testCase.getTags() != null ? 
                        Arrays.asList(testCase.getTags().split(",")) : null)
                .estimatedMinutes(testCase.getEstimatedMinutes())
                .createdById(testCase.getCreatedBy() != null ? testCase.getCreatedBy().getId() : null)
                .createdByName(testCase.getCreatedBy() != null ? testCase.getCreatedBy().getUsername() : null)
                .updatedById(testCase.getUpdatedBy() != null ? testCase.getUpdatedBy().getId() : null)
                .updatedByName(testCase.getUpdatedBy() != null ? testCase.getUpdatedBy().getUsername() : null)
                .createdAt(testCase.getCreatedAt())
                .updatedAt(testCase.getUpdatedAt())
                .totalRuns(totalRuns)
                .passedRuns(passedRuns)
                .failedRuns(failedRuns)
                .passRate(totalRuns > 0 ? (double) passedRuns / totalRuns * 100 : null)
                .build();
    }
}
