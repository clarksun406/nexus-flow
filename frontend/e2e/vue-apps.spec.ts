import { expect, test } from "@playwright/test";
import { createReadStream, existsSync } from "node:fs";
import { createServer, type Server } from "node:http";
import path from "node:path";

const apiBase = "http://nexusflow.local/api";
const frontendRoot = path.resolve(__dirname, "..");
const port = 4179;
let server: Server;

const apps = [
  {
    key: "checkout",
    settingsKey: "nexusflow.checkout.config",
    title: "NexusFlow Checkout",
    visibleText: ["NexusFlow Checkout", "Payment address", "Generate Address", "Request log"]
  },
  {
    key: "merchant",
    settingsKey: "nexusflow.merchant.config",
    title: "NexusFlow Merchant",
    visibleText: ["Merchant Access", "Sign in", "Email", "Password"]
  },
  {
    key: "ops",
    settingsKey: "nexusflow.ops.config",
    title: "NexusFlow Ops",
    visibleText: ["Internal Access", "Sign in", "Email", "Password"]
  },
  {
    key: "admin",
    settingsKey: "nexusflow.admin.config",
    title: "NexusFlow Admin",
    visibleText: ["Platform Access", "Sign in", "Email", "Password"]
  }
];

test.describe("Vue dist-apps", () => {
  test.describe.configure({ mode: "serial" });

  test.beforeAll(async () => {
    server = await startDistAppServer();
  });

  test.afterAll(async () => {
    await new Promise<void>((resolve, reject) => {
      server.close((error) => {
        if (error) {
          reject(error);
          return;
        }
        resolve();
      });
    });
  });

  for (const app of apps) {
    test(`${app.key} Vue dist-app boots in Chromium`, async ({ page }) => {
      await page.route(`${apiBase}/**`, async (route) => {
        await route.fulfill({
          status: 401,
          contentType: "application/json",
          body: JSON.stringify({ success: false, message: "Not authenticated" })
        });
      });

      await page.addInitScript(({ key, base }) => {
        window.localStorage.setItem(key, JSON.stringify({ apiBase: base }));
      }, { key: app.settingsKey, base: apiBase });

      await page.goto(`http://127.0.0.1:${port}/${app.key}/`);

      await expect(page).toHaveTitle(app.title);
      for (const text of app.visibleText) {
        await expect(page.getByText(text, { exact: false }).first()).toBeVisible();
      }
    });
  }

  test("checkout loads an order and generates a deposit address", async ({ page }) => {
    await seedApiBase(page, "nexusflow.checkout.config");
    await page.route(`${apiBase}/cashier/order/status?**`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: {
            paymentId: "pay_checkout_001",
            status: "WAITING_PAYMENT",
            amountFiat: "100.00",
            currencyFiat: "USD",
            amountCrypto: "100.00",
            currencyCrypto: "USDT",
            network: "TRC20",
            requiredConfirmations: 12,
            expireTime: Date.now() + 900_000
          }
        })
      });
    });
    await page.route(`${apiBase}/cashier/pay/submit`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: {
            paymentId: "pay_checkout_001",
            status: "WAITING_PAYMENT",
            token: "USDT",
            network: "TRC20",
            cryptoAmount: "100.00",
            fiatAmount: "100.00",
            fiatCurrency: "USD",
            channelId: "STUB",
            payAddress: "TMockCheckoutAddress001",
            requiredConfirmations: 12,
            expireTime: Date.now() + 900_000
          }
        })
      });
    });

    await page.goto(`http://127.0.0.1:${port}/checkout/?payment_id=pay_checkout_001`);
    await expect(page.getByText("100.00", { exact: false }).first()).toBeVisible();
    await page.getByRole("button", { name: "Generate Address" }).click();

    await expect(page.getByText("TMockCheckoutAddress001")).toBeVisible();
    await expect(page.getByText("Payment address generated")).toBeVisible();
  });

  test("merchant signs in and creates a payment order", async ({ page }) => {
    const user = merchantUser();
    await seedApiBase(page, "nexusflow.merchant.config");
    await page.route(`${apiBase}/auth/me`, async (route) => fulfillJson(route, 401, { success: false, message: "Not authenticated" }));
    await page.route(`${apiBase}/auth/login`, async (route) => fulfillJson(route, 200, { success: true, data: user }));
    await page.route(`${apiBase}/pay/order`, async (route) => {
      await fulfillJson(route, 200, {
        success: true,
        data: {
          merchantId: "merchant-001",
          paymentId: "pay_merchant_001",
          merchantOrderNo: "ord-e2e-001",
          status: "WAITING_PAYMENT",
          amountFiat: "100.00",
          currencyFiat: "USD",
          amountCrypto: "100.00",
          currencyCrypto: "USDT",
          network: "TRC20",
          channelId: "STUB",
          payUrl: "https://checkout.example/pay_merchant_001"
        }
      });
    });

    await page.goto(`http://127.0.0.1:${port}/merchant/`);
    await page.getByLabel("Email").fill("merchant@example.com");
    await page.getByLabel("Password").fill("secret");
    await page.getByRole("button", { name: "Sign In" }).click();
    await expect(page.getByText("Orders and refunds")).toBeVisible();

    await page.locator("form").filter({ hasText: "Fiat Amount" }).first().getByLabel("Merchant Order No").fill("ord-e2e-001");
    await page.getByRole("button", { name: "Create Order" }).click();

    await expect(page.getByText("pay_merchant_001").first()).toBeVisible();
    await expect(page.getByText("Order created")).toBeVisible();
  });

  test("ops signs in and resolves an orphan transaction", async ({ page }) => {
    await seedApiBase(page, "nexusflow.ops.config");
    await page.route(`${apiBase}/auth/me`, async (route) => fulfillJson(route, 401, { success: false, message: "Not authenticated" }));
    await page.route(`${apiBase}/auth/login`, async (route) => fulfillJson(route, 200, { success: true, data: opsUser() }));
    await page.route(`${apiBase}/ops/dashboard`, async (route) => fulfillJson(route, 200, { success: true, data: opsDashboard() }));
    await page.route(`${apiBase}/crypto/orphan-transactions?**`, async (route) => {
      await fulfillJson(route, 200, {
        success: true,
        data: [{
          chain: "TRON",
          txHash: "0xorphane2e",
          amount: "9.5",
          currency: "USDT",
          status: "UNMATCHED"
        }]
      });
    });
    await page.route(`${apiBase}/crypto/orphan-transactions/TRON/0xorphane2e/resolve`, async (route) => {
      await fulfillJson(route, 200, {
        success: true,
        data: {
          chain: "TRON",
          txHash: "0xorphane2e",
          amount: "9.5",
          currency: "USDT",
          status: "RESOLVED",
          resolvedPaymentId: "pay_ops_001"
        }
      });
    });

    await page.goto(`http://127.0.0.1:${port}/ops/`);
    await page.getByLabel("Email").fill("ops@example.com");
    await page.getByLabel("Password").fill("secret");
    await page.getByRole("button", { name: "Sign In" }).click();
    await expect(page.getByText("Channel and order monitor")).toBeVisible();

    await page.getByRole("button", { name: "Interventions" }).click();
    await page.getByRole("button", { name: "Load" }).first().click();
    await expect(page.getByText("0xorphane2e")).toBeVisible();
    await page.getByPlaceholder("paymentId").fill("pay_ops_001");
    await page.getByRole("button", { name: "Resolve" }).click();

    await expect(page.getByText("Orphan resolved")).toBeVisible();
  });

  test("admin signs in, creates a role, and assigns a permission", async ({ page }) => {
    const roles = [{ id: "role-admin-viewer", code: "ADMIN_VIEWER", name: "Admin Viewer", scope: "SYSTEM", permissions: [] }];
    const permissions = [{ id: "perm-role-read", code: "role:read", name: "Read roles", scope: "SYSTEM" }];

    await seedApiBase(page, "nexusflow.admin.config");
    await page.route(`${apiBase}/auth/me`, async (route) => fulfillJson(route, 401, { success: false, message: "Not authenticated" }));
    await page.route(`${apiBase}/auth/login`, async (route) => fulfillJson(route, 200, { success: true, data: adminUser() }));
    await page.route(`${apiBase}/admin/permissions**`, async (route) => fulfillJson(route, 200, { success: true, data: permissions }));
    await page.route(`${apiBase}/admin/roles**`, async (route) => {
      const url = new URL(route.request().url());
      const method = route.request().method();

      if (url.pathname.endsWith("/permissions") && method === "PUT") {
        roles[0] = { ...roles[0], permissions };
        await fulfillJson(route, 200, { success: true, data: roles[0] });
        return;
      }

      if (url.pathname.endsWith("/role-settlement") && method === "GET") {
        await fulfillJson(route, 200, { success: true, data: roles[0] });
        return;
      }

      if (url.pathname.endsWith("/admin/roles") && method === "POST") {
        const created = { id: "role-settlement", code: "SETTLEMENT_AUDITOR", name: "Settlement Auditor", scope: "SYSTEM", permissions: [] };
        roles.unshift(created);
        await fulfillJson(route, 200, { success: true, data: created });
        return;
      }

      await fulfillJson(route, 200, { success: true, data: roles });
    });

    await page.goto(`http://127.0.0.1:${port}/admin/`);
    await page.getByLabel("Email").fill("admin@example.com");
    await page.getByLabel("Password").fill("secret");
    await page.getByRole("button", { name: "Sign In" }).click();
    await expect(page.getByText("Roles and permissions")).toBeVisible();

    await page.getByLabel("Role Code").fill("SETTLEMENT_AUDITOR");
    await page.getByLabel("Display Name").fill("Settlement Auditor");
    await page.getByRole("button", { name: "Create Role" }).click();
    await expect(page.getByText("Role created")).toBeVisible();

    await page.locator("tr").filter({ hasText: "SETTLEMENT_AUDITOR" }).getByRole("button", { name: "Open" }).click();
    await page.locator("form.inline-form").getByRole("combobox").selectOption("role:read");
    await page.getByRole("button", { name: "Add" }).click();
    await expect(page.getByText("Permission added")).toBeVisible();
  });
});

async function seedApiBase(page: import("@playwright/test").Page, settingsKey: string) {
  await page.addInitScript(({ key, base }) => {
    window.localStorage.setItem(key, JSON.stringify({ apiBase: base }));
  }, { key: settingsKey, base: apiBase });
}

async function fulfillJson(route: import("@playwright/test").Route, status: number, body: unknown) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body)
  });
}

function merchantUser() {
  return {
    userId: "user-merchant",
    email: "merchant@example.com",
    displayName: "Merchant User",
    activeMerchantId: "merchant-001",
    memberships: [{
      merchantId: "merchant-001",
      merchantCode: "M001",
      displayName: "Demo Merchant",
      roleCode: "MERCHANT_ADMIN"
    }]
  };
}

function opsUser() {
  return {
    userId: "user-ops",
    email: "ops@example.com",
    displayName: "Ops User"
  };
}

function adminUser() {
  return {
    userId: "user-admin",
    email: "admin@example.com",
    displayName: "Admin User",
    activeMerchantId: "SYSTEM"
  };
}

function opsDashboard() {
  return {
    generatedAt: Date.now(),
    channels: [{ channelId: "STUB", displayName: "Stub Channel", status: "UP", supportedCurrencyCount: 2 }],
    orderStatusCounts: { WAITING_PAYMENT: 1, CONFIRMED: 2 },
    reconciliation: {
      pendingExecutionPayments: 0,
      unconfirmedExecutionPayments: 1,
      unmatchedOrphanTransactions: 1,
      partiallyPaidOrders: 0,
      refundProcessingOrders: 0
    },
    alerts: [{ severity: "MEDIUM", code: "ORPHAN_BACKLOG", message: "Unmatched orphan transaction", count: 1 }],
    recentOrders: [{
      paymentId: "pay_ops_recent",
      merchantId: "merchant-001",
      merchantOrderNo: "ord-ops",
      status: "WAITING_PAYMENT",
      amountFiat: "10.00",
      currencyFiat: "USD",
      channelId: "STUB",
      updateTime: Date.now()
    }]
  };
}

async function startDistAppServer() {
  const server = createServer((request, response) => {
    const requestUrl = new URL(request.url ?? "/", `http://127.0.0.1:${port}`);
    const [app, ...rest] = requestUrl.pathname.replace(/^\/+/, "").split("/");
    if (!apps.some((candidate) => candidate.key === app)) {
      response.writeHead(404, { "content-type": "text/plain; charset=utf-8" });
      response.end("unknown app");
      return;
    }

    const relativePath = rest.length === 0 || rest.join("/") === "" ? "index.html" : rest.join("/");
    const distRoot = path.join(frontendRoot, "apps", app, "dist-app");
    const filePath = path.resolve(distRoot, relativePath);

    if (!filePath.startsWith(distRoot) || !existsSync(filePath)) {
      response.writeHead(404, { "content-type": "text/plain; charset=utf-8" });
      response.end("not found");
      return;
    }

    response.writeHead(200, {
      "content-type": contentType(filePath),
      "cache-control": "no-store"
    });
    createReadStream(filePath).pipe(response);
  });

  await new Promise<void>((resolve) => {
    server.listen(port, "127.0.0.1", resolve);
  });
  return server;
}

function contentType(filePath: string) {
  const extension = path.extname(filePath);
  if (extension === ".html") {
    return "text/html; charset=utf-8";
  }
  if (extension === ".js") {
    return "text/javascript; charset=utf-8";
  }
  if (extension === ".css") {
    return "text/css; charset=utf-8";
  }
  return "application/octet-stream";
}
