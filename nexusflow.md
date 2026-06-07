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

#### P0-1: TronAdapter — TRC20 Block Scanning

**File:** `flow-infra/.../blockchain/TronAdapter.java`

| Method | Status | What to do |
|--------|--------|-------------|
| `scanNewBlocks()` (L30) | Returns `Collections.emptyList()` | Call TronGrid `/v1/blocks?limit=N`, parse transactions, filter by `usdtContractAddress`, return `List<ScannedTransaction>` |
| `getCurrentBlockHeight()` (L40) | Returns `0` | Call `/wallet/getnowblock`, extract block number |
| `getConfirmations()` (L46) | Returns `0` | `currentBlockHeight - txBlockNumber` |
| `isHealthy()` (L52) | Returns `true` always | Ping node endpoint, timeout-based health check |

**Dependencies:** HTTP client (RestTemplate / WebClient / OkHttp) to call TronGrid API.

---

#### P0-2: KeyGenerator — Address Derivation

**File:** `flow-wallet/.../wallet/KeyGenerator.java`

| Method | Status | What to do |
|--------|--------|-------------|
| `deriveAddress()` (L35-42) | Placeholder: returns `"0x" + key[0:40]` | Implement chain-specific EC key→address derivation: **Tron** = ECKey → SHA3 → base58check; **ETH** = ECKey → pubkey → keccak256 → last 20 bytes → hex; **BTC** = ECKey → pubkey → SHA256+RIPEMD160 → base58check |

**Dependencies:** BouncyCastle or web3j crypto utilities.

---

#### P0-3: PaymentApplicationService — onPaymentDetected (Payment Matching)

**File:** `flow-application/.../application/PaymentApplicationService.java` (L72-87)

**Problem:** The method only logs incoming transactions; it never matches them to specific payments or transitions state.

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

**What to do:**
1. Create `IdempotencyStore` interface in `flow-domain`
2. Implement in `flow-infra` using Redis (TTL-based) or PostgreSQL
3. Wrap `createPayment` with: check key → if exists return cached response → if not, execute and store

---

#### P0-6: Payment Expiry Scheduler

**File:** `flow-domain/.../domain/payment/CryptoPayment.java` (`markExpired()` at L93)

**Problem:** `EXPIRED` state and `markExpired()` exist, but nothing ever triggers them.

**What to do:**
1. Create `PaymentExpiryJob` in `flow-listener` with `@Scheduled`
2. Query payments in `PENDING` status older than configurable TTL (e.g. 30 min)
3. Call `payment.markExpired()`, save, publish events, fire webhook

---

#### P0-7: Reconciliation Job

Per init.md (L493-504): "Blockchain is source of truth, system is derived state."

**What to do:**
1. Create `ReconciliationJob` in `flow-listener`
2. Periodically scan unconfirmed payments (DETECTED, CONFIRMING)
3. Re-query blockchain for latest confirmations via `BlockchainAdapter.getConfirmations()`
4. Update payment confirmations, transition to CONFIRMED/FAILED as appropriate
5. Detect missing events (tx seen on-chain but no payment record) and create catch-up payments

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

#### P3-1: Unit Tests

No tests exist. **Priority test targets:**

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

| Priority | Count | Items |
|----------|-------|-------|
| P0 (MVP must-have) | 7 | TronAdapter, KeyGenerator, PaymentMatching, Webhook, Idempotency, Expiry, Reconciliation |
| P1 (Phase 2) | 6 | EthereumAdapter, BitcoinAdapter, HDWallet, JPA Persistence, AddressPool, Retry/Reorg |
| P2 (Phase 3) | 4 | Kafka, MPC, GasAbstraction, OnOffRamp |
| P3 (Testing) | 2 | Unit tests, Integration tests |
| **Total** | **19** | |