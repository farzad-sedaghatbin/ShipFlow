package com.github.farzadsedaghatbin.shipflow.presence;

/**
 * The kinds of entities that support live "who's viewing this" presence tracking (v1.13.0 S64).
 * Binds directly as a Spring MVC path variable (exact-name enum match), so these three literal
 * names — {@code PITCH}, {@code RETROSPECTIVE}, {@code WIKI_PAGE} — are part of the fixed
 * frontend/backend API contract.
 */
public enum PresenceEntityType {
  PITCH,
  RETROSPECTIVE,
  WIKI_PAGE
}
