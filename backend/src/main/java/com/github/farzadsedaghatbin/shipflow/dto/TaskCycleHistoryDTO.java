package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.TaskCycleHistory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Read-only DTO for a single {@link TaskCycleHistory} snapshot row — the cycle-history audit
 * trail exposed by {@code GET /api/tasks/{id}/cycle-history}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCycleHistoryDTO {

  private Long id;
  private Long cycleId;
  private String cycleName;
  private TaskStatus status;
  private TaskCycleHistory.ChangeSource source;
  private LocalDateTime recordedAt;
}
