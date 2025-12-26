import type {
  ClaimAttachment,
  ClaimMessage,
  ClaimStatusHistory,
  ClaimSummary,
  SendClaimResponseRequest,
  UpdateClaimStatusRequest,
} from "@/types/Kafka";

const BASE_URL = "http://localhost:8080";

const assertOk = async (response: Response) => {
  if (!response.ok) {
    const text = await response.text().catch(() => "");
    throw new Error(
      `HTTP ${response.status} ${response.statusText}${
        text ? ` - ${text}` : ""
      }`
    );
  }
};

/**
 * ✅ fetch wrapper that automatically adds Authorization: Bearer <token>
 * (same behavior as axios interceptor)
 */
const authFetch = async (input: RequestInfo, init: RequestInit = {}) => {
  const token = localStorage.getItem("token");

  const headers: HeadersInit = {
    ...(init.headers || {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };

  return fetch(input, {
    ...init,
    headers,
  });
};

export const fetchClaims = async (): Promise<ClaimSummary[]> => {
  const response = await authFetch(`${BASE_URL}/api/claims`);
  await assertOk(response);
  const data: ClaimSummary[] = await response.json();
  return data || [];
};

export const fetchClaimMessages = async (
  claimId: number
): Promise<ClaimMessage[]> => {
  const response = await authFetch(`${BASE_URL}/api/claims/${claimId}/messages`);
  await assertOk(response);
  const data: ClaimMessage[] = await response.json();
  return data || [];
};

export const fetchClaimAttachments = async (
  claimId: number
): Promise<ClaimAttachment[]> => {
  const response = await authFetch(
    `${BASE_URL}/api/claims/${claimId}/attachments`
  );
  await assertOk(response);
  const data: ClaimAttachment[] = await response.json();
  return data || [];
};

export const fetchClaimStatusHistory = async (
  claimId: number
): Promise<ClaimStatusHistory[]> => {
  const response = await authFetch(
    `${BASE_URL}/api/claims/${claimId}/status-history`
  );
  await assertOk(response);
  const data: ClaimStatusHistory[] = await response.json();
  return data || [];
};

export const updateClaimStatus = async (
  claimId: number,
  payload: UpdateClaimStatusRequest
): Promise<ClaimStatusHistory> => {
  const response = await authFetch(`${BASE_URL}/api/claims/${claimId}/status`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  await assertOk(response);
  const data: ClaimStatusHistory = await response.json();
  return data;
};

export const sendClaimResponse = async (
  claimId: number,
  payload: SendClaimResponseRequest
): Promise<ClaimMessage> => {
  const response = await authFetch(`${BASE_URL}/api/claims/${claimId}/respond`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  await assertOk(response);
  const data: ClaimMessage = await response.json();
  return data;
};