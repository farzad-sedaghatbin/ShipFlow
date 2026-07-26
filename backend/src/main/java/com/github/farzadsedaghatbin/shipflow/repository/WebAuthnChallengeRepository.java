package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.ChallengePurpose;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.WebAuthnChallenge;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WebAuthnChallengeRepository extends JpaRepository<WebAuthnChallenge, Long> {

  Optional<WebAuthnChallenge> findByChallenge(String challenge);

  // Returns a List (not Optional<WebAuthnChallenge>/a single entity) on purpose: a user can start
  // (but not finish) a registration/login ceremony more than once before the first challenge
  // expires — e.g. cancelling the browser's biometric prompt and clicking "Add a passkey" again —
  // leaving 2+ valid, unused, unexpired rows for the same user+purpose. A method returning a
  // single Optional/entity from a query that can match more than one row throws
  // IncorrectResultSizeDataAccessException at runtime; callers take the first (newest, per the
  // ORDER BY) entry themselves instead.
  @Query("SELECT c FROM WebAuthnChallenge c WHERE c.user = :user AND c.purpose = :purpose "
      + "AND c.used = false AND c.expiryDate > :now ORDER BY c.createdAt DESC")
  List<WebAuthnChallenge> findValidChallengesByUserAndPurpose(
      @Param("user") User user, @Param("purpose") ChallengePurpose purpose, @Param("now") LocalDateTime now);

  // Same multi-row rationale as findValidChallengesByUserAndPurpose above.
  @Query("SELECT c FROM WebAuthnChallenge c WHERE c.usernameHint = :usernameHint AND c.purpose = :purpose "
      + "AND c.used = false AND c.expiryDate > :now ORDER BY c.createdAt DESC")
  List<WebAuthnChallenge> findValidChallengesByUsernameHintAndPurpose(
      @Param("usernameHint") String usernameHint, @Param("purpose") ChallengePurpose purpose,
      @Param("now") LocalDateTime now);

  @Modifying
  @Query("DELETE FROM WebAuthnChallenge c WHERE c.expiryDate < :now")
  void deleteExpiredChallenges(@Param("now") LocalDateTime now);
}
