#!/bin/bash
# Fix all malformed Grid2 size patterns

# Pattern: xs: {N} -> xs: N (remove extra braces around number)
find src -name "*.tsx" -type f -exec sed -i '' 's/xs: {\([0-9]*\)}/xs: \1/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/sm: {\([0-9]*\)}/sm: \1/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/md: {\([0-9]*\)}/md: \1/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/lg: {\([0-9]*\)}/lg: \1/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/xl: {\([0-9]*\)}/xl: \1/g' {} \;

# Pattern: size={ xs: N md=N } -> size={{ xs: N, md: N }}
# First ensure all have commas
find src -name "*.tsx" -type f -exec sed -i '' 's/size={ \(xs: [0-9]*\) \(sm\|md\|lg\|xl\)/size={{ \1, \2/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/size={{ \(xs: [0-9]*, sm: [0-9]*\) \(md\|lg\|xl\)/size={{ \1, \2/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/size={{ \(xs: [0-9]*, sm: [0-9]*, md: [0-9]*\) \(lg\|xl\)/size={{ \1, \2/g' {} \;

# Pattern: md={N} -> md: N
find src -name "*.tsx" -type f -exec sed -i '' 's/xs={\([0-9]*\)}/xs: \1/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/sm={\([0-9]*\)}/sm: \1/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/md={\([0-9]*\)}/md: \1/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/lg={\([0-9]*\)}/lg: \1/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/xl={\([0-9]*\)}/xl: \1/g' {} \;

# Pattern: size={ -> size={{
find src -name "*.tsx" -type f -exec sed -i '' 's/size={ /size={{ /g' {} \;

# Pattern: close with }} for all size props
find src -name "*.tsx" -type f -exec sed -i '' 's/size={{ \([^}]*\)}}>/size={{ \1 }}/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/size={{ \([^}]*\)}>/size={{ \1 }}/g' {} \;
