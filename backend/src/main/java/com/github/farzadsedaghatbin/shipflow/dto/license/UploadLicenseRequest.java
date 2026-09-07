package com.github.farzadsedaghatbin.shipflow.dto.license;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** {@code POST /api/license} request body — the raw licence file text, uploaded or pasted. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadLicenseRequest {

  @NotBlank
  private String content;
}
