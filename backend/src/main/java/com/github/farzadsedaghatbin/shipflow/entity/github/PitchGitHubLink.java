package com.github.farzadsedaghatbin.shipflow.entity.github;

import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.GitHubLinkType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pitch_github_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchGitHubLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitch_id", nullable = false)
    private Pitch pitch;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 50)
    private GitHubLinkType linkType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commit_id")
    private GitHubCommit commit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id")
    private GitHubPullRequest pullRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private GitHubBranch branch;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_by_user_id")
    private User linkedByUser;

    @PrePersist
    protected void onCreate() {
        linkedAt = LocalDateTime.now();
    }
}
