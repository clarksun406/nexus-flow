# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
mvn -DskipTests compile        # compile all modules
mvn test                       # run all unit tests
mvn install                    # full build + install to local repo

# Run a single test class or method (surefire)
mvn test -pl flow-application -Dtest=PaymentOrchestratorTest
mvn test -pl flow-domain -Dtest=CryptoPaymentTest#fullHappyPathTransitionsToConfirmed

# Run the API (needs local PostgreSQL `nexusflow` DB + Redis)
mvn -pl flow-api spring-boot:run
```

`-pl <module>` targets one Maven module; add `-am` to also build its dependencies.

**Surefire gotcha:** this project does NOT use `spring-boot-starter-parent`, so the
default surefire (2.12.4) would silently run zero JUnit 5 tests. `maven-surefire-plugin`
is pinned to 3.2.5 in the root `pom.xml` `pluginManagement` - keep it there or tests stop running.

**Spring MVC gotcha:** the compiler is not configured with `-parameters`, so controller
annotations must use explicit names, e.g. `@PathVariable("paymentId")` and
`@RequestParam("status")`. Implicit names can fail at runtime with HTTP 500.

CI (`.github/workflows/ci.yml`) runs `mvn -B verify` on push/PR to `main`.

## Architecture

NexusFlow is a **DDD modular monolith** for crypto payments with **two distinct payment layers**
that must not be conflated:

- **Execution layer** (`domain/payment`, `domain/wallet`, `infra/blockchain`) - on-chain
  payments. `CryptoPayment` aggregate, talks to blockchain nodes via `BlockchainAdapter`
  (ETH/TRON/BTC). Counterparty = the chain.
- **Orchestration layer** (`domain/order`, `domain/refund`, `domain/channel`,
  `application/PaymentOrchestrator`) - merchant fiat/crypto orders. `PaymentOrder` aggregate,
  talks to acquiring channels (exchanges/PSPs) via `ChannelAdapter`. Counterparty = the channel.

These have **separate aggregates, state machines, and repositories**. Self-hosting nodes is wired
as the optional `SELF_HOSTED_NODE` channel: when `nexusflow.self-hosted-channel.enabled=true`, an
orchestration `PaymentOrder` can delegate deposit-address creation down to an execution
`CryptoPayment`. Refunds create deterministic processing tasks and emit
`crypto.refund.requested`; an external signer/worker or live node integration must broadcast
the outbound chain transaction before refund callbacks mark success/failure.

### Module dependency direction (strict)

```text
flow-api / flow-listener
flow-application        -> flow-domain -> flow-common
flow-infra / flow-wallet
```

`flow-domain` depends only on `flow-common` - never on infrastructure. Ports (interfaces) live
in `flow-domain` (e.g. `PaymentRepository`, `ChannelAdapter`, `DomainEventPublisher`,
`ProcessedEventStore`); their implementations live in `flow-infra`. To add a capability the
domain needs, define the port in `flow-domain` and implement it in `flow-infra`.

### Domain events + state machines (the core pattern)

State changes flow through three coupled mechanisms - replicate this when adding transitions:

1. **State machines are enums** (`PaymentStatus`, `OrderStatus`, `FlowStatus`, `RefundStatus`)
   with an explicit `ALLOWED` set and `requireTransitionTo(target)` that throws
   `InvalidStateTransitionException` on illegal/backward moves. Never mutate status directly.
2. **Aggregates buffer domain events.** Every `markXxx()` transition appends a `DomainEvent`
   to an internal list. The aggregate does NOT publish.
3. **Application services drain and publish.** After `repository.save(aggregate)`, the service
   calls `aggregate.collectEvents()` (which clears the buffer) and pushes each to
   `DomainEventPublisher`. `collectEvents()` is single-shot - call it once per unit of work.

`DomainEventPublisher` is configurable: `SpringDomainEventPublisher` is the default in-process
publisher, and `KafkaDomainEventPublisher` is enabled with `EVENT_PUBLISHER=kafka`. Kafka publishing
routes each event to its `eventType()` topic and uses `eventId` as the message key.

### Idempotency (required on all inbound paths)

- **Order creation**: dedup by `(merchantId, merchantOrderNo)` / `orderId`. `/pay/order`
  accepts either fiat-denominated `amountFiat`/`currencyFiat` or crypto-denominated
  `amountCrypto`/`currencyCrypto`/`network`; do not mix both amount modes.
- **Channel callbacks**: dedup by `eventId` via `ProcessedEventStore.markProcessed(eventId)`
  (returns `false` if already seen). Two impls selected by `nexusflow.idempotency.store`:
  `InMemoryProcessedEventStore` (default) and `RedisProcessedEventStore` (`SET NX EX`, wired by
  `RedisIdempotencyConfig` when `=redis`). In `PaymentOrchestrator.handlePaymentCallback` the dedup
  runs **after** the order lookup on purpose - so a callback for a not-yet-existing order
  throws without permanently consuming the `eventId`, letting a legitimate retry succeed.
- **On-chain detection**: dedup by `txHash` before matching.
- **Execution createPayment**: accepts `Idempotency-Key` / `X-Idempotency-Key`, fingerprints the
  request, reserves/completes `idempotency_keys`, and replays the cached `PaymentResponse` for
  matching retries. A reused key with different parameters is rejected.

### Phase-1 implementation status

Tracked in `nexusflow-roadmap.md` and the implementation roadmap section of `nexusflow.md`:

- Repositories: execution-layer payments/wallets now use JPA by default
  (`nexusflow.execution.persistence=jpa`) via `JpaPaymentRepository` / `JpaWalletRepository`.
  `InMemoryPaymentRepository` and `InMemoryWalletRepository` are opt-in only with
  `nexusflow.execution.persistence=memory`.
- `TronAdapter`: `getCurrentBlockHeight`/`getConfirmations`/`isHealthy` are real (via `TronGridClient`,
  parsing unit-tested). `scanNewBlocks` uses TronGrid contract events to scan confirmed TRC20
  `Transfer` events by block and convert them to `ScannedTransaction`. `LiveBlockchainAdapterTest`
  provides opt-in smoke coverage when `LIVE_TRON_NODE_URL` is configured; it is skipped locally
  unless a live node is provided.
- `EthereumAdapter` implements ERC20 Transfer log scanning / confirmations / block hash via web3j.
  `BitcoinAdapter` implements Bitcoin Core JSON-RPC block scanning / confirmations / health. Both
  are unit-tested with mocked transports; `LiveBlockchainAdapterTest` can exercise them against
  `LIVE_ETH_RPC_URL` and `LIVE_BTC_RPC_URL`, but those live checks still need to be run before production.
- `KeyGenerator` uses BIP39/BIP44 HD derivation for ETH/TRON/BTC; SOLANA remains unsupported.
- `Wallet` can carry an optional `mpcWalletId`, persisted in `wallets.mpc_wallet_id`. `MpcSigner`
  is the domain port for external provider signing; an opt-in custom HTTP signer adapter is
  available with `MPC_HTTP_SIGN_URL` / `MPC_HTTP_API_KEY`. Fireblocks/Copper-specific adapters,
  transaction-signing workflow integration, and live signing smoke tests remain pending.
- `createPayment` now allocates from `AddressPoolEntry`. Configure `ADDRESS_POOL_SEED_MNEMONIC`
  to let `AddressPoolProvisioningService` replenish addresses; without it, the pool must be seeded
  manually or payment creation will fail with `ADDRESS_NOT_AVAILABLE`. JPA address allocation uses
  PostgreSQL `FOR UPDATE SKIP LOCKED` to avoid handing the same available address to concurrent
  payment creations.
- Execution-layer `CryptoPayment.callbackUrl` delivery is wired through `WebhookService` after
  payment state changes. The initial CREATED->PENDING setup event is not sent; DETECTED,
  CONFIRMING/CONFIRMED, FAILED, EXPIRED, and reorg rollback events use the shared webhook retry,
  HMAC signing, and SSRF protections. `@EnableAsync` is enabled in `NexusFlowApplication`; exhausted
  retries or SSRF-blocked deliveries are recorded in `webhook_dead_letters` via
  `WebhookDeadLetterStore` / `JpaWebhookDeadLetterStore`. Operators can query, replay, or ignore
  entries through `/ops/webhook-dead-letters` and the static ops dashboard.
  `LiveWebhookDeliveryTest` can exercise the real outbound `HttpWebhookClient` against
  `LIVE_WEBHOOK_URL` with optional `LIVE_WEBHOOK_SIGNING_SECRET`.
- `SELF_HOSTED_NODE` is a real `ChannelAdapter` backed by `PaymentApplicationService.createPayment`.
  It is disabled by default and currently supports USDT on TRC20/ERC20 with a USD/USDT parity rate.
  Refunds create deterministic `SELF_HOSTED_NODE_REFUND_*` processing tasks and publish
  `crypto.refund.requested` with a static native gas budget from the `GasEstimator` port; chain
  signing/broadcast is expected to be handled by an external worker or live node integration before
  refund callbacks mark success/failure.
  Its internal callback HMAC secret defaults to `WEBHOOK_HMAC_SECRET` via
  `CALLBACK_HMAC_SECRET_SELF_HOSTED_NODE`.
- Gas abstraction has a domain `GasEstimator` port, an infra `StaticGasEstimator` bean, GasBank
  policy objects for native-balance thresholds, low/high gas price bands, and a
  `GasBankFundingService` that can trigger immediate or low-gas batch top-ups against a configured
  `GasBank` implementation. Defaults are configurable under `nexusflow.gas.*`; live gas oracle,
  a real gas-bank funding adapter, alerting, and production scheduling remain follow-up work.
- Fiat on/off ramp core is represented by the `FiatGateway` port plus `FiatRampOrder` conversion
  tracking for fiat transfer ids, crypto tx hashes, and provider checkout/order ids. JPA persistence
  uses `fiat_ramp_orders`; `FiatRampApplicationService` and `/fiat/ramp` expose merchant quote,
  create-order, and get-order flows behind API key auth. `/callback/{gatewayId}/fiat-ramp` reuses
  callback HMAC verification for normalized provider status updates. Real MoonPay/Ramp/Banxa
  adapters, KYC, provider-specific payload/signature semantics, and live smoke tests remain
  follow-up work.
- `COINBASE_COMMERCE` is a REST-capable `ChannelAdapter` registered in `BlockchainConfig` when
  `COINBASE_COMMERCE_API_KEY` is set. It uses `COINBASE_COMMERCE_BASE_URL` (default
  `https://api.commerce.coinbase.com`) and `COINBASE_COMMERCE_API_VERSION` (default `2018-03-22`)
  for Coinbase Commerce charge and exchange-rate calls. Without an API key it preserves the old
  stub behavior for local/dev compatibility, but the bean is omitted in the `prod` profile unless
  an API key is configured. Coinbase live credentials, webhook semantics, and external refund
  operations still need environment verification. `LiveCoinbaseCommerceAdapterTest`
  provides opt-in live rate coverage and only creates a live charge when
  `LIVE_COINBASE_COMMERCE_CREATE_CHARGE=true`.
- BitMart and Binance Pay adapters are still stubs. Their `BlockchainConfig` beans are guarded with
  `@Profile("!prod")`, so do not rely on them in a production profile until real REST adapters are
  implemented and live-verified.
- Kafka domain-event publishing is available behind the same `DomainEventPublisher` port. Default
  remains Spring in-process events; set `EVENT_PUBLISHER=kafka` and `KAFKA_BOOTSTRAP_SERVERS` to
  publish to event-type topics such as `crypto.payment.confirmed`. `LiveMessagingInfrastructureTest`
  can smoke-test a real broker when `LIVE_KAFKA_BOOTSTRAP_SERVERS` is configured.
- Redis idempotency/cache behavior is unit-tested with mocked clients. `LiveMessagingInfrastructureTest`
  can verify real Redis `SET NX EX` behavior when `LIVE_REDIS_HOST` is configured.
- Merchant orchestration orders can be crypto-denominated. `PaymentOrchestrator.createOrder` accepts
  `amountCrypto` + `currencyCrypto` + `network`, routes with that asset, and derives the fiat display
  amount from the channel exchange rate.
- `flow-cashier/src/main/resources/static/checkout.html` is the static buyer Checkout. It accepts
  `payment_id`/`paymentId` from the URL, calls `/cashier/order/status` and `/cashier/pay/submit`,
  renders the deposit address and Canvas QR code, and polls status until terminal states.
  `nexusflow.cashier.base-url` controls the returned `payUrl`; the default is `/checkout.html`.
- `flow-cashier/src/main/resources/static/merchant.html` is the static Merchant Portal. It stores API
  base, API key, and callback URLs in browser localStorage and calls `/pay/order`,
  `/pay/order/{paymentId}`, and `/refund/order`; it is not a server-side merchant settings store.
- `flow-cashier/src/main/resources/static/ops.html` is the static Ops Dashboard. It calls
  `/ops/dashboard` for channel/order/reconciliation/risk data and `/crypto/orphan-transactions`
  for orphan resolve/ignore/compensate actions; `/ops/*` is protected by the same `X-API-Key` filter.
- When a scanned transaction hits a managed address but no PENDING payment matches, the application
  records an `orphan_transactions` row through `OrphanTransactionRepository` and publishes
  `crypto.orphan.detected`. Operators can list, resolve, ignore, or compensate these via
  `/crypto/orphan-transactions`; set `ORPHAN_AUTO_COMPENSATION_ENABLED=true` to create compensation
  `CryptoPayment` records automatically for unmatched inbound transactions.
- `StubAdapter` is a working fake `ChannelAdapter` used for routing/testing.

Before relying on a feature end-to-end, verify the relevant adapter is actually implemented
rather than a stub.

## Testing conventions

Only `mockito-core` is on the classpath (no `mockito-junit-jupiter`). Do **not** use
`@ExtendWith(MockitoExtension.class)` / `@Mock` / `@InjectMocks` - construct mocks manually with
`Mockito.mock(...)` and wire them via the constructor in a `@BeforeEach`. AssertJ is available in
`flow-domain` and `flow-application` but **not** in `flow-infra` / `flow-wallet` (use JUnit
`Assertions` there).
