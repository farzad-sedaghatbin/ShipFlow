package com.github.farzadsedaghatbin.shipflow.entity.enums;

/** Lifecycle of a change the ShipFlow Agent proposed and a person has yet to approve. */
public enum AgentActionStatus {
  /** Waiting for the requester to confirm or cancel. */
  PROPOSED,
  /** The requester confirmed; the change is being applied (the claim that prevents a double run). */
  CONFIRMED,
  /** Applied successfully. */
  EXECUTED,
  /** Confirmed but the change could not be applied (see the stored message). */
  FAILED,
  /** The requester declined it. */
  CANCELLED,
  /** Not confirmed in time; it can no longer be applied. */
  EXPIRED
}
