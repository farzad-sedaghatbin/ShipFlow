package com.github.farzadsedaghatbin.shipflow.dto.imports;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Full report returned from POST /api/import/zephyr — one entry per data row. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZephyrImportReportDTO {

  private Long importJobId;
  private String fileName;

  /** Import job status string (e.g. {@code "COMPLETED"}, {@code "FAILED"}). */
  private String status;

  private int totalRows;
  private int importedRows;
  private int failedRows;

  /** One result entry per data row processed (skipped blank-name rows are excluded). */
  private List<ZephyrRowResultDTO> rows;

  private LocalDateTime completedAt;
}
