import { describe, it, expect, afterEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useSeo, breadcrumbSchema, SITE_URL } from './useSeo';

function meta(keyType: 'name' | 'property', key: string): string | null {
  return document.head.querySelector(`meta[${keyType}="${key}"]`)?.getAttribute('content') ?? null;
}

function canonical(): string | null {
  return document.head.querySelector('link[rel="canonical"]')?.getAttribute('href') ?? null;
}

function jsonLdBlocks(): Record<string, unknown>[] {
  return [...document.head.querySelectorAll('script[data-seo-jsonld="1"]')].map((s) =>
    JSON.parse(s.textContent ?? '{}'),
  );
}

describe('useSeo', () => {
  afterEach(() => {
    document.head.querySelectorAll('[data-seo], [data-seo-jsonld]').forEach((n) => n.remove());
    document.title = '';
  });

  it('sets the document title with the site-name suffix', () => {
    renderHook(() => useSeo({ title: 'Hill Charts', description: 'd', path: '/blog/hc' }));
    expect(document.title).toBe('Hill Charts · ShipFlow');
  });

  it('honours exactTitle for pages that carry their own full title', () => {
    renderHook(() =>
      useSeo({ title: 'ShipFlow — Open Source', description: 'd', path: '/', exactTitle: true }),
    );
    expect(document.title).toBe('ShipFlow — Open Source');
  });

  it('builds an absolute canonical URL from the route path', () => {
    renderHook(() => useSeo({ title: 't', description: 'd', path: '/compare' }));
    expect(canonical()).toBe(`${SITE_URL}/compare`);
  });

  it('keeps the root canonical as a single trailing slash', () => {
    renderHook(() => useSeo({ title: 't', description: 'd', path: '/' }));
    expect(canonical()).toBe(`${SITE_URL}/`);
  });

  it('strips a trailing slash so one page cannot claim two canonical URLs', () => {
    renderHook(() => useSeo({ title: 't', description: 'd', path: '/blog/post/' }));
    expect(canonical()).toBe(`${SITE_URL}/blog/post`);
  });

  it('emits an indexable robots directive by default', () => {
    renderHook(() => useSeo({ title: 't', description: 'd', path: '/x' }));
    expect(meta('name', 'robots')).toContain('index, follow');
  });

  it('emits noindex when the page opts out', () => {
    renderHook(() => useSeo({ title: 't', description: 'd', path: '/login', noindex: true }));
    expect(meta('name', 'robots')).toBe('noindex, nofollow');
  });

  it('writes Open Graph and Twitter tags from the same source', () => {
    renderHook(() => useSeo({ title: 'Betting', description: 'How bets work', path: '/b' }));
    expect(meta('property', 'og:title')).toBe('Betting · ShipFlow');
    expect(meta('property', 'og:description')).toBe('How bets work');
    expect(meta('property', 'og:url')).toBe(`${SITE_URL}/b`);
    expect(meta('name', 'twitter:title')).toBe('Betting · ShipFlow');
    expect(meta('name', 'twitter:card')).toBe('summary_large_image');
  });

  it('adds article tags only for article-type pages', () => {
    const { unmount } = renderHook(() =>
      useSeo({
        title: 'Post',
        description: 'd',
        path: '/blog/p',
        type: 'article',
        publishedTime: '2026-08-16',
        author: 'farzad',
      }),
    );
    expect(meta('property', 'article:published_time')).toBe('2026-08-16');
    expect(meta('property', 'article:author')).toBe('farzad');
    unmount();

    // A website-type page must not inherit the previous article's dates.
    renderHook(() => useSeo({ title: 'Home', description: 'd', path: '/' }));
    expect(meta('property', 'article:published_time')).toBeNull();
    expect(meta('property', 'article:author')).toBeNull();
  });

  it('renders every supplied JSON-LD block', () => {
    renderHook(() =>
      useSeo({
        title: 't',
        description: 'd',
        path: '/x',
        jsonLd: [{ '@type': 'BlogPosting' }, { '@type': 'BreadcrumbList' }],
      }),
    );
    expect(jsonLdBlocks().map((b) => b['@type'])).toEqual(['BlogPosting', 'BreadcrumbList']);
  });

  it('accepts a single JSON-LD object as well as an array', () => {
    renderHook(() =>
      useSeo({ title: 't', description: 'd', path: '/x', jsonLd: { '@type': 'WebSite' } }),
    );
    expect(jsonLdBlocks()).toHaveLength(1);
  });

  it('removes structured data on unmount so it cannot describe the next route', () => {
    const { unmount } = renderHook(() =>
      useSeo({ title: 't', description: 'd', path: '/x', jsonLd: { '@type': 'BlogPosting' } }),
    );
    expect(jsonLdBlocks()).toHaveLength(1);
    unmount();
    expect(jsonLdBlocks()).toHaveLength(0);
  });

  it('drops the keywords tag when a page supplies none', () => {
    const { unmount } = renderHook(() =>
      useSeo({ title: 't', description: 'd', path: '/x', keywords: ['a', 'b'] }),
    );
    expect(meta('name', 'keywords')).toBe('a, b');
    unmount();

    renderHook(() => useSeo({ title: 't', description: 'd', path: '/y' }));
    expect(meta('name', 'keywords')).toBeNull();
  });
});

describe('breadcrumbSchema', () => {
  it('numbers positions from 1 and makes each item absolute', () => {
    const schema = breadcrumbSchema([
      { name: 'Home', path: '/' },
      { name: 'Blog', path: '/blog' },
    ]) as { itemListElement: Array<{ position: number; name: string; item: string }> };

    expect(schema.itemListElement).toEqual([
      { '@type': 'ListItem', position: 1, name: 'Home', item: `${SITE_URL}/` },
      { '@type': 'ListItem', position: 2, name: 'Blog', item: `${SITE_URL}/blog` },
    ]);
  });
});
