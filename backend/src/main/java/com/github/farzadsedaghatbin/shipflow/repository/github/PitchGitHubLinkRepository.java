package com.github.farzadsedaghatbin.shipflow.repository.github;

import com.github.farzadsedaghatbin.shipflow.entity.enums.GitHubLinkType;
import com.github.farzadsedaghatbin.shipflow.entity.github.PitchGitHubLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PitchGitHubLinkRepository extends JpaRepository<PitchGitHubLink, Long> {
  List<PitchGitHubLink> findByPitchId(Long pitchId);

  List<PitchGitHubLink> findByPitchIdAndLinkType(Long pitchId, GitHubLinkType linkType);

  List<PitchGitHubLink> findByCommitId(Long commitId);

  List<PitchGitHubLink> findByPullRequestId(Long pullRequestId);

  List<PitchGitHubLink> findByBranchId(Long branchId);
}
