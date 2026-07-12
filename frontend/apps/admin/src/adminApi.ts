import { apiDelete, apiGet, apiPost, apiPut } from "@nexusflow/api-client";
import type { CreateRoleRequest, GrantRoleRequest, Permission, Role, UserInfo, UserRole } from "./types";

type MaybeEnvelope<T> = T | { data?: T };

export async function login(email: string, password: string) {
  return apiPost<UserInfo>("/auth/login", { email, password });
}

export function logout() {
  return apiPost<void>("/auth/logout");
}

export function loadCurrentUser() {
  return apiGet<UserInfo>("/auth/me");
}

export async function listRoles(scope?: string) {
  const query = scope ? `?scope=${encodeURIComponent(scope)}` : "";
  return unwrapList<Role>(await apiGet<MaybeEnvelope<Role[]>>(`/admin/roles${query}`));
}

export async function getRole(id: string) {
  return unwrapOne<Role>(await apiGet<MaybeEnvelope<Role>>(`/admin/roles/${encodeURIComponent(id)}`));
}

export function createRole(request: CreateRoleRequest) {
  return apiPost<Role>("/admin/roles", request);
}

export function deleteRole(id: string) {
  return apiDelete<void>(`/admin/roles/${encodeURIComponent(id)}`);
}

export function setRolePermissions(roleId: string, permissionCodes: string[]) {
  return apiPut<Role>(`/admin/roles/${encodeURIComponent(roleId)}/permissions`, { permissionCodes });
}

export async function listPermissions(scope?: string) {
  const query = scope ? `?scope=${encodeURIComponent(scope)}` : "";
  return unwrapList<Permission>(await apiGet<MaybeEnvelope<Permission[]>>(`/admin/permissions${query}`));
}

export async function listUserRoles(userId: string, scopeType?: string) {
  const query = scopeType ? `?scopeType=${encodeURIComponent(scopeType)}` : "";
  return unwrapList<UserRole>(await apiGet<MaybeEnvelope<UserRole[]>>(
    `/admin/users/${encodeURIComponent(userId)}/roles${query}`
  ));
}

export function grantRole(request: GrantRoleRequest) {
  return apiPost<UserRole>("/admin/users/grant-role", request);
}

export function revokeRole(userId: string, roleId: string, scopeType: string, scopeId?: string) {
  const params = new URLSearchParams({ scopeType });
  if (scopeId) {
    params.set("scopeId", scopeId);
  }
  return apiDelete<void>(
    `/admin/users/${encodeURIComponent(userId)}/roles/${encodeURIComponent(roleId)}?${params.toString()}`
  );
}

function unwrapList<T>(payload: MaybeEnvelope<T[]>) {
  if (Array.isArray(payload)) {
    return payload;
  }
  return payload.data ?? [];
}

function unwrapOne<T>(payload: MaybeEnvelope<T>) {
  if (typeof payload === "object" && payload && "data" in payload) {
    return payload.data as T;
  }
  return payload as T;
}
