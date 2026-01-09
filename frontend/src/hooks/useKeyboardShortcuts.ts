import { useEffect, useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';

export interface KeyboardShortcut {
  key: string;
  ctrl?: boolean;
  alt?: boolean;
  shift?: boolean;
  meta?: boolean; // cmd on Mac
  description: string;
  action: () => void;
  category: 'navigation' | 'actions' | 'general';
}

// Check if user is typing in an input
const isTyping = () => {
  const activeElement = document.activeElement;
  if (!activeElement) return false;
  const tagName = activeElement.tagName.toLowerCase();
  return (
    tagName === 'input' ||
    tagName === 'textarea' ||
    tagName === 'select' ||
    (activeElement as HTMLElement).isContentEditable
  );
};

export function useKeyboardShortcuts(additionalShortcuts?: KeyboardShortcut[]) {
  const navigate = useNavigate();
  const [showHelp, setShowHelp] = useState(false);

  const defaultShortcuts: KeyboardShortcut[] = [
    // Navigation shortcuts
    {
      key: 'g',
      description: 'Go to Dashboard',
      action: () => navigate('/'),
      category: 'navigation',
    },
    {
      key: 'c',
      description: 'Go to Cycles',
      action: () => navigate('/cycles'),
      category: 'navigation',
    },
    {
      key: 'p',
      description: 'Go to Pitches',
      action: () => navigate('/pitches'),
      category: 'navigation',
    },
    {
      key: 't',
      description: 'Go to Tasks',
      action: () => navigate('/tasks'),
      category: 'navigation',
    },
    {
      key: 'm',
      description: 'Go to Meetings',
      action: () => navigate('/meetings'),
      category: 'navigation',
    },
    {
      key: 'r',
      description: 'Go to Reports',
      action: () => navigate('/reports'),
      category: 'navigation',
    },
    {
      key: 'h',
      description: 'Go to Health Overview',
      action: () => navigate('/health'),
      category: 'navigation',
    },
    // Quick create shortcuts
    {
      key: 'n',
      shift: true,
      description: 'Create New Cycle',
      action: () => navigate('/cycles/new'),
      category: 'actions',
    },
    {
      key: 'w',
      shift: true,
      description: 'Log Work',
      action: () => navigate('/worklogs'),
      category: 'actions',
    },
    // General
    {
      key: '?',
      description: 'Show keyboard shortcuts',
      action: () => setShowHelp((prev) => !prev),
      category: 'general',
    },
    {
      key: 'Escape',
      description: 'Close dialogs',
      action: () => setShowHelp(false),
      category: 'general',
    },
  ];

  const allShortcuts = [...defaultShortcuts, ...(additionalShortcuts || [])];

  const handleKeyDown = useCallback(
    (event: KeyboardEvent) => {
      // Don't trigger shortcuts when typing
      if (isTyping()) return;

      const { key, ctrlKey, altKey, shiftKey, metaKey } = event;

      for (const shortcut of allShortcuts) {
        const matchesKey = shortcut.key.toLowerCase() === key.toLowerCase() || 
                          (shortcut.key === '?' && key === '?');
        const matchesCtrl = !!shortcut.ctrl === ctrlKey;
        const matchesAlt = !!shortcut.alt === altKey;
        const matchesShift = !!shortcut.shift === shiftKey;
        const matchesMeta = !!shortcut.meta === metaKey;

        if (matchesKey && matchesCtrl && matchesAlt && matchesShift && matchesMeta) {
          event.preventDefault();
          shortcut.action();
          return;
        }
      }
    },
    [allShortcuts]
  );

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  return {
    shortcuts: allShortcuts,
    showHelp,
    setShowHelp,
  };
}

export default useKeyboardShortcuts;
