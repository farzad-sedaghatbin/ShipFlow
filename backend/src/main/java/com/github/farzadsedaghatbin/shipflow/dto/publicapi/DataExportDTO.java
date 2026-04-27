package com.github.farzadsedaghatbin.shipflow.dto.publicapi;

import java.util.List;
import lombok.*;

/**
 * Wrapper used for data export/import payloads.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataExportDTO {
  private String version;
  private String exportedAt;
  private String projectKey;
  private PublicProjectDTO project;
  private List<PublicCycleDTO> cycles;
  private List<PublicPitchDTO> pitches;
  private List<PublicTaskDTO> tasks;
  private List<PublicBugDTO> bugs;
  private List<PublicReleaseDTO> releases;
  private List<PublicEpicDTO> epics;
}
