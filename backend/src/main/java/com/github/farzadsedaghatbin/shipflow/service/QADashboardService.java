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

import java.util.ArrayList;
import java.util.List;

/**
 * Service for QA dashboard and coverage statistics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QADashboardService {

    private final TestCaseRepository testCaseRepository;
    private final BugReportRepository bugReportRepository;
    private final TestRunRepository testRunRepository;
    private final PitchRepository pitchRepository;
    private final CycleRepository cycleRepository;
    private final MessageService messageService;

    @Value("${app.qa.test-management.enabled:true}")
    private boolean testManagementEnabled;

    @Value("${app.qa.ai-test-generation.enabled:true}")
    private boolean aiTestGenerationEnabled;

    /**
     * Get QA test management feature status.
     */
    public QATestManagementStatusDTO getStatus() {
        long totalTestCases = testManagementEnabled ? testCaseRepository.count() : 0;
        long totalBugReports = testManagementEnabled ? bugReportRepository.count() : 0;
        long totalTestRuns = testManagementEnabled ? testRunRepository.count() : 0;
        long aiGeneratedTestCases = testManagementEnabled ? 
                testCaseRepository.findAllAiGenerated().size() : 0;

        return QATestManagementStatusDTO.builder()
                .testManagementEnabled(testManagementEnabled)
                .aiTestGenerationEnabled(aiTestGenerationEnabled)
                .totalTestCases(totalTestCases)
                .totalBugReports(totalBugReports)
                .totalTestRuns(totalTestRuns)
                .aiGeneratedTestCases(aiGeneratedTestCases)
                .build();
    }

    /**
     * Get test coverage for a pitch.
     */
    @Transactional(readOnly = true)
    public TestCoverageDTO getTestCoverageByPitch(Long pitchId) {
        checkFeatureEnabled();

        Pitch pitch = pitchRepository.findById(pitchId)
                .orElseThrow(() -> new RuntimeException("Pitch not found: " + pitchId));

        long totalTestCases = testCaseRepository.countByPitchId(pitchId);
        long draftTestCases = testCaseRepository.countByPitchIdAndStatus(pitchId, TestCaseStatus.DRAFT);
        long readyTestCases = testCaseRepository.countByPitchIdAndStatus(pitchId, TestCaseStatus.READY);
        long approvedTestCases = testCaseRepository.countByPitchIdAndStatus(pitchId, TestCaseStatus.APPROVED);

        long totalRuns = testRunRepository.countByPitchId(pitchId);
        long passedRuns = testRunRepository.countByPitchIdAndStatus(pitchId, TestRunStatus.PASSED);
        long failedRuns = testRunRepository.countByPitchIdAndStatus(pitchId, TestRunStatus.FAILED);
        long blockedRuns = testRunRepository.countByPitchIdAndStatus(pitchId, TestRunStatus.BLOCKED);
        long skippedRuns = testRunRepository.countByPitchIdAndStatus(pitchId, TestRunStatus.SKIPPED);

        long totalBugs = bugReportRepository.countByPitchId(pitchId);
        long openBugs = bugReportRepository.countOpenByPitchId(pitchId);
        long criticalBugs = bugReportRepository.countByPitchIdAndSeverity(pitchId, BugSeverity.CRITICAL);
        long blockerBugs = bugReportRepository.countByPitchIdAndSeverity(pitchId, BugSeverity.BLOCKER);

        // Calculate coverage metrics
        double testCaseCoverage = totalTestCases > 0 ? 
                (double) (readyTestCases + approvedTestCases) / totalTestCases * 100 : 0;
        double passRate = totalRuns > 0 ? (double) passedRuns / totalRuns * 100 : 0;
        double coverageScore = calculateCoverageScore(totalTestCases, totalRuns, passedRuns, openBugs, blockerBugs);

        return TestCoverageDTO.builder()
                .pitchId(pitchId)
                .pitchTitle(pitch.getTitle())
                .cycleId(pitch.getCycle() != null ? pitch.getCycle().getId() : null)
                .cycleName(pitch.getCycle() != null ? pitch.getCycle().getName() : null)
                .totalTestCases(totalTestCases)
                .draftTestCases(draftTestCases)
                .readyTestCases(readyTestCases)
                .approvedTestCases(approvedTestCases)
                .totalRuns(totalRuns)
                .passedRuns(passedRuns)
                .failedRuns(failedRuns)
                .blockedRuns(blockedRuns)
                .skippedRuns(skippedRuns)
                .testCaseCoverage(testCaseCoverage)
                .passRate(passRate)
                .coverageScore(coverageScore)
                .totalBugs(totalBugs)
                .openBugs(openBugs)
                .criticalBugs(criticalBugs)
                .blockerBugs(blockerBugs)
                .build();
    }

    /**
     * Get QA dashboard for a cycle.
     */
    @Transactional(readOnly = true)
    public QADashboardDTO getQADashboardByCycle(Long cycleId) {
        checkFeatureEnabled();

        Cycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new RuntimeException("Cycle not found: " + cycleId));

        // Test case statistics
        List<TestCase> testCases = testCaseRepository.findByCycleId(cycleId);
        long totalTestCases = testCases.size();
        long draftTestCases = testCases.stream()
                .filter(tc -> tc.getStatus() == TestCaseStatus.DRAFT).count();
        long readyTestCases = testCases.stream()
                .filter(tc -> tc.getStatus() == TestCaseStatus.READY).count();
        long approvedTestCases = testCases.stream()
                .filter(tc -> tc.getStatus() == TestCaseStatus.APPROVED).count();
        long deprecatedTestCases = testCases.stream()
                .filter(tc -> tc.getStatus() == TestCaseStatus.DEPRECATED).count();
        long aiGeneratedTestCases = testCases.stream()
                .filter(tc -> tc.getAiGenerated() != null && tc.getAiGenerated()).count();

        // Test run statistics
        long totalRuns = testRunRepository.countByCycleId(cycleId);
        long passedRuns = testRunRepository.countByCycleIdAndStatus(cycleId, TestRunStatus.PASSED);
        long failedRuns = testRunRepository.countByCycleIdAndStatus(cycleId, TestRunStatus.FAILED);
        long blockedRuns = testRunRepository.countByCycleIdAndStatus(cycleId, TestRunStatus.BLOCKED);
        long skippedRuns = testRunRepository.countByCycleIdAndStatus(cycleId, TestRunStatus.SKIPPED);
        long pendingRuns = testRunRepository.countByCycleIdAndStatus(cycleId, TestRunStatus.PENDING);

        // Bug statistics
        List<BugReport> bugs = bugReportRepository.findByCycleId(cycleId);
        long totalBugs = bugs.size();
        long openBugs = bugs.stream()
                .filter(b -> b.getStatus() == BugStatus.OPEN).count();
        long inProgressBugs = bugs.stream()
                .filter(b -> b.getStatus() == BugStatus.IN_PROGRESS).count();
        long resolvedBugs = bugs.stream()
                .filter(b -> b.getStatus() == BugStatus.RESOLVED).count();
        long verifiedBugs = bugs.stream()
                .filter(b -> b.getStatus() == BugStatus.VERIFIED).count();
        long closedBugs = bugs.stream()
                .filter(b -> b.getStatus() == BugStatus.CLOSED).count();

        // Bug severity breakdown
        long trivialBugs = bugs.stream()
                .filter(b -> b.getSeverity() == BugSeverity.TRIVIAL).count();
        long minorBugs = bugs.stream()
                .filter(b -> b.getSeverity() == BugSeverity.MINOR).count();
        long majorBugs = bugs.stream()
                .filter(b -> b.getSeverity() == BugSeverity.MAJOR).count();
        long criticalBugs = bugs.stream()
                .filter(b -> b.getSeverity() == BugSeverity.CRITICAL).count();
        long blockerBugs = bugs.stream()
                .filter(b -> b.getSeverity() == BugSeverity.BLOCKER).count();

        // Coverage metrics
        double overallPassRate = totalRuns > 0 ? (double) passedRuns / totalRuns * 100 : 0;
        double overallCoverage = calculateOverallCoverage(totalTestCases, totalRuns, passedRuns);

        // Per-pitch coverage
        List<TestCoverageDTO> pitchCoverage = new ArrayList<>();
        for (Pitch pitch : cycle.getPitches()) {
            pitchCoverage.add(getTestCoverageByPitch(pitch.getId()));
        }

        return QADashboardDTO.builder()
                .cycleId(cycleId)
                .cycleName(cycle.getName())
                .totalTestCases(totalTestCases)
                .draftTestCases(draftTestCases)
                .readyTestCases(readyTestCases)
                .approvedTestCases(approvedTestCases)
                .deprecatedTestCases(deprecatedTestCases)
                .aiGeneratedTestCases(aiGeneratedTestCases)
                .totalRuns(totalRuns)
                .passedRuns(passedRuns)
                .failedRuns(failedRuns)
                .blockedRuns(blockedRuns)
                .skippedRuns(skippedRuns)
                .pendingRuns(pendingRuns)
                .totalBugs(totalBugs)
                .openBugs(openBugs)
                .inProgressBugs(inProgressBugs)
                .resolvedBugs(resolvedBugs)
                .verifiedBugs(verifiedBugs)
                .closedBugs(closedBugs)
                .trivialBugs(trivialBugs)
                .minorBugs(minorBugs)
                .majorBugs(majorBugs)
                .criticalBugs(criticalBugs)
                .blockerBugs(blockerBugs)
                .overallPassRate(overallPassRate)
                .overallCoverage(overallCoverage)
                .pitchCoverage(pitchCoverage)
                .build();
    }

    private double calculateCoverageScore(long totalTestCases, long totalRuns, 
                                          long passedRuns, long openBugs, long blockerBugs) {
        if (totalTestCases == 0) return 0;
        
        // Base score from test execution
        double executionCoverage = totalRuns > 0 ? 
                Math.min(100, (double) totalRuns / totalTestCases * 100) : 0;
        
        // Pass rate contribution
        double passRate = totalRuns > 0 ? (double) passedRuns / totalRuns * 100 : 0;
        
        // Bug penalty
        double bugPenalty = Math.min(30, (openBugs * 2) + (blockerBugs * 5));
        
        // Final score
        double score = (executionCoverage * 0.4) + (passRate * 0.6) - bugPenalty;
        return Math.max(0, Math.min(100, score));
    }

    private double calculateOverallCoverage(long totalTestCases, long totalRuns, long passedRuns) {
        if (totalTestCases == 0) return 0;
        
        double executionRate = (double) totalRuns / totalTestCases * 100;
        double passRate = totalRuns > 0 ? (double) passedRuns / totalRuns * 100 : 0;
        
        return Math.min(100, (executionRate * 0.5) + (passRate * 0.5));
    }

    private void checkFeatureEnabled() {
        if (!testManagementEnabled) {
            throw new RuntimeException(messageService.getMessage("error.qa.test.management.disabled"));
        }
    }
}
