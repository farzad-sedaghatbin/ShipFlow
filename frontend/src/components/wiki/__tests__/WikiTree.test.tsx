import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import WikiTree from '../../../components/wiki/WikiTree';
import type { WikiTreeNodeDTO } from '../../../services/wikiService';

// ── Module mocks ──────────────────────────────────────────────────────────────

const mockNavigate = vi.fn();
vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

const mockMutate = vi.fn();
vi.mock('@tanstack/react-query', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-query')>();
  return {
    ...actual,
    useMutation: () => ({
      mutate: mockMutate,
      isPending: false,
    }),
    useQueryClient: () => ({
      invalidateQueries: vi.fn(),
    }),
  };
});

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}));

// ── Fixtures ──────────────────────────────────────────────────────────────────

const childNode: WikiTreeNodeDTO = {
  id: 2,
  title: 'Child Page',
  slug: 'child-page',
  position: 0,
  children: [],
};

const rootNode: WikiTreeNodeDTO = {
  id: 1,
  title: 'Root Page',
  slug: 'root-page',
  position: 0,
  children: [childNode],
};

const nodes: WikiTreeNodeDTO[] = [rootNode];

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('WikiTree', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders both nodes with correct titles', () => {
    render(<WikiTree spaceId={10} nodes={nodes} />);

    expect(screen.getByTestId('wiki-tree-node-1')).toBeTruthy();
    expect(screen.getByTestId('wiki-tree-node-2')).toBeTruthy();

    expect(screen.getByText('Root Page')).toBeTruthy();
    expect(screen.getByText('Child Page')).toBeTruthy();
  });

  it('calls navigate with the correct path when a node is clicked', () => {
    render(<WikiTree spaceId={10} nodes={nodes} />);

    // Click the root node title text
    fireEvent.click(screen.getByText('Root Page'));

    expect(mockNavigate).toHaveBeenCalledWith('/wiki/10/1');
  });

  it('calls onNavigate callback when a node is clicked', () => {
    const onNavigate = vi.fn();
    render(<WikiTree spaceId={10} nodes={nodes} onNavigate={onNavigate} />);

    fireEvent.click(screen.getByText('Root Page'));

    expect(onNavigate).toHaveBeenCalledWith(1);
  });

  it('calls the move mutation on drag-drop', () => {
    render(<WikiTree spaceId={10} nodes={nodes} />);

    const dragSource = screen.getByTestId('wiki-tree-node-2'); // child
    const dropTarget = screen.getByTestId('wiki-tree-node-1'); // root

    // Simulate drag-start on child carrying its id
    fireEvent.dragStart(dragSource, {
      dataTransfer: {
        effectAllowed: '',
        setData: vi.fn(),
        getData: () => '2',
      },
    });

    // Simulate drop onto root — fires the mutation
    fireEvent.drop(dropTarget, {
      dataTransfer: {
        dropEffect: '',
        getData: () => '2',
      },
    });

    expect(mockMutate).toHaveBeenCalledWith(
      expect.objectContaining({
        pageId: 2,
        newParentId: 1,
        newIndex: 0,
      })
    );
  });

  it('renders a dash when nodes array is empty', () => {
    render(<WikiTree spaceId={10} nodes={[]} />);
    expect(screen.getByText('—')).toBeTruthy();
  });
});
