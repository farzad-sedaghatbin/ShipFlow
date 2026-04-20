import React, { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  AlertTriangle,
  Clock,
  ExternalLink,
  History,
  Loader2,
  Maximize2,
  Paintbrush,
  RefreshCw,
  Send,
  Sparkles,
  Wand2,
} from 'lucide-react';
import { Button } from './ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Alert, AlertDescription } from './ui/alert';
import { Badge } from './ui/badge';
import { Input } from './ui/input';
import { ScrollArea } from './ui/scroll-area';
import { useToast } from '../contexts';
import {
  wiseDesignerService,
  WiseDesignerArtifactSummary,
  WiseDesignerResponse,
} from '../services/wiseDesignerService';

interface Props {
  pitchId: number;
  pitchName: string;
  /** Pass the active Wise Architecture session ID to link the two features. */
  wiseArchSessionId?: string;
}

// ────────────────────────────────────────────────────────────────────────────

export const WiseDesignerPanel: React.FC<Props> = ({ pitchId, pitchName, wiseArchSessionId }) => {
  const { t } = useTranslation();
  const { showToast } = useToast();

  const [status, setStatus] = useState<{ enabled: boolean; message: string } | null>(null);
  const [artifacts, setArtifacts] = useState<WiseDesignerArtifactSummary[]>([]);
  const [current, setCurrent] = useState<WiseDesignerResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [, setLoadingArtifacts] = useState(false);
  const [additionalContext, setAdditionalContext] = useState('');
  const [iterInstruction, setIterInstruction] = useState('');
  const [showHistory, setShowHistory] = useState(false);
  const [fullscreen, setFullscreen] = useState(false);

  const iframeRef = useRef<HTMLIFrameElement>(null);

  // ── Load feature status + artifact history on mount ──────────────────────

  useEffect(() => {
    wiseDesignerService
      .getStatus()
      .then(setStatus)
      .catch(() => setStatus({ enabled: false, message: 'Could not reach the server' }));

    loadArtifacts();
  }, [pitchId]);

  async function loadArtifacts() {
    setLoadingArtifacts(true);
    try {
      const list = await wiseDesignerService.getArtifactsForPitch(pitchId);
      setArtifacts(list);
      // Auto-load most recent artifact if available
      if (list.length > 0 && !current) {
        const latest = await wiseDesignerService.getArtifact(list[0].artifactId);
        setCurrent(latest);
      }
    } catch {
      // Silently ignore — user will see the empty state
    } finally {
      setLoadingArtifacts(false);
    }
  }

  // ── Generate ──────────────────────────────────────────────────────────────

  async function handleGenerate() {
    setLoading(true);
    try {
      const result = await wiseDesignerService.generate({
        pitchId,
        wiseArchSessionId: wiseArchSessionId || undefined,
        additionalContext: additionalContext.trim() || undefined,
      });
      setCurrent(result);
      setArtifacts((prev) => [
        {
          artifactId: result.artifactId,
          pitchId: result.pitchId,
          description: result.description,
          sessionId: result.sessionId,
          usedArchContext: result.usedArchContext,
          createdAt: result.generatedAt,
        },
        ...prev,
      ]);
      setAdditionalContext('');
      showToast(t('wiseDesigner.generated'), 'success');
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : t('wiseDesigner.errorGenerate');
      showToast(msg, 'error');
    } finally {
      setLoading(false);
    }
  }

  // ── Iterate ───────────────────────────────────────────────────────────────

  async function handleIterate() {
    if (!current || !iterInstruction.trim()) return;
    setLoading(true);
    try {
      const result = await wiseDesignerService.iterate({
        artifactId: current.artifactId,
        instruction: iterInstruction.trim(),
      });
      setCurrent(result);
      setArtifacts((prev) => [
        {
          artifactId: result.artifactId,
          pitchId: result.pitchId,
          description: result.description,
          sessionId: result.sessionId,
          usedArchContext: result.usedArchContext,
          createdAt: result.generatedAt,
        },
        ...prev,
      ]);
      setIterInstruction('');
      showToast(t('wiseDesigner.iterated'), 'success');
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : t('wiseDesigner.errorIterate');
      showToast(msg, 'error');
    } finally {
      setLoading(false);
    }
  }

  async function loadHistoryArtifact(artifactId: number) {
    setLoading(true);
    try {
      const artifact = await wiseDesignerService.getArtifact(artifactId);
      setCurrent(artifact);
      setShowHistory(false);
    } finally {
      setLoading(false);
    }
  }

  function openInNewTab() {
    if (!current) return;
    const blob = new Blob([current.htmlContent], { type: 'text/html' });
    const url = URL.createObjectURL(blob);
    window.open(url, '_blank');
    setTimeout(() => URL.revokeObjectURL(url), 10_000);
  }

  // ── Render ────────────────────────────────────────────────────────────────

  if (!status) {
    return (
      <div className="flex items-center justify-center p-8">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!status.enabled) {
    return (
      <Alert variant="default" className="border-yellow-200 bg-yellow-50 dark:bg-yellow-950/20">
        <AlertTriangle className="h-4 w-4 text-yellow-600" />
        <AlertDescription className="text-yellow-700 dark:text-yellow-400">
          {status.message}
        </AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center gap-2">
        <Paintbrush className="h-5 w-5 text-purple-500" />
        <h3 className="font-semibold text-base">{t('wiseDesigner.title')}</h3>
        <Badge variant="secondary" className="text-xs">
          {t('wiseDesigner.badge')}
        </Badge>
        {wiseArchSessionId && (
          <Badge variant="outline" className="text-xs text-blue-600 border-blue-300">
            <Sparkles className="h-3 w-3 mr-1" />
            {t('wiseDesigner.archLinked')}
          </Badge>
        )}
      </div>

      {/* Generate panel */}
      {!current && (
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm">{t('wiseDesigner.generateTitle')}</CardTitle>
            <CardDescription className="text-xs">{t('wiseDesigner.generateDesc')}</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <Input
              placeholder={t('wiseDesigner.contextPlaceholder')}
              value={additionalContext}
              onChange={(e) => setAdditionalContext(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleGenerate()}
              disabled={loading}
            />
            <Button onClick={handleGenerate} disabled={loading} className="w-full">
              {loading ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  {t('wiseDesigner.generating')}
                </>
              ) : (
                <>
                  <Wand2 className="h-4 w-4 mr-2" />
                  {t('wiseDesigner.generateBtn')}
                  {wiseArchSessionId && (
                    <span className="ml-1 text-xs opacity-70">
                      {t('wiseDesigner.withArchContext')}
                    </span>
                  )}
                </>
              )}
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Prototype preview */}
      {current && (
        <div className="space-y-3">
          {/* Preview toolbar */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 min-w-0">
              <Paintbrush className="h-4 w-4 text-purple-500 shrink-0" />
              <span className="text-sm font-medium truncate">{current.description}</span>
              {current.usedArchContext && (
                <Badge variant="outline" className="text-xs shrink-0 text-blue-600 border-blue-200">
                  <Sparkles className="h-3 w-3 mr-1" />
                  {t('wiseDesigner.archContext')}
                </Badge>
              )}
            </div>
            <div className="flex items-center gap-1 shrink-0">
              <Button
                variant="ghost"
                size="icon"
                className="h-7 w-7"
                onClick={() => setShowHistory(!showHistory)}
                title={t('wiseDesigner.history')}
              >
                <History className="h-4 w-4" />
              </Button>
              <Button
                variant="ghost"
                size="icon"
                className="h-7 w-7"
                onClick={handleGenerate}
                disabled={loading}
                title={t('wiseDesigner.regenerate')}
              >
                {loading ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <RefreshCw className="h-4 w-4" />
                )}
              </Button>
              <Button
                variant="ghost"
                size="icon"
                className="h-7 w-7"
                onClick={openInNewTab}
                title={t('wiseDesigner.openTab')}
              >
                <ExternalLink className="h-4 w-4" />
              </Button>
              <Button
                variant="ghost"
                size="icon"
                className="h-7 w-7"
                onClick={() => setFullscreen(!fullscreen)}
                title={t('wiseDesigner.fullscreen')}
              >
                <Maximize2 className="h-4 w-4" />
              </Button>
            </div>
          </div>

          {/* History panel */}
          {showHistory && artifacts.length > 0 && (
            <Card className="border-dashed">
              <CardHeader className="py-2 px-3">
                <CardTitle className="text-xs font-medium text-muted-foreground">
                  {t('wiseDesigner.historyTitle')} ({artifacts.length})
                </CardTitle>
              </CardHeader>
              <ScrollArea className="max-h-40">
                <div className="px-3 pb-2 space-y-1">
                  {artifacts.map((a) => (
                    <button
                      key={a.artifactId}
                      onClick={() => loadHistoryArtifact(a.artifactId)}
                      className={`w-full text-left text-xs px-2 py-1.5 rounded hover:bg-accent flex items-start gap-2 ${
                        current.artifactId === a.artifactId
                          ? 'bg-accent font-medium'
                          : 'text-muted-foreground'
                      }`}
                    >
                      <Clock className="h-3 w-3 mt-0.5 shrink-0" />
                      <div className="min-w-0">
                        <div className="truncate">{a.description}</div>
                        <div className="opacity-60">
                          {new Date(a.createdAt).toLocaleTimeString()}
                        </div>
                      </div>
                    </button>
                  ))}
                </div>
              </ScrollArea>
            </Card>
          )}

          {/* Iframe preview */}
          <div
            className={`rounded-lg border bg-white overflow-hidden transition-all ${
              fullscreen
                ? 'fixed inset-4 z-50 shadow-2xl'
                : 'relative'
            }`}
          >
            {fullscreen && (
              <div className="absolute top-2 right-2 z-10">
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => setFullscreen(false)}
                  className="shadow-md"
                >
                  {t('common.close')}
                </Button>
              </div>
            )}
            {loading ? (
              <div className="flex items-center justify-center h-96">
                <div className="text-center space-y-2">
                  <Loader2 className="h-8 w-8 animate-spin text-purple-500 mx-auto" />
                  <p className="text-sm text-muted-foreground">{t('wiseDesigner.generating')}</p>
                </div>
              </div>
            ) : (
              <iframe
                ref={iframeRef}
                srcDoc={current.htmlContent}
                sandbox="allow-scripts allow-same-origin"
                className={`w-full border-0 ${fullscreen ? 'h-full' : 'h-[500px]'}`}
                title={`${pitchName} — UI Prototype`}
              />
            )}
          </div>

          {/* Iterate / chat bar */}
          <div className="flex gap-2">
            <Input
              placeholder={t('wiseDesigner.iteratePlaceholder')}
              value={iterInstruction}
              onChange={(e) => setIterInstruction(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleIterate()}
              disabled={loading}
              className="text-sm"
            />
            <Button
              onClick={handleIterate}
              disabled={loading || !iterInstruction.trim()}
              size="icon"
              className="shrink-0"
              title={t('wiseDesigner.iterateBtn')}
            >
              {loading ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Send className="h-4 w-4" />
              )}
            </Button>
          </div>
          <p className="text-xs text-muted-foreground">{t('wiseDesigner.iterateHint')}</p>
        </div>
      )}
    </div>
  );
};
