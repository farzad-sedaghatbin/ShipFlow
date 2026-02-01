package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Permission;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

  /** Find all permissions for a specific role */
  List<Permission> findByRole(UserRole role);

  /** Find all permissions for a specific resource type */
  List<Permission> findByResourceType(ResourceType resourceType);

  /** Check if a permission exists for a role, resource, and permission type */
  Optional<Permission> findByRoleAndResourceTypeAndPermissionType(
      UserRole role, ResourceType resourceType, PermissionType permissionType);

  /** Check if a specific permission exists */
  boolean existsByRoleAndResourceTypeAndPermissionType(
      UserRole role, ResourceType resourceType, PermissionType permissionType);

  /** Get all permissions for a role and resource combination */
  @Query("SELECT p FROM Permission p WHERE p.role = :role AND p.resourceType = :resourceType")
  List<Permission> findPermissionsForRoleAndResource(
      @Param("role") UserRole role, @Param("resourceType") ResourceType resourceType);
}
