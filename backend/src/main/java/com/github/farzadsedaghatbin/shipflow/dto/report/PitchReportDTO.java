package com.github.farzadsedaghatbin.shipflow.dto.report;

import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchReportDTO {
    private Long pitchId;
    private String pitchTitle;
    private String teamName;
    private PitchStatus status;
    private Integer appetiteDays;
    private Double appetiteHours;
    private Double actualHours;
    private Double varianceHours;
    private Double variancePercentage;
    private Boolean isOverBudget;
}
