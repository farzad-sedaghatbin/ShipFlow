package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRetroRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String notes;
    
    @NotNull(message = "Cycle ID is required")
    private Long cycleId;
    
    @NotNull(message = "Project ID is required")
    private Long projectId;
}
