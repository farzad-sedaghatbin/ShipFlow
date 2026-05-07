package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.CustomDashboard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomDashboardRepository extends JpaRepository<CustomDashboard, Long> {

  /** Find dashboard by ID with scope relationships eagerly loaded */
  @Query("SELECT d FROM CustomDashboard d " + "LEFT JOIN FETCH d.cycle " + "LEFT JOIN FETCH d.pitch "
      + "LEFT JOIN FETCH d.team " + "WHERE d.id = :id")
  Optional<CustomDashboard> findById(@Param("id") Long id);

  /**
   * Find all user-created dashboards (excluding templates) with scope
   * relationships eagerly loaded
   */
  @Query("SELECT DISTINCT d FROM CustomDashboard d " + "LEFT JOIN FETCH d.cycle " + "LEFT JOIN FETCH d.pitch "
      + "LEFT JOIN FETCH d.team " + "WHERE d.user.id = :userId AND d.isTemplate = false " + "ORDER BY d.name ASC")
  List<CustomDashboard> findByUserIdOrderByNameAsc(@Param("userId") Long userId);

  /**
   * Find user's default dashboard (excluding templates) with scope relationships
   * eagerly loaded
   */
  @Query("SELECT d FROM CustomDashboard d " + "LEFT JOIN FETCH d.cycle " + "LEFT JOIN FETCH d.pitch "
      + "LEFT JOIN FETCH d.team " + "WHERE d.user.id = :userId AND d.isDefault = true AND d.isTemplate = false")
  Optional<CustomDashboard> findByUserIdAndIsDefaultTrue(@Param("userId") Long userId);

  /** Find all system templates with scope relationships eagerly loaded */
  @Query("SELECT DISTINCT d FROM CustomDashboard d " + "LEFT JOIN FETCH d.cycle " + "LEFT JOIN FETCH d.pitch "
      + "LEFT JOIN FETCH d.team " + "WHERE d.isTemplate = :isTemplate " + "ORDER BY d.templateCategory ASC")
  List<CustomDashboard> findByIsTemplateTrueOrderByTemplateCategoryAsc(@Param("isTemplate") Boolean isTemplate);

  /** Alternative method using method query instead of JPQL */
  List<CustomDashboard> findByIsTemplateOrderByTemplateCategoryAsc(Boolean isTemplate);

  /** Find templates by category with scope relationships eagerly loaded */
  @Query("SELECT DISTINCT d FROM CustomDashboard d " + "LEFT JOIN FETCH d.cycle " + "LEFT JOIN FETCH d.pitch "
      + "LEFT JOIN FETCH d.team " + "WHERE d.isTemplate = true AND d.templateCategory = :category")
  List<CustomDashboard> findByIsTemplateTrueAndTemplateCategory(
      @Param("category") CustomDashboard.TemplateCategory category);

  /**
   * Check if dashboard name exists for user (including templates to match DB
   * unique constraint)
   */
  @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END " + "FROM CustomDashboard d "
      + "WHERE d.user.id = :userId AND d.name = :name")
  boolean existsByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);

  /** Set all user's dashboards (excluding templates) to non-default */
  @Modifying
  @Query("UPDATE CustomDashboard d SET d.isDefault = false " + "WHERE d.user.id = :userId AND d.isTemplate = false")
  void clearDefaultDashboards(@Param("userId") Long userId);

  /** Count user-created dashboards (excluding templates) */
  @Query("SELECT COUNT(d) FROM CustomDashboard d " + "WHERE d.user.id = :userId AND d.isTemplate = false")
  long countByUserId(@Param("userId") Long userId);
}
