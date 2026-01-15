package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCycleRequest {
    @NotNull(message = "Project ID is required")
    private Long projectId;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    // End date is optional - will be auto-calculated from OrganizationSettings
    // Only ADMIN/PROJECT_MANAGER can provide custom end dates
    private LocalDate endDate;
    
    @Builder.Default
    private CyclePhase phase = CyclePhase.BUILD;
}
