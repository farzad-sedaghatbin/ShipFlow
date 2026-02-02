package com.github.farzadsedaghatbin.shipflow.entity.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

/**
 * Custom revision entity that extends the default Envers revision
 * to include the username of who made the change.
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(AuditRevisionListener.class)
@Getter
@Setter
public class AuditRevisionEntity extends DefaultRevisionEntity {

    /**
     * The username of the user who made the change.
     * This is automatically populated by the AuditRevisionListener
     * from the Spring Security context.
     */
    @Column(name = "modified_by")
    private String modifiedBy;
}
