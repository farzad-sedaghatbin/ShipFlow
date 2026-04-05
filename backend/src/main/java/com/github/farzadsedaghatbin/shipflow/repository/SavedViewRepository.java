package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.SavedView;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedViewRepository extends JpaRepository<SavedView, Long> {

  List<SavedView> findByUserIdAndProjectId(Long userId, Long projectId);

  Optional<SavedView> findByIdAndUserId(Long id, Long userId);

  void deleteByIdAndUserId(Long id, Long userId);

  Optional<SavedView> findByUserIdAndProjectIdAndIsDefaultTrue(Long userId, Long projectId);

  boolean existsByUserIdAndProjectIdAndName(Long userId, Long projectId, String name);
}
