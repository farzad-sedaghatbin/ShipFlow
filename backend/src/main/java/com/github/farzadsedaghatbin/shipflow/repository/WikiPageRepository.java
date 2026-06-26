package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.WikiPage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WikiPageRepository extends JpaRepository<WikiPage, Long> {

  List<WikiPage> findBySpaceIdAndParentIdAndDeletedAtIsNullOrderByPositionAsc(
      Long spaceId, Long parentId);

  List<WikiPage> findBySpaceIdAndDeletedAtIsNull(Long spaceId);

  List<WikiPage> findBySpaceIdAndParentIdIsNullAndDeletedAtIsNullOrderByPositionAsc(Long spaceId);

  boolean existsByIdAndDeletedAtIsNull(Long id);
}
