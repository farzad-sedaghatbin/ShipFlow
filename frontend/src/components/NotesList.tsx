import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import {
  Plus,
  Pencil,
  Trash2,
  ChevronDown,
  ChevronUp,
  StickyNote,
  Brain,
  Loader2,
} from 'lucide-react';
import { qaService, NoteDTO, CreateNoteRequest } from '../services/qaService';

import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';
import { Checkbox } from './ui/checkbox';
import { Label } from './ui/label';
import { Alert, AlertDescription } from './ui/alert';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from './ui/tooltip';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from './ui/collapsible';

interface NotesListProps {
  contextType: 'pitch' | 'meeting' | 'team' | 'cycle';
  contextId: number;
  title?: string;
}

export const NotesList: React.FC<NotesListProps> = ({
  contextType,
  contextId,
  title = 'Notes',
}) => {
  const { i18n, t } = useTranslation();
  const [notes, setNotes] = useState<NoteDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState(true);
  const [expandedNoteIds, setExpandedNoteIds] = useState<Set<number>>(new Set());
  const toggleNoteExpand = (id: number) =>
    setExpandedNoteIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  const CONTENT_CLAMP_THRESHOLD = 120; // chars
  const needsToggle = (content: string) =>
    content.length > CONTENT_CLAMP_THRESHOLD ||
    (content.match(/\n/g) || []).length >= 3; // 3+ newlines overflows line-clamp-3
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingNote, setEditingNote] = useState<NoteDTO | null>(null);
  const [deleteDialog, setDeleteDialog] = useState<{ open: boolean; note: NoteDTO | null }>({
    open: false,
    note: null,
  });
  const [formData, setFormData] = useState<CreateNoteRequest>({
    title: '',
    content: '',
    contextType,
    contextId,
    includeInKnowledge: true,
  });
  const [focusedNoteIndex, setFocusedNoteIndex] = useState<number>(-1);
  const notesListRef = useRef<HTMLDivElement>(null);

  // Keyboard navigation handler for notes list
  const handleKeyDown = useCallback((event: React.KeyboardEvent, noteIndex: number, note: NoteDTO) => {
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        if (noteIndex < notes.length - 1) {
          setFocusedNoteIndex(noteIndex + 1);
        }
        break;
      case 'ArrowUp':
        event.preventDefault();
        if (noteIndex > 0) {
          setFocusedNoteIndex(noteIndex - 1);
        }
        break;
      case 'Enter':
      case ' ':
        event.preventDefault();
        handleOpenDialog(note);
        break;
      case 'Delete':
      case 'Backspace':
        if (event.shiftKey) {
          event.preventDefault();
          setDeleteDialog({ open: true, note });
        }
        break;
      case 'Home':
        event.preventDefault();
        setFocusedNoteIndex(0);
        break;
      case 'End':
        event.preventDefault();
        setFocusedNoteIndex(notes.length - 1);
        break;
      default:
        break;
    }
  }, [notes.length]);

  // Focus management for notes list
  useEffect(() => {
    if (focusedNoteIndex >= 0 && notesListRef.current) {
      const noteCards = notesListRef.current.querySelectorAll('[role="article"]');
      const targetCard = noteCards[focusedNoteIndex] as HTMLElement;
      if (targetCard) {
        targetCard.focus();
      }
    }
  }, [focusedNoteIndex]);

  useEffect(() => {
    loadNotes();
  }, [contextType, contextId]);

  const loadNotes = async () => {
    try {
      setLoading(true);
      const response = await qaService.getNotesByContext(contextType, contextId);
      setNotes(response.data);
      setError(null);
    } catch (err: any) {
      setError(t('errors.loadNotesFailed'));
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (note?: NoteDTO) => {
    if (note) {
      setEditingNote(note);
      setFormData({
        title: note.title,
        content: note.content,
        contextType: note.contextType,
        contextId: note.contextId,
        includeInKnowledge: note.includeInKnowledge ?? true,
      });
    } else {
      setEditingNote(null);
      setFormData({
        title: '',
        content: '',
        contextType,
        contextId,
        includeInKnowledge: true,
      });
    }
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setEditingNote(null);
  };

  const handleSave = async () => {
    try {
      if (editingNote) {
        await qaService.updateNote(editingNote.id, formData);
      } else {
        await qaService.createNote(formData);
      }
      handleCloseDialog();
      loadNotes();
    } catch (err: any) {
      setError(t('errors.saveNoteFailed'));
      console.error(err);
    }
  };

  const handleDelete = async () => {
    if (!deleteDialog.note) return;
    
    try {
      await qaService.deleteNote(deleteDialog.note.id);
      setDeleteDialog({ open: false, note: null });
      loadNotes();
    } catch (err: any) {
      setError(t('errors.deleteNoteFailed'));
      console.error(err);
    }
  };

  return (
    <Card>
      <Collapsible open={expanded} onOpenChange={setExpanded}>
        {/* Header */}
        <CardHeader className="flex flex-row items-center justify-between py-3 px-4 bg-muted/50">
          <div className="flex items-center gap-2">
            <StickyNote className="h-5 w-5 text-primary" />
            <CardTitle className="text-base">{title}</CardTitle>
            <Badge variant="secondary" className="text-xs">
              {notes.length}
            </Badge>
          </div>
          <div className="flex items-center gap-1">
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8"
                    onClick={() => handleOpenDialog()}
                  >
                    <Plus className="h-4 w-4" />
                    <span className="sr-only">Add new note</span>
                  </Button>
                </TooltipTrigger>
                <TooltipContent>Add new note (Alt+N)</TooltipContent>
              </Tooltip>
            </TooltipProvider>
            <CollapsibleTrigger asChild>
              <Button variant="ghost" size="icon" className="h-8 w-8">
                {expanded ? (
                  <ChevronUp className="h-4 w-4" />
                ) : (
                  <ChevronDown className="h-4 w-4" />
                )}
                <span className="sr-only">
                  {expanded ? 'Collapse notes list' : 'Expand notes list'}
                </span>
              </Button>
            </CollapsibleTrigger>
          </div>
        </CardHeader>

        <CollapsibleContent>
          <CardContent className="pt-4">
            {loading ? (
              <div className="flex justify-center py-8">
                <Loader2 className="h-6 w-6 animate-spin text-primary" />
                <span className="sr-only">Loading notes, please wait...</span>
              </div>
            ) : error ? (
              <Alert variant="destructive">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            ) : notes.length === 0 ? (
              <div className="text-center py-8">
                <p className="text-muted-foreground mb-3">
                  No notes yet. Add a note to capture important information.
                </p>
                <Button variant="outline" onClick={() => handleOpenDialog()}>
                  <Plus className="h-4 w-4 mr-2" />
                  Add First Note
                </Button>
              </div>
            ) : (
              <div ref={notesListRef} className="space-y-3" role="list">
                <span className="sr-only">
                  {notes.length} {notes.length === 1 ? 'note' : 'notes'}. 
                  Use arrow keys to navigate between notes, Enter to edit, Shift+Delete to delete.
                </span>
                {notes.map((note, index) => (
                  <div
                    key={note.id}
                    role="article"
                    tabIndex={0}
                    onKeyDown={(e) => handleKeyDown(e, index, note)}
                    className="p-4 rounded-lg border border-border bg-card hover:border-primary/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring transition-colors"
                  >
                    <div className="flex justify-between items-start mb-2">
                      <h3 className="font-semibold text-foreground">{note.title}</h3>
                      {note.includeInKnowledge && (
                        <TooltipProvider>
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <Brain className="h-4 w-4 text-primary" />
                            </TooltipTrigger>
                            <TooltipContent>Included in AI knowledge base</TooltipContent>
                          </Tooltip>
                        </TooltipProvider>
                      )}
                    </div>
                    <p className={`text-sm text-muted-foreground mb-1 whitespace-pre-wrap ${!expandedNoteIds.has(note.id) ? 'line-clamp-3' : ''}`}>
                      {note.content}
                    </p>
                    {note.content && needsToggle(note.content) && (
                      <button
                        onClick={(e) => { e.stopPropagation(); toggleNoteExpand(note.id); }}
                        className="text-xs text-primary hover:underline mb-2 focus:outline-none"
                      >
                        {expandedNoteIds.has(note.id) ? t('common.showLess') : t('common.showMore')}
                      </button>
                    )}
                    <div className="flex items-center justify-between">
                      <span className="text-xs text-muted-foreground">
                        {note.authorName && `By ${note.authorName} • `}
                        {formatLocalizedDate(new Date(note.createdAt), i18n.language)}
                      </span>
                      <div className="flex gap-1">
                        <TooltipProvider>
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-7 w-7"
                                onClick={() => handleOpenDialog(note)}
                              >
                                <Pencil className="h-3.5 w-3.5" />
                                <span className="sr-only">Edit note: {note.title}</span>
                              </Button>
                            </TooltipTrigger>
                            <TooltipContent>Edit note</TooltipContent>
                          </Tooltip>
                        </TooltipProvider>
                        <TooltipProvider>
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-7 w-7 text-destructive hover:text-destructive"
                                onClick={() => setDeleteDialog({ open: true, note })}
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                                <span className="sr-only">Delete note: {note.title}</span>
                              </Button>
                            </TooltipTrigger>
                            <TooltipContent>Delete note</TooltipContent>
                          </Tooltip>
                        </TooltipProvider>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </CollapsibleContent>
      </Collapsible>

      {/* Add/Edit Dialog */}
      <Dialog open={dialogOpen} onOpenChange={(open) => !open && handleCloseDialog()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editingNote ? 'Edit Note' : 'Add Note'}</DialogTitle>
            <DialogDescription>
              {editingNote ? 'Edit the note title and content below.' : 'Enter a title and content for your new note.'}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="note-title">Title</Label>
              <Input
                id="note-title"
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                placeholder="Enter note title"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="note-content">Content</Label>
              <Textarea
                id="note-content"
                value={formData.content}
                onChange={(e) => setFormData({ ...formData, content: e.target.value })}
                placeholder="Enter note content"
                rows={6}
              />
            </div>
            <div className="flex items-start gap-3">
              <Checkbox
                id="include-knowledge"
                checked={formData.includeInKnowledge}
                onCheckedChange={(checked) => 
                  setFormData({ ...formData, includeInKnowledge: checked as boolean })
                }
              />
              <div className="space-y-1">
                <Label htmlFor="include-knowledge" className="flex items-center gap-2 cursor-pointer">
                  <Brain className="h-4 w-4 text-primary" />
                  Include in AI knowledge base
                </Label>
                <p className="text-xs text-muted-foreground">
                  When enabled, this note will be used to answer Q&A questions.
                </p>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={handleCloseDialog}>
              Cancel
            </Button>
            <Button 
              onClick={handleSave}
              disabled={!formData.title.trim() || !formData.content.trim()}
            >
              {editingNote ? 'Update' : 'Create'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialog.open} onOpenChange={(open) => !open && setDeleteDialog({ open: false, note: null })}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete Note</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete "{deleteDialog.note?.title}"? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialog({ open: false, note: null })}>
              Cancel
            </Button>
            <Button variant="destructive" onClick={handleDelete}>
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  );
};

export default NotesList;
