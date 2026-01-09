package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateHillChartPointRequest {
    @Size(max = 100, message = "Scope must not exceed 100 characters")
    private String scope;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Min(value = 0, message = "Position must be between 0 and 100")
    @Max(value = 100, message = "Position must be between 0 and 100")
    private Integer position;
}
