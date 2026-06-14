# nexusflow

**nexusflow** is a digital asset payment engine designed for scalable, multi-chain crypto payment processing.

It serves as the **execution layer (Data Plane)** in the NexusPay ecosystem, handling on-chain transactions, wallet operations, and blockchain integrations.

---

## 🧠 Positioning

nexusflow is **NOT** a checkout system and **NOT** an orchestration layer.

It is responsible for:

* Executing crypto payments
* Managing blockchain interactions
* Tracking on-chain transaction states

```text
NexusPay-Core (Control Plane)
        ↓
   nexusflow (Data Plane - Crypto)
```

---

## 🚀 Core Responsibilities

### 1. Crypto Payment Execution

* Generate payment instructions (address / memo / amount)
* Handle incoming blockchain transactions
* Validate payments against expected orders

---

### 2. Wallet Management

* Hot / cold wallet abstraction
* Address generation (HD wallet / MPC-ready)
* Private key encryption (AES-256-GCM or external KMS)

---

### 3. Blockchain Integration

Support multiple chains via pluggable adapters:

* Ethereum (ETH / ERC20)
* Tron (TRC20)
* Bitcoin (UTXO model)
* Solana (optional)

---

### 4. Transaction Lifecycle

```text
CREATED
→ PENDING (waiting for payment)
→ DETECTED (tx seen on-chain)
→ CONFIRMING
→ CONFIRMED
→ FAILED / EXPIRED
```

---

### 5. Event Emission

nexusflow emits domain events:

* `crypto.payment.detected`
* `crypto.payment.confirmed`
* `crypto.payment.failed`

These events are consumed by **NexusPay-Core**.

---

## 🧱 Architecture Overview

```text
nexusflow/
├── flow-common/          # shared utils (crypto, encryption, errors)
├── flow-domain/          # core domain models
├── flow-application/     # use cases (create payment, confirm)
├── flow-infra/           # blockchain adapters
│   ├── ethereum/
│   ├── tron/
│   ├── bitcoin/
│
├── flow-listener/        # blockchain listeners / indexers
├── flow-wallet/          # wallet service
├── flow-api/             # REST / gRPC API
```

---

## 🔌 Integration with NexusPay-Core

### Inbound (from Core)

```http
POST /crypto/payments
```

Request:

```json
{
  "orderId": "xxx",
  "currency": "USDT_TRC20",
  "amount": "1000000",
  "callbackUrl": "..."
}
```

Orchestration orders can also be created directly in crypto units:

```http
POST /pay/order
```

```json
{
  "merchantId": "merchant-1",
  "merchantOrderNo": "order-1001",
  "amountCrypto": "25.5",
  "currencyCrypto": "USDC",
  "network": "ERC20",
  "currencyFiat": "USD"
}
```

The static buyer checkout is available at `flow-cashier/src/main/resources/static/checkout.html`.
It loads `payment_id` from the URL, calls `/cashier/order/status` and `/cashier/pay/submit`,
renders the deposit address/Canvas QR code, and polls payment status until a terminal state.
`PaymentOrchestrator.createOrder` returns a configurable checkout URL via
`nexusflow.cashier.base-url`, defaulting to `/checkout.html`.

The static merchant console is available at `flow-cashier/src/main/resources/static/merchant.html`.
It stores API base, `X-API-Key`, and callback URLs locally in the browser and calls `/pay/order`,
`/pay/order/{paymentId}`, and `/refund/order`.

The static ops dashboard is available at `flow-cashier/src/main/resources/static/ops.html`. It calls
`/ops/dashboard` for channel health, order-board counts, reconciliation backlog, and risk alerts, and
uses `/crypto/orphan-transactions` for orphan resolve, ignore, and compensate actions.

---

### Outbound (to Core)

Event-driven (Kafka / Webhook):

```json
{
  "event": "crypto.payment.confirmed",
  "orderId": "xxx",
  "txHash": "...",
  "confirmations": 12
}
```

---

## 🔐 Security Principles

* Private keys must never be stored in plain text
* Support external KMS (AWS KMS / HashiCorp Vault)
* All sensitive data encrypted at rest
* Strict separation of signing and API layers

---

## 📊 Observability

* Prometheus metrics
* OpenTelemetry tracing
* Structured logs (JSON)

---

## ⚙️ Deployment

```bash
# start core services
docker-compose up flow-api flow-listener flow-wallet
```

---

## 🧪 MVP Scope (Phase 1)

* [ ] TRC20 USDT support
* [ ] Single wallet (hot wallet)
* [ ] Basic listener (polling or node subscription)
* [ ] Payment detection + confirmation
* [ ] Event push to NexusPay-Core

---

## 🛣️ Roadmap

### Phase 2

* Multi-chain support (ETH / BTC)
* Address pool management
* Retry / reorg handling

### Phase 3

* MPC wallet integration
* Gas abstraction
* On/Off ramp integration

---

## ⚠️ Non-Goals

nexusflow does NOT:

* Provide checkout UI
* Handle fiat payments
* Perform orchestration / routing decisions
* Replace NexusPay-Core

---

## 🧠 Philosophy

nexusflow is built as a **modular, chain-agnostic execution engine**.

* Core handles "what to do"
* nexusflow handles "how it happens on-chain"

---

## 📌 Summary

> nexusflow is the crypto execution engine powering digital asset payments in the Nexus ecosystem.

---


## 🧱 Tech Stack

### Runtime
- Java 17
- Spring Boot 3.x

### Architecture Style
- Domain-Driven Design (DDD)
- Modular Monolith (Phase 1)
- Event-Driven Architecture (internal events)

### Persistence
- PostgreSQL (primary database)
- Redis (cache / idempotency / rate limit)

### Messaging
- Kafka (optional, for Phase 2+)
- Spring Application Events (Phase 1)

### Observability
- OpenTelemetry (tracing)
- Prometheus (metrics)
- SLF4J + structured logging (JSON)

### Security
- AES-256-GCM encryption for sensitive data
- External KMS support (AWS KMS / Vault)
- No plaintext private keys allowed

### API Style
- REST (primary)
- gRPC (future extension)


## 🧠 Domain Boundaries

### Flow Domain (Crypto Core)
Responsible for:
- Crypto payment lifecycle
- On-chain transaction tracking
- Wallet abstraction

NOT responsible for:
- Checkout UI
- Payment orchestration
- Fiat payments

---

### Wallet Subdomain
- Address generation
- Key encryption
- Wallet lifecycle

---

### Blockchain Subdomain
- Chain adapters (ETH / TRON / BTC)
- Transaction scanning
- Confirmation tracking


## ⚙️ System Design Rules

### 1. Strict Layering
- API → Application → Domain → Infrastructure
- Domain must NOT depend on infrastructure

---

### 2. Event-Driven Communication
- Domain events must be emitted for all state transitions
- External systems consume events only (no direct DB access)

---

### 3. No Cross-Domain Coupling
- Wallet must NOT depend on Blockchain Adapter directly
- Payment must NOT depend on external APIs

---

### 4. Idempotency Required
- All inbound payment creation APIs must be idempotent
- Use requestId or orderId as dedup key

---

### 5. Time-Based State Transitions
- Crypto payments are asynchronous
- State changes must be driven by:
  - blockchain events
  - scheduled reconciliation jobs
  
  
  ## 🤖 Agent Development Mode

This repository is designed to support AI-assisted development.

### Code Generation Rules

- Always follow DDD structure
- Do not create logic in controller layer
- All business logic must be inside domain or application layer

---

### Task Decomposition Strategy

Agents should always split tasks into:

1. Domain modeling
2. Application use case implementation
3. Infrastructure adapters
4. API exposure
5. Event integration

---

### File Creation Rules

- Domain → `flow-domain`
- Use cases → `flow-application`
- External systems → `flow-infra`
- API layer → `flow-api`

---

### Forbidden Patterns

- No business logic in controllers
- No direct DB access outside repository layer
- No blockchain logic in domain layer


## 🧠 Runtime Model

nexusflow is an asynchronous event-driven payment execution system.

### Key Principle:
> All crypto payments are eventually consistent, not real-time consistent.

### Execution Flow:

1. Payment is created (sync API)
2. Blockchain event is detected (async)
3. System transitions state over time
4. Final confirmation is delayed by chain finality



## 🔄 State Machine Rules

### CryptoPayment State Machine is STRICT

Allowed transitions:

CREATED → PENDING
PENDING → DETECTED
DETECTED → CONFIRMING
CONFIRMING → CONFIRMED
CONFIRMING → FAILED
PENDING → EXPIRED

### Rules:
- No backward transitions allowed
- State changes MUST be event-driven
- Manual state update is forbidden (except admin tools)



## ⏱ Time Semantics

### Crypto payments are NOT time-based systems

There are 3 different timestamps:

- createdAt → system creation time
- detectedAt → blockchain detection time
- confirmedAt → finality time (NOT deterministic)

### Important Rule:
> Payment success is NOT determined by time, but by blockchain confirmations.


## 🔁 Idempotency Rules

All external APIs MUST be idempotent.

### Key Rules:

- orderId is global idempotency key
- repeated requests MUST return same result
- no duplicate payment creation allowed

### Applies to:
- create payment
- confirm payment
- webhook ingestion


## 🔐 Security Boundaries

### nexusflow MUST NEVER:
- expose private keys via API
- return raw wallet private data
- log sensitive blockchain credentials

### Allowed:
- encrypted key storage only
- signed transactions via internal wallet module

### Threat Model:
- API abuse
- replay attack
- chain spoofing


## 🌐 External Dependencies

### Blockchain Nodes:
- ETH RPC providers
- TRON full node / API
- BTC node (future)

### External Systems:
- NexusPay-Core (orchestration layer)
- Monitoring system
- KMS (optional)

### Rule:
> External systems MUST NOT access database directly


## ❌ Failure Model

### Failure Types:

1. API Failure (sync)
2. Blockchain Delay (async)
3. Chain Reorg (data inconsistency)
4. Node Failure (infrastructure)
5. Wallet Failure (signing issues)

### Rule:
> All failures are recoverable except invalid payment data

## 📊 Consistency Model

nexusflow is:

### NOT:
- strongly consistent

### IS:
- eventually consistent
- event-driven consistent
- reconciled by background jobs


## 🧭 System Boundaries

### nexusflow owns:
- wallet
- blockchain tracking
- crypto payment lifecycle

### NexusPay-Core owns:
- checkout
- orchestration
- routing
- merchant logic

### Shared only via:
- events
- APIs


## 🔁 Reconciliation Strategy

### Purpose:
Ensure blockchain truth matches system state

### Mechanism:
- periodic scan jobs
- re-check unconfirmed tx
- fix missing events

### Rule:
> Blockchain is source of truth, system is derived state

---

## 🛠️ Implementation Roadmap — Unimplemented Items

> Generated 2026-06-07. Lists every TODO, stub, placeholder, and missing feature across the codebase with exact file references.

---

### ═══════════════════════════════════════════
### 🔴 P0 — Phase 1 MVP: Must Complete to Ship
### ═══════════════════════════════════════════

---

#### P0-1: TronAdapter — TRC20 Block Scanning — ✅ IMPLEMENTED / NOT LIVE-VERIFIED

**File:** `flow-infra/.../blockchain/TronAdapter.java` (now backed by `TronGridClient` / `HttpTronGridClient`)

| Method | Status | Notes |
|--------|--------|-------|
| `scanNewBlocks()` | ✅ done | Uses TronGrid `/v1/contracts/{contract}/events` with `event_name=Transfer`, `only_confirmed=true`, and per-block `block_number` queries; parses txHash/from/to/value/block/timestamp/confirmations into `ScannedTransaction`. |
| `getCurrentBlockHeight()` | ✅ done | `POST /wallet/getnowblock` → `block_header.raw_data.number` |
| `getConfirmations()` | ✅ done | `gettransactioninfobyid` → `blockNumber`, then `currentHeight - txBlock` |
| `isHealthy()` | ✅ done | healthy when block height > 0 |

Response parsing and event scanning are covered by `TronAdapterTest` (HTTP transport stubbed).
Remaining: verify all TRON calls against a live TronGrid or full-node environment.

---

#### P0-2: KeyGenerator — Address Derivation — ✅ DONE (ETH/TRON/BTC)

**File:** `flow-wallet/.../wallet/KeyGenerator.java` (+ self-contained `Base58` util)

> Implemented with HD derivation: **ETH** = keccak256(pubkey)[12:] -> EIP-55 checksummed hex;
> **TRON** = 0x41 + keccak256(pubkey)[12:] -> Base58Check; **BTC** = compressed secp256k1 pubkey
> -> HASH160 -> Base58Check P2PKH. `KeyGeneratorTest` and `Base58Test` cover address derivation.
> **SOLANA** remains explicitly unsupported.

---

#### P0-3: PaymentApplicationService — onPaymentDetected (Payment Matching) — ✅ DONE

> Implemented: `onPaymentDetected` now dedups by `txHash`, matches a PENDING payment via
> `PaymentRepository.findPendingByReceivingAddress`, validates currency, transitions to DETECTED,
> saves, and publishes events. Covered by `PaymentApplicationServiceTest`.

**File:** `flow-application/.../application/PaymentApplicationService.java`

**Problem (original):** The method only logged incoming transactions; it never matched them to specific payments or transitioned state.

**What to do:**
1. Query `paymentRepository` for payments in `PENDING` status at `toAddress`
2. Match by `expected.amount` and `expected.currency`
3. Call `payment.markDetected(txHash, receivedAmount)`
4. Save payment and publish events
5. Trigger webhook callback to `callbackUrl` if set

---

#### P0-4: Webhook Callback to NexusPay-Core — ✅ DONE

**File:** `flow-application/.../application/PaymentApplicationService.java`

Implemented: `PaymentApplicationService` now publishes domain events and calls
`WebhookService.notifyCryptoPayment()` for execution-layer `PaymentStateChangedEvent`s.
`WebhookService` reuses the existing `WebhookClient` retry/HMAC/SSRF protection, records failed
or blocked deliveries in `webhook_dead_letters`, and skips the initial CREATED→PENDING setup event.
`@EnableAsync` enables asynchronous delivery in the API application. Operators can list, replay,
and ignore dead letters through `/ops/webhook-dead-letters` and `ops.html`. Covered by
`WebhookServiceTest`, `WebhookDeadLetterApplicationServiceTest`, `WebhookDeadLetterControllerTest`,
`JpaWebhookDeadLetterStoreTest`, and `PaymentApplicationServiceTest`.

---

#### P0-5: Idempotency Key Persistence — ✅ DONE

**DB migration already exists:** `flow-api/.../db/migration/V1__init_schema.sql` (`idempotency_keys` table)

Implemented:
1. ✅ Persistent, multi-instance dedup store (Redis) behind the existing `ProcessedEventStore` port
2. ✅ `createPayment` accepts `Idempotency-Key` / `X-Idempotency-Key`, reserves the key, checks request hash consistency, caches the full `PaymentResponse`, and replays matching retries
3. ✅ `idempotency_keys` is wired via `PaymentIdempotencyStore` and `JpaPaymentIdempotencyStore`; `V6__add_idempotency_request_hash.sql` adds request fingerprinting

---

#### P0-6: Payment Expiry Scheduler — ✅ DONE

> Implemented as `PaymentReconciliationJob.expireOverduePayments()` (`flow-application`,
> `@Scheduled`, TTL via `nexusflow.payment.expiry-minutes`, default 30 min). Queries PENDING
> payments past TTL and calls the new `PaymentApplicationService.expirePayment()` (own transaction
> per payment). Covered by `PaymentReconciliationJobTest`. Expiry now also flows through the
> execution-layer merchant webhook path when `callbackUrl` is set.

**File:** `flow-domain/.../domain/payment/CryptoPayment.java` (`markExpired()`)

**Problem (original):** `EXPIRED` state and `markExpired()` existed, but nothing ever triggered them.

---

#### P0-7: Reconciliation Job — 🟡 PARTIAL

Per init.md: "Blockchain is source of truth, system is derived state."

> Implemented: `PaymentReconciliationJob.reconcileConfirmations()` (`flow-application`, `@Scheduled`,
> interval via `nexusflow.reconciliation.interval-ms`). Scans DETECTED/CONFIRMING payments, resolves
> the chain adapter, re-queries `BlockchainAdapter.getConfirmations()`, and drives confirmation via
> `confirmPayment()` (own transaction per payment; one failure does not abort the batch). Backed by
> `PaymentRepository.findByStatusIn()`. Covered by `PaymentReconciliationJobTest`.
> NOTE: ETH/BTC adapters and TRON contract-event scanning are implemented, but all chain adapters
> still need live-node environment verification before production use.

**Still to do:**
1. ✅ Periodically scan unconfirmed payments (DETECTED, CONFIRMING)
2. ✅ Re-query blockchain for latest confirmations via `BlockchainAdapter.getConfirmations()`
3. ✅ Update payment confirmations, transition to CONFIRMED as appropriate
4. ✅ Retry/backoff transient chain failures and gate noisy RPC failures with `BlockchainCircuitBreaker`
5. ✅ Detect block-hash reorg in `BlockchainScanner` and roll affected payments back to `PENDING`
6. ✅ Detect missing events: tx seen on a managed address without a PENDING payment is persisted
   as an `orphan_transactions` record and emits `crypto.orphan.detected`; `/crypto/orphan-transactions`
   supports listing, resolve, ignore, and compensate, with optional automatic compensation via
   `ORPHAN_AUTO_COMPENSATION_ENABLED=true`.

---

### ═══════════════════════════════════════════
### 🟡 P1 — Phase 2: Multi-Chain & Production Hardening
### ═══════════════════════════════════════════

---

#### P1-1: EthereumAdapter — ERC20 Block Scanning — ✅ DONE

**File:** `flow-infra/.../blockchain/EthereumAdapter.java`

| Method | Status | What to do |
|--------|--------|-------------|
| `scanNewBlocks()` | ✅ done | Uses `eth_getLogs` with the ERC20 `Transfer` topic and configured USDT contract, parses from/to/amount/block/confirmations into `ScannedTransaction`. |
| `getConfirmations()` | ✅ done | Reads transaction receipt block number and computes `currentBlock - txBlock + 1`. |
| `getBlockHash()` | ✅ done | Reads canonical block hash for scanner reorg detection. |

Note: implementation is offline-compiled and covered indirectly by module tests; live verification still requires an ETH RPC endpoint.

---

#### P1-2: BitcoinAdapter — Full Implementation — ✅ DONE

**File:** `flow-infra/.../blockchain/BitcoinAdapter.java`

Implemented via Bitcoin Core JSON-RPC:
1. ✅ `getblockcount`, `getblockhash`, `getblock`, `getrawtransaction`
2. ✅ `scanNewBlocks()` iterates blocks and parses UTXO outputs into `ScannedTransaction`
3. ✅ `getCurrentBlockHeight()`, `getConfirmations()`, `getBlockHash()`, `isHealthy()`

Covered by `BitcoinAdapterTest` with mocked RPC responses. Live verification still requires a Bitcoin Core node.

---

#### P1-3: BIP32/BIP44 HD Wallet — ✅ DONE

**File:** `flow-wallet/.../wallet/KeyGenerator.java` (L13)

**Done:**
1. ✅ `KeyGenerator` now supports BIP39 mnemonic -> BIP32 seed -> BIP44 private-key derivation.
2. ✅ Paths: `m/44'/60'/0'/0/i` (ETH), `m/44'/195'/0'/0/i` (TRON), `m/44'/0'/0'/0/i` (BTC).
3. ✅ BTC P2PKH address derivation added; SOLANA remains explicitly unsupported.
4. ✅ `MnemonicStore` port + JPA-backed encrypted mnemonic backup (`mnemonic_backups`).
5. ✅ `WalletService.createHotWallet()` generates an HD mnemonic, derives index 0, stores encrypted private key, and stores encrypted mnemonic backup.

---

#### P1-4: Persistence Layer — Replace InMemory Repositories — ✅ DONE

**Files:**
- `flow-infra/.../persistence/InMemoryPaymentRepository.java`
- `flow-infra/.../persistence/InMemoryWalletRepository.java`

> Implemented 2026-06-13: execution-layer `crypto_payments` and `wallets` now have JPA entities,
> Spring Data DAOs, domain repository adapters, and `V4__add_execution_version_columns.sql`.
> Default wiring uses `nexusflow.execution.persistence=jpa`; the old in-memory repositories are
> opt-in via `=memory`. Added mapper unit tests and extended `NexusFlowApplicationIT` for payment
> and wallet round trips. Integration tests are present but were skipped locally when Docker was
> unavailable.

**Done:**
1. ✅ Create JPA entities mapping to `crypto_payments` and `wallets` tables
2. ✅ Implement `JpaPaymentRepository` and `JpaWalletRepository`
3. ✅ Keep in-memory repositories as explicit opt-in fallback
4. ✅ Add mapper tests and Testcontainers integration coverage

---

#### P1-5: Address Pool Management — ✅ DONE

**Done:**
1. ✅ Added `AddressPoolEntry` aggregate, `AddressPoolRepository`, and JPA mapping (`address_pool`).
2. ✅ Addresses move from `AVAILABLE` to `ASSIGNED` when allocated to a payment.
3. ✅ `PaymentApplicationService.createPayment()` now allocates from the pool instead of reusing one wallet address.
4. ✅ `AddressPoolProvisioningService` replenishes low pools from configured `ADDRESS_POOL_SEED_MNEMONIC`.
5. ✅ `TransactionProcessor` recognizes both address-pool addresses and legacy wallet addresses.

---

#### P1-6: Retry & Chain Reorg Handling — ✅ DONE

**Done:**
1. ✅ Added persistent `ChainScanCursor` with block hash tracking.
2. ✅ `BlockchainScanner` detects block-hash mismatch and rewinds by configurable `nexusflow.scanner.reorg-rewind-blocks`.
3. ✅ Affected `DETECTED` / `CONFIRMING` payments roll back to `PENDING` via `CryptoPayment.rollbackAfterReorg()`.
4. ✅ Added `retryCount`, `nextRetryAt`, and `lastFailureReason` to `CryptoPayment`; reconciliation failures use exponential backoff.
5. ✅ Added lightweight `BlockchainCircuitBreaker` for chain RPC failures.

---

### ═══════════════════════════════════════════
### 🟢 P2 — Phase 3: Advanced Features
### ═══════════════════════════════════════════

---

#### P2-1: Kafka Event Publishing — ✅ DONE

**File:** `flow-infra/.../event/SpringDomainEventPublisher.java` (L15)

Implemented:
1. ✅ `KafkaDomainEventPublisher` implements `DomainEventPublisher` and is enabled with `EVENT_PUBLISHER=kafka`.
2. ✅ Events publish to `event.eventType()` topics such as `crypto.payment.detected`, `crypto.payment.confirmed`, and `crypto.payment.failed`.
3. ✅ Kafka messages use `eventId` as the key and include an envelope with `event_id`, `event_type`, `event_class`, `occurred_at`, and the original payload. Producer config uses `acks=all` and idempotence.

---

#### P2-2: MPC Wallet Integration

**Status:** 🟡 PARTIAL — `MpcSigner` port, signing request/result value objects, `Wallet.mpcWalletId`,
JPA mapping, Flyway migration `V10__add_wallet_mpc_id.sql`, and opt-in custom HTTP signer adapter
are implemented. Fireblocks/Copper-specific adapters, transaction signing flow integration, and live
signing smoke tests remain pending.

**What to do:**
1. ✅ Define `MpcSigner` port in `flow-domain`
2. 🟡 Integrate with MPC provider (Fireblocks / Copper / custom MPC): custom HTTP signer adapter exists; Fireblocks/Copper adapters remain pending
3. ✅ `Wallet` aggregate gains `mpcWalletId` field
4. ⬜ Transaction signing flows through MPC instead of local private key

---

#### P2-3: Gas Abstraction

**Status:** 🟡 PARTIAL — `GasEstimator` port, `StaticGasEstimator`, configurable ETH/TRON/BTC
fee defaults, GasBank balance/top-up policy objects, and self-hosted refund event gas budget fields
are implemented. Live gas oracle, actual gas-bank funding worker, sweeping integration, and
production batching remain pending.

**What to do:**
1. ✅ `GasEstimator` port in `flow-domain`
2. 🟡 Estimate gas for outgoing transactions: self-hosted refunds now carry static ETH/TRON/BTC gas budgets
3. 🟡 `GasBank` to pre-fund wallets with native tokens for gas: port/request/result and policy recommendations exist; funding worker remains pending
4. 🟡 Monitor gas prices, batch transactions when gas is low: policy supports low/high gas bands and batch/top-up recommendations; live oracle and production scheduler remain pending

---

#### P2-4: On/Off Ramp Integration

**Status:** 🟡 PARTIAL — `FiatGateway` port, quote/order request value objects, persisted
`FiatRampOrder` conversion tracking, status transitions, `FiatRampRepository`, and Flyway migration
`V11__add_fiat_ramp_orders.sql` are implemented. `FiatRampApplicationService` and `/fiat/ramp`
now expose merchant quote/create/get flows behind API key auth, and `/callback/{gatewayId}/fiat-ramp`
accepts normalized status callbacks behind callback HMAC verification. Real MoonPay/Ramp/Banxa
adapters, KYC/provider-specific payload/signature semantics, and live settlement smoke tests remain
pending.

**What to do:**
1. ⬜ Integrate with fiat on/off ramp providers (MoonPay, Ramp, Banxa)
2. ✅ `FiatGateway` port in `flow-domain`
3. 🟡 Handle fiat→crypto and crypto→fiat conversion tracking: domain lifecycle, JPA persistence, merchant API, and normalized HMAC provider status callback exist; real provider payload mapping remains pending

---

### ═══════════════════════════════════════════
### 🔵 P3 — Testing
### ═══════════════════════════════════════════

---

#### P3-1: Unit Tests — 🟡 IN PROGRESS

> Current local verification (2026-06-14): `mvn -pl flow-api,flow-cashier -am test` runs 246 passing tests
> across common/domain/application/infra/listener/wallet and skips 14 opt-in integration/live tests when Docker or live dependency variables are
> unavailable. Coverage now includes state machines, orchestration flows, Redis/idempotency helpers,
> execution-layer JPA repositories, HD wallet derivation, MPC wallet-id persistence and custom HTTP signer adapter, ETH/BTC adapter parsing, address pool storage,
> mnemonic storage, createPayment idempotency, crypto-denominated order creation,
> execution webhooks with dead-letter replay/ignore workflow and opt-in live delivery smoke coverage, gas-estimated self-hosted refund events, GasBank policy recommendations, Coinbase Commerce REST-capable channel with non-prod no-key stub fallback and opt-in live smoke coverage, BitMart/Binance stub beans guarded out of the `prod` profile,
> persisted fiat on/off ramp gateway, merchant API orchestration, normalized HMAC callback, and conversion-tracking core,
> self-hosted node channel deposit/refund delegation,
> callback HMAC body caching, orphan transaction storage/resolution/compensation,
> reconciliation retry, scanner cursor/reorg behavior, Kafka event publishing, ops dashboard aggregation,
> immutable request DTO JSON binding, PaymentController HTTP contract/parameter binding,
> API response envelopes, Money edge cases,
> and the blockchain circuit breaker.

**Remaining priority test targets:**

| Module | What to test |
|--------|-------------|
| `flow-domain` | Any new aggregate state transitions |
| `flow-listener` | Scheduled scanner wiring and transaction processor edge cases |

---

#### P3-2: Integration Tests

| Module | What to test |
|--------|-------------|
| `flow-api` | Docker-backed `NexusFlowApplicationIT` for Spring/Flyway/JPA round trips, persistence-backed createPayment HTTP idempotency, and concurrent address-pool allocation; Redis-backed HTTP E2E still needs a brokered environment |
| `flow-infra` | Opt-in `LiveBlockchainAdapterTest` for ETH/BTC/TRON nodes; opt-in `LiveMessagingInfrastructureTest` for Redis/Kafka; opt-in `LiveCoinbaseCommerceAdapterTest` for Coinbase Commerce rate and guarded charge smoke checks; opt-in `LiveWebhookDeliveryTest` for outbound webhook reachability; repository CRUD against real database; `EthereumAdapter` against local Ganache/Hardhat node |
| `flow-listener` | `BlockchainScanner` scheduled execution with mocked adapter |

---

### 📊 Summary

| Priority | Count | Items | Status |
|----------|-------|-------|--------|
| P0 (MVP must-have) | 7 | TronAdapter, KeyGenerator, PaymentMatching, Webhook, Idempotency, Expiry, Reconciliation | ✅ KeyGenerator, PaymentMatching, Webhook, Idempotency, Expiry, TronAdapter · 🟡 Reconciliation live verification |
| P1 (Phase 2) | 6 | EthereumAdapter, BitcoinAdapter, HDWallet, JPA Persistence, AddressPool, Retry/Reorg | ✅ all |
| P2 (Phase 3) | 4 | Kafka, MPC, GasAbstraction, OnOffRamp | ✅ Kafka · 🟡 MPC core/GasEstimator+GasBank core/OnOffRamp core |
| P3 (Testing) | 2 | Unit tests, Integration tests | 🟡 Unit tests (246 passing locally) · 🟡 Integration/live tests present, 14 skipped locally without Docker/live env |
| **Total** | **19** | | |

> 进度更新 2026-06-07：
> - 修复编排层两处问题——`PaymentOrchestrator.submitPayment` 改为真正调用
>   `ChannelAdapter.createDepositAddress`（原为硬编码地址），`handlePaymentCallback` 增加 `eventId` 去重。
> - 新增确认对账 + 过期调度（`PaymentReconciliationJob`），KeyGenerator 真实派生（ETH/TRON），
>   TronAdapter 真实的 height/confirmations/health 查询，Redis 幂等存储（可选）。
> - 工程化：GitHub Actions CI（`.github/workflows/ci.yml`，`mvn verify`）；测试 48 个全绿。
> 详见 git 历史与 `README.md` / `CLAUDE.md`。

> 进度更新 2026-06-13：
> - 完成 P1-4 执行层持久化：新增 `CryptoPaymentEntity` / `WalletEntity`、JPA 仓储适配器、
>   V4 Flyway 迁移和 `nexusflow.execution.persistence` 装配开关。
> - 修正所有 `reconstitute` Lombok builder 的 class name 冲突，避免持久化恢复时字段被新建构造器覆盖。
> - `mvn test` 通过；Testcontainers 用例因本机 Docker 不可用自动跳过。

> 进度更新 2026-06-13（P1 完成）：
> - 完成 `EthereumAdapter` ERC20 Transfer 扫描 / confirmations / block hash；完成 `BitcoinAdapter` Bitcoin Core JSON-RPC 扫块 / confirmations / health。
> - 完成 BIP39/BIP44 HD 钱包、BTC 地址派生、`MnemonicStore` 加密备份、地址池分配和低水位补充。
> - 完成扫描游标持久化、reorg 回退、支付 retry/backoff、链 RPC circuit breaker。
> - `mvn test` 通过；Testcontainers 用例因本机 Docker 不可用自动跳过。真实 ETH/BTC/TRON 节点仍需环境级联调验证。

> 测试补充 2026-06-14：
> - 新增 `EthereumAdapterTest`，覆盖 ERC20 Transfer log 解析、确认数计算和 block hash 查询。
> - 新增 `BlockchainScannerTest`，覆盖初始游标推进、交易分发、reorg rewind 和支付回滚调用。
