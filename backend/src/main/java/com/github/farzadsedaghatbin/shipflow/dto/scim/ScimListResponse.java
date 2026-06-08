package com.github.farzadsedaghatbin.shipflow.dto.scim;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.*;

/** SCIM 2.0 list response envelope (RFC 7644 §3.4.2). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScimListResponse {

  private List<String> schemas;
  private int totalResults;
  private int startIndex;
  private int itemsPerPage;

  /** Upper-case R to match the SCIM spec field name {@code Resources}. */
  @JsonProperty("Resources")
  private List<ScimUser> resources;
}
