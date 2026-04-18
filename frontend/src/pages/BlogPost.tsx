import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Calendar, User, Rss, ChevronRight } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeSanitize from 'rehype-sanitize';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { format, parseISO } from 'date-fns';

// ── Frontmatter parser (shared logic with Blog.tsx) ───────────────────────────
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
      const arrMatch = rawVal.match(/^\[(.+)\]$/);
      if (arrMatch) {
        meta.keywords = arrMatch[1].split(',').map((s) => s.trim().replace(/^["']|["']$/g, ''));
      }
    } else {
      meta[key] = rawVal.replace(/^["']|["']$/g, '') as never;
    }
  }

  return { meta, body };
}

// ── Blog post page ────────────────────────────────────────────────────────────
export default function BlogPost() {
  const { slug } = useParams<{ slug: string }>();
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [meta, setMeta] = useState<Partial<PostFrontmatter>>({});
  const [body, setBody] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!slug) return;
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const res = await fetch(`/blog/posts/${slug}.md`);
        if (!res.ok) throw new Error(`Post not found (${res.status})`);
        const raw = await res.text();
        const parsed = parseFrontmatter(raw);
        if (!cancelled) {
          setMeta(parsed.meta);
          setBody(parsed.body);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load post');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [slug]);

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
                onClick={() => navigate('/blog')}
                aria-label={t('blog.backToBlog', 'Back to blog')}
              >
                <ArrowLeft className="h-5 w-5" />
              </Button>
              <Link to="/blog" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
                <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center">
                  <Rss className="h-4 w-4 text-primary-foreground" />
                </div>
                <span className="font-semibold text-foreground hidden sm:inline">
                  {t('blog.title', 'ShipFlow Blog')}
                </span>
              </Link>
            </div>
            {/* Breadcrumb */}
            <nav className="hidden md:flex items-center gap-1.5 text-sm text-muted-foreground">
              <Link to="/blog" className="hover:text-foreground transition-colors">
                {t('blog.title', 'Blog')}
              </Link>
              <ChevronRight className="h-3.5 w-3.5" />
              <span className="text-foreground truncate max-w-48">
                {meta.title ?? slug}
              </span>
            </nav>
          </div>
        </div>
      </header>

      <main className="container mx-auto px-4 max-w-3xl py-12 md:py-16">
        {/* ── Loading skeleton ─────────────────────────────────────────────── */}
        {loading && (
          <div className="animate-pulse">
            <div className="h-4 w-32 bg-muted rounded mb-6" />
            <div className="h-10 w-4/5 bg-muted rounded mb-4" />
            <div className="h-5 w-2/3 bg-muted rounded mb-8" />
            <div className="space-y-3">
              {[100, 85, 92, 78, 95].map((w, i) => (
                <div key={i} className={`h-4 bg-muted rounded`} style={{ width: `${w}%` }} />
              ))}
            </div>
          </div>
        )}

        {/* ── Error state ──────────────────────────────────────────────────── */}
        {error && (
          <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-8 text-center">
            <p className="text-destructive font-medium mb-2">
              {t('blog.postNotFound', 'Post not found')}
            </p>
            <p className="text-sm text-muted-foreground mb-6">{error}</p>
            <Button variant="outline" onClick={() => navigate('/blog')}>
              <ArrowLeft className="h-4 w-4 mr-2" />
              {t('blog.backToBlog', 'Back to blog')}
            </Button>
          </div>
        )}

        {/* ── Post content ─────────────────────────────────────────────────── */}
        {!loading && !error && (
          <>
            {/* Meta */}
            <div className="mb-8">
              <div className="flex flex-wrap items-center gap-3 text-sm text-muted-foreground mb-4">
                {meta.date && (
                  <span className="flex items-center gap-1.5">
                    <Calendar className="h-4 w-4 flex-shrink-0" />
                    <time dateTime={meta.date}>{formatDate(meta.date)}</time>
                  </span>
                )}
                {meta.author && (
                  <span className="flex items-center gap-1.5">
                    <User className="h-4 w-4 flex-shrink-0" />
                    {meta.author}
                  </span>
                )}
              </div>

              <h1 className="text-3xl md:text-4xl font-bold text-foreground leading-tight mb-4">
                {meta.title ?? slug}
              </h1>

              {meta.description && (
                <p className="text-lg text-muted-foreground leading-relaxed">
                  {meta.description}
                </p>
              )}

              {meta.keywords && meta.keywords.length > 0 && (
                <div className="flex flex-wrap gap-2 mt-4">
                  {meta.keywords.map((kw) => (
                    <Badge key={kw} variant="secondary" className="text-xs font-normal">
                      {kw}
                    </Badge>
                  ))}
                </div>
              )}
            </div>

            <Separator className="mb-8" />

            {/* Rendered markdown */}
            <div className="prose prose-neutral dark:prose-invert max-w-none
              prose-headings:font-bold prose-headings:text-foreground
              prose-h1:text-3xl prose-h2:text-2xl prose-h2:mt-10 prose-h2:mb-4
              prose-h3:text-xl prose-h3:mt-8
              prose-p:text-foreground/90 prose-p:leading-relaxed
              prose-a:text-primary prose-a:no-underline hover:prose-a:underline
              prose-strong:text-foreground
              prose-code:text-primary prose-code:bg-muted prose-code:px-1.5 prose-code:py-0.5 prose-code:rounded prose-code:text-sm prose-code:font-mono prose-code:before:content-none prose-code:after:content-none
              prose-pre:bg-muted prose-pre:border prose-pre:border-border prose-pre:rounded-lg prose-pre:overflow-x-auto
              prose-blockquote:border-l-primary prose-blockquote:text-muted-foreground prose-blockquote:not-italic
              prose-ul:text-foreground/90 prose-ol:text-foreground/90
              prose-li:my-1
              prose-hr:border-border
              prose-img:rounded-lg prose-img:border prose-img:border-border prose-img:shadow-sm
              prose-table:text-sm
              prose-th:text-foreground prose-th:font-semibold prose-th:bg-muted
              prose-td:text-foreground/90">
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                rehypePlugins={[rehypeSanitize]}
              >
                {body}
              </ReactMarkdown>
            </div>

            <Separator className="my-10" />

            {/* Back link */}
            <div className="flex items-center justify-between">
              <Button variant="outline" onClick={() => navigate('/blog')}>
                <ArrowLeft className="h-4 w-4 mr-2" />
                {t('blog.backToBlog', 'Back to blog')}
              </Button>
              <Button variant="ghost" size="sm" onClick={() => navigate('/login')}>
                {t('landing.getStarted', 'Try ShipFlow free')}
              </Button>
            </div>
          </>
        )}
      </main>

      {/* ── Footer ─────────────────────────────────────────────────────────── */}
      <footer className="border-t border-border py-8 mt-8">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4 text-sm text-muted-foreground">
            <span>© {new Date().getFullYear()} ShipFlow. Open source under MIT license.</span>
            <Link to="/blog" className="hover:text-foreground transition-colors">
              ← {t('blog.backToBlog', 'Back to blog')}
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
