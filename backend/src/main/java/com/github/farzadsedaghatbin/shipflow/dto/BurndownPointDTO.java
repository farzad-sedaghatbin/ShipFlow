package com.github.farzadsedaghatbin.shipflow.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A single data point on a sprint burndown chart. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BurndownPointDTO {
  private LocalDate date;
  private Integer remainingPoints;
  private Integer idealPoints;
}
