package com.github.farzadsedaghatbin.shipflow.exception;

/** A UI plugin threw while building its page. Reported as 502 so one bad plugin is isolated. */
public class PluginPageException extends RuntimeException {

  public PluginPageException(String message, Throwable cause) {
    super(message, cause);
  }
}
