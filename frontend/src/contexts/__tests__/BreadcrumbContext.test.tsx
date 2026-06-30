import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import type { ReactNode } from 'react';
import {
  BreadcrumbProvider,
  useBreadcrumbLabel,
  useBreadcrumbLabels,
} from '../BreadcrumbContext';

function wrapper({ children }: { children: ReactNode }) {
  return <BreadcrumbProvider>{children}</BreadcrumbProvider>;
}

describe('BreadcrumbContext', () => {
  it('registers a label for a path while mounted', () => {
    const { result } = renderHook(
      () => {
        useBreadcrumbLabel('/cycles/12', 'Q3 Payments Cycle');
        return useBreadcrumbLabels();
      },
      { wrapper },
    );
    expect(result.current['/cycles/12']).toBe('Q3 Payments Cycle');
  });

  it('clears the label when it becomes null (entity unloading)', () => {
    const { result, rerender } = renderHook(
      ({ label }: { label: string | null }) => {
        useBreadcrumbLabel('/wiki/1', label);
        return useBreadcrumbLabels();
      },
      { wrapper, initialProps: { label: 'Engineering' as string | null } },
    );
    expect(result.current['/wiki/1']).toBe('Engineering');

    rerender({ label: null });
    expect(result.current['/wiki/1']).toBeUndefined();
  });

  it('no-ops without a path or label (still loading)', () => {
    const { result } = renderHook(
      () => {
        useBreadcrumbLabel(undefined, 'ignored');
        useBreadcrumbLabel('/pitches/9', null);
        return useBreadcrumbLabels();
      },
      { wrapper },
    );
    expect(result.current['/pitches/9']).toBeUndefined();
    expect(Object.keys(result.current)).toHaveLength(0);
  });

  it('returns an empty map when no provider is mounted', () => {
    const { result } = renderHook(() => useBreadcrumbLabels());
    expect(result.current).toEqual({});
  });

  it('updates the label when it changes', () => {
    const { result, rerender } = renderHook(
      ({ label }: { label: string }) => {
        useBreadcrumbLabel('/pitches/3', label);
        return useBreadcrumbLabels();
      },
      { wrapper, initialProps: { label: 'Draft pitch' } },
    );
    expect(result.current['/pitches/3']).toBe('Draft pitch');
    act(() => rerender({ label: 'Shaped pitch' }));
    expect(result.current['/pitches/3']).toBe('Shaped pitch');
  });
});
