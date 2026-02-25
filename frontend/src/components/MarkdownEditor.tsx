import { useCallback, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Bold,
  Italic,
  Heading1,
  Heading2,
  Heading3,
  List,
  ListOrdered,
  Quote,
  Code,
  Code2,
  Minus,
  Link,
  Eye,
  PenLine,
  Strikethrough,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { Markdown } from './ui/markdown';

interface MarkdownEditorProps {
  id?: string;
  value?: string;
  onChange: (value: string) => void;
  placeholder?: string;
  rows?: number;
  className?: string;
  disabled?: boolean;
}

// ──────────────────────────────────────────────────────────────────────────────
// Formatting helpers
// ──────────────────────────────────────────────────────────────────────────────

interface InlineWrap { kind: 'inline'; prefix: string; suffix: string }
interface LinePrefix { kind: 'line'; prefix: string }
interface Insert    { kind: 'insert'; text: string }
interface LinkWrap  { kind: 'link' }
type FormatAction = InlineWrap | LinePrefix | Insert | LinkWrap;

function applyFormat(
  textarea: HTMLTextAreaElement,
  action: FormatAction,
  onChange: (v: string) => void
) {
  const { selectionStart: start, selectionEnd: end, value } = textarea;
  const selected = value.slice(start, end);
  let newValue = value;
  let cursor: [number, number] = [start, end];

  if (action.kind === 'insert') {
    newValue = value.slice(0, start) + action.text + value.slice(end);
    const pos = start + action.text.length;
    cursor = [pos, pos];

  } else if (action.kind === 'inline') {
    const { prefix, suffix } = action;
    const isBefore = value.slice(start - prefix.length, start) === prefix;
    const isAfter  = value.slice(end, end + suffix.length) === suffix;
    if (isBefore && isAfter) {
      // Unwrap
      newValue =
        value.slice(0, start - prefix.length) +
        selected +
        value.slice(end + suffix.length);
      cursor = [start - prefix.length, end - prefix.length];
    } else {
      const wrapped = prefix + selected + suffix;
      newValue = value.slice(0, start) + wrapped + value.slice(end);
      cursor = selected
        ? [start + prefix.length, end + prefix.length]
        : [start + prefix.length, start + prefix.length];
    }

  } else if (action.kind === 'line') {
    const { prefix } = action;
    const lineStart = value.lastIndexOf('\n', start - 1) + 1;
    const rawEnd    = selected ? value.indexOf('\n', end - 1) : value.indexOf('\n', start);
    const lineEnd   = rawEnd === -1 ? value.length : rawEnd;
    const block     = value.slice(lineStart, lineEnd);
    const lines     = block.split('\n');
    const pfxTrimmed = prefix.trimEnd();
    const allPrefixed = lines.every((l) => l.startsWith(pfxTrimmed));
    const transformed = allPrefixed
      ? lines.map((l) => l.slice(pfxTrimmed.length).replace(/^ /, '')).join('\n')
      : lines.map((l) => prefix + l).join('\n');
    newValue = value.slice(0, lineStart) + transformed + value.slice(lineEnd);
    const delta = transformed.length - block.length;
    cursor = [start + (allPrefixed ? -prefix.length : prefix.length), end + delta];

  } else if (action.kind === 'link') {
    const label    = selected || 'link text';
    const inserted = `[${label}](url)`;
    newValue = value.slice(0, start) + inserted + value.slice(end);
    const urlStart = start + label.length + 3; // skip "[label]("
    cursor = [urlStart, urlStart + 3];          // select "url"
  }

  onChange(newValue);
  requestAnimationFrame(() => {
    textarea.focus();
    textarea.setSelectionRange(cursor[0], cursor[1]);
  });
}

// ──────────────────────────────────────────────────────────────────────────────
// Toolbar definitions
// ──────────────────────────────────────────────────────────────────────────────

type ToolbarItem =
  | { type: 'sep' }
  | { type: 'btn'; icon: React.FC<{ className?: string }>; label: string; shortcut?: string; action: FormatAction };

const TOOLBAR: ToolbarItem[] = [
  { type: 'btn', icon: Bold,          label: 'Bold',           shortcut: 'Ctrl+B', action: { kind: 'inline', prefix: '**', suffix: '**' } },
  { type: 'btn', icon: Italic,        label: 'Italic',         shortcut: 'Ctrl+I', action: { kind: 'inline', prefix: '_',  suffix: '_'  } },
  { type: 'btn', icon: Strikethrough, label: 'Strikethrough',                      action: { kind: 'inline', prefix: '~~', suffix: '~~' } },
  { type: 'sep' },
  { type: 'btn', icon: Heading1,      label: 'Heading 1',                          action: { kind: 'line', prefix: '# '  } },
  { type: 'btn', icon: Heading2,      label: 'Heading 2',                          action: { kind: 'line', prefix: '## ' } },
  { type: 'btn', icon: Heading3,      label: 'Heading 3',                          action: { kind: 'line', prefix: '### '} },
  { type: 'sep' },
  { type: 'btn', icon: List,          label: 'Bullet list',                        action: { kind: 'line', prefix: '- '  } },
  { type: 'btn', icon: ListOrdered,   label: 'Numbered list',                      action: { kind: 'line', prefix: '1. ' } },
  { type: 'sep' },
  { type: 'btn', icon: Quote,         label: 'Blockquote',                         action: { kind: 'line', prefix: '> '  } },
  { type: 'btn', icon: Code,          label: 'Inline code',                        action: { kind: 'inline', prefix: '`',     suffix: '`'     } },
  { type: 'btn', icon: Code2,         label: 'Code block',                         action: { kind: 'inline', prefix: '```\n', suffix: '\n```' } },
  { type: 'sep' },
  { type: 'btn', icon: Link,          label: 'Insert link',    shortcut: 'Ctrl+K', action: { kind: 'link'   } },
  { type: 'btn', icon: Minus,         label: 'Horizontal rule',                    action: { kind: 'insert', text: '\n\n---\n\n' } },
];

// ──────────────────────────────────────────────────────────────────────────────
// Component
// ──────────────────────────────────────────────────────────────────────────────

export default function MarkdownEditor({
  id,
  value = '',
  onChange,
  placeholder,
  rows = 6,
  className,
  disabled = false,
}: MarkdownEditorProps) {
  const { t } = useTranslation();
  const [tab, setTab] = useState<'write' | 'preview'>('write');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleFormat = useCallback(
    (action: FormatAction) => {
      if (!textareaRef.current || disabled) return;
      applyFormat(textareaRef.current, action, onChange);
    },
    [onChange, disabled]
  );

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
      const ctrl = e.ctrlKey || e.metaKey;
      if (ctrl && e.key === 'b') { e.preventDefault(); handleFormat({ kind: 'inline', prefix: '**', suffix: '**' }); }
      else if (ctrl && e.key === 'i') { e.preventDefault(); handleFormat({ kind: 'inline', prefix: '_', suffix: '_' }); }
      else if (ctrl && e.key === 'k') { e.preventDefault(); handleFormat({ kind: 'link' }); }
    },
    [handleFormat]
  );

  return (
    <div className={cn('rounded-md border border-input bg-background', className)}>
      {/* ── Header ─────────────────────────────────────────────────────── */}
      <div className="flex flex-wrap items-center gap-0.5 border-b border-border px-2 py-1 bg-muted/20">

        {/* Write / Preview tabs */}
        <button
          type="button"
          onClick={() => setTab('write')}
          disabled={disabled}
          className={cn(
            'inline-flex items-center gap-1 rounded px-2.5 py-1 text-xs font-medium transition-colors',
            tab === 'write'
              ? 'bg-background text-foreground shadow-sm'
              : 'text-muted-foreground hover:text-foreground'
          )}
        >
          <PenLine className="h-3.5 w-3.5" />
          {t('markdownEditor.write', 'Write')}
        </button>
        <button
          type="button"
          onClick={() => setTab('preview')}
          className={cn(
            'inline-flex items-center gap-1 rounded px-2.5 py-1 text-xs font-medium transition-colors',
            tab === 'preview'
              ? 'bg-background text-foreground shadow-sm'
              : 'text-muted-foreground hover:text-foreground'
          )}
        >
          <Eye className="h-3.5 w-3.5" />
          {t('markdownEditor.preview', 'Preview')}
        </button>

        {/* Formatting toolbar — visible only in Write mode */}
        {tab === 'write' && !disabled && (
          <>
            <div className="mx-1 h-4 w-px bg-border" />
            {TOOLBAR.map((item, i) => {
              if (item.type === 'sep') {
                return <div key={`sep-${i}`} className="h-4 w-px bg-border mx-0.5" />;
              }
              const { icon: Icon, label, shortcut, action } = item;
              return (
                <button
                  key={label}
                  type="button"
                  title={shortcut ? `${label} (${shortcut})` : label}
                  aria-label={label}
                  onClick={() => handleFormat(action)}
                  className={cn(
                    'inline-flex items-center justify-center rounded p-1 text-muted-foreground',
                    'hover:bg-accent hover:text-foreground transition-colors',
                    'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring'
                  )}
                >
                  <Icon className="h-3.5 w-3.5" />
                </button>
              );
            })}
          </>
        )}
      </div>

      {/* ── Content area ────────────────────────────────────────────────── */}
      {tab === 'write' ? (
        <textarea
          ref={textareaRef}
          id={id}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={
            placeholder ??
            t(
              'markdownEditor.placeholder',
              'Write your description here…\n\nTip: Select text and click a toolbar button to format it, or use Ctrl+B / Ctrl+I.'
            )
          }
          rows={rows}
          disabled={disabled}
          className={cn(
            'w-full resize-y bg-transparent px-3 py-2.5 text-sm placeholder:text-muted-foreground',
            'focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-50',
            'leading-relaxed'
          )}
        />
      ) : (
        <div className={cn('min-h-[6rem] px-3 py-2.5 text-sm', !value && 'text-muted-foreground italic')}>
          {value
            ? <Markdown content={value} />
            : <span>{t('markdownEditor.nothingToPreview', 'Nothing to preview yet.')}</span>
          }
        </div>
      )}
    </div>
  );
}
