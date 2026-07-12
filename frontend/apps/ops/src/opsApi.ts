import { apiGet, apiPost } from "@nexusflow/api-client";
import type {
  DeadLetterStatus,
  OpsDashboard,
  OrphanTransaction,
  UserInfo,
  WebhookDeadLetter
} from "./types";

export function login(email: string, password: string) {
  return apiPost<UserInfo>("/auth/login", { email, password });
}

export function logout() {
  return apiPost<void>("/auth/logout");
}

export function loadCurrentUser() {
  return apiGet<UserInfo>("/auth/me");
}

export function loadDashboard() {
  return apiGet<OpsDashboard>("/ops/dashboard");
}

export function listOrphans(status = "UNMATCHED") {
  return apiGet<OrphanTransaction[]>(`/crypto/orphan-transactions?status=${encodeURIComponent(status)}`);
}

export function resolveOrphan(chain: string, txHash: string, paymentId: string) {
  return apiPost<OrphanTransaction>(
    `/crypto/orphan-transactions/${encodeURIComponent(chain)}/${encodeURIComponent(txHash)}/resolve`,
    { paymentId }
  );
}

export function ignoreOrphan(chain: string, txHash: string) {
  return apiPost<OrphanTransaction>(
    `/crypto/orphan-transactions/${encodeURIComponent(chain)}/${encodeURIComponent(txHash)}/ignore`
  );
}

export function compensateOrphan(chain: string, txHash: string) {
  return apiPost<OrphanTransaction>(
    `/crypto/orphan-transactions/${encodeURIComponent(chain)}/${encodeURIComponent(txHash)}/compensate`
  );
}

export function listDeadLetters(status: DeadLetterStatus, limit = 25) {
  return apiGet<WebhookDeadLetter[]>(
    `/ops/webhook-dead-letters?status=${encodeURIComponent(status)}&limit=${limit}`
  );
}

export function replayDeadLetter(id: string) {
  return apiPost<WebhookDeadLetter>(`/ops/webhook-dead-letters/${encodeURIComponent(id)}/replay`);
}

export function ignoreDeadLetter(id: string) {
  return apiPost<WebhookDeadLetter>(`/ops/webhook-dead-letters/${encodeURIComponent(id)}/ignore`);
}
