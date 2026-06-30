package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McpUserUsageDto {
  private String username;
  private String email;
  private long totalCalls;
  private long successCalls;
  private double successRate;
  private LocalDateTime lastCalledAt;
}
