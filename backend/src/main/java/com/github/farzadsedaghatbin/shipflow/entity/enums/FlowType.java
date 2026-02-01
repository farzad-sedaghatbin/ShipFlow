package com.github.farzadsedaghatbin.shipflow.entity.enums;

/** Enum representing different types of Microsoft Teams Power Automate flows */
public enum FlowType {
  /** Traditional Teams Incoming Webhook connector */
  WEBHOOK,

  /** Power Automate flow that posts to channel */
  POWER_AUTOMATE_POST,

  /** Power Automate flow that creates new thread */
  POWER_AUTOMATE_THREAD
}
