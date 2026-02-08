package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Enumeration of permission types for RBAC. Defines what actions can be
 * performed on resources.
 */
public enum PermissionType {
  /** Create new resources */
  CREATE,

  /** Read/view resources */
  READ,

  /** Update/modify existing resources */
  UPDATE,

  /** Delete resources */
  DELETE,

  /** Execute special operations (e.g., run reports, trigger AI analysis) */
  EXECUTE,

  /** Manage resource settings and configuration */
  MANAGE,

  /** Approve or reject resources (e.g., approve pitches) */
  APPROVE
}
