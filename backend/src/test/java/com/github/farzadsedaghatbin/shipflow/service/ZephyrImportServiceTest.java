package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.imports.LinkImportedTestCasesRequest;
import com.github.farzadsedaghatbin.shipflow.dto.imports.ZephyrImportReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.imports.ZephyrRowResultDTO;
import com.github.farzadsedaghatbin.shipflow.entity.ImportJob;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.TestCase;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ImportJobStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCasePriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseStatus;
import com.github.farzadsedaghatbin.shipflow.repository.ImportJobRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TestCaseRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Pure unit tests for ZephyrImportService — no Spring context.
 * Covers mapping helpers, parseRow, importZephyr workflow, and linkImportedTestCases.
 */
@ExtendWith(MockitoExtension.class)
class ZephyrImportServiceTest {

  @Mock private ImportJobRepository importJobRepository;
  @Mock private TestCaseRepository testCaseRepository;
  @Mock private UserRepository userRepository;
  @Mock private PitchRepository pitchRepository;
  @Mock private TaskRepository taskRepository;

  @Mock private UserDetails userDetails;

  private ZephyrImportService service;

  private static final String TEST_USERNAME = "tester";

  @BeforeEach
  void setUp() {
    service =
        new ZephyrImportService(
            importJobRepository,
            testCaseRepository,
            userRepository,
            pitchRepository,
            taskRepository);
  }

  // ---- mapStatus -----------------------------------------------------------

  @Test
  @DisplayName("mapStatus: 'Approved' maps to APPROVED")
  void mapStatus_approved() {
    assertThat(service.mapStatus("Approved")).isEqualTo(TestCaseStatus.APPROVED);
  }

  @Test
  @DisplayName("mapStatus: 'approved' (lowercase) maps to APPROVED")
  void mapStatus_approvedLowercase() {
    assertThat(service.mapStatus("approved")).isEqualTo(TestCaseStatus.APPROVED);
  }

  @Test
  @DisplayName("mapStatus: 'Draft' maps to DRAFT")
  void mapStatus_draft() {
    assertThat(service.mapStatus("Draft")).isEqualTo(TestCaseStatus.DRAFT);
  }

  @Test
  @DisplayName("mapStatus: unknown value defaults to DRAFT")
  void mapStatus_unknown() {
    assertThat(service.mapStatus("In Review")).isEqualTo(TestCaseStatus.DRAFT);
  }

  @Test
  @DisplayName("mapStatus: null defaults to DRAFT")
  void mapStatus_null() {
    assertThat(service.mapStatus(null)).isEqualTo(TestCaseStatus.DRAFT);
  }

  // ---- mapPriority ---------------------------------------------------------

  @Test
  @DisplayName("mapPriority: 'High' maps to HIGH")
  void mapPriority_high() {
    assertThat(service.mapPriority("High")).isEqualTo(TestCasePriority.HIGH);
  }

  @Test
  @DisplayName("mapPriority: 'Critical' maps to CRITICAL")
  void mapPriority_critical() {
    assertThat(service.mapPriority("Critical")).isEqualTo(TestCasePriority.CRITICAL);
  }

  @Test
  @DisplayName("mapPriority: 'Low' maps to LOW")
  void mapPriority_low() {
    assertThat(service.mapPriority("Low")).isEqualTo(TestCasePriority.LOW);
  }

  @Test
  @DisplayName("mapPriority: null defaults to MEDIUM")
  void mapPriority_null() {
    assertThat(service.mapPriority(null)).isEqualTo(TestCasePriority.MEDIUM);
  }

  @Test
  @DisplayName("mapPriority: unknown value defaults to MEDIUM")
  void mapPriority_unknown() {
    assertThat(service.mapPriority("Normal")).isEqualTo(TestCasePriority.MEDIUM);
  }

  // ---- cellString / parseRow -----------------------------------------------

  @Test
  @DisplayName("parseRow: returns null when Name column is blank")
  void parseRow_blankName_returnsNull() {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet();
      Row row = sheet.createRow(1);
      row.createCell(0).setCellValue("TC-001");
      // col 1 (Name) intentionally left empty
      assertThat(service.parseRow(row, null, new User())).isNull();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @DisplayName("parseRow: uses Zephyr key when present")
  void parseRow_usesZephyrKey() {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet();
      Row row = sheet.createRow(1);
      row.createCell(0).setCellValue("ZEP-42");
      row.createCell(1).setCellValue("Login smoke test");

      TestCase tc = service.parseRow(row, null, new User());

      assertThat(tc).isNotNull();
      assertThat(tc.getTestCaseKey()).isEqualTo("ZEP-42");
      assertThat(tc.getTitle()).isEqualTo("Login smoke test");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @DisplayName("parseRow: generates TC-N key when Zephyr key column is empty")
  void parseRow_generatesKeyWhenMissing() {
    when(testCaseRepository.findMaxTestCaseKeyNumber()).thenReturn(5);
    when(testCaseRepository.existsByTestCaseKey("TC-6")).thenReturn(false);

    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet();
      Row row = sheet.createRow(1);
      // col 0 empty — no Zephyr key
      row.createCell(1).setCellValue("API contract test");

      TestCase tc = service.parseRow(row, null, new User());

      assertThat(tc).isNotNull();
      assertThat(tc.getTestCaseKey()).isEqualTo("TC-6");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @DisplayName("parseRow: maps tags from labels + component columns")
  void parseRow_tagsMerged() {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet();
      Row row = sheet.createRow(1);
      row.createCell(0).setCellValue("ZEP-1");
      row.createCell(1).setCellValue("Test");
      row.createCell(7).setCellValue("Auth");        // component
      row.createCell(8).setCellValue("regression");  // labels

      TestCase tc = service.parseRow(row, null, new User());

      assertThat(tc).isNotNull();
      assertThat(tc.getTags()).contains("regression");
      assertThat(tc.getTags()).contains("Auth");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // ---- importZephyr --------------------------------------------------------

  /** Build a minimal in-memory XLSX with a header row and one data row. */
  private MockMultipartFile buildXlsx(String key, String name) throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet();
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("Key");
      header.createCell(1).setCellValue("Name");

      Row data = sheet.createRow(1);
      data.createCell(0).setCellValue(key);
      data.createCell(1).setCellValue(name);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      wb.write(out);
      return new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          new ByteArrayInputStream(out.toByteArray()));
    }
  }

  @Test
  @DisplayName("importZephyr: successful single-row import returns report with one success entry")
  void importZephyr_singleRow_success() throws IOException {
    User user = new User();
    user.setUsername(TEST_USERNAME);

    ImportJob savedJob = ImportJob.builder()
        .id(1L)
        .fileName("test.xlsx")
        .status(ImportJobStatus.COMPLETED)
        .totalRows(1)
        .importedRows(1)
        .failedRows(0)
        .createdAt(LocalDateTime.now())
        .completedAt(LocalDateTime.now())
        .build();

    TestCase savedTc = TestCase.builder()
        .id(10L)
        .testCaseKey("ZEP-1")
        .title("Login smoke test")
        .build();

    when(userDetails.getUsername()).thenReturn(TEST_USERNAME);
    when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
    when(importJobRepository.save(org.mockito.ArgumentMatchers.any(ImportJob.class)))
        .thenReturn(savedJob);
    when(testCaseRepository.save(org.mockito.ArgumentMatchers.any(TestCase.class)))
        .thenReturn(savedTc);

    MockMultipartFile file = buildXlsx("ZEP-1", "Login smoke test");
    ZephyrImportReportDTO report = service.importZephyr(file, null, userDetails);

    assertThat(report).isNotNull();
    assertThat(report.getRows()).hasSize(1);

    ZephyrRowResultDTO row = report.getRows().get(0);
    assertThat(row.isSuccess()).isTrue();
    assertThat(row.getZephyrKey()).isEqualTo("ZEP-1");
    assertThat(row.getTitle()).isEqualTo("Login smoke test");
    assertThat(row.getTestCaseId()).isEqualTo(10L);
    assertThat(row.getError()).isNull();
  }

  @Test
  @DisplayName("importZephyr: row with blank Name is silently skipped (not a failure)")
  void importZephyr_blankNameRow_skipped() throws IOException {
    User user = new User();
    user.setUsername(TEST_USERNAME);

    ImportJob savedJob = ImportJob.builder()
        .id(2L)
        .fileName("test.xlsx")
        .status(ImportJobStatus.COMPLETED)
        .totalRows(0)
        .importedRows(0)
        .failedRows(0)
        .createdAt(LocalDateTime.now())
        .completedAt(LocalDateTime.now())
        .build();

    when(userDetails.getUsername()).thenReturn(TEST_USERNAME);
    when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
    when(importJobRepository.save(org.mockito.ArgumentMatchers.any(ImportJob.class)))
        .thenReturn(savedJob);

    // Row with empty name
    MockMultipartFile file = buildXlsx("ZEP-2", "");
    ZephyrImportReportDTO report = service.importZephyr(file, null, userDetails);

    assertThat(report).isNotNull();
    // Blank-name rows must not produce a row result entry
    assertThat(report.getRows()).isEmpty();
  }

  // ---- linkImportedTestCases -----------------------------------------------

  @Test
  @DisplayName("linkImportedTestCases: links test cases to pitch and returns count")
  void linkImportedTestCases_linksToPitch() {
    ImportJob job = ImportJob.builder().id(5L).build();
    Pitch pitch = new Pitch();
    pitch.setId(20L);

    TestCase tc1 = TestCase.builder().id(1L).testCaseKey("TC-1").title("Test A").build();
    TestCase tc2 = TestCase.builder().id(2L).testCaseKey("TC-2").title("Test B").build();

    when(importJobRepository.findById(5L)).thenReturn(Optional.of(job));
    when(testCaseRepository.findByImportJobId(5L)).thenReturn(List.of(tc1, tc2));
    when(pitchRepository.findById(20L)).thenReturn(Optional.of(pitch));
    when(testCaseRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
        .thenReturn(List.of(tc1, tc2));

    LinkImportedTestCasesRequest request = new LinkImportedTestCasesRequest(20L, null);
    Map<String, Object> result =
        service.linkImportedTestCases(5L, request, userDetails);

    assertThat(result.get("linked")).isEqualTo(2);
    assertThat(tc1.getPitch()).isEqualTo(pitch);
    assertThat(tc2.getPitch()).isEqualTo(pitch);
  }

  @Test
  @DisplayName("linkImportedTestCases: throws EntityNotFoundException for unknown import job")
  void linkImportedTestCases_unknownJob() {
    when(importJobRepository.findById(99L)).thenReturn(Optional.empty());

    LinkImportedTestCasesRequest request = new LinkImportedTestCasesRequest(null, null);
    org.junit.jupiter.api.Assertions.assertThrows(
        EntityNotFoundException.class,
        () -> service.linkImportedTestCases(99L, request, userDetails));
  }

  @Test
  @DisplayName("linkImportedTestCases: throws EntityNotFoundException for unknown pitch")
  void linkImportedTestCases_unknownPitch() {
    ImportJob job = ImportJob.builder().id(5L).build();
    when(importJobRepository.findById(5L)).thenReturn(Optional.of(job));
    when(testCaseRepository.findByImportJobId(5L)).thenReturn(List.of());
    when(pitchRepository.findById(99L)).thenReturn(Optional.empty());

    LinkImportedTestCasesRequest request = new LinkImportedTestCasesRequest(99L, null);
    org.junit.jupiter.api.Assertions.assertThrows(
        EntityNotFoundException.class,
        () -> service.linkImportedTestCases(5L, request, userDetails));
  }
}
