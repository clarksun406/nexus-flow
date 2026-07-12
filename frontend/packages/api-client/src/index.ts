import { appConfig } from "@nexusflow/config";

export type ApiEnvelope<T> = {
  success?: boolean;
  message?: string;
  data?: T;
};

export type ApiClientConfig = {
  apiBase?: string;
};

let runtimeConfig: ApiClientConfig = {};

export function configureApiClient(config: ApiClientConfig) {
  runtimeConfig = {
    ...runtimeConfig,
    ...config,
    apiBase: normalizeApiBase(config.apiBase ?? runtimeConfig.apiBase)
  };
}

export function getApiBase() {
  return runtimeConfig.apiBase ?? normalizeApiBase(appConfig.apiBase);
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(getApiBase() + path, {
    credentials: "include",
    headers: { Accept: "application/json" }
  });
  return parseEnvelope<T>(response);
}

export async function apiPost<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(getApiBase() + path, {
    method: "POST",
    credentials: "include",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json"
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  return parseEnvelope<T>(response);
}

export async function apiPut<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(getApiBase() + path, {
    method: "PUT",
    credentials: "include",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json"
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  return parseEnvelope<T>(response);
}

export async function apiDelete<T>(path: string): Promise<T> {
  const response = await fetch(getApiBase() + path, {
    method: "DELETE",
    credentials: "include",
    headers: { Accept: "application/json" }
  });
  return parseEnvelope<T>(response);
}

async function parseEnvelope<T>(response: Response): Promise<T> {
  const text = await response.text();
  const payload = (text ? JSON.parse(text) : {}) as ApiEnvelope<T> | T;
  if (!response.ok) {
    const message = typeof payload === "object" && payload && "message" in payload
      ? String(payload.message ?? "Request failed")
      : "Request failed";
    throw new Error(message);
  }
  if (typeof payload === "object" && payload && "data" in payload) {
    return payload.data as T;
  }
  return payload as T;
}

function normalizeApiBase(apiBase?: string) {
  return (apiBase ?? "").replace(/\/$/, "");
}
