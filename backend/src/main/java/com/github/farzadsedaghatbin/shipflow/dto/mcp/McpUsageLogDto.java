package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McpUsageLogDto {
  private Long id;
  private String username;
  private String toolName;
  private Boolean success;
  private String errorMessage;
  private Long durationMs;
  private LocalDateTime calledAt;
  private String apiKeyPrefix;
}
