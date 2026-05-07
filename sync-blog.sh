#!/usr/bin/env bash
# sync-blog.sh — Pull blog posts from ShipFlow-blog private repo and prepare
# static assets for the frontend.
#
# Usage:
#   BLOG_TOKEN=ghp_xxxx ./sync-blog.sh
#
# Run this LOCALLY before deploying (rsync + docker-compose up --build).
# The generated files in frontend/public/blog/ are committed/copied as static
# assets — no backend API is involved.

set -euo pipefail

REPO_URL="https://${BLOG_TOKEN}@github.com/farzad-sedaghatbin/ShipFlow-blog.git"
TMP_DIR="$(mktemp -d)"
DEST_DIR="frontend/public/blog/posts"
INDEX_FILE="frontend/public/blog/index.json"

echo "→ Cloning ShipFlow-blog..."
git clone --depth 1 --quiet "$REPO_URL" "$TMP_DIR"

echo "→ Setting up destination..."
mkdir -p "$DEST_DIR"

echo "→ Copying posts..."
# Copy all markdown files from the posts/ directory
if [ -d "$TMP_DIR/posts" ]; then
  cp "$TMP_DIR/posts/"*.md "$DEST_DIR/" 2>/dev/null || true
else
  echo "⚠  No posts/ directory found in blog repo — skipping copy."
fi

echo "→ Generating index.json..."
# Build a JSON array of slugs (filenames without .md extension)
# Sorted by filename descending so newest posts appear first (assumes date-prefixed names
# or slug ordering — adjust sort if needed)
slugs=()
for f in "$DEST_DIR"/*.md; do
  [ -f "$f" ] || continue
  slug="$(basename "$f" .md)"
  slugs+=("\"$slug\"")
done

# Sort descending (reverse lexicographic — works well with date-prefixed slugs)
IFS=$'\n' sorted=($(printf '%s\n' "${slugs[@]}" | sort -r)); unset IFS

if [ ${#sorted[@]} -eq 0 ]; then
  echo "[]" > "$INDEX_FILE"
  echo "⚠  No .md files found — wrote empty index.json"
else
  printf '[%s]\n' "$(IFS=','; echo "${sorted[*]}")" > "$INDEX_FILE"
  echo "✓  index.json written with ${#sorted[@]} post(s):"
  for s in "${sorted[@]}"; do
    echo "   $s"
  done
fi

echo "→ Cleaning up temp clone..."
rm -rf "$TMP_DIR"

echo "✓  Blog sync complete. Files are in $DEST_DIR"
echo "   Deploy with: rsync + docker-compose up --build"
