package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.ApiKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

  /**
   * Looks up a key by its hash, eagerly fetching the owning {@link
   * com.github.farzadsedaghatbin.shipflow.entity.User}.
   *
   * <p>The {@code user} association is {@code LAZY}, but {@code McpAuthFilter} reads
   * {@code apiKey.getUser().getUsername()} after the validating transaction has closed. Without the
   * {@code JOIN FETCH} that access throws {@link org.hibernate.LazyInitializationException}, which
   * 500s every authenticated MCP request. Fetching the user here keeps it initialized.
   */
  @Query("SELECT ak FROM ApiKey ak JOIN FETCH ak.user WHERE ak.keyHash = :keyHash")
  Optional<ApiKey> findByKeyHash(@Param("keyHash") String keyHash);

  List<ApiKey> findByUserIdAndIsActiveTrue(Long userId);

  List<ApiKey> findByUserId(Long userId);

  @Query("SELECT ak FROM ApiKey ak JOIN FETCH ak.user ORDER BY ak.createdAt DESC")
  List<ApiKey> findAllWithUsers();

  boolean existsByKeyHash(String keyHash);
}
