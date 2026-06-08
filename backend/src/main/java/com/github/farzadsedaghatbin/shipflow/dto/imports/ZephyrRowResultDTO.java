package com.github.farzadsedaghatbin.shipflow.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Per-row outcome entry included in a {@link ZephyrImportReportDTO}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZephyrRowResultDTO {

  /** 1-based row number in the source XLSX (matching human-readable row numbers). */
  private int rowNumber;

  /** The Zephyr key from column 0, or {@code null} if the cell was empty. */
  private String zephyrKey;

  /** The test case title from column 1. */
  private String title;

  /** {@code true} when the row was saved successfully. */
  private boolean success;

  /** ID of the saved {@link com.github.farzadsedaghatbin.shipflow.entity.TestCase}; {@code null} on failure. */
  private Long testCaseId;

  /** Human-readable error message; {@code null} on success. */
  private String error;
}
