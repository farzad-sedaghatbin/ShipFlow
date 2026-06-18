import { describe, it, expect } from 'vitest';
import type { WikiTreeNodeDTO } from '../../../services/wikiService';

// ── Pure helper extracted from WikiPage (same algorithm) ─────────────────────

function findAncestorNodes(
  tree: WikiTreeNodeDTO[],
  targetId: number
): WikiTreeNodeDTO[] {
  function walk(
    nodes: WikiTreeNodeDTO[],
    path: WikiTreeNodeDTO[]
  ): WikiTreeNodeDTO[] | null {
    for (const node of nodes) {
      const current = [...path, node];
      if (node.id === targetId) return current;
      if (node.children.length > 0) {
        const found = walk(node.children, current);
        if (found) return found;
      }
    }
    return null;
  }

  const fullPath = walk(tree, []);
  if (!fullPath || fullPath.length <= 1) return [];
  return fullPath.slice(0, -1);
}

// ── Fixtures ──────────────────────────────────────────────────────────────────

const grandchildNode: WikiTreeNodeDTO = {
  id: 30,
  title: 'Grandchild',
  slug: 'grandchild',
  position: 0,
  children: [],
};

const childNode: WikiTreeNodeDTO = {
  id: 20,
  title: 'Child',
  slug: 'child',
  position: 0,
  children: [grandchildNode],
};

const rootNode: WikiTreeNodeDTO = {
  id: 10,
  title: 'Root',
  slug: 'root',
  position: 0,
  children: [childNode],
};

const tree: WikiTreeNodeDTO[] = [rootNode];

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('findAncestors', () => {
  it('returns [] for a root-level page (no ancestors)', () => {
    expect(findAncestorNodes(tree, 10)).toEqual([]);
  });

  it('returns [root] for a first-level child', () => {
    const ancestors = findAncestorNodes(tree, 20);
    expect(ancestors).toHaveLength(1);
    expect(ancestors[0].id).toBe(10);
    expect(ancestors[0].title).toBe('Root');
  });

  it('returns [root, child] in order for a grandchild (3-level → 2 ancestors)', () => {
    const ancestors = findAncestorNodes(tree, 30);
    expect(ancestors).toHaveLength(2);
    expect(ancestors[0].id).toBe(10);
    expect(ancestors[0].title).toBe('Root');
    expect(ancestors[1].id).toBe(20);
    expect(ancestors[1].title).toBe('Child');
  });

  it('returns [] when the page is not in the tree', () => {
    expect(findAncestorNodes(tree, 999)).toEqual([]);
  });

  it('returns [] for an empty tree', () => {
    expect(findAncestorNodes([], 10)).toEqual([]);
  });
});
