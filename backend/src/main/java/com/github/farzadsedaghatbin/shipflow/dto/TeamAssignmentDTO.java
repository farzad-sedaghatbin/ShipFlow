package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TeamMemberRole;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamAssignmentDTO {
    private Long id;
    private Long personId;
    private String personName;
    private Long teamId;
    private String teamName;
    private Long cycleId;
    private String cycleName;
    private TeamMemberRole role;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private String notes;
}
