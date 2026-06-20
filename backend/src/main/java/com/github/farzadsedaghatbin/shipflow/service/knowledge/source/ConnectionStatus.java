package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import lombok.Value;

@Value
public class ConnectionStatus {
  boolean ok;
  String message;

  public static ConnectionStatus ok() {
    return new ConnectionStatus(true, null);
  }

  public static ConnectionStatus fail(String why) {
    return new ConnectionStatus(false, why);
  }
}
