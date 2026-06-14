# NexusFlow Test Summary

Last verified: 2026-06-14 with `mvn -pl flow-api,flow-cashier -am test`.

## Current Result

| Total | Passed | Failed | Errors | Skipped |
|-------|--------|--------|--------|---------|
| 229 | 215 | 0 | 0 | 14 |

The 14 skipped tests are 6 `NexusFlowApplicationIT` Testcontainers cases, 3 opt-in live blockchain smoke tests, 2 opt-in live messaging smoke tests, 2 opt-in Coinbase Commerce smoke tests, and 1 opt-in live webhook delivery smoke test. They require Docker or explicit live dependency environment variables and are skipped automatically when unavailable.

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

# Run opt-in Coinbase Commerce smoke tests after setting LIVE_* variables
mvn test -pl flow-infra -Dtest=LiveCoinbaseCommerceAdapterTest

# Run opt-in outbound webhook smoke test after setting LIVE_* variables
mvn test -pl flow-infra -Dtest=LiveWebhookDeliveryTest
```

JUnit 5 support depends on `maven-surefire-plugin` 3.2.5, pinned in the root `pom.xml`.

`LiveBlockchainAdapterTest` uses `LIVE_ETH_RPC_URL`, `LIVE_BTC_RPC_URL`, and `LIVE_TRON_NODE_URL`.
Optional variables: `LIVE_ETH_USDT_CONTRACT`, `LIVE_TRON_USDT_CONTRACT`, `LIVE_BTC_RPC_USERNAME`,
`LIVE_BTC_RPC_PASSWORD`, `LIVE_ETH_TX_HASH`, `LIVE_BTC_TX_HASH`, and `LIVE_TRON_TX_HASH`.
`LiveMessagingInfrastructureTest` uses `LIVE_REDIS_HOST` / `LIVE_REDIS_PORT` and
`LIVE_KAFKA_BOOTSTRAP_SERVERS` / `LIVE_KAFKA_TOPIC`.
`LiveCoinbaseCommerceAdapterTest` uses `LIVE_COINBASE_COMMERCE_API_KEY` or
`COINBASE_COMMERCE_API_KEY`. Optional variables: `LIVE_COINBASE_COMMERCE_BASE_URL`,
`LIVE_COINBASE_COMMERCE_API_VERSION`, `LIVE_COINBASE_COMMERCE_TOKEN`,
`LIVE_COINBASE_COMMERCE_NETWORK`, `LIVE_COINBASE_COMMERCE_QUOTE`,
`LIVE_COINBASE_COMMERCE_CHARGE_TOKEN`, `LIVE_COINBASE_COMMERCE_CHARGE_NETWORK`, and
`LIVE_COINBASE_COMMERCE_CHARGE_AMOUNT`. The live charge test also requires
`LIVE_COINBASE_COMMERCE_CREATE_CHARGE=true`.
`LiveWebhookDeliveryTest` uses `LIVE_WEBHOOK_URL`. Optional variable:
`LIVE_WEBHOOK_SIGNING_SECRET`.

## Test Classes

| Module | Test class | Count | Coverage |
|--------|------------|-------|----------|
| `flow-common` | `AesGcmEncryptionTest` | 9 | AES-GCM key generation, encrypt/decrypt, invalid input handling |
| `flow-common` | `ApiResponseTest` | 3 | API success/failure envelope defaults and JSON serialization |
| `flow-domain` | `FlowStatusTest` | 7 | Payment flow status transitions |
| `flow-domain` | `OrderStatusTest` | 18 | Order status transitions, terminal-state semantics, refund retry path |
| `flow-domain` | `CryptoPaymentTest` | 5 | Crypto payment lifecycle and state transitions |
| `flow-domain` | `RefundStatusTest` | 7 | Refund status transitions |
| `flow-domain` | `ReconstituteBuilderTest` | 4 | Reconstitution builders preserve persisted aggregate fields, including wallet MPC provider ids |
| `flow-domain` | `MoneyTest` | 3 | Positive/zero/negative/null amount behavior and plain decimal rendering |
| `flow-application` | `BlockchainCircuitBreakerTest` | 1 | Circuit breaker opens after repeated chain RPC failures and recovers |
| `flow-application` | `OrphanTransactionApplicationServiceTest` | 5 | Orphan transaction listing, resolve, ignore, compensate, not-found behavior |
| `flow-application` | `OpsDashboardApplicationServiceTest` | 1 | Ops dashboard channel health, status counts, reconciliation summary, risk alerts |
| `flow-application` | `PaymentApplicationServiceTest` | 14 | Create payment, request/response idempotency, address-pool allocation, duplicate order rejection, dust/underpayment handling, orphan alerting and compensation |
| `flow-application` | `PaymentOrchestratorTest` | 18 | Fiat and crypto-denominated order creation, channel routing, refund flow, self-hosted refund gas budget event, callback idempotency |
| `flow-application` | `PaymentReconciliationJobTest` | 4 | Confirmation polling, expiry, retry/backoff |
| `flow-application` | `RequestDtoJsonTest` | 4 | Jackson deserialization for immutable create-payment, create-order, refund, and cashier-submit request DTOs |
| `flow-application` | `WebhookDeadLetterApplicationServiceTest` | 7 | Dead-letter listing, replay success/failure/exception handling, ignore, not-found, and closed-state rejection |
| `flow-application` | `WebhookServiceTest` | 5 | Merchant/execution callback payload filtering, SSRF blocking, and webhook dead-letter recording |
| `flow-infra` | `BitcoinAdapterTest` | 3 | Bitcoin Core RPC parsing, block scan, confirmations, failure behavior |
| `flow-infra` | `CoinbaseCommerceAdapterTest` | 6 | Coinbase Commerce REST charge/rate parsing, real-mode supported currencies, and no-key stub fallback |
| `flow-infra` | `DefaultChannelRouterTest` | 2 | Asset-support filtering before rate sorting and preferred-channel handling when the requested asset is unsupported |
| `flow-infra` | `EthereumAdapterTest` | 3 | ERC20 `Transfer` log parsing, confirmations, block hash lookup |
| `flow-infra` | `LiveBlockchainAdapterTest` | 3 skipped locally | Opt-in ETH/BTC/TRON live-node smoke checks for height, health, block hash where supported, one-block scan, and optional tx confirmations |
| `flow-infra` | `LiveCoinbaseCommerceAdapterTest` | 2 skipped locally | Opt-in Coinbase Commerce exchange-rate and guarded live-charge smoke checks |
| `flow-infra` | `SelfHostedNodeAdapterTest` | 5 | Self-hosted channel delegation to execution payments, refund task creation, and stablecoin rate behavior |
| `flow-infra` | `TronAdapterTest` | 8 | TronGrid response parsing, TRC20 Transfer event scanning, confirmations, health |
| `flow-infra` | `RedisCurrencyRateCacheTest` | 4 | Redis-backed exchange-rate and currency cache fallback behavior |
| `flow-infra` | `StaticGasEstimatorTest` | 4 | Static ETH/TRON/BTC gas and miner-fee estimates plus unsupported-chain rejection |
| `flow-infra` | `HttpWebhookClientTest` | 2 | Outbound webhook JSON delivery, HMAC signing, and injectable retry delays |
| `flow-infra` | `LiveWebhookDeliveryTest` | 1 skipped locally | Opt-in outbound webhook reachability smoke check against a configured HTTPS endpoint |
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
| `flow-api` | `BlockchainConfigTest` | 2 | Stubbed external BitMart/Binance channel beans are guarded out of the `prod` profile; Coinbase no-key stub is omitted in `prod` |
| `flow-api` | `PaymentControllerTest` | 6 | MockMvc HTTP contract for create/get/confirm/fail, idempotency headers, validation, and parameter binding |
| `flow-api` | `WebhookDeadLetterControllerTest` | 4 | Ops dead-letter list/replay/ignore HTTP contract and not-found response |
| `flow-api` | `NexusFlowApplicationIT` | 6 skipped locally | Spring Boot context, PostgreSQL Testcontainers, JPA round trips, persistence-backed `PaymentController` idempotency, concurrent address-pool allocation |

## Coverage by Area

| Area | Covered |
|------|---------|
| Domain state machines | `OrderStatus`, `FlowStatus`, `RefundStatus`, `CryptoPayment` lifecycle |
| Payment orchestration | Asset-aware channel routing, fiat and crypto-denominated create-order flows, Coinbase Commerce REST-capable channel with no-key stub fallback, BitMart/Binance stubs, self-hosted node deposit/refund delegation, refund flow, callback deduplication |
| Execution payments | Address allocation with row locking, Docker-backed concurrent allocation test, payment detection, underpayment/dust rules, confirmation reconciliation, merchant callback delivery, PaymentController HTTP contract |
| Persistence | Execution-layer JPA repositories, wallet persistence including optional MPC wallet ids, mnemonic backups, address pool mappings, idempotency keys, orphan transactions, webhook dead letters |
| Blockchain adapters | ETH/BTC mocked transport parsing; TRON height/confirmation parsing; opt-in live-node smoke tests; scanner reorg behavior |
| Gas abstraction | `GasEstimator` port, static ETH/TRON/BTC estimates, and self-hosted refund events carrying native gas budget fields |
| Wallet/key management | BIP39/BIP44 derivation, ETH/TRON/BTC address derivation, Base58Check, MPC signer port and wallet provider-id persistence |
| Reliability | Redis idempotency, opt-in Redis live smoke, persistent createPayment idempotency, Redis cache fallback, retry/backoff, blockchain circuit breaker, callback HMAC verification, outbound webhook HMAC/retry/SSRF/dead-letter replay/ignore workflow, opt-in outbound webhook live smoke, Kafka domain-event publishing, opt-in Kafka live smoke, orphan transaction deduplication/manual resolution/compensation, ops risk dashboard |
| API contracts | API envelope serialization, immutable request DTO JSON binding, MVC path/query parameter binding without `-parameters`, request validation for execution payment creation |
| Integration | PostgreSQL Testcontainers coverage includes Spring context/Flyway/JPA round trips, persistence-backed createPayment HTTP idempotency, and concurrent address allocation; opt-in live smoke coverage exists for blockchain nodes, Redis/Kafka, and Coinbase Commerce; needs Docker or live credentials to execute |

## Known Gaps

| Gap | Status |
|-----|--------|
| Docker-backed integration test run | Present but skipped locally without Docker |
| Live ETH/BTC/TRON node verification | Opt-in `LiveBlockchainAdapterTest` exists; local run skips it until `LIVE_ETH_RPC_URL`, `LIVE_BTC_RPC_URL`, and/or `LIVE_TRON_NODE_URL` are configured |
| TRON live block scanning | `scanNewBlocks()` parsing is unit-tested with mocked TronGrid responses; opt-in live smoke can exercise one-block scanning when `LIVE_TRON_NODE_URL` is set |
| `PaymentController` full persistence-backed HTTP E2E | PostgreSQL-backed createPayment idempotency is covered in `NexusFlowApplicationIT`; local run still skips it without Docker |
| Kafka broker integration | Opt-in `LiveMessagingInfrastructureTest` can write a smoke event to `LIVE_KAFKA_TOPIC`; local run skips it until `LIVE_KAFKA_BOOTSTRAP_SERVERS` is configured |
| Redis integration against a real Redis server | Opt-in `LiveMessagingInfrastructureTest` can verify Redis `SET NX EX`; local run skips it until `LIVE_REDIS_HOST` is configured |
| Coinbase Commerce live verification | Opt-in `LiveCoinbaseCommerceAdapterTest` can verify live exchange rates with `LIVE_COINBASE_COMMERCE_API_KEY` or `COINBASE_COMMERCE_API_KEY`; guarded charge creation requires `LIVE_COINBASE_COMMERCE_CREATE_CHARGE=true`; no-key stub remains non-prod only; webhook semantics and external refund operations still need environment verification |
| Production webhook reachability | Opt-in `LiveWebhookDeliveryTest` can POST a signed smoke payload to `LIVE_WEBHOOK_URL`; production replay policy and merchant endpoint behavior still need environment validation |
| BitMart/Binance production adapters | Stub implementations remain available only outside the `prod` profile; real REST adapters and live verification are still required before enabling those channels in production |
| Address-pool concurrent allocation | Docker-backed concurrent createPayment test covers distinct address assignment through `FOR UPDATE SKIP LOCKED`; local run still skips it without Docker |
| Missing-event catch-up live policy | Orphan transaction alerting, manual compensation, and configurable auto compensation exist; production auto-compensation policy still needs operator approval |
| Self-hosted node refund broadcast | Refund tasks and `crypto.refund.requested` events are emitted; chain signing/broadcast remains an external worker/live-environment responsibility |
| Live gas pricing | `StaticGasEstimator` provides configurable conservative defaults; live fee oracle integration and gas-bank funding automation are still pending |
| MPC provider integration | `MpcSigner` port and wallet `mpcWalletId` persistence exist; real provider adapters, signing workflow, and live signing smoke tests remain pending |

## Notes

- `NexusFlowApplicationIT` uses `@Testcontainers(disabledWithoutDocker = true)`, so local no-Docker runs can still show a green build while skipping integration coverage.
- `LiveBlockchainAdapterTest` runs only when live-node env vars are set. Optional tx confirmation checks use `LIVE_ETH_TX_HASH`, `LIVE_BTC_TX_HASH`, and `LIVE_TRON_TX_HASH`.
- `LiveMessagingInfrastructureTest` runs only when Redis/Kafka env vars are set. If Kafka auto topic creation is disabled, set `LIVE_KAFKA_TOPIC` to an existing writable topic.
- `LiveCoinbaseCommerceAdapterTest` runs only when Coinbase Commerce credentials are set. It never creates a live charge unless `LIVE_COINBASE_COMMERCE_CREATE_CHARGE=true`.
- `LiveWebhookDeliveryTest` runs only when `LIVE_WEBHOOK_URL` is set. It uses the production `HttpWebhookClient` path with a single fast attempt.
- Blockchain adapter tests validate request/response parsing and domain conversion. Without the live env vars, they do not prove behavior against real nodes or network-specific edge cases.
- `flow-cashier` has no Java tests; `mvn -pl flow-cashier test` verifies static resource packaging, including `checkout.html`, `merchant.html`, and `ops.html`.
- Roadmap status and production risks are tracked in `nexusflow-roadmap.md`, especially the "production preflight risk" section.
