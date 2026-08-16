import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Calendar, BookOpen, Rss } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { format, parseISO } from 'date-fns';
import { useSeo, breadcrumbSchema, SITE_URL } from '@/hooks/useSeo';

// ── Frontmatter parser ────────────────────────────────────────────────────────
interface PostFrontmatter {
  title: string;
  date: string;
  description: string;
  author?: string;
  keywords?: string[];
}

function parseFrontmatter(raw: string): { meta: Partial<PostFrontmatter>; body: string } {
  const fenceRe = /^---\r?\n([\s\S]*?)\r?\n---\r?\n?([\s\S]*)$/;
  const match = raw.match(fenceRe);
  if (!match) return { meta: {}, body: raw };

  const yamlBlock = match[1];
  const body = match[2] ?? '';
  const meta: Partial<PostFrontmatter> = {};

  for (const line of yamlBlock.split('\n')) {
    const colonIdx = line.indexOf(':');
    if (colonIdx === -1) continue;
    const key = line.slice(0, colonIdx).trim() as keyof PostFrontmatter;
    const rawVal = line.slice(colonIdx + 1).trim();

    if (key === 'keywords') {
      // Parse simple YAML array on one line: ["a", "b"] or [a, b]
      const arrMatch = rawVal.match(/^\[(.+)\]$/);
      if (arrMatch) {
        meta.keywords = arrMatch[1].split(',').map((s) => s.trim().replace(/^["']|["']$/g, ''));
      }
    } else {
      // Strip surrounding quotes
      meta[key] = rawVal.replace(/^["']|["']$/g, '') as never;
    }
  }

  return { meta, body };
}

// ── Types ─────────────────────────────────────────────────────────────────────
interface PostSummary {
  slug: string;
  title: string;
  date: string;
  description: string;
  author?: string;
  keywords?: string[];
}

// ── Blog index page ───────────────────────────────────────────────────────────
export default function Blog() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [posts, setPosts] = useState<PostSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useSeo({
    title: 'Blog — Shape Up, Scrum & Kanban in Practice',
    description:
      'Practical writing on Shape Up — cycles, betting tables, hill charts — and how it compares to Scrum and Kanban when a team runs more than one.',
    path: '/blog',
    keywords: [
      'shape up',
      'shape up methodology',
      'hill chart',
      'betting table',
      'shape up vs scrum',
      'product development',
    ],
    jsonLd: [
      {
        '@context': 'https://schema.org',
        '@type': 'Blog',
        name: 'ShipFlow Blog',
        description:
          'Practical writing on Shape Up, Scrum, and Kanban — cycles, betting tables, hill charts, and choosing between methodologies.',
        url: `${SITE_URL}/blog`,
        inLanguage: 'en',
      },
      breadcrumbSchema([
        { name: 'Home', path: '/' },
        { name: 'Blog', path: '/blog' },
      ]),
    ],
  });

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        // 1. Fetch list of slugs from static index
        const indexRes = await fetch('/blog/index.json');
        if (!indexRes.ok) throw new Error('Blog index not found');
        const slugs: string[] = await indexRes.json();

        // 2. Fetch each post and parse frontmatter
        const summaries = await Promise.all(
          slugs.map(async (slug) => {
            try {
              const res = await fetch(`/blog/posts/${slug}.md`);
              if (!res.ok) return null;
              const raw = await res.text();
              const { meta } = parseFrontmatter(raw);
              const post: PostSummary = {
                slug,
                title: meta.title ?? slug,
                date: meta.date ?? '',
                description: meta.description ?? '',
                author: meta.author,
                keywords: meta.keywords,
              };
              return post;
            } catch {
              return null;
            }
          })
        );

        if (!cancelled) {
          // Sort newest-first rather than trusting index.json's order, so a
          // newly added slug doesn't land in the middle of the list and so
          // the freshest post gets the top internal link on the page.
          const sorted = summaries
            .filter((p): p is PostSummary => Boolean(p))
            .sort((a, b) => (a.date < b.date ? 1 : a.date > b.date ? -1 : 0));
          setPosts(sorted);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load blog posts');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  const formatDate = (dateStr: string) => {
    try {
      return format(parseISO(dateStr), 'MMMM d, yyyy');
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="min-h-screen bg-background">
      {/* ── Header ─────────────────────────────────────────────────────────── */}
      <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b border-border">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-3">
              <Button
                variant="ghost"
                size="icon"
                onClick={() => navigate('/')}
                aria-label={t('common.back')}
              >
                <ArrowLeft className="h-5 w-5" />
              </Button>
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center">
                  <Rss className="h-4 w-4 text-primary-foreground" />
                </div>
                <span className="font-semibold text-foreground">
                  {t('blog.title', 'ShipFlow Blog')}
                </span>
              </div>
            </div>
            <Button variant="outline" size="sm" onClick={() => navigate('/login')}>
              {t('landing.getStarted', 'Get Started')}
            </Button>
          </div>
        </div>
      </header>

      {/* ── Hero ───────────────────────────────────────────────────────────── */}
      <section className="py-16 md:py-20 bg-gradient-to-br from-primary/5 via-background to-secondary/5">
        <div className="container mx-auto px-4 max-w-6xl text-center">
          <Badge variant="secondary" className="mb-4 gap-1.5">
            <BookOpen className="h-3.5 w-3.5" />
            {t('blog.badge', 'Articles & Insights')}
          </Badge>
          <h1 className="text-3xl md:text-5xl font-bold text-foreground mb-4">
            {t('blog.headline', 'The ShipFlow Blog')}
          </h1>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            {t(
              'blog.subheadline',
              'Shape Up methodology, product building, and engineering insights from the team behind ShipFlow.'
            )}
          </p>
        </div>
      </section>

      {/* ── Post list ──────────────────────────────────────────────────────── */}
      <section className="py-12 md:py-16">
        <div className="container mx-auto px-4 max-w-4xl">
          {loading && (
            <div className="flex flex-col gap-6">
              {[1, 2, 3].map((i) => (
                <div
                  key={i}
                  className="rounded-xl border border-border bg-card p-6 animate-pulse"
                >
                  <div className="h-4 w-24 bg-muted rounded mb-3" />
                  <div className="h-7 w-3/4 bg-muted rounded mb-3" />
                  <div className="h-4 w-full bg-muted rounded mb-2" />
                  <div className="h-4 w-2/3 bg-muted rounded" />
                </div>
              ))}
            </div>
          )}

          {error && (
            <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-8 text-center">
              <p className="text-destructive font-medium mb-1">
                {t('blog.errorTitle', 'Could not load posts')}
              </p>
              <p className="text-sm text-muted-foreground">{error}</p>
            </div>
          )}

          {!loading && !error && posts.length === 0 && (
            <div className="rounded-xl border border-border bg-card p-12 text-center">
              <BookOpen className="h-10 w-10 text-muted-foreground mx-auto mb-4" />
              <p className="text-lg font-medium text-foreground mb-1">
                {t('blog.emptyTitle', 'No posts yet')}
              </p>
              <p className="text-sm text-muted-foreground">
                {t('blog.emptySubtitle', 'Check back soon — articles are on the way.')}
              </p>
            </div>
          )}

          {!loading && !error && posts.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {posts.map((post) => (
                <Link
                  key={post.slug}
                  to={`/blog/${post.slug}`}
                  className="group rounded-xl border border-border bg-card hover:border-primary/40 hover:shadow-md transition-all duration-200 p-6 block"
                >
                  {post.date && (
                    <div className="flex items-center gap-1.5 text-xs text-muted-foreground mb-3">
                      <Calendar className="h-3.5 w-3.5 flex-shrink-0" />
                      <time dateTime={post.date}>{formatDate(post.date)}</time>
                    </div>
                  )}
                  <h2 className="text-lg font-bold text-foreground group-hover:text-primary transition-colors mb-2 leading-snug">
                    {post.title}
                  </h2>
                  {post.description && (
                    <p className="text-muted-foreground text-sm leading-relaxed line-clamp-3">
                      {post.description}
                    </p>
                  )}
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* ── Footer ─────────────────────────────────────────────────────────── */}
      <footer className="border-t border-border py-8">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4 text-sm text-muted-foreground">
            <span>© {new Date().getFullYear()} ShipFlow. Open source under MIT license.</span>
            <button
              onClick={() => navigate('/')}
              className="hover:text-foreground transition-colors"
            >
              ← {t('blog.backToHome', 'Back to home')}
            </button>
          </div>
        </div>
      </footer>
    </div>
  );
}
