# Flow Permission Roadmap

> Status: `flow-permission` is a lightweight RBAC module. It is now part of the root Maven reactor and can protect services through a service-token-authenticated permission API, but it is not yet a production permission center for merchant and ops workflows.

## 1. Goal

`flow-permission` should become the single permission decision layer for:

- merchant portal users operating within one merchant scope;
- internal ops/admin users operating globally or against a target merchant;
- service/API-key actors once the merchant identity system resolves them into an authenticated context;
- current `flow-api` endpoints, gradually replacing the single global `X-API-Key` model with scoped permissions.
- frontend page, route, menu, and button visibility in Merchant Portal, Ops Console, and Admin Console.

It should not own merchant onboarding/KYB data. Merchant identity, API keys, memberships, webhook configuration, and audit actor metadata belong to the merchant identity layer. `flow-permission` consumes the resulting `userId`, `merchantId`, `actorType`, and scopes.

## 2. Current State

Implemented:

- Maven modules: `flow-permission`, `flow-permission-server`, `flow-permission-client`.
- Tables: `permissions`, `roles`, `role_permissions`, `user_roles`.
- Scope model: `scope_type` + optional `scope_id`.
- CRUD APIs for permissions, roles, and user-role grants.
- Permission check API: `POST /api/v1/permission/check`.
- Effective permission API: `GET /api/v1/permission/eff`.
- Client SDK with `@CheckPermission`, HTTP lookup, and Caffeine cache.
- Service-token filter for `/api/v1/*`.
- Partial unique indexes for `user_roles` with nullable `scope_id`.

Known gaps:

- No merchant/user identity source.
- No controller/service tests beyond the service-token filter.
- No permission-management permissions protecting permission server admin APIs.
- No audit logs for grant/revoke/role changes/check failures.
- No role templates, role inheritance, deny rules, or conditional permissions.
- No cache invalidation event or versioning.
- No integration with `flow-api` controllers yet.

## 3. Target Model

### Scope Types

| Scope type | Meaning | Example `scope_id` |
|------------|---------|--------------------|
| `SYSTEM` | global platform/admin scope | `null` |
| `MERCHANT` | one merchant tenant | merchant UUID |
| `PROVIDER` | one external channel/provider | provider UUID or code-backed UUID |
| `ORGANIZATION` | future org/group scope | org UUID |

### Actor Context

The caller authentication layer must set request attributes before permission checks:

| Attribute | Meaning |
|-----------|---------|
| `userId` | merchant user or internal user UUID; can be key owner for API-key calls |
| `merchantId` | active merchant UUID when operating in merchant scope |
| `actorType` | `MERCHANT_USER`, `INTERNAL_USER`, `API_KEY`, `SYSTEM` |
| `keyId` | API key UUID when actor type is `API_KEY` |
| `scopes` | optional API-key scopes for pre-filtering |

`flow-permission-client` should remain ignorant of login/session details. It only checks the context it receives.

## 4. Permission Taxonomy

Use namespaced action permissions instead of generic `READ` / `WRITE`.

| Area | Permissions |
|------|-------------|
| Merchant profile | `merchant:read`, `merchant:create`, `merchant:update`, `merchant:activate`, `merchant:suspend` |
| Merchant users | `merchant_user:read`, `merchant_user:invite`, `merchant_user:update`, `merchant_user:disable` |
| API keys | `api_key:read`, `api_key:create`, `api_key:rotate`, `api_key:revoke` |
| Webhooks | `webhook:read`, `webhook:update`, `webhook:test`, `webhook:replay` |
| Payment orders | `payment_order:read`, `payment_order:create`, `payment_order:export` |
| Execution payments | `crypto_payment:read`, `crypto_payment:create`, `crypto_payment:confirm`, `crypto_payment:fail` |
| Refunds | `refund:read`, `refund:create`, `refund:approve`, `refund:retry` |
| Fiat ramp | `fiat_ramp:quote`, `fiat_ramp:create`, `fiat_ramp:read`, `fiat_ramp:operate` |
| Orphans | `orphan:read`, `orphan:resolve`, `orphan:ignore`, `orphan:compensate` |
| Webhook dead letters | `webhook_dlq:read`, `webhook_dlq:replay`, `webhook_dlq:ignore` |
| Ops dashboard | `ops_dashboard:read` |
| Providers/channels | `provider:read`, `provider:update`, `provider:disable`, `provider:enable` |
| Permission admin | `permission:read`, `permission:manage`, `role:read`, `role:manage`, `user_role:read`, `user_role:grant`, `user_role:revoke` |
| Audit | `audit:read` |

## 5. Flow API Integration Matrix

Public/user-facing and callback endpoints should keep their own controls:

| Endpoint | Permission path |
|----------|-----------------|
| `GET /cashier/order/status` | public checkout token or payment token, not RBAC |
| `POST /cashier/pay/submit` | checkout token, not RBAC |
| `POST /callback/**` | provider HMAC, not RBAC |

Merchant/server API endpoints should be permission-managed after merchant API-key authentication is implemented:

| Endpoint | Required permission | Scope |
|----------|---------------------|-------|
| `POST /pay/order` | `payment_order:create` | `MERCHANT` |
| `GET /pay/order/{paymentId}` | `payment_order:read` | `MERCHANT` |
| `POST /refund/order` | `refund:create` | `MERCHANT` |
| `POST /fiat/ramp/quote` | `fiat_ramp:quote` | `MERCHANT` |
| `POST /fiat/ramp/orders` | `fiat_ramp:create` | `MERCHANT` |
| `GET /fiat/ramp/orders/{rampOrderId}` | `fiat_ramp:read` | `MERCHANT` |
| `POST /crypto/payments` | `crypto_payment:create` | `MERCHANT` or `SYSTEM` |
| `GET /crypto/payments/{paymentId}` | `crypto_payment:read` | `MERCHANT` or `SYSTEM` |

Ops endpoints should be permission-managed after internal-user authentication is implemented:

| Endpoint | Required permission | Scope |
|----------|---------------------|-------|
| `GET /ops/dashboard` | `ops_dashboard:read` | `SYSTEM` |
| `GET /crypto/orphan-transactions` | `orphan:read` | `SYSTEM` |
| `POST /crypto/orphan-transactions/{chain}/{txHash}/resolve` | `orphan:resolve` | `SYSTEM` |
| `POST /crypto/orphan-transactions/{chain}/{txHash}/ignore` | `orphan:ignore` | `SYSTEM` |
| `POST /crypto/orphan-transactions/{chain}/{txHash}/compensate` | `orphan:compensate` | `SYSTEM` |
| `GET /ops/webhook-dead-letters` | `webhook_dlq:read` | `SYSTEM` |
| `POST /ops/webhook-dead-letters/{id}/replay` | `webhook_dlq:replay` | `SYSTEM` |
| `POST /ops/webhook-dead-letters/{id}/ignore` | `webhook_dlq:ignore` | `SYSTEM` |

Operational/manual execution endpoints should not be exposed to merchant API keys by default:

| Endpoint | Required permission | Scope |
|----------|---------------------|-------|
| `POST /crypto/payments/{paymentId}/confirm` | `crypto_payment:confirm` | `SYSTEM` |
| `POST /crypto/payments/{paymentId}/fail` | `crypto_payment:fail` | `SYSTEM` |

Permission server admin endpoints should protect themselves:

| Endpoint | Required permission | Scope |
|----------|---------------------|-------|
| `GET /api/v1/permission/**` | `permission:read` or service admin token | `SYSTEM` |
| `POST/PUT/DELETE /api/v1/permission/**` | `permission:manage` | `SYSTEM` |
| `GET /api/v1/role/**` | `role:read` | `SYSTEM` |
| `POST/PUT/DELETE /api/v1/role/**` | `role:manage` | `SYSTEM` |
| `/api/v1/user/**/roles`, `/api/v1/user/grant-role` | `user_role:read/grant/revoke` | `SYSTEM` |

## 6. Implementation Phases

### P0 - Stabilize Permission Module

Goal: make `flow-permission` internally consistent and testable.

- Add controller/service tests for permission check, effective permissions, role CRUD, role-permission assignment, grant/revoke/set roles.
- ✅ Remove JPA `@UniqueConstraint` from `UserRole` and rely on partial indexes for nullable `scope_id` uniqueness.
- ✅ Add validation to create/update/grant/check DTOs.
- ✅ Add a global exception handler with stable error envelopes.
- ✅ Add permission seed migration using namespaced permissions from section 4.
- ✅ Add `PermissionCodes` constants for backend annotations, frontend contracts, and tests.
- ✅ Add role code constants and role seed migration for initial templates:
  - `SYSTEM_ADMIN`
  - `OPS_ADMIN`
  - `OPS_SUPPORT`
  - `MERCHANT_OWNER`
  - `MERCHANT_DEVELOPER`
  - `MERCHANT_FINANCE`
  - `MERCHANT_SUPPORT`
  - `MERCHANT_VIEWER`

### P1 - Secure Permission Administration

Goal: permission server management APIs cannot be modified by any bearer token holder.

- Split service-token mode into:
  - `permission:check` service token for check/effective APIs;
  - admin actor context for role/permission/user-role management.
- Add `@CheckPermission` or server-side equivalent for permission server admin controllers.
- Add admin operation audit logs for role/permission/user-role changes.
- Add request IDs and actor metadata to audit rows.
- Add tests for unauthorized and insufficient-permission admin calls.

### P2 - Merchant Identity Prerequisite

Goal: main API can supply reliable permission context.

This phase depends on merchant identity work outside `flow-permission`:

- `merchant_profiles`
- `merchant_users`
- `merchant_user_memberships`
- `merchant_api_keys`
- merchant session or internal session auth
- request attributes: `userId`, `merchantId`, `actorType`, `keyId`

`flow-permission` deliverables:

- Define client-side `PermissionContext` helper for reading request attributes consistently.
- Support API-key actor checks where `userId` may be key owner or service principal.
- Document required attributes and failure behavior.

### P3 - Integrate Current Flow API

Goal: current `flow-api` endpoints can be managed by permission rules.

- Add `flow-permission-client` dependency to `flow-api`.
- Annotate merchant endpoints with `@CheckPermission`.
- Annotate ops endpoints with `@CheckPermission(scopeType = "SYSTEM")`.
- Keep `/cashier/**` protected by checkout token, not RBAC.
- Keep `/callback/**` protected by HMAC, not RBAC.
- Replace broad global `X-API-Key` authorization with merchant API-key authentication plus permission checks.
- Add MockMvc tests proving:
  - missing context is rejected;
  - merchant context can only access own merchant data;
  - ops context can access system endpoints;
  - merchant context cannot call ops/manual execution endpoints.

### P3-F - Frontend Permission Contract

Goal: frontend applications can use `flow-permission` decisions for page, route, menu, and button visibility without treating UI checks as the security boundary.

Backend contract:

- Add a current-principal permission endpoint in the main API layer, backed by `flow-permission`:
  - `GET /auth/me` returns identity, active scope, memberships, and effective permissions.
  - `GET /auth/permissions?scopeType=MERCHANT&scopeId=...` returns permissions for scope switching or refresh.
- Return permissions as stable namespaced strings from section 4.
- Return role codes only for display/debug; frontend authorization should use permission codes, not role names.
- Include a permission version or `permissionsUpdatedAt` field so the frontend can refresh cached permissions after role changes.
- Never expose service tokens or permission-server credentials to the browser.
- Keep backend endpoint checks mandatory with `@CheckPermission`; frontend checks are only for user experience.

Example response:

```json
{
  "userId": "usr_123",
  "actorType": "MERCHANT_USER",
  "activeScope": {
    "scopeType": "MERCHANT",
    "scopeId": "9f7c0000-0000-0000-0000-000000000001"
  },
  "roles": ["MERCHANT_OWNER"],
  "permissions": [
    "payment_order:read",
    "payment_order:create",
    "refund:create",
    "api_key:rotate"
  ],
  "permissionsUpdatedAt": 1719230000000
}
```

Frontend contract:

- Provide a shared permission helper, for example `can(permission: string): boolean`.
- Vue apps may use either:
  - `v-if="can('refund:create')"` for simple controls;
  - `v-permission="'refund:create'"` for reusable button/menu visibility;
  - route guards using `meta.permission`.
- Menus and routes should be generated from the same permission map to avoid drift.
- Missing permissions must hide or disable actions by default.
- UI permission checks must not be used to skip backend validation.

Example Vue usage:

```vue
<button v-if="can('refund:create')">Create refund</button>
<button v-permission="'api_key:rotate'">Rotate API key</button>
```

Route example:

```ts
{
  path: "/ops/orphans",
  component: OrphanTransactionsPage,
  meta: { permission: "orphan:read" }
}
```

Testing:

- Add frontend unit tests for `can()` and route guards.
- Add E2E tests proving hidden actions remain rejected when called directly through HTTP.
- Add backend MockMvc tests proving permissions returned by `/auth/me` match effective permission data from `flow-permission`.

### P4 - Cache Invalidation and Reliability

Goal: permission changes take effect predictably.

- Add `permission_version` or `updated_at` version to users/roles/grants.
- Add explicit client cache eviction endpoint or event.
- Publish grant/revoke/role-change events.
- Add fail-open/fail-closed configuration by endpoint category:
  - fail closed for payment/refund/ops writes;
  - optionally fail closed for all production APIs;
  - never silently allow if permission server is unreachable unless explicitly configured for local dev.
- Add metrics:
  - check latency;
  - cache hit rate;
  - denied count;
  - permission service error count.

### P5 - Conditional Permissions

Goal: cover real ops/finance rules without hardcoding everything in controllers.

- Add optional condition payload to check request.
- Add simple rule types:
  - amount threshold;
  - merchant risk level;
  - provider/channel;
  - operation reason required;
  - maker-checker for sensitive actions.
- Use conditions for:
  - refund approval thresholds;
  - orphan compensation;
  - merchant suspension/reactivation;
  - provider enable/disable.

Do not start P5 before P0-P3 are complete; conditional authorization without clean identity and audit is hard to reason about.

## 7. Acceptance Criteria

Permission module is usable for merchant and ops production flows when:

- all current `flow-api` non-public endpoints are mapped to explicit permissions;
- permission decisions are based on authenticated request context, not caller-supplied `merchantId`;
- merchant users cannot access another merchant's resources;
- merchant API keys cannot call ops/admin endpoints;
- frontend route/menu/button visibility uses effective permissions from `flow-permission`;
- permission changes are audited;
- cache behavior is deterministic and test-covered;
- permission server admin APIs are protected by system-scope permissions;
- local/dev fallback behavior is explicit and disabled in production.

## 8. Recommended Next Tasks

1. Implement service and controller tests for permission, role, and user-role workflows.
2. Add menu/button/API resource model for frontend route, menu, and action visibility.
3. Add role ownership so merchants can create isolated custom roles.
4. Add permission admin audit table.
5. Build merchant identity context, then wire `flow-api` through `flow-permission-client`.

## 8.1 Immediate Work Queue

This is the concrete short-term queue. Do these in order.

### Q1 - Finish Unit Test Coverage

Current permission server coverage is still low because the service and controller logic is mostly untested. Do not add more schema surface before this is fixed.

Required tests:

| Test class | Required coverage |
|------------|-------------------|
| `PermissionServiceTest` | granted check, denied check, effective permissions, create permission, duplicate permission rejection, get/update/delete not found |
| `RoleServiceTest` | create custom role, duplicate role rejection, set role permissions, unknown permission rejection, system role update/delete rejected, custom role delete clears role permissions |
| `UserRoleServiceTest` | grant role, duplicate grant idempotency, revoke role, set roles replaces only target scope, default `scopeType=MERCHANT` |
| `PermissionApiControllerTest` | list/get/create/update/delete/check/effective HTTP contract and validation |
| `RoleApiControllerTest` | list/get/create/update/delete/set-permissions HTTP contract and validation |
| `UserRoleApiControllerTest` | list/grant/revoke/set roles HTTP contract and validation |
| `PermissionClientTest` | effective-permission loading, cache hit behavior, denied behavior on non-200 or malformed response |
| `CheckPermissionAspectTest` | missing request, missing `userId`, merchant/system scope extraction, granted/denied paths |

Target:

- `flow-permission-server` line coverage >= 70%.
- `flow-permission-client` line coverage >= 70%.
- No new model or integration work until the service/controller tests above exist.

### Q2 - Add RuoYi-Style Resource/Menu/Button/API Model

After tests are in place, add `permission_resources` to support:

- dynamic menus;
- route guards;
- button visibility;
- API endpoint cataloging;
- frontend payload generation.

This is the next product-facing capability because Merchant Portal, Ops Console, and Admin Console all need route/menu/button permission control.

### Q3 - Add Role Ownership

After resources are modeled, add merchant-owned custom roles:

- `roles.owner_scope_type`;
- `roles.owner_scope_id`;
- owner-aware uniqueness for role codes;
- system roles remain global templates;
- merchant custom roles are isolated by merchant.

### Q4 - Add Audit

Add audit logs for:

- permission changes;
- role changes;
- role-permission changes;
- user-role grants/revokes;
- denied checks for sensitive endpoints.

### Q5 - Integrate Flow API

Only after merchant identity can set trusted request context:

- add `flow-permission-client` to `flow-api`;
- annotate merchant endpoints;
- annotate ops endpoints;
- add allow/deny MockMvc tests.

## 8.2 What Not To Do Yet

Avoid these until their prerequisites are done.

| Do not do yet | Why |
|---------------|-----|
| Do not introduce Casbin as a dependency now | The immediate gaps are identity context, tests, resource/menu model, audit, and API integration. A policy engine adds complexity before the local model is stable. Keep the design Casbin-style, not Casbin-dependent. |
| Do not wire `flow-api` to `@CheckPermission` yet | Main API still lacks trusted `userId` / `merchantId` request attributes. Wiring now would either fail closed everywhere or tempt caller-supplied `merchantId` checks. |
| Do not build Merchant Portal permission UI first | The backend does not yet expose resources/menus or reliable role ownership. A UI now would hardcode temporary assumptions. |
| Do not implement conditional/ABAC permissions yet | Amount thresholds, risk-level rules, and maker-checker require identity, audit, and stable RBAC first. |
| Do not copy RuoYi department data scope directly | NexusFlow's domain is merchant/provider/system scope, not department tree scope. Borrow menu/button/admin patterns, not the department model. |
| Do not let frontend permission checks become security boundaries | `v-if`, `v-permission`, and route guards are UX only. Backend APIs must still enforce permission decisions. |
| Do not add merchant-owned custom roles before role ownership | Current `roles.code` is global. Merchant role customization without ownership would create naming collisions and unclear isolation. |
| Do not expose permission service tokens to browsers | Browsers should call main API session endpoints like `/auth/me`; main API calls `flow-permission`. |

## 9. RuoYi-Style Admin Capability Gap

RuoYi-style admin systems include more than RBAC checks. They usually combine identity, organization, menu/button permissions, API authorization, data scope, and audit. `flow-permission` should borrow the useful parts without copying the department-centric model directly, because NexusFlow needs merchant-scope and ops-scope isolation.

| Capability | Current `flow-permission` | RuoYi-style baseline | Required direction |
|------------|---------------------------|----------------------|--------------------|
| Users | Not owned here | User CRUD, status, login metadata | Merchant/internal identity module owns users; permission consumes `userId` |
| Organization | Not owned here | Department tree and posts | Merchant identity owns merchant memberships; permission supports `MERCHANT` scope |
| Roles | Basic CRUD and templates | Role CRUD, menu permissions, data scope | Add role ownership and merchant-isolated custom roles |
| Permissions | Namespaced permission codes | Menu + button + API permissions | Add resource model linked to permission codes |
| Menus/routes | Missing | Dynamic menus, routes, icons, ordering | Add menu/resource catalog and effective menu endpoint |
| Frontend buttons | Contract only | `v-hasPermi` / button permissions | Add button resource catalog and frontend permission payload |
| API authorization | Client exists, not integrated | Backend annotation/interceptor | Wire `flow-api` with `@CheckPermission` after identity context |
| Data scope | Missing | all/dept/dept-tree/self/custom | Implement merchant/system/provider scope rules, not department rules |
| Audit | Missing | login log + operation log | Add permission admin audit and later ops action audit |
| Cache invalidation | TTL only | Permission cache refresh | Add version/updated-at/event invalidation |
| Admin protection | Service token only | Admin APIs permission-protected | Protect permission admin APIs with system permissions |

## 10. Fill-In Order

Start with tests, then resource/menu model. Do not start by wiring `flow-api`; without stronger tests and frontend resource contracts, integration will spread unstable permission strings across the codebase.

### Step A - Service and Controller Tests

Goal: make the current RBAC core trustworthy before adding more model surface.

Add:

- `PermissionServiceTest`
  - granted check;
  - denied check;
  - effective permissions;
  - create permission;
  - duplicate permission rejection;
  - get/update/delete not found.
- `RoleServiceTest`
  - create custom role;
  - duplicate role rejection;
  - set role permissions;
  - unknown permission rejection;
  - system role update/delete rejected;
  - custom role delete clears role permissions.
- `UserRoleServiceTest`
  - grant role;
  - duplicate grant is idempotent;
  - revoke role;
  - set roles replaces only target scope;
  - default `scopeType=MERCHANT`.
- Controller MockMvc tests for `PermissionApiController`, `RoleApiController`, and `UserRoleApiController`.

Target after Step A:

- `flow-permission-server` line coverage at or above 70%.
- Current role/permission/grant behavior covered before schema grows.

### Step B - Resource/Menu/Button/API Model

Goal: support RuoYi-like page/menu/button control and frontend dynamic routing.

Proposed table: `permission_resources`

| Field | Purpose |
|-------|---------|
| `resource_id` | UUID primary key |
| `parent_id` | menu tree parent |
| `resource_type` | `MENU`, `BUTTON`, `API` |
| `code` | stable resource code, e.g. `merchant.orders` |
| `title` | display title |
| `path` | frontend route or API pattern |
| `component` | frontend component key |
| `icon` | frontend icon key |
| `sort_order` | display order |
| `permission_code` | required permission, nullable for grouping menu |
| `scope` | `SYSTEM` / `MERCHANT` |
| `enabled` | visibility flag |

APIs:

- `GET /api/v1/resource/tree?scope=MERCHANT`
- `POST /api/v1/resource`
- `PUT /api/v1/resource/{id}`
- `DELETE /api/v1/resource/{id}`
- `GET /api/v1/permission/frontend?userId=...&scopeType=...&scopeId=...`

Frontend payload:

```json
{
  "permissions": ["payment_order:read", "refund:create"],
  "menus": [
    {
      "code": "merchant.orders",
      "title": "Orders",
      "path": "/orders",
      "component": "MerchantOrdersPage",
      "icon": "receipt",
      "children": []
    }
  ],
  "buttons": {
    "merchant.orders": ["refund:create", "payment_order:export"]
  }
}
```

### Step C - Role Ownership

Goal: support merchant-owned custom roles.

Add to `roles`:

- `owner_scope_type`
- `owner_scope_id`

Change uniqueness from global `code` to owner-aware uniqueness:

- global/system roles unique by `(owner_scope_type, code)` when `owner_scope_id is null`;
- merchant roles unique by `(owner_scope_type, owner_scope_id, code)`.

### Step D - Audit

Goal: make permission administration traceable.

Add:

- `permission_audit_logs`
- audit records for permission create/update/delete;
- role create/update/delete/permission-set;
- user-role grant/revoke/set;
- actor fields from request context or service token metadata.

### Step E - Flow API Integration

Goal: use `flow-permission` to manage current API permissions.

Prerequisite:

- merchant identity can set trusted `userId`, `merchantId`, `actorType`, and scopes.

Then:

- add `flow-permission-client` to `flow-api`;
- annotate merchant endpoints;
- annotate ops endpoints with `scopeType = "SYSTEM"`;
- add MockMvc tests for merchant/ops allow-deny behavior.
