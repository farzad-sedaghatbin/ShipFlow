/**
 * ShipFlow Theme
 * 
 * A warm, analog aesthetic inspired by sketching and tactile design.
 * Colors evoke paper, ink, pencil marks, and sticky notes.
 */

// Brand Colors - Warm, Analog Palette
export const palette = {
  // Warm terracotta orange - primary actions, progress
  terracotta: {
    main: '#E07A5F',
    light: '#F4A582',
    dark: '#C4563D',
    contrastText: '#FFFFFF',
  },
  // Deep ink blue - secondary actions, headers
  ink: {
    main: '#3D5A80',
    light: '#5A7CA0',
    dark: '#2B4157',
    contrastText: '#FFFFFF',
  },
  // Sage green - success states
  sage: {
    main: '#81B29A',
    light: '#A7D4BC',
    dark: '#5A8F78',
    contrastText: '#1A1A1A',
  },
  // Golden amber - warnings, highlights
  amber: {
    main: '#F2CC8F',
    light: '#F7DDB4',
    dark: '#D9A84E',
    contrastText: '#1A1A1A',
  },
  // Coral red - errors, destructive
  coral: {
    main: '#E76F51',
    light: '#F09D85',
    dark: '#C4472A',
    contrastText: '#FFFFFF',
  },
  // Paper and canvas backgrounds
  canvas: {
    cream: '#FFFCF7',
    warm: '#FDF8F3',
    paper: '#FFFFFF',
    sketch: '#F7F5F2',
  },
  // Ink and pencil text
  text: {
    charcoal: '#2D3142',
    graphite: '#4F5D75',
    pencil: '#8D99AE',
    faded: '#BFC0C0',
  },
};

// Alpha utility function for color transparency
export function alpha(color: string, opacity: number): string {
  // Handle hex colors
  if (color.startsWith('#')) {
    const hex = color.slice(1);
    const r = parseInt(hex.slice(0, 2), 16);
    const g = parseInt(hex.slice(2, 4), 16);
    const b = parseInt(hex.slice(4, 6), 16);
    return `rgba(${r}, ${g}, ${b}, ${opacity})`;
  }
  // Handle rgb/rgba colors
  if (color.startsWith('rgb')) {
    const match = color.match(/\d+/g);
    if (match && match.length >= 3) {
      return `rgba(${match[0]}, ${match[1]}, ${match[2]}, ${opacity})`;
    }
  }
  return color;
}

// Light theme colors (for use with Tailwind CSS variables)
export const lightTheme = {
  background: palette.canvas.cream,
  foreground: palette.text.charcoal,
  primary: palette.terracotta.main,
  primaryForeground: palette.terracotta.contrastText,
  secondary: palette.ink.main,
  secondaryForeground: palette.ink.contrastText,
  accent: palette.amber.main,
  accentForeground: palette.amber.contrastText,
  muted: palette.canvas.sketch,
  mutedForeground: palette.text.pencil,
  success: palette.sage.main,
  successForeground: palette.sage.contrastText,
  warning: palette.amber.main,
  warningForeground: palette.amber.contrastText,
  destructive: palette.coral.main,
  destructiveForeground: palette.coral.contrastText,
  border: '#E5E7EB',
  card: palette.canvas.paper,
  cardForeground: palette.text.charcoal,
};

// Dark theme colors
export const darkTheme = {
  background: '#1A1A2E',
  foreground: '#E0E0E0',
  primary: palette.terracotta.light,
  primaryForeground: '#1A1A2E',
  secondary: palette.ink.light,
  secondaryForeground: '#1A1A2E',
  accent: palette.amber.light,
  accentForeground: '#1A1A2E',
  muted: '#2D2D44',
  mutedForeground: '#9CA3AF',
  success: palette.sage.light,
  successForeground: '#1A1A2E',
  warning: palette.amber.light,
  warningForeground: '#1A1A2E',
  destructive: palette.coral.light,
  destructiveForeground: '#1A1A2E',
  border: '#3D3D5C',
  card: '#242438',
  cardForeground: '#E0E0E0',
};

// Export the palette for use in other components
export { palette as shipFlowPalette };

// Default export for backward compatibility
export default lightTheme;
