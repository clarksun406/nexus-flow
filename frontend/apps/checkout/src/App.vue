<template>
  <main class="checkout-app">
    <header class="topbar">
      <div class="brand">
        <span class="brand__mark" aria-hidden="true"></span>
        <div>
          <strong>NexusFlow Checkout</strong>
          <span>Hosted crypto payment</span>
        </div>
      </div>
      <form class="load-form" @submit.prevent="loadOrder">
        <input v-model="apiBaseInput" placeholder="/api" @blur="saveSettings" />
        <input v-model="paymentIdInput" class="mono" placeholder="payment_id" />
        <button class="btn btn--primary" type="submit" :disabled="busy">Load</button>
        <button class="btn" type="button" @click="togglePolling">
          {{ polling ? "Pause" : "Resume" }}
        </button>
      </form>
    </header>

    <section class="wrap">
      <aside class="summary">
        <section class="panel">
          <div class="panel__head">
            <h1>Order</h1>
            <span class="pill" :class="statusClass(order?.status)">{{ statusLabel(order?.status) }}</span>
          </div>
          <div class="panel__body">
            <div class="amount">
              <strong>{{ order?.amountCrypto || "-" }}</strong>
              <span>{{ order?.currencyCrypto || submitForm.token }}</span>
            </div>
            <p class="subtle">{{ money(order?.amountFiat, order?.currencyFiat) }}</p>
            <div class="kv-grid">
              <div><span>Payment ID</span><strong class="mono">{{ paymentId || "-" }}</strong></div>
              <div><span>Channel</span><strong>{{ order?.channelName || order?.channelId || "-" }}</strong></div>
              <div><span>Network</span><strong>{{ order?.network || submitForm.network }}</strong></div>
              <div><span>Expires</span><strong>{{ formatTime(order?.expireTime) }}</strong></div>
              <div><span>Countdown</span><strong>{{ countdown }}</strong></div>
              <div><span>Paid</span><strong>{{ money(order?.paidAmountCrypto || "0", order?.currencyCrypto) }}</strong></div>
              <div><span>Confirmations</span><strong>{{ order?.transactionCount ?? 0 }}/{{ order?.requiredConfirmations ?? 0 }}</strong></div>
              <div><span>Tx Hash</span><strong class="mono">{{ order?.txHash || "-" }}</strong></div>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <h2>Progress</h2>
            <span class="muted">{{ polling ? "Polling every 5s" : "Paused" }}</span>
          </div>
          <div class="timeline">
            <div v-for="step in steps" :key="step.key" class="step" :class="{ active: step.active, done: step.done }">
              <span>{{ step.index }}</span>
              <div>
                <strong>{{ step.title }}</strong>
                <small>{{ step.body }}</small>
              </div>
            </div>
          </div>
        </section>
      </aside>

      <section class="payment">
        <section class="panel">
          <div class="panel__head">
            <h2>Payment address</h2>
            <span class="muted">{{ lastUpdated }}</span>
          </div>
          <div class="pay-grid">
            <div class="pay-details">
              <div class="copy-box">
                <div class="copy-box__head">
                  <span>Deposit address</span>
                  <button class="btn btn--small" type="button" :disabled="!order?.payAddress" @click="copy(order?.payAddress)">
                    Copy
                  </button>
                </div>
                <strong class="mono">{{ order?.payAddress || "Generate a payment address after loading an order." }}</strong>
              </div>

              <div class="copy-box">
                <div class="copy-box__head">
                  <span>Memo / tag</span>
                  <button class="btn btn--small" type="button" :disabled="!order?.memo" @click="copy(order?.memo)">
                    Copy
                  </button>
                </div>
                <strong class="mono">{{ order?.memo || "-" }}</strong>
              </div>

              <form class="submit-grid" @submit.prevent="submitPayment">
                <label>
                  Token
                  <input v-model="submitForm.token" required />
                </label>
                <label>
                  Network
                  <select v-model="submitForm.network">
                    <option>TRC20</option>
                    <option>ERC20</option>
                    <option>BEP20</option>
                    <option>BTC</option>
                  </select>
                </label>
                <label>
                  Channel
                  <input v-model="submitForm.channelId" placeholder="Auto" />
                </label>
                <button class="btn btn--accent" type="submit" :disabled="!canSubmitPayment">
                  Generate Address
                </button>
              </form>

              <div class="actions">
                <button class="btn" type="button" :disabled="!paymentId || busy" @click="loadOrder">Refresh now</button>
                <button class="btn" type="button" :disabled="!paymentId" @click="copy(paymentId)">Copy payment ID</button>
              </div>

              <p class="notice" :class="noticeTone">{{ noticeText }}</p>
            </div>

            <div class="qr-panel">
              <canvas ref="qrCanvas" width="260" height="260" :hidden="!order?.payAddress"></canvas>
              <div v-if="!order?.payAddress" class="qr-empty">QR code appears after address generation.</div>
              <span class="muted">Address QR</span>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <h2>Request log</h2>
            <button class="btn btn--small" type="button" @click="logs = []">Clear</button>
          </div>
          <pre class="log">{{ logs.length ? logs.join("\n\n") : "No requests yet." }}</pre>
        </section>
      </section>

      <p v-if="errorMessage" class="toast toast--error">{{ errorMessage }}</p>
      <p v-if="noticeMessage" class="toast">{{ noticeMessage }}</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { configureApiClient } from "@nexusflow/api-client";
import { getCashierStatus, submitCashierPayment } from "./checkoutApi";
import { drawQrToCanvas } from "./qr";
import type { CashierStatus, CashierSubmitResponse } from "./types";

const settingsKey = "nexusflow.checkout.config";
const pollMs = 5000;
const terminalStatuses = new Set(["CONFIRMED", "EXPIRED", "CANCELLED", "REFUNDED", "REFUND_FAILED"]);
const payableStatuses = new Set(["WAITING_PAYMENT", "PARTIALLY_PAID"]);

const apiBaseInput = ref("");
const paymentIdInput = ref("");
const paymentId = ref("");
const order = ref<CashierStatus | null>(null);
const busy = ref(false);
const polling = ref(true);
const countdown = ref("-");
const lastUpdated = ref("Never refreshed");
const errorMessage = ref("");
const noticeMessage = ref("");
const logs = ref<string[]>([]);
const qrCanvas = ref<HTMLCanvasElement | null>(null);
let pollTimer: number | undefined;
let countdownTimer: number | undefined;

const submitForm = ref({
  token: "USDT",
  network: "TRC20",
  channelId: ""
});

const canSubmitPayment = computed(() => {
  const status = order.value?.status;
  return Boolean(paymentId.value)
    && !busy.value
    && !order.value?.payAddress
    && (!status || payableStatuses.has(status));
});

const noticeText = computed(() => {
  if (!order.value) {
    return "Load a payment order from the URL or by entering a payment ID.";
  }
  if (terminalStatuses.has(order.value.status)) {
    return order.value.status === "CONFIRMED"
      ? "Payment is confirmed. No further action is required."
      : "This order can no longer accept payment.";
  }
  if (order.value.payAddress) {
    return "Send the exact amount on the selected network. This page will keep polling for updates.";
  }
  return "Generate a deposit address before sending funds.";
});

const noticeTone = computed(() => {
  if (!order.value) {
    return "notice--idle";
  }
  if (order.value.status === "CONFIRMED") {
    return "notice--ok";
  }
  if (terminalStatuses.has(order.value.status)) {
    return "notice--bad";
  }
  return "notice--wait";
});

const steps = computed(() => {
  const status = order.value?.status ?? "";
  const hasAddress = Boolean(order.value?.payAddress);
  const detected = status === "PARTIALLY_PAID" || status === "CONFIRMED" || Boolean(order.value?.txHash);
  const confirmed = status === "CONFIRMED";
  return [
    { key: "loaded", index: 1, title: "Order loaded", body: "Amount and asset are available.", done: Boolean(order.value), active: Boolean(order.value) && !hasAddress },
    { key: "address", index: 2, title: "Address generated", body: "Buyer can transfer to the deposit address.", done: hasAddress, active: hasAddress && !detected },
    { key: "detected", index: 3, title: "Payment detected", body: "Funds are partially or fully detected.", done: detected, active: detected && !confirmed },
    { key: "confirmed", index: 4, title: "Confirmed", body: "Required confirmations are complete.", done: confirmed, active: confirmed }
  ];
});

onMounted(() => {
  loadSettings();
  const params = new URLSearchParams(window.location.search);
  const fromUrl = params.get("payment_id") || params.get("paymentId") || params.get("id");
  if (fromUrl) {
    paymentIdInput.value = fromUrl;
    void loadOrder();
  }
  countdownTimer = window.setInterval(updateCountdown, 1000);
});

onBeforeUnmount(() => {
  stopPolling();
  if (countdownTimer) {
    window.clearInterval(countdownTimer);
  }
});

watch(() => order.value?.payAddress, async (address) => {
  await nextTick();
  if (address && qrCanvas.value) {
    try {
      drawQrToCanvas(address, qrCanvas.value);
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : "Unable to draw QR";
    }
  }
});

async function loadOrder() {
  paymentId.value = paymentIdInput.value.trim();
  if (!paymentId.value) {
    errorMessage.value = "payment_id is required";
    return;
  }
  await run(async () => {
    saveSettings();
    log(`GET /cashier/order/status?paymentId=${paymentId.value}`);
    const data = await getCashierStatus(paymentId.value);
    applyStatus(data);
    schedulePolling();
  });
}

async function submitPayment() {
  if (!paymentId.value) {
    errorMessage.value = "Load an order first";
    return;
  }
  await run(async () => {
    const request = {
      paymentId: paymentId.value,
      token: submitForm.value.token.trim().toUpperCase(),
      network: submitForm.value.network.trim().toUpperCase(),
      channelId: submitForm.value.channelId.trim() || undefined
    };
    log(`POST /cashier/pay/submit\n${JSON.stringify(request, null, 2)}`);
    const data = await submitCashierPayment(request);
    applySubmit(data);
    schedulePolling();
    notice("Payment address generated");
  });
}

function applyStatus(data: CashierStatus) {
  order.value = data;
  hydrateSubmitForm(data);
  lastUpdated.value = `Updated ${new Date().toLocaleTimeString()}`;
  updateCountdown();
  if (terminalStatuses.has(data.status)) {
    stopPolling();
  }
}

function applySubmit(data: CashierSubmitResponse) {
  order.value = {
    ...(order.value ?? { paymentId: data.paymentId, status: data.status ?? "WAITING_PAYMENT" }),
    paymentId: data.paymentId,
    status: data.status === "WAITING" ? "WAITING_PAYMENT" : data.status ?? order.value?.status ?? "WAITING_PAYMENT",
    currencyCrypto: data.token ?? submitForm.value.token,
    amountCrypto: data.cryptoAmount,
    amountFiat: data.fiatAmount,
    currencyFiat: data.fiatCurrency,
    network: data.network ?? submitForm.value.network,
    exchangeRate: data.exchangeRate,
    channelId: data.channelId,
    payAddress: data.payAddress,
    memo: data.memo,
    requiredConfirmations: data.requiredConfirmations,
    expireTime: data.expireTime
  };
  hydrateSubmitForm(order.value);
  lastUpdated.value = `Updated ${new Date().toLocaleTimeString()}`;
  updateCountdown();
}

function hydrateSubmitForm(data: CashierStatus) {
  if (data.currencyCrypto) {
    submitForm.value.token = data.currencyCrypto;
  }
  if (data.network) {
    submitForm.value.network = data.network.toUpperCase();
  }
  if (data.channelId) {
    submitForm.value.channelId = data.channelId;
  }
}

function schedulePolling() {
  stopPolling();
  if (!polling.value || !order.value || terminalStatuses.has(order.value.status)) {
    return;
  }
  pollTimer = window.setTimeout(() => void loadOrder(), pollMs);
}

function stopPolling() {
  if (pollTimer) {
    window.clearTimeout(pollTimer);
  }
  pollTimer = undefined;
}

function togglePolling() {
  polling.value = !polling.value;
  if (polling.value) {
    notice("Polling resumed");
    void loadOrder();
  } else {
    stopPolling();
    notice("Polling paused");
  }
}

function updateCountdown() {
  const expireTime = order.value?.expireTime;
  if (!expireTime) {
    countdown.value = "-";
    return;
  }
  const diff = expireTime - Date.now();
  if (diff <= 0) {
    countdown.value = "Expired";
    return;
  }
  const minutes = Math.floor(diff / 60000);
  const seconds = Math.floor((diff % 60000) / 1000);
  countdown.value = `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

async function copy(value?: string) {
  if (!value) {
    return;
  }
  try {
    await navigator.clipboard.writeText(value);
    notice("Copied");
  } catch {
    errorMessage.value = "Copy failed";
  }
}

async function run(action: () => Promise<void>) {
  busy.value = true;
  errorMessage.value = "";
  try {
    await action();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "Request failed";
  } finally {
    busy.value = false;
  }
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

function statusClass(status?: string) {
  if (status === "CONFIRMED") {
    return "pill--done";
  }
  if (status === "PARTIALLY_PAID") {
    return "pill--warn";
  }
  if (status === "WAITING_PAYMENT" || status === "WAITING") {
    return "pill--wait";
  }
  if (status && terminalStatuses.has(status)) {
    return "pill--bad";
  }
  return "pill--idle";
}

function statusLabel(status?: string) {
  return status ? status.replace(/_/g, " ") : "Not loaded";
}

function money(amount?: string, currency?: string) {
  return amount ? `${amount} ${currency ?? ""}`.trim() : "-";
}

function formatTime(value?: number) {
  return value ? new Date(value).toLocaleString() : "-";
}

function log(message: string) {
  logs.value = [`[${new Date().toLocaleTimeString()}] ${message}`, ...logs.value].slice(0, 30);
}

function notice(message: string) {
  noticeMessage.value = message;
  window.setTimeout(() => {
    if (noticeMessage.value === message) {
      noticeMessage.value = "";
    }
  }, 2200);
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
  color: #17202a;
  font-family: "Aptos", "Segoe UI", sans-serif;
}

button,
input,
select {
  font: inherit;
}

.checkout-app {
  min-height: 100vh;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #dce3ea;
  background: rgba(255, 255, 255, 0.94);
  padding: 14px 24px;
  backdrop-filter: blur(10px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 220px;
}

.brand__mark {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: linear-gradient(135deg, #0f766e, #2563eb);
}

.brand strong,
.brand span,
.subtle,
.muted,
.copy-box__head span,
.kv-grid span {
  display: block;
}

.brand strong {
  font-size: 15px;
}

.brand span,
.subtle,
.muted,
.copy-box__head span,
label,
.kv-grid span {
  color: #617080;
  font-size: 12px;
  font-weight: 800;
}

.load-form {
  display: grid;
  grid-template-columns: minmax(180px, 300px) minmax(220px, 360px) auto auto;
  gap: 8px;
  align-items: center;
  min-width: min(820px, 100%);
}

input,
select {
  width: 100%;
  min-height: 40px;
  border: 1px solid #d9e1e8;
  border-radius: 8px;
  background: #ffffff;
  color: #17202a;
  padding: 9px 10px;
  outline: none;
}

input:focus,
select:focus {
  border-color: #0f766e;
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.12);
}

.wrap {
  max-width: 1280px;
  display: grid;
  grid-template-columns: minmax(300px, 390px) minmax(0, 1fr);
  gap: 16px;
  margin: 0 auto;
  padding: 20px 24px 34px;
}

.summary,
.payment {
  display: grid;
  gap: 16px;
  align-content: start;
}

.panel {
  overflow: hidden;
  border: 1px solid #d9e1e8;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(23, 32, 42, 0.07);
}

.panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #edf1f5;
  padding: 14px 16px;
}

h1,
h2 {
  margin: 0;
  letter-spacing: 0;
}

h1,
h2 {
  font-size: 15px;
}

.panel__body {
  display: grid;
  gap: 16px;
  padding: 16px;
}

.amount {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 10px;
}

.amount strong {
  font-size: clamp(32px, 5vw, 52px);
  line-height: 1;
}

.amount span {
  color: #617080;
  font-weight: 900;
}

.kv-grid {
  display: grid;
  gap: 1px;
  overflow: hidden;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  background: #edf1f5;
}

.kv-grid > div {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  min-height: 42px;
  background: #ffffff;
  padding: 9px 10px;
  font-size: 13px;
}

.kv-grid strong {
  min-width: 0;
  overflow-wrap: anywhere;
}

.pay-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 16px;
  padding: 16px;
}

.pay-details {
  display: grid;
  gap: 12px;
}

.copy-box {
  display: grid;
  gap: 10px;
  border: 1px solid #d9e1e8;
  border-radius: 8px;
  background: #ffffff;
  padding: 12px;
}

.copy-box__head,
.actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.copy-box strong {
  min-height: 28px;
  overflow-wrap: anywhere;
  font-size: 14px;
}

.submit-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  align-items: end;
}

label {
  display: grid;
  gap: 6px;
}

.qr-panel {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 280px;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  background: #fbfcfe;
  padding: 14px;
}

canvas {
  width: 220px;
  height: 220px;
  border: 10px solid #ffffff;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(23, 32, 42, 0.08);
  image-rendering: pixelated;
}

.qr-empty {
  width: 220px;
  height: 220px;
  display: grid;
  place-items: center;
  border: 1px dashed #cbd5df;
  border-radius: 8px;
  background: #ffffff;
  color: #617080;
  padding: 18px;
  text-align: center;
}

.timeline {
  display: grid;
  gap: 10px;
  padding: 16px;
}

.step {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
}

.step > span {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border: 1px solid #d9e1e8;
  border-radius: 999px;
  background: #ffffff;
  color: #617080;
  font-size: 12px;
  font-weight: 900;
}

.step.active > span {
  border-color: #0f766e;
  background: #0f766e;
  color: #ffffff;
}

.step.done > span {
  border-color: #15803d;
  background: #15803d;
  color: #ffffff;
}

.step strong,
.step small {
  display: block;
}

.step small {
  margin-top: 2px;
  color: #617080;
}

.notice {
  margin: 0;
  border: 1px solid #f1d4ae;
  border-radius: 8px;
  background: #fff7ed;
  color: #7c2d12;
  padding: 12px;
  font-size: 13px;
}

.notice--ok {
  border-color: #bfe3c8;
  background: #f0fdf4;
  color: #14532d;
}

.notice--bad {
  border-color: #efcaca;
  background: #fff5f5;
  color: #7f1d1d;
}

.log {
  max-height: 180px;
  overflow: auto;
  margin: 0;
  border-radius: 8px;
  background: #fbfcfe;
  padding: 12px;
  color: #617080;
  white-space: pre-wrap;
}

.btn {
  min-height: 40px;
  border: 1px solid #d9e1e8;
  border-radius: 8px;
  background: #ffffff;
  color: #17202a;
  padding: 8px 12px;
  font-weight: 900;
  cursor: pointer;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.btn--primary {
  border-color: #0f766e;
  background: #0f766e;
  color: #ffffff;
}

.btn--accent {
  border-color: #2563eb;
  background: #2563eb;
  color: #ffffff;
}

.btn--small {
  min-height: 30px;
  padding: 5px 8px;
  font-size: 12px;
}

.pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  border: 1px solid #d9e1e8;
  border-radius: 999px;
  background: #ffffff;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 900;
}

.pill--wait {
  border-color: #c9d9f5;
  background: #f2f7ff;
  color: #2563eb;
}

.pill--warn {
  border-color: #f1d4ae;
  background: #fff7ed;
  color: #b45309;
}

.pill--done {
  border-color: #bfe3c8;
  background: #f0fdf4;
  color: #15803d;
}

.pill--bad {
  border-color: #efcaca;
  background: #fff5f5;
  color: #b91c1c;
}

.pill--idle {
  background: #f8fafc;
  color: #617080;
}

.mono {
  font-family: "Cascadia Mono", Consolas, monospace;
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
  box-shadow: 0 14px 34px rgba(23, 32, 42, 0.12);
  padding: 12px 14px;
  font-size: 13px;
  font-weight: 800;
}

.toast--error {
  border-color: #fecaca;
  color: #b91c1c;
}

@media (max-width: 1000px) {
  .topbar {
    position: static;
    align-items: stretch;
    flex-direction: column;
  }

  .load-form,
  .wrap,
  .pay-grid {
    grid-template-columns: 1fr;
  }

  .submit-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .wrap {
    padding: 14px;
  }

  .submit-grid,
  .kv-grid > div {
    grid-template-columns: 1fr;
  }

  .actions,
  .copy-box__head {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
