package com.github.farzadsedaghatbin.shipflow.service.wiki;

import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiRevisionDTO;
import com.github.farzadsedaghatbin.shipflow.entity.WikiPage;
import java.util.List;
import java.util.Optional;

/** Seam over Hibernate Envers so WikiService is testable without a live EntityManager. */
public interface WikiHistoryReader {

  List<WikiRevisionDTO> history(Long pageId);

  Optional<WikiPage> revision(Long pageId, int revision);
}
