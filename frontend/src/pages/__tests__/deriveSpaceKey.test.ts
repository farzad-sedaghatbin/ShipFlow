import { describe, it, expect } from 'vitest';
import { deriveSpaceKey } from '../WikiSpaceList';

describe('deriveSpaceKey', () => {
  it('uppercases a simple name', () => {
    expect(deriveSpaceKey('Design')).toBe('DESIGN');
  });

  it('strips spaces and punctuation', () => {
    expect(deriveSpaceKey('Product Docs!')).toBe('PRODUCTDOC');
  });

  it('caps the result at 10 characters', () => {
    expect(deriveSpaceKey('Engineering Handbook')).toHaveLength(10);
    expect(deriveSpaceKey('Engineering Handbook')).toBe('ENGINEERIN');
  });

  it('keeps digits', () => {
    expect(deriveSpaceKey('Q3 2026 Plan')).toBe('Q32026PLAN');
  });

  it('returns empty string for empty / falsy input', () => {
    expect(deriveSpaceKey('')).toBe('');
    // @ts-expect-error guard against undefined at runtime
    expect(deriveSpaceKey(undefined)).toBe('');
  });

  it('returns empty string when name has no alphanumerics', () => {
    expect(deriveSpaceKey('—– …')).toBe('');
  });
});
