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
        <label class="label" for="apiBase">API Base</label>
        <input id="apiBase" v-model="apiBaseInput" placeholder="/api" @blur="saveSettings" />
      </section>

      <section v-if="user" class="side-section">
        <span class="label">Signed in</span>
        <strong class="user-line">{{ user.email || user.displayName || user.userId }}</strong>
        <div class="side-actions">
          <button class="btn btn--sm" type="button" @click="refreshSession" :disabled="busy">Refresh session</button>
          <button class="btn btn--sm" type="button" @click="signOut">Sign out</button>
        </div>
      </section>

      <nav v-if="user" class="nav" aria-label="Ops sections">
        <span class="label">Views</span>
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
        <div class="login-brand">
          <span class="brand__mark brand__mark--lg" aria-hidden="true"></span>
          <span class="login-brand__name">NexusFlow <em>Ops</em></span>
        </div>
        <div>
          <p class="eyebrow">Internal access</p>
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
          {{ busy ? "Signing in" : "Sign in" }}
        </button>
      </form>
    </section>

    <section v-else class="workspace">
      <div v-if="busy" class="busy-bar" aria-hidden="true"></div>

      <header class="workspace__header">
        <div>
          <p class="eyebrow">Operations</p>
          <h1>{{ viewTitle }}</h1>
        </div>
        <div class="header-actions">
          <button class="btn btn--primary" type="button" @click="refreshCurrentView" :disabled="busy">
            {{ activeView === "dashboard" ? "Refresh" : "Refresh list" }}
          </button>
        </div>
      </header>

      <section v-if="activeView === 'dashboard'" class="metrics" aria-label="ops metrics">
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
            <table class="table--narrow">
              <thead>
                <tr>
                  <th>Channel</th>
                  <th>Status</th>
                  <th class="num">Currencies</th>
                  <th>Message</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="channel in channels" :key="channel.channelId">
                  <td>
                    <strong>{{ channel.displayName || channel.channelId }}</strong>
                    <span class="sub mono">{{ channel.channelId }}</span>
                  </td>
                  <td>
                    <span class="pill" :class="statusTone(channel.status)" :title="channel.status">
                      {{ statusLabel(channel.status) }}
                    </span>
                  </td>
                  <td class="num mono">{{ channel.supportedCurrencyCount }}</td>
                  <td class="dim">{{ channel.message || "-" }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="channels.length === 0" class="empty">
              <strong>No channel data loaded</strong>
              <span>Press Refresh to pull the latest channel monitor.</span>
            </div>
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
              <strong class="mono">{{ alert.count }}</strong>
            </div>
            <div v-if="alerts.length === 0" class="empty">
              <strong>No active alerts</strong>
              <span>Channels and reconciliation are reporting clean.</span>
            </div>
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
                  <th class="num">Fiat</th>
                  <th class="num">Crypto</th>
                  <th>Channel</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in recentOrders" :key="order.paymentId">
                  <td class="mono copyable" :title="order.paymentId" @click="copyValue(order.paymentId)">
                    {{ shorten(order.paymentId, 18) }}
                  </td>
                  <td class="mono copyable" :title="order.merchantOrderNo" @click="copyValue(order.merchantOrderNo)">
                    {{ shorten(order.merchantOrderNo, 18) }}
                  </td>
                  <td>
                    <span class="pill" :class="statusTone(order.status)" :title="order.status">
                      {{ statusLabel(order.status) }}
                    </span>
                  </td>
                  <td class="num mono">{{ money(order.amountFiat, order.currencyFiat) }}</td>
                  <td class="num mono">
                    {{ money(order.amountCrypto, order.currencyCrypto) }}
                    <span v-if="order.network" class="net">{{ order.network }}</span>
                  </td>
                  <td>{{ order.channelId || "-" }}</td>
                  <td class="mono dim">{{ formatTime(order.updateTime) }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="recentOrders.length === 0" class="empty">
              <strong>No recent orders</strong>
              <span>New merchant orders will appear here as they come in.</span>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head"><h2>Order status</h2></div>
          <div class="status-grid">
            <div v-for="[name, value] in orderStatusRows" :key="name" class="status-card">
              <span>{{ statusLabel(name) }}</span>
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

      <div v-else-if="activeView === 'orders'" class="content-grid content-grid--interventions">
        <section class="panel">
          <form class="toolbar" @submit.prevent="ordersPageNo = 0; loadOrders()">
            <label class="toolbar__field">
              <span>Status</span>
              <select v-model="orderStatusFilter">
                <option value="">All statuses</option>
                <option v-for="status in ORDER_STATUSES" :key="status" :value="status">{{ statusLabel(status) }}</option>
              </select>
            </label>
            <label class="toolbar__field toolbar__field--grow">
              <span>Merchant ID</span>
              <input v-model="orderMerchantFilter" placeholder="Any merchant" />
            </label>
            <div class="toolbar__actions">
              <button class="btn btn--primary" type="submit" :disabled="busy">Search</button>
              <button class="btn" type="button" :disabled="busy" @click="clearOrderFilters">Clear</button>
            </div>
          </form>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Payment ID</th>
                  <th>Merchant Order</th>
                  <th>Status</th>
                  <th class="num">Fiat</th>
                  <th class="num">Crypto</th>
                  <th>Channel</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in orderItems" :key="order.paymentId">
                  <td class="mono copyable" :title="order.paymentId" @click="copyValue(order.paymentId)">
                    {{ shorten(order.paymentId, 18) }}
                  </td>
                  <td class="mono copyable" :title="order.merchantOrderNo" @click="copyValue(order.merchantOrderNo)">
                    {{ shorten(order.merchantOrderNo, 18) }}
                  </td>
                  <td>
                    <span class="pill" :class="statusTone(order.status)" :title="order.status">
                      {{ statusLabel(order.status) }}
                    </span>
                  </td>
                  <td class="num mono">{{ money(order.amountFiat, order.currencyFiat) }}</td>
                  <td class="num mono">
                    {{ money(order.amountCrypto, order.currencyCrypto) }}
                    <span v-if="order.network" class="net">{{ order.network }}</span>
                  </td>
                  <td>{{ order.channelId || "-" }}</td>
                  <td class="mono dim">{{ formatTime(order.createTime) }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="orderItems.length === 0" class="empty">
              <strong>No orders match these filters</strong>
              <span>Try another status or merchant ID, or clear the filters.</span>
              <button class="btn btn--sm" type="button" @click="clearOrderFilters">Clear filters</button>
            </div>
          </div>
          <div class="panel__foot">
            <span class="foot-note">{{ rangeLabel(ordersPageNo, ordersTotal) }}</span>
            <div class="pager">
              <button class="btn btn--sm" type="button" :disabled="ordersPageNo === 0 || busy" @click="ordersPageNo--; loadOrders()">
                Prev
              </button>
              <span class="pager__page mono">Page {{ ordersPageNo + 1 }} / {{ totalPages(ordersTotal) }}</span>
              <button
                class="btn btn--sm"
                type="button"
                :disabled="!ordersTotal || (ordersPageNo + 1) * PAGE_SIZE >= ordersTotal || busy"
                @click="ordersPageNo++; loadOrders()"
              >
                Next
              </button>
            </div>
          </div>
        </section>
      </div>

      <div v-else-if="activeView === 'payments'" class="content-grid content-grid--interventions">
        <section class="panel">
          <form class="toolbar" @submit.prevent="paymentsPageNo = 0; loadPayments()">
            <label class="toolbar__field">
              <span>Status</span>
              <select v-model="paymentStatusFilter">
                <option value="">All statuses</option>
                <option v-for="status in PAYMENT_STATUSES" :key="status" :value="status">{{ statusLabel(status) }}</option>
              </select>
            </label>
            <div class="toolbar__actions">
              <button class="btn btn--primary" type="submit" :disabled="busy">Search</button>
              <button class="btn" type="button" :disabled="busy" @click="clearPaymentFilters">Clear</button>
            </div>
          </form>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Payment ID</th>
                  <th>Order</th>
                  <th>Status</th>
                  <th class="num">Expected</th>
                  <th class="num">Received</th>
                  <th>Confirmations</th>
                  <th>Address</th>
                  <th>Tx Hash</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="payment in paymentItems" :key="payment.id">
                  <td class="mono copyable" :title="payment.id" @click="copyValue(payment.id)">
                    {{ shorten(payment.id, 14) }}
                  </td>
                  <td class="mono copyable" :title="payment.orderId" @click="copyValue(payment.orderId)">
                    {{ shorten(payment.orderId, 14) }}
                  </td>
                  <td>
                    <span class="pill" :class="statusTone(payment.status)" :title="payment.status">
                      {{ statusLabel(payment.status) }}
                    </span>
                    <span v-if="payment.lastFailureReason" class="sub fail-reason">{{ payment.lastFailureReason }}</span>
                  </td>
                  <td class="num mono">
                    <template v-if="payment.expectedAmount">
                      {{ money(payment.expectedAmount) }}
                      <span v-if="currencyParts(payment.currency).code" class="cur">{{ currencyParts(payment.currency).code }}</span>
                      <span v-if="currencyParts(payment.currency).net" class="net">{{ currencyParts(payment.currency).net }}</span>
                    </template>
                    <span v-else class="dim">-</span>
                  </td>
                  <td class="num mono">
                    <template v-if="payment.receivedAmount">
                      {{ money(payment.receivedAmount) }}
                      <span v-if="currencyParts(payment.currency).code" class="cur">{{ currencyParts(payment.currency).code }}</span>
                    </template>
                    <span v-else class="dim">-</span>
                  </td>
                  <td>
                    <div
                      v-if="payment.requiredConfirmations"
                      class="meter"
                      :title="`${payment.confirmations ?? 0} of ${payment.requiredConfirmations} confirmations`"
                    >
                      <span
                        v-for="seg in meterSegments(payment.requiredConfirmations)"
                        :key="seg"
                        class="meter__seg"
                        :class="{ 'meter__seg--on': seg <= meterFilled(payment.confirmations, payment.requiredConfirmations) }"
                      ></span>
                      <span class="meter__label mono">{{ payment.confirmations ?? 0 }}/{{ payment.requiredConfirmations }}</span>
                    </div>
                    <span v-else class="dim">-</span>
                  </td>
                  <td class="mono copyable" :title="payment.receivingAddress" @click="copyValue(payment.receivingAddress)">
                    {{ shorten(payment.receivingAddress, 12) }}
                  </td>
                  <td class="mono copyable" :title="payment.txHash" @click="copyValue(payment.txHash)">
                    {{ shorten(payment.txHash, 12) }}
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="paymentItems.length === 0" class="empty">
              <strong>No payments match these filters</strong>
              <span>Try another status, or clear the filters to see all execution payments.</span>
              <button class="btn btn--sm" type="button" @click="clearPaymentFilters">Clear filters</button>
            </div>
          </div>
          <div class="panel__foot">
            <span class="foot-note">{{ rangeLabel(paymentsPageNo, paymentsTotal) }}</span>
            <div class="pager">
              <button class="btn btn--sm" type="button" :disabled="paymentsPageNo === 0 || busy" @click="paymentsPageNo--; loadPayments()">
                Prev
              </button>
              <span class="pager__page mono">Page {{ paymentsPageNo + 1 }} / {{ totalPages(paymentsTotal) }}</span>
              <button
                class="btn btn--sm"
                type="button"
                :disabled="!paymentsTotal || (paymentsPageNo + 1) * PAGE_SIZE >= paymentsTotal || busy"
                @click="paymentsPageNo++; loadPayments()"
              >
                Next
              </button>
            </div>
          </div>
        </section>
      </div>

      <div v-else-if="activeView === 'fiat'" class="content-grid content-grid--interventions">
        <section class="panel">
          <form class="toolbar" @submit.prevent="fiatPageNo = 0; loadFiatRampOrders()">
            <label class="toolbar__field">
              <span>Status</span>
              <select v-model="fiatStatusFilter">
                <option value="">All statuses</option>
                <option v-for="status in FIAT_STATUSES" :key="status" :value="status">{{ statusLabel(status) }}</option>
              </select>
            </label>
            <label class="toolbar__field toolbar__field--grow">
              <span>Merchant ID</span>
              <input v-model="fiatMerchantFilter" placeholder="Any merchant" />
            </label>
            <div class="toolbar__actions">
              <button class="btn btn--primary" type="submit" :disabled="busy">Search</button>
              <button class="btn" type="button" :disabled="busy" @click="clearFiatFilters">Clear</button>
            </div>
          </form>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Ramp Order</th>
                  <th>Direction</th>
                  <th>Status</th>
                  <th class="num">Fiat</th>
                  <th class="num">Crypto</th>
                  <th>Provider</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in fiatItems" :key="order.rampOrderId">
                  <td class="mono copyable" :title="order.rampOrderId" @click="copyValue(order.rampOrderId)">
                    {{ shorten(order.rampOrderId, 18) }}
                  </td>
                  <td>
                    <span v-if="order.direction" class="dir" :class="directionTone(order.direction)">
                      {{ directionLabel(order.direction) }}
                    </span>
                    <span v-else class="dim">-</span>
                  </td>
                  <td>
                    <span class="pill" :class="statusTone(order.status)" :title="order.status">
                      {{ statusLabel(order.status) }}
                    </span>
                  </td>
                  <td class="num mono">{{ money(order.fiatAmount, order.fiatCurrency) }}</td>
                  <td class="num mono">
                    {{ money(order.cryptoAmount, order.token) }}
                    <span v-if="order.network" class="net">{{ order.network }}</span>
                  </td>
                  <td>{{ order.providerId || "-" }}</td>
                  <td class="mono dim">{{ formatTime(order.createTime) }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="fiatItems.length === 0" class="empty">
              <strong>No fiat ramp orders match these filters</strong>
              <span>Try another status or merchant ID, or clear the filters.</span>
              <button class="btn btn--sm" type="button" @click="clearFiatFilters">Clear filters</button>
            </div>
          </div>
          <div class="panel__foot">
            <span class="foot-note">{{ rangeLabel(fiatPageNo, fiatTotal) }}</span>
            <div class="pager">
              <button class="btn btn--sm" type="button" :disabled="fiatPageNo === 0 || busy" @click="fiatPageNo--; loadFiatRampOrders()">
                Prev
              </button>
              <span class="pager__page mono">Page {{ fiatPageNo + 1 }} / {{ totalPages(fiatTotal) }}</span>
              <button
                class="btn btn--sm"
                type="button"
                :disabled="!fiatTotal || (fiatPageNo + 1) * PAGE_SIZE >= fiatTotal || busy"
                @click="fiatPageNo++; loadFiatRampOrders()"
              >
                Next
              </button>
            </div>
          </div>
        </section>
      </div>

      <div v-else-if="activeView === 'interventions'" class="content-grid content-grid--interventions">
        <section class="panel">
          <div class="panel__head">
            <h2>Orphan transactions</h2>
            <div class="header-actions">
              <select v-model="orphanStatus" aria-label="Orphan status">
                <option>UNMATCHED</option>
                <option>RESOLVED</option>
                <option>IGNORED</option>
                <option>COMPENSATED</option>
              </select>
              <button class="btn btn--primary" type="button" @click="loadOrphans()" :disabled="busy">Load</button>
            </div>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Chain</th>
                  <th>Tx Hash</th>
                  <th class="num">Amount</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="orphan in orphans" :key="`${orphan.chain}-${orphan.txHash}`">
                  <td>
                    <span class="net">{{ orphan.chain }}</span>
                  </td>
                  <td class="mono copyable" :title="orphan.txHash" @click="copyValue(orphan.txHash)">
                    {{ shorten(orphan.txHash, 20) }}
                  </td>
                  <td class="num mono">{{ money(orphan.amount, orphan.currency) }}</td>
                  <td>
                    <span class="pill" :class="statusTone(orphan.status)" :title="orphan.status">
                      {{ statusLabel(orphan.status) }}
                    </span>
                  </td>
                  <td>
                    <div class="row-actions">
                      <input
                        v-model="resolvePaymentIds[orphanKey(orphan)]"
                        placeholder="paymentId"
                        aria-label="Payment ID to resolve against"
                      />
                      <button class="btn btn--primary" type="button" @click="submitResolve(orphan)">Resolve</button>
                      <button class="btn" type="button" @click="submitCompensate(orphan)">Compensate</button>
                      <button class="btn btn--danger" type="button" @click="submitIgnore(orphan)">Ignore</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="orphans.length === 0" class="empty">
              <strong>No orphan transactions loaded</strong>
              <span>Pick a status and press Load, or wait for the next on-chain scan.</span>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <h2>Webhook dead letters</h2>
            <div class="header-actions">
              <select v-model="deadLetterStatus" aria-label="Dead letter status">
                <option value="PENDING">Pending</option>
                <option value="REPLAYED">Replayed</option>
                <option value="IGNORED">Ignored</option>
              </select>
              <button class="btn btn--primary" type="button" @click="loadDeadLetters()" :disabled="busy">Load</button>
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
                  <th class="num">Attempts</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="letter in deadLetters" :key="letter.id">
                  <td>{{ letter.deliveryType || "-" }}</td>
                  <td class="mono copyable" :title="letter.targetUrl" @click="copyValue(letter.targetUrl)">
                    {{ shorten(letter.targetUrl, 42) }}
                  </td>
                  <td>
                    <span class="mono">{{ letter.eventType || "-" }}</span>
                    <span class="sub">{{ letter.paymentId || letter.orderId || letter.eventId || "" }}</span>
                  </td>
                  <td>
                    <span class="pill" :class="statusTone(letter.status)" :title="letter.status">
                      {{ statusLabel(letter.status) }}
                    </span>
                  </td>
                  <td class="num mono">{{ letter.attempts ?? 0 }}</td>
                  <td>
                    <div class="row-actions row-actions--compact">
                      <button class="btn btn--primary" type="button" :disabled="letter.status !== 'PENDING'" @click="submitReplay(letter.id)">
                        Replay
                      </button>
                      <button class="btn btn--danger" type="button" :disabled="letter.status !== 'PENDING'" @click="submitIgnoreDeadLetter(letter.id)">
                        Ignore
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="deadLetters.length === 0" class="empty">
              <strong>No dead letters loaded</strong>
              <span>Pick a status and press Load to inspect webhook deliveries.</span>
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

const PAGE_SIZE = 20;

const ORDER_STATUSES = [
  "WAITING_PAYMENT",
  "PARTIALLY_PAID",
  "CONFIRMED",
  "EXPIRED",
  "REFUND_PROCESSING",
  "REFUNDED",
  "REFUND_FAILED"
];
const PAYMENT_STATUSES = ["PENDING", "DETECTED", "CONFIRMING", "CONFIRMED", "FAILED", "EXPIRED"];
const FIAT_STATUSES = ["CREATED", "PENDING_PAYMENT", "PROCESSING", "COMPLETED", "FAILED", "EXPIRED", "CANCELLED"];

const STATUS_LABELS: Record<string, string> = {
  WAITING_PAYMENT: "Awaiting payment",
  PENDING_PAYMENT: "Awaiting payment",
  PARTIALLY_PAID: "Partially paid",
  CONFIRMED: "Confirmed",
  EXPIRED: "Expired",
  REFUND_PROCESSING: "Refund processing",
  REFUNDED: "Refunded",
  REFUND_FAILED: "Refund failed",
  PENDING: "Pending",
  DETECTED: "Detected",
  CONFIRMING: "Confirming",
  FAILED: "Failed",
  CREATED: "Created",
  PROCESSING: "Processing",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
  UNMATCHED: "Unmatched",
  RESOLVED: "Resolved",
  IGNORED: "Ignored",
  COMPENSATED: "Compensated",
  REPLAYED: "Replayed",
  UP: "Up",
  DOWN: "Down"
};

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

function refreshCurrentView() {
  if (activeView.value === "orders") {
    void loadOrders();
  } else if (activeView.value === "payments") {
    void loadPayments();
  } else if (activeView.value === "fiat") {
    void loadFiatRampOrders();
  } else if (activeView.value === "interventions") {
    void loadOrphans(false);
    void loadDeadLetters(false);
  } else {
    void refreshDashboard();
  }
}

async function loadOrphans(notify = true) {
  await run(async () => {
    orphans.value = await listOrphans(orphanStatus.value);
    if (notify) {
      notice("Orphans loaded");
    }
  });
}

async function loadDeadLetters(notify = true) {
  await run(async () => {
    deadLetters.value = await listDeadLetters(deadLetterStatus.value, 25);
    if (notify) {
      notice("Dead letters loaded");
    }
  });
}

async function loadOrders() {
  await run(async () => {
    const result = await listOrders(orderStatusFilter.value, orderMerchantFilter.value.trim(), ordersPageNo.value, PAGE_SIZE);
    orderItems.value = result.items;
    ordersTotal.value = result.total;
  });
}

async function loadPayments() {
  await run(async () => {
    const result = await listPayments(paymentStatusFilter.value, paymentsPageNo.value, PAGE_SIZE);
    paymentItems.value = result.items;
    paymentsTotal.value = result.total;
  });
}

async function loadFiatRampOrders() {
  await run(async () => {
    const result = await listFiatRampOrders(fiatStatusFilter.value, fiatMerchantFilter.value.trim(), fiatPageNo.value, PAGE_SIZE);
    fiatItems.value = result.items;
    fiatTotal.value = result.total;
  });
}

function clearOrderFilters() {
  orderStatusFilter.value = "";
  orderMerchantFilter.value = "";
  ordersPageNo.value = 0;
  void loadOrders();
}

function clearPaymentFilters() {
  paymentStatusFilter.value = "";
  paymentsPageNo.value = 0;
  void loadPayments();
}

function clearFiatFilters() {
  fiatStatusFilter.value = "";
  fiatMerchantFilter.value = "";
  fiatPageNo.value = 0;
  void loadFiatRampOrders();
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
  } else if (view === "interventions") {
    if (orphans.value.length === 0) {
      void loadOrphans(false);
    }
    if (deadLetters.value.length === 0) {
      void loadDeadLetters(false);
    }
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
    await Promise.all([loadOrphans(false), refreshDashboard()]);
    notice("Orphan resolved");
  });
}

async function submitCompensate(orphan: OrphanTransaction) {
  if (!window.confirm(`Compensate orphan ${orphan.txHash}?`)) {
    return;
  }
  await run(async () => {
    await compensateOrphan(orphan.chain, orphan.txHash);
    await Promise.all([loadOrphans(false), refreshDashboard()]);
    notice("Compensation requested");
  });
}

async function submitIgnore(orphan: OrphanTransaction) {
  if (!window.confirm(`Ignore orphan ${orphan.txHash}?`)) {
    return;
  }
  await run(async () => {
    await ignoreOrphan(orphan.chain, orphan.txHash);
    await Promise.all([loadOrphans(false), refreshDashboard()]);
    notice("Orphan ignored");
  });
}

async function submitReplay(id: string) {
  await run(async () => {
    await replayDeadLetter(id);
    await loadDeadLetters(false);
    notice("Replay submitted");
  });
}

async function submitIgnoreDeadLetter(id: string) {
  if (!window.confirm("Ignore this dead letter?")) {
    return;
  }
  await run(async () => {
    await ignoreDeadLetter(id);
    await loadDeadLetters(false);
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

async function copyValue(value?: string) {
  const text = (value ?? "").trim();
  if (!text || text === "-") {
    return;
  }
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
    } else {
      const area = document.createElement("textarea");
      area.value = text;
      area.setAttribute("readonly", "");
      area.style.position = "fixed";
      area.style.opacity = "0";
      document.body.appendChild(area);
      area.select();
      document.execCommand("copy");
      area.remove();
    }
    notice("Copied to clipboard");
  } catch {
    notice("Copy failed");
  }
}

function orphanKey(orphan: OrphanTransaction) {
  return `${orphan.chain}:${orphan.txHash}`;
}

function count(map: Record<string, number> | undefined, key: string) {
  return Number(map?.[key] ?? 0);
}

function statusLabel(status?: string) {
  if (!status) {
    return "-";
  }
  return STATUS_LABELS[status] ?? labelize(status);
}

function statusTone(status?: string) {
  switch (status) {
    case "UP":
    case "CONFIRMED":
    case "COMPLETED":
    case "RESOLVED":
    case "REPLAYED":
      return "pill--ok";
    case "CONFIRMING":
    case "PROCESSING":
    case "REFUND_PROCESSING":
      return "pill--progress";
    case "CREATED":
    case "WAITING_PAYMENT":
    case "PENDING_PAYMENT":
    case "DETECTED":
    case "REFUNDED":
    case "COMPENSATED":
      return "pill--info";
    case "PARTIALLY_PAID":
    case "PENDING":
      return "pill--warn";
    case "FAILED":
    case "REFUND_FAILED":
    case "DOWN":
    case "UNMATCHED":
      return "pill--bad";
    case "EXPIRED":
    case "CANCELLED":
    case "IGNORED":
      return "pill--muted";
    default:
      return "pill--info";
  }
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

function directionLabel(direction?: string) {
  const value = (direction ?? "").toUpperCase();
  if (value.includes("OFF")) {
    return "Crypto → Fiat";
  }
  if (value.includes("ON")) {
    return "Fiat → Crypto";
  }
  return labelize(direction ?? "");
}

function directionTone(direction?: string) {
  const value = (direction ?? "").toUpperCase();
  return value.includes("OFF") ? "dir--off" : "dir--on";
}

function meterSegments(required?: number | null) {
  const value = Number(required ?? 0);
  if (!value || value <= 0) {
    return 0;
  }
  return Math.min(value, 10);
}

function meterFilled(confirmations?: number | null, required?: number | null) {
  const total = Number(required ?? 0);
  const done = Number(confirmations ?? 0);
  if (!total) {
    return 0;
  }
  const segments = meterSegments(total);
  return Math.max(0, Math.min(segments, Math.ceil((done / total) * segments)));
}

function rangeLabel(pageNo: number, total: number) {
  if (!total) {
    return "No results";
  }
  const from = pageNo * PAGE_SIZE + 1;
  const to = Math.min((pageNo + 1) * PAGE_SIZE, total);
  return `Showing ${from}–${to} of ${total}`;
}

function totalPages(total: number) {
  return Math.max(1, Math.ceil(total / PAGE_SIZE));
}

function money(amount?: string, currency?: string) {
  if (!amount) {
    return "-";
  }
  const trimmed = amount.includes(".") ? amount.replace(/0+$/, "").replace(/\.$/, "") : amount;
  return `${trimmed} ${currency ?? ""}`.trim();
}

function currencyParts(currency?: string) {
  const value = (currency ?? "").trim();
  if (!value) {
    return { code: "", net: "" };
  }
  const separator = value.indexOf("_");
  if (separator < 0) {
    return { code: value, net: "" };
  }
  return { code: value.slice(0, separator), net: value.slice(separator + 1) };
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

:where(button, input, select):focus-visible {
  outline: 2px solid #4f46e5;
  outline-offset: 2px;
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

.brand__mark--lg {
  width: 44px;
  height: 44px;
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
.muted {
  color: #8b99ab;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.sub {
  display: block;
  color: #8b99ab;
  font-size: 11px;
  font-weight: 600;
}

.side-section {
  display: grid;
  gap: 8px;
  margin-bottom: 18px;
}

.side-actions {
  display: grid;
  gap: 8px;
}

.nav {
  display: grid;
  gap: 8px;
}

.nav .label {
  margin-bottom: 2px;
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
  text-transform: none;
  letter-spacing: 0;
  color: #0b1526;
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

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.login-brand__name {
  font-size: 15px;
  font-weight: 750;
  letter-spacing: -0.01em;
}

.login-brand__name em {
  font-style: normal;
  color: #8b99ab;
  font-weight: 650;
}

.workspace {
  position: relative;
  min-width: 0;
  padding: 24px clamp(16px, 3vw, 32px) 40px;
}

.busy-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  overflow: hidden;
  z-index: 5;
}

.busy-bar::after {
  content: "";
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 40%;
  border-radius: 999px;
  background: linear-gradient(90deg, #4f46e5, #0ea5a4);
  animation: busy-sweep 1.1s ease-in-out infinite;
}

@keyframes busy-sweep {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(350%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .busy-bar::after {
    animation: none;
    width: 100%;
    opacity: 0.5;
  }

  .metric:hover {
    transform: none;
  }
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

.btn--sm {
  min-height: 32px;
  padding: 4px 12px;
  font-size: 12.5px;
  border-radius: 8px;
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

.panel__head .header-actions {
  flex-wrap: wrap;
}

.panel__head .header-actions select {
  width: auto;
  min-height: 36px;
  min-width: 150px;
}

.toolbar {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid #edf1f7;
  background: #fbfcfe;
}

.toolbar__field {
  display: grid;
  gap: 5px;
  min-width: 170px;
}

.toolbar__field--grow {
  flex: 1 1 220px;
  max-width: 420px;
}

.toolbar__field > span {
  color: #8b99ab;
  font-size: 10.5px;
  font-weight: 750;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.toolbar__field input,
.toolbar__field select {
  min-height: 36px;
  padding: 7px 11px;
  background: #ffffff;
}

.toolbar__actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.panel__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 20px;
  border-top: 1px solid #edf1f7;
  background: #fbfcfe;
}

.foot-note {
  color: #5b6b7e;
  font-size: 12.5px;
}

.foot-note strong {
  color: #0b1526;
  font-variant-numeric: tabular-nums;
}

.pager {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pager__page {
  min-width: 84px;
  text-align: center;
  color: #5b6b7e;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 820px;
  border-collapse: collapse;
}

.table--narrow {
  min-width: 540px;
}

th,
td {
  padding: 11px 13px;
  border-bottom: 1px solid #edf1f7;
  text-align: left;
  font-size: 13px;
  vertical-align: top;
}

th {
  color: #8b99ab;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  background: #fbfcfe;
  white-space: nowrap;
}

td {
  padding-top: 11px;
  padding-bottom: 11px;
}

tbody tr:last-child td {
  border-bottom: 0;
}

tbody tr:hover {
  background: #f8fafd;
}

th.num,
td.num {
  text-align: right;
}

td.num {
  white-space: nowrap;
}

td strong {
  font-weight: 650;
}

.mono {
  font-family: ui-monospace, "Cascadia Mono", "JetBrains Mono", SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-variant-numeric: tabular-nums;
}

.dim {
  color: #5b6b7e;
}

.copyable {
  cursor: copy;
  white-space: nowrap;
}

.copyable:hover {
  text-decoration: underline;
  text-decoration-style: dotted;
  text-underline-offset: 3px;
}

.cur {
  margin-left: 6px;
  color: #8b99ab;
  font-size: 11px;
  font-weight: 600;
}

.net {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 7px;
  border-radius: 999px;
  background: rgba(14, 165, 164, 0.1);
  color: #0f766e;
  font-family: ui-monospace, "Cascadia Mono", "JetBrains Mono", SFMono-Regular, Consolas, monospace;
  font-size: 10.5px;
  font-weight: 750;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  vertical-align: 1px;
}

td .net:first-child {
  margin-left: 0;
}

.pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 26px;
  border-radius: 999px;
  padding: 3px 11px;
  background: #f6f8fb;
  color: #5b6b7e;
  border: 1px solid #e3e8f0;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.pill::before {
  content: "";
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: currentColor;
  flex: none;
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

.pill--progress {
  background: #eef2ff;
  color: #4f46e5;
  border-color: #c7d2fe;
}

.pill--muted {
  background: #f6f8fb;
  color: #8b99ab;
  border-color: #e3e8f0;
}

.dir {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  border-radius: 8px;
  padding: 3px 10px;
  border: 1px solid;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.dir--on {
  background: rgba(14, 165, 164, 0.08);
  border-color: rgba(14, 165, 164, 0.35);
  color: #0f766e;
}

.dir--off {
  background: #eef2ff;
  border-color: #c7d2fe;
  color: #4f46e5;
}

.meter {
  display: flex;
  align-items: center;
  gap: 3px;
  min-width: 130px;
}

.meter__seg {
  height: 8px;
  flex: 1 1 0;
  min-width: 5px;
  max-width: 14px;
  border-radius: 2px;
  background: #e3e8f0;
}

.meter__seg--on {
  background: #4f46e5;
}

.meter__label {
  margin-left: 7px;
  color: #5b6b7e;
  font-size: 11px;
  white-space: nowrap;
}

.fail-reason {
  display: block;
  margin-top: 5px;
  max-width: 220px;
  color: #dc2626;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0;
  text-transform: none;
  white-space: normal;
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

.alert > div > span {
  color: #5b6b7e;
  font-size: 12px;
  text-transform: none;
  letter-spacing: 0;
  font-weight: 500;
}

.alert > strong.mono {
  font-size: 15px;
  font-variant-numeric: tabular-nums;
}

.row-actions {
  align-items: stretch;
  min-width: 520px;
}

.row-actions--compact {
  min-width: 0;
}

.row-actions input {
  min-width: 180px;
}

.empty {
  display: grid;
  justify-items: center;
  gap: 6px;
  padding: 36px 16px;
  text-align: center;
}

.empty strong {
  color: #0b1526;
  font-size: 14px;
}

.empty span {
  color: #5b6b7e;
  font-size: 13px;
}

.empty .btn {
  margin-top: 8px;
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
  z-index: 20;
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

  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar__field,
  .toolbar__field--grow {
    min-width: 0;
  }

  .toolbar__actions {
    margin-left: 0;
  }

  .toolbar__actions .btn {
    flex: 1;
  }

  .panel__foot {
    flex-direction: column;
    align-items: stretch;
  }

  .pager {
    justify-content: space-between;
  }

  .meter {
    min-width: 110px;
  }
}
</style>
