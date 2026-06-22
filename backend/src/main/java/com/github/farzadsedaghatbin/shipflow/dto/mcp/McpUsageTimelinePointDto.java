package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McpUsageTimelinePointDto {
  private String date;
  private long totalCalls;
  private long successCalls;
}
