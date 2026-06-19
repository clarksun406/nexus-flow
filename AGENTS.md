# Repository Guidelines

## Project Structure & Module Organization

This is a Java 17 / Spring Boot 3.3 multi-module Maven project. The root `pom.xml` defines shared dependency and plugin versions.

- `flow-domain/`: domain aggregates, value objects, state machines, and ports.
- `flow-application/`: use cases, orchestration services, jobs, and DTOs.
- `flow-infra/`: adapters for blockchain, channels, persistence, cache, Kafka, webhooks, MPC, and fiat ramp HTTP integrations.
- `flow-api/`: Spring Boot application, controllers, security filters, configuration, and Flyway migrations under `src/main/resources/db/migration`.
- `flow-listener/`, `flow-wallet/`, `flow-common/`: scanner, wallet derivation, and shared utilities.
- `flow-cashier/` and `frontend/`: static cashier, merchant, and ops UI assets.

Tests live beside each module under `src/test/java`.

## Build, Test, and Development Commands

- `mvn test`: run all module tests.
- `mvn -pl flow-api,flow-cashier -am test`: run the main API/cashier verification set and required upstream modules.
- `mvn -pl flow-infra,flow-api -am -Dtest=HttpFiatRampGatewayTest,FiatRampGatewayConfigTest test`: run focused tests.
- `mvn -DskipTests compile`: compile quickly without tests.
- `mvn -pl flow-api -am package`: build the API and dependencies.

Some integration/live tests are opt-in and skip without Docker or `LIVE_*` environment variables. See `TESTING.md`.

## Coding Style & Naming Conventions

Use Java 17, 4-space indentation, and existing Spring idioms. Keep domain code free of infrastructure dependencies; ports belong in `flow-domain`, implementations in `flow-infra`, and wiring in `flow-api`. Name tests as `*Test` for unit tests and `*IT` for integration tests. Prefer builders already used in domain DTOs and aggregates.

## Testing Guidelines

The project uses JUnit 5, Mockito, Surefire, and JaCoCo. Add focused tests for every behavior change, especially state transitions, adapter request/response mapping, and Spring conditional configuration. Keep external integrations mocked unless a test is explicitly opt-in live coverage.

## Commit & Pull Request Guidelines

Git history uses concise conventional-style messages, for example `feat: add HTTP fiat ramp gateway adapter`, `test: add opt-in live messaging smoke checks`, and `docs: update fiat ramp gateway status`. Keep commits coherent and include tests/docs when behavior or roadmap status changes. PRs should describe scope, list verification commands, call out skipped live tests, and note required environment variables or migrations.

## Security & Configuration Tips

Never commit secrets. Production requires explicit encryption keys and real provider credentials. Stub channel beans are guarded from production; do not enable production routes until the matching real adapter and live smoke tests are complete.
