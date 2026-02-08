package com.github.farzadsedaghatbin.shipflow.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingDTO {
  private Long id;
  private Long pitchId;
  private String pitchTitle;
  private Long cycleId;
  private String cycleName;
  private Long projectId;
  private String projectName;
  private String projectKey;
  private String type;
  private LocalDate dateHeld;
  private Boolean dorReady;
  private Boolean dodReady;
  @Builder.Default
  private List<MeetingChecklistItem> dorItems = new ArrayList<>();
  @Builder.Default
  private List<MeetingChecklistItem> dodItems = new ArrayList<>();
  private String notes;
  private Long retrospectiveId;
  private String retrospectiveTitle;
  private String decisions;
  private String attendees;

  @Builder.Default
  private List<MeetingActionDTO> actions = new ArrayList<>();

  /**
   * A checklist item for DOR/DOD with completion status.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class MeetingChecklistItem {
    private Long id;
    private String name;
    private String description;
    private Boolean isRequired;
    private Boolean isCompleted;
  }
}
