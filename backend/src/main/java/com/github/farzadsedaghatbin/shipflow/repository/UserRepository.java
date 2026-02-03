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

  Optional<User> findByUsername(String username);

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.person WHERE u.username = :username")
  Optional<User> findByUsernameWithPerson(@Param("username") String username);

  Optional<User> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  @Query("SELECT u FROM User u WHERE u.person.id = :personId")
  Optional<User> findByPersonId(@Param("personId") Long personId);

  List<User> findByIsActiveTrue();

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
  @Query("SELECT u FROM User u JOIN FETCH u.person p WHERE u.isActive = true AND p.name IN :names")
  List<User> findByPersonNameIn(@Param("names") List<String> names);
}
