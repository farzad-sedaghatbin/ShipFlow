package com.github.farzadsedaghatbin.shipflow.event;

/** Event published when a wiki page is created, updated, deleted, or restored. */
public record WikiPageChangedEvent(Long pageId, Long spaceId, ChangeType type) {
  public enum ChangeType {
    CREATED,
    UPDATED,
    DELETED,
    RESTORED
  }
}
