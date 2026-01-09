import React from 'react';
import { Sun, Moon, Monitor, Check } from 'lucide-react';
import { useTheme, ThemeMode } from '../contexts';
import { Button } from './ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from './ui/dropdown-menu';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from './ui/tooltip';

interface ThemeToggleProps {
  showLabel?: boolean;
}

export const ThemeToggle: React.FC<ThemeToggleProps> = ({ showLabel = false }) => {
  const { mode, actualMode, setMode } = useTheme();

  const handleModeChange = (newMode: ThemeMode) => {
    setMode(newMode);
  };

  const getIcon = () => {
    if (actualMode === 'dark') {
      return <Moon className="h-4 w-4" />;
    }
    return <Sun className="h-4 w-4" />;
  };

  const getModeLabel = () => {
    switch (mode) {
      case 'light':
        return 'Light';
      case 'dark':
        return 'Dark';
      case 'system':
        return 'System';
      default:
        return 'Theme';
    }
  };

  return (
    <DropdownMenu>
      <TooltipProvider>
        <Tooltip>
          <TooltipTrigger asChild>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="flex items-center gap-1"
                aria-label={`Current theme: ${getModeLabel()}. Click to change theme`}
              >
                {getIcon()}
                {showLabel && (
                  <span className="ml-1 text-sm" aria-hidden="true">
                    {getModeLabel()}
                  </span>
                )}
              </Button>
            </DropdownMenuTrigger>
          </TooltipTrigger>
          <TooltipContent>
            <p>Change theme</p>
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
      
      <DropdownMenuContent align="end" className="w-40">
        <DropdownMenuLabel>Theme Mode</DropdownMenuLabel>
        <DropdownMenuSeparator />
        
        <DropdownMenuItem
          onClick={() => handleModeChange('light')}
          className="flex items-center justify-between cursor-pointer"
          aria-label="Light theme"
        >
          <span className="flex items-center gap-2">
            <Sun className="h-4 w-4" aria-hidden="true" />
            Light
          </span>
          {mode === 'light' && <Check className="h-4 w-4 text-primary" aria-hidden="true" />}
        </DropdownMenuItem>
        
        <DropdownMenuItem
          onClick={() => handleModeChange('dark')}
          className="flex items-center justify-between cursor-pointer"
          aria-label="Dark theme"
        >
          <span className="flex items-center gap-2">
            <Moon className="h-4 w-4" aria-hidden="true" />
            Dark
          </span>
          {mode === 'dark' && <Check className="h-4 w-4 text-primary" aria-hidden="true" />}
        </DropdownMenuItem>
        
        <DropdownMenuItem
          onClick={() => handleModeChange('system')}
          className="flex items-center justify-between cursor-pointer"
          aria-label="System theme - follows your device settings"
        >
          <span className="flex items-center gap-2">
            <Monitor className="h-4 w-4" aria-hidden="true" />
            System
          </span>
          {mode === 'system' && <Check className="h-4 w-4 text-primary" aria-hidden="true" />}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

// Simple toggle button for quick switching between light and dark
export const ThemeToggleSimple: React.FC = () => {
  const { actualMode, toggleTheme } = useTheme();

  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <Button
            variant="ghost"
            size="icon"
            onClick={toggleTheme}
            aria-label={`Switch to ${actualMode === 'light' ? 'dark' : 'light'} mode`}
          >
            {actualMode === 'dark' ? (
              <Sun className="h-4 w-4" aria-hidden="true" />
            ) : (
              <Moon className="h-4 w-4" aria-hidden="true" />
            )}
          </Button>
        </TooltipTrigger>
        <TooltipContent>
          <p>Switch to {actualMode === 'light' ? 'dark' : 'light'} mode</p>
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
};

export default ThemeToggle;
