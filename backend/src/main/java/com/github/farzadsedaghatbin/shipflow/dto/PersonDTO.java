package com.github.farzadsedaghatbin.shipflow.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonDTO {
  private Long id;
  private String name;
  private String email;
  private String avatarUrl;
  private String skills;
  private Boolean isActive;
  private LocalDateTime createdAt;
  private List<TeamAssignmentDTO> currentAssignments;
  private List<TeamAssignmentDTO> pastAssignments;
}
