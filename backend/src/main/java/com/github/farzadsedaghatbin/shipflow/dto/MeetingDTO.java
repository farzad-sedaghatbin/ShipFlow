package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.MeetingType;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    private MeetingType type;
    private LocalDate dateHeld;
    private Boolean dorReady;
    private Boolean dodReady;
    private String notes;
    private Long retrospectiveId;
    private String retrospectiveTitle;
    private String decisions;
    private String attendees;
    
    @Builder.Default
    private List<MeetingActionDTO> actions = new ArrayList<>();
}
