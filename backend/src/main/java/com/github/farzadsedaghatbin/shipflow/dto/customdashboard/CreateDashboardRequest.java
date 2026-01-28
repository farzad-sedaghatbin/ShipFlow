package com.github.farzadsedaghatbin.shipflow.dto.customdashboard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDashboardRequest {
    
    @NotBlank(message = "Dashboard name is required")
    @Size(max = 100, message = "Dashboard name must not exceed 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private String layoutConfig;
    
    private Boolean setAsDefault;
    
    private Long cloneFromTemplateId;
    
    // Scope fields - all optional
    private Long cycleId;
    
    private Long pitchId;
    
    private Long teamId;
}
