package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.MeetingType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingType type;

    @Column(nullable = false)
    private LocalDate dateHeld;

    @Column(nullable = false)
    private Boolean dorReady;

    @Column(nullable = false)
    private Boolean dodReady;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
