package com.github.farzadsedaghatbin.shipflow.dto;

import java.time.LocalDateTime;
import lombok.*;

/** Response DTO for an import job — safe to expose at the controller boundary. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJobDTO {

  private Long id;
  private String fileName;
  private String sourceFormat;
  private String status;
  private int totalRows;
  private int importedRows;
  private int failedRows;
  private String errorLog;
  private Long projectId;
  private String projectName;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;
}
