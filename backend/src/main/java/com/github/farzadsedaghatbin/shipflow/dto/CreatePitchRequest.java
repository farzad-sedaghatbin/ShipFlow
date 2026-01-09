package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePitchRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotNull(message = "Appetite days is required")
    @Min(value = 1, message = "Appetite must be at least 1 day")
    private Integer appetiteDays;
    
    @NotNull(message = "Cycle ID is required")
    private Long cycleId;
    
    private Long teamId;
    
    private PitchStatus status = PitchStatus.PENDING;
}
