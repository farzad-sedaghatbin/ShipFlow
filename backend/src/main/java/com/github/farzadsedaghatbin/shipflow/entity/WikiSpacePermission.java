package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.WikiGranteeType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.WikiPermissionLevel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wiki_space_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WikiSpacePermission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "space_id", nullable = false)
  private Long spaceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "grantee_type", nullable = false, length = 8)
  private WikiGranteeType granteeType;

  @Column(name = "grantee_ref", nullable = false, length = 255)
  private String granteeRef;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 8)
  private WikiPermissionLevel level;
}
