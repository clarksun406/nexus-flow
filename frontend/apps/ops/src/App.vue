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
        <button :class="{ active: activeView === 'orders' }" type="button" @click="activeView = 'orders'">
          Orders
        </button>
        <button :class="{ active: activeView === 'payments' }" type="button" @click="activeView = 'payments'">
          Payments
        </button>
        <button :class="{ active: activeView === 'fiat' }" type="button" @click="activeView = 'fiat'">
          Fiat Ramp
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
          <h1>{{ viewTitle }}</h1>
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

      <div v-else-if="activeView === 'interventions'" class="content-grid content-grid--interventions">
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

      <div v-else-if="activeView === 'orders'" class="content-grid content-grid--interventions">
        <section class="panel">
          <div class="panel__head">
            <h2>Orders</h2>
            <div class="header-actions">
              <select v-model="orderStatusFilter">
                <option value="">All statuses</option>
                <option>WAITING_PAYMENT</option>
                <option>CONFIRMED</option>
                <option>PARTIALLY_PAID</option>
                <option>EXPIRED</option>
                <option>REFUND_PROCESSING</option>
                <option>REFUNDED</option>
                <option>REFUND_FAILED</option>
              </select>
              <input v-model="orderMerchantFilter" placeholder="merchantId" />
              <button class="btn btn--primary" type="button" @click="ordersPageNo = 0; loadOrders()" :disabled="busy">Search</button>
            </div>
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
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in orderItems" :key="order.paymentId">
                  <td class="mono">{{ order.paymentId }}</td>
                  <td class="mono">{{ order.merchantOrderNo }}</td>
                  <td><span class="pill" :class="statusClass(order.status)">{{ order.status }}</span></td>
                  <td>{{ money(order.amountFiat, order.currencyFiat) }}</td>
                  <td>{{ money(order.amountCrypto, order.currencyCrypto) }} {{ order.network || "" }}</td>
                  <td>{{ order.channelId || "-" }}</td>
                  <td>{{ formatTime(order.createTime) }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="orderItems.length === 0" class="empty">No orders match the filters</div>
          </div>
          <div class="panel__head">
            <div class="header-actions">
              <button class="btn" type="button" :disabled="ordersPageNo === 0 || busy" @click="ordersPageNo--; loadOrders()">Prev</button>
              <span class="muted">Page {{ ordersPageNo + 1 }} · {{ ordersTotal }} total</span>
              <button class="btn" type="button" :disabled="(ordersPageNo + 1) * 20 >= ordersTotal || busy" @click="ordersPageNo++; loadOrders()">Next</button>
            </div>
          </div>
        </section>
      </div>

      <div v-else-if="activeView === 'payments'" class="content-grid content-grid--interventions">
        <section class="panel">
          <div class="panel__head">
            <h2>Execution payments</h2>
            <div class="header-actions">
              <select v-model="paymentStatusFilter">
                <option value="">All statuses</option>
                <option>PENDING</option>
                <option>DETECTED</option>
                <option>CONFIRMING</option>
                <option>CONFIRMED</option>
                <option>FAILED</option>
                <option>EXPIRED</option>
              </select>
              <button class="btn btn--primary" type="button" @click="paymentsPageNo = 0; loadPayments()" :disabled="busy">Search</button>
            </div>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Payment ID</th>
                  <th>Order</th>
                  <th>Status</th>
                  <th>Expected</th>
                  <th>Received</th>
                  <th>Conf.</th>
                  <th>Address</th>
                  <th>Tx Hash</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="payment in paymentItems" :key="payment.id">
                  <td class="mono">{{ payment.id }}</td>
                  <td class="mono">{{ payment.orderId }}</td>
                  <td><span class="pill" :class="statusClass(payment.status)">{{ payment.status }}</span></td>
                  <td>{{ money(payment.expectedAmount, payment.currency) }}</td>
                  <td>{{ money(payment.receivedAmount, payment.currency) }}</td>
                  <td>{{ payment.confirmations ?? 0 }}/{{ payment.requiredConfirmations ?? "-" }}</td>
                  <td class="mono">{{ shorten(payment.receivingAddress, 18) }}</td>
                  <td class="mono">{{ shorten(payment.txHash, 18) }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="paymentItems.length === 0" class="empty">No payments match the filters</div>
          </div>
          <div class="panel__head">
            <div class="header-actions">
              <button class="btn" type="button" :disabled="paymentsPageNo === 0 || busy" @click="paymentsPageNo--; loadPayments()">Prev</button>
              <span class="muted">Page {{ paymentsPageNo + 1 }} · {{ paymentsTotal }} total</span>
              <button class="btn" type="button" :disabled="(paymentsPageNo + 1) * 20 >= paymentsTotal || busy" @click="paymentsPageNo++; loadPayments()">Next</button>
            </div>
          </div>
        </section>
      </div>

      <div v-else-if="activeView === 'fiat'" class="content-grid content-grid--interventions">
        <section class="panel">
          <div class="panel__head">
            <h2>Fiat ramp orders</h2>
            <div class="header-actions">
              <select v-model="fiatStatusFilter">
                <option value="">All statuses</option>
                <option>CREATED</option>
                <option>PENDING_PAYMENT</option>
                <option>PROCESSING</option>
                <option>COMPLETED</option>
                <option>FAILED</option>
                <option>EXPIRED</option>
                <option>CANCELLED</option>
              </select>
              <input v-model="fiatMerchantFilter" placeholder="merchantId" />
              <button class="btn btn--primary" type="button" @click="fiatPageNo = 0; loadFiatRampOrders()" :disabled="busy">Search</button>
            </div>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Ramp Order</th>
                  <th>Direction</th>
                  <th>Status</th>
                  <th>Fiat</th>
                  <th>Crypto</th>
                  <th>Provider</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in fiatItems" :key="order.rampOrderId">
                  <td class="mono">{{ order.rampOrderId }}</td>
                  <td>{{ order.direction || "-" }}</td>
                  <td><span class="pill" :class="statusClass(order.status)">{{ order.status }}</span></td>
                  <td>{{ money(order.fiatAmount, order.fiatCurrency) }}</td>
                  <td>{{ money(order.cryptoAmount, order.token) }} {{ order.network || "" }}</td>
                  <td>{{ order.providerId || "-" }}</td>
                  <td>{{ formatTime(order.createTime) }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="fiatItems.length === 0" class="empty">No fiat ramp orders match the filters</div>
          </div>
          <div class="panel__head">
            <div class="header-actions">
              <button class="btn" type="button" :disabled="fiatPageNo === 0 || busy" @click="fiatPageNo--; loadFiatRampOrders()">Prev</button>
              <span class="muted">Page {{ fiatPageNo + 1 }} · {{ fiatTotal }} total</span>
              <button class="btn" type="button" :disabled="(fiatPageNo + 1) * 20 >= fiatTotal || busy" @click="fiatPageNo++; loadFiatRampOrders()">Next</button>
            </div>
          </div>
        </section>
      </div>

      <p v-if="errorMessage" class="toast toast--error">{{ errorMessage }}</p>
      <p v-if="noticeMessage" class="toast">{{ noticeMessage }}</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { configureApiClient } from "@nexusflow/api-client";
import {
  compensateOrphan,
  ignoreDeadLetter,
  ignoreOrphan,
  listDeadLetters,
  listFiatRampOrders,
  listOrders,
  listOrphans,
  listPayments,
  loadCurrentUser,
  loadDashboard,
  login,
  logout,
  replayDeadLetter,
  resolveOrphan
} from "./opsApi";
import type {
  DeadLetterStatus,
  FiatRampListItem,
  OpsDashboard,
  OrderListItem,
  OrphanTransaction,
  PaymentListItem,
  UserInfo,
  WebhookDeadLetter
} from "./types";

type OpsView = "dashboard" | "interventions" | "orders" | "payments" | "fiat";

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

const orderItems = ref<OrderListItem[]>([]);
const ordersPageNo = ref(0);
const ordersTotal = ref(0);
const orderStatusFilter = ref("");
const orderMerchantFilter = ref("");

const paymentItems = ref<PaymentListItem[]>([]);
const paymentsPageNo = ref(0);
const paymentsTotal = ref(0);
const paymentStatusFilter = ref("");

const fiatItems = ref<FiatRampListItem[]>([]);
const fiatPageNo = ref(0);
const fiatTotal = ref(0);
const fiatStatusFilter = ref("");
const fiatMerchantFilter = ref("");

const loginForm = ref({
  email: "",
  password: ""
});

const viewTitles: Record<OpsView, string> = {
  dashboard: "Channel and order monitor",
  interventions: "Risk interventions",
  orders: "Order search",
  payments: "Execution payments",
  fiat: "Fiat ramp orders"
};
const viewTitle = computed(() => viewTitles[activeView.value]);

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

async function loadOrders() {
  await run(async () => {
    const result = await listOrders(orderStatusFilter.value, orderMerchantFilter.value.trim(), ordersPageNo.value, 20);
    orderItems.value = result.items;
    ordersTotal.value = result.total;
    notice("Orders loaded");
  });
}

async function loadPayments() {
  await run(async () => {
    const result = await listPayments(paymentStatusFilter.value, paymentsPageNo.value, 20);
    paymentItems.value = result.items;
    paymentsTotal.value = result.total;
    notice("Payments loaded");
  });
}

async function loadFiatRampOrders() {
  await run(async () => {
    const result = await listFiatRampOrders(fiatStatusFilter.value, fiatMerchantFilter.value.trim(), fiatPageNo.value, 20);
    fiatItems.value = result.items;
    fiatTotal.value = result.total;
    notice("Fiat ramp orders loaded");
  });
}

watch(activeView, (view) => {
  if (view === "orders") {
    ordersPageNo.value = 0;
    void loadOrders();
  } else if (view === "payments") {
    paymentsPageNo.value = 0;
    void loadPayments();
  } else if (view === "fiat") {
    fiatPageNo.value = 0;
    void loadFiatRampOrders();
  }
});

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

.ops-app {
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
.label,
.sub {
  display: block;
}

.brand strong {
  font-size: 15px;
  letter-spacing: -0.01em;
}

.brand span,
.label,
label,
.muted,
.sub {
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

.nav {
  display: grid;
  gap: 8px;
}

.nav button {
  min-height: 38px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: #5b6b7e;
  padding: 8px 12px;
  text-align: left;
  font-weight: 650;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.nav button:hover {
  background: #f6f8fb;
}

.nav button.active {
  border-color: rgba(79, 70, 229, 0.35);
  background: #eef2ff;
  color: #4f46e5;
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
.panel__head,
.row-actions {
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
  grid-template-columns: repeat(5, minmax(0, 1fr));
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
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.8fr);
  gap: 18px;
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
  padding: 16px 20px;
  border-bottom: 1px solid #edf1f7;
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

.pill {
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

.pill::before {
  content: "";
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: currentColor;
}

.pill--ok {
  background: #ecfdf5;
  color: #059669;
  border-color: #b5e3cd;
}

.pill--bad {
  background: #fef2f2;
  color: #dc2626;
  border-color: #f3c6c6;
}

.pill--warn {
  background: #fff8ee;
  color: #d97706;
  border-color: #f3d9ad;
}

.pill--info {
  background: #eff6ff;
  color: #2563eb;
  border-color: #c9dcf8;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  padding: 20px;
}

.status-card {
  min-height: 70px;
  border: 1px solid #edf1f7;
  border-radius: 14px;
  background: #fbfcfe;
  padding: 12px 14px;
}

.status-card span,
.status-card strong {
  display: block;
}

.status-card span {
  color: #8b99ab;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.status-card strong {
  margin-top: 7px;
  font-size: 20px;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.alert-list {
  display: grid;
  gap: 8px;
  padding: 20px;
}

.alert {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: start;
  border: 1px solid #edf1f7;
  border-radius: 14px;
  background: #fbfcfe;
  padding: 12px;
}

.alert strong,
.alert span {
  display: block;
}

.alert span {
  color: #5b6b7e;
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

@media (max-width: 1180px) {
  .ops-app {
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
