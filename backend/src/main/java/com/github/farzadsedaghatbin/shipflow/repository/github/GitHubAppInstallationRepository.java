package com.github.farzadsedaghatbin.shipflow.repository.github;

import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubAppInstallation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitHubAppInstallationRepository extends JpaRepository<GitHubAppInstallation, Long> {

    /**
     * Find installation by GitHub installation ID
     */
    Optional<GitHubAppInstallation> findByInstallationId(Long installationId);

    /**
     * Find installation by account login (organization or user name)
     */
    Optional<GitHubAppInstallation> findByAccountLogin(String accountLogin);

    /**
     * Find all active installations
     */
    List<GitHubAppInstallation> findByIsActiveTrue();

    /**
     * Find all installations for an account type (Organization or User)
     */
    List<GitHubAppInstallation> findByAccountTypeAndIsActiveTrue(String accountType);

    /**
     * Check if an installation exists for a given account
     */
    boolean existsByAccountLogin(String accountLogin);

    /**
     * Find installations that need token refresh
     */
    @Query("SELECT i FROM GitHubAppInstallation i WHERE i.isActive = true AND " +
           "(i.tokenExpiresAt IS NULL OR i.tokenExpiresAt < CURRENT_TIMESTAMP)")
    List<GitHubAppInstallation> findInstallationsNeedingTokenRefresh();

    /**
     * Count total repositories across all active installations
     */
    @Query("SELECT COALESCE(SUM(i.repositoryCount), 0) FROM GitHubAppInstallation i WHERE i.isActive = true")
    Long countTotalRepositories();
}
