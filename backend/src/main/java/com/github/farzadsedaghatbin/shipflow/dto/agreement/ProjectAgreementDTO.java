package com.github.farzadsedaghatbin.shipflow.dto.agreement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Response DTO for a ProjectAgreement entry.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAgreementDTO {

  private Long id;
  private Long projectId;
  private String title;
  private String content;
  private Long meetingId;
  private LocalDate agreedDate;
  private String createdByName;
  private LocalDateTime createdAt;
}
