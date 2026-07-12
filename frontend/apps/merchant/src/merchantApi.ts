import { apiGet, apiPost } from "@nexusflow/api-client";
import type {
  CreateOrderRequest,
  OrderResponse,
  RefundRequest,
  RefundResponse,
  UserInfo
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

export function switchActiveMerchant(merchantId: string) {
  return apiPost<UserInfo>("/auth/active-merchant", { merchantId });
}

export function createOrder(request: CreateOrderRequest) {
  return apiPost<OrderResponse>("/pay/order", request);
}

export function getOrder(paymentId: string) {
  return apiGet<OrderResponse>(`/pay/order/${encodeURIComponent(paymentId)}`);
}

export function refundOrder(request: RefundRequest) {
  return apiPost<RefundResponse>("/refund/order", request);
}
