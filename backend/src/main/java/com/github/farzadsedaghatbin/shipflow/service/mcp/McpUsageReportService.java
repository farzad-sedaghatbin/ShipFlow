package com.github.farzadsedaghatbin.shipflow.service.mcp;

import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpToolUsageDto;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpUsageLogDto;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpUsageSummaryDto;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpUsageTimelinePointDto;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpUserUsageDto;
import com.github.farzadsedaghatbin.shipflow.entity.ApiKey;
import com.github.farzadsedaghatbin.shipflow.entity.McpUsageLog;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.McpUsageLogRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class McpUsageReportService {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final McpUsageLogRepository usageLogRepository;
  private final UserRepository userRepository;

  /**
   * Records a single MCP tool invocation. Runs asynchronously so it never adds latency to the tool
   * response path. Failures are caught and logged — a metrics write must never break the tool call.
   */
  @Async
  public void record(String username, ApiKey apiKey, String toolName, boolean success,
      String errorMessage, long durationMs) {
    try {
      User user = userRepository.findByUsername(username).orElse(null);
      if (user == null) {
        log.warn("McpUsageReportService.record: unknown user '{}'", username);
        return;
      }
      McpUsageLog entry = McpUsageLog.builder()
          .user(user)
          .apiKey(apiKey)
          .toolName(toolName)
          .success(success)
          .errorMessage(errorMessage)
          .durationMs(durationMs)
          .calledAt(LocalDateTime.now())
          .build();
      usageLogRepository.save(entry);
    } catch (Exception ex) {
      log.warn("McpUsageReportService: failed to record tool call — {}", ex.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public McpUsageSummaryDto getSummary(int days) {
    LocalDateTime since = LocalDateTime.now().minusDays(days);
    long total = usageLogRepository.countByCalledAtAfter(since);
    long success = usageLogRepository.countBySuccessTrueAndCalledAtAfter(since);
    long failures = total - success;
    double rate = total == 0 ? 0.0 : Math.round((success * 100.0 / total) * 10) / 10.0;
    long activeUsers = usageLogRepository.countDistinctUsersSince(since);
    long uniqueTools = usageLogRepository.countDistinctToolsSince(since);

    return McpUsageSummaryDto.builder()
        .totalCalls(total)
        .successCalls(success)
        .failureCalls(failures)
        .successRate(rate)
        .activeUsersLast30Days(activeUsers)
        .uniqueToolsUsed(uniqueTools)
        .build();
  }

  @Transactional(readOnly = true)
  public List<McpUserUsageDto> getUserUsage(int days) {
    LocalDateTime since = LocalDateTime.now().minusDays(days);
    return usageLogRepository.findUserUsageAggregatesSince(since).stream()
        .map(row -> {
          long total = ((Number) row[2]).longValue();
          long successCalls = ((Number) row[3]).longValue();
          return McpUserUsageDto.builder()
              .username((String) row[0])
              .email((String) row[1])
              .totalCalls(total)
              .successCalls(successCalls)
              .successRate(total == 0 ? 0.0 : Math.round((successCalls * 100.0 / total) * 10) / 10.0)
              .lastCalledAt(row[4] instanceof LocalDateTime ldt ? ldt : null)
              .build();
        })
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<McpToolUsageDto> getToolUsage(int days) {
    LocalDateTime since = LocalDateTime.now().minusDays(days);
    return usageLogRepository.findToolUsageAggregatesSince(since).stream()
        .map(row -> {
          long total = ((Number) row[1]).longValue();
          long successCalls = ((Number) row[2]).longValue();
          long uniqueUsers = ((Number) row[3]).longValue();
          return McpToolUsageDto.builder()
              .toolName((String) row[0])
              .totalCalls(total)
              .successCalls(successCalls)
              .successRate(total == 0 ? 0.0 : Math.round((successCalls * 100.0 / total) * 10) / 10.0)
              .uniqueUsers(uniqueUsers)
              .build();
        })
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<McpUsageTimelinePointDto> getTimeline(int days) {
    LocalDateTime since = LocalDateTime.now().minusDays(days);
    return usageLogRepository.findDailyCallsSince(since).stream()
        .map(row -> {
          String date = row[0] != null ? row[0].toString().substring(0, 10) : "";
          long total = ((Number) row[1]).longValue();
          long successCalls = ((Number) row[2]).longValue();
          return McpUsageTimelinePointDto.builder()
              .date(date)
              .totalCalls(total)
              .successCalls(successCalls)
              .build();
        })
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<McpUsageLogDto> getRecentLogs(int limit) {
    return usageLogRepository
        .findAllByOrderByCalledAtDesc(PageRequest.of(0, limit))
        .stream()
        .map(log -> McpUsageLogDto.builder()
            .id(log.getId())
            .username(log.getUser().getUsername())
            .toolName(log.getToolName())
            .success(log.getSuccess())
            .errorMessage(log.getErrorMessage())
            .durationMs(log.getDurationMs())
            .calledAt(log.getCalledAt())
            .apiKeyPrefix(log.getApiKey() != null ? log.getApiKey().getKeyPrefix() : null)
            .build())
        .collect(Collectors.toList());
  }
}
