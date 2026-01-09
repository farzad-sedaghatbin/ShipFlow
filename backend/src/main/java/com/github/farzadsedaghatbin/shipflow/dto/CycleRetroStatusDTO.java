package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleRetroStatusDTO {
    private Long cycleId;
    private String cycleName;
    private Integer totalRetros;
    private Integer closedRetros;
    private Boolean canCloseCycle;
    private String message;
}
