package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartTimerRequest {
    private Long pitchId;
    private Long taskId;
    private String note;
}
