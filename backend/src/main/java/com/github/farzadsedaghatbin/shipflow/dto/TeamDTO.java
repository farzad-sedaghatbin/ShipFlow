package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamDTO {
    private Long id;
    private String name;
    private Long cycleId;
    private String cycleName;
    private Long projectId;
    private String projectName;
    private String projectKey;
    private List<TeamAssignmentDTO> assignments;
}
