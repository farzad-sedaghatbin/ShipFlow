package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.service.DataExportService;
import com.github.farzadsedaghatbin.shipflow.service.DataImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/public/data")
@RequiredArgsConstructor
@Tag(name = "Public API – Data Export/Import",
    description = "Export and import project data as JSON or CSV")
public class DataManagementController {

  private final DataExportService dataExportService;
  private final DataImportService dataImportService;

  // ──────────────────────────── Export ────────────────────────────

  @GetMapping("/export/{projectId}/json")
  @Operation(summary = "Export a full project snapshot as JSON")
  public ResponseEntity<byte[]> exportProjectJson(@PathVariable Long projectId) {
    byte[] json = dataExportService.exportProjectAsJson(projectId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shipflow-export-" + projectId + ".json")
        .contentType(MediaType.APPLICATION_JSON)
        .body(json);
  }

  @GetMapping("/export/{projectId}/tasks.csv")
  @Operation(summary = "Export project tasks as CSV")
  public ResponseEntity<byte[]> exportTasksCsv(@PathVariable Long projectId) {
    byte[] csv = dataExportService.exportTasksAsCsv(projectId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shipflow-tasks-" + projectId + ".csv")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv);
  }

  @GetMapping("/export/{projectId}/bugs.csv")
  @Operation(summary = "Export project bugs as CSV")
  public ResponseEntity<byte[]> exportBugsCsv(@PathVariable Long projectId) {
    byte[] csv = dataExportService.exportBugsAsCsv(projectId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shipflow-bugs-" + projectId + ".csv")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv);
  }

  // ──────────────────────────── Import ────────────────────────────

  @PostMapping("/import/json")
  @Operation(summary = "Import a project snapshot from JSON")
  public ResponseEntity<DataImportService.ImportSummary> importProjectJson(
      @RequestParam("file") MultipartFile file) throws IOException {
    DataImportService.ImportSummary summary = dataImportService.importProjectFromJson(file.getInputStream());
    return ResponseEntity.ok(summary);
  }

  @PostMapping("/import/tasks.csv")
  @Operation(summary = "Import tasks from CSV")
  public ResponseEntity<DataImportService.ImportSummary> importTasksCsv(
      @RequestParam("file") MultipartFile file) throws IOException {
    DataImportService.ImportSummary summary = dataImportService.importTasksFromCsv(file.getInputStream());
    return ResponseEntity.ok(summary);
  }
}
