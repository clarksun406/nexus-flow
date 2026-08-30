import { apiGet, apiPost } from "@nexusflow/api-client";
import type {
  DeadLetterStatus,
  FiatRampPage,
  OpsDashboard,
  OrderPage,
  OrphanTransaction,
  PaymentPage,
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

function pageQuery(page: number, size: number, status: string, merchantId?: string) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) params.set("status", status);
  if (merchantId) params.set("merchantId", merchantId);
  return params.toString();
}

export function listOrders(status: string, merchantId: string, page: number, size: number) {
  return apiGet<OrderPage>(`/ops/orders?${pageQuery(page, size, status, merchantId)}`);
}

export function listPayments(status: string, page: number, size: number) {
  return apiGet<PaymentPage>(`/ops/payments?${pageQuery(page, size, status)}`);
}

export function listFiatRampOrders(status: string, merchantId: string, page: number, size: number) {
  return apiGet<FiatRampPage>(`/ops/fiat-ramp-orders?${pageQuery(page, size, status, merchantId)}`);
}
