import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Eye, Pencil } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Markdown } from './ui/markdown';

interface MarkdownEditorProps {
  id?: string;
  value?: string;
  onChange: (value: string) => void;
  placeholder?: string;
  rows?: number;
  className?: string;
  /** Disable the editor (read-only textarea) */
  disabled?: boolean;
}

/**
 * A textarea with live markdown preview toggle.
 * Write tab shows a plain textarea; Preview tab renders the markdown.
 */
export default function MarkdownEditor({
  id,
  value = '',
  onChange,
  placeholder,
  rows = 4,
  className,
  disabled = false,
}: MarkdownEditorProps) {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState<'write' | 'preview'>('write');

  return (
    <div className={cn('rounded-md border border-input', className)}>
      {/* Tab bar */}
      <div className="flex items-center gap-1 border-b border-border px-2 py-1 bg-muted/30">
        <button
          type="button"
          onClick={() => setActiveTab('write')}
          className={cn(
            'inline-flex items-center gap-1 rounded px-2.5 py-1 text-xs font-medium transition-colors',
            activeTab === 'write'
              ? 'bg-background text-foreground shadow-sm'
              : 'text-muted-foreground hover:text-foreground'
          )}
        >
          <Pencil className="h-3 w-3" />
          {t('markdownEditor.write')}
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('preview')}
          className={cn(
            'inline-flex items-center gap-1 rounded px-2.5 py-1 text-xs font-medium transition-colors',
            activeTab === 'preview'
              ? 'bg-background text-foreground shadow-sm'
              : 'text-muted-foreground hover:text-foreground'
          )}
        >
          <Eye className="h-3 w-3" />
          {t('markdownEditor.preview')}
        </button>
        <span className="ml-auto text-[10px] text-muted-foreground hidden sm:inline">
          {t('markdownEditor.markdownSupported')}
        </span>
      </div>

      {/* Content area */}
      {activeTab === 'write' ? (
        <textarea
          id={id}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          rows={rows}
          disabled={disabled}
          className={cn(
            'w-full resize-y bg-transparent px-3 py-2 text-sm placeholder:text-muted-foreground',
            'focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-50',
            'font-mono'
          )}
        />
      ) : (
        <div
          className={cn(
            'min-h-[calc(theme(spacing.6)*var(--rows,4))] px-3 py-2 text-sm',
            !value && 'text-muted-foreground italic'
          )}
          style={{ '--rows': rows } as React.CSSProperties}
        >
          {value ? (
            <Markdown content={value} />
          ) : (
            t('markdownEditor.nothingToPreview')
          )}
        </div>
      )}
    </div>
  );
}
