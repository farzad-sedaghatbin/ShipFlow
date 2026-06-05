package com.github.farzadsedaghatbin.shipflow.dto.scim;

import java.util.List;
import lombok.*;

/** SCIM 2.0 error response (RFC 7644 §3.12). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScimError {

  private List<String> schemas;
  private int status;
  private String detail;
}
