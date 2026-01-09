package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkLogDTO {
    private Long id;
    private Long personId;
    private String personName;
    private Long pitchId;
    private String pitchTitle;
    private Long cycleId;
    private String cycleName;
    private Long projectId;
    private String projectName;
    private String projectKey;
    private LocalDate date;
    private BigDecimal hoursSpent;
    private String note;
}
