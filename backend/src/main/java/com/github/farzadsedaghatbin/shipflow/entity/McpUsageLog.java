package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "mcp_usage_log",
    indexes = {
      @Index(name = "idx_mcp_usage_log_user_id", columnList = "user_id"),
      @Index(name = "idx_mcp_usage_log_tool_name", columnList = "tool_name"),
      @Index(name = "idx_mcp_usage_log_called_at", columnList = "called_at"),
      @Index(name = "idx_mcp_usage_log_api_key_id", columnList = "api_key_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McpUsageLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "api_key_id")
  private ApiKey apiKey;

  @Column(name = "tool_name", nullable = false, length = 120)
  private String toolName;

  @Column(nullable = false)
  @Builder.Default
  private Boolean success = true;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "duration_ms")
  private Long durationMs;

  @Column(name = "called_at", nullable = false)
  private LocalDateTime calledAt;
}
