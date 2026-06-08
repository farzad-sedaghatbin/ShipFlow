package com.github.farzadsedaghatbin.shipflow.entity.enums;

/** Supported import source formats for competitor migration tooling. */
public enum ImportSourceFormat {
  JIRA_CSV,
  LINEAR_CSV,
  ASANA_CSV,
  GENERIC_CSV,
  /** Linear API import (OAuth-based, v1.2.0 S29). */
  LINEAR_API,
  /** Jira API import (OAuth-based, v1.2.0 S30). */
  JIRA_API
}
