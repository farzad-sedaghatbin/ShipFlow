package com.github.farzadsedaghatbin.shipflow.service.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeItem;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import org.junit.jupiter.api.Test;

class KnowledgeProvenanceFormatterTest {

  KnowledgeProvenanceFormatter f = new KnowledgeProvenanceFormatter();

  @Test
  void knowledge_source_item_tag_includes_name_and_provider() {
    var src = KnowledgeSource.builder().id(1L).name("Engineering Handbook")
        .providerType(KnowledgeProviderType.FILE_UPLOAD).build();
    var item = new KnowledgeItem();
    item.setEntityType(KnowledgeEntityType.KNOWLEDGE_SOURCE);
    item.setKnowledgeSource(src);
    assertThat(f.tag(item)).isEqualTo("[Knowledge Center — \"Engineering Handbook\" (file_upload)]");
  }

  @Test
  void pitch_item_tag_uses_title() {
    var item = new KnowledgeItem();
    item.setEntityType(KnowledgeEntityType.PITCH);
    item.setTitle("Auth retrospective");
    assertThat(f.tag(item)).isEqualTo("[pitch — \"Auth retrospective\"]");
  }

  @Test
  void null_item_returns_unknown() {
    assertThat(f.tag(null)).isEqualTo("[Unknown source]");
  }
}
