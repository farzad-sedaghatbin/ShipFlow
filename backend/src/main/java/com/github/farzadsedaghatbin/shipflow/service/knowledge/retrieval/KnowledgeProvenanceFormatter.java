package com.github.farzadsedaghatbin.shipflow.service.knowledge.retrieval;

import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeItem;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeEntityType;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeProvenanceFormatter {

  /** Returns a one-line prefix used as a tag in the prompt for a retrieved chunk. */
  public String tag(KnowledgeItem item) {
    if (item == null) return "[Unknown source]";
    if (item.getEntityType() == KnowledgeEntityType.KNOWLEDGE_SOURCE) {
      KnowledgeSource s = item.getKnowledgeSource();
      String provider = s == null ? "?" : s.getProviderType().name().toLowerCase();
      String name = s == null ? "Knowledge Center" : s.getName();
      return "[Knowledge Center — \"" + name + "\" (" + provider + ")]";
    }
    String type = item.getEntityType() != null ? item.getEntityType().name().toLowerCase() : "unknown";
    return "[" + type + " — \"" + nullSafe(item.getTitle()) + "\"]";
  }

  private String nullSafe(String s) {
    return s == null ? "" : s;
  }
}
