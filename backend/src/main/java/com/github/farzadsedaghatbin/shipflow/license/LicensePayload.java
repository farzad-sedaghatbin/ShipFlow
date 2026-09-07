package com.github.farzadsedaghatbin.shipflow.license;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The signed payload of a ShipFlow licence file. A licence file on disk or in
 * the {@code license_file} table is {@code {"payload": <this>, "signature":
 * "<base64 Ed25519 signature over the canonical JSON bytes of payload>"}}.
 *
 * <p>Produced by a separate, private licence-issuer tool — this class is only
 * the reading/verification side. Field set and JSON shape are a fixed
 * contract shared with that tool; do not rename fields without coordinating
 * there too. {@code issuedAt}/{@code expiresAt}/{@code supportUntil} are plain
 * ISO-8601 dates ({@code yyyy-MM-dd}), not timestamps.
 *
 * <p>{@link #instanceId} is the only nullable field — a licence may be
 * instance-agnostic (usable on any self-hosted deployment) or pinned to one.
 * ShipFlow does not currently enforce {@code instanceId} matching; it is
 * reserved for a future per-instance binding check.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} so a future issuer
 * adding a new field doesn't break parsing on an older ShipFlow build —
 * signature verification (over the raw JSON as received, not this POJO) is
 * what actually guarantees integrity, not strict field matching.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicensePayload {

  private String id;

  private String licensee;

  /** Currently always {@code "COMMERCIAL"}; kept as a string, not an enum, for forward compatibility. */
  private String edition;

  private int seats;

  /** Feature flags this licence unlocks, e.g. {@code "sso"}, {@code "audit-export"}. */
  private List<String> features;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate issuedAt;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate expiresAt;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate supportUntil;

  /** Nullable — see class Javadoc. */
  private String instanceId;

  public boolean hasFeature(String featureName) {
    return features != null && features.contains(featureName);
  }
}
