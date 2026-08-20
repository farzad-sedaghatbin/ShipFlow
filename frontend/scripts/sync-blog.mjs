#!/usr/bin/env node
/**
 * Pulls blog posts from the private ShipFlow-blog repository into
 * `public/blog/posts/`, then regenerates `public/blog/index.json`.
 *
 * WHY THIS EXISTS
 * ---------------
 * Published blog posts live in a private repo, not this open-source one, so a
 * merged pull request here cannot publish an article on shipflow.dev.
 *
 * The GitHub Actions image build (`.github/workflows/docker.yml`) already does
 * this injection — but only on a `v*.*.*` tag push. A local `docker build`
 * skips it entirely, which silently published nothing: three posts written in
 * May 2026 sat unshipped for months because every deploy was a local build.
 *
 * Run this before any local production build and that failure mode is gone:
 *
 *   npm run blog:sync && npm run build      # or: npm run build:deploy
 *
 * AUTH — tries, in order:
 *   1. BLOG_SYNC_TOKEN env var (what CI uses)
 *   2. the `gh` CLI's own credentials (what a local machine usually has)
 *
 * With neither, it exits 0 and leaves whatever posts are already on disk. That
 * is deliberate: an outside contributor must be able to build the frontend
 * without access to the private content repo.
 */

import { execFileSync } from 'node:child_process';
import { readFileSync, writeFileSync, readdirSync, mkdirSync, copyFileSync, rmSync, existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const POSTS_DIR = join(ROOT, 'public', 'blog', 'posts');
const INDEX_FILE = join(ROOT, 'public', 'blog', 'index.json');
const REPO = 'farzad-sedaghatbin/ShipFlow-blog';

function log(msg) {
  console.log(`[blog:sync] ${msg}`);
}

/** Clones the private repo into a temp dir. Returns its path, or null. */
function fetchPrivatePosts() {
  const dest = join(tmpdir(), `shipflow-blog-${process.pid}`);
  rmSync(dest, { recursive: true, force: true });

  const token = process.env.BLOG_SYNC_TOKEN;
  if (token) {
    try {
      execFileSync('git', ['clone', '--depth', '1', '--quiet', `https://${token}@github.com/${REPO}.git`, dest], {
        stdio: ['ignore', 'ignore', 'pipe'],
      });
      log('fetched via BLOG_SYNC_TOKEN');
      return dest;
    } catch {
      log('BLOG_SYNC_TOKEN is set but the clone failed — is the token still valid?');
      return null;
    }
  }

  try {
    execFileSync('gh', ['repo', 'clone', REPO, dest, '--', '--depth', '1', '--quiet'], {
      stdio: ['ignore', 'ignore', 'pipe'],
    });
    log('fetched via gh CLI credentials');
    return dest;
  } catch {
    return null;
  }
}

/**
 * Files in posts/ that are documentation, not articles. Without this the
 * directory README lands in index.json and the blog renders a "README" post.
 */
const NOT_A_POST = new Set(['README.md', 'readme.md']);

/** Rebuilds index.json from every post on disk, newest first. */
function rebuildIndex() {
  const slugs = readdirSync(POSTS_DIR)
    .filter((f) => f.endsWith('.md') && !NOT_A_POST.has(f))
    .map((f) => {
      const raw = readFileSync(join(POSTS_DIR, f), 'utf8');
      const m = raw.match(/^---[\s\S]*?\bdate:\s*(\S+)[\s\S]*?---/);
      return { slug: f.replace(/\.md$/, ''), date: m ? m[1] : '1970-01-01' };
    })
    .sort((a, b) => b.date.localeCompare(a.date))
    .map((p) => p.slug);

  writeFileSync(INDEX_FILE, `${JSON.stringify(slugs, null, 2)}\n`, 'utf8');
  return slugs;
}

mkdirSync(POSTS_DIR, { recursive: true });

const clone = fetchPrivatePosts();

if (!clone) {
  log('no access to the private blog repo — leaving existing posts untouched.');
  log('this is expected for outside contributors; set BLOG_SYNC_TOKEN or run `gh auth login` to sync.');
  const slugs = rebuildIndex();
  log(`index.json rebuilt from ${slugs.length} local post(s)`);
  process.exit(0);
}

try {
  const src = join(clone, 'posts');
  if (!existsSync(src)) {
    log(`the private repo has no posts/ directory — nothing to sync`);
  } else {
    const files = readdirSync(src).filter((f) => f.endsWith('.md') && !NOT_A_POST.has(f));
    for (const f of files) copyFileSync(join(src, f), join(POSTS_DIR, f));
    log(`copied ${files.length} post(s) from ${REPO}`);
  }

  const slugs = rebuildIndex();
  log(`index.json now lists ${slugs.length} post(s), newest first:`);
  for (const s of slugs) log(`  - ${s}`);
} finally {
  rmSync(clone, { recursive: true, force: true });
}
