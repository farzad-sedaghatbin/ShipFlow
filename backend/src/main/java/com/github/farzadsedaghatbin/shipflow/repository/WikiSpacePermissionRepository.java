package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.WikiSpacePermission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WikiSpacePermissionRepository extends JpaRepository<WikiSpacePermission, Long> {

  List<WikiSpacePermission> findBySpaceId(Long spaceId);

  boolean existsBySpaceId(Long spaceId);
}
