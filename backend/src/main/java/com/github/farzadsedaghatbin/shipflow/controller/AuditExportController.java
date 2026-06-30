package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.audit.AuditExportRow;
import com.github.farzadsedaghatbin.shipflow.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only export of the Hibernate Envers audit trail for compliance and
 * offline review. Supports CSV (default) and JSON, an entity-type filter, and an
 * inclusive date range.
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Export", description = "Admin-only audit-trail export (v1.9.0)")
public class AuditExportController {

  private final AuditService auditService;

  @GetMapping("/export")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Export the audit trail",
      description = "Export Envers revision history as CSV or JSON. entityType: task|bug|pitch|testcase|all.")
  public ResponseEntity<?> exportAuditTrail(
      @RequestParam(defaultValue = "all") String entityType,
      @RequestParam(defaultValue = "csv") String format,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    String stamp = LocalDate.now().toString();

    if ("json".equalsIgnoreCase(format)) {
      List<AuditExportRow> rows = auditService.exportAuditTrail(entityType, from, to);
      String filename = "audit-" + entityType + "-" + stamp + ".json";
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
          .contentType(MediaType.APPLICATION_JSON)
          .body(rows);
    }

    byte[] csv = auditService.exportAuditTrailCsv(entityType, from, to);
    String filename = "audit-" + entityType + "-" + stamp + ".csv";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(csv);
  }
}
