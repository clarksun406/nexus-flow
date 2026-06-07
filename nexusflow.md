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

#### P0-1: TronAdapter — TRC20 Block Scanning — 🟡 PARTIAL

**File:** `flow-infra/.../blockchain/TronAdapter.java` (now backed by `TronGridClient` / `HttpTronGridClient`)

| Method | Status | Notes |
|--------|--------|-------|
| `scanNewBlocks()` | ⬜ explicit stub | TronGrid's TRC20 transfer API is account/timestamp-scoped and doesn't map onto the block-range abstraction; needs the event-info endpoint or full-node log decoding. Left documented rather than faked. |
| `getCurrentBlockHeight()` | ✅ done | `POST /wallet/getnowblock` → `block_header.raw_data.number` |
| `getConfirmations()` | ✅ done | `gettransactioninfobyid` → `blockNumber`, then `currentHeight - txBlock` |
| `isHealthy()` | ✅ done | healthy when block height > 0 |

Response parsing covered by `TronAdapterTest` (HTTP transport stubbed — not live-verified).
Remaining: implement real `scanNewBlocks` and verify all calls against a live TronGrid endpoint.

---

#### P0-2: KeyGenerator — Address Derivation — ✅ DONE (ETH/TRON)

**File:** `flow-wallet/.../wallet/KeyGenerator.java` (+ self-contained `Base58` util)

> Implemented with web3j EC/keccak: **ETH** = keccak256(pubkey)[12:] → EIP-55 checksummed hex;
> **TRON** = 0x41 ‖ keccak256(pubkey)[12:] → Base58Check. Verified against the secp256k1 private-key=1
> test vector (`KeyGeneratorTest`, `Base58Test`). **BTC/SOLANA** throw `UnsupportedOperationException`
> (BTC needs RIPEMD160 / bitcoinj, currently excluded from the build — do under P1).

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

#### P0-4: Webhook Callback to NexusPay-Core

**File:** `flow-application/.../application/PaymentApplicationService.java`

`CryptoPayment.callbackUrl` is stored but never invoked. After each state transition (DETECTED, CONFIRMED, FAILED, EXPIRED), POST the `PaymentStateChangedEvent` payload to `callbackUrl`.

**What to do:**
1. Create `WebhookClient` in `flow-infra` (RestTemplate with retry)
2. Call it after publishing domain events in `PaymentApplicationService`
3. Handle failures gracefully (async, retry with backoff, dead-letter queue)

---

#### P0-5: Idempotency Key Persistence

**DB migration already exists:** `flow-api/.../db/migration/V1__init_schema.sql` (`idempotency_keys` table)

**Problem:** No code reads/writes the `idempotency_keys` table. Currently only `existsByOrderId` on in-memory repo provides idempotency.

> 🟡 Partial: channel-callback idempotency is handled by `ProcessedEventStore` (port in `flow-domain`),
> with TWO impls — `InMemoryProcessedEventStore` (default) and `RedisProcessedEventStore`
> (`SET NX EX`, multi-instance safe), selected via `nexusflow.idempotency.store=memory|redis`
> (`RedisIdempotencyConfig`). Redis logic unit-tested with a mocked Jedis (`RedisProcessedEventStoreTest`);
> not yet verified against a live Redis. Still does NOT cover `createPayment` response caching or the
> `idempotency_keys` table.

**Remaining:**
1. ✅ Persistent, multi-instance dedup store (Redis) behind the existing `ProcessedEventStore` port
2. ⬜ Wrap `createPayment` with: check key → if exists return cached response → if not, execute and store
3. ⬜ Use the `idempotency_keys` table (or Redis) for full request/response idempotency, not just callbacks

---

#### P0-6: Payment Expiry Scheduler — ✅ DONE

> Implemented as `PaymentReconciliationJob.expireOverduePayments()` (`flow-application`,
> `@Scheduled`, TTL via `nexusflow.payment.expiry-minutes`, default 30 min). Queries PENDING
> payments past TTL and calls the new `PaymentApplicationService.expirePayment()` (own transaction
> per payment). Covered by `PaymentReconciliationJobTest`.
> Not yet done: firing the merchant webhook on expiry (see P0-4).

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
> NOTE: only produces real progress once a real `BlockchainAdapter` is wired (TronAdapter still
> returns 0 confirmations — see P0-1).

**Still to do (items 4–5 below):**
1. ✅ Periodically scan unconfirmed payments (DETECTED, CONFIRMING)
2. ✅ Re-query blockchain for latest confirmations via `BlockchainAdapter.getConfirmations()`
3. ✅ Update payment confirmations, transition to CONFIRMED as appropriate
4. ⬜ Transition to FAILED on reorg / failure detection
5. ⬜ Detect missing events (tx seen on-chain but no payment record) and create catch-up payments

---

### ═══════════════════════════════════════════
### 🟡 P1 — Phase 2: Multi-Chain & Production Hardening
### ═══════════════════════════════════════════

---

#### P1-1: EthereumAdapter — ERC20 Block Scanning

**File:** `flow-infra/.../blockchain/EthereumAdapter.java`

| Method | Status | What to do |
|--------|--------|-------------|
| `scanNewBlocks()` (L28-31) | Returns `Collections.emptyList()` | Filter ERC20 `Transfer` event logs by `usdtContractAddress`, parse to/from/amount, return `List<ScannedTransaction>` |
| `getConfirmations()` (L45-47) | Returns `0` | `currentBlock - txReceipt.blockNumber` |

---

#### P1-2: BitcoinAdapter — Full Implementation

**File:** `flow-infra/.../blockchain/BitcoinAdapter.java`

Entire class is a stub. **What to do:**
1. Integrate bitcoinj `PeerGroup` or connect to BTC RPC node
2. Implement `scanNewBlocks()`: iterate blocks, parse UTXO outputs, match to wallet addresses
3. Implement `getCurrentBlockHeight()`, `getConfirmations()`, `isHealthy()`

---

#### P1-3: BIP32/BIP44 HD Wallet

**File:** `flow-wallet/.../wallet/KeyGenerator.java` (L13)

**What to do:**
1. Replace random key generation with BIP39 mnemonic → BIP32 seed → BIP44 derivation path
2. Support derivation paths: `m/44'/60'/0'/0/0` (ETH), `m/44'/195'/0'/0/0` (TRON), `m/44'/0'/0'/0/0` (BTC)
3. Add `MnemonicStore` for secure seed phrase backup

---

#### P1-4: Persistence Layer — Replace InMemory Repositories

**Files:**
- `flow-infra/.../persistence/InMemoryPaymentRepository.java`
- `flow-infra/.../persistence/InMemoryWalletRepository.java`

**What to do:**
1. Create JPA entities mapping to `crypto_payments` and `wallets` tables (already defined in V1__init_schema.sql)
2. Implement `JpaPaymentRepository` and `JpaWalletRepository`
3. Add MyBatis-Plus alternative if preferred (already in parent POM)
4. Write integration tests with Testcontainers

---

#### P1-5: Address Pool Management

**What to do:**
1. Create `AddressPool` aggregate in `flow-domain`
2. Pre-generate N addresses per chain, mark as available/assigned
3. `PaymentApplicationService.createPayment()` pulls from pool instead of reusing single wallet address
4. Background job replenishes pool when low

---

#### P1-6: Retry & Chain Reorg Handling

**What to do:**
1. Detect chain reorgs: block hash mismatch on re-scan
2. Rollback affected payments to PENDING, re-detect on new chain tip
3. Add `retryCount` to `CryptoPayment`, exponential backoff for failed RPC calls
4. Circuit breaker pattern on blockchain node connections (Resilience4j)

---

### ═══════════════════════════════════════════
### 🟢 P2 — Phase 3: Advanced Features
### ═══════════════════════════════════════════

---

#### P2-1: Kafka Event Publishing

**File:** `flow-infra/.../event/SpringDomainEventPublisher.java` (L15)

**What to do:**
1. Create `KafkaDomainEventPublisher` implementing `DomainEventPublisher`
2. Publish to topics: `crypto.payment.detected`, `crypto.payment.confirmed`, `crypto.payment.failed`
3. Ensure at-least-once delivery with idempotent consumers on NexusPay-Core side

---

#### P2-2: MPC Wallet Integration

**What to do:**
1. Define `MpcSigner` port in `flow-domain`
2. Integrate with MPC provider (Fireblocks / Copper / custom MPC)
3. `Wallet` aggregate gains `mpcWalletId` field
4. Transaction signing flows through MPC instead of local private key

---

#### P2-3: Gas Abstraction

**What to do:**
1. `GasEstimator` port in `flow-domain`
2. Estimate gas for outgoing transactions (sweeping, refunds)
3. `GasBank` to pre-fund wallets with native tokens for gas
4. Monitor gas prices, batch transactions when gas is low

---

#### P2-4: On/Off Ramp Integration

**What to do:**
1. Integrate with fiat on/off ramp providers (MoonPay, Ramp, Banxa)
2. `FiatGateway` port in `flow-domain`
3. Handle fiat→crypto and crypto→fiat conversion tracking

---

### ═══════════════════════════════════════════
### 🔵 P3 — Testing
### ═══════════════════════════════════════════

---

#### P3-1: Unit Tests — 🟡 IN PROGRESS

> Added (27 tests, all passing): `CryptoPaymentTest` (state machine), `PaymentApplicationServiceTest`
> (detection matching + expiry), `PaymentOrchestratorTest` (channel address resolution + callback
> dedup), `PaymentReconciliationJobTest` (reconcile/expire), `InMemoryPaymentRepositoryTest`,
> `InMemoryProcessedEventStoreTest`. Surefire pinned to 3.2.5 so JUnit 5 actually runs.
> Still missing: `flow-common` (`AesGcmEncryption`, `ApiResponse`), orchestration JPA repos, `Money`.

**Remaining priority test targets:**

| Module | What to test |
|--------|-------------|
| `flow-domain` | `PaymentStatus` state machine transitions, `CryptoPayment` lifecycle, `Money` validation |
| `flow-common` | `AesGcmEncryption` encrypt/decrypt round-trip, `ApiResponse` builder |
| `flow-application` | `PaymentApplicationService` with mocked repositories, idempotency behavior |

---

#### P3-2: Integration Tests

| Module | What to test |
|--------|-------------|
| `flow-api` | `PaymentController` end-to-end with Testcontainers PostgreSQL + Redis |
| `flow-infra` | Repository CRUD against real database, `EthereumAdapter` against local Ganache/Hardhat node |
| `flow-listener` | `BlockchainScanner` scheduled execution with mocked adapter |

---

### 📊 Summary

| Priority | Count | Items | Status |
|----------|-------|-------|--------|
| P0 (MVP must-have) | 7 | TronAdapter, KeyGenerator, PaymentMatching, Webhook, Idempotency, Expiry, Reconciliation | ✅ KeyGenerator, PaymentMatching, Expiry · 🟡 TronAdapter, Reconciliation, Idempotency · ⬜ Webhook |
| P1 (Phase 2) | 6 | EthereumAdapter, BitcoinAdapter, HDWallet, JPA Persistence, AddressPool, Retry/Reorg | ⬜ all |
| P2 (Phase 3) | 4 | Kafka, MPC, GasAbstraction, OnOffRamp | ⬜ all |
| P3 (Testing) | 2 | Unit tests, Integration tests | 🟡 Unit tests (48, all green) · ⬜ Integration (needs Testcontainers/Docker) |
| **Total** | **19** | | |

> 进度更新 2026-06-07：
> - 修复编排层两处问题——`PaymentOrchestrator.submitPayment` 改为真正调用
>   `ChannelAdapter.createDepositAddress`（原为硬编码地址），`handlePaymentCallback` 增加 `eventId` 去重。
> - 新增确认对账 + 过期调度（`PaymentReconciliationJob`），KeyGenerator 真实派生（ETH/TRON），
>   TronAdapter 真实的 height/confirmations/health 查询，Redis 幂等存储（可选）。
> - 工程化：GitHub Actions CI（`.github/workflows/ci.yml`，`mvn verify`）；测试 48 个全绿。
> 详见 git 历史与 `README.md` / `CLAUDE.md`。