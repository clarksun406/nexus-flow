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
is pinned to 3.2.5 in the root `pom.xml` `pluginManagement` — keep it there or tests stop running.

CI (`.github/workflows/ci.yml`) runs `mvn -B verify` on push/PR to `main`.

## Architecture

NexusFlow is a **DDD modular monolith** for crypto payments with **two distinct payment layers**
that must not be conflated:

- **Execution layer** (`domain/payment`, `domain/wallet`, `infra/blockchain`) — on-chain
  payments. `CryptoPayment` aggregate, talks to blockchain nodes via `BlockchainAdapter`
  (ETH/TRON/BTC). Counterparty = the chain.
- **Orchestration layer** (`domain/order`, `domain/refund`, `domain/channel`,
  `application/PaymentOrchestrator`) — merchant fiat/crypto orders. `PaymentOrder` aggregate,
  talks to acquiring channels (exchanges/PSPs) via `ChannelAdapter`. Counterparty = the channel.

These have **separate aggregates, state machines, and repositories**. When self-hosting nodes,
an orchestration `PaymentOrder` is meant to delegate down to an execution `CryptoPayment`
(Phase 2; not yet wired).

### Module dependency direction (strict)

```
flow-api / flow-listener  ─┐
flow-application ──────────┤→ flow-domain → flow-common
flow-infra / flow-wallet ──┘
```

`flow-domain` depends only on `flow-common` — never on infrastructure. Ports (interfaces) live
in `flow-domain` (e.g. `PaymentRepository`, `ChannelAdapter`, `DomainEventPublisher`,
`ProcessedEventStore`); their implementations live in `flow-infra`. To add a capability the
domain needs, define the port in `flow-domain` and implement it in `flow-infra`.

### Domain events + state machines (the core pattern)

State changes flow through three coupled mechanisms — replicate this when adding transitions:

1. **State machines are enums** (`PaymentStatus`, `OrderStatus`, `FlowStatus`, `RefundStatus`)
   with an explicit `ALLOWED` set and `requireTransitionTo(target)` that throws
   `InvalidStateTransitionException` on illegal/backward moves. Never mutate status directly.
2. **Aggregates buffer domain events.** Every `markXxx()` transition appends a `DomainEvent`
   to an internal list. The aggregate does NOT publish.
3. **Application services drain and publish.** After `repository.save(aggregate)`, the service
   calls `aggregate.collectEvents()` (which clears the buffer) and pushes each to
   `DomainEventPublisher`. `collectEvents()` is single-shot — call it once per unit of work.

`DomainEventPublisher` is currently `SpringDomainEventPublisher` (in-process Spring events);
Kafka is a planned swap-in behind the same port.

### Idempotency (required on all inbound paths)

- **Order creation**: dedup by `(merchantId, merchantOrderNo)` / `orderId`.
- **Channel callbacks**: dedup by `eventId` via `ProcessedEventStore.markProcessed(eventId)`
  (returns `false` if already seen). Two impls selected by `nexusflow.idempotency.store`:
  `InMemoryProcessedEventStore` (default) and `RedisProcessedEventStore` (`SET NX EX`, wired by
  `RedisIdempotencyConfig` when `=redis`). In `PaymentOrchestrator.handlePaymentCallback` the dedup
  runs **after** the order lookup on purpose — so a callback for a not-yet-existing order
  throws without permanently consuming the `eventId`, letting a legitimate retry succeed.
- **On-chain detection**: dedup by `txHash` before matching.

### Phase-1 stubs — know what is NOT real yet

Many infrastructure pieces are intentional placeholders (tracked in `nexusflow-roadmap.md` and
the implementation roadmap section of `nexusflow.md`):

- Repositories: `InMemoryPaymentRepository`, `InMemoryWalletRepository` are non-persistent (data
  lost on restart, single-instance only). JPA repos exist only for the orchestration tables
  (order/flow/refund). Execution-layer payments/wallets have NO persistence yet.
- `TronAdapter`: `getCurrentBlockHeight`/`getConfirmations`/`isHealthy` are real (via `TronGridClient`,
  parsing unit-tested) but **not live-verified**; `scanNewBlocks` is an explicit stub. `EthereumAdapter`
  and `BitcoinAdapter` are still stubs.
- `KeyGenerator` derives real ETH/TRON addresses (web3j + `Base58`); BTC/SOLANA throw.
- Wallets are never seeded — `createPayment` will throw `WALLET_NOT_FOUND` until a wallet exists for
  the chain.
- `StubAdapter` is a working fake `ChannelAdapter` used for routing/testing.

Before relying on a feature end-to-end, verify the relevant adapter is actually implemented
rather than a stub.

## Testing conventions

Only `mockito-core` is on the classpath (no `mockito-junit-jupiter`). Do **not** use
`@ExtendWith(MockitoExtension.class)` / `@Mock` / `@InjectMocks` — construct mocks manually with
`Mockito.mock(...)` and wire them via the constructor in a `@BeforeEach`. AssertJ is available in
`flow-domain` and `flow-application` but **not** in `flow-infra` / `flow-wallet` (use JUnit
`Assertions` there).
```
