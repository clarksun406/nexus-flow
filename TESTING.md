# NexusFlow Test Summary

Last verified: 2026-06-14 with `mvn -pl flow-api,flow-cashier -am test`.

## Current Result

| Total | Passed | Failed | Errors | Skipped |
|-------|--------|--------|--------|---------|
| 216 | 205 | 0 | 0 | 11 |

The 11 skipped tests are 6 `NexusFlowApplicationIT` Testcontainers cases, 3 opt-in live blockchain smoke tests, and 2 opt-in live messaging smoke tests. They require Docker or explicit live dependency environment variables and are skipped automatically when unavailable.

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

# Run opt-in live node smoke tests after setting LIVE_* variables
mvn test -pl flow-infra -Dtest=LiveBlockchainAdapterTest

# Run opt-in Redis/Kafka smoke tests after setting LIVE_* variables
mvn test -pl flow-infra -Dtest=LiveMessagingInfrastructureTest
```

JUnit 5 support depends on `maven-surefire-plugin` 3.2.5, pinned in the root `pom.xml`.

`LiveBlockchainAdapterTest` uses `LIVE_ETH_RPC_URL`, `LIVE_BTC_RPC_URL`, and `LIVE_TRON_NODE_URL`.
Optional variables: `LIVE_ETH_USDT_CONTRACT`, `LIVE_TRON_USDT_CONTRACT`, `LIVE_BTC_RPC_USERNAME`,
`LIVE_BTC_RPC_PASSWORD`, `LIVE_ETH_TX_HASH`, `LIVE_BTC_TX_HASH`, and `LIVE_TRON_TX_HASH`.
`LiveMessagingInfrastructureTest` uses `LIVE_REDIS_HOST` / `LIVE_REDIS_PORT` and
`LIVE_KAFKA_BOOTSTRAP_SERVERS` / `LIVE_KAFKA_TOPIC`.

## Test Classes

| Module | Test class | Count | Coverage |
|--------|------------|-------|----------|
| `flow-common` | `AesGcmEncryptionTest` | 9 | AES-GCM key generation, encrypt/decrypt, invalid input handling |
| `flow-common` | `ApiResponseTest` | 3 | API success/failure envelope defaults and JSON serialization |
| `flow-domain` | `FlowStatusTest` | 7 | Payment flow status transitions |
| `flow-domain` | `OrderStatusTest` | 18 | Order status transitions, terminal-state semantics, refund retry path |
| `flow-domain` | `CryptoPaymentTest` | 5 | Crypto payment lifecycle and state transitions |
| `flow-domain` | `RefundStatusTest` | 7 | Refund status transitions |
| `flow-domain` | `ReconstituteBuilderTest` | 3 | Reconstitution builders preserve persisted aggregate fields |
| `flow-domain` | `MoneyTest` | 3 | Positive/zero/negative/null amount behavior and plain decimal rendering |
| `flow-application` | `BlockchainCircuitBreakerTest` | 1 | Circuit breaker opens after repeated chain RPC failures and recovers |
| `flow-application` | `OrphanTransactionApplicationServiceTest` | 5 | Orphan transaction listing, resolve, ignore, compensate, not-found behavior |
| `flow-application` | `OpsDashboardApplicationServiceTest` | 1 | Ops dashboard channel health, status counts, reconciliation summary, risk alerts |
| `flow-application` | `PaymentApplicationServiceTest` | 14 | Create payment, request/response idempotency, address-pool allocation, duplicate order rejection, dust/underpayment handling, orphan alerting and compensation |
| `flow-application` | `PaymentOrchestratorTest` | 17 | Fiat and crypto-denominated order creation, channel routing, refund flow, callback idempotency |
| `flow-application` | `PaymentReconciliationJobTest` | 4 | Confirmation polling, expiry, retry/backoff |
| `flow-application` | `RequestDtoJsonTest` | 4 | Jackson deserialization for immutable create-payment, create-order, refund, and cashier-submit request DTOs |
| `flow-application` | `WebhookDeadLetterApplicationServiceTest` | 7 | Dead-letter listing, replay success/failure/exception handling, ignore, not-found, and closed-state rejection |
| `flow-application` | `WebhookServiceTest` | 5 | Merchant/execution callback payload filtering, SSRF blocking, and webhook dead-letter recording |
| `flow-infra` | `BitcoinAdapterTest` | 3 | Bitcoin Core RPC parsing, block scan, confirmations, failure behavior |
| `flow-infra` | `CoinbaseCommerceAdapterTest` | 6 | Coinbase Commerce REST charge/rate parsing, real-mode supported currencies, and no-key stub fallback |
| `flow-infra` | `DefaultChannelRouterTest` | 2 | Asset-support filtering before rate sorting and preferred-channel handling when the requested asset is unsupported |
| `flow-infra` | `EthereumAdapterTest` | 3 | ERC20 `Transfer` log parsing, confirmations, block hash lookup |
| `flow-infra` | `LiveBlockchainAdapterTest` | 3 skipped locally | Opt-in ETH/BTC/TRON live-node smoke checks for height, health, block hash where supported, one-block scan, and optional tx confirmations |
| `flow-infra` | `SelfHostedNodeAdapterTest` | 5 | Self-hosted channel delegation to execution payments, refund task creation, and stablecoin rate behavior |
| `flow-infra` | `TronAdapterTest` | 8 | TronGrid response parsing, TRC20 Transfer event scanning, confirmations, health |
| `flow-infra` | `RedisCurrencyRateCacheTest` | 4 | Redis-backed exchange-rate and currency cache fallback behavior |
| `flow-infra` | `InMemoryProcessedEventStoreTest` | 3 | In-memory callback idempotency |
| `flow-infra` | `KafkaDomainEventPublisherTest` | 1 | Kafka domain-event envelope, eventId key, and event-type topic routing |
| `flow-infra` | `LiveMessagingInfrastructureTest` | 2 skipped locally | Opt-in Redis SET-NX idempotency and Kafka producer smoke checks against live dependencies |
| `flow-infra` | `RedisProcessedEventStoreTest` | 3 | Redis `SET NX EX` callback idempotency behavior |
| `flow-infra` | `InMemoryPaymentRepositoryTest` | 6 | In-memory payment repository matching and lookup |
| `flow-infra` | `JpaAddressPoolRepositoryTest` | 2 | Address pool JPA mapping and available-address lookup |
| `flow-infra` | `JpaMnemonicStoreTest` | 2 | Encrypted mnemonic backup persistence mapping |
| `flow-infra` | `JpaPaymentIdempotencyStoreTest` | 5 | Persistent createPayment idempotency reserve/replay/delete behavior |
| `flow-infra` | `JpaOrphanTransactionRepositoryTest` | 3 | Orphan transaction JPA mapping and lookup |
| `flow-infra` | `JpaPaymentRepositoryTest` | 4 | Crypto payment JPA mapping, round-trip fields, status queries |
| `flow-infra` | `JpaWebhookDeadLetterStoreTest` | 3 | Failed webhook dead-letter JPA mapping, status lookup, and recent-item lookup bounds |
| `flow-infra` | `JpaWalletRepositoryTest` | 4 | Wallet JPA mapping, active-wallet lookup |
| `flow-listener` | `BlockchainScannerTest` | 2 | Scanner cursor advance, transaction dispatch, reorg rewind and rollback |
| `flow-wallet` | `Base58Test` | 5 | Base58/Base58Check encoding |
| `flow-wallet` | `KeyGeneratorTest` | 7 | BIP39/BIP44 derivation and ETH/TRON/BTC address derivation |
| `flow-api` | `CallbackHmacFilterTest` | 1 | Callback HMAC verification keeps request body readable downstream |
| `flow-api` | `PaymentControllerTest` | 6 | MockMvc HTTP contract for create/get/confirm/fail, idempotency headers, validation, and parameter binding |
| `flow-api` | `WebhookDeadLetterControllerTest` | 4 | Ops dead-letter list/replay/ignore HTTP contract and not-found response |
| `flow-api` | `NexusFlowApplicationIT` | 6 skipped locally | Spring Boot context, PostgreSQL Testcontainers, JPA round trips, persistence-backed `PaymentController` idempotency, concurrent address-pool allocation |

## Coverage by Area

| Area | Covered |
|------|---------|
| Domain state machines | `OrderStatus`, `FlowStatus`, `RefundStatus`, `CryptoPayment` lifecycle |
| Payment orchestration | Asset-aware channel routing, fiat and crypto-denominated create-order flows, Coinbase Commerce REST-capable channel with no-key stub fallback, BitMart/Binance stubs, self-hosted node deposit/refund delegation, refund flow, callback deduplication |
| Execution payments | Address allocation with row locking, Docker-backed concurrent allocation test, payment detection, underpayment/dust rules, confirmation reconciliation, merchant callback delivery, PaymentController HTTP contract |
| Persistence | Execution-layer JPA repositories, wallet persistence, mnemonic backups, address pool mappings, idempotency keys, orphan transactions, webhook dead letters |
| Blockchain adapters | ETH/BTC mocked transport parsing; TRON height/confirmation parsing; opt-in live-node smoke tests; scanner reorg behavior |
| Wallet/key management | BIP39/BIP44 derivation, ETH/TRON/BTC address derivation, Base58Check |
| Reliability | Redis idempotency, opt-in Redis live smoke, persistent createPayment idempotency, Redis cache fallback, retry/backoff, blockchain circuit breaker, callback HMAC verification, outbound webhook HMAC/retry/SSRF/dead-letter replay/ignore workflow, Kafka domain-event publishing, opt-in Kafka live smoke, orphan transaction deduplication/manual resolution/compensation, ops risk dashboard |
| API contracts | API envelope serialization, immutable request DTO JSON binding, MVC path/query parameter binding without `-parameters`, request validation for execution payment creation |
| Integration | PostgreSQL Testcontainers coverage includes Spring context/Flyway/JPA round trips, persistence-backed createPayment HTTP idempotency, and concurrent address allocation; needs Docker to execute |

## Known Gaps

| Gap | Status |
|-----|--------|
| Docker-backed integration test run | Present but skipped locally without Docker |
| Live ETH/BTC/TRON node verification | Opt-in `LiveBlockchainAdapterTest` exists; local run skips it until `LIVE_ETH_RPC_URL`, `LIVE_BTC_RPC_URL`, and/or `LIVE_TRON_NODE_URL` are configured |
| TRON live block scanning | `scanNewBlocks()` parsing is unit-tested with mocked TronGrid responses; opt-in live smoke can exercise one-block scanning when `LIVE_TRON_NODE_URL` is set |
| `PaymentController` full persistence-backed HTTP E2E | PostgreSQL-backed createPayment idempotency is covered in `NexusFlowApplicationIT`; local run still skips it without Docker |
| Kafka broker integration | Opt-in `LiveMessagingInfrastructureTest` can write a smoke event to `LIVE_KAFKA_TOPIC`; local run skips it until `LIVE_KAFKA_BOOTSTRAP_SERVERS` is configured |
| Redis integration against a real Redis server | Opt-in `LiveMessagingInfrastructureTest` can verify Redis `SET NX EX`; local run skips it until `LIVE_REDIS_HOST` is configured |
| Coinbase Commerce live verification | REST charge/rate paths are unit-tested with mocked transport and can be enabled with `COINBASE_COMMERCE_API_KEY`; live credentials, webhook semantics, and external refund operations still need environment verification |
| Address-pool concurrent allocation | Docker-backed concurrent createPayment test covers distinct address assignment through `FOR UPDATE SKIP LOCKED`; local run still skips it without Docker |
| Missing-event catch-up live policy | Orphan transaction alerting, manual compensation, and configurable auto compensation exist; production auto-compensation policy still needs operator approval |
| Self-hosted node refund broadcast | Refund tasks and `crypto.refund.requested` events are emitted; chain signing/broadcast remains an external worker/live-environment responsibility |

## Notes

- `NexusFlowApplicationIT` uses `@Testcontainers(disabledWithoutDocker = true)`, so local no-Docker runs can still show a green build while skipping integration coverage.
- `LiveBlockchainAdapterTest` runs only when live-node env vars are set. Optional tx confirmation checks use `LIVE_ETH_TX_HASH`, `LIVE_BTC_TX_HASH`, and `LIVE_TRON_TX_HASH`.
- `LiveMessagingInfrastructureTest` runs only when Redis/Kafka env vars are set. If Kafka auto topic creation is disabled, set `LIVE_KAFKA_TOPIC` to an existing writable topic.
- Blockchain adapter tests validate request/response parsing and domain conversion. Without the live env vars, they do not prove behavior against real nodes or network-specific edge cases.
- `flow-cashier` has no Java tests; `mvn -pl flow-cashier test` verifies static resource packaging, including `checkout.html`, `merchant.html`, and `ops.html`.
- Roadmap status and production risks are tracked in `nexusflow-roadmap.md`, especially the "production preflight risk" section.
