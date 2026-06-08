package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.ImportJobDTO;
import com.github.farzadsedaghatbin.shipflow.entity.ImportJob;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.TestCase;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ImportJobStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ImportSourceFormat;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCasePriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseType;
import com.github.farzadsedaghatbin.shipflow.repository.ImportJobRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TestCaseRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for importing test cases from a Zephyr Scale XLSX export (v1.6.0 S41).
 *
 * <p>Column layout (0-based index):
 *
 * <pre>
 *  0  Key
 *  1  Name                 → title (required; skip row if blank)
 *  2  Status               → status
 *  3  Precondition         → preconditions
 *  4  Objective            → description
 *  5  Folder               (ignored)
 *  6  Priority             → priority
 *  7  Component            → appended to tags if non-blank
 *  8  Labels               → tags (comma-separated)
 *  9  Owner                (ignored)
 * 10  Estimated Time       → estimatedMinutes (integer minutes)
 * 11  Coverage (Issues)    (ignored)
 * 12  Coverage (Pages)     (ignored)
 * 13  Step                 → steps (falls back to col 16 plain text, then col 17 BDD)
 * 14  Test Data            → appended to steps with "\nTest Data:\n" prefix
 * 15  Expected Result      → expectedResult
 * 16  Plain Text script    → steps fallback
 * 17  BDD script           → steps fallback
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZephyrImportService {

  private final ImportJobRepository importJobRepository;
  private final TestCaseRepository testCaseRepository;
  private final UserRepository userRepository;
  private final PitchRepository pitchRepository;

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /**
   * Parse a Zephyr Scale XLSX file and import all valid rows as {@link TestCase} entities.
   *
   * @param file the uploaded XLSX file
   * @param pitchId optional pitch to link every imported test case to
   * @param userDetails the authenticated caller
   * @return a populated {@link ImportJobDTO} describing the outcome
   */
  @Transactional
  public ImportJobDTO importZephyr(MultipartFile file, Long pitchId, UserDetails userDetails) {

    User currentUser =
        userRepository
            .findByUsername(userDetails.getUsername())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "User not found: " + userDetails.getUsername()));

    ImportJob job =
        ImportJob.builder()
            .fileName(file.getOriginalFilename())
            .sourceFormat(ImportSourceFormat.ZEPHYR_XLSX)
            .status(ImportJobStatus.PENDING)
            .createdBy(currentUser)
            .createdAt(LocalDateTime.now())
            .build();
    job = importJobRepository.save(job);

    try {
      job.setStatus(ImportJobStatus.PARSING);
      importJobRepository.save(job);

      Pitch pitch = resolvePitch(pitchId);

      job.setStatus(ImportJobStatus.IMPORTING);
      importJobRepository.save(job);

      int imported = 0;
      int failed = 0;
      int totalDataRows = 0;
      StringBuilder errorLog = new StringBuilder();

      try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
        Sheet sheet = workbook.getSheetAt(0);

        // Row 0 is the header — data starts at row index 1
        int lastRow = sheet.getLastRowNum();
        totalDataRows = Math.max(0, lastRow); // lastRow is 0-based inclusive

        for (int rowIdx = 1; rowIdx <= lastRow; rowIdx++) {
          Row row = sheet.getRow(rowIdx);
          if (row == null) {
            // Sparse sheet — treat as skipped, not as a failure
            continue;
          }

          int humanRowNum = rowIdx + 1; // 1-based + header
          try {
            TestCase testCase = parseRow(row, pitch, currentUser);
            if (testCase == null) {
              // Name column was blank — silently skip per spec
              totalDataRows--;
              continue;
            }
            testCaseRepository.save(testCase);
            imported++;
          } catch (Exception e) {
            failed++;
            String msg = "Row " + humanRowNum + ": " + e.getMessage();
            errorLog.append(msg).append("\n");
            log.debug("Zephyr import {}", msg, e);
          }
        }
      }

      job.setTotalRows(totalDataRows);
      job.setImportedRows(imported);
      job.setFailedRows(failed);
      if (errorLog.length() > 0) {
        job.setErrorLog(errorLog.toString());
      }
      job.setStatus(imported == 0 && totalDataRows > 0 ? ImportJobStatus.FAILED : ImportJobStatus.COMPLETED);
      job.setCompletedAt(LocalDateTime.now());

    } catch (IOException e) {
      log.error("Zephyr import job {} failed reading workbook", job.getId(), e);
      job.setStatus(ImportJobStatus.FAILED);
      job.setCompletedAt(LocalDateTime.now());
      job.setErrorLog("Job-level failure: " + e.getMessage());
    } catch (Exception e) {
      log.error("Zephyr import job {} failed", job.getId(), e);
      job.setStatus(ImportJobStatus.FAILED);
      job.setCompletedAt(LocalDateTime.now());
      job.setErrorLog("Job-level failure: " + e.getMessage());
    }

    return toDTO(importJobRepository.save(job));
  }

  // -------------------------------------------------------------------------
  // Row parsing
  // -------------------------------------------------------------------------

  /**
   * Parse one sheet row and return a ready-to-save {@link TestCase}, or {@code null} if the Name
   * column is blank (the row should be skipped silently).
   */
  TestCase parseRow(Row row, Pitch pitch, User createdBy) {
    String name = cellString(row, 1);
    if (name == null || name.isBlank()) {
      return null;
    }

    String key = cellString(row, 0);
    String preconditions = cellString(row, 3);
    String description = cellString(row, 4);
    String component = cellString(row, 7);
    String labels = cellString(row, 8);
    String estimatedTimeRaw = cellString(row, 10);

    String stepCol = cellString(row, 13);
    String testData = cellString(row, 14);
    String expectedResult = cellString(row, 15);
    String plainText = cellString(row, 16);
    String bdd = cellString(row, 17);

    // Build steps: prefer step-by-step col, fall back to plain text then BDD
    String steps = buildSteps(stepCol, testData, plainText, bdd);

    // Build tags: labels + optional component
    String tags = buildTags(labels, component);

    TestCaseStatus status = mapStatus(cellString(row, 2));
    TestCasePriority priority = mapPriority(cellString(row, 6));
    Integer estimatedMinutes = parseMinutes(estimatedTimeRaw);

    // Derive a unique key: use Zephyr key if present, otherwise generate TC-style key
    String resolvedKey = (key != null && !key.isBlank()) ? key.trim() : generateKey();

    return TestCase.builder()
        .testCaseKey(resolvedKey)
        .title(name.trim())
        .description(description)
        .preconditions(preconditions)
        .steps(steps)
        .expectedResult(expectedResult)
        .status(status)
        .priority(priority)
        .type(TestCaseType.FUNCTIONAL)
        .tags(tags)
        .estimatedMinutes(estimatedMinutes)
        .pitch(pitch)
        .createdBy(createdBy)
        .aiGenerated(false)
        .build();
  }

  // -------------------------------------------------------------------------
  // Mapping helpers (package-private for unit tests)
  // -------------------------------------------------------------------------

  /** Map Zephyr status string to {@link TestCaseStatus}. */
  TestCaseStatus mapStatus(String raw) {
    if (raw == null) return TestCaseStatus.DRAFT;
    return switch (raw.trim().toLowerCase()) {
      case "approved" -> TestCaseStatus.APPROVED;
      case "draft" -> TestCaseStatus.DRAFT;
      default -> TestCaseStatus.DRAFT;
    };
  }

  /** Map Zephyr priority string to {@link TestCasePriority}. */
  TestCasePriority mapPriority(String raw) {
    if (raw == null) return TestCasePriority.MEDIUM;
    return switch (raw.trim().toLowerCase()) {
      case "high" -> TestCasePriority.HIGH;
      case "critical" -> TestCasePriority.CRITICAL;
      case "low" -> TestCasePriority.LOW;
      default -> TestCasePriority.MEDIUM;
    };
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  /**
   * Read a cell value as a trimmed String. Handles STRING, NUMERIC (formatted as integer), BOOLEAN,
   * FORMULA, and BLANK cell types. Returns {@code null} for blank/missing cells.
   */
  String cellString(Row row, int colIdx) {
    if (row == null) return null;
    Cell cell = row.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    if (cell == null) return null;

    CellType effectiveType =
        cell.getCellType() == CellType.FORMULA
            ? cell.getCachedFormulaResultType()
            : cell.getCellType();

    String value =
        switch (effectiveType) {
          case STRING -> cell.getStringCellValue();
          case NUMERIC -> {
            double d = cell.getNumericCellValue();
            // Render whole numbers without ".0"
            yield d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
          }
          case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
          default -> null;
        };

    return (value == null || value.isBlank()) ? null : value.trim();
  }

  /**
   * Compose the steps field. Prefers the step-by-step column; appends Test Data if present. Falls
   * back to plain-text then BDD when the step column is blank.
   */
  private String buildSteps(String stepCol, String testData, String plainText, String bdd) {
    String base = stepCol;
    if (base == null || base.isBlank()) {
      base = (plainText != null && !plainText.isBlank()) ? plainText : bdd;
    }
    if (base == null) base = "";

    if (testData != null && !testData.isBlank()) {
      base = base + "\nTest Data:\n" + testData;
    }
    return base.isBlank() ? null : base;
  }

  /** Merge comma-separated labels with the component value. */
  private String buildTags(String labels, String component) {
    List<String> parts = new ArrayList<>();
    if (labels != null && !labels.isBlank()) {
      parts.add(labels.trim());
    }
    if (component != null && !component.isBlank()) {
      parts.add(component.trim());
    }
    return parts.isEmpty() ? null : String.join(",", parts);
  }

  /**
   * Parse the Zephyr "Estimated Time" cell as an integer number of minutes. Accepts plain integers
   * ("30"), decimals ("30.0"), or strings suffixed with "m"/"min"/"minutes" ("30m"). Returns
   * {@code null} if the value is absent or cannot be parsed.
   */
  private Integer parseMinutes(String raw) {
    if (raw == null || raw.isBlank()) return null;
    String cleaned = raw.trim().replaceAll("(?i)\\s*(minutes?|mins?|m)$", "").trim();
    try {
      return (int) Double.parseDouble(cleaned);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** Generate a unique TC-NNN style key for rows that have no Zephyr key. */
  private String generateKey() {
    Integer max = testCaseRepository.findMaxTestCaseKeyNumber();
    int next = (max == null ? 0 : max) + 1;
    String candidate;
    do {
      candidate = "TC-" + next;
      next++;
    } while (testCaseRepository.existsByTestCaseKey(candidate));
    return candidate;
  }

  /** Load Pitch by id (optional). Returns {@code null} when {@code pitchId} is null. */
  private Pitch resolvePitch(Long pitchId) {
    if (pitchId == null) return null;
    return pitchRepository
        .findById(pitchId)
        .orElseThrow(
            () -> new EntityNotFoundException("Pitch not found: " + pitchId));
  }

  // -------------------------------------------------------------------------
  // DTO conversion
  // -------------------------------------------------------------------------

  private ImportJobDTO toDTO(ImportJob job) {
    return ImportJobDTO.builder()
        .id(job.getId())
        .fileName(job.getFileName())
        .sourceFormat(job.getSourceFormat() != null ? job.getSourceFormat().name() : null)
        .status(job.getStatus() != null ? job.getStatus().name() : null)
        .totalRows(job.getTotalRows())
        .importedRows(job.getImportedRows())
        .failedRows(job.getFailedRows())
        .errorLog(job.getErrorLog())
        .projectId(job.getProject() != null ? job.getProject().getId() : null)
        .projectName(job.getProject() != null ? job.getProject().getName() : null)
        .createdAt(job.getCreatedAt())
        .completedAt(job.getCompletedAt())
        .build();
  }
}
