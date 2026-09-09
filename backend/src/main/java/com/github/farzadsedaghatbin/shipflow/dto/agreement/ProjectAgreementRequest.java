package com.github.farzadsedaghatbin.shipflow.dto.agreement;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.*;

/**
 * Request DTO for creating/updating a project agreement.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAgreementRequest {

  @NotBlank(message = "Title is required")
  private String title;

  @NotBlank(message = "Content is required")
  private String content;

  /** Optional meeting this agreement originated from — must belong to the same project. */
  private Long meetingId;

  /** Date the agreement was reached; defaults to today when omitted. */
  private LocalDate agreedDate;
}
