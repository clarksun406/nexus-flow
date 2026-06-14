# NexusFlow Test Summary

Last verified: 2026-06-14 with `mvn test`.

## Current Result

| Total | Passed | Failed | Errors | Skipped |
|-------|--------|--------|--------|---------|
| 138 | 134 | 0 | 0 | 4 |

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
| `flow-application` | `PaymentApplicationServiceTest` | 10 | Create payment, address-pool allocation, duplicate order rejection, dust/underpayment handling |
| `flow-application` | `PaymentOrchestratorTest` | 14 | Order creation, channel routing, refund flow, callback idempotency |
| `flow-application` | `PaymentReconciliationJobTest` | 4 | Confirmation polling, expiry, retry/backoff |
| `flow-infra` | `BitcoinAdapterTest` | 3 | Bitcoin Core RPC parsing, block scan, confirmations, failure behavior |
| `flow-infra` | `EthereumAdapterTest` | 3 | ERC20 `Transfer` log parsing, confirmations, block hash lookup |
| `flow-infra` | `TronAdapterTest` | 8 | TronGrid response parsing, confirmations, health, explicit scan stub |
| `flow-infra` | `RedisCurrencyRateCacheTest` | 4 | Redis-backed exchange-rate and currency cache fallback behavior |
| `flow-infra` | `InMemoryProcessedEventStoreTest` | 3 | In-memory callback idempotency |
| `flow-infra` | `RedisProcessedEventStoreTest` | 3 | Redis `SET NX EX` callback idempotency behavior |
| `flow-infra` | `InMemoryPaymentRepositoryTest` | 6 | In-memory payment repository matching and lookup |
| `flow-infra` | `JpaAddressPoolRepositoryTest` | 2 | Address pool JPA mapping and available-address lookup |
| `flow-infra` | `JpaMnemonicStoreTest` | 2 | Encrypted mnemonic backup persistence mapping |
| `flow-infra` | `JpaPaymentRepositoryTest` | 4 | Crypto payment JPA mapping, round-trip fields, status queries |
| `flow-infra` | `JpaWalletRepositoryTest` | 4 | Wallet JPA mapping, active-wallet lookup |
| `flow-listener` | `BlockchainScannerTest` | 2 | Scanner cursor advance, transaction dispatch, reorg rewind and rollback |
| `flow-wallet` | `Base58Test` | 5 | Base58/Base58Check encoding |
| `flow-wallet` | `KeyGeneratorTest` | 7 | BIP39/BIP44 derivation and ETH/TRON/BTC address derivation |
| `flow-api` | `NexusFlowApplicationIT` | 4 skipped locally | Spring Boot context, PostgreSQL Testcontainers, JPA round trips |

## Coverage by Area

| Area | Covered |
|------|---------|
| Domain state machines | `OrderStatus`, `FlowStatus`, `RefundStatus`, `CryptoPayment` lifecycle |
| Payment orchestration | Channel routing, create-order flow, refund flow, callback deduplication |
| Execution payments | Address allocation, payment detection, underpayment/dust rules, confirmation reconciliation |
| Persistence | Execution-layer JPA repositories, wallet persistence, mnemonic backups, address pool mappings |
| Blockchain adapters | ETH/BTC mocked transport parsing; TRON height/confirmation parsing; scanner reorg behavior |
| Wallet/key management | BIP39/BIP44 derivation, ETH/TRON/BTC address derivation, Base58Check |
| Reliability | Redis idempotency, Redis cache fallback, retry/backoff, blockchain circuit breaker |
| Integration | PostgreSQL Testcontainers test class exists but needs Docker to execute |

## Known Gaps

| Gap | Status |
|-----|--------|
| Docker-backed integration test run | Present but skipped locally without Docker |
| Live ETH/BTC/TRON node verification | Not covered; adapter tests use mocked transports or parsed HTTP responses |
| TRON `scanNewBlocks()` | Explicit stub; no automatic USDT_TRC20 block scanning test until implementation exists |
| `PaymentController` end-to-end API tests | Not yet covered with HTTP-level integration tests |
| Redis integration against a real Redis server | Cache/idempotency tests use mocked clients |
| Address-pool concurrent allocation | No stress/concurrency test; repository currently lacks DB row locking |
| `createPayment` full idempotency semantics | Partial duplicate-order protection exists; full idempotency-key behavior is not covered |
| Missing-event catch-up | Not implemented or tested |
| Execution-layer merchant callback delivery | Not implemented or tested |
| `Money` and `ApiResponse` edge cases | Still worth adding focused unit tests |

## Notes

- `NexusFlowApplicationIT` uses `@Testcontainers(disabledWithoutDocker = true)`, so local no-Docker runs can still show a green build while skipping integration coverage.
- Blockchain adapter tests validate request/response parsing and domain conversion. They do not prove behavior against real nodes or network-specific edge cases.
- Roadmap status and production risks are tracked in `nexusflow-roadmap.md`, especially the "production preflight risk" section.
