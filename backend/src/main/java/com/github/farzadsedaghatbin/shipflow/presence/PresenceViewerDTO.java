package com.github.farzadsedaghatbin.shipflow.presence;

import lombok.Value;

/**
 * A single active viewer of a presence-tracked entity. Serializes as {@code {"userId":..,
 * "displayName":..}} — part of the fixed {@code presence-update} SSE payload contract.
 */
@Value
public class PresenceViewerDTO {
  Long userId;
  String displayName;
}
