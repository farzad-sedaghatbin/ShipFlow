import { describe, it, expect } from 'vitest';
import type { AxiosError } from 'axios';
import { getConflictBody } from './conflictError';

function fakeAxiosError(status: number, data: unknown): AxiosError {
  return {
    isAxiosError: true,
    name: 'AxiosError',
    message: 'Request failed',
    toJSON: () => ({}),
    response: {
      status,
      statusText: '',
      data,
      headers: {},
      config: {} as never,
    },
  } as unknown as AxiosError;
}

describe('getConflictBody', () => {
  it('returns the conflict body for a well-formed 409 optimistic-lock response', () => {
    const body = {
      entityType: 'PITCH' as const,
      entityId: 42,
      currentVersion: 7,
      current: { id: 42, title: 'Server version' },
    };
    const error = fakeAxiosError(409, body);

    expect(getConflictBody(error)).toEqual(body);
  });

  it('returns null for a 409 that is not an optimistic-lock conflict (e.g. plain duplicate-data)', () => {
    const error = fakeAxiosError(409, { message: 'Duplicate key' });

    expect(getConflictBody(error)).toBeNull();
  });

  it('returns null for a non-409 error', () => {
    const error = fakeAxiosError(400, { message: 'Bad request' });

    expect(getConflictBody(error)).toBeNull();
  });

  it('returns null for a non-axios error (e.g. a plain network failure)', () => {
    expect(getConflictBody(new Error('network down'))).toBeNull();
  });

  it('returns null for undefined/null input', () => {
    expect(getConflictBody(undefined)).toBeNull();
    expect(getConflictBody(null)).toBeNull();
  });
});
