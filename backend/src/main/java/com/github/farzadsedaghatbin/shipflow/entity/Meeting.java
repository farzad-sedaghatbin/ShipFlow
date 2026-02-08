package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "meetings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meeting {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pitch_id")
  private Pitch pitch;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private LocalDate dateHeld;

  @Column(nullable = false)
  private Boolean dorReady;

  @Column(nullable = false)
  private Boolean dodReady;

  // DOR checklist items (stored as JSON)
  @Column(columnDefinition = "TEXT")
  private String dorItemsJson;

  // DOD checklist items (stored as JSON)
  @Column(columnDefinition = "TEXT")
  private String dodItemsJson;

  @Column(columnDefinition = "TEXT")
  private String notes;

  // Link to retrospective for RETROSPECTIVE meetings
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "retrospective_id")
  private Retrospective retrospective;

  @Column(columnDefinition = "TEXT")
  private String decisions;

  @Column(columnDefinition = "TEXT")
  private String attendees;

  @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<MeetingAction> actions = new ArrayList<>();
}
