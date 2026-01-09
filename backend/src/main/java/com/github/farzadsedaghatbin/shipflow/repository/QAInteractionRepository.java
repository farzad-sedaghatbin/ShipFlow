package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.QAInteraction;
import com.github.farzadsedaghatbin.shipflow.entity.enums.QAFeedbackType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QAInteractionRepository extends JpaRepository<QAInteraction, Long> {

    /**
     * Find interactions by user ID.
     */
    Page<QAInteraction> findByUserId(Long userId, Pageable pageable);

    /**
     * Find interactions by context.
     */
    List<QAInteraction> findByContextTypeAndContextId(String contextType, Long contextId);

    /**
     * Find interactions by cycle ID.
     */
    List<QAInteraction> findByCycleId(Long cycleId);

    /**
     * Find validated interactions that haven't been embedded yet.
     */
    @Query("SELECT q FROM QAInteraction q WHERE q.feedbackType IN :types AND q.isEmbedded = false")
    List<QAInteraction> findValidatedNotEmbedded(@Param("types") List<QAFeedbackType> types);

    /**
     * Count validated interactions.
     */
    @Query("SELECT COUNT(q) FROM QAInteraction q WHERE q.feedbackType IN :types")
    long countValidated(@Param("types") List<QAFeedbackType> types);

    /**
     * Find recent interactions by user.
     */
    List<QAInteraction> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find interactions with feedback.
     */
    List<QAInteraction> findByFeedbackTypeIsNotNull();
}
