import { describe, it, expect, vi, afterEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type * as ReactRouterDom from 'react-router-dom';
import React from 'react';
import { useBackNavigation } from './useBackNavigation';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof ReactRouterDom>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('useBackNavigation', () => {
  afterEach(() => {
    mockNavigate.mockClear();
  });

  it('navigates to location.state.from when present', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <MemoryRouter
        initialEntries={[{ pathname: '/backlog/1', state: { from: '/backlog?status=open' } }]}
      >
        {children}
      </MemoryRouter>
    );

    const { result } = renderHook(() => useBackNavigation('/backlog'), { wrapper });
    result.current();

    expect(mockNavigate).toHaveBeenCalledWith('/backlog?status=open');
  });

  it('falls back to the given path when location.state.from is absent', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <MemoryRouter initialEntries={['/backlog/1']}>{children}</MemoryRouter>
    );

    const { result } = renderHook(() => useBackNavigation('/backlog'), { wrapper });
    result.current();

    expect(mockNavigate).toHaveBeenCalledWith('/backlog');
  });
});
