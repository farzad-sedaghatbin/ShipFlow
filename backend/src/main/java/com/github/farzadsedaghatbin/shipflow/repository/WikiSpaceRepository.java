package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.WikiSpace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WikiSpaceRepository extends JpaRepository<WikiSpace, Long> {

  List<WikiSpace> findByDeletedAtIsNullOrderByNameAsc();

  Optional<WikiSpace> findBySpaceKeyAndDeletedAtIsNull(String spaceKey);
}
