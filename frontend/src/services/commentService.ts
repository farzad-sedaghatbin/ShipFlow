import api from './api';

// Types
export type CommentEntityType = 'TASK' | 'BUG_REPORT';

export type CommentReaction = 
  | 'THUMBS_UP' 
  | 'THUMBS_DOWN' 
  | 'HEART' 
  | 'LAUGH' 
  | 'SURPRISED' 
  | 'SAD' 
  | 'ROCKET' 
  | 'EYES';

export interface ReactionSummary {
  reactionCounts: Record<CommentReaction, number>;
  userReactions: Record<CommentReaction, boolean>;
  totalReactions: number;
}

export interface Comment {
  id: number;
  content: string;
  entityType: CommentEntityType;
  entityId: number;
  authorId: number;
  authorName: string;
  authorUsername: string;
  isEdited: boolean;
  createdAt: string;
  updatedAt?: string;
  reactions: ReactionSummary;
  canEdit: boolean;
  canDelete: boolean;
}

export interface CreateCommentRequest {
  content: string;
  entityType: CommentEntityType;
  entityId: number;
}

export interface UpdateCommentRequest {
  content: string;
}

export interface ReactionRequest {
  reactionType: CommentReaction;
}

export interface ReactionInfo {
  type: CommentReaction;
  emoji: string;
}

/**
 * User info for @mention suggestions
 */
export interface MentionUser {
  id: number;
  username: string;
  displayName: string;
}

// Emoji mapping for reactions
export const REACTION_EMOJIS: Record<CommentReaction, string> = {
  THUMBS_UP: '👍',
  THUMBS_DOWN: '👎',
  HEART: '❤️',
  LAUGH: '😄',
  SURPRISED: '😮',
  SAD: '😢',
  ROCKET: '🚀',
  EYES: '👀',
};

// Service methods
export const commentService = {
  /**
   * Get all comments for an entity
   */
  getComments: (entityType: CommentEntityType, entityId: number) =>
    api.get<Comment[]>(`/comments/${entityType.toLowerCase()}/${entityId}`),

  /**
   * Get comments with pagination
   */
  getCommentsPaginated: (
    entityType: CommentEntityType,
    entityId: number,
    page = 0,
    size = 20
  ) =>
    api.get<{ content: Comment[]; totalElements: number; totalPages: number }>(
      `/comments/${entityType.toLowerCase()}/${entityId}/paginated`,
      { params: { page, size } }
    ),

  /**
   * Get a single comment
   */
  getComment: (commentId: number) =>
    api.get<Comment>(`/comments/single/${commentId}`),

  /**
   * Create a new comment
   */
  createComment: (request: CreateCommentRequest) =>
    api.post<Comment>('/comments', request),

  /**
   * Update an existing comment
   */
  updateComment: (commentId: number, request: UpdateCommentRequest) =>
    api.put<Comment>(`/comments/${commentId}`, request),

  /**
   * Delete a comment
   */
  deleteComment: (commentId: number) =>
    api.delete<{ message: string }>(`/comments/${commentId}`),

  /**
   * Toggle a reaction on a comment
   */
  toggleReaction: (commentId: number, request: ReactionRequest) =>
    api.post<Comment>(`/comments/${commentId}/reactions`, request),

  /**
   * Get comment count for an entity
   */
  getCommentCount: (entityType: CommentEntityType, entityId: number) =>
    api.get<{ count: number }>(
      `/comments/${entityType.toLowerCase()}/${entityId}/count`
    ),

  /**
   * Get available reactions
   */
  getAvailableReactions: () =>
    api.get<ReactionInfo[]>('/comments/reactions/available'),

  /**
   * Search users for @mention autocomplete
   */
  searchUsersForMention: (query: string) =>
    api.get<MentionUser[]>('/comments/users/search', { params: { query } }),
};

export default commentService;
