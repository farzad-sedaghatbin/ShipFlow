package com.github.farzadsedaghatbin.shipflow.entity.enums;

/** Types of test cases for the QA system. */
public enum TestType {
  /** Smoke tests - critical path validation (5-10 tests, <2 minutes each). */
  SMOKE,

  /**
   * Functional tests - comprehensive feature validation with happy path, edge cases, and error
   * handling.
   */
  FUNCTIONAL,

  /** Regression tests - validate previously working features after changes. */
  REGRESSION,

  /** Integration tests - validate interactions between system components. */
  INTEGRATION,

  /** End-to-end tests - validate complete user workflows. */
  E2E,

  /** Unit tests - validate individual components in isolation. */
  UNIT,

  /** Performance tests - validate system performance under load. */
  PERFORMANCE,

  /** Security tests - validate security controls and vulnerabilities. */
  SECURITY
}
