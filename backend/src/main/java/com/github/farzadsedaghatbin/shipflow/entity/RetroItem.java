package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "retro_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetroItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RetroColumnType columnType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retrospective_id", nullable = false)
    private Retrospective retrospective;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Builder.Default
    @Column(nullable = false)
    private Integer voteCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_into_id")
    private RetroItem mergedInto;

    @Builder.Default
    @OneToMany(mappedBy = "mergedInto", cascade = CascadeType.ALL)
    private List<RetroItem> mergedItems = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "retroItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RetroItemVote> votes = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
