package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.MeetingType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMeetingRequest {
  private Long pitchId;

  @NotNull(message = "Meeting type is required")
  private MeetingType type;

  @NotNull(message = "Date held is required")
  private LocalDate dateHeld;

  @Builder.Default
  private Boolean dorReady = false;
  @Builder.Default
  private Boolean dodReady = false;
  private String notes;
  private Long retrospectiveId;
  private String decisions;
  private String attendees;

  @Builder.Default
  private List<MeetingActionDTO> actions = new ArrayList<>();
}
