package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  // ── Username lookups are case-insensitive ──────────────────────────────────
  // "Hooman" and "hooman" are the same person (phones and browsers capitalise the first
  // letter). An exact match always wins, so an install that already holds two accounts
  // differing only by case keeps resolving each of them correctly; otherwise the
  // case-insensitive match is used only when it is unambiguous. The stored username stays
  // the canonical spelling — the token and every downstream lookup use it, not what was typed.

  @Query("SELECT u FROM User u WHERE u.username = :username")
  Optional<User> findByUsernameExact(@Param("username") String username);

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.person WHERE u.username = :username")
  Optional<User> findByUsernameWithPersonExact(@Param("username") String username);

  @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:username)")
  List<User> findAllByUsernameIgnoreCase(@Param("username") String username);

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.person WHERE LOWER(u.username) = LOWER(:username)")
  List<User> findAllByUsernameIgnoreCaseWithPerson(@Param("username") String username);

  default Optional<User> findByUsername(String username) {
    if (username == null) {
      return Optional.empty();
    }
    Optional<User> exact = findByUsernameExact(username);
    if (exact.isPresent()) {
      return exact;
    }
    return onlyOne(findAllByUsernameIgnoreCase(username));
  }

  default Optional<User> findByUsernameWithPerson(String username) {
    if (username == null) {
      return Optional.empty();
    }
    Optional<User> exact = findByUsernameWithPersonExact(username);
    if (exact.isPresent()) {
      return exact;
    }
    return onlyOne(findAllByUsernameIgnoreCaseWithPerson(username));
  }

  private static Optional<User> onlyOne(List<User> matches) {
    return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
  }

  Optional<User> findByEmail(String email);

  /** True when any account has this username, ignoring case — so "Hooman" cannot be registered next to "hooman". */
  @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u.username) = LOWER(:username)")
  boolean existsByUsername(@Param("username") String username);

  boolean existsByEmail(String email);

  @Query("SELECT u FROM User u WHERE u.person.id = :personId")
  Optional<User> findByPersonId(@Param("personId") Long personId);

  List<User> findByIsActiveTrue();

  List<User> findByDeletedAtIsNull();

  /** Active, non-deleted user count — the number that counts against the licence seat cap. */
  long countByIsActiveTrueAndDeletedAtIsNull();

  @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
  List<User> findByRoleAndActive(@Param("role") com.github.farzadsedaghatbin.shipflow.entity.UserRole role);

  /**
   * Search for users by person name pattern for @mentions. Returns active users
   * whose person name contains the search query (case-insensitive).
   */
  @Query("SELECT u FROM User u LEFT JOIN FETCH u.person p " + "WHERE u.isActive = true "
      + "AND p IS NOT NULL AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " + "ORDER BY p.name ASC")
  List<User> searchByPersonNameForMention(@Param("query") String query);

  /**
   * Find multiple users by their usernames.
   */
  @Query("SELECT u FROM User u WHERE u.username IN :usernames AND u.isActive = true")
  List<User> findByUsernameIn(@Param("usernames") List<String> usernames);

  /**
   * Find multiple users by their person names (for @mention notification lookup).
   */
  @Query("SELECT u FROM User u LEFT JOIN FETCH u.person p WHERE u.isActive = true AND p IS NOT NULL AND p.name IN :names")
  List<User> findByPersonNameIn(@Param("names") List<String> names);

  /**
   * Find a user by their external IdP subject/sub claim and IdP id.
   * Used during SSO callback to match an existing SSO-provisioned account.
   */
  Optional<User> findByExternalUserIdAndIdentityProviderId(
      String externalUserId, Long identityProviderId);
}
