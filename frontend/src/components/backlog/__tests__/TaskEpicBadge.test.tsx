import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { TaskEpicBadge } from '../TaskEpicBadge';
import { parseEpicParam } from '../../../hooks/useBacklogPage';

describe('TaskEpicBadge', () => {
  it('shows the epic name as a link to the epic', () => {
    render(
      <MemoryRouter>
        <TaskEpicBadge task={{ epicId: 12, epicName: 'Onboarding' }} />
      </MemoryRouter>,
    );
    const badge = screen.getByTestId('task-epic-badge');
    expect(badge.textContent).toContain('Onboarding');
    expect(badge.getAttribute('href')).toBe('/epics/12');
  });

  it('falls back to the epic id when the name is missing', () => {
    render(
      <MemoryRouter>
        <TaskEpicBadge task={{ epicId: 5 }} />
      </MemoryRouter>,
    );
    expect(screen.getByTestId('task-epic-badge').textContent).toContain('#5');
  });

  it('renders nothing for a task without an epic', () => {
    const { container } = render(
      <MemoryRouter>
        <TaskEpicBadge task={{}} />
      </MemoryRouter>,
    );
    expect(container.firstChild).toBeNull();
  });
});

describe('parseEpicParam', () => {
  it('reads an epic id, "none", and treats anything else as no filter', () => {
    expect(parseEpicParam('7')).toBe(7);
    expect(parseEpicParam('none')).toBe('none');
    for (const v of [null, '', 'abc', '0', '-3', '1.5']) expect(parseEpicParam(v)).toBe('all');
  });
});
