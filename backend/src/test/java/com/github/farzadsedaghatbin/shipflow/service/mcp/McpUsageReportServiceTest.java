package com.github.farzadsedaghatbin.shipflow.service.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class McpUsageReportServiceTest {

  @Mock private McpUsageLogRepository usageLogRepository;
  @Mock private UserRepository userRepository;

  private McpUsageReportService service;

  @BeforeEach
  void setUp() {
    service = new McpUsageReportService(usageLogRepository, userRepository);
  }

  @Test
  void getSummary_returnsAggregatedCounts() {
    when(usageLogRepository.countByCalledAtAfter(any())).thenReturn(100L);
    when(usageLogRepository.countBySuccessTrueAndCalledAtAfter(any())).thenReturn(90L);
    when(usageLogRepository.countDistinctUsersSince(any())).thenReturn(5L);
    when(usageLogRepository.countDistinctToolsSince(any())).thenReturn(12L);

    McpUsageSummaryDto summary = service.getSummary(30);

    assertThat(summary.getTotalCalls()).isEqualTo(100L);
    assertThat(summary.getSuccessCalls()).isEqualTo(90L);
    assertThat(summary.getFailureCalls()).isEqualTo(10L);
    assertThat(summary.getSuccessRate()).isEqualTo(90.0);
    assertThat(summary.getActiveUsersLast30Days()).isEqualTo(5L);
    assertThat(summary.getUniqueToolsUsed()).isEqualTo(12L);
  }

  @Test
  void getSummary_zeroTotalCallsDoesNotDivideByZero() {
    when(usageLogRepository.countByCalledAtAfter(any())).thenReturn(0L);
    when(usageLogRepository.countBySuccessTrueAndCalledAtAfter(any())).thenReturn(0L);
    when(usageLogRepository.countDistinctUsersSince(any())).thenReturn(0L);
    when(usageLogRepository.countDistinctToolsSince(any())).thenReturn(0L);

    McpUsageSummaryDto summary = service.getSummary(30);
    assertThat(summary.getSuccessRate()).isEqualTo(0.0);
  }

  @Test
  void getUserUsage_mapsAggregateRowsToDto() {
    LocalDateTime lastSeen = LocalDateTime.of(2026, 6, 20, 10, 0);
    Object[] row = {"alice", "alice@example.com", 50L, 48L, lastSeen};
    List<Object[]> rows = new java.util.ArrayList<>();
    rows.add(row);
    when(usageLogRepository.findUserUsageAggregatesSince(any())).thenReturn(rows);

    List<McpUserUsageDto> result = service.getUserUsage(30);

    assertThat(result).hasSize(1);
    McpUserUsageDto dto = result.get(0);
    assertThat(dto.getUsername()).isEqualTo("alice");
    assertThat(dto.getTotalCalls()).isEqualTo(50L);
    assertThat(dto.getSuccessCalls()).isEqualTo(48L);
    assertThat(dto.getSuccessRate()).isEqualTo(96.0);
    assertThat(dto.getLastCalledAt()).isEqualTo(lastSeen);
  }

  @Test
  void getToolUsage_mapsAggregateRowsToDto() {
    Object[] row = {"list_projects", 30L, 30L, 3L};
    List<Object[]> rows = new java.util.ArrayList<>();
    rows.add(row);
    when(usageLogRepository.findToolUsageAggregatesSince(any())).thenReturn(rows);

    List<McpToolUsageDto> result = service.getToolUsage(30);

    assertThat(result).hasSize(1);
    McpToolUsageDto dto = result.get(0);
    assertThat(dto.getToolName()).isEqualTo("list_projects");
    assertThat(dto.getTotalCalls()).isEqualTo(30L);
    assertThat(dto.getSuccessRate()).isEqualTo(100.0);
    assertThat(dto.getUniqueUsers()).isEqualTo(3L);
  }

  @Test
  void getTimeline_mapsRowsToDto() {
    Object[] row = {"2026-06-20 00:00:00", 15L, 14L};
    List<Object[]> rows = new java.util.ArrayList<>();
    rows.add(row);
    when(usageLogRepository.findDailyCallsSince(any())).thenReturn(rows);

    List<McpUsageTimelinePointDto> result = service.getTimeline(30);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTotalCalls()).isEqualTo(15L);
    assertThat(result.get(0).getSuccessCalls()).isEqualTo(14L);
  }

  @Test
  void getRecentLogs_mapsEntitiesToDto() {
    User user = new User();
    user.setUsername("bob");

    ApiKey key = ApiKey.builder().keyPrefix("sf_live_").build();

    McpUsageLog log = McpUsageLog.builder()
        .id(1L)
        .user(user)
        .apiKey(key)
        .toolName("get_tasks")
        .success(true)
        .durationMs(42L)
        .calledAt(LocalDateTime.of(2026, 6, 22, 9, 0))
        .build();

    when(usageLogRepository.findAllByOrderByCalledAtDesc(any(Pageable.class)))
        .thenReturn(List.of(log));

    List<McpUsageLogDto> result = service.getRecentLogs(10);

    assertThat(result).hasSize(1);
    McpUsageLogDto dto = result.get(0);
    assertThat(dto.getUsername()).isEqualTo("bob");
    assertThat(dto.getToolName()).isEqualTo("get_tasks");
    assertThat(dto.getSuccess()).isTrue();
    assertThat(dto.getDurationMs()).isEqualTo(42L);
    assertThat(dto.getApiKeyPrefix()).isEqualTo("sf_live_");
  }

  @Test
  void record_savesEntryForKnownUser() {
    User user = new User();
    user.setUsername("carol");
    when(userRepository.findByUsername("carol")).thenReturn(Optional.of(user));

    service.record("carol", null, "list_projects", true, null, 100L);

    verify(usageLogRepository).save(any(McpUsageLog.class));
  }

  @Test
  void record_skipsWhenUserNotFound() {
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    service.record("ghost", null, "list_projects", true, null, 50L);

    verify(usageLogRepository, org.mockito.Mockito.never()).save(any());
  }
}
