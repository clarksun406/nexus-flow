import { apiGet, apiPost } from "@nexusflow/api-client";
import type { CashierStatus, CashierSubmitRequest, CashierSubmitResponse } from "./types";

export function getCashierStatus(paymentId: string) {
  return apiGet<CashierStatus>(`/cashier/order/status?paymentId=${encodeURIComponent(paymentId)}`);
}

export function submitCashierPayment(request: CashierSubmitRequest) {
  return apiPost<CashierSubmitResponse>("/cashier/pay/submit", request);
}
