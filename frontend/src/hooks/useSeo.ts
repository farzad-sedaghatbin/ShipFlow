import { useEffect } from 'react';

/**
 * Per-route SEO metadata for the ShipFlow SPA.
 *
 * ShipFlow ships as a single-page app served from one static `index.html`,
 * so without this hook every route shares one `<title>` and one meta
 * description. Search engines then have effectively a single indexable
 * document for the whole site, and no page can rank for its own topic.
 *
 * `useSeo` writes the tags that matter into `<head>` on mount and rewrites
 * them on every route change. Tags it owns carry `data-seo="1"` so they can
 * be cleaned up without disturbing the static tags in `index.html`.
 *
 * Note on the product name: "ShipFlow" collides with several unrelated
 * products (ship-hull CFD software, freight/e-commerce shipping platforms).
 * Titles and descriptions should therefore always carry a disambiguating
 * qualifier — "Shape Up", "project management", "open source" — so the
 * search snippet tells the right reader they are in the right place and the
 * wrong reader that they are not.
 *
 * Note on positioning: ShipFlow is methodology-agnostic — Shape Up, Scrum and
 * Kanban all run as first-class project modes. Shape Up leads in copy because
 * it is the differentiator and the winnable search ground, not because it is
 * the only mode. Never write a description that implies Shape Up is all this
 * does; "does it also do Scrum?" is the first question a visiting team asks.
 * See SEO_GUIDE.md for which Scrum/Kanban terms are worth targeting.
 */

export const SITE_URL = 'https://shipflow.dev';
export const SITE_NAME = 'ShipFlow';
export const DEFAULT_OG_IMAGE = `${SITE_URL}/android-chrome-512x512.png`;

/** Appended to page titles so every SERP result self-disambiguates. */
const TITLE_SUFFIX = ` · ${SITE_NAME}`;

export interface SeoConfig {
  /** Page title without the site-name suffix. */
  title: string;
  /** Meta description. Aim for 140–160 characters. */
  description: string;
  /** Canonical path, e.g. `/blog/what-is-shape-up`. Leading slash required. */
  path: string;
  keywords?: string[];
  image?: string;
  type?: 'website' | 'article';
  /** ISO date — article pages only. */
  publishedTime?: string;
  /** ISO date — article pages only. */
  modifiedTime?: string;
  author?: string;
  /** Set for pages that should be reachable but never indexed. */
  noindex?: boolean;
  /** One or more schema.org objects, emitted as `application/ld+json`. */
  jsonLd?: Record<string, unknown> | Record<string, unknown>[];
  /** Use the title verbatim, without the ` · ShipFlow` suffix. */
  exactTitle?: boolean;
}

type MetaKey = 'name' | 'property';

function upsertMeta(keyType: MetaKey, key: string, content: string): void {
  let el = document.head.querySelector<HTMLMetaElement>(`meta[${keyType}="${key}"]`);
  if (!el) {
    el = document.createElement('meta');
    el.setAttribute(keyType, key);
    el.setAttribute('data-seo', '1');
    document.head.appendChild(el);
  }
  el.setAttribute('content', content);
}

function removeMeta(keyType: MetaKey, key: string): void {
  const el = document.head.querySelector(`meta[${keyType}="${key}"][data-seo="1"]`);
  el?.remove();
}

function upsertCanonical(href: string): void {
  let el = document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
  if (!el) {
    el = document.createElement('link');
    el.setAttribute('rel', 'canonical');
    el.setAttribute('data-seo', '1');
    document.head.appendChild(el);
  }
  el.setAttribute('href', href);
}

function setJsonLd(blocks: Record<string, unknown>[]): void {
  document.head.querySelectorAll('script[data-seo-jsonld="1"]').forEach((n) => n.remove());
  for (const block of blocks) {
    const script = document.createElement('script');
    script.type = 'application/ld+json';
    script.setAttribute('data-seo-jsonld', '1');
    script.textContent = JSON.stringify(block);
    document.head.appendChild(script);
  }
}

/**
 * Applies SEO metadata for the current route.
 *
 * Pass a memoised config (or a module-level constant) — the hook re-runs
 * whenever the serialised config changes, so an inline object literal is
 * fine but will re-apply on every render of a changed field only.
 */
export function useSeo(config: SeoConfig): void {
  const serialized = JSON.stringify(config);

  useEffect(() => {
    const cfg: SeoConfig = JSON.parse(serialized);
    const {
      title,
      description,
      path,
      keywords,
      image = DEFAULT_OG_IMAGE,
      type = 'website',
      publishedTime,
      modifiedTime,
      author,
      noindex = false,
      jsonLd,
      exactTitle = false,
    } = cfg;

    const fullTitle = exactTitle ? title : `${title}${TITLE_SUFFIX}`;
    const canonical = `${SITE_URL}${path === '/' ? '/' : path.replace(/\/$/, '')}`;

    document.title = fullTitle;

    upsertMeta('name', 'description', description);
    upsertCanonical(canonical);

    if (keywords?.length) {
      upsertMeta('name', 'keywords', keywords.join(', '));
    } else {
      removeMeta('name', 'keywords');
    }

    upsertMeta(
      'name',
      'robots',
      noindex ? 'noindex, nofollow' : 'index, follow, max-image-preview:large, max-snippet:-1',
    );

    // Open Graph
    upsertMeta('property', 'og:title', fullTitle);
    upsertMeta('property', 'og:description', description);
    upsertMeta('property', 'og:url', canonical);
    upsertMeta('property', 'og:type', type);
    upsertMeta('property', 'og:image', image);
    upsertMeta('property', 'og:site_name', SITE_NAME);

    // Twitter
    upsertMeta('name', 'twitter:card', 'summary_large_image');
    upsertMeta('name', 'twitter:title', fullTitle);
    upsertMeta('name', 'twitter:description', description);
    upsertMeta('name', 'twitter:image', image);

    // Article-only tags
    if (type === 'article') {
      if (publishedTime) upsertMeta('property', 'article:published_time', publishedTime);
      if (modifiedTime) upsertMeta('property', 'article:modified_time', modifiedTime);
      if (author) upsertMeta('property', 'article:author', author);
    } else {
      removeMeta('property', 'article:published_time');
      removeMeta('property', 'article:modified_time');
      removeMeta('property', 'article:author');
    }

    setJsonLd(jsonLd ? (Array.isArray(jsonLd) ? jsonLd : [jsonLd]) : []);

    return () => {
      // Structured data is strictly per-page; leaving a stale block behind
      // would describe the previous route to any crawler that reads on
      // navigation. Meta tags are overwritten by the next route's useSeo.
      document.head.querySelectorAll('script[data-seo-jsonld="1"]').forEach((n) => n.remove());
    };
  }, [serialized]);
}

/** schema.org Organization — stable across the site. */
export const organizationSchema: Record<string, unknown> = {
  '@context': 'https://schema.org',
  '@type': 'Organization',
  name: SITE_NAME,
  url: SITE_URL,
  logo: DEFAULT_OG_IMAGE,
  description:
    'ShipFlow is an open-source, self-hostable, methodology-agnostic project management platform supporting Shape Up, Scrum, and Kanban in one workspace.',
  sameAs: ['https://github.com/farzad-sedaghatbin/shipflow'],
};

/** Builds a BreadcrumbList for a page nested under one or more parents. */
export function breadcrumbSchema(
  trail: Array<{ name: string; path: string }>,
): Record<string, unknown> {
  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: trail.map((crumb, i) => ({
      '@type': 'ListItem',
      position: i + 1,
      name: crumb.name,
      item: `${SITE_URL}${crumb.path}`,
    })),
  };
}
