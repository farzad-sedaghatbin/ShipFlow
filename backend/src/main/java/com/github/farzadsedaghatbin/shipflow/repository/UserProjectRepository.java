package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.UserProject;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectRole;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProjectRepository extends JpaRepository<UserProject, Long> {

  /** Find direct project assignment for a user-project pair */
  Optional<UserProject> findByUserIdAndProjectId(Long userId, Long projectId);

  /** Check if user has direct project access */
  boolean existsByUserIdAndProjectId(Long userId, Long projectId);

  /** Get all direct project assignments for a user */
  List<UserProject> findByUserId(Long userId);

  /** Get all users with direct access to a project */
  List<UserProject> findByProjectId(Long projectId);

  /** Get all users with a specific role in a project */
  List<UserProject> findByProjectIdAndProjectRole(Long projectId, ProjectRole role);

  /** Get project IDs that user has direct access to */
  @Query("SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId")
  Set<Long> findProjectIdsByUserId(@Param("userId") Long userId);

  /**
   * Find all projects accessible to a user through ANY access path: 1. Direct
   * assignment via user_projects 2. Project ownership 3. Team membership (via
   * team_assignments → teams → cycles → projects)
   *
   * <p>
   * Note: ADMIN role bypass is handled in service layer
   */
  @Query("""
      SELECT DISTINCT p FROM Project p
      WHERE p.isActive = true
      AND (
          p.owner.id = :userId
          OR p.id IN (SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId)
          OR p.id IN (
              SELECT DISTINCT c.project.id
              FROM Cycle c
              JOIN c.teams t
              JOIN TeamAssignment ta ON ta.team.id = t.id
              JOIN ta.person per
              JOIN User u ON u.person.id = per.id
              WHERE u.id = :userId
          )
      )
      ORDER BY p.name
      """)
  List<Project> findAccessibleProjectsByUserId(@Param("userId") Long userId);

  /** Find all active projects accessible to a user */
  @Query("""
      SELECT DISTINCT p FROM Project p
      WHERE p.isActive = true
      AND (
          p.owner.id = :userId
          OR p.id IN (SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId)
          OR p.id IN (
              SELECT DISTINCT c.project.id
              FROM Cycle c
              JOIN c.teams t
              JOIN TeamAssignment ta ON ta.team.id = t.id
              JOIN ta.person per
              JOIN User u ON u.person.id = per.id
              WHERE u.id = :userId
          )
      )
      ORDER BY p.name
      """)
  List<Project> findActiveAccessibleProjectsByUserId(@Param("userId") Long userId);

  /** Check if user has access to a specific project through any path */
  @Query("""
      SELECT COUNT(p) > 0 FROM Project p
      WHERE p.id = :projectId
      AND (
          p.owner.id = :userId
          OR p.id IN (SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId)
          OR p.id IN (
              SELECT DISTINCT c.project.id
              FROM Cycle c
              JOIN c.teams t
              JOIN TeamAssignment ta ON ta.team.id = t.id
              JOIN ta.person per
              JOIN User u ON u.person.id = per.id
              WHERE u.id = :userId
          )
      )
      """)
  boolean hasProjectAccess(@Param("userId") Long userId, @Param("projectId") Long projectId);

  /**
   * Get the effective role a user has in a project Returns the highest privilege
   * role from direct assignment
   */
  @Query("SELECT up.projectRole FROM UserProject up WHERE up.user.id = :userId AND up.project.id = :projectId")
  Optional<ProjectRole> findProjectRoleByUserIdAndProjectId(@Param("userId") Long userId,
      @Param("projectId") Long projectId);

  /** Delete all project assignments for a user */
  void deleteByUserId(Long userId);

  /** Delete all user assignments for a project */
  void deleteByProjectId(Long projectId);

  /** Delete specific user-project assignment */
  void deleteByUserIdAndProjectId(Long userId, Long projectId);
}
