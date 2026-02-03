import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDateTime } from '../utils/dateLocalization';
import { Send, MessageSquare, Pencil, Trash2, Loader2, MoreHorizontal, AtSign } from 'lucide-react';
import { Button } from './ui/button';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Textarea } from './ui/textarea';
import { Avatar, AvatarFallback } from './ui/avatar';
import { Badge } from './ui/badge';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from './ui/dropdown-menu';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from './ui/alert-dialog';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from './ui/tooltip';
import { cn } from '../lib/utils';
import commentService, { 
  Comment, 
  CommentEntityType, 
  CommentReaction,
  MentionUser,
  REACTION_EMOJIS 
} from '../services/commentService';
import UserProfilePopover from './UserProfilePopover';

interface CommentsProps {
  entityType: 'task' | 'bug';
  entityId: number;
  locale?: string;
}

const AVAILABLE_REACTIONS: CommentReaction[] = [
  'THUMBS_UP',
  'THUMBS_DOWN',
  'HEART',
  'LAUGH',
  'SURPRISED',
  'SAD',
  'ROCKET',
  'EYES',
];

/**
 * MentionLink component - renders a clickable @mention with user profile popover
 */
interface MentionLinkProps {
  name: string;
  cachedUser?: MentionUser;
  onFetchUser: (name: string) => Promise<MentionUser | null>;
}

const MentionLink: React.FC<MentionLinkProps> = ({ name, cachedUser, onFetchUser }) => {
  const [user, setUser] = useState<MentionUser | null>(cachedUser || null);
  const [loading, setLoading] = useState(false);
  
  const handleClick = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!user && !loading) {
      setLoading(true);
      const fetchedUser = await onFetchUser(name);
      setUser(fetchedUser);
      setLoading(false);
    }
  };

  // If we have user data, render with popover
  if (user) {
    return (
      <UserProfilePopover userId={user.id} displayName={user.displayName}>
        <button
          type="button"
          className="text-primary font-medium hover:underline cursor-pointer inline bg-transparent border-none p-0"
          onClick={(e) => e.stopPropagation()}
        >
          @{name}
        </button>
      </UserProfilePopover>
    );
  }

  // If no user data yet, render clickable mention that fetches on click
  return (
    <button
      type="button"
      className="text-primary font-medium hover:underline cursor-pointer inline bg-transparent border-none p-0"
      onClick={handleClick}
      disabled={loading}
    >
      @{name}
    </button>
  );
};

const Comments: React.FC<CommentsProps> = ({
  entityType,
  entityId,
  locale = 'en',
}) => {
  const { t } = useTranslation();
  const [comments, setComments] = useState<Comment[]>([]);
  const [newComment, setNewComment] = useState('');
  const [editingComment, setEditingComment] = useState<number | null>(null);
  const [editContent, setEditContent] = useState('');
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [commentToDelete, setCommentToDelete] = useState<number | null>(null);
  const [showReactionPicker, setShowReactionPicker] = useState<number | null>(null);
  
  // Mention state
  const [mentionSuggestions, setMentionSuggestions] = useState<MentionUser[]>([]);
  const [showMentionSuggestions, setShowMentionSuggestions] = useState(false);
  const [mentionQuery, setMentionQuery] = useState('');
  const [mentionStartIndex, setMentionStartIndex] = useState(-1);
  const [selectedMentionIndex, setSelectedMentionIndex] = useState(0);
  const [mentionUserCache, setMentionUserCache] = useState<Map<string, MentionUser>>(new Map());
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const mentionDropdownRef = useRef<HTMLDivElement>(null);

  const apiEntityType: CommentEntityType = entityType === 'task' ? 'TASK' : 'BUG_REPORT';

  const loadComments = useCallback(async () => {
    setLoading(true);
    try {
      const response = await commentService.getComments(apiEntityType, entityId);
      setComments(response.data);
    } catch (error) {
      console.error('Failed to load comments:', error);
    } finally {
      setLoading(false);
    }
  }, [apiEntityType, entityId]);

  useEffect(() => {
    loadComments();
  }, [loadComments]);

  // Search for users when mention query changes
  useEffect(() => {
    const searchMentions = async () => {
      if (mentionQuery.length > 0) {
        try {
          const response = await commentService.searchUsersForMention(mentionQuery);
          setMentionSuggestions(response.data);
          setSelectedMentionIndex(0);
        } catch (error) {
          console.error('Failed to search users:', error);
          setMentionSuggestions([]);
        }
      } else if (showMentionSuggestions) {
        // Show default suggestions when @ is typed
        try {
          const response = await commentService.searchUsersForMention('');
          setMentionSuggestions(response.data);
          setSelectedMentionIndex(0);
        } catch (error) {
          console.error('Failed to load users:', error);
          setMentionSuggestions([]);
        }
      }
    };

    const debounce = setTimeout(searchMentions, 150);
    return () => clearTimeout(debounce);
  }, [mentionQuery, showMentionSuggestions]);

  // Handle text input changes with mention detection
  const handleCommentChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const value = e.target.value;
    const cursorPos = e.target.selectionStart || 0;
    setNewComment(value);

    // Check for @ symbol to trigger mention suggestions
    const textBeforeCursor = value.substring(0, cursorPos);
    const lastAtIndex = textBeforeCursor.lastIndexOf('@');

    if (lastAtIndex !== -1) {
      const textAfterAt = textBeforeCursor.substring(lastAtIndex + 1);
      // Check if there's a space after @ (which would end the mention)
      if (!textAfterAt.includes(' ')) {
        setMentionStartIndex(lastAtIndex);
        setMentionQuery(textAfterAt);
        setShowMentionSuggestions(true);
        return;
      }
    }

    setShowMentionSuggestions(false);
    setMentionQuery('');
    setMentionStartIndex(-1);
  };

  // Handle keyboard navigation in mention suggestions
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (!showMentionSuggestions || mentionSuggestions.length === 0) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedMentionIndex((prev) => 
        prev < mentionSuggestions.length - 1 ? prev + 1 : 0
      );
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedMentionIndex((prev) => 
        prev > 0 ? prev - 1 : mentionSuggestions.length - 1
      );
    } else if (e.key === 'Enter' && showMentionSuggestions) {
      e.preventDefault();
      insertMention(mentionSuggestions[selectedMentionIndex]);
    } else if (e.key === 'Escape') {
      setShowMentionSuggestions(false);
    }
  };

  // Insert selected mention into the text
  const insertMention = (user: MentionUser) => {
    if (mentionStartIndex === -1) return;

    const beforeMention = newComment.substring(0, mentionStartIndex);
    const afterMention = newComment.substring(
      mentionStartIndex + mentionQuery.length + 1
    );
    // Use displayName (person's name) for the mention
    // If name has spaces, wrap in quotes for proper parsing
    const mentionText = user.displayName.includes(' ') 
      ? `@"${user.displayName}"` 
      : `@${user.displayName}`;
    const newValue = `${beforeMention}${mentionText} ${afterMention}`;
    
    setNewComment(newValue);
    setShowMentionSuggestions(false);
    setMentionQuery('');
    setMentionStartIndex(-1);
    
    // Focus back on textarea
    setTimeout(() => {
      if (textareaRef.current) {
        textareaRef.current.focus();
        const newCursorPos = beforeMention.length + mentionText.length + 1;
        textareaRef.current.setSelectionRange(newCursorPos, newCursorPos);
      }
    }, 0);
  };

  // Fetch and cache user profile for mention
  const fetchMentionUser = useCallback(async (name: string): Promise<MentionUser | null> => {
    // Check cache first
    if (mentionUserCache.has(name)) {
      return mentionUserCache.get(name) || null;
    }
    
    try {
      const response = await commentService.getUserByDisplayName(name);
      const user = response.data;
      setMentionUserCache(prev => new Map(prev).set(name, user));
      return user;
    } catch {
      return null;
    }
  }, [mentionUserCache]);

  // Render comment content with highlighted and clickable mentions
  const renderCommentContent = (content: string) => {
    // Match both @"Full Name" and @SingleWord formats (word can include letters, numbers, dots, underscores)
    const mentionRegex = /@"([^"]+)"|@([\p{L}\p{N}._]+)/gu;
    const result: React.ReactNode[] = [];
    let lastIndex = 0;
    let match;

    while ((match = mentionRegex.exec(content)) !== null) {
      // Add text before the match
      if (match.index > lastIndex) {
        result.push(<span key={`text-${lastIndex}`}>{content.substring(lastIndex, match.index)}</span>);
      }
      // Add the highlighted mention (group 1 for quoted, group 2 for unquoted)
      const name = match[1] || match[2];
      const cachedUser = mentionUserCache.get(name);
      
      // Create clickable mention with profile popover
      result.push(
        <MentionLink 
          key={`mention-${match.index}`}
          name={name}
          cachedUser={cachedUser}
          onFetchUser={fetchMentionUser}
        />
      );
      lastIndex = match.index + match[0].length;
    }
    
    // Add remaining text after last match
    if (lastIndex < content.length) {
      result.push(<span key={`text-${lastIndex}`}>{content.substring(lastIndex)}</span>);
    }
    
    return result.length > 0 ? result : content;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    setSubmitting(true);
    try {
      const response = await commentService.createComment({
        content: newComment.trim(),
        entityType: apiEntityType,
        entityId,
      });
      setComments([...comments, response.data]);
      setNewComment('');
    } catch (error) {
      console.error('Failed to add comment:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = async (commentId: number) => {
    if (!editContent.trim()) return;

    setSubmitting(true);
    try {
      const response = await commentService.updateComment(commentId, {
        content: editContent.trim(),
      });
      setComments(comments.map(c => c.id === commentId ? response.data : c));
      setEditingComment(null);
      setEditContent('');
    } catch (error) {
      console.error('Failed to update comment:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!commentToDelete) return;

    try {
      await commentService.deleteComment(commentToDelete);
      setComments(comments.filter(c => c.id !== commentToDelete));
    } catch (error) {
      console.error('Failed to delete comment:', error);
    } finally {
      setDeleteDialogOpen(false);
      setCommentToDelete(null);
    }
  };

  const handleReaction = async (commentId: number, reactionType: CommentReaction) => {
    try {
      const response = await commentService.toggleReaction(commentId, { reactionType });
      setComments(comments.map(c => c.id === commentId ? response.data : c));
    } catch (error) {
      console.error('Failed to toggle reaction:', error);
    }
    setShowReactionPicker(null);
  };

  const getAuthorInitials = (name: string) => {
    return name
      .split(' ')
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  };

  const startEditing = (comment: Comment) => {
    setEditingComment(comment.id);
    setEditContent(comment.content);
  };

  const confirmDelete = (commentId: number) => {
    setCommentToDelete(commentId);
    setDeleteDialogOpen(true);
  };

  const getReactionSummary = (comment: Comment) => {
    const { reactionCounts, userReactions } = comment.reactions;
    const entries = Object.entries(reactionCounts) as [CommentReaction, number][];
    return entries
      .filter(([_, count]) => count > 0)
      .map(([type, count]) => ({
        type,
        emoji: REACTION_EMOJIS[type],
        count,
        userReacted: userReactions[type] || false,
      }));
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-lg">
          <MessageSquare className="h-5 w-5" />
          {t('comments.title')} ({comments.length})
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Add Comment Form */}
        <form onSubmit={handleSubmit} className="space-y-3">
          <div className="relative">
            <Textarea
              ref={textareaRef}
              placeholder={t('comments.placeholder', { entityType })}
              value={newComment}
              onChange={handleCommentChange}
              onKeyDown={handleKeyDown}
              rows={3}
              disabled={submitting}
            />
            
            {/* Mention Suggestions Dropdown */}
            {showMentionSuggestions && mentionSuggestions.length > 0 && (
              <div 
                ref={mentionDropdownRef}
                className="absolute left-0 right-0 mt-1 bg-popover border rounded-lg shadow-lg z-50 max-h-48 overflow-y-auto"
              >
                <div className="p-1">
                  <div className="px-2 py-1 text-xs text-muted-foreground flex items-center gap-1">
                    <AtSign className="h-3 w-3" />
                    {t('comments.mentionSuggestions')}
                  </div>
                  {mentionSuggestions.map((user, index) => (
                    <button
                      key={user.id}
                      type="button"
                      className={cn(
                        "w-full text-left px-2 py-2 rounded flex items-center gap-2 hover:bg-accent",
                        index === selectedMentionIndex && "bg-accent"
                      )}
                      onClick={() => insertMention(user)}
                    >
                      <Avatar className="h-6 w-6">
                        <AvatarFallback className="text-xs">
                          {getAuthorInitials(user.displayName)}
                        </AvatarFallback>
                      </Avatar>
                      <div className="flex flex-col">
                        <span className="text-sm font-medium">{user.displayName}</span>
                        <span className="text-xs text-muted-foreground">@{user.username}</span>
                      </div>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
          <div className="flex items-center justify-between">
            <span className="text-xs text-muted-foreground flex items-center gap-1">
              <AtSign className="h-3 w-3" />
              {t('comments.mentionHint')}
            </span>
            <Button
              type="submit"
              size="sm"
              disabled={!newComment.trim() || submitting}
              className="gap-2"
            >
              {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
              {submitting ? t('comments.posting') : t('comments.post')}
            </Button>
          </div>
        </form>

        {/* Loading State */}
        {loading && (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        )}

        {/* Comments List */}
        {!loading && (
          <div className="space-y-4">
            {comments.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                <MessageSquare className="h-12 w-12 mx-auto mb-3 opacity-30" />
                <p className="text-sm">{t('comments.noCommentsYet')}</p>
              </div>
            ) : (
              comments.map((comment) => (
                <div
                  key={comment.id}
                  className="flex gap-3 p-4 rounded-lg border bg-muted/30"
                >
                  <Avatar className="h-8 w-8 flex-shrink-0">
                    <AvatarFallback className="text-xs">
                      {getAuthorInitials(comment.authorName)}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex-1 space-y-2">
                    {/* Header */}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-medium text-sm">{comment.authorName}</span>
                        <Badge variant="outline" className="text-xs px-2 py-0">
                          {formatLocalizedDateTime(comment.createdAt, locale)}
                        </Badge>
                        {comment.isEdited && (
                          <Badge variant="secondary" className="text-xs px-2 py-0">
                            {t('comments.edited')}
                          </Badge>
                        )}
                      </div>
                      
                      {/* Actions Menu */}
                      {(comment.canEdit || comment.canDelete) && (
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon" className="h-8 w-8">
                              <MoreHorizontal className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            {comment.canEdit && (
                              <DropdownMenuItem onClick={() => startEditing(comment)}>
                                <Pencil className="mr-2 h-4 w-4" />
                                {t('common.edit')}
                              </DropdownMenuItem>
                            )}
                            {comment.canDelete && (
                              <DropdownMenuItem 
                                onClick={() => confirmDelete(comment.id)}
                                className="text-destructive focus:text-destructive"
                              >
                                <Trash2 className="mr-2 h-4 w-4" />
                                {t('common.delete')}
                              </DropdownMenuItem>
                            )}
                          </DropdownMenuContent>
                        </DropdownMenu>
                      )}
                    </div>

                    {/* Content */}
                    {editingComment === comment.id ? (
                      <div className="space-y-2">
                        <Textarea
                          value={editContent}
                          onChange={(e) => setEditContent(e.target.value)}
                          rows={3}
                          disabled={submitting}
                        />
                        <div className="flex gap-2 justify-end">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              setEditingComment(null);
                              setEditContent('');
                            }}
                            disabled={submitting}
                          >
                            {t('common.cancel')}
                          </Button>
                          <Button
                            size="sm"
                            onClick={() => handleEdit(comment.id)}
                            disabled={!editContent.trim() || submitting}
                          >
                            {submitting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                            {t('common.save')}
                          </Button>
                        </div>
                      </div>
                    ) : (
                      <div className="text-sm whitespace-pre-wrap">
                        {renderCommentContent(comment.content)}
                      </div>
                    )}

                    {/* Reactions */}
                    <div className="flex items-center gap-2 flex-wrap pt-1">
                      {/* Existing Reactions */}
                      {getReactionSummary(comment).map(({ type, emoji, count, userReacted }) => (
                        <TooltipProvider key={type}>
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <Button
                                variant="outline"
                                size="sm"
                                className={cn(
                                  "h-7 px-2 gap-1 text-xs",
                                  userReacted && "bg-primary/10 border-primary"
                                )}
                                onClick={() => handleReaction(comment.id, type)}
                              >
                                <span>{emoji}</span>
                                <span>{count}</span>
                              </Button>
                            </TooltipTrigger>
                            <TooltipContent>
                              {userReacted ? t('comments.removeReaction') : t('comments.addReaction')}
                            </TooltipContent>
                          </Tooltip>
                        </TooltipProvider>
                      ))}

                      {/* Add Reaction Button */}
                      <div className="relative">
                        <TooltipProvider>
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <Button
                                variant="ghost"
                                size="sm"
                                className="h-7 px-2 text-muted-foreground hover:text-foreground"
                                onClick={() => setShowReactionPicker(
                                  showReactionPicker === comment.id ? null : comment.id
                                )}
                              >
                                😀+
                              </Button>
                            </TooltipTrigger>
                            <TooltipContent>{t('comments.addReaction')}</TooltipContent>
                          </Tooltip>
                        </TooltipProvider>

                        {/* Reaction Picker */}
                        {showReactionPicker === comment.id && (
                          <div className="absolute bottom-full left-0 mb-1 p-1 bg-popover border rounded-lg shadow-lg flex gap-1 z-50">
                            {AVAILABLE_REACTIONS.map((reaction) => (
                              <Button
                                key={reaction}
                                variant="ghost"
                                size="sm"
                                className="h-8 w-8 p-0"
                                onClick={() => handleReaction(comment.id, reaction)}
                              >
                                {REACTION_EMOJIS[reaction]}
                              </Button>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </CardContent>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t('comments.deleteTitle')}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('comments.deleteConfirmation')}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('common.cancel')}</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-destructive text-destructive-foreground">
              {t('common.delete')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  );
};

export default Comments;