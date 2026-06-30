package com.github.farzadsedaghatbin.shipflow.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for PATCH /api/import/{importJobId}/link-test-cases. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkImportedTestCasesRequest {

  /** ID of the pitch to link all imported test cases to; may be {@code null}. */
  private Long pitchId;

  /** ID of the task to link all imported test cases to; may be {@code null}. */
  private Long taskId;
}
