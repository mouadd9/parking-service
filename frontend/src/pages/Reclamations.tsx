import React, { useEffect, useMemo, useRef, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { cn } from "@/lib/utils";
import { fetchClaimAttachments, fetchClaimMessages, fetchClaimStatusHistory, fetchClaims, sendClaimResponse, updateClaimStatus } from "@/services/Kafka";
import type { ClaimAttachment, ClaimAttachmentDto, ClaimMessage, ClaimStatusHistory, ClaimSummary } from "@/types/Kafka";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
// import { Header } from "./Header";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
// Removed i18n imports - using static text instead

// Translation function replacement
const t = (key: string) => {
  const translations: Record<string, string> = {
    "reclamation.title": "Reclamations",
    "reclamation.searchPlaceholder": "Search...",
    "reclamation.loading": "Loading...",
    "reclamation.empty": "No reclamations",
    "reclamation.selectClaim": "Select a claim",
    "reclamation.tab.conversation": "Conversation",
    "reclamation.tab.status": "Status",
    "reclamation.noSelected": "No claim selected",
    "reclamation.statusLabel": "Status:",
    "reclamation.details": "Details",
    "reclamation.attachments": "Attachments",
    "reclamation.attachmentsLoading": "Loading attachments...",
    "reclamation.attachmentsEmpty": "No attachments",
    "reclamation.conversation": "Conversation",
    "reclamation.messagesLoading": "Loading messages...",
    "reclamation.messagesEmpty": "No messages yet",
    "reclamation.reply": "Reply",
    "reclamation.resolvedNotice": "This claim is resolved",
    "reclamation.replyPlaceholder": "Type your reply...",
    "reclamation.cancel": "Cancel",
    "reclamation.sending": "Sending...",
    "reclamation.send": "Send",
    "reclamation.statusHistory": "Status History",
    "reclamation.historyLoading": "Loading history...",
    "reclamation.historyEmpty": "No history",
    "reclamation.assignedTo": "Assigned to:",
    "reclamation.updateStatus": "Update Status",
    "reclamation.newStatus": "New Status",
    "reclamation.chooseStatus": "Choose status",
    "reclamation.reason": "Reason",
    "reclamation.reasonPlaceholder": "Reason for status change...",
    "reclamation.resolutionOptional": "Resolution (Optional)",
    "reclamation.summary": "Summary",
    "reclamation.actionsPlaceholder": "Actions taken (one per line)...",
    "reclamation.closingMessage": "Closing message",
    "reclamation.updating": "Updating...",
    "reclamation.update": "Update",
  };
  return translations[key] || key;
};

// If it works, DONT TOUCH IT, AND I MEAN IT LITERALLY


const formatTime = (iso?: string) => {
  if (!iso) return "";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString();
};

const safeParseJsonObject = <T,>(raw?: string | null): T | null => {
  if (!raw) return null;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
};

const statusBadgeVariant = (status?: string) => {
  const s = (status || "").toLowerCase();
  if (s.includes("resolved") || s.includes("closed")) return "default";
  if (s.includes("in_progress") || s.includes("processing")) return "secondary";
  if (s.includes("rejected")) return "destructive";
  return "outline";
};

const safeParseAttachments = (raw?: string | null): ClaimAttachmentDto[] => {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) return parsed as ClaimAttachmentDto[];
    return [];
  } catch {
    return [];
  }
};

const claimSortKey = (c: ClaimSummary) => {
  const iso = c.updatedAt || c.createdAt;
  if (!iso) return 0;
  const t = new Date(iso).getTime();
  return Number.isNaN(t) ? 0 : t;
};

type OperatorProfile = {
  id: number;
  username: string;
  email: string;
  matricule?: string;
};

const Reclamation: React.FC = () => {
  // Removed useTranslation - using static translations defined above

  const [claims, setClaims] = useState<ClaimSummary[]>([]);
  const [selectedClaimId, setSelectedClaimId] = useState<number | null>(null);
  const [messages, setMessages] = useState<ClaimMessage[]>([]);
  const [claimAttachments, setClaimAttachments] = useState<ClaimAttachment[]>([]);
  const [statusHistory, setStatusHistory] = useState<ClaimStatusHistory[]>([]);
  const [search, setSearch] = useState("");
  const [reply, setReply] = useState("");
  const [loadingClaims, setLoadingClaims] = useState(false);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [loadingAttachments, setLoadingAttachments] = useState(false);
  const [loadingStatusHistory, setLoadingStatusHistory] = useState(false);
  const [sending, setSending] = useState(false);
  const [updatingStatus, setUpdatingStatus] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [operator, setOperator] = useState<OperatorProfile | null>(null);

  const [newStatus, setNewStatus] = useState("in_progress");
  const [statusReason, setStatusReason] = useState("");
  const [resolutionSummary, setResolutionSummary] = useState("");
  const [resolutionActions, setResolutionActions] = useState("");
  const [resolutionClosingMessage, setResolutionClosingMessage] = useState("");

  const [activeTab, setActiveTab] = useState("conversation");

  const messagesEndRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const run = async () => {
      const token = localStorage.getItem("token");
      if (!token) return;
      try {
        const response = await fetch("http://localhost:8080/api/profile", {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
        if (!response.ok) return;
        const data = (await response.json()) as OperatorProfile;
        if (data?.id && data?.username) setOperator(data);
      } catch {
        // ignore
      }
    };

    run();
  }, []); // Removed i18n.language dependency

  useEffect(() => {
    let cancelled = false;

    const refreshClaims = async (showLoading: boolean) => {
      if (showLoading) setLoadingClaims(true);
      if (showLoading) setError(null);
      try {
        const data = await fetchClaims();
        const sorted = [...data].sort((a, b) => claimSortKey(b) - claimSortKey(a));
        if (cancelled) return;
        setClaims(sorted);
        if (sorted.length > 0) {
          setSelectedClaimId((prev) => (prev == null ? sorted[0].id : prev));
        }
      } catch (e) {
        if (showLoading) setError(e instanceof Error ? e.message : String(t("reclamation.loading")));
      } finally {
        if (showLoading) setLoadingClaims(false);
      }
    };

    refreshClaims(true);
    const id = window.setInterval(() => refreshClaims(false), 5000);
    return () => {
      cancelled = true;
      window.clearInterval(id);
    };
  }, []);

  useEffect(() => {
    if (selectedClaimId == null) {
      setMessages([]);
      setClaimAttachments([]);
      setStatusHistory([]);
      return;
    }

    let cancelled = false;

    const refreshMessages = async (showLoading: boolean) => {
      if (showLoading) setLoadingMessages(true);
      if (showLoading) setError(null);
      try {
        const data = await fetchClaimMessages(selectedClaimId);
        if (cancelled) return;
        setMessages(data);
      } catch (e) {
        if (showLoading) setError(e instanceof Error ? e.message : String(t("reclamation.messagesLoading")));
      } finally {
        if (showLoading) setLoadingMessages(false);
      }
    };

    refreshMessages(true);
    const id = window.setInterval(() => refreshMessages(false), 3000);
    return () => {
      cancelled = true;
      window.clearInterval(id);
    };
  }, [selectedClaimId]); // Removed i18n.language dependency

  useEffect(() => {
    if (selectedClaimId == null) return;

    let cancelled = false;

    const refreshAttachments = async () => {
      setLoadingAttachments(true);
      try {
        const data = await fetchClaimAttachments(selectedClaimId);
        if (cancelled) return;
        setClaimAttachments(data);
      } catch {
        if (cancelled) return;
        setClaimAttachments([]);
      } finally {
        if (!cancelled) setLoadingAttachments(false);
      }
    };

    refreshAttachments();
    return () => {
      cancelled = true;
    };
  }, [selectedClaimId]);

  useEffect(() => {
    if (selectedClaimId == null) return;

    let cancelled = false;

    const refreshStatus = async (showLoading: boolean) => {
      if (showLoading) setLoadingStatusHistory(true);
      try {
        const data = await fetchClaimStatusHistory(selectedClaimId);
        if (cancelled) return;
        setStatusHistory(data);
      } catch {
        if (cancelled) return;
        setStatusHistory([]);
      } finally {
        if (showLoading && !cancelled) setLoadingStatusHistory(false);
      }
    };

    refreshStatus(true);
    const id = window.setInterval(() => refreshStatus(false), 5000);
    return () => {
      cancelled = true;
      window.clearInterval(id);
    };
  }, [selectedClaimId]);

  useEffect(() => {
    if (!messagesEndRef.current) return;
    messagesEndRef.current.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages.length, selectedClaimId]);

  const selectedClaim = useMemo(
    () => claims.find((c) => c.id === selectedClaimId) ?? null,
    [claims, selectedClaimId]
  );

  const isResolved = (selectedClaim?.currentStatus || "").toLowerCase() === "resolved";

  const operatorIdentity = useMemo(() => {
    if (operator) {
      return { operatorId: String(operator.id), operatorName: operator.username, operatorMatricule: operator.matricule };
    }
    const operatorId = localStorage.getItem("operatorId") || "operator-1";
    const operatorName = localStorage.getItem("operatorName") || "Operateur";
    const operatorMatricule = localStorage.getItem("operatorMatricule") || undefined;
    return { operatorId, operatorName, operatorMatricule };
  }, [operator]);

  const operatorDisplayName = useMemo(() => {
    return operatorIdentity.operatorMatricule
      ? `${operatorIdentity.operatorName} - ${operatorIdentity.operatorMatricule}`
      : operatorIdentity.operatorName;
  }, [operatorIdentity]);

  const filteredClaims = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return claims;
    return claims.filter((c) => {
      return (
        (c.claimNumber || "").toLowerCase().includes(q) ||
        (c.userName || "").toLowerCase().includes(q) ||
        (c.userEmail || "").toLowerCase().includes(q) ||
        (c.title || "").toLowerCase().includes(q) ||
        (c.description || "").toLowerCase().includes(q)
      );
    });
  }, [claims, search]);

  const handleSend = async () => {
    if (!selectedClaimId || !selectedClaim) return;
    if (isResolved) return;
    const message = reply.trim();
    if (!message) return;

    setSending(true);
    setError(null);
    try {
      const { operatorId, operatorName } = operatorIdentity;
      const created = await sendClaimResponse(selectedClaimId, {
        operatorId,
        operatorName,
        message,
        serviceReference: selectedClaim.claimNumber,
        attachments: [],
      });

      setMessages((prev) => [...prev, created]);
      setReply("");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Erreur lors de l'envoi");
    } finally {
      setSending(false);
    }
  };

  const handleUpdateStatus = async () => {
    if (!selectedClaimId || !selectedClaim) return;
    const next = newStatus.trim();
    if (!next) return;

    const { operatorId, operatorName } = operatorIdentity;
    const actionsTaken = resolutionActions
      .split("\n")
      .map((s) => s.trim())
      .filter(Boolean);

    const resolution =
      resolutionSummary.trim() || actionsTaken.length > 0 || resolutionClosingMessage.trim()
        ? {
            summary: resolutionSummary.trim() || undefined,
            actionsTaken: actionsTaken.length > 0 ? actionsTaken : undefined,
            closingMessage: resolutionClosingMessage.trim() || undefined,
          }
        : undefined;

    setUpdatingStatus(true);
    setError(null);
    try {
      const created = await updateClaimStatus(selectedClaimId, {
        newStatus: next,
        reason: statusReason.trim() || undefined,
        operatorId,
        operatorName,
        serviceReference: selectedClaim.claimNumber,
        resolution,
      });

      setStatusHistory((prev) => [...prev, created]);
      setClaims((prev) =>
        prev.map((c) => (c.id === selectedClaimId ? { ...c, currentStatus: next } : c))
      );

      setStatusReason("");
      setResolutionSummary("");
      setResolutionActions("");
      setResolutionClosingMessage("");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Erreur lors de la mise à jour du statut");
    } finally {
      setUpdatingStatus(false);
    }
  };

  return (
    <div className="container mx-auto p-4">
      
      <Card className="grid grid-cols-1 md:grid-cols-4 gap-4 mt-6 border-[#EA580C]/25">
        <div className="md:col-span-1">
          <Card className="border-[#EA580C]/25">
            <CardHeader>
              <CardTitle className="text-[#EA580C]">{t("reclamation.title")}</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center gap-2 mb-3">
                <Input value={search} onChange={(e) => setSearch(e.target.value)} placeholder={t("reclamation.searchPlaceholder")} />
              </div>
              <div className="space-y-2 max-h-[70vh] overflow-auto pr-2 thin-scrollbar">
                {loadingClaims ? (
                  <div className="text-sm text-muted-foreground">{t("reclamation.loading")}</div>
                ) : filteredClaims.length === 0 ? (
                  <div className="text-sm text-muted-foreground">{t("reclamation.empty")}</div>
                ) : (
                  filteredClaims.map((it) => (
                    <div
                      key={it.id}
                      onClick={() => setSelectedClaimId(it.id)}
                      className={cn(
                        "flex items-start gap-3 p-3 rounded-lg cursor-pointer hover:bg-muted/70 border",
                        selectedClaimId === it.id ? "bg-[#EA580C]/5 border-[#EA580C]/25" : "border-transparent"
                      )}
                    >
                      <Avatar>
                        <AvatarImage src={undefined} />
                        <AvatarFallback>
                          {(it.userName || it.userEmail || "U")
                            .split(" ")
                            .map((n) => n[0])
                            .slice(0, 2)
                            .join("")}
                        </AvatarFallback>
                      </Avatar>
                      <div className="flex-1">
                        <div className="flex justify-between items-start gap-2">
                          <div>
                            <div className="font-semibold text-foreground leading-tight">{it.userName || it.userEmail || "Utilisateur"}</div>
                            <div className="text-sm text-muted-foreground mt-0.5">{it.title || it.claimNumber}</div>
                          </div>
                          <div className="flex flex-col items-end gap-1">
                            <div className="text-xs text-muted-foreground">
                              {formatTime(it.updatedAt || it.createdAt)}
                            </div>
                            <Badge variant={statusBadgeVariant(it.currentStatus) as any}>
                              {it.currentStatus || "submitted"}
                            </Badge>
                          </div>
                        </div>
                        <div className="text-sm text-muted-foreground mt-2 leading-snug">
                          {(it.description || "").slice(0, 80)}
                          {(it.description || "").length > 80 ? "..." : ""}
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="md:col-span-3">
          <Card className="min-h-[72vh] border-[#EA580C]/25">
            <Tabs value={activeTab} onValueChange={setActiveTab}>
              <CardHeader>
                <div className="flex flex-col gap-3 md:flex-row md:justify-between md:items-center w-full">
                  <div className="min-w-0">
                    <CardTitle className="text-[#EA580C] truncate">
                      {selectedClaim ? selectedClaim.title || selectedClaim.claimNumber : t("reclamation.selectClaim")}
                    </CardTitle>
                    <div className="text-sm text-muted-foreground">
                      {selectedClaim
                        ? `${selectedClaim.userName || selectedClaim.userEmail || "Utilisateur"} • ${formatTime(
                            selectedClaim.updatedAt || selectedClaim.createdAt
                          )}`
                        : ""}
                    </div>
                  </div>

                  <div className="flex items-center justify-between w-full md:w-auto gap-2">
                    <TabsList className="justify-start md:justify-end border border-[#EA580C]/20 bg-[#EA580C]/5">
                      <TabsTrigger value="conversation">{t("reclamation.tab.conversation")}</TabsTrigger>
                      <TabsTrigger value="status">{t("reclamation.tab.status")}</TabsTrigger>
                    </TabsList>
                  </div>
                </div>
              </CardHeader>

              <CardContent>
                {error ? (
                  <div className="text-sm text-destructive">{error}</div>
                ) : !selectedClaim ? (
                  <div className="text-muted-foreground">{t("reclamation.noSelected")}</div>
                ) : (
                  <div className="space-y-4">
                    <div className="flex items-center gap-2">
                      <div className="text-sm font-medium">{t("reclamation.statusLabel")}</div>
                      <Badge variant={statusBadgeVariant(selectedClaim.currentStatus) as any}>
                        {selectedClaim.currentStatus || "submitted"}
                      </Badge>
                      <div className="ml-auto text-xs text-muted-foreground">{selectedClaim.claimNumber}</div>
                    </div>

                    <TabsContent value="conversation">
                      <div className="rounded-lg border border-[#EA580C]/25 bg-[#EA580C]/5 p-3">
                        <div className="text-sm font-semibold text-foreground">{t("reclamation.details")}</div>
                        <div className="text-sm text-muted-foreground mt-1 whitespace-pre-wrap leading-relaxed">
                          {selectedClaim.description || ""}
                        </div>
                        <div className="mt-3">
                          <div className="text-sm font-semibold text-foreground">{t("reclamation.attachments")}</div>
                          {loadingAttachments ? (
                            <div className="text-sm text-muted-foreground mt-1">{t("reclamation.attachmentsLoading")}</div>
                          ) : claimAttachments.length === 0 ? (
                            <div className="text-sm text-muted-foreground mt-1">{t("reclamation.attachmentsEmpty")}</div>
                          ) : (
                            <div className="mt-2 flex flex-wrap gap-2">
                              {claimAttachments.map((a) => (
                                <a
                                  key={a.id}
                                  href={a.url}
                                  target="_blank"
                                  rel="noreferrer"
                                  className="text-xs px-2.5 py-1 rounded-full border border-[#EA580C]/25 bg-background hover:bg-[#EA580C]/5"
                                >
                                  {a.fileName || a.url}
                                </a>
                              ))}
                            </div>
                          )}
                        </div>
                      </div>

                      <div className="space-y-2">
                        <div className="text-sm font-semibold text-foreground">{t("reclamation.conversation")}</div>
                        <div className="space-y-2 max-h-[46vh] overflow-auto pr-2 thin-scrollbar">
                          {loadingMessages ? (
                            <div className="text-sm text-muted-foreground">{t("reclamation.messagesLoading")}</div>
                          ) : messages.length === 0 ? (
                            <div className="text-sm text-muted-foreground">{t("reclamation.messagesEmpty")}</div>
                          ) : (
                            messages.map((m) => {
                              const isOperator = m.senderType === "OPERATOR";
                              const parsedAttachments = safeParseAttachments(m.attachments);
                              return (
                                <div
                                  key={m.id}
                                  className={cn(
                                    "flex",
                                    isOperator ? "justify-end" : "justify-start"
                                  )}
                                >
                                  <div
                                    className={cn(
                                      "p-3 rounded-2xl border max-w-[85%] shadow-sm",
                                      isOperator ? "bg-[#EA580C]/5 border-[#EA580C]/25" : "bg-background border-border"
                                    )}
                                  >
                                    <div className="text-sm whitespace-pre-wrap">{m.message}</div>

                                    {parsedAttachments.length > 0 ? (
                                      <div className="mt-2 space-y-1">
                                        {parsedAttachments.map((a, idx) => (
                                          <a
                                            key={`${m.id}-att-${idx}`}
                                            href={a.url}
                                            target="_blank"
                                            rel="noreferrer"
                                            className="text-xs underline"
                                          >
                                            {a.fileName || a.url}
                                          </a>
                                        ))}
                                      </div>
                                    ) : null}

                                    <div
                                      className={cn(
                                        "mt-2 text-[11px] text-muted-foreground",
                                        isOperator ? "text-right" : "text-left"
                                      )}
                                    >
                                      <div>
                                        {isOperator
                                          ? (m.senderName || operatorDisplayName || "Opérateur")
                                          : (m.senderName || "Utilisateur")}
                                        {!isOperator && m.senderId ? ` (${m.senderId})` : ""}
                                      </div>
                                      <div>{formatTime(m.createdAt || m.messageTimestamp)}</div>
                                    </div>
                                  </div>
                                </div>
                              );
                            })
                          )}
                          <div ref={messagesEndRef} />
                        </div>
                      </div>

                      <div>
                        <label className="block text-sm font-semibold mb-2">{t("reclamation.reply")}</label>
                        {isResolved ? (
                          <div className="text-sm text-muted-foreground bg-muted/40 border rounded-md p-3">
                            {t("reclamation.resolvedNotice")}
                          </div>
                        ) : null}
                        <Textarea
                          value={reply}
                          onChange={(e) => setReply(e.target.value)}
                          placeholder={t("reclamation.replyPlaceholder")}
                          disabled={isResolved}
                          className={cn(isResolved ? "opacity-60" : "")}
                        />
                        <div className="flex justify-end mt-3 gap-2">
                          <Button variant="outline" onClick={() => setReply("")}>
                            {t("reclamation.cancel")}
                          </Button>
                          <Button
                            onClick={handleSend}
                            disabled={isResolved || sending || reply.trim().length === 0}
                          >
                            {sending ? t("reclamation.sending") : t("reclamation.send")}
                          </Button>
                        </div>
                      </div>
                    </TabsContent>

                    <TabsContent value="status">
                      <div className="space-y-4">
                        <div className="space-y-2 rounded-lg border border-[#EA580C]/25 p-3">
                          <div className="text-sm font-semibold text-foreground">{t("reclamation.statusHistory")}</div>
                          {loadingStatusHistory ? (
                            <div className="text-sm text-muted-foreground">{t("reclamation.historyLoading")}</div>
                          ) : statusHistory.length === 0 ? (
                            <div className="text-sm text-muted-foreground">{t("reclamation.historyEmpty")}</div>
                          ) : (
                            <div className="space-y-2 max-h-[34vh] overflow-auto pr-2 thin-scrollbar">
                              {statusHistory.map((h) => {
                                const assigned = safeParseJsonObject<{ operatorId?: string; operatorName?: string }>(
                                  h.assignedTo
                                );
                                const resolution = safeParseJsonObject<{
                                  summary?: string;
                                  actionsTaken?: string[];
                                  closingMessage?: string;
                                }>(h.resolution);

                                return (
                                  <div key={h.id} className="rounded-lg border p-3 bg-background">
                                    <div className="flex items-start justify-between gap-2">
                                      <div className="text-sm font-semibold text-foreground">
                                        <Badge variant={statusBadgeVariant(h.newStatus) as any}>{h.newStatus}</Badge>
                                        {h.previousStatus ? (
                                          <span className="ml-2 text-xs text-muted-foreground">
                                            depuis {h.previousStatus}
                                          </span>
                                        ) : null}
                                      </div>
                                      <div className="text-xs text-muted-foreground">{formatTime(h.createdAt || h.messageTimestamp)}</div>
                                    </div>

                                    {h.reason ? (
                                      <div className="mt-2 text-sm text-foreground/80 whitespace-pre-wrap">{h.reason}</div>
                                    ) : null}

                                    {assigned?.operatorId || assigned?.operatorName ? (
                                      <div className="mt-2 text-xs text-muted-foreground">
                                        {t("reclamation.assignedTo")} {assigned?.operatorName || ""}
                                        {assigned?.operatorId ? ` (${assigned.operatorId})` : ""}
                                      </div>
                                    ) : null}

                                    {resolution?.summary || resolution?.closingMessage || (resolution?.actionsTaken || []).length > 0 ? (
                                      <div className="mt-3">
                                        <Separator className="my-2" />
                                        {resolution?.summary ? (
                                          <div className="text-xs text-muted-foreground">Résumé: {resolution.summary}</div>
                                        ) : null}
                                        {(resolution?.actionsTaken || []).length > 0 ? (
                                          <div className="text-xs text-muted-foreground">
                                            Actions: {(resolution?.actionsTaken || []).join(", ")}
                                          </div>
                                        ) : null}
                                        {resolution?.closingMessage ? (
                                          <div className="text-xs text-muted-foreground">Message: {resolution.closingMessage}</div>
                                        ) : null}
                                      </div>
                                    ) : null}
                                  </div>
                                );
                              })}
                            </div>
                          )}
                        </div>

                        <div className="rounded-lg border border-[#EA580C]/25 p-3 space-y-3 bg-[#EA580C]/5">
                          <div className="text-sm font-semibold text-foreground">{t("reclamation.updateStatus")}</div>

                          <div className="space-y-2">
                            <label className="block text-sm font-medium">{t("reclamation.newStatus")}</label>
                            <Select value={newStatus} onValueChange={setNewStatus}>
                              <SelectTrigger>
                                <SelectValue placeholder={t("reclamation.chooseStatus")} />
                              </SelectTrigger>
                              <SelectContent>
                                <SelectItem value="submitted">submitted</SelectItem>
                                <SelectItem value="in_progress">in_progress</SelectItem>
                                <SelectItem value="resolved">resolved</SelectItem>
                              </SelectContent>
                            </Select>
                          </div>

                          <div className="space-y-2">
                            <label className="block text-sm font-medium">{t("reclamation.reason")}</label>
                            <Textarea value={statusReason} onChange={(e) => setStatusReason(e.target.value)} placeholder={t("reclamation.reasonPlaceholder")} />
                          </div>

                          <div className="space-y-2">
                            <label className="block text-sm font-medium">{t("reclamation.resolutionOptional")}</label>
                            <Input value={resolutionSummary} onChange={(e) => setResolutionSummary(e.target.value)} placeholder={t("reclamation.summary")} />
                            <Textarea
                              value={resolutionActions}
                              onChange={(e) => setResolutionActions(e.target.value)}
                              placeholder={t("reclamation.actionsPlaceholder")}
                            />
                            <Input
                              value={resolutionClosingMessage}
                              onChange={(e) => setResolutionClosingMessage(e.target.value)}
                              placeholder={t("reclamation.closingMessage")}
                            />
                          </div>

                          <div className="flex justify-end">
                            <Button
                              onClick={handleUpdateStatus}
                              disabled={updatingStatus || newStatus.trim().length === 0}
                            >
                              {updatingStatus ? t("reclamation.updating") : t("reclamation.update")}
                            </Button>
                          </div>
                        </div>
                      </div>
                    </TabsContent>
                  </div>
                )}
              </CardContent>
            </Tabs>
          </Card>
        </div>
      </Card>
    </div>
  );
};

export default Reclamation;


