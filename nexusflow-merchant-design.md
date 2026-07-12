# NexusFlow 商户体系设计

> 当前结论：现有代码里的 `merchantId` 只是业务字段，认证仍是单个全局 `X-API-Key`。生产级商户体系需要补齐商户主数据、商户用户、商户级 API key、Webhook 配置、数据隔离、审计和权限上下文。

## 1. 目标

商户体系要解决四件事：

1. 识别谁是商户：商户资料、状态、外部 merchant code、内部 UUID。
2. 识别谁在操作：商户用户、内部运营用户、服务端 API key、会话。
3. 限定能看/能改什么：商户数据隔离、角色权限、接口 scope。
4. 保存商户配置：API key、Webhook、品牌、结算/回调偏好、审计。

这部分是 `nexusflow-frontend-design.md`、R-17 前端体系和 R-18 RBAC 接入的前置。

## 2. 当前状态

| 能力 | 当前实现 | 缺口 |
|------|----------|------|
| 商户标识 | `payment_orders.merchant_id`、`fiat_ramp_orders.merchant_id`、DTO 中的 `merchantId` | 没有商户表；`merchantId` 没有注册、状态、归属和唯一来源 |
| API 认证 | `ApiKeyAuthFilter` 校验全局 `nexusflow.api.key` / `X-API-Key` | 不能区分商户；不能绑定 `merchantId`；无法轮换、禁用、审计 |
| 商户配置 | `merchant.html` 把 API key / callback URL 存在 `localStorage` | Webhook、品牌、回调 secret、IP allowlist 等都未持久化 |
| 商户用户 | `merchant_users` / `merchant_user_memberships` 表已创建 (V13)；领域实体和 JPA 仓储已实现 | 登录认证（email/password、session/JWT）和管理 API 待实现 |
| 数据隔离 | 订单表有 `merchant_id`，查询接口未从认证上下文约束 | 商户可以传任意 `merchantId`；运营/商户查询边界未区分 |
| RBAC | `flow-permission` 已接入：`flow-permission-client` 依赖已添加；`@CheckPermission` 已标注所有 controller 方法；`ApiKeyAuthFilter` 设置 `userId` request attribute；`MerchantUserProvisioningService` 启动时自动同步角色；`GlobalExceptionHandler` 处理 403 | Merchant Portal 登录用户的角色同步、key scopes 细粒度控制待实现 |

## 3. 身份模型

建议区分三个概念：

| 概念 | 示例 | 用途 |
|------|------|------|
| `merchant_id` | UUID，如 `9f7c...` | 内部主键，所有新表和权限 scope 使用它 |
| `merchant_code` | `merchant-1` / `acme` | 对外可读编码，兼容当前请求中的 `merchantId` |
| `display_name` | `Acme Store` | UI 展示，不参与鉴权 |

兼容策略：短期内 API request 里的 `merchantId` 继续接收外部 `merchant_code`；认证通过后解析成内部 `merchant_id`，并校验请求里的 `merchantId` 与凭证绑定的商户一致。长期可以把字段改名为 `merchantCode` 或允许省略，由认证上下文决定。

## 4. 数据模型

### 4.1 `merchant_profiles`

商户主表。

| 字段 | 类型 | 说明 |
|------|------|------|
| `merchant_id` | UUID / VARCHAR(36) PK | 内部商户 ID |
| `merchant_code` | VARCHAR(64) UNIQUE | 外部商户编码，兼容当前 `merchantId` |
| `display_name` | VARCHAR(128) | 展示名 |
| `legal_name` | VARCHAR(256) | 主体名称 |
| `status` | VARCHAR(32) | `PENDING` / `ACTIVE` / `SUSPENDED` / `CLOSED` |
| `risk_level` | VARCHAR(32) | `LOW` / `MEDIUM` / `HIGH` |
| `default_fiat_currency` | VARCHAR(16) | 默认法币 |
| `metadata` | JSONB / TEXT | 扩展资料 |
| `created_at` / `updated_at` | TIMESTAMPTZ | 时间戳 |
| `version` | BIGINT | 乐观锁 |

### 4.2 `merchant_users`

商户门户登录用户。

| 字段 | 类型 | 说明 |
|------|------|------|
| `user_id` | UUID / VARCHAR(36) PK | 用户 ID |
| `email` | VARCHAR(256) UNIQUE | 登录邮箱 |
| `password_hash` | VARCHAR(256) | 密码 hash；如接外部 IdP 可为空 |
| `display_name` | VARCHAR(128) | 显示名 |
| `status` | VARCHAR(32) | `ACTIVE` / `INVITED` / `DISABLED` |
| `last_login_at` | TIMESTAMPTZ | 最近登录 |
| `created_at` / `updated_at` | TIMESTAMPTZ | 时间戳 |

### 4.3 `merchant_user_memberships`

用户与商户的成员关系。一个用户可属于多个商户。

| 字段 | 类型 | 说明 |
|------|------|------|
| `merchant_id` | UUID / VARCHAR(36) | 商户 |
| `user_id` | UUID / VARCHAR(36) | 用户 |
| `role_code` | VARCHAR(64) | 角色模板，如 `OWNER` / `DEVELOPER` / `FINANCE` / `SUPPORT` |
| `status` | VARCHAR(32) | `ACTIVE` / `DISABLED` |
| `created_at` | TIMESTAMPTZ | 创建时间 |

唯一约束：`(merchant_id, user_id)`。

### 4.4 `merchant_api_keys`

商户服务器调用 `/pay`、`/refund`、`/fiat/ramp` 等接口使用。

| 字段 | 类型 | 说明 |
|------|------|------|
| `key_id` | UUID / VARCHAR(36) PK | API key ID |
| `merchant_id` | UUID / VARCHAR(36) | 绑定商户 |
| `key_prefix` | VARCHAR(16) | 展示和检索用，如 `nf_live_abc123` 前缀 |
| `key_hash` | VARCHAR(256) | HMAC/Argon2/bcrypt hash，不能存明文 |
| `environment` | VARCHAR(16) | `TEST` / `LIVE` |
| `status` | VARCHAR(32) | `ACTIVE` / `DISABLED` / `REVOKED` |
| `scopes` | TEXT / JSONB | `payment:create`、`refund:create` 等 |
| `ip_allowlist` | TEXT / JSONB | 可选 |
| `last_used_at` | TIMESTAMPTZ | 最近使用 |
| `expires_at` | TIMESTAMPTZ | 可选过期时间 |
| `created_by` | UUID / VARCHAR(36) | 创建人 |
| `created_at` / `updated_at` | TIMESTAMPTZ | 时间戳 |

API key 只在创建时返回一次明文。后续只能显示 `key_prefix`、状态、scope 和最近使用时间。

### 4.5 `merchant_webhook_configs`

商户回调配置。

| 字段 | 类型 | 说明 |
|------|------|------|
| `webhook_id` | UUID / VARCHAR(36) PK | 配置 ID |
| `merchant_id` | UUID / VARCHAR(36) | 商户 |
| `url` | VARCHAR(1024) | HTTPS 回调地址 |
| `secret_hash` / `encrypted_secret` | VARCHAR / TEXT | 出站签名 secret，推荐加密存储 |
| `event_types` | TEXT / JSONB | 订阅事件 |
| `status` | VARCHAR(32) | `ACTIVE` / `DISABLED` |
| `retry_policy` | TEXT / JSONB | 重试策略 |
| `last_success_at` / `last_failure_at` | TIMESTAMPTZ | 投递健康 |
| `created_at` / `updated_at` | TIMESTAMPTZ | 时间戳 |

当前 `CreateOrderRequest.notifyUrl` 可保留为一次性 override，但生产默认应使用商户持久化配置；override 必须通过商户配置开关控制，并执行 SSRF/HTTPS 校验。

### 4.6 `merchant_audit_logs`

商户侧和平台侧关键操作审计。

| 字段 | 类型 | 说明 |
|------|------|------|
| `audit_id` | UUID / VARCHAR(36) PK | 审计 ID |
| `merchant_id` | UUID / VARCHAR(36) | 可为空，平台全局操作可为空 |
| `actor_type` | VARCHAR(32) | `MERCHANT_USER` / `INTERNAL_USER` / `API_KEY` / `SYSTEM` |
| `actor_id` | VARCHAR(64) | 用户 ID 或 key ID |
| `action` | VARCHAR(128) | 操作名 |
| `target_type` / `target_id` | VARCHAR | 操作对象 |
| `reason` | VARCHAR(512) | 运营处置原因 |
| `request_id` | VARCHAR(64) | 请求链路 ID |
| `ip_address` | VARCHAR(64) | 来源 IP |
| `before_data` / `after_data` | TEXT / JSONB | 变更快照 |
| `created_at` | TIMESTAMPTZ | 时间戳 |

## 5. 认证与上下文

### 5.1 商户服务器 API key

适用：商户后端调用 `/pay/order`、`/refund/order`、`/fiat/ramp/**`。

流程：

1. 请求携带 `X-API-Key: nf_live_xxx`。
2. Filter 通过 key prefix 找候选记录，验证 hash、状态、过期时间、IP allowlist。
3. 解析出 `merchant_id`、`merchant_code`、`key_id`、`scopes`。
4. 写入 request attributes：
   - `authType=MERCHANT_API_KEY`
   - `merchantId=<internal UUID>`
   - `merchantCode=<external code>`
   - `actorId=<key_id>`
   - `scopes=[...]`
5. Controller/Application 校验请求体中的 `merchantId` 等于 `merchant_code`，或后续直接从上下文填充。
6. 记录 `last_used_at` 和审计日志。

全局 `nexusflow.api.key` 只能作为 dev/test fallback 或迁移期内部 key，不能作为生产商户认证方案。

### 5.2 Merchant Portal 会话

适用：商户用户登录前端。

优先使用 HttpOnly + Secure + SameSite cookie 保存 session。登录后 `/auth/me` 返回：

```json
{
  "userId": "usr_...",
  "email": "ops@example.com",
  "memberships": [
    {
      "merchantId": "9f7c...",
      "merchantCode": "merchant-1",
      "displayName": "Merchant One",
      "roleCode": "OWNER",
      "permissions": ["payment:read", "refund:create", "apikey:manage"]
    }
  ]
}
```

Merchant Portal 调用业务 API 时不应使用商户 API key。它使用登录会话，由服务端根据当前 membership 和 RBAC 决定可访问的 `merchant_id`。

### 5.3 内部 Ops/Admin 会话

内部用户与商户用户分开建模，或者接公司 SSO。内部会话必须带 `internalUserId` 和内部 role/scope。Ops/Admin 可以跨商户查询，但所有写操作必须有权限、原因和审计。

## 6. API 规划

### 6.1 Admin / Ops 商户管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/admin/merchants` | 创建商户 |
| `GET` | `/admin/merchants` | 商户列表，支持状态/关键字分页 |
| `GET` | `/admin/merchants/{merchantId}` | 商户详情 |
| `PATCH` | `/admin/merchants/{merchantId}` | 更新资料 |
| `POST` | `/admin/merchants/{merchantId}/activate` | 激活 |
| `POST` | `/admin/merchants/{merchantId}/suspend` | 冻结，必须传 reason |

### 6.2 Merchant Portal API

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/merchant/me` | 当前用户和商户 membership |
| `GET` | `/merchant/profile` | 当前商户资料 |
| `PATCH` | `/merchant/profile` | 更新允许商户自改的资料 |
| `GET` | `/merchant/api-keys` | API key 列表 |
| `POST` | `/merchant/api-keys` | 创建 API key，返回一次明文 |
| `POST` | `/merchant/api-keys/{keyId}/rotate` | 轮换 |
| `POST` | `/merchant/api-keys/{keyId}/disable` | 禁用 |
| `GET` | `/merchant/webhooks` | Webhook 配置 |
| `POST` | `/merchant/webhooks` | 新增配置 |
| `PATCH` | `/merchant/webhooks/{webhookId}` | 更新配置 |
| `POST` | `/merchant/webhooks/{webhookId}/test` | 发送测试事件 |

### 6.3 商户服务端 API 兼容改造

| 当前接口 | 改造要求 |
|----------|----------|
| `POST /pay/order` | 从 API key 解析商户；请求 `merchantId` 必须与 key 绑定商户一致，后续可允许省略 |
| `GET /pay/order/{paymentId}` | 只能查本商户订单；内部 Ops 走 `/ops` 查询 |
| `POST /refund/order` | 只能退本商户订单；需要 `refund:create` scope |
| `POST /fiat/ramp/quote` | 只能用本商户上下文报价 |
| `POST /fiat/ramp/orders` | 只能创建本商户 ramp order |
| `GET /fiat/ramp/orders/{rampOrderId}` | 只能查本商户 ramp order |

## 7. 数据隔离规则

- 商户 API key 请求不能信任 body 中的 `merchantId`，只能把它当兼容字段校验。
- 所有商户侧查询必须带 `merchant_id` 条件，包括订单、退款、fiat ramp、webhook dead letter 的商户视图。
- Ops/Admin 查询可以跨商户，但必须走内部权限，不复用商户 API key。
- `payment_orders.merchant_id` 和 `fiat_ramp_orders.merchant_id` 建议迁移为内部 UUID；如短期保留 external code，需要新增 `merchant_uuid` 或建立映射视图。
- 所有跨商户操作必须审计：actor、scope、reason、request id、before/after。

## 8. Webhook 策略

当前订单请求允许传 `notifyUrl`。生产建议改成：

1. 默认使用 `merchant_webhook_configs` 中的 active URL。
2. 订单级 `notifyUrl` 作为可选 override，默认关闭。
3. 每个商户有独立 webhook secret，不能继续使用单个全局 `nexusflow.webhook.hmac-secret`。
4. 出站 Webhook payload 必须包含 `event_id`、`event_type`、`merchant_id`、`merchant_order_no`、`payment_id`、`occurred_at`。
5. `webhook_dead_letters` 应补 `merchant_id` 字段，方便商户门户和运营台按商户隔离查询。

## 9. 与 RBAC 的关系

R-18 的 permission client 需要 request attributes 中已有 `userId` 和 `merchantId`。商户体系落地后才能安全提供这些字段：

| 请求类型 | `userId` | `merchantId` | `actorType` |
|----------|----------|--------------|-------------|
| 商户 API key | `UserIdMapper.toUuid(merchantId)` 派生确定性 UUID | API key 绑定商户 | `API_KEY` |
| Merchant Portal | 登录用户 ID（待实现） | 当前选择商户 | `MERCHANT_USER` |
| Ops Console | 固定 ops UUID (`00000000-0000-0000-0000-000000000001`) | 可为空或目标商户 | `INTERNAL_USER` |
| Admin Console | 内部管理员 ID（待实现） | 可为空或目标商户 | `INTERNAL_USER` |

**当前已接入：**
- `flow-permission-client` 已添加为 `flow-api` 依赖
- `ApiKeyAuthFilter` 在认证成功后设置 `nexusflow.userId` request attribute（商户 key 用派生 UUID，全局 key 用固定 ops UUID）
- `@CheckPermission` 已标注所有 controller 方法：
  - 商户端点使用 `scopeType="MERCHANT"`（默认），如 `payment_order:create`、`refund:create`
  - 运营端点使用 `scopeType="SYSTEM"`，如 `ops_dashboard:read`、`orphan:resolve`
- `MerchantUserProvisioningService` 在启动时自动将商户和 ops 用户同步到 `flow-permission` 的 `user_roles` 表
- `GlobalExceptionHandler` 处理 `PermissionDeniedException` 返回 HTTP 403
- 权限配置通过 `permission.*` 属性控制，默认 `enabled: false` 以兼容无权限服务的开发环境

**注意：** `merchantId` 必须是 UUID 格式才能让 MERCHANT scope 的权限检查正常工作。非 UUID 格式的 merchantId 会导致权限检查时 scopeId 解析失败（降级为无 scope 检查）。

**待实现：** Merchant Portal 登录用户的角色同步、API key scopes 细粒度控制、`merchantId` 统一为 UUID 格式。

## 10. 迁移阶段

| 阶段 | 目标 | 说明 |
|------|------|------|
| M0 | 文档和模型确认 | ✅ 本设计文档进入 roadmap；确认 merchant code / UUID 策略 |
| M1 | 商户主数据 | 🟡 新增 `merchant_profiles` 表和 V12 Flyway 迁移；`MerchantProfile` 值对象和 `MerchantProfileEntity` JPA 实体已落地；商户管理 CRUD API 待实现 |
| M2 | 商户级 API key | 🟡 新增 `merchant_credentials` 表和迁移；`MerchantApiKey` DTO、`MerchantCredentialRepository` 端口、`JpaMerchantCredentialRepository` 适配器、`ApiKeyHasher` (SHA-256) 已实现；`ApiKeyAuthFilter` 改造为商户 key 解析，设置 request attributes；key 轮换/禁用/管理 API 待实现 |
| M3 | 业务接口隔离 | 🟡 `/pay`、`/refund`、`/fiat/ramp` POST 接口通过 `MerchantRequestGuard.requireMatchingMerchant()` 校验请求 body `merchantId`；GET 查询接口通过 response `merchantId` 校验所有权；`/ops/*` 和 `/crypto/*` 限制商户 key 访问；`OrderResponse` 新增 `merchantId` 字段；`GlobalExceptionHandler` UNAUTHORIZED 返回 401 |
| M4 | Webhook 持久化 | 新增 `merchant_webhook_configs` 表和迁移已落地，但 webhook dispatch 仍走 per-order `notifyUrl`，尚未接入商户级 webhook 配置 |
| M5 | Merchant Portal 会话 | 🟡 `merchant_users` / `merchant_user_memberships` 表和领域层已落地 (V13)；`MerchantUserProvisioningService` 已实现启动时角色同步；`@CheckPermission` 已接入全部 controller；登录认证（email/password、session/JWT）、`/auth/me` 接口和管理 API 待实现 |
| M6 | Ops/Admin + RBAC | 内部登录、商户管理、权限接入、审计全覆盖（待实现） |
| M7 | 清理全局 key | 生产禁用全局 `nexusflow.api.key`，仅 dev/test fallback（待实现） |

## 11. 第一批实现任务

1. 新增 `flow-domain` 的 merchant 聚合：`MerchantProfile`、`MerchantCredential`、`MerchantWebhookConfig`。
2. 新增 Flyway：商户表、API key 表、Webhook 配置表、审计表。
3. 新增 `MerchantRepository` / `MerchantCredentialRepository` 端口和 JPA 实现。
4. 改造 `ApiKeyAuthFilter`：从全局字符串比较改为商户 key 解析，并写入 request attributes。
5. 改造 `/pay`、`/refund`、`/fiat/ramp`：校验请求 merchant 与认证上下文一致。
6. 新增管理 API：创建商户、创建/禁用/轮换 API key、Webhook 配置 CRUD。
7. 为订单、fiat ramp、webhook dead letter 补商户隔离查询和测试。
8. 明确全局 `nexusflow.api.key` 的 dev/test fallback 行为，并在 prod profile 禁用。

## 12. 风险与决策点

- 是否立即把历史 `merchant_id` 改成 UUID：直接改最干净，但需要迁移现有订单；保留 code 最兼容，但长期权限 scope 会混乱。
- API key hash 方案：建议用不可逆 hash，并加服务端 pepper；不能加密后可逆保存。
- Merchant Portal 是否自建账号密码：短期可自建，长期最好支持 SSO/OIDC。
- Webhook secret 是否每个 URL 一个：建议是，方便轮换和多 endpoint 管理。
- `flow-permission` 接入业务接口的时机：✅ 已完成。`@CheckPermission` 已接入全部 controller，商户 key 通过 `UserIdMapper` 派生确定性 UUID 作为 userId，全局 key 使用固定 ops UUID。默认 `permission.enabled=false` 以兼容无权限服务的开发环境。
