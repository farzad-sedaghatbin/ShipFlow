import { useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { PermissionGate } from '../../hooks/usePermission';
import {
  X,
  AlertTriangle,
  Lightbulb,
  Ban,
  Link2,
  Target,
  Edit2,
  Save,
  Loader2,
  Sparkles,
  FileUp,
  Clock,
} from 'lucide-react';
import { Pitch } from '../../types';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import { Markdown } from '../ui/markdown';
import MarkdownEditor from '../MarkdownEditor';
import { Badge } from '../ui/badge';

export interface ShapeUpFields {
  problemStatement: string;
  solution: string;
  rabbitHoles: string;
  risks: string;
  noGos: string;
  wireframeLinks: string;
  appetiteDays?: number;
}

interface PitchShapingSectionProps {
  pitch: Pitch;
  editingShapeUp: boolean;
  savingShapeUp: boolean;
  shapeUpFields: ShapeUpFields;
  extracting: boolean;
  extractedDocumentName: string;
  hasShapeUpContent: boolean;
  onSetEditingShapeUp: (value: boolean) => void;
  onSaveShapeUp: () => void;
  onCancelShapeUpEdit: () => void;
  onExtractFromDocument: (file: File) => void;
  onShapeUpFieldChange: (fields: ShapeUpFields) => void;
}

export function PitchShapingSection({
  pitch,
  editingShapeUp,
  savingShapeUp,
  shapeUpFields,
  extracting,
  extractedDocumentName,
  hasShapeUpContent,
  onSetEditingShapeUp,
  onSaveShapeUp,
  onCancelShapeUpEdit,
  onExtractFromDocument,
  onShapeUpFieldChange,
}: PitchShapingSectionProps) {
  const { t } = useTranslation();
  const extractUploadRef = useRef<HTMLInputElement>(null);

  return (
    <Card className="mb-6">
      <CardHeader>
        <div className="flex justify-between items-center">
          <CardTitle className="flex items-center gap-2">
            <Target className="h-5 w-5 text-primary" />
            {t('pitchDetailPage.shapeUpDetails')}
          </CardTitle>
          {!editingShapeUp ? (
            <PermissionGate resource="PITCH" permission="UPDATE">
              <Button variant="outline" size="sm" onClick={() => onSetEditingShapeUp(true)}>
                <Edit2 className="h-4 w-4 mr-1" />
                {t('pitchDetailPage.editShapeUp')}
              </Button>
            </PermissionGate>
          ) : (
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={onCancelShapeUpEdit} disabled={savingShapeUp}>
                {t('pitchDetailPage.cancelEdit')}
              </Button>
              <Button size="sm" onClick={onSaveShapeUp} disabled={savingShapeUp}>
                {savingShapeUp ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Save className="h-4 w-4 mr-1" />}
                {savingShapeUp ? t('pitchDetailPage.saving') : t('pitchDetailPage.saveShapeUp')}
              </Button>
            </div>
          )}
        </div>
      </CardHeader>
      <CardContent>
        {editingShapeUp ? (
          // Edit Mode
          <div className="space-y-4">
            {/* AI Extraction Section */}
            <div className="bg-gradient-to-r from-purple-50 to-blue-50 dark:from-purple-950/30 dark:to-blue-950/30 rounded-lg p-4 border border-purple-200 dark:border-purple-800">
              <div className="flex items-center gap-2 mb-2">
                <Sparkles className="h-5 w-5 text-purple-500" />
                <h4 className="font-semibold">{t('pitchDetailPage.aiExtraction')}</h4>
              </div>
              <p className="text-sm text-muted-foreground mb-3">
                {t('pitchDetailPage.aiExtractionDescription')}
              </p>
              {extractedDocumentName && (
                <div className="mb-3 bg-green-50 dark:bg-green-950/30 rounded-lg p-3 border border-green-200 dark:border-green-800">
                  <div className="flex items-center gap-2">
                    <FileUp className="h-4 w-4 text-green-600 dark:text-green-400" />
                    <div className="flex-1">
                      <p className="text-sm font-medium text-green-900 dark:text-green-100">{t('pitchBoard.documentExtracted')}</p>
                      <p className="text-xs text-green-700 dark:text-green-300">{extractedDocumentName}</p>
                    </div>
                    <Badge variant="outline" className="bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300 border-green-300 dark:border-green-700">
                      {t('pitchBoard.processed')}
                    </Badge>
                  </div>
                </div>
              )}
              <div
                className="border-2 border-dashed border-purple-300 dark:border-purple-700 rounded-lg p-4 text-center cursor-pointer hover:border-purple-500 hover:bg-purple-50/50 dark:hover:bg-purple-950/50 transition-colors"
                onClick={() => !extracting && extractUploadRef.current?.click()}
                onDragOver={(e) => e.preventDefault()}
                onDrop={(e) => {
                  e.preventDefault();
                  if (e.dataTransfer.files.length > 0) {
                    onExtractFromDocument(e.dataTransfer.files[0]);
                  }
                }}
              >
                <input
                  ref={extractUploadRef}
                  type="file"
                  hidden
                  accept=".pdf,.doc,.docx,.txt,.md"
                  onChange={(e) => e.target.files?.[0] && onExtractFromDocument(e.target.files[0])}
                  disabled={extracting}
                />
                {extracting ? (
                  <>
                    <Loader2 className="h-8 w-8 mx-auto text-purple-500 animate-spin mb-2" />
                    <p className="text-sm text-purple-600 dark:text-purple-400">{t('pitchDetailPage.extracting')}</p>
                  </>
                ) : (
                  <>
                    <Sparkles className="h-8 w-8 mx-auto text-purple-400 mb-2" />
                    <p className="text-sm text-muted-foreground">{t('pitchDetailPage.dropDocument')}</p>
                  </>
                )}
              </div>
            </div>

            {/* Problem Statement */}
            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <AlertTriangle className="h-4 w-4 text-orange-500" />
                {t('pitchDetailPage.problemStatement')}
              </Label>
              <MarkdownEditor
                value={shapeUpFields.problemStatement}
                onChange={(value) => onShapeUpFieldChange({ ...shapeUpFields, problemStatement: value })}
                placeholder={t('pitchDetailPage.problemPlaceholder')}
                rows={3}
              />
            </div>

            {/* Solution */}
            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <Lightbulb className="h-4 w-4 text-yellow-500" />
                {t('pitchDetailPage.solution')}
              </Label>
              <MarkdownEditor
                value={shapeUpFields.solution}
                onChange={(value) => onShapeUpFieldChange({ ...shapeUpFields, solution: value })}
                placeholder={t('pitchDetailPage.solutionPlaceholder')}
                rows={4}
              />
            </div>

            {/* Rabbit Holes */}
            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <Ban className="h-4 w-4 text-red-500" />
                {t('pitchDetailPage.rabbitHoles')}
              </Label>
              <MarkdownEditor
                value={shapeUpFields.rabbitHoles}
                onChange={(value) => onShapeUpFieldChange({ ...shapeUpFields, rabbitHoles: value })}
                placeholder={t('pitchDetailPage.rabbitHolesPlaceholder')}
                rows={3}
              />
            </div>

            {/* Risks */}
            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <AlertTriangle className="h-4 w-4 text-amber-500" />
                {t('pitchDetailPage.risks')}
              </Label>
              <MarkdownEditor
                value={shapeUpFields.risks}
                onChange={(value) => onShapeUpFieldChange({ ...shapeUpFields, risks: value })}
                placeholder={t('pitchDetailPage.risksPlaceholder')}
                rows={3}
              />
            </div>

            {/* No-Gos */}
            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <X className="h-4 w-4 text-red-500" />
                {t('pitchDetailPage.noGos')}
              </Label>
              <MarkdownEditor
                value={shapeUpFields.noGos}
                onChange={(value) => onShapeUpFieldChange({ ...shapeUpFields, noGos: value })}
                placeholder={t('pitchDetailPage.noGosPlaceholder')}
                rows={2}
              />
            </div>

            {/* Wireframe Links */}
            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <Link2 className="h-4 w-4 text-blue-500" />
                {t('pitchDetailPage.wireframeLinks')}
              </Label>
              <Textarea
                value={shapeUpFields.wireframeLinks}
                onChange={(e) => onShapeUpFieldChange({ ...shapeUpFields, wireframeLinks: e.target.value })}
                placeholder={t('pitchDetailPage.wireframeLinksPlaceholder')}
                rows={2}
              />
            </div>

            {/* Appetite */}
            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <Clock className="h-4 w-4 text-green-500" />
                {t('pitches.appetite')} ({t('common.days')})
              </Label>
              <Input
                type="number"
                min={1}
                max={180}
                value={shapeUpFields.appetiteDays ?? ''}
                onChange={(e) => onShapeUpFieldChange({ ...shapeUpFields, appetiteDays: e.target.value ? Number(e.target.value) : undefined })}
                placeholder="e.g. 6"
                className="w-40"
              />
            </div>
          </div>
        ) : hasShapeUpContent ? (
          // Display Mode with content
          <div className="space-y-6">
            {pitch.problemStatement && (
              <div>
                <h4 className="font-semibold flex items-center gap-2 mb-2">
                  <AlertTriangle className="h-4 w-4 text-orange-500" />
                  {t('pitchDetailPage.problemStatement')}
                </h4>
                <Markdown content={pitch.problemStatement} className="text-muted-foreground" />
              </div>
            )}

            {pitch.solution && (
              <div>
                <h4 className="font-semibold flex items-center gap-2 mb-2">
                  <Lightbulb className="h-4 w-4 text-yellow-500" />
                  {t('pitchDetailPage.solution')}
                </h4>
                <Markdown content={pitch.solution} className="text-muted-foreground" />
              </div>
            )}

            {pitch.rabbitHoles && (
              <div>
                <h4 className="font-semibold flex items-center gap-2 mb-2">
                  <Ban className="h-4 w-4 text-red-500" />
                  {t('pitchDetailPage.rabbitHoles')}
                </h4>
                <Markdown content={pitch.rabbitHoles} className="text-muted-foreground" />
              </div>
            )}

            {pitch.risks && (
              <div>
                <h4 className="font-semibold flex items-center gap-2 mb-2">
                  <AlertTriangle className="h-4 w-4 text-amber-500" />
                  {t('pitchDetailPage.risks')}
                </h4>
                <Markdown content={pitch.risks} className="text-muted-foreground" />
              </div>
            )}

            {pitch.noGos && (
              <div>
                <h4 className="font-semibold flex items-center gap-2 mb-2">
                  <X className="h-4 w-4 text-red-500" />
                  {t('pitchDetailPage.noGos')}
                </h4>
                <Markdown content={pitch.noGos} className="text-muted-foreground" />
              </div>
            )}

            {pitch.wireframeLinks && (
              <div>
                <h4 className="font-semibold flex items-center gap-2 mb-2">
                  <Link2 className="h-4 w-4 text-blue-500" />
                  {t('pitchDetailPage.wireframeLinks')}
                </h4>
                <div className="space-y-1">
                  {pitch.wireframeLinks.split('\n').map((link, idx) => {
                    const trimmedLink = link.trim();
                    if (!trimmedLink) return null;
                    const isUrl = trimmedLink.startsWith('http://') || trimmedLink.startsWith('https://');
                    return (
                      <p key={idx}>
                        {isUrl ? (
                          <a
                            href={trimmedLink}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-primary hover:underline"
                          >
                            {trimmedLink}
                          </a>
                        ) : (
                          <span className="text-muted-foreground">{trimmedLink}</span>
                        )}
                      </p>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        ) : (
          // Empty state
          <div className="text-center py-8">
            <Target className="h-12 w-12 mx-auto text-muted-foreground/50 mb-3" />
            <p className="text-muted-foreground mb-4">
              {t('pitchDetailPage.noShapeUpDetails')}
            </p>
            <Button variant="outline" onClick={() => onSetEditingShapeUp(true)}>
              <Edit2 className="h-4 w-4 mr-2" />
              {t('pitchDetailPage.addShapeUpDetails')}
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
