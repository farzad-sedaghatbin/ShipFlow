package com.github.farzadsedaghatbin.shipflow.dto.hillchart;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for hill chart position history.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HillChartHistoryDTO {
    
    private Long id;
    private Long hillChartPointId;
    private String scope;
    private Long pitchId;
    private String pitchTitle;
    private Long userId;
    private String userName;
    
    private Integer previousPosition;
    private Integer newPosition;
    private Integer confidenceLevel;
    private String note;
    
    private Double actualHoursAtMove;
    private Double progressAtMove;
    
    private LocalDateTime createdAt;
}
