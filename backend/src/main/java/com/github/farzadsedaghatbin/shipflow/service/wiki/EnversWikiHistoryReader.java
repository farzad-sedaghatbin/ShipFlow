package com.github.farzadsedaghatbin.shipflow.service.wiki;

import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiRevisionDTO;
import com.github.farzadsedaghatbin.shipflow.entity.WikiPage;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.stereotype.Component;

@Component
public class EnversWikiHistoryReader implements WikiHistoryReader {

  private final EntityManager entityManager;

  public EnversWikiHistoryReader(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public List<WikiRevisionDTO> history(Long pageId) {
    AuditReader reader = AuditReaderFactory.get(entityManager);
    List<Number> revisions = reader.getRevisions(WikiPage.class, pageId);
    List<WikiRevisionDTO> result = new ArrayList<>();
    for (Number rev : revisions) {
      WikiPage page = reader.find(WikiPage.class, pageId, rev);
      Date revDate = reader.getRevisionDate(rev);
      Instant ts = revDate != null ? revDate.toInstant() : null;
      // Use createdBy as the editor reference since WikiPage tracks creator only.
      Long editorId = page != null ? page.getCreatedBy() : null;
      String title = page != null ? page.getTitle() : null;
      result.add(new WikiRevisionDTO(rev.intValue(), ts, editorId, title));
    }
    return result;
  }

  @Override
  public Optional<WikiPage> revision(Long pageId, int revision) {
    AuditReader reader = AuditReaderFactory.get(entityManager);
    WikiPage page = reader.find(WikiPage.class, pageId, revision);
    return Optional.ofNullable(page);
  }
}
