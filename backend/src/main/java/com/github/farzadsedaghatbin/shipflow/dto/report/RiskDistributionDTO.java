package com.github.farzadsedaghatbin.shipflow.dto.report;

import lombok.*;

/** DTO for risk distribution statistics in reports. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskDistributionDTO {
  private Integer lowRiskCount;
  private Integer mediumRiskCount;
  private Integer highRiskCount;
  private Integer criticalRiskCount;
  private Double averageRiskScore;
  private Double maxRiskScore;
  private Double minRiskScore;
}
