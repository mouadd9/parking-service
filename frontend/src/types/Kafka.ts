export type ClaimAttachmentDto = {
  url: string;
  fileName?: string;
  fileType?: string;
};

export type ClaimAttachment = {
  id: number;
  url: string;
  fileName?: string;
  fileType?: string;
  source?: string;
  createdAt?: string;
};

export type StatusResolution = {
  summary?: string;
  actionsTaken?: string[];
  closingMessage?: string;
};

export type ClaimStatusHistory = {
  id: number;
  messageId: string;
  messageTimestamp: string;
  previousStatus?: string | null;
  newStatus: string;
  reason?: string | null;
  assignedTo?: string | null;
  resolution?: string | null;
  serviceReference?: string | null;
  createdAt?: string;
};

export type ClaimSummary = {
  id: number;
  claimUuid: string;
  claimNumber: string;
  userName?: string;
  userEmail?: string;
  title?: string;
  description?: string;
  currentStatus?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type ClaimMessage = {
  id: number;
  messageId: string;
  messageType: string;
  messageTimestamp: string;
  senderType: "USER" | "OPERATOR" | string;
  senderId?: string;
  senderName?: string;
  message: string;
  attachments?: string | null;
  serviceReference?: string | null;
  createdAt?: string;
};

export type SendClaimResponseRequest = {
  operatorId: string;
  operatorName: string;
  message: string;
  serviceReference?: string;
  attachments?: ClaimAttachmentDto[];
};

export type UpdateClaimStatusRequest = {
  newStatus: string;
  reason?: string;
  operatorId: string;
  operatorName: string;
  serviceReference?: string;
  resolution?: StatusResolution;
};