import { Keyboard } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { KeyboardShortcut } from '../hooks/useKeyboardShortcuts';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { Badge } from './ui/badge';

interface KeyboardShortcutsHelpProps {
  open: boolean;
  onClose: () => void;
  shortcuts: KeyboardShortcut[];
}

function KeyChip({ children }: { children: string }) {
  return (
    <Badge
      variant="outline"
      className="font-mono font-semibold text-xs min-w-7 h-6 px-2 bg-primary/10 border-primary/20"
    >
      {children}
    </Badge>
  );
}

function ShortcutRow({ shortcut }: { shortcut: KeyboardShortcut }) {
  const keys = [];
  if (shortcut.meta) keys.push('⌘');
  if (shortcut.ctrl) keys.push('Ctrl');
  if (shortcut.alt) keys.push('Alt');
  if (shortcut.shift) keys.push('⇧');
  keys.push(shortcut.key.toUpperCase());

  return (
    <div
      role="listitem"
      aria-label={`${shortcut.description}: Press ${keys.join(' plus ')}`}
      className="flex justify-between items-center py-2 px-1 rounded hover:bg-muted/50 transition-colors"
    >
      <span className="text-sm text-foreground">
        {shortcut.description}
      </span>
      <div className="flex gap-1" aria-hidden="true">
        {keys.map((key, idx) => (
          <KeyChip key={idx}>{key}</KeyChip>
        ))}
      </div>
    </div>
  );
}

export function KeyboardShortcutsHelp({ open, onClose, shortcuts }: KeyboardShortcutsHelpProps) {
  const { t } = useTranslation();
  const navigationShortcuts = shortcuts.filter((s) => s.category === 'navigation');
  const actionShortcuts = shortcuts.filter((s) => s.category === 'actions');
  const generalShortcuts = shortcuts.filter((s) => s.category === 'general');

  return (
    <Dialog open={open} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Keyboard className="h-5 w-5 text-primary" aria-hidden="true" />
            {t('keyboardShortcuts.title')}
          </DialogTitle>
        </DialogHeader>
        
        <div className="space-y-4 pt-2">
          {/* Navigation */}
          {navigationShortcuts.length > 0 && (
            <div>
              <h3 className="text-sm font-semibold text-primary mb-2">
                {t('keyboardShortcuts.navigation')}
              </h3>
              <div className="space-y-0.5">
                {navigationShortcuts.map((shortcut, idx) => (
                  <ShortcutRow key={idx} shortcut={shortcut} />
                ))}
              </div>
            </div>
          )}

          {/* Actions */}
          {actionShortcuts.length > 0 && (
            <div>
              <h3 className="text-sm font-semibold text-primary mb-2">
                {t('keyboardShortcuts.quickActions')}
              </h3>
              <div className="space-y-0.5">
                {actionShortcuts.map((shortcut, idx) => (
                  <ShortcutRow key={idx} shortcut={shortcut} />
                ))}
              </div>
            </div>
          )}

          {/* General */}
          {generalShortcuts.length > 0 && (
            <div>
              <h3 className="text-sm font-semibold text-primary mb-2">
                {t('keyboardShortcuts.general')}
              </h3>
              <div className="space-y-0.5">
                {generalShortcuts.map((shortcut, idx) => (
                  <ShortcutRow key={idx} shortcut={shortcut} />
                ))}
              </div>
            </div>
          )}

          {/* Tip */}
          <div className="p-3 rounded-lg bg-blue-500/10 border border-blue-500/20">
            <p className="text-sm text-muted-foreground">
              💡 <strong>Tip:</strong> Press <KeyChip>?</KeyChip> anytime to show this help dialog.
              Shortcuts are disabled when typing in input fields.
            </p>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

export default KeyboardShortcutsHelp;
