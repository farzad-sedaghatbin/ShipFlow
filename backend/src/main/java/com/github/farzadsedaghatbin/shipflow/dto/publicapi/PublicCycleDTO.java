package com.github.farzadsedaghatbin.shipflow.dto.publicapi;

import java.time.LocalDate;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicCycleDTO {
  private Long id;
  private Long projectId;
  private String name;
  private LocalDate startDate;
  private LocalDate endDate;
  private String phase;
  private Boolean isActive;
}
