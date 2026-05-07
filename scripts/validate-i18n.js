#!/usr/bin/env node

/**
 * i18n Translation Validation Script
 * 
 * This script performs comprehensive validation of translation files:
 * 1. Compares en.json and fa.json to find missing keys
 * 2. Scans TypeScript/TSX files for translation usage
 * 3. Identifies keys used in code but missing from translation files
 * 4. Reports naming mismatches and structural inconsistencies
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// ANSI color codes for terminal output
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  magenta: '\x1b[35m',
  cyan: '\x1b[36m',
};

class I18nValidator {
  constructor() {
    this.projectRoot = path.resolve(__dirname, '..');
    this.frontendRoot = path.join(this.projectRoot, 'frontend');
    this.i18nPath = path.join(this.frontendRoot, 'src', 'i18n', 'locales');
    this.enPath = path.join(this.i18nPath, 'en.json');
    this.faPath = path.join(this.i18nPath, 'fa.json');
    this.srcPath = path.join(this.frontendRoot, 'src');
    
    this.enKeys = new Set();
    this.faKeys = new Set();
    this.usedKeys = new Set();
    this.issues = {
      missingInFa: [],
      missingInEn: [],
      missingInBoth: [],
      potentialMismatches: [],
      duplicateKeysEn: [],
      duplicateKeysFa: [],
    };
    
    this.stats = {
      totalEnKeys: 0,
      totalFaKeys: 0,
      totalUsedKeys: 0,
      filesScanned: 0,
    };
  }

  log(message, color = 'reset') {
    console.log(`${colors[color]}${message}${colors.reset}`);
  }

  logSection(title) {
    console.log('\n' + '='.repeat(80));
    this.log(title, 'bright');
    console.log('='.repeat(80));
  }

  /**
   * Check if a string looks like a valid translation key
   * Filters out false positives like single characters, date formats, etc.
   */
  isValidTranslationKey(key) {
    // Skip single characters and very short strings
    if (key.length <= 1) return false;
    
    // Skip date format strings
    if (/^[YMDHmsTZ\-\/: ]+$/.test(key)) return false;
    
    // Skip pure numbers
    if (/^\d+$/.test(key)) return false;
    
    // Skip strings that are just punctuation/whitespace
    if (/^[\s\.,;:!?\/\-_]+$/.test(key)) return false;
    
    // Skip keys that end with a dot (incomplete dynamic keys)
    if (key.endsWith('.')) return false;
    
    // Skip keys that contain segments starting with underscore (internal/metadata keys)
    if (/\._/.test(key)) return false;
    
    // Skip single word keys that look like HTML attributes or generic terms
    if (!key.includes('.') && /^(category|type|value|name|id|key|href|src|alt)$/i.test(key)) return false;
    
    // Valid keys should match camelCase.camelCase or camelCase pattern
    // Allow alphanumeric with dots and underscores
    if (!/^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)*$/.test(key)) {
      // Also allow keys that start with a namespace like "common." etc.
      if (!/^[a-zA-Z]/.test(key)) return false;
    }
    
    return true;
  }

  /**
   * Detect duplicate keys in a JSON file by parsing the raw text.
   * JSON.parse() silently keeps only the last value for duplicate keys,
   * so we need to parse manually to detect them.
   */
  detectDuplicateKeys(filePath, fileName) {
    const content = fs.readFileSync(filePath, 'utf8');
    const duplicates = [];
    
    // Track keys at each nesting level
    // We use a stack-based approach to handle nested objects
    const keyStack = []; // Track the current path in the JSON tree
    const seenKeysAtLevel = [new Map()]; // Track seen keys at each depth level
    
    // Simple state machine to parse JSON keys
    let inString = false;
    let escaped = false;
    let currentKey = '';
    let readingKey = false;
    let depth = 0;
    let lineNumber = 1;
    let lastKeyLine = 1;
    
    for (let i = 0; i < content.length; i++) {
      const char = content[i];
      const prevChar = i > 0 ? content[i - 1] : '';
      
      if (char === '\n') {
        lineNumber++;
      }
      
      if (escaped) {
        if (readingKey) currentKey += char;
        escaped = false;
        continue;
      }
      
      if (char === '\\' && inString) {
        escaped = true;
        if (readingKey) currentKey += char;
        continue;
      }
      
      if (char === '"') {
        if (!inString) {
          // Starting a string - check if this could be a key
          // Keys come after { or , at the current depth
          inString = true;
          // Look back for : or start of object
          let j = i - 1;
          while (j >= 0 && /[\s\n]/.test(content[j])) j--;
          if (j >= 0 && (content[j] === '{' || content[j] === ',')) {
            readingKey = true;
            currentKey = '';
            lastKeyLine = lineNumber;
          }
        } else {
          // Ending a string
          inString = false;
          if (readingKey) {
            // We just finished reading a key
            // Check for colon after whitespace
            let j = i + 1;
            while (j < content.length && /[\s\n]/.test(content[j])) j++;
            if (content[j] === ':') {
              // This IS a key
              const fullKey = keyStack.length > 0 
                ? keyStack.join('.') + '.' + currentKey 
                : currentKey;
              
              const levelMap = seenKeysAtLevel[depth] || new Map();
              if (levelMap.has(currentKey)) {
                duplicates.push({
                  key: fullKey,
                  simpleKey: currentKey,
                  firstLine: levelMap.get(currentKey),
                  secondLine: lastKeyLine,
                });
              } else {
                levelMap.set(currentKey, lastKeyLine);
              }
              seenKeysAtLevel[depth] = levelMap;
            }
            readingKey = false;
          }
        }
        continue;
      }
      
      if (inString) {
        if (readingKey) currentKey += char;
        continue;
      }
      
      // Handle structure characters outside of strings
      if (char === '{') {
        // Push the last key onto the stack if we just saw one
        if (currentKey && !readingKey) {
          keyStack.push(currentKey);
        }
        depth++;
        seenKeysAtLevel[depth] = new Map();
        currentKey = '';
      } else if (char === '}') {
        depth--;
        if (keyStack.length > 0) {
          keyStack.pop();
        }
        seenKeysAtLevel[depth + 1] = new Map();
      }
    }
    
    return duplicates;
  }

  /**
   * Check both i18n files for duplicate keys
   */
  checkDuplicateKeys() {
    this.logSection('🔑 Checking for Duplicate Keys');
    
    const enDuplicates = this.detectDuplicateKeys(this.enPath, 'en.json');
    const faDuplicates = this.detectDuplicateKeys(this.faPath, 'fa.json');
    
    this.issues.duplicateKeysEn = enDuplicates;
    this.issues.duplicateKeysFa = faDuplicates;
    
    if (enDuplicates.length === 0 && faDuplicates.length === 0) {
      this.log('✓ No duplicate keys found in either file.', 'green');
    } else {
      if (enDuplicates.length > 0) {
        this.log(`✗ ${enDuplicates.length} duplicate key(s) found in en.json:`, 'red');
        enDuplicates.forEach(dup => {
          this.log(`  - "${dup.key}" appears on lines ${dup.firstLine} and ${dup.secondLine}`, 'red');
        });
      }
      if (faDuplicates.length > 0) {
        this.log(`✗ ${faDuplicates.length} duplicate key(s) found in fa.json:`, 'red');
        faDuplicates.forEach(dup => {
          this.log(`  - "${dup.key}" appears on lines ${dup.firstLine} and ${dup.secondLine}`, 'red');
        });
      }
    }
    
    return enDuplicates.length + faDuplicates.length;
  }

  /**
   * Recursively extract all keys from a nested object
   */
  extractKeys(obj, prefix = '') {
    const keys = [];
    
    for (const [key, value] of Object.entries(obj)) {
      const fullKey = prefix ? `${prefix}.${key}` : key;
      
      if (value && typeof value === 'object' && !Array.isArray(value)) {
        // Recursively process nested objects
        keys.push(...this.extractKeys(value, fullKey));
      } else {
        // Leaf node - this is an actual translation key
        keys.push(fullKey);
      }
    }
    
    return keys;
  }

  /**
   * Load and parse JSON translation files
   */
  loadTranslations() {
    this.logSection('📁 Loading Translation Files');
    
    try {
      const enJson = JSON.parse(fs.readFileSync(this.enPath, 'utf8'));
      const faJson = JSON.parse(fs.readFileSync(this.faPath, 'utf8'));
      
      const enKeys = this.extractKeys(enJson);
      const faKeys = this.extractKeys(faJson);
      
      enKeys.forEach(key => this.enKeys.add(key));
      faKeys.forEach(key => this.faKeys.add(key));
      
      this.stats.totalEnKeys = this.enKeys.size;
      this.stats.totalFaKeys = this.faKeys.size;
      
      this.log(`✓ English (en.json): ${this.stats.totalEnKeys} keys`, 'green');
      this.log(`✓ Persian (fa.json): ${this.stats.totalFaKeys} keys`, 'green');
      
      return { enJson, faJson };
    } catch (error) {
      this.log(`✗ Error loading translation files: ${error.message}`, 'red');
      throw error;
    }
  }

  /**
   * Compare translation files and find missing keys
   */
  compareTranslations() {
    this.logSection('🔍 Comparing Translation Files');
    
    // Find keys in English but missing in Persian
    for (const key of this.enKeys) {
      if (!this.faKeys.has(key)) {
        this.issues.missingInFa.push(key);
      }
    }
    
    // Find keys in Persian but missing in English
    for (const key of this.faKeys) {
      if (!this.enKeys.has(key)) {
        this.issues.missingInEn.push(key);
      }
    }
    
    if (this.issues.missingInFa.length === 0 && this.issues.missingInEn.length === 0) {
      this.log('✓ Perfect match! All keys exist in both files.', 'green');
    } else {
      if (this.issues.missingInFa.length > 0) {
        this.log(`⚠ ${this.issues.missingInFa.length} keys missing in Persian (fa.json)`, 'yellow');
      }
      if (this.issues.missingInEn.length > 0) {
        this.log(`⚠ ${this.issues.missingInEn.length} keys missing in English (en.json)`, 'yellow');
      }
    }
  }

  /**
   * Scan TypeScript/TSX files for translation key usage
   */
  scanCodeForKeys() {
    this.logSection('🔎 Scanning Code for Translation Usage');
    
    try {
      // Find all .ts and .tsx files
      const files = execSync(
        `find "${this.srcPath}" -type f \\( -name "*.ts" -o -name "*.tsx" \\) ! -path "*/node_modules/*"`,
        { encoding: 'utf8' }
      )
        .trim()
        .split('\n')
        .filter(Boolean);
      
      this.stats.filesScanned = files.length;
      this.log(`Found ${files.length} TypeScript files to scan`, 'cyan');
      
      // Regex patterns to match translation key usage
      const patterns = [
        /\bt\(['"]([^'"]+)['"]/g,           // t('key')
        /\bt\("([^"]+)"/g,                   // t("key")
        /i18n\.t\(['"]([^'"]+)['"]/g,       // i18n.t('key')
        /i18n\.t\("([^"]+)"/g,               // i18n.t("key")
        /useTranslation.*?t\(['"]([^'"]+)['"]/gs, // const { t } = useTranslation(); ... t('key')
      ];
      
      // Additional patterns to detect dynamic key usage (for reporting, not validation)
      const dynamicKeyPatterns = [
        /\bt\([^'")\n]+\.([a-zA-Z]+Key)\)/g,       // t(item.textKey), t(config.labelKey)
        /\bt\(`([^`]+)\$\{[^}]+\}[^`]*`\)/g,       // t(`prefix.${dynamic}`)
        /textKey:\s*['"]([^'"]+)['"]/g,             // textKey: 'nav.dashboard'
        /labelKey:\s*['"]([^'"]+)['"]/g,            // labelKey: 'common.save'
        /titleKey:\s*['"]([^'"]+)['"]/g,            // titleKey: 'page.title'
        /messageKey:\s*['"]([^'"]+)['"]/g,          // messageKey: 'errors.failed'
        /translationKey:\s*['"]([^'"]+)['"]/g,      // translationKey: 'key'
      ];
      
      let keysFound = 0;
      let dynamicKeysFound = 0;
      
      files.forEach(file => {
        try {
          const content = fs.readFileSync(file, 'utf8');
          
          // Process direct t('key') patterns
          patterns.forEach(pattern => {
            let match;
            while ((match = pattern.exec(content)) !== null) {
              const key = match[1];
              // Skip interpolated/dynamic keys and false positives
              if (!key.includes('${') && !key.includes('{') && key.length > 1 && this.isValidTranslationKey(key)) {
                this.usedKeys.add(key);
                keysFound++;
              }
            }
          });
          
          // Process dynamic key patterns (textKey, labelKey, etc.)
          dynamicKeyPatterns.forEach(pattern => {
            let match;
            while ((match = pattern.exec(content)) !== null) {
              const key = match[1];
              // These are typically full keys like 'nav.dashboard'
              if (key && !key.includes('${') && key.includes('.') && this.isValidTranslationKey(key)) {
                this.usedKeys.add(key);
                dynamicKeysFound++;
              }
            }
          });
        } catch (error) {
          // Skip files that can't be read
        }
      });
      
      this.stats.totalUsedKeys = this.usedKeys.size;
      this.log(`✓ Found ${keysFound} direct translation calls using ${this.usedKeys.size} unique keys`, 'green');
      if (dynamicKeysFound > 0) {
        this.log(`✓ Found ${dynamicKeysFound} additional keys via dynamic patterns (textKey, labelKey, etc.)`, 'green');
      }
      
    } catch (error) {
      this.log(`✗ Error scanning code: ${error.message}`, 'red');
    }
  }

  /**
   * Cross-reference code usage with translation files
   */
  crossReference() {
    this.logSection('🔗 Cross-Referencing Code Usage with Translation Files');
    
    for (const key of this.usedKeys) {
      const inEn = this.enKeys.has(key);
      const inFa = this.faKeys.has(key);
      
      if (!inEn && !inFa) {
        this.issues.missingInBoth.push(key);
      } else if (!inEn) {
        // Key used in code, exists in fa.json but missing in en.json (unusual)
        if (!this.issues.missingInEn.includes(key)) {
          this.issues.missingInEn.push(key);
        }
      } else if (!inFa) {
        // Key used in code, exists in en.json but missing in fa.json
        if (!this.issues.missingInFa.includes(key)) {
          this.issues.missingInFa.push(key);
        }
      }
    }
    
    // Detect potential naming mismatches (e.g., lowRiskLabel vs lowRisk)
    for (const usedKey of this.usedKeys) {
      if (!this.enKeys.has(usedKey)) {
        // Check if there's a similar key without common suffixes
        const withoutSuffix = usedKey
          .replace(/Label$/, '')
          .replace(/Title$/, '')
          .replace(/Text$/, '')
          .replace(/Desc$/, '');
        
        if (withoutSuffix !== usedKey && this.enKeys.has(withoutSuffix)) {
          this.issues.potentialMismatches.push({
            used: usedKey,
            suggested: withoutSuffix,
          });
        }
      }
    }
    
    if (this.issues.missingInBoth.length === 0 && this.issues.potentialMismatches.length === 0) {
      this.log('✓ All keys used in code are defined in translation files.', 'green');
    } else {
      if (this.issues.missingInBoth.length > 0) {
        this.log(`✗ ${this.issues.missingInBoth.length} keys used in code but missing in both translation files`, 'red');
      }
      if (this.issues.potentialMismatches.length > 0) {
        this.log(`⚠ ${this.issues.potentialMismatches.length} potential naming mismatches detected`, 'yellow');
      }
    }
  }

  /**
   * Generate detailed report
   */
  generateReport() {
    this.logSection('📊 Detailed Report');
    
    // Summary
    console.log('\n📈 Summary:');
    console.log(`  English keys:        ${this.stats.totalEnKeys}`);
    console.log(`  Persian keys:        ${this.stats.totalFaKeys}`);
    console.log(`  Keys used in code:   ${this.stats.totalUsedKeys}`);
    console.log(`  Files scanned:       ${this.stats.filesScanned}`);
    
    // Missing in Persian
    if (this.issues.missingInFa.length > 0) {
      console.log('\n❌ Keys Missing in Persian (fa.json):');
      this.issues.missingInFa.slice(0, 50).forEach(key => {
        this.log(`  • ${key}`, 'red');
      });
      if (this.issues.missingInFa.length > 50) {
        this.log(`  ... and ${this.issues.missingInFa.length - 50} more`, 'yellow');
      }
    }
    
    // Missing in English
    if (this.issues.missingInEn.length > 0) {
      console.log('\n❌ Keys Missing in English (en.json):');
      this.issues.missingInEn.slice(0, 50).forEach(key => {
        this.log(`  • ${key}`, 'red');
      });
      if (this.issues.missingInEn.length > 50) {
        this.log(`  ... and ${this.issues.missingInEn.length - 50} more`, 'yellow');
      }
    }
    
    // Missing in both
    if (this.issues.missingInBoth.length > 0) {
      console.log('\n❌ Keys Used in Code but Missing in Both Translation Files:');
      this.issues.missingInBoth.forEach(key => {
        this.log(`  • ${key}`, 'red');
      });
    }
    
    // Potential mismatches
    if (this.issues.potentialMismatches.length > 0) {
      console.log('\n⚠️  Potential Naming Mismatches:');
      this.issues.potentialMismatches.forEach(({ used, suggested }) => {
        this.log(`  • Code uses: "${used}"`, 'yellow');
        this.log(`    Suggestion: "${suggested}" (exists in translation files)`, 'cyan');
      });
    }
    
    // Unused keys (exist in translations but not used in code)
    const unusedKeys = [];
    for (const key of this.enKeys) {
      if (!this.usedKeys.has(key)) {
        unusedKeys.push(key);
      }
    }
    
    if (unusedKeys.length > 0) {
      console.log(`\n📦 Keys Defined but Not Used in Code: ${unusedKeys.length}`);
      if (unusedKeys.length <= 20) {
        unusedKeys.forEach(key => {
          this.log(`  • ${key}`, 'cyan');
        });
      } else {
        this.log(`  (Run with --show-unused to see all unused keys)`, 'cyan');
      }
    }
    
    // Write JSON report
    const reportPath = path.join(this.projectRoot, 'i18n-validation-report.json');
    const report = {
      timestamp: new Date().toISOString(),
      stats: this.stats,
      issues: {
        missingInFa: this.issues.missingInFa,
        missingInEn: this.issues.missingInEn,
        missingInBoth: this.issues.missingInBoth,
        potentialMismatches: this.issues.potentialMismatches,
      },
      unusedKeys: unusedKeys,
    };
    
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    this.log(`\n💾 Full report saved to: ${reportPath}`, 'green');
  }

  /**
   * Run the validation
   */
  async run() {
    this.log('\n🚀 i18n Translation Validator', 'bright');
    this.log('━'.repeat(80), 'cyan');
    
    try {
      this.loadTranslations();
      const duplicateCount = this.checkDuplicateKeys();
      this.compareTranslations();
      this.scanCodeForKeys();
      this.crossReference();
      this.generateReport();
      
      // Exit code based on issues
      const hasErrors = 
        this.issues.missingInBoth.length > 0 ||
        this.issues.potentialMismatches.length > 0 ||
        duplicateCount > 0; // Duplicate keys are now a critical error
      
      const hasWarnings = 
        this.issues.missingInFa.length > 0 ||
        this.issues.missingInEn.length > 0;
      
      this.logSection('🏁 Validation Complete');
      
      if (hasErrors) {
        this.log('Status: FAILED - Critical issues found', 'red');
        if (duplicateCount > 0) {
          this.log(`  → ${duplicateCount} duplicate key(s) detected (JSON allows only unique keys)`, 'red');
        }
        process.exit(1);
      } else if (hasWarnings) {
        this.log('Status: WARNING - Translation gaps detected', 'yellow');
        process.exit(0);
      } else {
        this.log('Status: PASSED - All validations successful', 'green');
        process.exit(0);
      }
      
    } catch (error) {
      this.log(`\n✗ Validation failed: ${error.message}`, 'red');
      console.error(error);
      process.exit(1);
    }
  }
}

// Run the validator
const validator = new I18nValidator();
validator.run();
