import React from 'react';

// Color constants matching Tailwind theme colors
const colors = {
  primary: '#3b82f6', // blue-500
  secondary: '#8b5cf6', // violet-500
  success: '#22c55e', // green-500
  warning: '#f59e0b', // amber-500
  error: '#ef4444', // red-500
  textSecondary: '#6b7280', // gray-500
};

// Helper function to create rgba colors
const alpha = (color: string, opacity: number): string => {
  const hex = color.replace('#', '');
  const r = parseInt(hex.substring(0, 2), 16);
  const g = parseInt(hex.substring(2, 4), 16);
  const b = parseInt(hex.substring(4, 6), 16);
  return `rgba(${r}, ${g}, ${b}, ${opacity})`;
};

// Hook to get theme colors (can be extended to support dark mode)
const useThemeColors = () => colors;

interface IllustrationProps {
  width?: number;
  height?: number;
  className?: string;
}

/**
 * Illustration: Empty Cycles
 * A sketch-style illustration of a circular cycle with dots
 */
export const EmptyCyclesIllustration: React.FC<IllustrationProps> = ({ 
  width = 200, 
  height = 160 
}) => {
  const theme = useThemeColors();
  const primary = theme.primary;
  const secondary = theme.secondary;
  const textSecondary = theme.textSecondary;

  return (
    <svg width={width} height={height} viewBox="0 0 200 160" fill="none">
      {/* Background circle - dashed */}
      <circle 
        cx="100" 
        cy="80" 
        r="55" 
        stroke={alpha(textSecondary, 0.2)} 
        strokeWidth="2" 
        strokeDasharray="8 6"
        fill="none"
      />
      
      {/* Animated rotating arrow */}
      <g transform="translate(100, 80)">
        <path 
          d="M0,-45 A45,45 0 0,1 45,0" 
          stroke={primary} 
          strokeWidth="3" 
          strokeLinecap="round"
          fill="none"
        >
          <animateTransform 
            attributeName="transform" 
            type="rotate" 
            from="0" 
            to="360" 
            dur="8s" 
            repeatCount="indefinite"
          />
        </path>
        <polygon 
          points="42,-8 50,0 42,8" 
          fill={primary}
        >
          <animateTransform 
            attributeName="transform" 
            type="rotate" 
            from="0" 
            to="360" 
            dur="8s" 
            repeatCount="indefinite"
          />
        </polygon>
      </g>

      {/* Calendar/Sprint boxes */}
      <rect x="30" y="110" width="24" height="24" rx="4" fill={alpha(primary, 0.1)} stroke={primary} strokeWidth="1.5"/>
      <rect x="88" y="110" width="24" height="24" rx="4" fill={alpha(secondary, 0.1)} stroke={secondary} strokeWidth="1.5" strokeDasharray="4 3"/>
      <rect x="146" y="110" width="24" height="24" rx="4" fill={alpha(primary, 0.1)} stroke={primary} strokeWidth="1.5" strokeDasharray="4 3"/>
      
      {/* Week dots */}
      <circle cx="38" cy="120" r="2" fill={primary}/>
      <circle cx="46" cy="120" r="2" fill={primary}/>
      <circle cx="38" cy="128" r="2" fill={alpha(primary, 0.4)}/>
      <circle cx="46" cy="128" r="2" fill={alpha(primary, 0.4)}/>

      {/* Center icon */}
      <circle cx="100" cy="80" r="20" fill={alpha(primary, 0.1)}/>
      <text x="100" y="87" textAnchor="middle" fontSize="24">🔄</text>
      
      {/* Floating dots animation */}
      <circle cx="60" cy="50" r="4" fill={primary}>
        <animate attributeName="cy" values="50;45;50" dur="2s" repeatCount="indefinite"/>
        <animate attributeName="opacity" values="1;0.5;1" dur="2s" repeatCount="indefinite"/>
      </circle>
      <circle cx="140" cy="50" r="4" fill={secondary}>
        <animate attributeName="cy" values="50;55;50" dur="2.5s" repeatCount="indefinite"/>
        <animate attributeName="opacity" values="0.5;1;0.5" dur="2.5s" repeatCount="indefinite"/>
      </circle>
    </svg>
  );
};

/**
 * Illustration: Empty Pitches
 * A sketch of sticky notes / idea cards
 */
export const EmptyPitchesIllustration: React.FC<IllustrationProps> = ({ 
  width = 200, 
  height = 160 
}) => {
  const theme = useThemeColors();
  const primary = theme.primary;
  const secondary = theme.secondary;
  const warning = theme.warning;
  const success = theme.success;
  
  return (
    <svg width={width} height={height} viewBox="0 0 200 160" fill="none">
      {/* Sticky note 1 - back */}
      <g transform="rotate(-8, 70, 80)">
        <rect x="40" y="50" width="60" height="70" rx="2" fill={alpha(warning, 0.3)} stroke={warning} strokeWidth="1"/>
        <line x1="50" y1="65" x2="90" y2="65" stroke={alpha(warning, 0.6)} strokeWidth="2" strokeLinecap="round"/>
        <line x1="50" y1="75" x2="80" y2="75" stroke={alpha(warning, 0.4)} strokeWidth="2" strokeLinecap="round"/>
        <line x1="50" y1="85" x2="85" y2="85" stroke={alpha(warning, 0.4)} strokeWidth="2" strokeLinecap="round"/>
      </g>
      
      {/* Sticky note 2 - middle */}
      <g transform="rotate(5, 100, 80)">
        <rect x="70" y="40" width="60" height="70" rx="2" fill={alpha(success, 0.3)} stroke={success} strokeWidth="1"/>
        <line x1="80" y1="55" x2="120" y2="55" stroke={alpha(success, 0.6)} strokeWidth="2" strokeLinecap="round"/>
        <line x1="80" y1="65" x2="110" y2="65" stroke={alpha(success, 0.4)} strokeWidth="2" strokeLinecap="round"/>
        <circle cx="115" cy="95" r="8" fill={alpha(success, 0.2)} stroke={success} strokeWidth="1"/>
        <text x="115" y="99" textAnchor="middle" fontSize="10">✓</text>
      </g>
      
      {/* Sticky note 3 - front (main) */}
      <g transform="rotate(-2, 130, 80)">
        <rect x="100" y="45" width="70" height="80" rx="3" fill={alpha(primary, 0.15)} stroke={primary} strokeWidth="2"/>
        <line x1="112" y1="62" x2="158" y2="62" stroke={primary} strokeWidth="2" strokeLinecap="round"/>
        <line x1="112" y1="75" x2="150" y2="75" stroke={alpha(primary, 0.5)} strokeWidth="2" strokeLinecap="round"/>
        <line x1="112" y1="88" x2="155" y2="88" stroke={alpha(primary, 0.5)} strokeWidth="2" strokeLinecap="round"/>
        
        {/* Pin/pushpin */}
        <circle cx="135" cy="45" r="6" fill={secondary}/>
        <ellipse cx="135" cy="45" rx="3" ry="2" fill={alpha('#fff', 0.4)}/>
      </g>
      
      {/* Floating lightbulb */}
      <g transform="translate(160, 30)">
        <text fontSize="24">💡</text>
        <animate attributeName="opacity" values="1;0.6;1" dur="2s" repeatCount="indefinite"/>
      </g>
      
      {/* Plus sign hint */}
      <circle cx="45" cy="130" r="15" fill={alpha(primary, 0.1)} stroke={primary} strokeWidth="2" strokeDasharray="4 3">
        <animate attributeName="stroke-dashoffset" values="0;14" dur="1s" repeatCount="indefinite"/>
      </circle>
      <text x="45" y="136" textAnchor="middle" fontSize="20" fill={primary}>+</text>
    </svg>
  );
};

/**
 * Illustration: Empty Teams
 * People silhouettes in a collaborative arrangement
 */
export const EmptyTeamsIllustration: React.FC<IllustrationProps> = ({ 
  width = 200, 
  height = 160 
}) => {
  const theme = useThemeColors();
  const primary = theme.primary;
  const secondary = theme.secondary;
  const success = theme.success;
  
  return (
    <svg width={width} height={height} viewBox="0 0 200 160" fill="none">
      {/* Connection lines */}
      <line x1="65" y1="70" x2="135" y2="70" stroke={alpha(primary, 0.2)} strokeWidth="2" strokeDasharray="6 4">
        <animate attributeName="stroke-dashoffset" values="0;20" dur="2s" repeatCount="indefinite"/>
      </line>
      <line x1="100" y1="45" x2="100" y2="95" stroke={alpha(primary, 0.2)} strokeWidth="2" strokeDasharray="6 4">
        <animate attributeName="stroke-dashoffset" values="0;20" dur="2s" repeatCount="indefinite"/>
      </line>
      
      {/* Person 1 - Left */}
      <g transform="translate(50, 55)">
        <circle cx="0" cy="0" r="18" fill={alpha(primary, 0.15)} stroke={primary} strokeWidth="2"/>
        <circle cx="0" cy="-4" r="7" fill={primary}/>
        <path d="M-10,8 Q0,14 10,8" stroke={primary} strokeWidth="3" strokeLinecap="round" fill="none"/>
      </g>
      
      {/* Person 2 - Right */}
      <g transform="translate(150, 55)">
        <circle cx="0" cy="0" r="18" fill={alpha(secondary, 0.15)} stroke={secondary} strokeWidth="2"/>
        <circle cx="0" cy="-4" r="7" fill={secondary}/>
        <path d="M-10,8 Q0,14 10,8" stroke={secondary} strokeWidth="3" strokeLinecap="round" fill="none"/>
      </g>
      
      {/* Person 3 - Top */}
      <g transform="translate(100, 30)">
        <circle cx="0" cy="0" r="16" fill={alpha(success, 0.15)} stroke={success} strokeWidth="2"/>
        <circle cx="0" cy="-3" r="6" fill={success}/>
        <path d="M-8,6 Q0,11 8,6" stroke={success} strokeWidth="2.5" strokeLinecap="round" fill="none"/>
      </g>
      
      {/* Person 4 - Bottom (highlighted/empty slot) */}
      <g transform="translate(100, 110)">
        <circle cx="0" cy="0" r="18" fill="none" stroke={alpha(primary, 0.3)} strokeWidth="2" strokeDasharray="5 4"/>
        <text x="0" y="6" textAnchor="middle" fontSize="16" fill={alpha(primary, 0.5)}>?</text>
        <animate attributeName="opacity" values="1;0.5;1" dur="2s" repeatCount="indefinite"/>
      </g>
      
      {/* Collaboration sparkles */}
      <text x="80" y="70" fontSize="12">✨</text>
      <text x="115" y="50" fontSize="10">✨</text>
    </svg>
  );
};

/**
 * Illustration: Empty Hill Chart
 * A stylized hill with animated dots climbing
 */
export const EmptyHillChartIllustration: React.FC<IllustrationProps> = ({ 
  width = 200, 
  height = 160 
}) => {
  const theme = useThemeColors();
  const primary = theme.primary;
  const secondary = theme.secondary;
  const success = theme.success;
  const warning = theme.warning;
  
  return (
    <svg width={width} height={height} viewBox="0 0 200 160" fill="none">
      {/* Background grid pattern */}
      <defs>
        <pattern id="hillGrid" width="20" height="20" patternUnits="userSpaceOnUse">
          <path d="M 20 0 L 0 0 0 20" fill="none" stroke={alpha(primary, 0.06)} strokeWidth="0.5"/>
        </pattern>
        
        {/* Gradient for the hill */}
        <linearGradient id="hillGradient" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor={alpha(primary, 0.15)}/>
          <stop offset="100%" stopColor={alpha(primary, 0.02)}/>
        </linearGradient>
        
        {/* Glow filter for dots */}
        <filter id="dotGlow" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
          <feMerge>
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
      </defs>
      
      {/* Grid background */}
      <rect width="200" height="160" fill="url(#hillGrid)"/>
      
      {/* Hill fill area */}
      <path 
        d="M15,115 Q100,15 185,115 L185,130 L15,130 Z" 
        fill="url(#hillGradient)"
      />
      
      {/* Hill shadow */}
      <path 
        d="M15,118 Q100,20 185,118" 
        stroke={alpha(primary, 0.12)} 
        strokeWidth="10" 
        fill="none"
        strokeLinecap="round"
      />
      
      {/* Main hill curve */}
      <path 
        d="M15,115 Q100,15 185,115" 
        stroke={primary} 
        strokeWidth="4" 
        fill="none"
        strokeLinecap="round"
      />
      
      {/* Center peak dashed line */}
      <line 
        x1="100" y1="25" x2="100" y2="115" 
        stroke={alpha(secondary, 0.25)} 
        strokeWidth="2" 
        strokeDasharray="6 4"
      />
      
      {/* Peak marker */}
      <circle cx="100" cy="22" r="4" fill={alpha(secondary, 0.3)}/>
      
      {/* Labels */}
      <text x="55" y="140" textAnchor="middle" fontSize="9" fill={alpha(primary, 0.6)} fontWeight="600">
        🔍 Figuring out
      </text>
      <text x="145" y="140" textAnchor="middle" fontSize="9" fill={alpha(primary, 0.6)} fontWeight="600">
        🚀 Making it
      </text>
      
      {/* Animated Dot 1 - Climbing up the left side */}
      <g filter="url(#dotGlow)">
        <circle r="7" fill={primary}>
          <animateMotion 
            dur="6s" 
            repeatCount="indefinite"
            path="M30,98 Q50,70 70,50"
            keyPoints="0;1;1;0"
            keyTimes="0;0.4;0.6;1"
            calcMode="spline"
            keySplines="0.4 0 0.2 1; 0.4 0 0.2 1; 0.4 0 0.2 1"
          />
          <animate attributeName="opacity" values="0.4;1;1;0.4" dur="6s" repeatCount="indefinite"/>
        </circle>
        <circle r="4" fill="white" opacity="0.4">
          <animateMotion 
            dur="6s" 
            repeatCount="indefinite"
            path="M30,98 Q50,70 70,50"
            keyPoints="0;1;1;0"
            keyTimes="0;0.4;0.6;1"
            calcMode="spline"
            keySplines="0.4 0 0.2 1; 0.4 0 0.2 1; 0.4 0 0.2 1"
          />
        </circle>
      </g>
      
      {/* Animated Dot 2 - Going over the peak */}
      <g filter="url(#dotGlow)">
        <circle r="8" fill={success}>
          <animateMotion 
            dur="8s" 
            repeatCount="indefinite"
            path="M60,60 Q80,30 100,22 Q120,30 140,60"
            keyPoints="0;0.5;1;1;0"
            keyTimes="0;0.35;0.7;0.85;1"
            calcMode="spline"
            keySplines="0.4 0 0.2 1; 0.4 0 0.2 1; 0.4 0 0.2 1; 0.4 0 0.2 1"
          />
          <animate attributeName="opacity" values="0.5;1;1;0.5;0.5" dur="8s" repeatCount="indefinite"/>
        </circle>
        <circle r="4.5" fill="white" opacity="0.5">
          <animateMotion 
            dur="8s" 
            repeatCount="indefinite"
            path="M60,60 Q80,30 100,22 Q120,30 140,60"
            keyPoints="0;0.5;1;1;0"
            keyTimes="0;0.35;0.7;0.85;1"
            calcMode="spline"
            keySplines="0.4 0 0.2 1; 0.4 0 0.2 1; 0.4 0 0.2 1; 0.4 0 0.2 1"
          />
        </circle>
      </g>
      
      {/* Animated Dot 3 - Rolling down the right side */}
      <g filter="url(#dotGlow)">
        <circle r="6" fill={warning}>
          <animateMotion 
            dur="5s" 
            repeatCount="indefinite"
            path="M130,45 Q155,75 175,100"
            keyPoints="0;1;1;0"
            keyTimes="0;0.5;0.7;1"
            calcMode="spline"
            keySplines="0.4 0 0.2 1; 0.4 0 0.2 1; 0.4 0 0.2 1"
          />
          <animate attributeName="opacity" values="0.6;1;1;0.6" dur="5s" repeatCount="indefinite"/>
        </circle>
        <circle r="3.5" fill="white" opacity="0.4">
          <animateMotion 
            dur="5s" 
            repeatCount="indefinite"
            path="M130,45 Q155,75 175,100"
            keyPoints="0;1;1;0"
            keyTimes="0;0.5;0.7;1"
            calcMode="spline"
            keySplines="0.4 0 0.2 1; 0.4 0 0.2 1; 0.4 0 0.2 1"
          />
        </circle>
      </g>
      
      {/* Static dot at start - waiting */}
      <g>
        <circle cx="25" cy="108" r="5" fill={alpha(secondary, 0.3)} stroke={secondary} strokeWidth="1.5" strokeDasharray="3 2">
          <animate attributeName="stroke-dashoffset" values="0;10" dur="1s" repeatCount="indefinite"/>
        </circle>
      </g>
      
      {/* Static dot at end - completed */}
      <g>
        <circle cx="180" cy="108" r="6" fill={success}/>
        <circle cx="180" cy="108" r="3" fill="white" opacity="0.5"/>
        <text x="180" y="111" textAnchor="middle" fontSize="6" fill="white">✓</text>
      </g>
      
      {/* Trail sparkles */}
      <circle cx="50" cy="80" r="1.5" fill={alpha(primary, 0.4)}>
        <animate attributeName="opacity" values="0;1;0" dur="2s" repeatCount="indefinite"/>
      </circle>
      <circle cx="90" cy="35" r="1.5" fill={alpha(success, 0.4)}>
        <animate attributeName="opacity" values="0;1;0" dur="2.5s" repeatCount="indefinite" begin="0.5s"/>
      </circle>
      <circle cx="150" cy="55" r="1.5" fill={alpha(warning, 0.4)}>
        <animate attributeName="opacity" values="0;1;0" dur="1.8s" repeatCount="indefinite" begin="1s"/>
      </circle>
    </svg>
  );
};

/**
 * Illustration: Getting Started / Welcome
 * A rocket launching with stars
 */
export const WelcomeIllustration: React.FC<IllustrationProps> = ({ 
  width = 240, 
  height = 180 
}) => {
  const theme = useThemeColors();
  const primary = theme.primary;
  const secondary = theme.secondary;
  const warning = theme.warning;
  
  return (
    <svg width={width} height={height} viewBox="0 0 240 180" fill="none">
      {/* Stars background */}
      <circle cx="30" cy="40" r="2" fill={warning}>
        <animate attributeName="opacity" values="1;0.3;1" dur="1.5s" repeatCount="indefinite"/>
      </circle>
      <circle cx="210" cy="30" r="2.5" fill={warning}>
        <animate attributeName="opacity" values="0.3;1;0.3" dur="2s" repeatCount="indefinite"/>
      </circle>
      <circle cx="50" cy="100" r="1.5" fill={warning}>
        <animate attributeName="opacity" values="0.5;1;0.5" dur="1.8s" repeatCount="indefinite"/>
      </circle>
      <circle cx="190" cy="80" r="2" fill={warning}>
        <animate attributeName="opacity" values="1;0.5;1" dur="2.2s" repeatCount="indefinite"/>
      </circle>
      <circle cx="170" cy="140" r="1.5" fill={warning}>
        <animate attributeName="opacity" values="0.7;1;0.7" dur="1.6s" repeatCount="indefinite"/>
      </circle>

      {/* Rocket */}
      <g transform="translate(120, 90) rotate(-45)">
        {/* Flame */}
        <ellipse cx="0" cy="55" rx="8" ry="20" fill={warning}>
          <animate attributeName="ry" values="20;25;20" dur="0.3s" repeatCount="indefinite"/>
          <animate attributeName="opacity" values="1;0.7;1" dur="0.3s" repeatCount="indefinite"/>
        </ellipse>
        <ellipse cx="0" cy="55" rx="5" ry="15" fill={alpha('#fff', 0.8)}>
          <animate attributeName="ry" values="15;18;15" dur="0.3s" repeatCount="indefinite"/>
        </ellipse>
        
        {/* Rocket body */}
        <path 
          d="M-15,40 L-15,0 Q-15,-25 0,-40 Q15,-25 15,0 L15,40 Z" 
          fill={primary}
        />
        <path 
          d="M-10,-10 L-10,0 Q-10,-20 0,-30 Q10,-20 10,0 L10,-10 Z" 
          fill={alpha('#fff', 0.3)}
        />
        
        {/* Window */}
        <circle cx="0" cy="0" r="8" fill={secondary}/>
        <circle cx="0" cy="0" r="5" fill={alpha('#fff', 0.4)}/>
        
        {/* Fins */}
        <path d="M-15,25 L-28,45 L-15,40 Z" fill={secondary}/>
        <path d="M15,25 L28,45 L15,40 Z" fill={secondary}/>
      </g>

      {/* Trail particles */}
      <circle cx="165" cy="135" r="4" fill={alpha(warning, 0.6)}>
        <animate attributeName="cy" values="135;150;135" dur="1s" repeatCount="indefinite"/>
        <animate attributeName="opacity" values="0.6;0;0.6" dur="1s" repeatCount="indefinite"/>
      </circle>
      <circle cx="175" cy="145" r="3" fill={alpha(warning, 0.4)}>
        <animate attributeName="cy" values="145;165;145" dur="1.2s" repeatCount="indefinite"/>
        <animate attributeName="opacity" values="0.4;0;0.4" dur="1.2s" repeatCount="indefinite"/>
      </circle>

      {/* Target/goal */}
      <g transform="translate(70, 60)">
        <circle cx="0" cy="0" r="20" fill="none" stroke={alpha(primary, 0.2)} strokeWidth="2"/>
        <circle cx="0" cy="0" r="12" fill="none" stroke={alpha(primary, 0.3)} strokeWidth="2"/>
        <circle cx="0" cy="0" r="4" fill={primary}/>
      </g>
    </svg>
  );
};

/**
 * Illustration: No Data / Empty Results
 * A magnifying glass with nothing found
 */
export const NoDataIllustration: React.FC<IllustrationProps> = ({ 
  width = 200, 
  height = 160 
}) => {
  const theme = useThemeColors();
  const primary = theme.primary;
  const textSecondary = theme.textSecondary;
  
  return (
    <svg width={width} height={height} viewBox="0 0 200 160" fill="none">
      {/* Magnifying glass */}
      <circle 
        cx="85" 
        cy="65" 
        r="35" 
        fill={alpha(primary, 0.05)} 
        stroke={primary} 
        strokeWidth="4"
      />
      <line 
        x1="112" 
        y1="92" 
        x2="145" 
        y2="125" 
        stroke={primary} 
        strokeWidth="6" 
        strokeLinecap="round"
      />
      
      {/* Empty/question inside */}
      <text x="85" y="75" textAnchor="middle" fontSize="30" fill={alpha(textSecondary, 0.4)}>?</text>
      
      {/* Floating search dots */}
      <circle cx="50" cy="40" r="3" fill={alpha(primary, 0.3)}>
        <animate attributeName="cy" values="40;35;40" dur="2s" repeatCount="indefinite"/>
      </circle>
      <circle cx="130" cy="35" r="2" fill={alpha(primary, 0.2)}>
        <animate attributeName="cy" values="35;40;35" dur="2.5s" repeatCount="indefinite"/>
      </circle>
      <circle cx="155" cy="70" r="2.5" fill={alpha(primary, 0.25)}>
        <animate attributeName="cx" values="155;160;155" dur="3s" repeatCount="indefinite"/>
      </circle>
    </svg>
  );
};

/**
 * Illustration: Success / Completed
 * A checkmark with celebration
 */
export const SuccessIllustration: React.FC<IllustrationProps> = ({ 
  width = 200, 
  height = 160 
}) => {
  const theme = useThemeColors();
  const success = theme.success;
  const warning = theme.warning;
  
  return (
    <svg width={width} height={height} viewBox="0 0 200 160" fill="none">
      {/* Confetti */}
      <rect x="40" y="30" width="8" height="8" rx="1" fill={warning} transform="rotate(15, 44, 34)">
        <animate attributeName="y" values="30;50;30" dur="2s" repeatCount="indefinite"/>
        <animate attributeName="opacity" values="1;0;1" dur="2s" repeatCount="indefinite"/>
      </rect>
      <rect x="160" y="25" width="6" height="6" rx="1" fill={success} transform="rotate(-20, 163, 28)">
        <animate attributeName="y" values="25;45;25" dur="2.5s" repeatCount="indefinite"/>
        <animate attributeName="opacity" values="1;0;1" dur="2.5s" repeatCount="indefinite"/>
      </rect>
      <circle cx="55" cy="110" r="4" fill={warning}>
        <animate attributeName="cy" values="110;130;110" dur="1.8s" repeatCount="indefinite"/>
        <animate attributeName="opacity" values="1;0;1" dur="1.8s" repeatCount="indefinite"/>
      </circle>
      <circle cx="150" cy="115" r="3" fill={success}>
        <animate attributeName="cy" values="115;135;115" dur="2.2s" repeatCount="indefinite"/>
        <animate attributeName="opacity" values="1;0;1" dur="2.2s" repeatCount="indefinite"/>
      </circle>

      {/* Main checkmark circle */}
      <circle cx="100" cy="80" r="45" fill={alpha(success, 0.1)} stroke={success} strokeWidth="4"/>
      
      {/* Checkmark */}
      <path 
        d="M75,80 L92,97 L125,64" 
        stroke={success} 
        strokeWidth="6" 
        strokeLinecap="round" 
        strokeLinejoin="round"
        fill="none"
      />
      
      {/* Sparkles */}
      <g>
        <text x="145" y="55" fontSize="16">✨</text>
        <text x="50" y="60" fontSize="14">✨</text>
      </g>
    </svg>
  );
};

export default {
  EmptyCyclesIllustration,
  EmptyPitchesIllustration,
  EmptyTeamsIllustration,
  EmptyHillChartIllustration,
  WelcomeIllustration,
  NoDataIllustration,
  SuccessIllustration,
};

/**
 * Illustration: Login / Authentication
 * A shield with a key and user icon for secure login
 */
export const LoginIllustration: React.FC<IllustrationProps> = ({ 
  width = 280, 
  height = 220 
}) => {
  const theme = useThemeColors();
  const primary = theme.primary;
  const secondary = theme.secondary;
  const warning = theme.warning;
  const success = theme.success;
  
  return (
    <svg width={width} height={height} viewBox="0 0 280 220" fill="none">
      {/* Background decorative elements */}
      <circle cx="50" cy="50" r="3" fill={alpha(primary, 0.2)}>
        <animate attributeName="opacity" values="0.2;0.6;0.2" dur="3s" repeatCount="indefinite"/>
      </circle>
      <circle cx="230" cy="40" r="4" fill={alpha(secondary, 0.3)}>
        <animate attributeName="opacity" values="0.3;0.7;0.3" dur="2.5s" repeatCount="indefinite"/>
      </circle>
      <circle cx="260" cy="140" r="3" fill={alpha(warning, 0.3)}>
        <animate attributeName="opacity" values="0.3;0.6;0.3" dur="3.5s" repeatCount="indefinite"/>
      </circle>
      <circle cx="30" cy="170" r="2" fill={alpha(success, 0.3)}>
        <animate attributeName="opacity" values="0.4;0.8;0.4" dur="2s" repeatCount="indefinite"/>
      </circle>
      
      {/* Shield / Security Icon */}
      <g transform="translate(140, 100)">
        {/* Shield shadow */}
        <path 
          d="M-55,8 L-55,-40 Q-55,-55 0,-75 Q55,-55 55,-40 L55,8 Q55,55 0,85 Q-55,55 -55,8 Z" 
          fill={alpha(primary, 0.1)} 
        />
        
        {/* Main shield */}
        <path 
          d="M-50,5 L-50,-35 Q-50,-50 0,-70 Q50,-50 50,-35 L50,5 Q50,50 0,80 Q-50,50 -50,5 Z" 
          fill={alpha(primary, 0.15)} 
          stroke={primary} 
          strokeWidth="3"
        />
        
        {/* Shield inner glow */}
        <path 
          d="M-35,0 L-35,-25 Q-35,-38 0,-55 Q35,-38 35,-25 L35,0 Q35,35 0,60 Q-35,35 -35,0 Z" 
          fill={alpha(primary, 0.08)}
        />
        
        {/* User icon circle */}
        <circle cx="0" cy="-15" r="22" fill={alpha(secondary, 0.2)} stroke={secondary} strokeWidth="2"/>
        
        {/* User head */}
        <circle cx="0" cy="-22" r="10" fill={secondary}/>
        
        {/* User body */}
        <path d="M-14,-5 Q-14,8 0,10 Q14,8 14,-5" fill={secondary}/>
        
        {/* Checkmark badge */}
        <g transform="translate(25, 25)">
          <circle cx="0" cy="0" r="14" fill={success}/>
          <path d="M-6,0 L-2,5 L7,-5" stroke="white" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
        </g>
      </g>
      
      {/* Floating key icon */}
      <g transform="translate(65, 75)">
        <ellipse cx="0" cy="0" rx="12" ry="12" fill={alpha(warning, 0.2)} stroke={warning} strokeWidth="2"/>
        <circle cx="0" cy="0" r="5" fill="none" stroke={warning} strokeWidth="2"/>
        <rect x="8" y="-2" width="20" height="4" rx="2" fill={warning}/>
        <rect x="22" y="-6" width="4" height="8" rx="1" fill={warning}/>
        <rect x="18" y="-4" width="4" height="6" rx="1" fill={warning}/>
        <animate attributeName="transform" type="translate" values="0,0; 0,-5; 0,0" dur="3s" repeatCount="indefinite"/>
      </g>
      
      {/* Floating lock icon */}
      <g transform="translate(210, 160)">
        <rect x="-12" y="0" width="24" height="18" rx="3" fill={alpha(primary, 0.3)} stroke={primary} strokeWidth="2"/>
        <path d="M-7,-2 L-7,-8 Q-7,-15 0,-15 Q7,-15 7,-8 L7,-2" stroke={primary} strokeWidth="2.5" strokeLinecap="round" fill="none"/>
        <circle cx="0" cy="9" r="3" fill={primary}/>
        <animate attributeName="transform" type="translate" values="0,0; 0,3; 0,0" dur="2.5s" repeatCount="indefinite"/>
      </g>
      
      {/* Sparkles */}
      <text x="100" y="50" fontSize="12">✨</text>
      <text x="200" y="80" fontSize="10">✨</text>
    </svg>
  );
};

/**
 * Illustration: Empty Meetings
 * A calendar with meeting icon
 */
export const EmptyMeetingsIllustration: React.FC<IllustrationProps> = ({ 
  width = 200, 
  height = 160 
}) => {
  const theme = useThemeColors();
  const primary = theme.primary;
  const secondary = theme.secondary;
  const warning = theme.warning;
  
  return (
    <svg width={width} height={height} viewBox="0 0 200 160" fill="none">
      {/* Calendar base */}
      <rect x="40" y="35" width="120" height="100" rx="8" fill={alpha(primary, 0.1)} stroke={primary} strokeWidth="2"/>
      
      {/* Calendar header */}
      <rect x="40" y="35" width="120" height="25" rx="8" fill={primary}/>
      <rect x="40" y="52" width="120" height="8" fill={primary}/>
      
      {/* Calendar rings */}
      <rect x="65" y="28" width="8" height="18" rx="2" fill={alpha(primary, 0.6)}/>
      <rect x="127" y="28" width="8" height="18" rx="2" fill={alpha(primary, 0.6)}/>
      
      {/* Calendar grid lines */}
      <line x1="80" y1="70" x2="80" y2="125" stroke={alpha(primary, 0.15)} strokeWidth="1"/>
      <line x1="120" y1="70" x2="120" y2="125" stroke={alpha(primary, 0.15)} strokeWidth="1"/>
      <line x1="50" y1="90" x2="150" y2="90" stroke={alpha(primary, 0.15)} strokeWidth="1"/>
      <line x1="50" y1="110" x2="150" y2="110" stroke={alpha(primary, 0.15)} strokeWidth="1"/>
      
      {/* Empty day slots with dashed borders */}
      <rect x="55" y="75" width="20" height="12" rx="2" fill="none" stroke={alpha(secondary, 0.3)} strokeWidth="1" strokeDasharray="2 2"/>
      <rect x="95" y="95" width="20" height="12" rx="2" fill="none" stroke={alpha(secondary, 0.3)} strokeWidth="1" strokeDasharray="2 2"/>
      <rect x="125" y="75" width="20" height="12" rx="2" fill="none" stroke={alpha(secondary, 0.3)} strokeWidth="1" strokeDasharray="2 2"/>
      
      {/* Question mark in center */}
      <text x="100" y="108" textAnchor="middle" fontSize="24" fill={alpha(primary, 0.3)}>?</text>
      
      {/* Video call icon floating */}
      <g transform="translate(155, 45)">
        <rect x="0" y="0" width="28" height="20" rx="3" fill={warning}/>
        <polygon points="28,5 38,10 28,15" fill={warning}/>
        <animate attributeName="opacity" values="1;0.6;1" dur="2s" repeatCount="indefinite"/>
      </g>
      
      {/* Plus sign hint */}
      <circle cx="165" cy="130" r="12" fill={alpha(secondary, 0.1)} stroke={secondary} strokeWidth="2" strokeDasharray="4 2">
        <animate attributeName="stroke-dashoffset" values="0;12" dur="1.5s" repeatCount="indefinite"/>
      </circle>
      <text x="165" y="135" textAnchor="middle" fontSize="16" fill={secondary}>+</text>
    </svg>
  );
};

/**
 * Illustration: Empty People / Contacts
 * Person silhouettes with add hint
 */
export const EmptyPeopleIllustration: React.FC<IllustrationProps> = ({ 
  width = 200, 
  height = 160 
}) => {
  const theme = useThemeColors();
  const primary = theme.primary;
  const secondary = theme.secondary;
  const textSecondary = theme.textSecondary;
  
  return (
    <svg width={width} height={height} viewBox="0 0 200 160" fill="none">
      {/* Background card */}
      <rect x="30" y="30" width="140" height="100" rx="8" fill={alpha(primary, 0.05)} stroke={alpha(textSecondary, 0.2)} strokeWidth="1" strokeDasharray="4 3"/>
      
      {/* Person 1 - faded */}
      <g transform="translate(65, 70)" opacity="0.3">
        <circle cx="0" cy="0" r="18" fill={alpha(primary, 0.2)} stroke={primary} strokeWidth="1.5" strokeDasharray="4 2"/>
        <circle cx="0" cy="-4" r="7" fill={primary}/>
        <path d="M-10,8 Q0,14 10,8" stroke={primary} strokeWidth="2.5" strokeLinecap="round" fill="none"/>
      </g>
      
      {/* Person 2 - faded */}
      <g transform="translate(100, 70)" opacity="0.5">
        <circle cx="0" cy="0" r="18" fill={alpha(secondary, 0.2)} stroke={secondary} strokeWidth="1.5" strokeDasharray="4 2"/>
        <circle cx="0" cy="-4" r="7" fill={secondary}/>
        <path d="M-10,8 Q0,14 10,8" stroke={secondary} strokeWidth="2.5" strokeLinecap="round" fill="none"/>
      </g>
      
      {/* Person 3 - most visible (add new) */}
      <g transform="translate(135, 70)">
        <circle cx="0" cy="0" r="18" fill="none" stroke={primary} strokeWidth="2" strokeDasharray="5 3">
          <animate attributeName="stroke-dashoffset" values="0;16" dur="2s" repeatCount="indefinite"/>
        </circle>
        <text x="0" y="7" textAnchor="middle" fontSize="24" fill={primary}>+</text>
      </g>
      
      {/* ID card icon */}
      <g transform="translate(155, 35)">
        <rect x="0" y="0" width="30" height="22" rx="3" fill={alpha(primary, 0.15)} stroke={primary} strokeWidth="1.5"/>
        <circle cx="10" cy="10" r="5" fill={primary}/>
        <line x1="18" y1="7" x2="26" y2="7" stroke={primary} strokeWidth="1.5" strokeLinecap="round"/>
        <line x1="18" y1="12" x2="24" y2="12" stroke={alpha(primary, 0.5)} strokeWidth="1.5" strokeLinecap="round"/>
      </g>
      
      {/* Contact lines hint */}
      <g transform="translate(45, 110)">
        <line x1="0" y1="0" x2="40" y2="0" stroke={alpha(textSecondary, 0.2)} strokeWidth="2" strokeLinecap="round"/>
        <line x1="0" y1="8" x2="30" y2="8" stroke={alpha(textSecondary, 0.15)} strokeWidth="2" strokeLinecap="round"/>
      </g>
    </svg>
  );
};

/**
 * Illustration: Empty Tasks
 * Checkbox list with pending items
 */
export const EmptyTasksIllustration: React.FC<IllustrationProps> = ({ 
  width = 200, 
  height = 160 
}) => {
  const theme = useThemeColors();
  const primary = theme.primary;
  const secondary = theme.secondary;
  const success = theme.success;
  const textSecondary = theme.textSecondary;
  
  return (
    <svg width={width} height={height} viewBox="0 0 200 160" fill="none">
      {/* Clipboard base */}
      <rect x="45" y="25" width="110" height="115" rx="6" fill={alpha(primary, 0.08)} stroke={primary} strokeWidth="2"/>
      
      {/* Clipboard clip */}
      <rect x="75" y="18" width="50" height="16" rx="3" fill={primary}/>
      <rect x="85" y="22" width="30" height="8" rx="2" fill={alpha('#fff', 0.3)}/>
      
      {/* Task line 1 - checked */}
      <g transform="translate(60, 50)">
        <rect x="0" y="0" width="18" height="18" rx="4" fill={success} stroke={success} strokeWidth="1.5"/>
        <path d="M4,9 L8,13 L14,5" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
        <line x1="25" y1="9" x2="120" y2="9" stroke={alpha(textSecondary, 0.3)} strokeWidth="2" strokeLinecap="round"/>
      </g>
      
      {/* Task line 2 - empty */}
      <g transform="translate(60, 80)">
        <rect x="0" y="0" width="18" height="18" rx="4" fill="none" stroke={alpha(textSecondary, 0.3)} strokeWidth="1.5" strokeDasharray="3 2"/>
        <line x1="25" y1="9" x2="100" y2="9" stroke={alpha(textSecondary, 0.2)} strokeWidth="2" strokeLinecap="round"/>
      </g>
      
      {/* Task line 3 - empty with hint */}
      <g transform="translate(60, 110)">
        <rect x="0" y="0" width="18" height="18" rx="4" fill="none" stroke={secondary} strokeWidth="1.5" strokeDasharray="3 2">
          <animate attributeName="stroke-dashoffset" values="0;10" dur="1.5s" repeatCount="indefinite"/>
        </rect>
        <text x="9" y="14" textAnchor="middle" fontSize="14" fill={secondary}>+</text>
        <line x1="25" y1="9" x2="80" y2="9" stroke={alpha(textSecondary, 0.15)} strokeWidth="2" strokeLinecap="round"/>
      </g>
      
      {/* Floating star */}
      <g transform="translate(165, 45)">
        <text fontSize="16">⭐</text>
        <animate attributeName="opacity" values="1;0.5;1" dur="2s" repeatCount="indefinite"/>
      </g>
    </svg>
  );
};

/**
 * Illustration: Empty Work Logs
 * Clock with log entries
 */
export const EmptyWorkLogsIllustration: React.FC<IllustrationProps> = ({ 
  width = 200, 
  height = 160 
}) => {
  const theme = useThemeColors();
  const primary = theme.primary;
  const secondary = theme.secondary;
  const warning = theme.warning;
  
  return (
    <svg width={width} height={height} viewBox="0 0 200 160" fill="none">
      {/* Clock face */}
      <circle cx="80" cy="80" r="45" fill={alpha(primary, 0.08)} stroke={primary} strokeWidth="3"/>
      <circle cx="80" cy="80" r="38" fill="none" stroke={alpha(primary, 0.15)} strokeWidth="1"/>
      
      {/* Clock ticks */}
      {[0, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330].map((angle, i) => (
        <line
          key={i}
          x1={80 + 32 * Math.cos((angle - 90) * Math.PI / 180)}
          y1={80 + 32 * Math.sin((angle - 90) * Math.PI / 180)}
          x2={80 + 38 * Math.cos((angle - 90) * Math.PI / 180)}
          y2={80 + 38 * Math.sin((angle - 90) * Math.PI / 180)}
          stroke={i % 3 === 0 ? primary : alpha(primary, 0.3)}
          strokeWidth={i % 3 === 0 ? 2 : 1}
          strokeLinecap="round"
        />
      ))}
      
      {/* Hour hand */}
      <line x1="80" y1="80" x2="80" y2="55" stroke={primary} strokeWidth="3" strokeLinecap="round">
        <animateTransform attributeName="transform" type="rotate" from="0 80 80" to="360 80 80" dur="43200s" repeatCount="indefinite"/>
      </line>
      
      {/* Minute hand */}
      <line x1="80" y1="80" x2="80" y2="48" stroke={secondary} strokeWidth="2" strokeLinecap="round">
        <animateTransform attributeName="transform" type="rotate" from="0 80 80" to="360 80 80" dur="3600s" repeatCount="indefinite"/>
      </line>
      
      {/* Center dot */}
      <circle cx="80" cy="80" r="4" fill={primary}/>
      
      {/* Log entries hint */}
      <g transform="translate(135, 50)">
        <rect x="0" y="0" width="50" height="12" rx="2" fill={alpha(secondary, 0.2)} stroke={secondary} strokeWidth="1"/>
        <line x1="5" y1="6" x2="35" y2="6" stroke={secondary} strokeWidth="1.5" strokeLinecap="round"/>
      </g>
      <g transform="translate(140, 70)">
        <rect x="0" y="0" width="45" height="12" rx="2" fill={alpha(primary, 0.15)} stroke={alpha(primary, 0.3)} strokeWidth="1" strokeDasharray="2 2"/>
        <line x1="5" y1="6" x2="30" y2="6" stroke={alpha(primary, 0.3)} strokeWidth="1.5" strokeLinecap="round"/>
      </g>
      <g transform="translate(145, 90)">
        <rect x="0" y="0" width="40" height="12" rx="2" fill="none" stroke={alpha(primary, 0.2)} strokeWidth="1" strokeDasharray="2 2"/>
      </g>
      
      {/* Timer icon */}
      <g transform="translate(160, 115)">
        <circle cx="0" cy="0" r="15" fill={alpha(warning, 0.15)} stroke={warning} strokeWidth="2"/>
        <text x="0" y="5" textAnchor="middle" fontSize="14">⏱️</text>
        <animate attributeName="opacity" values="1;0.6;1" dur="2s" repeatCount="indefinite"/>
      </g>
    </svg>
  );
};

/**
 * Illustration: Empty Projects
 * Folder with project icon
 */
export const EmptyProjectsIllustration: React.FC<IllustrationProps> = ({ 
  width = 200, 
  height = 160 
}) => {
  const theme = useThemeColors();
  const primary = theme.primary;
  const secondary = theme.secondary;
  const warning = theme.warning;
  
  return (
    <svg width={width} height={height} viewBox="0 0 200 160" fill="none">
      {/* Back folder */}
      <g transform="translate(55, 45)">
        <path d="M0,20 L0,90 Q0,95 5,95 L95,95 Q100,95 100,90 L100,20 Q100,15 95,15 L55,15 L45,5 L5,5 Q0,5 0,10 Z" fill={alpha(primary, 0.15)} stroke={primary} strokeWidth="2"/>
      </g>
      
      {/* Front folder */}
      <g transform="translate(45, 50)">
        <path d="M0,20 L0,85 Q0,90 5,90 L105,90 Q110,90 110,85 L110,25 Q110,20 105,20 L55,20 L45,10 L5,10 Q0,10 0,15 Z" fill={alpha(secondary, 0.1)} stroke={secondary} strokeWidth="2"/>
        
        {/* Folder tab */}
        <path d="M5,10 L45,10 L50,17 L5,17 Z" fill={secondary}/>
      </g>
      
      {/* Document hints inside */}
      <g transform="translate(60, 75)">
        <rect x="0" y="0" width="35" height="40" rx="2" fill="white" stroke={alpha(primary, 0.3)} strokeWidth="1"/>
        <line x1="5" y1="10" x2="30" y2="10" stroke={alpha(primary, 0.2)} strokeWidth="2" strokeLinecap="round"/>
        <line x1="5" y1="18" x2="25" y2="18" stroke={alpha(primary, 0.15)} strokeWidth="2" strokeLinecap="round"/>
        <line x1="5" y1="26" x2="28" y2="26" stroke={alpha(primary, 0.1)} strokeWidth="2" strokeLinecap="round"/>
      </g>
      
      <g transform="translate(80, 70)">
        <rect x="0" y="0" width="35" height="40" rx="2" fill="white" stroke={alpha(secondary, 0.3)} strokeWidth="1" transform="rotate(5)"/>
      </g>
      
      {/* Plus add hint */}
      <g transform="translate(150, 120)">
        <circle cx="0" cy="0" r="18" fill={alpha(warning, 0.15)} stroke={warning} strokeWidth="2" strokeDasharray="4 3">
          <animate attributeName="stroke-dashoffset" values="0;14" dur="1.5s" repeatCount="indefinite"/>
        </circle>
        <text x="0" y="6" textAnchor="middle" fontSize="22" fill={warning}>+</text>
      </g>
      
      {/* Floating elements */}
      <text x="165" y="55" fontSize="14">📁</text>
      <text x="35" y="40" fontSize="12">✨</text>
    </svg>
  );
};

/**
 * Illustration: Empty Reports
 * Charts and graphs placeholder
 */
export const EmptyReportsIllustration: React.FC<IllustrationProps> = ({ 
  width = 200, 
  height = 160 
}) => {
  const theme = useThemeColors();
  const primary = theme.primary;
  const secondary = theme.secondary;
  const success = theme.success;
  const warning = theme.warning;
  
  return (
    <svg width={width} height={height} viewBox="0 0 200 160" fill="none">
      {/* Chart background */}
      <rect x="35" y="30" width="130" height="90" rx="6" fill={alpha(primary, 0.05)} stroke={alpha(primary, 0.2)} strokeWidth="1"/>
      
      {/* Chart axes */}
      <line x1="55" y1="105" x2="150" y2="105" stroke={alpha(primary, 0.3)} strokeWidth="2" strokeLinecap="round"/>
      <line x1="55" y1="45" x2="55" y2="105" stroke={alpha(primary, 0.3)} strokeWidth="2" strokeLinecap="round"/>
      
      {/* Bar chart bars - animated */}
      <rect x="70" y="75" width="15" height="30" rx="2" fill={alpha(primary, 0.3)}>
        <animate attributeName="height" values="30;45;30" dur="2s" repeatCount="indefinite"/>
        <animate attributeName="y" values="75;60;75" dur="2s" repeatCount="indefinite"/>
      </rect>
      <rect x="95" y="55" width="15" height="50" rx="2" fill={alpha(secondary, 0.4)}>
        <animate attributeName="height" values="50;35;50" dur="2.5s" repeatCount="indefinite"/>
        <animate attributeName="y" values="55;70;55" dur="2.5s" repeatCount="indefinite"/>
      </rect>
      <rect x="120" y="65" width="15" height="40" rx="2" fill={alpha(success, 0.4)}>
        <animate attributeName="height" values="40;55;40" dur="3s" repeatCount="indefinite"/>
        <animate attributeName="y" values="65;50;65" dur="3s" repeatCount="indefinite"/>
      </rect>
      
      {/* Pie chart hint */}
      <g transform="translate(165, 55)">
        <circle cx="0" cy="0" r="20" fill={alpha(warning, 0.2)} stroke={warning} strokeWidth="2"/>
        <path d="M0,0 L0,-18 A18,18 0 0,1 15,10 Z" fill={warning}/>
        <animate attributeName="opacity" values="1;0.7;1" dur="2s" repeatCount="indefinite"/>
      </g>
      
      {/* Trend line */}
      <path d="M60,90 Q85,70 110,80 Q135,90 145,60" stroke={primary} strokeWidth="2" strokeLinecap="round" fill="none" strokeDasharray="5 3">
        <animate attributeName="stroke-dashoffset" values="0;16" dur="2s" repeatCount="indefinite"/>
      </path>
      
      {/* Data points */}
      <circle cx="60" cy="90" r="3" fill={primary}/>
      <circle cx="110" cy="80" r="3" fill={primary}/>
      <circle cx="145" cy="60" r="3" fill={primary}/>
      
      {/* Label hint */}
      <g transform="translate(45, 135)">
        <rect x="0" y="0" width="50" height="8" rx="2" fill={alpha(primary, 0.15)}/>
        <rect x="60" y="0" width="40" height="8" rx="2" fill={alpha(secondary, 0.15)}/>
        <rect x="110" y="0" width="45" height="8" rx="2" fill={alpha(success, 0.15)}/>
      </g>
    </svg>
  );
};
