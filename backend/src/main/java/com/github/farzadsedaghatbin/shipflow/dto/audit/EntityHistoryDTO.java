package com.github.farzadsedaghatbin.shipflow.dto.audit;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single revision entry in the entity's change history.
 * Contains metadata about when and who made the change, plus the list of field changes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityHistoryDTO {

    /**
     * The revision number (sequential identifier for this change).
     */
    private Long revisionNumber;

    /**
     * The timestamp when this revision was created.
     */
    private LocalDateTime revisionDate;

    /**
     * The username of the user who made this change.
     * "system" for changes made by scheduled tasks or migrations.
     */
    private String modifiedBy;

    /**
     * The type of revision: CREATED, MODIFIED, or DELETED.
     */
    private RevisionType revisionType;

    /**
     * The list of individual field changes in this revision.
     * Empty for DELETE revisions, and for CREATE revisions, shows initial values.
     */
    private List<FieldChangeDTO> changes;

    /**
     * Enum representing the type of revision.
     */
    public enum RevisionType {
        CREATED,
        MODIFIED,
        DELETED
    }
}
