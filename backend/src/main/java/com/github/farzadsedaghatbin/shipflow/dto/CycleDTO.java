package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleDTO {
    private Long id;
    private Long projectId;
    private String projectName;
    private String projectKey;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private CyclePhase phase;
    private Boolean isActive;
    private Integer pitchCount;
    private Integer teamCount;
}
