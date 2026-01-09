package com.github.farzadsedaghatbin.shipflow.dto.report;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TeamMemberRole;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberWorkReportDTO {
    private Long memberId;
    private String memberName;
    private TeamMemberRole role;
    private String teamName;
    private Double totalHours;
    private Integer workDays;
    private Double avgHoursPerDay;
    private List<PitchWorkSummary> pitchWork;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PitchWorkSummary {
        private Long pitchId;
        private String pitchTitle;
        private Double hoursSpent;
    }
}
