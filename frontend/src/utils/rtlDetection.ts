/**
 * RTL (Right-to-Left) text detection utility
 * 
 * Detects if a string contains RTL characters and should be displayed
 * with right-to-left text direction. Supports Arabic, Hebrew, Persian (Farsi),
 * Urdu, and other RTL scripts.
 */

// Unicode ranges for RTL scripts
const RTL_RANGES = [
  // Arabic (0600–06FF)
  [0x0600, 0x06FF],
  // Arabic Supplement (0750–077F)
  [0x0750, 0x077F],
  // Arabic Extended-A (08A0–08FF)
  [0x08A0, 0x08FF],
  // Arabic Presentation Forms-A (FB50–FDFF)
  [0xFB50, 0xFDFF],
  // Arabic Presentation Forms-B (FE70–FEFF)
  [0xFE70, 0xFEFF],
  // Hebrew (0590–05FF)
  [0x0590, 0x05FF],
  // Hebrew Presentation Forms (FB1D–FB4F)
  [0xFB1D, 0xFB4F],
  // Syriac (0700–074F)
  [0x0700, 0x074F],
  // Thaana (0780–07BF)
  [0x0780, 0x07BF],
  // N'Ko (07C0–07FF)
  [0x07C0, 0x07FF],
];

/**
 * Check if a character code is within RTL Unicode ranges
 */
function isRTLCharCode(charCode: number): boolean {
  return RTL_RANGES.some(([start, end]) => charCode >= start && charCode <= end);
}

/**
 * Detect if text contains RTL characters
 * 
 * @param text - The text to analyze
 * @returns true if the text contains RTL characters
 */
export function containsRTL(text: string): boolean {
  if (!text) return false;
  
  for (let i = 0; i < text.length; i++) {
    if (isRTLCharCode(text.charCodeAt(i))) {
      return true;
    }
  }
  return false;
}

/**
 * Detect text direction based on the first strong directional character
 * 
 * This follows the Unicode Bidirectional Algorithm (UBA) first-strong heuristic.
 * It finds the first character with strong directionality and returns its direction.
 * 
 * @param text - The text to analyze
 * @returns 'rtl' if the text should be displayed right-to-left, 'ltr' otherwise
 */
export function detectTextDirection(text: string): 'rtl' | 'ltr' {
  if (!text) return 'ltr';
  
  // Strip leading whitespace and numbers
  const stripped = text.replace(/^[\s\d\p{P}]+/u, '');
  
  // Find first strong character
  for (let i = 0; i < stripped.length; i++) {
    const charCode = stripped.charCodeAt(i);
    
    // Skip neutral/weak characters (numbers, punctuation, spaces)
    if (isNeutralCharacter(charCode)) {
      continue;
    }
    
    // Check if it's RTL
    if (isRTLCharCode(charCode)) {
      return 'rtl';
    }
    
    // If it's a strong LTR character (Latin, etc.), return LTR
    if (isStrongLTRCharCode(charCode)) {
      return 'ltr';
    }
  }
  
  return 'ltr';
}

/**
 * Check if a character is neutral (doesn't have inherent directionality)
 */
function isNeutralCharacter(charCode: number): boolean {
  // Common neutral ranges
  return (
    // Basic punctuation and symbols
    (charCode >= 0x0020 && charCode <= 0x0040) ||
    (charCode >= 0x005B && charCode <= 0x0060) ||
    (charCode >= 0x007B && charCode <= 0x007E) ||
    // Numbers
    (charCode >= 0x0030 && charCode <= 0x0039) ||
    // General punctuation
    (charCode >= 0x2000 && charCode <= 0x206F)
  );
}

/**
 * Check if a character is a strong LTR character
 */
function isStrongLTRCharCode(charCode: number): boolean {
  // Latin Basic (A-Z, a-z)
  return (
    (charCode >= 0x0041 && charCode <= 0x005A) ||
    (charCode >= 0x0061 && charCode <= 0x007A) ||
    // Latin Extended (covers most European languages)
    (charCode >= 0x00C0 && charCode <= 0x024F) ||
    // Greek
    (charCode >= 0x0370 && charCode <= 0x03FF) ||
    // Cyrillic
    (charCode >= 0x0400 && charCode <= 0x04FF) ||
    // CJK characters are typically treated as LTR
    (charCode >= 0x4E00 && charCode <= 0x9FFF)
  );
}

/**
 * Calculate the percentage of RTL characters in a string
 * 
 * @param text - The text to analyze
 * @returns Percentage (0-100) of RTL characters
 */
export function getRTLPercentage(text: string): number {
  if (!text) return 0;
  
  let rtlCount = 0;
  let totalStrongChars = 0;
  
  for (let i = 0; i < text.length; i++) {
    const charCode = text.charCodeAt(i);
    
    if (isNeutralCharacter(charCode)) {
      continue;
    }
    
    totalStrongChars++;
    
    if (isRTLCharCode(charCode)) {
      rtlCount++;
    }
  }
  
  return totalStrongChars === 0 ? 0 : (rtlCount / totalStrongChars) * 100;
}

/**
 * Get CSS direction style based on text content
 * 
 * @param text - The text to analyze
 * @returns CSS direction property value
 */
export function getTextDirectionStyle(text: string): React.CSSProperties {
  const direction = detectTextDirection(text);
  return {
    direction,
    textAlign: direction === 'rtl' ? 'right' : 'left',
  };
}

/**
 * Get HTML dir attribute value based on text content
 * 
 * @param text - The text to analyze
 * @returns 'rtl', 'ltr', or 'auto'
 */
export function getDirAttribute(text: string): 'rtl' | 'ltr' | 'auto' {
  return detectTextDirection(text);
}

export default {
  containsRTL,
  detectTextDirection,
  getRTLPercentage,
  getTextDirectionStyle,
  getDirAttribute,
};
