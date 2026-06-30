package com.github.farzadsedaghatbin.shipflow.dto.qa;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import lombok.*;

/** Compact representation of a bug report (defect) linked to a test run. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedBugReportDTO {
  private Long id;
  private String bugKey;
  private String title;
  private BugStatus status;
}
