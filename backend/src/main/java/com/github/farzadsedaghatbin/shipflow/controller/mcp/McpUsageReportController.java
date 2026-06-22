package com.github.farzadsedaghatbin.shipflow.controller.mcp;

import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpToolUsageDto;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpUsageLogDto;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpUsageSummaryDto;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpUsageTimelinePointDto;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpUserUsageDto;
import com.github.farzadsedaghatbin.shipflow.service.mcp.McpUsageReportService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only REST endpoints for the MCP usage report dashboard.
 * All routes require ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/mcp/usage")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class McpUsageReportController {

  private final McpUsageReportService usageReportService;

  @GetMapping("/summary")
  public ResponseEntity<McpUsageSummaryDto> summary(
      @RequestParam(defaultValue = "30") int days) {
    return ResponseEntity.ok(usageReportService.getSummary(Math.min(Math.max(days, 1), 365)));
  }

  @GetMapping("/by-user")
  public ResponseEntity<List<McpUserUsageDto>> byUser(
      @RequestParam(defaultValue = "30") int days) {
    return ResponseEntity.ok(usageReportService.getUserUsage(Math.min(Math.max(days, 1), 365)));
  }

  @GetMapping("/by-tool")
  public ResponseEntity<List<McpToolUsageDto>> byTool(
      @RequestParam(defaultValue = "30") int days) {
    return ResponseEntity.ok(usageReportService.getToolUsage(Math.min(Math.max(days, 1), 365)));
  }

  @GetMapping("/timeline")
  public ResponseEntity<List<McpUsageTimelinePointDto>> timeline(
      @RequestParam(defaultValue = "30") int days) {
    return ResponseEntity.ok(usageReportService.getTimeline(Math.min(Math.max(days, 1), 365)));
  }

  @GetMapping("/recent")
  public ResponseEntity<List<McpUsageLogDto>> recent(
      @RequestParam(defaultValue = "50") int limit) {
    return ResponseEntity.ok(usageReportService.getRecentLogs(Math.min(limit, 200)));
  }
}
