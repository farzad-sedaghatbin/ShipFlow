#!/bin/bash

# Fix: size={{ xs: 6> -> size={{ xs: 6 }}>
find src -name "*.tsx" -type f -exec sed -i '' 's/size={{ xs: \([0-9]*\)>/size={{ xs: \1 }}>/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/size={{ sm: \([0-9]*\)>/size={{ sm: \1 }}>/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/size={{ md: \([0-9]*\)>/size={{ md: \1 }}>/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/size={{ lg: \([0-9]*\)>/size={{ lg: \1 }}>/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/size={{ xl: \([0-9]*\)>/size={{ xl: \1 }}>/g' {} \;

# Fix: size={{ xs: 12 sm: 6 md: 3 key -> size={{ xs: 12, sm: 6, md: 3 }} key (add commas)
find src -name "*.tsx" -type f -exec sed -i '' 's/\(xs: [0-9]*\) \(sm: \)/\1, \2/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/\(sm: [0-9]*\) \(md: \)/\1, \2/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/\(md: [0-9]*\) \(lg: \)/\1, \2/g' {} \;
find src -name "*.tsx" -type f -exec sed -i '' 's/\(lg: [0-9]*\) \(xl: \)/\1, \2/g' {} \;

# Fix: size={{ xs: 12, sm: 6, md: 3 key -> size={{ xs: 12, sm: 6, md: 3 }} key (close and reopen before attributes)
find src -name "*.tsx" -type f -exec sed -i '' 's/size={{ \([^}]*\) \(key=\|sx=\|container\|spacing=\)/size={{ \1 }} \2/g' {} \;

