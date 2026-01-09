package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.MeetingType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMeetingRequest {
    private Long pitchId;
    
    @NotNull(message = "Meeting type is required")
    private MeetingType type;
    
    @NotNull(message = "Date held is required")
    private LocalDate dateHeld;
    
    private Boolean dorReady = false;
    private Boolean dodReady = false;
    private String notes;
}
