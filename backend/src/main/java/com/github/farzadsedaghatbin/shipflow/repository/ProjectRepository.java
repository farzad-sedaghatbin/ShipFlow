package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

  Optional<Project> findByProjectKey(String projectKey);

  boolean existsByProjectKey(String projectKey);

  List<Project> findByIsActiveTrue();

  @Query("SELECT p FROM Project p WHERE p.owner.id = :ownerId")
  List<Project> findByOwnerId(@Param("ownerId") Long ownerId);

  @Query("SELECT p FROM Project p WHERE p.isActive = true ORDER BY p.name")
  List<Project> findAllActiveOrderByName();

  @Query("SELECT p FROM Project p LEFT JOIN FETCH p.owner WHERE p.id = :id")
  Optional<Project> findByIdWithOwner(@Param("id") Long id);
}
