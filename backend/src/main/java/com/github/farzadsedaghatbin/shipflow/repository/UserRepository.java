package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
}
