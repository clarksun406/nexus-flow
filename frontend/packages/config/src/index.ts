export const appConfig = {
  apiBase: import.meta.env.VITE_API_BASE ?? "/api"
};

export type AppShell = "checkout" | "merchant" | "ops" | "admin";
