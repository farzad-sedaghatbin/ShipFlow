package com.github.farzadsedaghatbin.shipflow.repository.github;

import com.github.farzadsedaghatbin.shipflow.entity.github.PitchGitHubLink;
import com.github.farzadsedaghatbin.shipflow.entity.enums.GitHubLinkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PitchGitHubLinkRepository extends JpaRepository<PitchGitHubLink, Long> {
    List<PitchGitHubLink> findByPitchId(Long pitchId);
    List<PitchGitHubLink> findByPitchIdAndLinkType(Long pitchId, GitHubLinkType linkType);
    List<PitchGitHubLink> findByCommitId(Long commitId);
    List<PitchGitHubLink> findByPullRequestId(Long pullRequestId);
    List<PitchGitHubLink> findByBranchId(Long branchId);
}
