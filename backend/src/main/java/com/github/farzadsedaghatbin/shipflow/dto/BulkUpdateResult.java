package com.github.farzadsedaghatbin.shipflow.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/** Response returned by POST /api/tasks/bulk-update. */
@Data
@Builder
public class BulkUpdateResult {
  private int successCount;
  private int failureCount;
  private List<String> errors;
}
