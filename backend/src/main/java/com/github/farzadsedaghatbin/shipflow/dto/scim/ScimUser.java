package com.github.farzadsedaghatbin.shipflow.dto.scim;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.*;

/**
 * SCIM 2.0 User resource (RFC 7643 §4.1).
 *
 * <p>Field names match the SCIM spec capitalisation (e.g. {@code userName}, {@code externalId}).
 * Jackson will serialise them as-is.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScimUser {

  private List<String> schemas;

  /** ShipFlow internal user id (as string). */
  private String id;

  /** External IdP identifier provided by the provisioning client. */
  private String externalId;

  /** Unique username / login email. */
  private String userName;

  private ScimName name;

  private List<ScimEmail> emails;

  private boolean active;

  private ScimMeta meta;

  // ── Nested types ─────────────────────────────────────────────────────────

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ScimName {
    private String formatted;
    private String givenName;
    private String familyName;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ScimEmail {
    private String value;
    private String type;

    @JsonProperty("primary")
    private boolean primary;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ScimMeta {
    private String resourceType;
    private String location;
  }
}
