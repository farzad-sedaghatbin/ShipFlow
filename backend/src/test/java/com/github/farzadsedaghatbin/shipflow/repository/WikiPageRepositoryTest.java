package com.github.farzadsedaghatbin.shipflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.WikiPage;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpace;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpacePermission;
import com.github.farzadsedaghatbin.shipflow.entity.enums.WikiGranteeType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.WikiPermissionLevel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class WikiPageRepositoryTest {

  @Autowired private WikiSpaceRepository wikiSpaceRepository;
  @Autowired private WikiPageRepository wikiPageRepository;
  @Autowired private WikiSpacePermissionRepository wikiSpacePermissionRepository;

  @Test
  void findChildren_orderedByPosition() {
    WikiSpace space =
        wikiSpaceRepository.save(
            WikiSpace.builder()
                .name("Engineering Docs")
                .spaceKey("eng")
                .description("Engineering wiki")
                .createdBy(1L)
                .build());

    WikiPage parent =
        wikiPageRepository.save(
            WikiPage.builder()
                .spaceId(space.getId())
                .title("Parent Page")
                .slug("parent-page")
                .position(0)
                .createdBy(1L)
                .build());

    WikiPage child1 =
        wikiPageRepository.save(
            WikiPage.builder()
                .spaceId(space.getId())
                .parentId(parent.getId())
                .title("Child A")
                .slug("child-a")
                .position(0)
                .createdBy(1L)
                .build());

    WikiPage child2 =
        wikiPageRepository.save(
            WikiPage.builder()
                .spaceId(space.getId())
                .parentId(parent.getId())
                .title("Child B")
                .slug("child-b")
                .position(1)
                .createdBy(1L)
                .build());

    List<WikiPage> children =
        wikiPageRepository.findBySpaceIdAndParentIdAndDeletedAtIsNullOrderByPositionAsc(
            space.getId(), parent.getId());

    assertThat(children).hasSize(2);
    assertThat(children.get(0).getId()).isEqualTo(child1.getId());
    assertThat(children.get(1).getId()).isEqualTo(child2.getId());
  }

  @Test
  void wikiSpacePermission_persistAndRead() {
    WikiSpace space =
        wikiSpaceRepository.save(
            WikiSpace.builder()
                .name("Product Docs")
                .spaceKey("product")
                .createdBy(1L)
                .build());

    WikiSpacePermission perm =
        wikiSpacePermissionRepository.save(
            WikiSpacePermission.builder()
                .spaceId(space.getId())
                .granteeType(WikiGranteeType.ROLE)
                .granteeRef("MEMBER")
                .level(WikiPermissionLevel.READ)
                .build());

    List<WikiSpacePermission> perms =
        wikiSpacePermissionRepository.findBySpaceId(space.getId());

    assertThat(perms).hasSize(1);
    assertThat(perms.get(0).getId()).isEqualTo(perm.getId());
    assertThat(perms.get(0).getLevel()).isEqualTo(WikiPermissionLevel.READ);
  }
}
