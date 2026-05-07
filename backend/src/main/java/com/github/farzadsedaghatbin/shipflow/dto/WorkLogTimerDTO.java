package com.github.farzadsedaghatbin.shipflow.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkLogTimerDTO {
  private Long id;
  private Long personId;
  private String personName;
  private Long pitchId;
  private String pitchTitle;
  private Long taskId;
  private String taskTitle;
  private LocalDateTime startTime;
  private String note;
  private Long elapsedSeconds; // Calculated duration
}
