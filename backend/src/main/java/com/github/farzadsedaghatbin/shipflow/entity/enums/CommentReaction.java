package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Enum representing available emoji reactions for comments. Limited to 8
 * common, universally understood emojis.
 */
public enum CommentReaction {
  THUMBS_UP("👍"), THUMBS_DOWN("👎"), HEART("❤️"), LAUGH("😄"), SURPRISED("😮"), SAD("😢"), ROCKET("🚀"), EYES("👀");

  private final String emoji;

  CommentReaction(String emoji) {
    this.emoji = emoji;
  }

  public String getEmoji() {
    return emoji;
  }
}
