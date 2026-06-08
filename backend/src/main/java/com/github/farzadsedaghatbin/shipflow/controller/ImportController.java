package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.ImportJobDTO;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.CsvImportService;
import com.github.farzadsedaghatbin.shipflow.service.ZephyrImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for competitor migration import (v1.2.0+).
 * Accepts Jira, Linear, Asana, and generic CSV exports, and Zephyr Scale XLSX test-case exports.
 */
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Import", description = "Competitor migration import APIs")
public class ImportController {

  private final CsvImportService csvImportService;
  private final ZephyrImportService zephyrImportService;
  private final UserRepository userRepository;

  /**
   * Upload and process a CSV file, creating a new Kanban project with all imported tasks.
   * Allowed for ADMIN and MANAGER roles (project creation is a privileged action).
   */
  @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Import tasks from a CSV file (Jira, Linear, Asana, or generic)")
  public ResponseEntity<ImportJobDTO> importCsv(
      @RequestParam("file") MultipartFile file,
      @RequestParam("projectName") String projectName,
      @RequestParam(value = "format", defaultValue = "auto") String format,
      @AuthenticationPrincipal UserDetails userDetails) {

    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must not be empty");
    }
    String originalName = file.getOriginalFilename();
    if (originalName == null || !originalName.toLowerCase().endsWith(".csv")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .csv files are accepted");
    }
    if (projectName == null || projectName.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "projectName must not be blank");
    }

    User currentUser = resolveUser(userDetails);
    log.info(
        "CSV import started by user={} file={} format={}", currentUser.getUsername(), originalName, format);

    ImportJobDTO result = csvImportService.importCsv(file, projectName.trim(), format, currentUser);
    return ResponseEntity.ok(result);
  }

  /** Get the status and result of an import job by id. */
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Get an import job by id")
  public ResponseEntity<ImportJobDTO> getJob(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {

    ImportJobDTO job;
    try {
      job = csvImportService.getJob(id);
    } catch (EntityNotFoundException e) {
      return ResponseEntity.notFound().build();
    }

    User currentUser = resolveUser(userDetails);
    // Non-ADMIN users can only view their own jobs
    boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
    if (!isAdmin && job.getProjectId() != null) {
      // Verify ownership by checking that this job appears in the user's list
      boolean owned =
          csvImportService.listJobs(currentUser).stream().anyMatch(j -> j.getId().equals(id));
      if (!owned) {
        throw new AccessDeniedException("Access denied to import job " + id);
      }
    }

    return ResponseEntity.ok(job);
  }

  /** List all import jobs belonging to the current user. */
  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "List all import jobs for the current user")
  public ResponseEntity<List<ImportJobDTO>> listJobs(
      @AuthenticationPrincipal UserDetails userDetails) {
    User currentUser = resolveUser(userDetails);
    return ResponseEntity.ok(csvImportService.listJobs(currentUser));
  }

  /**
   * Upload and process a Zephyr Scale XLSX export, importing all rows as TestCase entities.
   * Allowed for ADMIN, MANAGER, and QA roles.
   */
  @PostMapping(value = "/zephyr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'QA')")
  @Operation(summary = "Import test cases from a Zephyr Scale XLSX export")
  public ResponseEntity<ImportJobDTO> importZephyr(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "pitchId", required = false) Long pitchId,
      @AuthenticationPrincipal UserDetails userDetails) {

    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must not be empty");
    }
    String originalName = file.getOriginalFilename();
    if (originalName == null
        || (!originalName.toLowerCase().endsWith(".xlsx")
            && !originalName.toLowerCase().endsWith(".xls"))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Only .xlsx or .xls files are accepted");
    }

    log.info(
        "Zephyr XLSX import started by user={} file={} pitchId={}",
        userDetails.getUsername(),
        originalName,
        pitchId);

    ImportJobDTO result = zephyrImportService.importZephyr(file, pitchId, userDetails);
    return ResponseEntity.ok(result);
  }

  // -------------------------------------------------------------------------

  private User resolveUser(UserDetails userDetails) {
    return userRepository
        .findByUsername(userDetails.getUsername())
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User not found: " + userDetails.getUsername()));
  }
}
