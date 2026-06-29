# Flow Permission Operations Guide

This guide describes how to operate `flow-permission` with the current API surface. It covers permission lookup, custom role management, user-role grants, and the intended integration pattern for merchant and ops systems.

## 1. Concepts

| Concept | Meaning |
|---------|---------|
| Permission | A namespaced action string, such as `refund:create` or `orphan:compensate`. |
| Role | A named bundle of permissions. Seeded roles are system templates; custom roles can be created through the role API. |
| User role | Assignment of a role to a user within a scope. |
| Scope type | `SYSTEM`, `MERCHANT`, `PROVIDER`, or future scopes. |
| Scope ID | UUID of the scoped resource, such as a merchant UUID. `SYSTEM` normally uses `null`. |

## 2. Authentication

All permission server APIs under `/api/v1/*` require:

```http
Authorization: Bearer <PERMISSION_SERVICE_TOKEN>
```

Configure the token on the permission server:

```bash
PERMISSION_SERVICE_TOKEN=replace-with-strong-token
```

For local development only, authentication can be disabled:

```bash
PERMISSION_AUTH_ENABLED=false
```

Do not expose this service token to browsers. Browser applications should call the main API, and the main API should call `flow-permission`.

## 3. Seeded Permissions

Business permissions are seeded by:

- `V3__seed_business_permissions.sql`

Examples:

| Area | Examples |
|------|----------|
| Payment orders | `payment_order:read`, `payment_order:create`, `payment_order:export` |
| Refunds | `refund:read`, `refund:create`, `refund:approve`, `refund:retry` |
| API keys | `api_key:read`, `api_key:create`, `api_key:rotate`, `api_key:revoke` |
| Ops | `ops_dashboard:read`, `orphan:compensate`, `webhook_dlq:replay` |
| Permission admin | `permission:manage`, `role:manage`, `user_role:grant` |

Java constants are available in:

- `com.nexusflow.permission.client.PermissionCodes`

Use constants in backend annotations and tests instead of hand-written strings where possible.

## 4. Seeded Role Templates

Role templates are seeded by:

- `V4__seed_role_templates.sql`

Current templates:

| Role | Scope | Purpose |
|------|-------|---------|
| `SYSTEM_ADMIN` | `SYSTEM` | Full platform and permission administration |
| `OPS_ADMIN` | `SYSTEM` | Operational administration |
| `OPS_SUPPORT` | `SYSTEM` | Operational read/support actions |
| `MERCHANT_OWNER` | `MERCHANT` | Full merchant workspace access |
| `MERCHANT_DEVELOPER` | `MERCHANT` | API key, webhook, and integration access |
| `MERCHANT_FINANCE` | `MERCHANT` | Payments, refunds, fiat ramp, exports |
| `MERCHANT_SUPPORT` | `MERCHANT` | Lookup/support workflows |
| `MERCHANT_VIEWER` | `MERCHANT` | Read-only merchant workspace access |

Java constants are available in:

- `com.nexusflow.permission.client.RoleCodes`

Seeded roles are marked `is_system=true`. Current service logic prevents updating or deleting system roles.

## 5. List Permissions

```http
GET /api/v1/permission/list
Authorization: Bearer <token>
```

Optional scope filter:

```http
GET /api/v1/permission/list?scope=MERCHANT
Authorization: Bearer <token>
```

## 6. List Roles

```http
GET /api/v1/role/list
Authorization: Bearer <token>
```

Optional scope filter:

```http
GET /api/v1/role/list?scope=MERCHANT
Authorization: Bearer <token>
```

Get one role with permissions:

```http
GET /api/v1/role/{roleId}
Authorization: Bearer <token>
```

## 7. Create a Custom Role

Use custom roles when a merchant or ops team needs a role different from the seeded templates.

```http
POST /api/v1/role
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "code": "MERCHANT_REFUND_OPERATOR",
  "name": "Merchant Refund Operator",
  "scope": "MERCHANT",
  "description": "Can view payments and create refunds",
  "permissionCodes": [
    "merchant:read",
    "payment_order:read",
    "refund:read",
    "refund:create"
  ]
}
```

Response is the created role. The role will have `isSystem=false`, so it can be updated or deleted.

## 8. Update Custom Role Metadata

```http
PUT /api/v1/role/{roleId}
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "name": "Refund Operator",
  "description": "Can inspect orders and create refunds"
}
```

System roles cannot be updated.

## 9. Set Role Permissions

This replaces the full permission set for the role.

```http
PUT /api/v1/role/{roleId}/permissions
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "permissionCodes": [
    "payment_order:read",
    "refund:read",
    "refund:create"
  ]
}
```

Use this carefully. The current API replaces all role-permission mappings for the role.

## 10. Delete a Custom Role

```http
DELETE /api/v1/role/{roleId}
Authorization: Bearer <token>
```

System roles cannot be deleted. Delete currently removes role-permission mappings and the role; review user assignments before deleting.

## 11. Grant a Role to a User

Grant a merchant-scoped role:

```http
POST /api/v1/user/grant-role
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "userId": "11111111-1111-1111-1111-111111111111",
  "roleId": "22222222-2222-2222-2222-222222222222",
  "scopeType": "MERCHANT",
  "scopeId": "33333333-3333-3333-3333-333333333333",
  "grantedBy": "99999999-9999-9999-9999-999999999999"
}
```

Grant a system role:

```json
{
  "userId": "11111111-1111-1111-1111-111111111111",
  "roleId": "22222222-2222-2222-2222-222222222222",
  "scopeType": "SYSTEM",
  "scopeId": null,
  "grantedBy": "99999999-9999-9999-9999-999999999999"
}
```

`V2__fix_user_role_scope_uniqueness.sql` ensures duplicate global grants with `scopeId=null` are blocked.

## 12. Replace User Roles in a Scope

```http
PUT /api/v1/user/{userId}/roles
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "scopeType": "MERCHANT",
  "scopeId": "33333333-3333-3333-3333-333333333333",
  "roleIds": [
    "22222222-2222-2222-2222-222222222222"
  ],
  "grantedBy": "99999999-9999-9999-9999-999999999999"
}
```

This replaces existing roles for the user in the target scope.

## 13. Revoke a Role

```http
DELETE /api/v1/user/{userId}/roles/{roleId}?scopeType=MERCHANT&scopeId=33333333-3333-3333-3333-333333333333
Authorization: Bearer <token>
```

For `SYSTEM` scope, omit `scopeId`:

```http
DELETE /api/v1/user/{userId}/roles/{roleId}?scopeType=SYSTEM
Authorization: Bearer <token>
```

## 14. Check a Permission

```http
POST /api/v1/permission/check
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "userId": "11111111-1111-1111-1111-111111111111",
  "permission": "refund:create",
  "scopeType": "MERCHANT",
  "scopeId": "33333333-3333-3333-3333-333333333333"
}
```

Granted responses return HTTP 200. Denied responses return HTTP 403.

## 15. Get Effective Permissions

```http
GET /api/v1/permission/eff?userId=11111111-1111-1111-1111-111111111111&scopeType=MERCHANT&scopeId=33333333-3333-3333-3333-333333333333
Authorization: Bearer <token>
```

This endpoint is the source for frontend page/menu/button visibility, but browsers should not call it directly. The main API should expose a session-aware endpoint such as `/auth/me`.

Expected main API response shape:

```json
{
  "userId": "11111111-1111-1111-1111-111111111111",
  "actorType": "MERCHANT_USER",
  "activeScope": {
    "scopeType": "MERCHANT",
    "scopeId": "33333333-3333-3333-3333-333333333333"
  },
  "roles": ["MERCHANT_OWNER"],
  "permissions": [
    "payment_order:read",
    "payment_order:create",
    "refund:create"
  ]
}
```

Frontend examples:

```vue
<button v-if="can('refund:create')">Create refund</button>
<button v-permission="'api_key:rotate'">Rotate API key</button>
```

Route guard example:

```ts
{
  path: "/ops/orphans",
  component: OrphanTransactionsPage,
  meta: { permission: "orphan:read" }
}
```

UI checks are only for visibility. Backend APIs must still enforce permissions.

## 16. Recommended Merchant Portal Flow

1. Merchant identity service authenticates the user.
2. Main API resolves current merchant membership and selected merchant.
3. Main API queries `flow-permission` effective permissions.
4. Main API returns `/auth/me` with permissions.
5. Frontend uses `can()` / `v-permission` / route guards for display.
6. Backend controllers enforce `@CheckPermission` on every protected API.

## 17. Recommended Ops/Admin Flow

1. Internal identity service authenticates the operator.
2. Main API sets `userId`, `actorType=INTERNAL_USER`, and `scopeType=SYSTEM`.
3. Main API queries `flow-permission`.
4. Ops/Admin frontend displays menus/actions from effective permissions.
5. Sensitive backend actions require both `@CheckPermission` and operation audit data such as reason/comment.

## 18. Current Limitations

- Custom roles are currently global by `roles.code`. Two merchants cannot both create a role with the same code.
- There is no `owner_scope_type` / `owner_scope_id` on roles yet, so merchant-owned custom roles are not isolated.
- Permission server admin APIs are service-token protected but not yet permission-protected by `permission:manage`, `role:manage`, or `user_role:grant`.
- There is no audit table for role or grant changes yet.
- There is no active cache invalidation event; clients rely on TTL or manual eviction.
- DTO validation and stable error envelopes are implemented for permission server controllers. Broader controller/service coverage is still pending.

## 19. Error Response Contract

Validation and common API errors use a stable JSON envelope:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "status": 400,
  "path": "/api/v1/role",
  "timestamp": 1719230000000,
  "violations": [
    {
      "field": "code",
      "message": "must match \"^[A-Z][A-Z0-9_]{1,99}$\""
    }
  ]
}
```

Common codes:

| Code | HTTP status | Meaning |
|------|-------------|---------|
| `VALIDATION_FAILED` | 400 | Request body failed bean validation |
| `BAD_REQUEST` | 400 | Illegal argument or duplicate input |
| `CONFLICT` | 409 | Operation conflicts with current state, such as modifying a system role |
| `NOT_FOUND` | 404 | Requested permission, role, or grant was not found |

## 20. Naming Guidance

Use stable uppercase role codes:

```text
MERCHANT_REFUND_OPERATOR
MERCHANT_RECON_VIEWER
OPS_RISK_REVIEWER
```

Use namespaced lowercase permission codes:

```text
refund:create
orphan:compensate
webhook_dlq:replay
```

Avoid UI-specific permissions such as `showRefundButton`. Define business capabilities instead and let frontend components map UI to capabilities.
