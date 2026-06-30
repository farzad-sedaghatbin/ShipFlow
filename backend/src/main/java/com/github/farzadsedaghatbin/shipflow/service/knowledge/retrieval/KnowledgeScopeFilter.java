package com.github.farzadsedaghatbin.shipflow.service.knowledge.retrieval;

import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeItem;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeEntityType;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Determines whether a retrieved {@link KnowledgeItem} is visible under a given
 * {@link RetrievalScope}. Used by AI services that do vector retrieval to filter
 * out items whose backing {@link KnowledgeSource} is not visible to the caller.
 */
@Component
@RequiredArgsConstructor
public class KnowledgeScopeFilter {

  private final KnowledgeSourceRepository sources;

  /** True when the given KnowledgeItem is visible under the requested retrieval scope. */
  public boolean isVisible(KnowledgeItem item, RetrievalScope scope) {
    if (item.getEntityType() != KnowledgeEntityType.KNOWLEDGE_SOURCE) {
      // Pre-existing entity-bound items keep their current visibility behavior — passthrough.
      return true;
    }
    KnowledgeSource src = item.getKnowledgeSource();
    if (src == null && item.getKnowledgeSourceId() != null) {
      src = sources.findActiveById(item.getKnowledgeSourceId()).orElse(null);
    }
    if (src == null) {
      return false;
    }
    return switch (src.getScope()) {
      case ORG -> true;
      case TEAM -> scope.getTeamIds() != null
          && src.getTeamId() != null
          && scope.getTeamIds().contains(src.getTeamId());
      case PROJECT -> scope.getProjectId() != null
          && src.getProjectId() != null
          && scope.getProjectId().equals(src.getProjectId());
    };
  }
}
