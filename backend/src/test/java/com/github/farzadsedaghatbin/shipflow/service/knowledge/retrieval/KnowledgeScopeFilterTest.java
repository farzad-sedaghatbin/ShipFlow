package com.github.farzadsedaghatbin.shipflow.service.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeItem;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeSourceRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;

class KnowledgeScopeFilterTest {

  KnowledgeSourceRepository repo = mock(KnowledgeSourceRepository.class);
  KnowledgeScopeFilter f = new KnowledgeScopeFilter(repo);

  @Test
  void non_knowledge_source_items_pass_through() {
    var item = new KnowledgeItem();
    item.setEntityType(KnowledgeEntityType.PITCH);
    assertThat(f.isVisible(item, RetrievalScope.builder().teamIds(Set.of()).build())).isTrue();
  }

  @Test
  void org_scope_visible_to_anyone() {
    var src = KnowledgeSource.builder().id(1L).scope(KnowledgeSourceScope.ORG).build();
    var item = new KnowledgeItem();
    item.setEntityType(KnowledgeEntityType.KNOWLEDGE_SOURCE);
    item.setKnowledgeSource(src);
    assertThat(f.isVisible(item, RetrievalScope.builder().teamIds(Set.of()).build())).isTrue();
  }

  @Test
  void team_scope_visible_when_user_in_team() {
    var src = KnowledgeSource.builder().id(2L).teamId(7L).scope(KnowledgeSourceScope.TEAM).build();
    var item = new KnowledgeItem();
    item.setEntityType(KnowledgeEntityType.KNOWLEDGE_SOURCE);
    item.setKnowledgeSource(src);
    var scope = RetrievalScope.builder().teamIds(Set.of(7L, 8L)).build();
    assertThat(f.isVisible(item, scope)).isTrue();
  }

  @Test
  void team_scope_hidden_when_user_not_in_team() {
    var src = KnowledgeSource.builder().id(3L).teamId(99L).scope(KnowledgeSourceScope.TEAM).build();
    var item = new KnowledgeItem();
    item.setEntityType(KnowledgeEntityType.KNOWLEDGE_SOURCE);
    item.setKnowledgeSource(src);
    var scope = RetrievalScope.builder().teamIds(Set.of(7L, 8L)).build();
    assertThat(f.isVisible(item, scope)).isFalse();
  }

  @Test
  void project_scope_visible_to_matching_project() {
    var src = KnowledgeSource.builder().id(4L).projectId(42L).scope(KnowledgeSourceScope.PROJECT).build();
    var item = new KnowledgeItem();
    item.setEntityType(KnowledgeEntityType.KNOWLEDGE_SOURCE);
    item.setKnowledgeSource(src);
    var scope = RetrievalScope.builder().teamIds(Set.of()).projectId(42L).build();
    assertThat(f.isVisible(item, scope)).isTrue();
  }

  @Test
  void project_scope_hidden_for_other_project() {
    var src = KnowledgeSource.builder().id(5L).projectId(42L).scope(KnowledgeSourceScope.PROJECT).build();
    var item = new KnowledgeItem();
    item.setEntityType(KnowledgeEntityType.KNOWLEDGE_SOURCE);
    item.setKnowledgeSource(src);
    var scope = RetrievalScope.builder().teamIds(Set.of()).projectId(7L).build();
    assertThat(f.isVisible(item, scope)).isFalse();
  }
}
