import api from './api';

export type AgentActionStatus =
  | 'PROPOSED'
  | 'CONFIRMED'
  | 'EXECUTED'
  | 'FAILED'
  | 'CANCELLED'
  | 'EXPIRED';

/** A change the ShipFlow Agent proposed in a comment, awaiting the requester's confirmation. */
export interface AgentActionProposal {
  id: number;
  commentId: number;
  /** The MCP tool that will run. */
  tool: string;
  /** Exactly what will run, built by the server from the real tool and arguments. */
  label: string;
  /** The model's own description; informational only. */
  summary?: string;
  status: AgentActionStatus;
  resultMessage?: string;
  /** True only for the person who asked, while it is still PROPOSED. */
  canConfirm: boolean;
  createdAt: string;
  resolvedAt?: string;
}

export const agentActionService = {
  /** Applies the change as the signed-in requester. Resolves to the proposal as it now stands. */
  confirm: (id: number) => api.post<AgentActionProposal>(`/agent/actions/${id}/confirm`),

  /** Declines the change. */
  cancel: (id: number) => api.post<AgentActionProposal>(`/agent/actions/${id}/cancel`),
};

export default agentActionService;
