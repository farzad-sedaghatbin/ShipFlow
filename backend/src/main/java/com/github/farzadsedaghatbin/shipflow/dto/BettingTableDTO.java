package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO representing the full betting table view for a cycle
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BettingTableDTO {
    private Long cycleId;
    private String cycleName;
    private Long projectId;
    private String projectName;
    private String projectKey;
    private LocalDate cycleStartDate;
    private LocalDate cycleEndDate;
    private Integer cycleDurationWeeks;
    private Boolean isCycleActive;
    
    // Shaped pitches ready for betting (not yet assigned to a slot)
    private List<PitchDTO> shapedPitches;
    
    // Teams available in this cycle with their slots
    private List<TeamTrackDTO> teamTracks;
    
    // Summary statistics
    private Integer totalShapedPitches;
    private Integer totalAssignedPitches;
    private Integer totalCapacityWeeks;
    private Integer usedCapacityWeeks;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamTrackDTO {
        private Long teamId;
        private String teamName;
        private List<BettingSlotDTO> slots;
        private Integer totalCapacityWeeks;
        private Integer usedCapacityWeeks;
        private Integer availableCapacityWeeks;
    }
}
