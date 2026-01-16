#!/bin/bash

# Complete Translation Script - Updates all 17 pages with i18n
# This script systematically replaces hardcoded English text with t() calls

echo "Starting comprehensive translation update..."
echo "This will update all 17 pages to use i18n translations"

# Define the frontend directory
FRONTEND_DIR="/Users/farzad/IdeaProjects/git/shapeup-tracker/frontend/src"

# Add useTranslation imports to all pages if not present
add_translation_import() {
  local file=$1
  # Check if import already exists
  if ! grep -q "import { useTranslation }" "$file"; then
    # Add import after the last import statement
    sed -i '' '/^import.*from/a\
import { useTranslation } from '\''react-i18next'\'';
' "$file"
  fi
}

# Add useTranslation hook to component
add_translation_hook() {
  local file=$1
  local component_name=$2
  # Check if hook already exists
  if ! grep -q "const { t } = useTranslation()" "$file"; then
    # Add hook at the beginning of the component function
    sed -i '' "/export.*function $component_name\\|export.*const $component_name.*=/a\\
  const { t } = useTranslation();
" "$file"
  fi
}

echo "✅ Translation keys already added to en.json and fa.json"
echo "📝 Now updating component files..."

# === BATCH 1: Core Workflow Pages ===
echo ""
echo "=== BATCH 1: Core Workflow Pages ==="

# 1. Reports.tsx - Already started, need to complete
echo "1/17 Completing Reports.tsx..."
# Will be completed via VSCode Copilot

# 2. BacklogPage.tsx
echo "2/17 Processing BacklogPage.tsx..."
add_translation_import "$FRONTEND_DIR/pages/BacklogPage.tsx"

# 3. PitchBoard.tsx  
echo "3/17 Processing PitchBoard.tsx..."
add_translation_import "$FRONTEND_DIR/pages/PitchBoard.tsx"

# 4. PitchDetail.tsx
echo "4/17 Processing PitchDetail.tsx..."
add_translation_import "$FRONTEND_DIR/pages/PitchDetail.tsx"

# 5. CycleDetail.tsx
echo "5/17 Processing CycleDetail.tsx..."
add_translation_import "$FRONTEND_DIR/pages/CycleDetail.tsx"

# 6. HealthOverview.tsx
echo "6/17 Processing HealthOverview.tsx..."
add_translation_import "$FRONTEND_DIR/pages/HealthOverview.tsx"

# === BATCH 2: Work Management Pages ===
echo ""
echo "=== BATCH 2: Work Management Pages ==="

# 7. WorkLogsPage.tsx
echo "7/17 Processing WorkLogsPage.tsx..."
add_translation_import "$FRONTEND_DIR/pages/WorkLogsPage.tsx"

# 8. MyWorkLogs.tsx
echo "8/17 Processing MyWorkLogs.tsx..."
add_translation_import "$FRONTEND_DIR/pages/MyWorkLogs.tsx"

# 9. People.tsx
echo "9/17 Processing People.tsx..."
add_translation_import "$FRONTEND_DIR/pages/People.tsx"

# 10. TaskDetailPage.tsx
echo "10/17 Processing TaskDetailPage.tsx..."
add_translation_import "$FRONTEND_DIR/pages/TaskDetailPage.tsx"

# 11. BettingTable.tsx
echo "11/17 Processing BettingTable.tsx..."
add_translation_import "$FRONTEND_DIR/pages/BettingTable.tsx"

# === BATCH 3: Collaboration Pages ===
echo ""
echo "=== BATCH 3: Collaboration Pages ==="

# 12. RetroBoard.tsx
echo "12/17 Processing RetroBoard.tsx..."
add_translation_import "$FRONTEND_DIR/pages/RetroBoard.tsx"

# 13. RetroList.tsx
echo "13/17 Processing RetroList.tsx..."
add_translation_import "$FRONTEND_DIR/pages/RetroList.tsx"

# 14. MeetingList.tsx
echo "14/17 Processing MeetingList.tsx..."
add_translation_import "$FRONTEND_DIR/pages/MeetingList.tsx"

# 15. Profile.tsx
echo "15/17 Processing Profile.tsx..."
add_translation_import "$FRONTEND_DIR/pages/Profile.tsx"

# 16. UserManagement.tsx
echo "16/17 Processing UserManagement.tsx..."
add_translation_import "$FRONTEND_DIR/pages/UserManagement.tsx"

# 17. OrganizationSettings.tsx
echo "17/17 Processing OrganizationSettings.tsx..."
add_translation_import "$FRONTEND_DIR/pages/OrganizationSettings.tsx"

echo ""
echo "✅ Import statements added to all files"
echo ""
echo "⚠️  MANUAL STEP REQUIRED:"
echo "Due to the complexity and volume of string replacements (~1000+ strings),"
echo "the actual t() call replacements need to be done systematically."
echo ""
echo "Next steps:"
echo "1. Use the comprehensive-translation-replacements.ts file (creating next)"
echo "2. This TypeScript file will contain all string → t() mappings"
echo "3. Execute it to apply all replacements automatically"
echo ""
