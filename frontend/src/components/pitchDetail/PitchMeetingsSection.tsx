import { useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus, ChevronDown, ChevronUp, X } from 'lucide-react';
import { formatLocalizedDate } from '../../utils/dateLocalization';
import { LocalizedDateInput } from '../LocalizedDateInput';
import { Meeting, CreateMeetingRequest, MeetingType } from '../../types';
import { MeetingTypeConfig } from '../../types/organizationSettings';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import { Badge } from '../ui/badge';
import { Checkbox } from '../ui/checkbox';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../ui/select';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '../ui/collapsible';
import { cn } from '../../lib/utils';

interface PitchMeetingsSectionProps {
  meetings: Meeting[];
  meetingDialog: boolean;
  viewMeetingDialog: boolean;
  viewMeeting: Meeting | null;
  newMeeting: CreateMeetingRequest;
  meetingDate: string;
  meetingPendingDocs: File[];
  showMeetingDocUpload: boolean;
  meetingTypeConfigs: MeetingTypeConfig[];
  language: string;
  getMeetingTypeDisplayName: (type: MeetingType) => string;
  onSetMeetingDialog: (open: boolean) => void;
  onSetViewMeetingDialog: (open: boolean) => void;
  onOpenMeetingDialog: () => void;
  onCreateMeeting: () => void;
  onViewMeeting: (meetingId: number) => void;
  onMeetingTypeChange: (type: MeetingType) => void;
  onNewMeetingChange: (meeting: CreateMeetingRequest) => void;
  onMeetingDateChange: (date: string) => void;
  onMeetingPendingFileSelect: (files: FileList) => void;
  onRemoveMeetingPendingDoc: (index: number) => void;
  onSetShowMeetingDocUpload: (show: boolean) => void;
  onToggleChecklistItem: (listType: 'dor' | 'dod', itemId: number) => void;
}

export function PitchMeetingsSection({
  meetings,
  meetingDialog,
  viewMeetingDialog,
  viewMeeting,
  newMeeting,
  meetingDate,
  meetingPendingDocs,
  showMeetingDocUpload,
  meetingTypeConfigs,
  language,
  getMeetingTypeDisplayName,
  onSetMeetingDialog,
  onSetViewMeetingDialog,
  onOpenMeetingDialog,
  onCreateMeeting,
  onViewMeeting,
  onMeetingTypeChange,
  onNewMeetingChange,
  onMeetingDateChange,
  onMeetingPendingFileSelect,
  onRemoveMeetingPendingDoc,
  onSetShowMeetingDocUpload,
  onToggleChecklistItem,
}: PitchMeetingsSectionProps) {
  const { t } = useTranslation();
  const meetingDocUploadRef = useRef<HTMLInputElement>(null);

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle>{t('pitchDetailPage.meetings')}</CardTitle>
            <Button size="sm" onClick={onOpenMeetingDialog}>
              <Plus className="h-4 w-4 mr-1" />
              {t('pitchDetailPage.add')}
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {meetings.length === 0 ? (
            <p className="text-muted-foreground">{t('pitchDetailPage.noMeetings')}</p>
          ) : (
            <div className="space-y-1">
              {[...meetings].sort((a, b) => new Date(b.dateHeld).getTime() - new Date(a.dateHeld).getTime()).map((m, index) => (
                <div key={m.id}>
                  <div className="py-3">
                    <div className="flex gap-2 items-center mb-2">
                      <Badge
                        variant="outline"
                        className="cursor-pointer hover:opacity-80"
                        onClick={() => onViewMeeting(m.id)}
                      >
                        {getMeetingTypeDisplayName(m.type)}
                      </Badge>
                      <span className="text-sm text-muted-foreground">
                        {formatLocalizedDate(new Date(m.dateHeld), language)}
                      </span>
                    </div>
                    <div className="flex gap-2 mb-2">
                      <Badge
                        variant={m.dorReady ? 'success' : 'outline'}
                        className={cn(!m.dorReady && 'text-muted-foreground')}
                      >
                        DOR
                      </Badge>
                      <Badge
                        variant={m.dodReady ? 'success' : 'outline'}
                        className={cn(!m.dodReady && 'text-muted-foreground')}
                      >
                        DOD
                      </Badge>
                    </div>
                    {m.notes && (
                      <p className="text-sm text-muted-foreground">{m.notes}</p>
                    )}
                  </div>
                  {index < meetings.length - 1 && (
                    <div className="border-b border-border" />
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Add Meeting Dialog */}
      <Dialog open={meetingDialog} onOpenChange={onSetMeetingDialog}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t('pitchDetailPage.addMeeting')}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="meeting-type">{t('pitchDetailPage.type')} *</Label>
                <Select
                  value={newMeeting.type}
                  onValueChange={(value) => onMeetingTypeChange(value as MeetingType)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t('pitchDetailPage.selectType')} />
                  </SelectTrigger>
                  <SelectContent>
                    {meetingTypeConfigs.filter(c => c.isActive).map(config => (
                      <SelectItem key={config.name} value={config.name}>
                        {config.displayName}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="meeting-date">{t('pitchDetailPage.date')} *</Label>
                <LocalizedDateInput
                  id="meeting-date"
                  value={meetingDate}
                  onChange={onMeetingDateChange}
                  aria-required="true"
                />
              </div>
            </div>

            {/* DOR Checklist */}
            {(newMeeting.dorItems && newMeeting.dorItems.length > 0) && (
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  {t('pitchDetailPage.dor')}
                  {newMeeting.dorReady ? (
                    <span className="text-xs text-green-600 font-medium">✓ {t('pitchDetailPage.ready')}</span>
                  ) : (
                    <span className="text-xs text-amber-600 font-medium">({t('pitchDetailPage.pending')})</span>
                  )}
                </Label>
                <div className="border rounded-md p-2 space-y-1 max-h-32 overflow-y-auto">
                  {newMeeting.dorItems.map((item, index) => (
                    <div key={item.id ?? index} className="flex items-center gap-2">
                      <input
                        type="checkbox"
                        id={`dor-${item.id ?? index}`}
                        checked={item.isCompleted}
                        onChange={() => onToggleChecklistItem('dor', item.id ?? index)}
                        className="h-4 w-4 rounded border-gray-300"
                      />
                      <label
                        htmlFor={`dor-${item.id ?? index}`}
                        className={`text-sm flex-1 ${item.isCompleted ? 'line-through text-muted-foreground' : ''}`}
                      >
                        {item.name}
                        {item.isRequired && <span className="text-red-500 ml-1">*</span>}
                      </label>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* DOD Checklist */}
            {(newMeeting.dodItems && newMeeting.dodItems.length > 0) && (
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  {t('pitchDetailPage.dod')}
                  {newMeeting.dodReady ? (
                    <span className="text-xs text-green-600 font-medium">✓ {t('pitchDetailPage.ready')}</span>
                  ) : (
                    <span className="text-xs text-amber-600 font-medium">({t('pitchDetailPage.pending')})</span>
                  )}
                </Label>
                <div className="border rounded-md p-2 space-y-1 max-h-32 overflow-y-auto">
                  {newMeeting.dodItems.map((item, index) => (
                    <div key={item.id ?? index} className="flex items-center gap-2">
                      <input
                        type="checkbox"
                        id={`dod-${item.id ?? index}`}
                        checked={item.isCompleted}
                        onChange={() => onToggleChecklistItem('dod', item.id ?? index)}
                        className="h-4 w-4 rounded border-gray-300"
                      />
                      <label
                        htmlFor={`dod-${item.id ?? index}`}
                        className={`text-sm flex-1 ${item.isCompleted ? 'line-through text-muted-foreground' : ''}`}
                      >
                        {item.name}
                        {item.isRequired && <span className="text-red-500 ml-1">*</span>}
                      </label>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="meeting-notes">{t('pitchDetailPage.meetingNotes')}</Label>
              <Textarea
                id="meeting-notes"
                value={newMeeting.notes}
                onChange={(e) =>
                  onNewMeetingChange({ ...newMeeting, notes: e.target.value })
                }
                rows={2}
              />
            </div>
            <div>
              <Collapsible
                open={showMeetingDocUpload}
                onOpenChange={onSetShowMeetingDocUpload}
              >
                <CollapsibleTrigger asChild>
                  <Button variant="ghost" size="sm" className="p-0">
                    {showMeetingDocUpload ? (
                      <ChevronUp className="h-4 w-4 mr-1" />
                    ) : (
                      <ChevronDown className="h-4 w-4 mr-1" />
                    )}
                    {showMeetingDocUpload ? t('pitchDetailPage.hideDocuments') : t('pitchDetailPage.addDocuments')} (MOM, etc.)
                  </Button>
                </CollapsibleTrigger>
                <CollapsibleContent className="mt-3">
                  <p className="text-sm text-muted-foreground mb-2">
                    {t('pitchDetailPage.meetingDocsDesc')}
                  </p>
                  <div
                    className="border-2 border-dashed border-border rounded-md p-4 text-center cursor-pointer hover:border-primary hover:bg-accent transition-colors"
                    onClick={() => meetingDocUploadRef.current?.click()}
                    onDragOver={(e) => e.preventDefault()}
                    onDrop={(e) => {
                      e.preventDefault();
                      if (e.dataTransfer.files.length > 0) {
                        onMeetingPendingFileSelect(e.dataTransfer.files);
                      }
                    }}
                  >
                    <input
                      ref={meetingDocUploadRef}
                      type="file"
                      hidden
                      multiple
                      accept=".pdf,.doc,.docx,.txt,.md"
                      onChange={(e) =>
                        e.target.files &&
                        onMeetingPendingFileSelect(e.target.files)
                      }
                    />
                    <p className="text-muted-foreground">
                      {t('pitchDetailPage.dropFiles')}
                    </p>
                  </div>
                  {meetingPendingDocs.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-1">
                      {meetingPendingDocs.map((file, index) => (
                        <Badge
                          key={index}
                          variant="secondary"
                          className="pr-1 gap-1"
                        >
                          {file.name}
                          <button
                            type="button"
                            onClick={() => onRemoveMeetingPendingDoc(index)}
                            className="ml-1 rounded-full hover:bg-muted p-0.5"
                          >
                            <X className="h-3 w-3" />
                          </button>
                        </Badge>
                      ))}
                    </div>
                  )}
                </CollapsibleContent>
              </Collapsible>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => onSetMeetingDialog(false)}>
              {t('pitchDetailPage.cancel')}
            </Button>
            <Button onClick={onCreateMeeting}>{t('pitchDetailPage.add')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* View Meeting Dialog (Read-only, shows only completed items) */}
      {viewMeeting && (
        <Dialog open={viewMeetingDialog} onOpenChange={onSetViewMeetingDialog}>
          <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>{t('meetingList.dialog.viewTitle')}</DialogTitle>
            </DialogHeader>
            <div className="grid gap-4 py-4">
              {/* Meeting Info */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>{t('meetingList.dialog.type')}</Label>
                  <div className="text-sm font-medium">{getMeetingTypeDisplayName(viewMeeting.type)}</div>
                </div>
                <div className="space-y-2">
                  <Label>{t('meetingList.dialog.date')}</Label>
                  <div className="text-sm">{formatLocalizedDate(new Date(viewMeeting.dateHeld), language)}</div>
                </div>
              </div>

              {viewMeeting.attendees && (
                <div className="space-y-2">
                  <Label>{t('meetingList.dialog.attendees')}</Label>
                  <div className="text-sm whitespace-pre-wrap">{viewMeeting.attendees}</div>
                </div>
              )}

              {/* DOR Items */}
              {viewMeeting.dorItems && viewMeeting.dorItems.length > 0 && (
                <div className="space-y-2">
                  <Label>{t('meetingList.dialog.dor')}</Label>
                  <div className="space-y-2">
                    {viewMeeting.dorItems.map((item, index) => (
                      <div key={index} className={`flex items-start gap-2 text-sm ${!item.isCompleted ? 'opacity-50' : ''}`}>
                        <Checkbox checked={item.isCompleted} disabled className="mt-0.5" />
                        <div className="flex-1">
                          <div className={`font-medium ${item.isCompleted ? '' : 'line-through'}`}>{item.name}</div>
                          {item.description && (
                            <div className="text-muted-foreground text-xs mt-1">{item.description}</div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* DOD Items */}
              {viewMeeting.dodItems && viewMeeting.dodItems.length > 0 && (
                <div className="space-y-2">
                  <Label>{t('meetingList.dialog.dod')}</Label>
                  <div className="space-y-2">
                    {viewMeeting.dodItems.map((item, index) => (
                      <div key={index} className={`flex items-start gap-2 text-sm ${!item.isCompleted ? 'opacity-50' : ''}`}>
                        <Checkbox checked={item.isCompleted} disabled className="mt-0.5" />
                        <div className="flex-1">
                          <div className={`font-medium ${item.isCompleted ? '' : 'line-through'}`}>{item.name}</div>
                          {item.description && (
                            <div className="text-muted-foreground text-xs mt-1">{item.description}</div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {viewMeeting.notes && (
                <div className="space-y-2">
                  <Label>{t('meetingList.dialog.notes')}</Label>
                  <div className="text-sm whitespace-pre-wrap bg-muted p-3 rounded-md">{viewMeeting.notes}</div>
                </div>
              )}

              {viewMeeting.decisions && (
                <div className="space-y-2">
                  <Label>{t('meetingList.dialog.decisions')}</Label>
                  <div className="text-sm whitespace-pre-wrap bg-muted p-3 rounded-md">{viewMeeting.decisions}</div>
                </div>
              )}

              {/* Action Items */}
              {viewMeeting.actions && viewMeeting.actions.length > 0 && (
                <div className="space-y-2">
                  <Label>{t('meetingList.actionItems.title')}</Label>
                  <div className="space-y-2">
                    {viewMeeting.actions.map((action, index) => (
                      <Card key={index}>
                        <CardContent className="p-4">
                          <div className="flex flex-col gap-2">
                            <div className="flex items-start justify-between gap-2">
                              <div className="text-sm flex-1">{action.description}</div>
                              <Badge variant={action.status === 'COMPLETED' ? 'success' : action.status === 'IN_PROGRESS' ? 'warning' : 'outline'}>
                                {action.status === 'COMPLETED' ? t('meetingList.actionItems.completed') :
                                 action.status === 'IN_PROGRESS' ? t('meetingList.actionItems.inProgress') :
                                 t('meetingList.actionItems.open')}
                              </Badge>
                            </div>
                            {action.assignedToName && (
                              <div className="text-xs text-muted-foreground">
                                {t('meetingList.actionItems.assignedTo')}: {action.assignedToName}
                              </div>
                            )}
                            {action.dueDate && (
                              <div className="text-xs text-muted-foreground">
                                {t('meetingList.actionItems.dueDate')}: {formatLocalizedDate(new Date(action.dueDate), language)}
                              </div>
                            )}
                            {action.notes && (
                              <div className="text-xs text-muted-foreground mt-1">{action.notes}</div>
                            )}
                          </div>
                        </CardContent>
                      </Card>
                    ))}
                  </div>
                </div>
              )}
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => onSetViewMeetingDialog(false)}>
                {t('meetingList.dialog.close')}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </>
  );
}
