import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { PluginBlockRenderer } from '../PluginBlockRenderer';
import type { PluginBlock } from '@/services/pluginService';

const renderBlock = (block: PluginBlock) =>
  render(
    <MemoryRouter>
      <PluginBlockRenderer block={block} />
    </MemoryRouter>,
  );

describe('PluginBlockRenderer', () => {
  it('renders markdown but never executes embedded script or javascript: links', () => {
    const { container } = renderBlock({
      type: 'MARKDOWN',
      data: { text: '## Title\n\n<script>window.__pwned = true</script>\n\n[x](javascript:alert(1))' },
    });

    expect(screen.getByRole('heading', { name: 'Title' })).toBeInTheDocument();
    expect(container.querySelector('script')).toBeNull();
    expect((window as unknown as { __pwned?: boolean }).__pwned).toBeUndefined();
    const anchor = container.querySelector('a');
    expect(anchor?.getAttribute('href') ?? '').not.toMatch(/^javascript:/i);
  });

  it('renders stat cards with optional hint', () => {
    renderBlock({
      type: 'STAT_CARDS',
      data: { stats: [{ label: 'Open bugs', value: '12', hint: 'since Monday' }, { label: 'Done', value: '40' }] },
    });

    expect(screen.getByText('Open bugs')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText('since Monday')).toBeInTheDocument();
    expect(screen.getByText('Done')).toBeInTheDocument();
  });

  it('renders a table with headers and rows', () => {
    renderBlock({
      type: 'TABLE',
      data: { columns: ['Name', 'Owner'], rows: [['Alpha', 'Mia'], ['Beta', 'Sam']] },
    });

    expect(screen.getByRole('columnheader', { name: 'Owner' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: 'Beta' })).toBeInTheDocument();
    expect(screen.getAllByRole('row')).toHaveLength(3);
  });

  it('uses an in-app link for paths and a new-tab link for https targets', () => {
    renderBlock({
      type: 'LINK_LIST',
      data: {
        links: [
          { label: 'Backlog', target: '/backlog' },
          { label: 'Docs', target: 'https://example.com/docs' },
        ],
      },
    });

    expect(screen.getByRole('link', { name: 'Backlog' })).toHaveAttribute('href', '/backlog');
    const external = screen.getByRole('link', { name: /Docs/ });
    expect(external).toHaveAttribute('href', 'https://example.com/docs');
    expect(external).toHaveAttribute('target', '_blank');
    expect(external).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('renders an alert', () => {
    renderBlock({ type: 'ALERT', data: { level: 'warning', text: 'Heads up' } });

    expect(screen.getByRole('alert')).toHaveTextContent('Heads up');
  });
});
