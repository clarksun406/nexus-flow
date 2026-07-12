<template>
  <main class="ops-app">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand__mark" aria-hidden="true"></span>
        <div>
          <strong>NexusFlow</strong>
          <span>Ops Console</span>
        </div>
      </div>

      <section class="side-section">
        <label for="apiBase">API Base</label>
        <input id="apiBase" v-model="apiBaseInput" placeholder="/api" @blur="saveSettings" />
      </section>

      <section v-if="user" class="side-section">
        <span class="label">Signed In</span>
        <strong class="user-line">{{ user.email || user.displayName || user.userId }}</strong>
        <button class="btn btn--ghost" type="button" @click="signOut">Sign Out</button>
      </section>

      <nav v-if="user" class="nav" aria-label="Ops sections">
        <button :class="{ active: activeView === 'dashboard' }" type="button" @click="activeView = 'dashboard'">
          Dashboard
        </button>
        <button :class="{ active: activeView === 'interventions' }" type="button" @click="activeView = 'interventions'">
          Interventions
        </button>
      </nav>
    </aside>

    <section v-if="!user" class="login-view">
      <form class="login-panel" @submit.prevent="submitLogin">
        <div>
          <p class="eyebrow">Internal Access</p>
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
          <p class="eyebrow">Operations</p>
          <h1>{{ activeView === "dashboard" ? "Channel and order monitor" : "Risk interventions" }}</h1>
        </div>
        <div class="header-actions">
          <button class="btn" type="button" @click="refreshSession" :disabled="busy">Refresh Session</button>
          <button class="btn btn--primary" type="button" @click="refreshDashboard" :disabled="busy">Refresh</button>
        </div>
      </header>

      <section class="metrics" aria-label="ops metrics">
        <div class="metric">
          <span>Channels Up</span>
          <strong>{{ channelsUp }}/{{ channels.length }}</strong>
        </div>
        <div class="metric">
          <span>Open Orders</span>
          <strong>{{ openOrders }}</strong>
        </div>
        <div class="metric">
          <span>Unconfirmed</span>
          <strong>{{ dashboard?.reconciliation?.unconfirmedExecutionPayments ?? 0 }}</strong>
        </div>
        <div class="metric">
          <span>Orphans</span>
          <strong>{{ dashboard?.reconciliation?.unmatchedOrphanTransactions ?? 0 }}</strong>
        </div>
        <div class="metric">
          <span>Refund Processing</span>
          <strong>{{ dashboard?.reconciliation?.refundProcessingOrders ?? 0 }}</strong>
        </div>
      </section>

      <div v-if="activeView === 'dashboard'" class="content-grid">
        <section class="panel">
          <div class="panel__head">
            <h2>Channel monitor</h2>
            <span class="muted">{{ formatTime(dashboard?.generatedAt) }}</span>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Channel</th>
                  <th>Status</th>
                  <th>Currencies</th>
                  <th>Message</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="channel in channels" :key="channel.channelId">
                  <td>
                    <strong>{{ channel.displayName || channel.channelId }}</strong>
                    <span class="sub mono">{{ channel.channelId }}</span>
                  </td>
                  <td><span class="pill" :class="channel.status === 'UP' ? 'pill--ok' : 'pill--bad'">{{ channel.status }}</span></td>
                  <td>{{ channel.supportedCurrencyCount }}</td>
                  <td>{{ channel.message || "-" }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="channels.length === 0" class="empty">No channel data loaded</div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <h2>Risk alerts</h2>
            <span class="muted">{{ alerts.length }} active</span>
          </div>
          <div class="alert-list">
            <div v-for="alert in alerts" :key="alert.code" class="alert">
              <span class="pill" :class="severityClass(alert.severity)">{{ alert.severity }}</span>
              <div>
                <strong>{{ alert.code }}</strong>
                <span>{{ alert.message }}</span>
              </div>
              <strong>{{ alert.count }}</strong>
            </div>
            <div v-if="alerts.length === 0" class="empty">No active alerts</div>
          </div>
        </section>

        <section class="panel panel--wide">
          <div class="panel__head">
            <h2>Recent orders</h2>
            <span class="muted">{{ recentOrders.length }} rows</span>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Payment ID</th>
                  <th>Merchant Order</th>
                  <th>Status</th>
                  <th>Fiat</th>
                  <th>Crypto</th>
                  <th>Channel</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in recentOrders" :key="order.paymentId">
                  <td class="mono">{{ order.paymentId }}</td>
                  <td class="mono">{{ order.merchantOrderNo }}</td>
                  <td><span class="pill" :class="statusClass(order.status)">{{ order.status }}</span></td>
                  <td>{{ money(order.amountFiat, order.currencyFiat) }}</td>
                  <td>{{ money(order.amountCrypto, order.currencyCrypto) }} {{ order.network || "" }}</td>
                  <td>{{ order.channelId || "-" }}</td>
                  <td>{{ formatTime(order.updateTime) }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="recentOrders.length === 0" class="empty">No recent orders</div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head"><h2>Order status</h2></div>
          <div class="status-grid">
            <div v-for="[name, value] in orderStatusRows" :key="name" class="status-card">
              <span>{{ name }}</span>
              <strong>{{ value }}</strong>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head"><h2>Reconciliation</h2></div>
          <div class="status-grid">
            <div v-for="[name, value] in reconciliationRows" :key="name" class="status-card">
              <span>{{ labelize(name) }}</span>
              <strong>{{ value }}</strong>
            </div>
          </div>
        </section>
      </div>

      <div v-else class="content-grid content-grid--interventions">
        <section class="panel">
          <div class="panel__head">
            <h2>Orphan transactions</h2>
            <div class="header-actions">
              <select v-model="orphanStatus">
                <option>UNMATCHED</option>
                <option>RESOLVED</option>
                <option>IGNORED</option>
                <option>COMPENSATED</option>
              </select>
              <button class="btn btn--primary" type="button" @click="loadOrphans" :disabled="busy">Load</button>
            </div>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Chain</th>
                  <th>Tx Hash</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="orphan in orphans" :key="`${orphan.chain}-${orphan.txHash}`">
                  <td>{{ orphan.chain }}</td>
                  <td class="mono">{{ orphan.txHash }}</td>
                  <td>{{ money(orphan.amount, orphan.currency) }}</td>
                  <td><span class="pill" :class="statusClass(orphan.status)">{{ orphan.status || "-" }}</span></td>
                  <td>
                    <div class="row-actions">
                      <input v-model="resolvePaymentIds[orphanKey(orphan)]" placeholder="paymentId" />
                      <button class="btn btn--primary" type="button" @click="submitResolve(orphan)">Resolve</button>
                      <button class="btn" type="button" @click="submitCompensate(orphan)">Compensate</button>
                      <button class="btn btn--danger" type="button" @click="submitIgnore(orphan)">Ignore</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="orphans.length === 0" class="empty">No orphan transactions loaded</div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <h2>Webhook dead letters</h2>
            <div class="header-actions">
              <select v-model="deadLetterStatus">
                <option value="PENDING">Pending</option>
                <option value="REPLAYED">Replayed</option>
                <option value="IGNORED">Ignored</option>
              </select>
              <button class="btn btn--primary" type="button" @click="loadDeadLetters" :disabled="busy">Load</button>
            </div>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Target</th>
                  <th>Event</th>
                  <th>Status</th>
                  <th>Attempts</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="letter in deadLetters" :key="letter.id">
                  <td>{{ letter.deliveryType || "-" }}</td>
                  <td class="mono">{{ shorten(letter.targetUrl, 42) }}</td>
                  <td>
                    <span class="mono">{{ letter.eventType || "-" }}</span>
                    <span class="sub">{{ letter.paymentId || letter.orderId || letter.eventId || "" }}</span>
                  </td>
                  <td><span class="pill" :class="statusClass(letter.status)">{{ letter.status || "-" }}</span></td>
                  <td>{{ letter.attempts ?? 0 }}</td>
                  <td>
                    <div class="row-actions">
                      <button class="btn btn--primary" type="button" :disabled="letter.status !== 'PENDING'" @click="submitReplay(letter.id)">Replay</button>
                      <button class="btn btn--danger" type="button" :disabled="letter.status !== 'PENDING'" @click="submitIgnoreDeadLetter(letter.id)">Ignore</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="deadLetters.length === 0" class="empty">No dead letters loaded</div>
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
  compensateOrphan,
  ignoreDeadLetter,
  ignoreOrphan,
  listDeadLetters,
  listOrphans,
  loadCurrentUser,
  loadDashboard,
  login,
  logout,
  replayDeadLetter,
  resolveOrphan
} from "./opsApi";
import type { DeadLetterStatus, OpsDashboard, OrphanTransaction, UserInfo, WebhookDeadLetter } from "./types";

type OpsView = "dashboard" | "interventions";

const settingsKey = "nexusflow.ops.config";

const apiBaseInput = ref("");
const user = ref<UserInfo | null>(null);
const activeView = ref<OpsView>("dashboard");
const busy = ref(false);
const errorMessage = ref("");
const noticeMessage = ref("");
const dashboard = ref<OpsDashboard | null>(null);
const orphans = ref<OrphanTransaction[]>([]);
const deadLetters = ref<WebhookDeadLetter[]>([]);
const orphanStatus = ref("UNMATCHED");
const deadLetterStatus = ref<DeadLetterStatus>("PENDING");
const resolvePaymentIds = ref<Record<string, string>>({});

const loginForm = ref({
  email: "",
  password: ""
});

const channels = computed(() => dashboard.value?.channels ?? []);
const channelsUp = computed(() => channels.value.filter((channel) => channel.status === "UP").length);
const recentOrders = computed(() => dashboard.value?.recentOrders ?? []);
const alerts = computed(() => dashboard.value?.alerts ?? []);
const openOrders = computed(() =>
  count(dashboard.value?.orderStatusCounts, "WAITING_PAYMENT")
  + count(dashboard.value?.orderStatusCounts, "PARTIALLY_PAID")
);
const orderStatusRows = computed(() => Object.entries(dashboard.value?.orderStatusCounts ?? {}));
const reconciliationRows = computed(() => Object.entries(dashboard.value?.reconciliation ?? {}));

onMounted(() => {
  loadSettings();
  void refreshSession();
});

async function submitLogin() {
  await run(async () => {
    saveSettings();
    user.value = await login(loginForm.value.email, loginForm.value.password);
    loginForm.value.password = "";
    await refreshDashboard();
    notice("Signed in");
  });
}

async function refreshSession() {
  await run(async () => {
    user.value = await loadCurrentUser();
    await refreshDashboard();
  }, { quietUnauthorized: true });
}

async function signOut() {
  await run(async () => {
    await logout();
    user.value = null;
    dashboard.value = null;
    orphans.value = [];
    deadLetters.value = [];
  });
}

async function refreshDashboard() {
  await run(async () => {
    dashboard.value = await loadDashboard();
  });
}

async function loadOrphans() {
  await run(async () => {
    orphans.value = await listOrphans(orphanStatus.value);
    notice("Orphans loaded");
  });
}

async function loadDeadLetters() {
  await run(async () => {
    deadLetters.value = await listDeadLetters(deadLetterStatus.value, 25);
    notice("Dead letters loaded");
  });
}

async function submitResolve(orphan: OrphanTransaction) {
  const paymentId = resolvePaymentIds.value[orphanKey(orphan)]?.trim();
  if (!paymentId) {
    errorMessage.value = "paymentId is required";
    return;
  }
  await run(async () => {
    await resolveOrphan(orphan.chain, orphan.txHash, paymentId);
    await Promise.all([loadOrphans(), refreshDashboard()]);
    notice("Orphan resolved");
  });
}

async function submitCompensate(orphan: OrphanTransaction) {
  if (!window.confirm(`Compensate orphan ${orphan.txHash}?`)) {
    return;
  }
  await run(async () => {
    await compensateOrphan(orphan.chain, orphan.txHash);
    await Promise.all([loadOrphans(), refreshDashboard()]);
    notice("Compensation requested");
  });
}

async function submitIgnore(orphan: OrphanTransaction) {
  if (!window.confirm(`Ignore orphan ${orphan.txHash}?`)) {
    return;
  }
  await run(async () => {
    await ignoreOrphan(orphan.chain, orphan.txHash);
    await Promise.all([loadOrphans(), refreshDashboard()]);
    notice("Orphan ignored");
  });
}

async function submitReplay(id: string) {
  await run(async () => {
    await replayDeadLetter(id);
    await loadDeadLetters();
    notice("Replay submitted");
  });
}

async function submitIgnoreDeadLetter(id: string) {
  if (!window.confirm("Ignore this dead letter?")) {
    return;
  }
  await run(async () => {
    await ignoreDeadLetter(id);
    await loadDeadLetters();
    notice("Dead letter ignored");
  });
}

function loadSettings() {
  const settings = readJson<{ apiBase?: string }>(settingsKey, {});
  apiBaseInput.value = settings.apiBase ?? "";
  configureApiClient({ apiBase: apiBaseInput.value });
}

function saveSettings() {
  const apiBase = apiBaseInput.value.trim().replace(/\/$/, "");
  apiBaseInput.value = apiBase;
  configureApiClient({ apiBase });
  localStorage.setItem(settingsKey, JSON.stringify({ apiBase }));
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

function notice(message: string) {
  noticeMessage.value = message;
  window.setTimeout(() => {
    if (noticeMessage.value === message) {
      noticeMessage.value = "";
    }
  }, 2200);
}

function orphanKey(orphan: OrphanTransaction) {
  return `${orphan.chain}:${orphan.txHash}`;
}

function count(map: Record<string, number> | undefined, key: string) {
  return Number(map?.[key] ?? 0);
}

function statusClass(status?: string) {
  if (status === "UP" || status === "CONFIRMED" || status === "REPLAYED" || status === "RESOLVED") {
    return "pill--ok";
  }
  if (status === "FAILED" || status === "EXPIRED" || status === "PENDING") {
    return "pill--bad";
  }
  if (status === "PARTIALLY_PAID" || status === "REFUND_PROCESSING" || status === "COMPENSATED") {
    return "pill--warn";
  }
  return "pill--info";
}

function severityClass(severity?: string) {
  if (severity === "HIGH") {
    return "pill--bad";
  }
  if (severity === "MEDIUM") {
    return "pill--warn";
  }
  return "pill--info";
}

function money(amount?: string, currency?: string) {
  return amount ? `${amount} ${currency ?? ""}`.trim() : "-";
}

function formatTime(value?: number) {
  return value ? new Date(value).toLocaleString() : "-";
}

function labelize(value: string) {
  return value.replace(/([A-Z])/g, " $1").replace(/^./, (char) => char.toUpperCase());
}

function shorten(value: string | undefined, size: number) {
  const text = value ?? "";
  return text.length > size ? `${text.slice(0, Math.max(0, size - 3))}...` : text;
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
  background: #f4f6f8;
  color: #18212b;
  font-family: "Aptos", "Segoe UI", sans-serif;
}

button,
input,
select {
  font: inherit;
}

.ops-app {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
}

.sidebar {
  min-height: 100vh;
  padding: 22px;
  border-right: 1px solid #dde5ed;
  background: #ffffff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.brand__mark {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: linear-gradient(135deg, #155e75, #15803d);
}

.brand strong,
.brand span,
.metric span,
.label,
.sub {
  display: block;
}

.brand strong {
  font-size: 15px;
}

.brand span,
.label,
label,
.muted,
.sub {
  color: #5f6f80;
  font-size: 12px;
  font-weight: 700;
}

.side-section {
  display: grid;
  gap: 8px;
  margin-bottom: 18px;
}

.nav {
  display: grid;
  gap: 8px;
}

.nav button {
  min-height: 38px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: #475569;
  padding: 8px 10px;
  text-align: left;
  font-weight: 800;
  cursor: pointer;
}

.nav button.active {
  border-color: #cbd5df;
  background: #f7fafc;
  color: #18212b;
}

input,
select {
  width: 100%;
  min-height: 40px;
  border: 1px solid #d9e1e8;
  border-radius: 8px;
  background: #ffffff;
  color: #18212b;
  padding: 9px 10px;
  outline: none;
}

input:focus,
select:focus {
  border-color: #155e75;
  box-shadow: 0 0 0 3px rgba(21, 94, 117, 0.12);
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
  width: min(420px, 100%);
  display: grid;
  gap: 16px;
  padding: 28px;
  border: 1px solid #d9e1e8;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(24, 33, 43, 0.07);
}

.workspace {
  min-width: 0;
  padding: 24px;
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
  color: #5f6f80;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0;
  letter-spacing: 0;
}

h1 {
  font-size: 28px;
}

h2 {
  font-size: 15px;
}

.header-actions,
.panel__head,
.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn {
  min-height: 38px;
  border: 1px solid #d9e1e8;
  border-radius: 8px;
  background: #ffffff;
  color: #18212b;
  padding: 8px 12px;
  font-weight: 800;
  cursor: pointer;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.btn--primary {
  border-color: #155e75;
  background: #155e75;
  color: #ffffff;
}

.btn--danger {
  border-color: #efcaca;
  color: #b91c1c;
}

.btn--ghost {
  background: #f7fafc;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.metric,
.panel {
  border: 1px solid #d9e1e8;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(24, 33, 43, 0.05);
}

.metric {
  min-height: 82px;
  padding: 14px;
}

.metric strong {
  display: block;
  margin-top: 8px;
  overflow-wrap: anywhere;
  font-size: 20px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.8fr);
  gap: 16px;
  align-items: start;
}

.content-grid--interventions {
  grid-template-columns: 1fr;
}

.panel {
  overflow: hidden;
}

.panel--wide {
  grid-column: span 2;
}

.panel__head {
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #edf1f5;
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 820px;
  border-collapse: collapse;
}

th,
td {
  padding: 11px 14px;
  border-bottom: 1px solid #edf1f5;
  text-align: left;
  font-size: 13px;
}

th {
  color: #5f6f80;
  font-size: 12px;
}

.mono {
  font-family: "Cascadia Mono", Consolas, monospace;
}

.pill {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  border-radius: 999px;
  padding: 4px 9px;
  background: #edf2f7;
  color: #5f6f80;
  font-size: 12px;
  font-weight: 900;
}

.pill--ok {
  background: #e8f7ee;
  color: #15803d;
}

.pill--bad {
  background: #fef2f2;
  color: #b91c1c;
}

.pill--warn {
  background: #fff7ed;
  color: #b45309;
}

.pill--info {
  background: #eef5ff;
  color: #2563eb;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  padding: 16px;
}

.status-card {
  min-height: 70px;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  background: #fbfcfe;
  padding: 11px;
}

.status-card span,
.status-card strong {
  display: block;
}

.status-card span {
  color: #5f6f80;
  font-size: 12px;
  font-weight: 800;
}

.status-card strong {
  margin-top: 7px;
  font-size: 20px;
}

.alert-list {
  display: grid;
  gap: 8px;
  padding: 16px;
}

.alert {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: start;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  background: #fbfcfe;
  padding: 10px;
}

.alert strong,
.alert span {
  display: block;
}

.alert span {
  color: #5f6f80;
  font-size: 12px;
}

.row-actions {
  align-items: stretch;
  min-width: 520px;
}

.row-actions input {
  min-width: 180px;
}

.empty {
  padding: 34px 16px;
  color: #5f6f80;
  text-align: center;
}

.error {
  margin: 0;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fef2f2;
  color: #b91c1c;
  padding: 10px;
  font-size: 13px;
}

.toast {
  position: fixed;
  right: 18px;
  bottom: 18px;
  max-width: min(420px, calc(100vw - 36px));
  margin: 0;
  border: 1px solid #d9e1e8;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(24, 33, 43, 0.12);
  padding: 12px 14px;
  font-size: 13px;
  font-weight: 800;
}

.toast--error {
  border-color: #fecaca;
  color: #b91c1c;
}

@media (max-width: 1180px) {
  .ops-app {
    grid-template-columns: 1fr;
  }

  .sidebar {
    min-height: auto;
    border-right: 0;
    border-bottom: 1px solid #dde5ed;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .panel--wide {
    grid-column: span 1;
  }

  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
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
  .status-grid {
    grid-template-columns: 1fr;
  }

  .header-actions {
    width: 100%;
  }
}
</style>
