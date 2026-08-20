import { describe, it, expect, beforeEach } from 'vitest';
import {
  buildLoginPath,
  clearPendingRedirect,
  consumeRedirect,
  DEFAULT_POST_LOGIN_PATH,
  isOnAuthPage,
  peekRedirect,
  rememberRedirect,
  resolvePostLoginTarget,
  sanitizeRedirect,
  toRelativePath,
} from '../lib/redirect';

describe('sanitizeRedirect', () => {
  it('accepts an internal path with query and hash', () => {
    expect(sanitizeRedirect('/backlog/42?tab=scopes#comments')).toBe(
      '/backlog/42?tab=scopes#comments'
    );
  });

  it('rejects absolute and scheme-relative URLs', () => {
    expect(sanitizeRedirect('https://evil.com/steal')).toBeNull();
    expect(sanitizeRedirect('//evil.com')).toBeNull();
    expect(sanitizeRedirect('/\\evil.com')).toBeNull();
    expect(sanitizeRedirect('javascript:alert(1)')).toBeNull();
  });

  it('rejects values with whitespace or control characters', () => {
    expect(sanitizeRedirect('/back log')).toBeNull();
    expect(sanitizeRedirect('/backlog\n/evil')).toBeNull();
  });

  it('rejects the auth routes so login cannot loop back to itself', () => {
    expect(sanitizeRedirect('/login')).toBeNull();
    expect(sanitizeRedirect('/login?redirect=/dashboard')).toBeNull();
    expect(sanitizeRedirect('/sso-callback?token=abc')).toBeNull();
  });

  it('rejects the bare landing page and empty input', () => {
    expect(sanitizeRedirect('/')).toBeNull();
    expect(sanitizeRedirect('')).toBeNull();
    expect(sanitizeRedirect(null)).toBeNull();
    expect(sanitizeRedirect(undefined)).toBeNull();
  });
});

describe('buildLoginPath', () => {
  it('encodes the intended destination as a query parameter', () => {
    expect(buildLoginPath('/backlog/42?tab=scopes')).toBe(
      '/login?redirect=%2Fbacklog%2F42%3Ftab%3Dscopes'
    );
  });

  it('falls back to a bare login path for unusable targets', () => {
    expect(buildLoginPath('https://evil.com')).toBe('/login');
    expect(buildLoginPath('/login')).toBe('/login');
    expect(buildLoginPath(null)).toBe('/login');
  });
});

describe('toRelativePath', () => {
  it('joins pathname, search and hash', () => {
    expect(toRelativePath({ pathname: '/cycles/7', search: '?view=board', hash: '#top' })).toBe(
      '/cycles/7?view=board#top'
    );
  });

  it('tolerates a missing search and hash', () => {
    expect(toRelativePath({ pathname: '/projects' })).toBe('/projects');
  });
});

describe('isOnAuthPage', () => {
  it('recognises the login and SSO callback routes', () => {
    expect(isOnAuthPage('/login')).toBe(true);
    expect(isOnAuthPage('/sso-callback')).toBe(true);
    expect(isOnAuthPage('/dashboard')).toBe(false);
    expect(isOnAuthPage('/loginish')).toBe(false);
  });
});

describe('pending redirect storage', () => {
  beforeEach(() => {
    clearPendingRedirect();
  });

  it('stashes and reads back a sanitised destination', () => {
    rememberRedirect('/wiki/3/12');
    expect(peekRedirect()).toBe('/wiki/3/12');
    expect(consumeRedirect()).toBe('/wiki/3/12');
    // consume clears it
    expect(peekRedirect()).toBeNull();
  });

  it('does not stash an unsafe destination', () => {
    rememberRedirect('https://evil.com');
    expect(peekRedirect()).toBeNull();
  });

  it('clears a previous value when handed an unusable one', () => {
    rememberRedirect('/dashboard');
    rememberRedirect(null);
    expect(peekRedirect()).toBeNull();
  });
});

describe('resolvePostLoginTarget', () => {
  beforeEach(() => {
    clearPendingRedirect();
  });

  it('prefers router state, keeping query and hash', () => {
    const target = resolvePostLoginTarget({
      state: { from: { pathname: '/backlog', search: '?assignee=me', hash: '' } },
      search: '?redirect=%2Fdashboard',
    });
    expect(target).toBe('/backlog?assignee=me');
  });

  it('falls back to the redirect query parameter', () => {
    expect(resolvePostLoginTarget({ search: '?redirect=%2Fcycles%2F7' })).toBe('/cycles/7');
  });

  it('falls back to the stashed SSO destination', () => {
    rememberRedirect('/qa/bug-reports/9');
    expect(resolvePostLoginTarget({ search: '' })).toBe('/qa/bug-reports/9');
  });

  it('leaves the stashed destination in place (read-only)', () => {
    rememberRedirect('/qa/bug-reports/9');
    resolvePostLoginTarget({ search: '' });
    expect(peekRedirect()).toBe('/qa/bug-reports/9');
  });

  it('defaults to the dashboard when nothing was recorded', () => {
    expect(resolvePostLoginTarget({})).toBe(DEFAULT_POST_LOGIN_PATH);
  });

  it('ignores an off-site target in router state and in the query string', () => {
    expect(
      resolvePostLoginTarget({
        state: { from: { pathname: '//evil.com' } },
        search: '?redirect=https%3A%2F%2Fevil.com',
      })
    ).toBe(DEFAULT_POST_LOGIN_PATH);
  });
});
