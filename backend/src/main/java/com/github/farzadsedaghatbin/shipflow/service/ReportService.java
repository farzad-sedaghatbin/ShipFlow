package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.report.CycleReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.report.EnhancedCycleReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.report.MemberWorkReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.report.PitchReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.report.RiskDistributionDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.TeamAssignment;
import com.github.farzadsedaghatbin.shipflow.entity.WorkLog;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TeamMemberRole;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

  private static final double HOURS_PER_DAY = 8.0;

  private final CycleRepository cycleRepository;
  private final PitchRepository pitchRepository;
  private final WorkLogRepository workLogRepository;
  private final PersonRepository personRepository;
  private final TeamAssignmentRepository teamAssignmentRepository;
  private final TaskRepository taskRepository;
  private final RiskAnalysisService riskAnalysisService;
  private final LocalizationService localizationService;

  @SuppressWarnings("null")
  public CycleReportDTO getCycleReport(Long cycleId) {
    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new RuntimeException("Cycle not found with id: " + cycleId));

    List<Pitch> pitches = pitchRepository.findByCycleId(cycleId);
    List<WorkLog> workLogs = workLogRepository.findByCycleId(cycleId);

    // Calculate pitch reports
    List<PitchReportDTO> pitchReports = pitches.stream().map(p -> buildPitchReport(p, workLogs))
        .collect(Collectors.toList());

    // Calculate member reports
    Map<Long, List<WorkLog>> personWorkLogs = workLogs.stream()
        .collect(Collectors.groupingBy(wl -> wl.getPerson().getId()));

    List<MemberWorkReportDTO> memberReports = personWorkLogs.entrySet().stream()
        .map(entry -> buildMemberReport(entry.getKey(), entry.getValue(), cycleId))
        .collect(Collectors.toList());

    // Calculate totals
    int completedPitches = (int) pitches.stream().filter(p -> p.getStatus() == PitchStatus.DONE).count();
    int inProgressPitches = (int) pitches.stream()
        .filter(p -> p.getStatus() == PitchStatus.IN_PROGRESS || p.getStatus() == PitchStatus.STARTED).count();

    double totalAppetiteHours = pitches.stream().mapToDouble(p -> p.getAppetiteDays() * HOURS_PER_DAY).sum();

    double totalActualHours = workLogs.stream().mapToDouble(wl -> wl.getHoursSpent().doubleValue()).sum();

    double efficiency = totalAppetiteHours > 0 ? (totalActualHours / totalAppetiteHours) * 100 : 0;

    // Calculate task statistics (out-of-scope work)
    int totalTasks = taskRepository.countByCycleId(cycleId);
    int completedTasks = taskRepository.countByCycleIdAndStatus(cycleId, TaskStatus.DONE);
    Double totalTaskEstimateHours = taskRepository.getTotalEstimateHoursByCycleId(cycleId);
    Double totalTaskActualHours = taskRepository.getTotalActualHoursByCycleId(cycleId);

    return CycleReportDTO.builder().cycleId(cycle.getId()).cycleName(cycle.getName()).totalPitches(pitches.size())
        .completedPitches(completedPitches).inProgressPitches(inProgressPitches)
        .totalAppetiteHours(totalAppetiteHours).totalActualHours(totalActualHours)
        .efficiencyPercentage(efficiency).totalTasks(totalTasks).completedTasks(completedTasks)
        .totalTaskEstimateHours(totalTaskEstimateHours != null ? totalTaskEstimateHours : 0.0)
        .totalTaskActualHours(totalTaskActualHours != null ? totalTaskActualHours : 0.0)
        .pitchReports(pitchReports).memberReports(memberReports).build();
  }

  public List<PitchReportDTO> getPitchReports(Long cycleId) {
    List<Pitch> pitches = pitchRepository.findByCycleIdNotDeleted(cycleId);
    List<WorkLog> workLogs = workLogRepository.findByCycleId(cycleId);

    return pitches.stream().map(p -> buildPitchReport(p, workLogs)).collect(Collectors.toList());
  }

  public List<MemberWorkReportDTO> getMemberReports(Long cycleId) {
    List<WorkLog> workLogs = workLogRepository.findByCycleId(cycleId);

    Map<Long, List<WorkLog>> personWorkLogs = workLogs.stream()
        .collect(Collectors.groupingBy(wl -> wl.getPerson().getId()));

    return personWorkLogs.entrySet().stream()
        .map(entry -> buildMemberReport(entry.getKey(), entry.getValue(), cycleId))
        .collect(Collectors.toList());
  }

  public String exportCycleReportCsv(Long cycleId) {
    CycleReportDTO report = getCycleReport(cycleId);
    StringBuilder csv = new StringBuilder();

    // Header
    csv.append("Cycle Report: ").append(report.getCycleName()).append("\n\n");

    // Summary
    csv.append("Summary\n");
    csv.append("Total Pitches,").append(report.getTotalPitches()).append("\n");
    csv.append("Completed,").append(report.getCompletedPitches()).append("\n");
    csv.append("In Progress,").append(report.getInProgressPitches()).append("\n");
    csv.append("Total Appetite (hours),").append(report.getTotalAppetiteHours()).append("\n");
    csv.append("Total Actual (hours),").append(report.getTotalActualHours()).append("\n");
    csv.append("Efficiency %,").append(String.format("%.2f", report.getEfficiencyPercentage())).append("\n\n");

    // Out-of-scope work statistics
    csv.append("Out-of-Scope Work (Tasks)\n");
    csv.append("Total Tasks,").append(report.getTotalTasks()).append("\n");
    csv.append("Completed Tasks,").append(report.getCompletedTasks()).append("\n");
    csv.append("Task Estimate (hours),").append(String.format("%.2f", report.getTotalTaskEstimateHours()))
        .append("\n");
    csv.append("Task Actual (hours),").append(String.format("%.2f", report.getTotalTaskActualHours()))
        .append("\n\n");

    // Pitch details
    csv.append("Pitch Reports\n");
    csv.append(
        "Pitch,Team,Status,Appetite Days,Appetite Hours,Actual Hours,Variance Hours,Variance %,Over Budget\n");
    for (PitchReportDTO pitch : report.getPitchReports()) {
      csv.append(escapeCsv(pitch.getPitchTitle())).append(",").append(escapeCsv(pitch.getTeamName())).append(",")
          .append(pitch.getStatus()).append(",").append(pitch.getAppetiteDays()).append(",")
          .append(pitch.getAppetiteHours()).append(",").append(String.format("%.2f", pitch.getActualHours()))
          .append(",").append(String.format("%.2f", pitch.getVarianceHours())).append(",")
          .append(String.format("%.2f", pitch.getVariancePercentage())).append(",")
          .append(pitch.getIsOverBudget()).append("\n");
    }

    csv.append("\nMember Reports\n");
    csv.append("Member,Role,Team,Total Hours,Work Days,Avg Hours/Day\n");
    for (MemberWorkReportDTO member : report.getMemberReports()) {
      csv.append(escapeCsv(member.getMemberName())).append(",").append(member.getRole()).append(",")
          .append(escapeCsv(member.getTeamName())).append(",")
          .append(String.format("%.2f", member.getTotalHours())).append(",").append(member.getWorkDays())
          .append(",").append(String.format("%.2f", member.getAvgHoursPerDay())).append("\n");
    }

    return csv.toString();
  }

  public byte[] exportCycleReportPdf(Long cycleId) {
    CycleReportDTO report = getCycleReport(cycleId);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    try {
      PdfWriter writer = new PdfWriter(baos);
      PdfDocument pdf = new PdfDocument(writer);
      Document document = new Document(pdf);

      // Title
      Paragraph title = new Paragraph("Cycle Report: " + report.getCycleName()).setFontSize(20).setBold()
          .setTextAlignment(TextAlignment.CENTER);
      document.add(title);
      document.add(new Paragraph(" "));

      // Summary Section
      Paragraph summaryHeader = new Paragraph("Summary").setFontSize(16).setBold();
      document.add(summaryHeader);

      Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{3, 2}));
      summaryTable.setWidth(UnitValue.createPercentValue(60));
      addSummaryRow(summaryTable, "Total Pitches", String.valueOf(report.getTotalPitches()));
      addSummaryRow(summaryTable, "Completed", String.valueOf(report.getCompletedPitches()));
      addSummaryRow(summaryTable, "In Progress", String.valueOf(report.getInProgressPitches()));
      addSummaryRow(summaryTable, "Total Appetite (hours)",
          String.format("%.2f", report.getTotalAppetiteHours()));
      addSummaryRow(summaryTable, "Total Actual (hours)", String.format("%.2f", report.getTotalActualHours()));
      addSummaryRow(summaryTable, "Efficiency %", String.format("%.2f%%", report.getEfficiencyPercentage()));
      document.add(summaryTable);
      document.add(new Paragraph(" "));

      // Out-of-Scope Work Section
      Paragraph taskHeader = new Paragraph("Out-of-Scope Work (Tasks)").setFontSize(16).setBold();
      document.add(taskHeader);

      Table taskTable = new Table(UnitValue.createPercentArray(new float[]{3, 2}));
      taskTable.setWidth(UnitValue.createPercentValue(60));
      addSummaryRow(taskTable, "Total Tasks", String.valueOf(report.getTotalTasks()));
      addSummaryRow(taskTable, "Completed Tasks", String.valueOf(report.getCompletedTasks()));
      addSummaryRow(taskTable, "Task Estimate (hours)",
          String.format("%.2f", report.getTotalTaskEstimateHours()));
      addSummaryRow(taskTable, "Task Actual (hours)", String.format("%.2f", report.getTotalTaskActualHours()));
      document.add(taskTable);
      document.add(new Paragraph(" "));

      // Pitch Reports Section
      Paragraph pitchHeader = new Paragraph("Pitch Reports").setFontSize(16).setBold();
      document.add(pitchHeader);

      Table pitchTable = new Table(
          UnitValue.createPercentArray(new float[]{3, 2, 2, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f}));
      pitchTable.setWidth(UnitValue.createPercentValue(100));

      // Header row
      addHeaderCell(pitchTable, "Pitch");
      addHeaderCell(pitchTable, "Team");
      addHeaderCell(pitchTable, "Status");
      addHeaderCell(pitchTable, "Appetite Days");
      addHeaderCell(pitchTable, "Appetite Hours");
      addHeaderCell(pitchTable, "Actual Hours");
      addHeaderCell(pitchTable, "Variance Hours");
      addHeaderCell(pitchTable, "Variance %");
      addHeaderCell(pitchTable, "Over Budget");

      // Data rows
      for (PitchReportDTO pitch : report.getPitchReports()) {
        pitchTable.addCell(new Cell().add(new Paragraph(pitch.getPitchTitle()).setFontSize(9)));
        pitchTable.addCell(new Cell().add(new Paragraph(pitch.getTeamName()).setFontSize(9)));
        pitchTable.addCell(new Cell().add(new Paragraph(pitch.getStatus().toString()).setFontSize(9)));
        pitchTable
            .addCell(new Cell().add(new Paragraph(String.valueOf(pitch.getAppetiteDays())).setFontSize(9)));
        pitchTable.addCell(
            new Cell().add(new Paragraph(String.format("%.1f", pitch.getAppetiteHours())).setFontSize(9)));
        pitchTable.addCell(
            new Cell().add(new Paragraph(String.format("%.1f", pitch.getActualHours())).setFontSize(9)));
        pitchTable.addCell(
            new Cell().add(new Paragraph(String.format("%.1f", pitch.getVarianceHours())).setFontSize(9)));
        pitchTable.addCell(new Cell()
            .add(new Paragraph(String.format("%.1f", pitch.getVariancePercentage())).setFontSize(9)));
        pitchTable
            .addCell(new Cell().add(new Paragraph(pitch.getIsOverBudget() ? "Yes" : "No").setFontSize(9)));
      }
      document.add(pitchTable);
      document.add(new Paragraph(" "));

      // Member Reports Section
      Paragraph memberHeader = new Paragraph("Member Work Summary").setFontSize(16).setBold();
      document.add(memberHeader);

      Table memberTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2, 2, 2}));
      memberTable.setWidth(UnitValue.createPercentValue(100));

      // Header row
      addHeaderCell(memberTable, "Member");
      addHeaderCell(memberTable, "Role");
      addHeaderCell(memberTable, "Team");
      addHeaderCell(memberTable, "Total Hours");
      addHeaderCell(memberTable, "Work Days");
      addHeaderCell(memberTable, "Avg Hours/Day");

      // Data rows
      for (MemberWorkReportDTO member : report.getMemberReports()) {
        memberTable.addCell(new Cell().add(new Paragraph(member.getMemberName()).setFontSize(9)));
        memberTable.addCell(new Cell().add(new Paragraph(member.getRole().toString()).setFontSize(9)));
        memberTable.addCell(new Cell().add(new Paragraph(member.getTeamName()).setFontSize(9)));
        memberTable.addCell(
            new Cell().add(new Paragraph(String.format("%.1f", member.getTotalHours())).setFontSize(9)));
        memberTable.addCell(new Cell().add(new Paragraph(String.valueOf(member.getWorkDays())).setFontSize(9)));
        memberTable.addCell(new Cell()
            .add(new Paragraph(String.format("%.1f", member.getAvgHoursPerDay())).setFontSize(9)));
      }
      document.add(memberTable);

      document.close();
    } catch (Exception e) {
      throw new RuntimeException(localizationService.getMessage("report.pdf.failed"), e);
    }

    return baos.toByteArray();
  }

  private void addSummaryRow(Table table, String label, String value) {
    table.addCell(new Cell().add(new Paragraph(label).setBold()));
    table.addCell(new Cell().add(new Paragraph(value)));
  }

  private void addHeaderCell(Table table, String text) {
    Cell cell = new Cell().add(new Paragraph(text).setBold().setFontSize(10));
    cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
    cell.setTextAlignment(TextAlignment.CENTER);
    table.addHeaderCell(cell);
  }

  private PitchReportDTO buildPitchReport(Pitch pitch, List<WorkLog> allWorkLogs) {
    double actualHours = allWorkLogs.stream().filter(wl -> wl.getPitch().getId().equals(pitch.getId()))
        .mapToDouble(wl -> wl.getHoursSpent().doubleValue()).sum();

    double appetiteHours = pitch.getAppetiteDays() * HOURS_PER_DAY;
    double variance = actualHours - appetiteHours;
    double variancePercent = appetiteHours > 0 ? (variance / appetiteHours) * 100 : 0;

    return PitchReportDTO.builder().pitchId(pitch.getId()).pitchTitle(pitch.getTitle())
        .teamName(pitch.getTeam() != null ? pitch.getTeam().getName() : "Unassigned").status(pitch.getStatus())
        .appetiteDays(pitch.getAppetiteDays()).appetiteHours(appetiteHours).actualHours(actualHours)
        .varianceHours(variance).variancePercentage(variancePercent).isOverBudget(variance > 0).build();
  }

  @SuppressWarnings("null")
  private MemberWorkReportDTO buildMemberReport(Long personId, List<WorkLog> workLogs, Long cycleId) {
    Person person = personRepository.findById(personId).orElseThrow(() -> new RuntimeException("Person not found"));

    // Get the person's assignment for this cycle to determine role and team
    List<TeamAssignment> assignments = teamAssignmentRepository.findByPersonAndCycle(personId, cycleId);
    TeamAssignment assignment = assignments.isEmpty() ? null : assignments.get(0);

    TeamMemberRole role = assignment != null ? assignment.getRole() : TeamMemberRole.FULLSTACK;
    String teamName = assignment != null ? assignment.getTeam().getName() : "Unknown";

    double totalHours = workLogs.stream().mapToDouble(wl -> wl.getHoursSpent().doubleValue()).sum();

    Set<java.time.LocalDate> workDays = workLogs.stream().map(WorkLog::getDate).collect(Collectors.toSet());

    Map<Long, Double> pitchHours = workLogs.stream().collect(Collectors.groupingBy(wl -> wl.getPitch().getId(),
        Collectors.summingDouble(wl -> wl.getHoursSpent().doubleValue())));

    List<MemberWorkReportDTO.PitchWorkSummary> pitchWork = pitchHours.entrySet().stream().map(entry -> {
      WorkLog sample = workLogs.stream().filter(wl -> wl.getPitch().getId().equals(entry.getKey())).findFirst()
          .orElse(null);
      return MemberWorkReportDTO.PitchWorkSummary.builder().pitchId(entry.getKey())
          .pitchTitle(sample != null ? sample.getPitch().getTitle() : "Unknown").hoursSpent(entry.getValue())
          .build();
    }).collect(Collectors.toList());

    return MemberWorkReportDTO.builder().memberId(person.getId()).memberName(person.getName()).role(role)
        .teamName(teamName).totalHours(totalHours).workDays(workDays.size())
        .avgHoursPerDay(workDays.isEmpty() ? 0 : totalHours / workDays.size()).pitchWork(pitchWork).build();
  }

  private String escapeCsv(String value) {
    if (value == null)
      return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  /**
   * Get enhanced cycle report with comprehensive metrics including risk
   * distribution.
   */
  @SuppressWarnings("null")
  public EnhancedCycleReportDTO getEnhancedCycleReport(Long cycleId) {
    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new RuntimeException("Cycle not found with id: " + cycleId));

    List<Pitch> pitches = pitchRepository.findByCycleIdNotDeleted(cycleId);
    List<WorkLog> workLogs = workLogRepository.findByCycleId(cycleId);

    // Calculate pitch reports
    List<PitchReportDTO> pitchReports = pitches.stream().map(p -> buildPitchReport(p, workLogs))
        .collect(Collectors.toList());

    // Calculate member reports
    Map<Long, List<WorkLog>> personWorkLogs = workLogs.stream()
        .collect(Collectors.groupingBy(wl -> wl.getPerson().getId()));

    List<MemberWorkReportDTO> memberReports = personWorkLogs.entrySet().stream()
        .map(entry -> buildMemberReport(entry.getKey(), entry.getValue(), cycleId))
        .collect(Collectors.toList());

    // Calculate pitch status counts
    int completedPitches = (int) pitches.stream().filter(p -> p.getStatus() == PitchStatus.DONE).count();
    int inProgressPitches = (int) pitches.stream()
        .filter(p -> p.getStatus() == PitchStatus.IN_PROGRESS || p.getStatus() == PitchStatus.STARTED).count();
    int notStartedPitches = (int) pitches.stream()
        .filter(p -> p.getStatus() == PitchStatus.PENDING || p.getStatus() == PitchStatus.SHAPED).count();

    // Calculate hours and efficiency
    double totalAppetiteHours = pitches.stream().mapToDouble(p -> p.getAppetiteDays() * HOURS_PER_DAY).sum();

    double totalActualHours = workLogs.stream().mapToDouble(wl -> wl.getHoursSpent().doubleValue()).sum();

    double varianceHours = totalActualHours - totalAppetiteHours;
    double variancePercentage = totalAppetiteHours > 0 ? (varianceHours / totalAppetiteHours) * 100 : 0;
    double efficiency = totalAppetiteHours > 0 ? (totalActualHours / totalAppetiteHours) * 100 : 0;

    // Calculate task statistics (out-of-scope work)
    int totalTasks = taskRepository.countByCycleId(cycleId);
    int completedTasks = taskRepository.countByCycleIdAndStatus(cycleId, TaskStatus.DONE);
    Double totalTaskEstimateHours = taskRepository.getTotalEstimateHoursByCycleId(cycleId);
    Double totalTaskActualHours = taskRepository.getTotalActualHoursByCycleId(cycleId);

    // Calculate risk distribution using RiskAnalysisService
    RiskDistributionDTO riskDistribution = calculateRiskDistribution(pitches);

    // Calculate member statistics
    int totalTeamMembers = memberReports.size();
    double averageHoursPerMember = totalTeamMembers > 0 ? totalActualHours / totalTeamMembers : 0;
    double maxHoursPerMember = memberReports.stream().mapToDouble(MemberWorkReportDTO::getTotalHours).max()
        .orElse(0);
    double minHoursPerMember = memberReports.stream().mapToDouble(MemberWorkReportDTO::getTotalHours).min()
        .orElse(0);

    // Identify top performers (members with above-average hours and efficiency)
    List<String> topPerformers = memberReports.stream()
        .filter(m -> m.getTotalHours() >= averageHoursPerMember && m.getAvgHoursPerDay() >= 6.0)
        .sorted(Comparator.comparing(MemberWorkReportDTO::getTotalHours).reversed()).limit(5)
        .map(MemberWorkReportDTO::getMemberName).collect(Collectors.toList());

    // Identify over-budget pitches
    List<String> overBudgetPitches = pitchReports.stream().filter(PitchReportDTO::getIsOverBudget)
        .map(PitchReportDTO::getPitchTitle).collect(Collectors.toList());

    return EnhancedCycleReportDTO.builder().cycleId(cycle.getId()).cycleName(cycle.getName())
        .projectName(cycle.getProject() != null ? cycle.getProject().getName() : null)
        .startDate(cycle.getStartDate()).endDate(cycle.getEndDate()).totalPitches(pitches.size())
        .completedPitches(completedPitches).inProgressPitches(inProgressPitches)
        .notStartedPitches(notStartedPitches).totalAppetiteHours(totalAppetiteHours)
        .totalActualHours(totalActualHours).varianceHours(varianceHours).variancePercentage(variancePercentage)
        .efficiencyPercentage(efficiency).totalTasks(totalTasks).completedTasks(completedTasks)
        .totalTaskEstimateHours(totalTaskEstimateHours != null ? totalTaskEstimateHours : 0.0)
        .totalTaskActualHours(totalTaskActualHours != null ? totalTaskActualHours : 0.0)
        .riskDistribution(riskDistribution).totalTeamMembers(totalTeamMembers)
        .averageHoursPerMember(averageHoursPerMember).maxHoursPerMember(maxHoursPerMember)
        .minHoursPerMember(minHoursPerMember).pitchReports(pitchReports).memberReports(memberReports)
        .topPerformers(topPerformers).overBudgetPitches(overBudgetPitches).build();
  }

  /** Calculate risk distribution for pitches in a cycle. */
  private RiskDistributionDTO calculateRiskDistribution(List<Pitch> pitches) {
    if (pitches.isEmpty()) {
      return RiskDistributionDTO.builder().lowRiskCount(0).mediumRiskCount(0).highRiskCount(0)
          .criticalRiskCount(0).averageRiskScore(0.0).maxRiskScore(0.0).minRiskScore(0.0).build();
    }

    // Use fast mode (rule-based) for risk analysis in reports
    List<PitchRiskDTO> riskAnalyses = pitches.stream().map(p -> riskAnalysisService.analyzePitchRisk(p, false))
        .collect(Collectors.toList());

    int lowRiskCount = (int) riskAnalyses.stream().filter(r -> r.getRiskLevel() == RiskLevel.LOW).count();
    int mediumRiskCount = (int) riskAnalyses.stream().filter(r -> r.getRiskLevel() == RiskLevel.MEDIUM).count();
    int highRiskCount = (int) riskAnalyses.stream().filter(r -> r.getRiskLevel() == RiskLevel.HIGH).count();
    int criticalRiskCount = (int) riskAnalyses.stream().filter(r -> r.getRiskLevel() == RiskLevel.CRITICAL).count();

    double averageRiskScore = riskAnalyses.stream().mapToDouble(PitchRiskDTO::getRiskScore).average().orElse(0);
    double maxRiskScore = riskAnalyses.stream().mapToDouble(PitchRiskDTO::getRiskScore).max().orElse(0);
    double minRiskScore = riskAnalyses.stream().mapToDouble(PitchRiskDTO::getRiskScore).min().orElse(0);

    return RiskDistributionDTO.builder().lowRiskCount(lowRiskCount).mediumRiskCount(mediumRiskCount)
        .highRiskCount(highRiskCount).criticalRiskCount(criticalRiskCount).averageRiskScore(averageRiskScore)
        .maxRiskScore(maxRiskScore).minRiskScore(minRiskScore).build();
  }
}
