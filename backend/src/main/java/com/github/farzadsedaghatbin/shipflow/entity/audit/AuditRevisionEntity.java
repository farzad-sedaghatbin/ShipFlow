package com.github.farzadsedaghatbin.shipflow.entity.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.io.Serializable;

/**
 * Custom revision entity that tracks audit information.
 * Uses 'rev' column name to match the existing database schema.
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(AuditRevisionListener.class)
@Getter
@Setter
public class AuditRevisionEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev")
    private int id;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private long timestamp;

    /**
     * The username of the user who made the change.
     * This is automatically populated by the AuditRevisionListener
     * from the Spring Security context.
     */
    @Column(name = "modified_by")
    private String modifiedBy;
}
