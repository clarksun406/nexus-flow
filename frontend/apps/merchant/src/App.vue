<template>
  <main class="merchant-app">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand__mark" aria-hidden="true"></span>
        <div>
          <strong>NexusFlow</strong>
          <span>Merchant Portal</span>
        </div>
      </div>

      <section class="side-section">
        <label for="apiBase">API Base</label>
        <input id="apiBase" v-model="apiBaseInput" placeholder="/api" @blur="saveSettings" />
      </section>

      <section v-if="user" class="side-section">
        <label for="merchantSelect">Merchant</label>
        <select id="merchantSelect" v-model="selectedMerchantId" @change="changeMerchant">
          <option
            v-for="membership in memberships"
            :key="membership.merchantId"
            :value="membership.merchantId"
          >
            {{ membership.displayName || membership.merchantCode || membership.merchantId }}
          </option>
        </select>
      </section>

      <section v-if="user" class="side-section">
        <span class="label">Signed In</span>
        <strong class="user-line">{{ user.email || user.displayName || user.userId }}</strong>
        <button class="btn btn--ghost" type="button" @click="signOut">Sign Out</button>
      </section>
    </aside>

    <section v-if="!user" class="login-view">
      <form class="login-panel" @submit.prevent="submitLogin">
        <div>
          <p class="eyebrow">Merchant Access</p>
          <h1>Sign in</h1>
        </div>
        <label>
          Email
          <input v-model="loginForm.email" type="email" autocomplete="email" required />
        </label>
        <label>
          Password
          <input v-model="loginForm.password" type="password" autocomplete="current-password" required />
        </label>
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
        <button class="btn btn--primary" type="submit" :disabled="busy">
          {{ busy ? "Signing in" : "Sign In" }}
        </button>
      </form>
    </section>

    <section v-else class="workspace">
      <header class="workspace__header">
        <div>
          <p class="eyebrow">Merchant Operations</p>
          <h1>Orders and refunds</h1>
        </div>
        <div class="header-actions">
          <button class="btn" type="button" @click="refreshSession" :disabled="busy">Refresh Session</button>
          <button class="btn btn--primary" type="button" @click="submitCreateOrder" :disabled="busy">
            Create Order
          </button>
        </div>
      </header>

      <section class="metrics" aria-label="merchant metrics">
        <div class="metric">
          <span>Recent Orders</span>
          <strong>{{ recentOrders.length }}</strong>
        </div>
        <div class="metric">
          <span>Confirmed Volume</span>
          <strong>{{ confirmedVolume }}</strong>
        </div>
        <div class="metric">
          <span>Open Refund</span>
          <strong>{{ lastRefund?.status || "None" }}</strong>
        </div>
        <div class="metric">
          <span>Active Merchant</span>
          <strong>{{ selectedMerchantId || "Unset" }}</strong>
        </div>
      </section>

      <div class="content-grid">
        <section class="panel">
          <div class="panel__head">
            <h2>Create order</h2>
            <div class="segmented" aria-label="order mode">
              <button :class="{ active: orderMode === 'fiat' }" type="button" @click="orderMode = 'fiat'">
                Fiat
              </button>
              <button :class="{ active: orderMode === 'crypto' }" type="button" @click="orderMode = 'crypto'">
                Crypto
              </button>
            </div>
          </div>

          <form class="form-grid" @submit.prevent="submitCreateOrder">
            <label>
              Merchant ID
              <input v-model="selectedMerchantId" readonly />
            </label>
            <label>
              Merchant Order No
              <input v-model="orderForm.merchantOrderNo" required />
            </label>

            <template v-if="orderMode === 'fiat'">
              <label>
                Fiat Amount
                <input v-model="orderForm.amountFiat" inputmode="decimal" placeholder="100.00" />
              </label>
              <label>
                Fiat Currency
                <input v-model="orderForm.currencyFiat" />
              </label>
            </template>

            <template v-else>
              <label>
                Crypto Amount
                <input v-model="orderForm.amountCrypto" inputmode="decimal" placeholder="25.5" />
              </label>
              <label>
                Crypto Asset
                <input v-model="orderForm.currencyCrypto" />
              </label>
              <label>
                Network
                <select v-model="orderForm.network">
                  <option>TRC20</option>
                  <option>ERC20</option>
                  <option>BTC</option>
                </select>
              </label>
              <label>
                Fiat Currency
                <input v-model="orderForm.currencyFiat" />
              </label>
            </template>

            <label>
              Preferred Channel
              <select v-model="orderForm.preferredChannel">
                <option value="">Auto</option>
                <option>STUB</option>
                <option>BITMART</option>
                <option>BINANCE_PAY</option>
                <option>COINBASE_COMMERCE</option>
                <option>SELF_HOSTED_NODE</option>
              </select>
            </label>
            <label>
              Callback URL
              <input v-model="defaultNotifyUrl" placeholder="https://merchant.example/callback" @blur="saveSettings" />
            </label>
          </form>
        </section>

        <section class="panel">
          <div class="panel__head">
            <h2>Order lookup</h2>
            <form class="lookup" @submit.prevent="submitLookup">
              <input v-model="lookupPaymentId" placeholder="paymentId" />
              <button class="btn btn--primary" type="submit" :disabled="busy">Load</button>
            </form>
          </div>

          <div v-if="selectedOrder" class="detail-grid">
            <div>
              <span>Status</span>
              <strong><span class="status" :class="statusClass(selectedOrder.status)">{{ selectedOrder.status }}</span></strong>
            </div>
            <div>
              <span>Payment ID</span>
              <strong class="mono">{{ selectedOrder.paymentId }}</strong>
            </div>
            <div>
              <span>Amount</span>
              <strong>{{ money(selectedOrder.amountFiat, selectedOrder.currencyFiat) }}</strong>
            </div>
            <div>
              <span>Crypto</span>
              <strong>{{ money(selectedOrder.amountCrypto, selectedOrder.currencyCrypto) }}</strong>
            </div>
            <div>
              <span>Channel</span>
              <strong>{{ selectedOrder.channelName || selectedOrder.channelId || "-" }}</strong>
            </div>
            <div>
              <span>Network</span>
              <strong>{{ selectedOrder.network || "-" }}</strong>
            </div>
            <div class="wide">
              <span>Pay Address</span>
              <strong class="mono">{{ selectedOrder.payAddress || "-" }}</strong>
            </div>
            <div class="wide">
              <span>Pay URL</span>
              <strong class="mono">{{ selectedOrder.payUrl || "-" }}</strong>
            </div>
          </div>
          <div v-else class="empty">No order loaded</div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <h2>Refund</h2>
            <button class="btn btn--danger" type="button" @click="submitRefund" :disabled="busy">Submit Refund</button>
          </div>

          <form class="form-grid" @submit.prevent="submitRefund">
            <label>
              Merchant ID
              <input v-model="selectedMerchantId" readonly />
            </label>
            <label>
              Merchant Order No
              <input v-model="refundForm.merchantOrderNo" required />
            </label>
            <label>
              Refund Order No
              <input v-model="refundForm.refundOrderNo" required />
            </label>
            <label>
              Refund Fiat Amount
              <input v-model="refundForm.refundAmountFiat" required inputmode="decimal" />
            </label>
            <label class="span-2">
              To Address
              <input v-model="refundForm.toAddress" />
            </label>
          </form>
        </section>

        <section class="panel">
          <div class="panel__head">
            <h2>Recent orders</h2>
            <button class="btn" type="button" @click="clearRecent">Clear</button>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Payment ID</th>
                  <th>Merchant Order</th>
                  <th>Status</th>
                  <th>Amount</th>
                  <th>Channel</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in recentOrders" :key="order.paymentId">
                  <td class="mono">{{ order.paymentId }}</td>
                  <td class="mono">{{ order.merchantOrderNo }}</td>
                  <td><span class="status" :class="statusClass(order.status)">{{ order.status }}</span></td>
                  <td>{{ money(order.amountFiat, order.currencyFiat) }}</td>
                  <td>{{ order.channelId || "-" }}</td>
                  <td><button class="btn" type="button" @click="loadOrderById(order.paymentId)">Load</button></td>
                </tr>
              </tbody>
            </table>
            <div v-if="recentOrders.length === 0" class="empty">No recent orders</div>
          </div>
        </section>
      </div>

      <p v-if="errorMessage" class="toast toast--error">{{ errorMessage }}</p>
      <p v-if="noticeMessage" class="toast">{{ noticeMessage }}</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { configureApiClient } from "@nexusflow/api-client";
import {
  createOrder,
  getOrder,
  loadCurrentUser,
  login,
  logout,
  refundOrder,
  switchActiveMerchant
} from "./merchantApi";
import type { OrderResponse, RefundResponse, UserInfo } from "./types";

type OrderMode = "fiat" | "crypto";

const settingsKey = "nexusflow.merchant.config";
const recentKey = "nexusflow.merchant.recentOrders";

const apiBaseInput = ref("");
const defaultNotifyUrl = ref("");
const selectedMerchantId = ref("");
const user = ref<UserInfo | null>(null);
const busy = ref(false);
const errorMessage = ref("");
const noticeMessage = ref("");
const orderMode = ref<OrderMode>("fiat");
const lookupPaymentId = ref("");
const selectedOrder = ref<OrderResponse | null>(null);
const recentOrders = ref<OrderResponse[]>([]);
const lastRefund = ref<RefundResponse | null>(null);

const loginForm = ref({
  email: "",
  password: ""
});

const orderForm = ref({
  merchantOrderNo: nextOrderNo("ord"),
  amountFiat: "100.00",
  currencyFiat: "USD",
  amountCrypto: "",
  currencyCrypto: "USDT",
  network: "TRC20",
  preferredChannel: ""
});

const refundForm = ref({
  merchantOrderNo: "",
  refundOrderNo: nextOrderNo("ref"),
  refundAmountFiat: "",
  toAddress: ""
});

const memberships = computed(() => user.value?.memberships ?? []);
const confirmedVolume = computed(() => {
  const total = recentOrders.value
    .filter((order) => order.status === "CONFIRMED")
    .reduce((sum, order) => sum + Number(order.amountFiat ?? 0), 0);
  return Number.isFinite(total) ? String(Math.round(total * 100) / 100) : "0";
});

onMounted(() => {
  loadSettings();
  recentOrders.value = readJson<OrderResponse[]>(recentKey, []);
  void refreshSession();
});

async function submitLogin() {
  await run(async () => {
    saveSettings();
    const currentUser = await login(loginForm.value.email, loginForm.value.password);
    applyUser(currentUser);
    loginForm.value.password = "";
    notice("Signed in");
  });
}

async function refreshSession() {
  await run(async () => {
    saveSettings();
    const currentUser = await loadCurrentUser();
    applyUser(currentUser);
  }, { quietUnauthorized: true });
}

async function changeMerchant() {
  if (!selectedMerchantId.value) {
    return;
  }
  await run(async () => {
    const currentUser = await switchActiveMerchant(selectedMerchantId.value);
    applyUser(currentUser);
    notice("Merchant changed");
  });
}

async function signOut() {
  await run(async () => {
    await logout();
    user.value = null;
    selectedMerchantId.value = "";
    selectedOrder.value = null;
    notice("Signed out");
  });
}

async function submitCreateOrder() {
  await run(async () => {
    requireMerchant();
    const request = {
      merchantId: selectedMerchantId.value,
      merchantOrderNo: orderForm.value.merchantOrderNo,
      preferredChannel: emptyToNull(orderForm.value.preferredChannel),
      notifyUrl: emptyToNull(defaultNotifyUrl.value)
    };
    const payload = orderMode.value === "fiat"
      ? {
          ...request,
          amountFiat: orderForm.value.amountFiat,
          currencyFiat: orderForm.value.currencyFiat.toUpperCase()
        }
      : {
          ...request,
          amountCrypto: orderForm.value.amountCrypto,
          currencyCrypto: orderForm.value.currencyCrypto.toUpperCase(),
          network: orderForm.value.network.toUpperCase(),
          currencyFiat: orderForm.value.currencyFiat.toUpperCase() || "USD"
        };
    const order = await createOrder(payload);
    selectOrder(order);
    orderForm.value.merchantOrderNo = nextOrderNo("ord");
    notice("Order created");
  });
}

async function submitLookup() {
  if (!lookupPaymentId.value.trim()) {
    errorMessage.value = "paymentId is required";
    return;
  }
  await loadOrderById(lookupPaymentId.value.trim());
}

async function loadOrderById(paymentId: string) {
  await run(async () => {
    const order = await getOrder(paymentId);
    selectOrder(order);
    notice("Order loaded");
  });
}

async function submitRefund() {
  await run(async () => {
    requireMerchant();
    const refund = await refundOrder({
      merchantId: selectedMerchantId.value,
      merchantOrderNo: refundForm.value.merchantOrderNo,
      refundOrderNo: refundForm.value.refundOrderNo,
      refundAmountFiat: refundForm.value.refundAmountFiat,
      toAddress: emptyToNull(refundForm.value.toAddress),
      notifyUrl: emptyToNull(defaultNotifyUrl.value)
    });
    lastRefund.value = refund;
    refundForm.value.refundOrderNo = nextOrderNo("ref");
    notice("Refund submitted");
  });
}

function selectOrder(order: OrderResponse) {
  selectedOrder.value = order;
  lookupPaymentId.value = order.paymentId;
  refundForm.value.merchantOrderNo = order.merchantOrderNo;
  recentOrders.value = [
    order,
    ...recentOrders.value.filter((item) => item.paymentId !== order.paymentId)
  ].slice(0, 12);
  localStorage.setItem(recentKey, JSON.stringify(recentOrders.value));
}

function applyUser(currentUser: UserInfo) {
  user.value = currentUser;
  const nextMerchantId = currentUser.activeMerchantId
    || selectedMerchantId.value
    || currentUser.memberships?.[0]?.merchantId
    || "";
  selectedMerchantId.value = nextMerchantId;
  saveSettings();
}

function loadSettings() {
  const settings = readJson<{ apiBase?: string; notifyUrl?: string; activeMerchantId?: string }>(settingsKey, {});
  apiBaseInput.value = settings.apiBase ?? "";
  defaultNotifyUrl.value = settings.notifyUrl ?? "";
  selectedMerchantId.value = settings.activeMerchantId ?? "";
  configureApiClient({ apiBase: apiBaseInput.value });
}

function saveSettings() {
  configureApiClient({ apiBase: apiBaseInput.value });
  localStorage.setItem(settingsKey, JSON.stringify({
    apiBase: apiBaseInput.value.trim().replace(/\/$/, ""),
    notifyUrl: defaultNotifyUrl.value.trim(),
    activeMerchantId: selectedMerchantId.value
  }));
}

function clearRecent() {
  recentOrders.value = [];
  selectedOrder.value = null;
  localStorage.removeItem(recentKey);
}

async function run(action: () => Promise<void>, options: { quietUnauthorized?: boolean } = {}) {
  busy.value = true;
  errorMessage.value = "";
  try {
    await action();
  } catch (error) {
    const message = error instanceof Error ? error.message : "Request failed";
    if (!options.quietUnauthorized || !message.toLowerCase().includes("authenticated")) {
      errorMessage.value = message;
    }
  } finally {
    busy.value = false;
  }
}

function requireMerchant() {
  if (!selectedMerchantId.value) {
    throw new Error("Select a merchant first");
  }
}

function notice(message: string) {
  noticeMessage.value = message;
  window.setTimeout(() => {
    if (noticeMessage.value === message) {
      noticeMessage.value = "";
    }
  }, 2200);
}

function statusClass(status?: string) {
  if (status === "CONFIRMED" || status === "REFUNDED") {
    return "status--ok";
  }
  if (status === "FAILED" || status === "EXPIRED" || status === "REFUND_FAILED") {
    return "status--bad";
  }
  if (status === "PARTIALLY_PAID" || status === "REFUND_PROCESSING") {
    return "status--warn";
  }
  return "status--wait";
}

function money(amount?: string, currency?: string) {
  return amount ? `${amount} ${currency ?? ""}`.trim() : "-";
}

function emptyToNull(value: string) {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function nextOrderNo(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function readJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) as T : fallback;
  } catch {
    return fallback;
  }
}
</script>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

:global(body) {
  margin: 0;
  background:
    radial-gradient(900px 500px at 88% -12%, rgba(79, 70, 229, 0.12), transparent 62%),
    radial-gradient(760px 460px at -8% 24%, rgba(14, 165, 164, 0.1), transparent 60%),
    #eef1f7;
  color: #0b1526;
  font-family: ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
  -webkit-font-smoothing: antialiased;
}

button,
input,
select {
  font: inherit;
}

.merchant-app {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
}

.sidebar {
  min-height: 100vh;
  padding: 22px;
  border-right: 1px solid rgba(11, 21, 38, 0.07);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(14px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.brand__mark {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4f46e5 10%, #7c6cf2 45%, #0ea5a4 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.4), 0 8px 18px -8px rgba(79, 70, 229, 0.55);
}

.brand strong,
.brand span,
.metric span,
.detail-grid span,
.label {
  display: block;
}

.brand strong {
  font-size: 15px;
  letter-spacing: -0.01em;
}

.brand span,
.label,
label {
  color: #8b99ab;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.side-section {
  display: grid;
  gap: 8px;
  margin-bottom: 18px;
}

input,
select {
  width: 100%;
  min-height: 40px;
  border: 1px solid #e3e8f0;
  border-radius: 10px;
  background: #ffffff;
  color: #0b1526;
  padding: 9px 12px;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}

input::placeholder {
  color: #8b99ab;
}

input:focus,
select:focus {
  border-color: #4f46e5;
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.15);
}

.user-line {
  overflow-wrap: anywhere;
  font-size: 13px;
}

.login-view {
  display: grid;
  place-items: center;
  padding: 24px;
}

.login-panel {
  position: relative;
  width: min(420px, 100%);
  display: grid;
  gap: 16px;
  padding: 32px 30px 28px;
  border: 1px solid rgba(11, 21, 38, 0.08);
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 30px 60px -25px rgba(11, 21, 38, 0.25);
  overflow: hidden;
}

.login-panel::before {
  content: "";
  position: absolute;
  inset: 0 0 auto 0;
  height: 4px;
  background: linear-gradient(90deg, #4f46e5, #7c6cf2 45%, #0ea5a4);
}

.workspace {
  min-width: 0;
  padding: 24px clamp(16px, 3vw, 32px) 40px;
}

.workspace__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #8b99ab;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.09em;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0;
  letter-spacing: -0.01em;
}

h1 {
  font-size: 26px;
}

h2 {
  font-size: 12px;
  font-weight: 750;
  letter-spacing: 0.09em;
  text-transform: uppercase;
  color: #5b6b7e;
}

.header-actions,
.lookup,
.panel__head,
.segmented {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn {
  min-height: 40px;
  border: 1px solid #e3e8f0;
  border-radius: 10px;
  background: #ffffff;
  color: #0b1526;
  padding: 8px 14px;
  font-weight: 650;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, transform 0.05s;
}

.btn:hover {
  background: #f7f9fd;
  border-color: #cdd7e4;
}

.btn:active {
  transform: translateY(1px);
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.btn--primary {
  border-color: transparent;
  background: linear-gradient(135deg, #6366f1, #4338ca);
  color: #ffffff;
  box-shadow: 0 10px 20px -10px rgba(79, 70, 229, 0.65);
}

.btn--primary:hover {
  background: linear-gradient(135deg, #5558e8, #3730a3);
  border-color: transparent;
}

.btn--danger {
  border-color: #f3c6c6;
  color: #dc2626;
}

.btn--danger:hover {
  background: #fef2f2;
}

.btn--ghost {
  background: #f6f8fb;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.metric,
.panel {
  border: 1px solid rgba(11, 21, 38, 0.07);
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 24px 48px -20px rgba(11, 21, 38, 0.16);
}

.metric {
  min-height: 88px;
  padding: 16px;
  transition: transform 0.15s;
}

.metric:hover {
  transform: translateY(-2px);
}

.metric span {
  color: #8b99ab;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.metric strong {
  display: block;
  margin-top: 8px;
  overflow-wrap: anywhere;
  font-size: 22px;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(340px, 430px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.panel {
  overflow: hidden;
}

.panel__head {
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #edf1f7;
}

.segmented {
  gap: 4px;
  padding: 4px;
  border-radius: 10px;
  background: #edf1f7;
}

.segmented button {
  min-height: 30px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #5b6b7e;
  padding: 5px 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}

.segmented button.active {
  background: #ffffff;
  color: #0b1526;
  box-shadow: 0 2px 8px rgba(11, 21, 38, 0.1);
}

.form-grid,
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 20px;
}

label {
  display: grid;
  gap: 6px;
}

.span-2,
.wide {
  grid-column: span 2;
}

.lookup {
  min-width: min(420px, 100%);
}

.detail-grid > div {
  min-height: 70px;
  padding: 12px 14px;
  border: 1px solid #edf1f7;
  border-radius: 14px;
  background: #fbfcfe;
}

.detail-grid span {
  color: #8b99ab;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.detail-grid strong {
  display: block;
  margin-top: 7px;
  overflow-wrap: anywhere;
  font-size: 13px;
  font-weight: 650;
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 28px;
  border-radius: 999px;
  padding: 4px 12px;
  background: #f6f8fb;
  color: #5b6b7e;
  border: 1px solid #e3e8f0;
  font-size: 12px;
  font-weight: 750;
}

.status::before {
  content: "";
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: currentColor;
}

.status--ok {
  background: #ecfdf5;
  color: #059669;
  border-color: #b5e3cd;
}

.status--bad {
  background: #fef2f2;
  color: #dc2626;
  border-color: #f3c6c6;
}

.status--warn {
  background: #fff8ee;
  color: #d97706;
  border-color: #f3d9ad;
}

.status--wait {
  background: #eff6ff;
  color: #2563eb;
  border-color: #c9dcf8;
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
}

th,
td {
  padding: 12px 16px;
  border-bottom: 1px solid #edf1f7;
  text-align: left;
  font-size: 13px;
}

th {
  color: #8b99ab;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  background: #fbfcfe;
}

tbody tr:hover {
  background: #f8fafd;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-variant-numeric: tabular-nums;
}

.empty {
  padding: 34px 16px;
  color: #5b6b7e;
  text-align: center;
}

.error {
  margin: 0;
  border: 1px solid #f3c6c6;
  border-radius: 12px;
  background: #fef2f2;
  color: #991b1b;
  padding: 11px 13px;
  font-size: 13px;
}

.toast {
  position: fixed;
  right: 18px;
  bottom: 18px;
  max-width: min(420px, calc(100vw - 36px));
  margin: 0;
  border: none;
  border-radius: 12px;
  background: #101a2e;
  color: #f2f6ff;
  box-shadow: 0 22px 44px -14px rgba(11, 21, 38, 0.5);
  padding: 12px 16px;
  font-size: 13px;
  font-weight: 600;
}

.toast--error {
  background: #7f1d1d;
  color: #fef2f2;
}

@media (max-width: 1120px) {
  .merchant-app {
    grid-template-columns: 1fr;
  }

  .sidebar {
    min-height: auto;
    border-right: 0;
    border-bottom: 1px solid rgba(11, 21, 38, 0.07);
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .workspace,
  .login-view,
  .sidebar {
    padding: 16px;
  }

  .workspace__header,
  .panel__head {
    align-items: stretch;
    flex-direction: column;
  }

  .metrics,
  .form-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .span-2,
  .wide {
    grid-column: span 1;
  }

  .header-actions,
  .lookup {
    width: 100%;
  }

  .header-actions .btn,
  .lookup .btn {
    flex: 1;
  }
}
</style>
