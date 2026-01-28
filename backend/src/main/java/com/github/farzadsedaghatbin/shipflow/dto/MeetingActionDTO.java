package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionStatus;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingActionDTO {
    private Long id;
    private String description;
    private Long assignedToId;
    private String assignedToName;
    private ActionStatus status;
    private LocalDate dueDate;
    private String notes;
}
