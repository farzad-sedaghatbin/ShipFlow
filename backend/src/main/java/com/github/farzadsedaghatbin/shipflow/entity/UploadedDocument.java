package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity for storing uploaded documents and their extracted content.
 */
@Entity
@Table(name = "uploaded_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String fileType;

    private Long fileSize;

    @Column(length = 500)
    private String storagePath;

    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @Column(nullable = false)
    private boolean textExtracted;

    // Association - document can be linked to pitch, meeting, cycle, or note
    @Column(name = "entity_type")
    private String entityType; // PITCH, MEETING, CYCLE, NOTE

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "uploader_id")
    private Long uploaderId;

    @Column(name = "uploader_username")
    private String uploaderUsername;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "indexed_for_qa")
    private boolean indexedForQA;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
