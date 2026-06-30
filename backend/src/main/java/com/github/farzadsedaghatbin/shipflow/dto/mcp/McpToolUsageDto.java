package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McpToolUsageDto {
  private String toolName;
  private long totalCalls;
  private long successCalls;
  private double successRate;
  private long uniqueUsers;
}
