package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.TechStackType;
import com.github.farzadsedaghatbin.shipflow.entity.RepositoryTechStack;
import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryTechStackRepository extends JpaRepository<RepositoryTechStack, Long> {

    /**
     * Find all tech stacks detected for a repository.
     */
    List<RepositoryTechStack> findByRepository(GitHubRepository repository);

    /**
     * Find all tech stacks detected for a repository by ID.
     */
    @Query("SELECT rts FROM RepositoryTechStack rts WHERE rts.repository.id = :repositoryId")
    List<RepositoryTechStack> findByRepositoryId(@Param("repositoryId") Long repositoryId);

    /**
     * Find a specific tech stack for a repository.
     */
    Optional<RepositoryTechStack> findByRepositoryAndStackType(GitHubRepository repository, TechStackType stackType);

    /**
     * Check if a repository has any cached tech stacks.
     */
    boolean existsByRepository(GitHubRepository repository);

    /**
     * Delete all tech stacks for a repository (e.g., when forcing re-detection).
     */
    void deleteByRepository(GitHubRepository repository);
}
