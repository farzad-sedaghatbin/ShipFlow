package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.report.CycleReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.report.EnhancedCycleReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.report.MemberWorkReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.report.PitchReportDTO;
import com.github.farzadsedaghatbin.shipflow.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Analytics and reporting")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/cycle/{cycleId}")
    @Operation(summary = "Get full cycle report")
    public ResponseEntity<CycleReportDTO> getCycleReport(@PathVariable Long cycleId) {
        return ResponseEntity.ok(reportService.getCycleReport(cycleId));
    }

    @GetMapping("/cycle/{cycleId}/enhanced")
    @Operation(summary = "Get enhanced cycle report with comprehensive metrics including risk distribution")
    public ResponseEntity<EnhancedCycleReportDTO> getEnhancedCycleReport(@PathVariable Long cycleId) {
        return ResponseEntity.ok(reportService.getEnhancedCycleReport(cycleId));
    }

    @GetMapping("/cycle/{cycleId}/pitches")
    @Operation(summary = "Get pitch reports for a cycle")
    public ResponseEntity<List<PitchReportDTO>> getPitchReports(@PathVariable Long cycleId) {
        return ResponseEntity.ok(reportService.getPitchReports(cycleId));
    }

    @GetMapping("/cycle/{cycleId}/members")
    @Operation(summary = "Get member work reports for a cycle")
    public ResponseEntity<List<MemberWorkReportDTO>> getMemberReports(@PathVariable Long cycleId) {
        return ResponseEntity.ok(reportService.getMemberReports(cycleId));
    }

    @GetMapping("/cycle/{cycleId}/export")
    @Operation(summary = "Export cycle report as CSV")
    public ResponseEntity<String> exportCycleReportCsv(@PathVariable Long cycleId) {
        String csv = reportService.exportCycleReportCsv(cycleId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cycle_report_" + cycleId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/cycle/{cycleId}/export/pdf")
    @Operation(summary = "Export cycle report as PDF")
    public ResponseEntity<byte[]> exportCycleReportPdf(@PathVariable Long cycleId) {
        byte[] pdf = reportService.exportCycleReportPdf(cycleId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cycle_report_" + cycleId + ".pdf\"")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_PDF))
                .body(pdf);
    }
}
