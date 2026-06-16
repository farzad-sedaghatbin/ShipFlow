import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  CheckCircle,
  PlusCircle,
  UserCheck,
  MessageSquare,
  FileText,
  Lightbulb,
  Play,
  AlertTriangle,
  TrendingUp,
  Lock,
  ClipboardList,
  CheckSquare,
  Mail,
  Zap,
} from 'lucide-react';
import type { WorkflowAutomationTemplate } from '../../types/automation';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Input } from '../ui/input';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs';

const ICON_MAP: Record<string, React.ElementType> = {
  CheckCircle,
  PlusCircle,
  UserCheck,
  MessageSquare,
  FileText,
  Lightbulb,
  Play,
  AlertTriangle,
  TrendingUp,
  Lock,
  ClipboardList,
  CheckSquare,
  Mail,
  Webhook: Zap,
  FlagOff: Play,
  default: Zap,
};

interface Props {
  open: boolean;
  templates: WorkflowAutomationTemplate[];
  onSelect: (template: WorkflowAutomationTemplate) => void;
  onClose: () => void;
}

export default function AutomationTemplateGallery({ open, templates, onSelect, onClose }: Props) {
  const { t } = useTranslation();
  const [search, setSearch] = useState('');

  const categories = useMemo(() => {
    const cats = Array.from(new Set(templates.map(t => t.category)));
    return ['All', ...cats];
  }, [templates]);

  const filtered = (category: string) =>
    templates.filter(tmpl => {
      const matchesCat = category === 'All' || tmpl.category === category;
      const matchesSearch =
        !search ||
        tmpl.name.toLowerCase().includes(search.toLowerCase()) ||
        (tmpl.description ?? '').toLowerCase().includes(search.toLowerCase());
      return matchesCat && matchesSearch;
    });

  const TemplateCard = ({ tmpl }: { tmpl: WorkflowAutomationTemplate }) => {
    const Icon = ICON_MAP[tmpl.iconName ?? ''] ?? ICON_MAP.default;
    return (
      <button
        className="flex flex-col gap-2 rounded-lg border p-4 text-left hover:border-primary hover:bg-accent transition-colors w-full"
        onClick={() => onSelect(tmpl)}
      >
        <div className="flex items-start gap-3">
          <div className="rounded-md bg-primary/10 p-2 shrink-0">
            <Icon className="h-4 w-4 text-primary" />
          </div>
          <div className="flex-1 min-w-0">
            <p className="font-medium text-sm leading-tight">{tmpl.name}</p>
            {tmpl.description && (
              <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{tmpl.description}</p>
            )}
          </div>
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <Badge variant="secondary" className="text-xs">{tmpl.category}</Badge>
          <Badge variant="outline" className="text-xs">{tmpl.triggerType.replace(/_/g, ' ')}</Badge>
          <span className="text-xs text-muted-foreground">→</span>
          <Badge variant="outline" className="text-xs">{tmpl.actionType.replace(/_/g, ' ')}</Badge>
        </div>
      </button>
    );
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-3xl max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle>{t('automation.templateGalleryTitle')}</DialogTitle>
          <DialogDescription>{t('automation.templateGalleryDesc')}</DialogDescription>
        </DialogHeader>

        <Input
          placeholder={t('common.search') + '...'}
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="shrink-0"
        />

        <div className="flex-1 overflow-y-auto">
          <Tabs defaultValue="All">
            <TabsList className="flex-wrap h-auto gap-1 mb-4">
              {categories.map(cat => (
                <TabsTrigger key={cat} value={cat} className="text-xs">
                  {cat} {cat !== 'All' && `(${filtered(cat).length})`}
                </TabsTrigger>
              ))}
            </TabsList>

            {categories.map(cat => (
              <TabsContent key={cat} value={cat}>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {filtered(cat).map(tmpl => (
                    <TemplateCard key={tmpl.id} tmpl={tmpl} />
                  ))}
                  {filtered(cat).length === 0 && (
                    <p className="col-span-2 text-center text-sm text-muted-foreground py-8">
                      {t('common.noResults')}
                    </p>
                  )}
                </div>
              </TabsContent>
            ))}
          </Tabs>
        </div>

        <div className="flex justify-end pt-2 shrink-0">
          <Button variant="outline" onClick={onClose}>{t('common.cancel')}</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
