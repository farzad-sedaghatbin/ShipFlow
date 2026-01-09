package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for users creating work logs for themselves.
 * Does not require personId - the current user's linked person will be used.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkLogForSelfRequest {
    
    @NotNull(message = "Pitch ID is required")
    private Long pitchId;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    @NotNull(message = "Hours spent is required")
    @DecimalMin(value = "0.25", message = "Minimum time is 15 minutes (0.25 hours)")
    private BigDecimal hoursSpent;
    
    private String note;
}
