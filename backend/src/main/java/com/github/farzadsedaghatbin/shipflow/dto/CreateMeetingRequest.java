package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.validation.ValidMeetingType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMeetingRequest {
  private Long pitchId;
  private Long projectId;

  @NotNull(message = "Meeting type is required")
  @ValidMeetingType(message = "Invalid meeting type")
  private String type;

  @NotNull(message = "Date held is required")
  private LocalDate dateHeld;

  @Builder.Default
  private Boolean dorReady = false;
  @Builder.Default
  private Boolean dodReady = false;
  @Builder.Default
  private List<MeetingDTO.MeetingChecklistItem> dorItems = new ArrayList<>();
  @Builder.Default
  private List<MeetingDTO.MeetingChecklistItem> dodItems = new ArrayList<>();
  private String notes;
  private Long retrospectiveId;
  private String decisions;
  private String attendees;

  @Builder.Default
  private List<MeetingActionDTO> actions = new ArrayList<>();
  
  /**
   * Custom setter for type field to normalize the value (trim and uppercase).
   * This ensures consistent storage and comparison of meeting types.
   */
  public void setType(String type) {
    this.type = type != null ? type.trim().toUpperCase() : null;
  }
}
