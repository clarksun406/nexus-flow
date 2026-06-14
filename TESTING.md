# NexusFlow Test Summary

Last verified: 2026-06-14 with `mvn -pl flow-api,flow-cashier -am test`.

## Current Result

| Total | Passed | Failed | Errors | Skipped |
|-------|--------|--------|--------|---------|
| 172 | 168 | 0 | 0 | 4 |

The 4 skipped tests are `NexusFlowApplicationIT` Testcontainers cases. They require a working Docker environment and are skipped automatically when Docker is unavailable.

## How to Run

```bash
# Run all tests
mvn test

# Compile without tests
mvn -DskipTests compile

# Run a module
mvn test -pl flow-application

# Run one test class
mvn test -pl flow-application -Dtest=PaymentOrchestratorTest

# Run one test method
mvn test -pl flow-domain -Dtest=CryptoPaymentTest#fullHappyPathTransitionsToConfirmed
```

JUnit 5 support depends on `maven-surefire-plugin` 3.2.5, pinned in the root `pom.xml`.

## Test Classes

| Module | Test class | Count | Coverage |
|--------|------------|-------|----------|
| `flow-common` | `AesGcmEncryptionTest` | 9 | AES-GCM key generation, encrypt/decrypt, invalid input handling |
| `flow-domain` | `FlowStatusTest` | 7 | Payment flow status transitions |
| `flow-domain` | `OrderStatusTest` | 18 | Order status transitions, terminal-state semantics, refund retry path |
| `flow-domain` | `CryptoPaymentTest` | 5 | Crypto payment lifecycle and state transitions |
| `flow-domain` | `RefundStatusTest` | 7 | Refund status transitions |
| `flow-domain` | `ReconstituteBuilderTest` | 3 | Reconstitution builders preserve persisted aggregate fields |
| `flow-application` | `BlockchainCircuitBreakerTest` | 1 | Circuit breaker opens after repeated chain RPC failures and recovers |
| `flow-application` | `OrphanTransactionApplicationServiceTest` | 5 | Orphan transaction listing, resolve, ignore, compensate, not-found behavior |
| `flow-application` | `OpsDashboardApplicationServiceTest` | 1 | Ops dashboard channel health, status counts, reconciliation summary, risk alerts |
| `flow-application` | `PaymentApplicationServiceTest` | 14 | Create payment, request/response idempotency, address-pool allocation, duplicate order rejection, dust/underpayment handling, orphan alerting and compensation |
| `flow-application` | `PaymentOrchestratorTest` | 17 | Fiat and crypto-denominated order creation, channel routing, refund flow, callback idempotency |
| `flow-application` | `PaymentReconciliationJobTest` | 4 | Confirmation polling, expiry, retry/backoff |
| `flow-application` | `WebhookServiceTest` | 3 | Execution-layer merchant callback payload filtering and delivery |
| `flow-infra` | `BitcoinAdapterTest` | 3 | Bitcoin Core RPC parsing, block scan, confirmations, failure behavior |
| `flow-infra` | `CoinbaseCommerceAdapterTest` | 3 | Coinbase Commerce stub deposit address, supported currencies, exchange-rate quote |
| `flow-infra` | `EthereumAdapterTest` | 3 | ERC20 `Transfer` log parsing, confirmations, block hash lookup |
| `flow-infra` | `SelfHostedNodeAdapterTest` | 5 | Self-hosted channel delegation to execution payments, refund task creation, and stablecoin rate behavior |
| `flow-infra` | `TronAdapterTest` | 8 | TronGrid response parsing, TRC20 Transfer event scanning, confirmations, health |
| `flow-infra` | `RedisCurrencyRateCacheTest` | 4 | Redis-backed exchange-rate and currency cache fallback behavior |
| `flow-infra` | `InMemoryProcessedEventStoreTest` | 3 | In-memory callback idempotency |
| `flow-infra` | `KafkaDomainEventPublisherTest` | 1 | Kafka domain-event envelope, eventId key, and event-type topic routing |
| `flow-infra` | `RedisProcessedEventStoreTest` | 3 | Redis `SET NX EX` callback idempotency behavior |
| `flow-infra` | `InMemoryPaymentRepositoryTest` | 6 | In-memory payment repository matching and lookup |
| `flow-infra` | `JpaAddressPoolRepositoryTest` | 2 | Address pool JPA mapping and available-address lookup |
| `flow-infra` | `JpaMnemonicStoreTest` | 2 | Encrypted mnemonic backup persistence mapping |
| `flow-infra` | `JpaPaymentIdempotencyStoreTest` | 5 | Persistent createPayment idempotency reserve/replay/delete behavior |
| `flow-infra` | `JpaOrphanTransactionRepositoryTest` | 3 | Orphan transaction JPA mapping and lookup |
| `flow-infra` | `JpaPaymentRepositoryTest` | 4 | Crypto payment JPA mapping, round-trip fields, status queries |
| `flow-infra` | `JpaWalletRepositoryTest` | 4 | Wallet JPA mapping, active-wallet lookup |
| `flow-listener` | `BlockchainScannerTest` | 2 | Scanner cursor advance, transaction dispatch, reorg rewind and rollback |
| `flow-wallet` | `Base58Test` | 5 | Base58/Base58Check encoding |
| `flow-wallet` | `KeyGeneratorTest` | 7 | BIP39/BIP44 derivation and ETH/TRON/BTC address derivation |
| `flow-api` | `CallbackHmacFilterTest` | 1 | Callback HMAC verification keeps request body readable downstream |
| `flow-api` | `NexusFlowApplicationIT` | 4 skipped locally | Spring Boot context, PostgreSQL Testcontainers, JPA round trips |

## Coverage by Area

| Area | Covered |
|------|---------|
| Domain state machines | `OrderStatus`, `FlowStatus`, `RefundStatus`, `CryptoPayment` lifecycle |
| Payment orchestration | Channel routing, fiat and crypto-denominated create-order flows, Coinbase/BitMart/Binance stubs, self-hosted node deposit/refund delegation, refund flow, callback deduplication |
| Execution payments | Address allocation with row locking, payment detection, underpayment/dust rules, confirmation reconciliation, merchant callback delivery |
| Persistence | Execution-layer JPA repositories, wallet persistence, mnemonic backups, address pool mappings, idempotency keys, orphan transactions |
| Blockchain adapters | ETH/BTC mocked transport parsing; TRON height/confirmation parsing; scanner reorg behavior |
| Wallet/key management | BIP39/BIP44 derivation, ETH/TRON/BTC address derivation, Base58Check |
| Reliability | Redis idempotency, persistent createPayment idempotency, Redis cache fallback, retry/backoff, blockchain circuit breaker, callback HMAC verification, Kafka domain-event publishing, orphan transaction deduplication/manual resolution/compensation, ops risk dashboard |
| Integration | PostgreSQL Testcontainers test class exists but needs Docker to execute |

## Known Gaps

| Gap | Status |
|-----|--------|
| Docker-backed integration test run | Present but skipped locally without Docker |
| Live ETH/BTC/TRON node verification | Not covered; adapter tests use mocked transports or parsed HTTP responses |
| TRON live block scanning | `scanNewBlocks()` parsing is unit-tested with mocked TronGrid responses; not yet live-verified against TronGrid/full node |
| `PaymentController` end-to-end API tests | Not yet covered with HTTP-level integration tests |
| Kafka broker integration | Publisher payload and topic routing are unit-tested with a mocked `KafkaTemplate`; no live Kafka broker test yet |
| Redis integration against a real Redis server | Cache/idempotency tests use mocked clients |
| Address-pool concurrent allocation | Repository uses PostgreSQL `FOR UPDATE SKIP LOCKED`; still worth stress testing against a real database |
| Missing-event catch-up live policy | Orphan transaction alerting, manual compensation, and configurable auto compensation exist; production auto-compensation policy still needs operator approval |
| Self-hosted node refund broadcast | Refund tasks and `crypto.refund.requested` events are emitted; chain signing/broadcast remains an external worker/live-environment responsibility |
| `Money` and `ApiResponse` edge cases | Still worth adding focused unit tests |

## Notes

- `NexusFlowApplicationIT` uses `@Testcontainers(disabledWithoutDocker = true)`, so local no-Docker runs can still show a green build while skipping integration coverage.
- Blockchain adapter tests validate request/response parsing and domain conversion. They do not prove behavior against real nodes or network-specific edge cases.
- `flow-cashier` has no Java tests; `mvn -pl flow-cashier test` verifies static resource packaging, including `checkout.html`, `merchant.html`, and `ops.html`.
- Roadmap status and production risks are tracked in `nexusflow-roadmap.md`, especially the "production preflight risk" section.
