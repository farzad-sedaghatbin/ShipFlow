import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { X, Keyboard } from 'lucide-react';
import { Button } from './ui/button';
import { cn } from '../lib/utils';

interface ShortcutRowProps {
  keys: string[];
  description: string;
}

function ShortcutRow({ keys, description }: ShortcutRowProps) {
  return (
    <div className="flex items-center justify-between gap-4 py-1.5">
      <span className="text-sm text-muted-foreground">{description}</span>
      <div className="flex items-center gap-1 shrink-0">
        {keys.map((key, i) => (
          <span key={i} className="flex items-center gap-1">
            {i > 0 && <span className="text-xs text-muted-foreground">+</span>}
            <kbd className="inline-flex h-6 min-w-[24px] items-center justify-center rounded border border-border bg-muted px-1.5 text-xs font-medium font-mono">
              {key}
            </kbd>
          </span>
        ))}
      </div>
    </div>
  );
}

interface ShortcutSectionProps {
  title: string;
  shortcuts: { keys: string[]; descriptionKey: string }[];
  t: (key: string) => string;
}

function ShortcutSection({ title, shortcuts, t }: ShortcutSectionProps) {
  return (
    <div>
      <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-2 pb-1 border-b">
        {title}
      </h3>
      <div className="space-y-0.5">
        {shortcuts.map((s, i) => (
          <ShortcutRow key={i} keys={s.keys} description={t(s.descriptionKey)} />
        ))}
      </div>
    </div>
  );
}

interface KeyboardShortcutSheetProps {
  open: boolean;
  onClose: () => void;
}

export function KeyboardShortcutSheet({ open, onClose }: KeyboardShortcutSheetProps) {
  const { t } = useTranslation();

  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  const navigationShortcuts = [
    { keys: ['G'], descriptionKey: 'shortcuts.navDashboard' },
    { keys: ['P'], descriptionKey: 'shortcuts.navPitches' },
    { keys: ['C'], descriptionKey: 'shortcuts.navCycles' },
    { keys: ['R'], descriptionKey: 'shortcuts.navReports' },
    { keys: ['H'], descriptionKey: 'shortcuts.navHealth' },
    { keys: ['M'], descriptionKey: 'shortcuts.navMeetings' },
  ];

  const actionShortcuts = [
    { keys: ['Shift', 'N'], descriptionKey: 'shortcuts.createNew' },
    { keys: ['Shift', 'W'], descriptionKey: 'shortcuts.logWork' },
    { keys: ['⌘', 'K'], descriptionKey: 'shortcuts.openSearch' },
  ];

  const generalShortcuts = [
    { keys: ['?'], descriptionKey: 'shortcuts.showShortcuts' },
    { keys: ['Esc'], descriptionKey: 'shortcuts.closedialog' },
  ];

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-50 bg-black/40 backdrop-blur-sm"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Sheet panel — slides in from the right */}
      <div
        className={cn(
          'fixed right-0 top-0 z-50 h-full w-full max-w-sm bg-background border-s border-border shadow-xl',
          'flex flex-col'
        )}
        role="dialog"
        aria-modal="true"
        aria-label={t('shortcuts.title')}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b">
          <div className="flex items-center gap-2">
            <Keyboard className="h-5 w-5 text-muted-foreground" />
            <h2 className="text-base font-semibold">{t('shortcuts.title')}</h2>
          </div>
          <Button variant="ghost" size="icon" onClick={onClose} className="h-8 w-8">
            <X className="h-4 w-4" />
            <span className="sr-only">{t('common.close')}</span>
          </Button>
        </div>

        {/* Shortcut list */}
        <div className="flex-1 overflow-y-auto px-5 py-4 space-y-6">
          <ShortcutSection
            title={t('shortcuts.navigation')}
            shortcuts={navigationShortcuts}
            t={t}
          />
          <ShortcutSection
            title={t('shortcuts.actions')}
            shortcuts={actionShortcuts}
            t={t}
          />
          <ShortcutSection
            title={t('shortcuts.general')}
            shortcuts={generalShortcuts}
            t={t}
          />
        </div>

        {/* Footer hint */}
        <div className="px-5 py-3 border-t text-xs text-muted-foreground text-center">
          {t('shortcuts.pressToOpen')}
        </div>
      </div>
    </>
  );
}

export default KeyboardShortcutSheet;
