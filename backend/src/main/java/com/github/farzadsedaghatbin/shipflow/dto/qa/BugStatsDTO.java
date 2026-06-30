package com.github.farzadsedaghatbin.shipflow.dto.qa;

import lombok.*;

/** Aggregate counts for the bug reports overview stat cards. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BugStatsDTO {

  private long total;
  private long open;
  private long inProgress;
  private long resolved;
  private long critical;
}
