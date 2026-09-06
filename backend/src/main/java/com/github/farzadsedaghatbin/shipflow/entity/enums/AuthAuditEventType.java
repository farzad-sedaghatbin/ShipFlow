package com.github.farzadsedaghatbin.shipflow.entity.enums;

/** Authentication events recorded in the auth audit trail. */
public enum AuthAuditEventType {
  /** Username + password accepted. */
  LOGIN_SUCCESS,
  /** Username + password rejected — wrong password, unknown user, or disabled account. */
  LOGIN_FAILURE,
  /** Signed in with a passkey (WebAuthn). */
  PASSKEY_LOGIN_SUCCESS,
  /** Passkey assertion rejected. */
  PASSKEY_LOGIN_FAILURE,
  /** A new passkey was registered against an account. */
  PASSKEY_REGISTERED,
  /** A passkey was removed from an account. */
  PASSKEY_REMOVED,
  /** Password changed while signed in. */
  PASSWORD_CHANGED,
  /** A password reset was requested by email. */
  PASSWORD_RESET_REQUESTED,
  /** A password reset token was redeemed. */
  PASSWORD_RESET_COMPLETED,
  /** A new account was created through self-registration. */
  ACCOUNT_REGISTERED,
  /** Explicit sign-out. */
  LOGOUT
}
