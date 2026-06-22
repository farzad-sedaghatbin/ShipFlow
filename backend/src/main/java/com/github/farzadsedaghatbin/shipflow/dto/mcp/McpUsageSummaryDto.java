package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McpUsageSummaryDto {
  private long totalCalls;
  private long successCalls;
  private long failureCalls;
  private double successRate;
  private long activeUsersLast30Days;
  private long uniqueToolsUsed;
}
