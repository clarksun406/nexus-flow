# NexusFlow 前端产品设计与实施规划

> 当前结论：前端已拆为 `frontend-checkout` / `frontend-merchant` / `frontend-ops` / `frontend-admin` 4 个 Maven 静态资源模块，并已建立 `frontend/` Vue 3 + Vite + TypeScript workspace。四端 Vue app、共享 API/UI 包、Node smoke、Playwright Chromium mock E2E 和 CI 前端验证已落地；旧静态页仅作为 legacy fallback 和迁移参考。

## 1. 现状判断

| 位置 | 当前作用 | 主要问题 |
|------|----------|----------|
| `frontend/apps/checkout` | 买家收银台 Vue app，覆盖订单加载、地址生成、二维码/复制、倒计时和状态轮询 | 缺 checkout token、品牌配置、移动钱包 deep link、异常页和真实后端 E2E |
| `frontend/apps/merchant` | 商户门户 Vue app，覆盖登录、商户切换、创建订单、订单查询、退款和最近订单缓存 | 缺 API key 管理、Webhook 配置持久化、分页订单列表、报表和团队管理 |
| `frontend/apps/ops` | 运营控制台 Vue app，覆盖登录、dashboard、orphan 处置、webhook dead letter replay/ignore | 缺正式分页筛选、处置原因/审计字段、跨商户查询边界和真实后端 E2E |
| `frontend/apps/admin` | 平台管理端 Vue app，覆盖登录、角色管理、权限分配、用户角色授权/撤销 | 缺商户生命周期、provider/system 配置、审计页面和权限服务真实联调 E2E |
| `frontend/legacy-static/` | 旧静态 Demo 页面归档 | 仅作为迁移参考，不再作为正式前端源码主线 |

现在最大差距已经不是前端工程底座，而是真实后端联调、产品级商户/权限/配置/审计能力，以及正式部署、灰度回滚和前端观测体系。

## 2. 需要分几个端

建议按产品边界分 **4 个端**，不要把商户、运营、平台管理混在一个静态控制台里。

| 端 | 用户 | 登录要求 | 核心职责 | 当前状态 |
|----|------|----------|----------|----------|
| 买家收银台 Checkout | 付款用户 | 默认不登录，使用 checkout token / `payment_id` | 展示订单、币种/网络、收款地址/二维码、倒计时、支付状态、失败/过期处理 | Vue app 已落地，mock E2E 已覆盖；缺真实后端 E2E 和 checkout token 产品化 |
| 商户门户 Merchant Portal | 商户管理员、开发者、财务、客服 | 必须登录，绑定 merchant | 订单、退款、API key、Webhook、回调日志、团队成员、结算/报表、fiat ramp | Vue app 已落地登录/建单/查单/退款；缺 API key/Webhook/团队/报表 |
| 运营控制台 Ops Console | 内部运营、风控、技术支持 | 必须登录，内部 RBAC | 通道健康、全局订单监控、orphan transaction、webhook dead letter、对账 backlog、风险处置 | Vue app 已落地 dashboard/orphan/DL 操作；缺分页筛选、原因审计和真实后端 E2E |
| 平台管理端 Admin Console | 平台超级管理员、系统管理员 | 必须登录，高权限 RBAC | 商户生命周期、商户凭证、角色权限、通道/provider 配置、系统策略、审计 | Vue app 已落地角色/权限/用户授权；缺商户/provider/system/audit 管理 |

暂不单独拆“开发者文档端”。API 文档、SDK、Webhook 示例可以先放进 Merchant Portal 的 Developer 区域；等开放平台化后再拆 Developer Portal。

## 3. 推荐前端工程形态

目标是一个前端 workspace，多个应用入口，共享组件和 API client：

```text
frontend/
  package.json
  apps/
    checkout/          # 买家收银台
    merchant/          # 商户门户
    ops/               # 运营控制台
    admin/             # 平台管理端
  packages/
    ui/                # 表格、表单、状态标签、弹窗、布局、主题
    api-client/        # 类型化 API client、错误处理、重试、分页模型
    auth/              # 会话、权限、路由守卫
    config/            # 环境变量、构建配置、lint/test 配置
```

当前实现采用 Vue 3 + TypeScript + Vite + npm workspaces。关键不是框架本身，而是要有构建、类型、API client、路由、权限、测试和部署边界。

静态 Maven 模块后续只负责托管入口页或接收构建产物，不再作为前端源码主工程。旧静态页面已迁入 `frontend/legacy-static/` 作为迁移参考。

## 4. 后端依赖与缺口

产品级前端不能绕过后端能力直接做。商户侧能力的详细模型见 `nexusflow-merchant-design.md`。前端正式落地前，至少需要补以下后端 API。

| 能力 | 必要 API / 模型 | 依赖 Roadmap |
|------|------------------|--------------|
| 登录会话 | `POST /auth/login`、`POST /auth/logout`、`GET /auth/me`、refresh/session 管理 | R-16 / R-18 |
| 商户身份 | `MerchantProfile`、商户状态、商户用户、merchant UUID 与外部 merchant code 映射；详见 `nexusflow-merchant-design.md` | R-16 |
| 商户 API key | 创建、展示一次、hash 存储、轮换、禁用、最后使用时间、IP allowlist；详见 `nexusflow-merchant-design.md` | R-16 |
| Webhook 配置 | callback URL、secret、事件订阅、重试策略、测试发送、投递日志；详见 `nexusflow-merchant-design.md` | R-16 / R-5 |
| 权限 | 用户、角色、权限点、商户 scope、内部 scope、服务端鉴权 | R-18 |
| 商户订单列表 | 按 merchant scope 查询、分页、筛选、导出、详情、时间线 | R-16 / R-17 |
| 退款管理 | 退款列表、详情、审批/提交、状态流、失败重试记录 | R-17 |
| 运营处置 | orphan、dead letter、对账、通道健康的分页查询、操作审计、确认流 | R-17 / R-18 |
| 平台管理 | 商户创建/审核/冻结、凭证管理、通道/provider 配置、系统审计 | R-16 / R-18 |

浏览器端不应再保存全局 `X-API-Key`。商户 API key 是给商户服务器调用的，不是给 Merchant Portal 前端直接调用的。

## 5. 安全模型

- Checkout 是公开端，只能通过一次性或可校验的 checkout token / `payment_id` 读取有限订单信息；不得暴露商户 API key。
- Merchant / Ops / Admin 都必须使用登录会话。优先考虑 HttpOnly + Secure + SameSite cookie；如果使用 Bearer token，也必须有短有效期、refresh、退出和设备管理。
- RBAC 必须在服务端强制执行。前端隐藏菜单和按钮只改善体验，不能作为安全边界。
- API key 只在创建时展示一次，后端只存 hash；支持轮换、禁用、审计、最后使用时间。
- 内部运营操作必须有二次确认、操作者、原因、前后状态和审计日志。

## 6. 各端页面范围

### 6.1 Checkout

- 订单摘要：商户名、订单号、金额、币种、网络、过期时间。
- 支付动作：二维码、地址复制、金额复制、网络切换限制、移动端钱包跳转。
- 状态体验：待支付、部分支付、确认中、已完成、已过期、失败、重复支付提示。
- 安全与品牌：商户品牌展示、checkout token 校验、敏感字段最小化。
- 测试：移动端 viewport、倒计时、轮询、过期、异常 API、复制行为 E2E。

### 6.2 Merchant Portal

- Dashboard：订单量、成功率、待处理退款、Webhook 失败、通道可用性摘要。
- Orders：列表、筛选、详情、支付时间线、链上 tx、收银台链接。
- Refunds：发起退款、查看退款状态、失败重试、退款限制说明。
- Developers：API key、Webhook 配置、签名文档、测试回调、事件日志。
- Team：成员、角色、邀请、禁用、最近登录。
- Reports：订单导出、对账报表、fiat ramp 订单。

### 6.3 Ops Console

- Global Dashboard：通道健康、订单状态、异常趋势、积压任务。
- Channels：provider 健康、费率、失败率、禁用/恢复、配置只读或受控编辑。
- Risk / Orphans：orphan transaction 查询、resolve、ignore、compensate、审计原因。
- Webhook DLQ：dead letter 查询、payload 摘要、replay、ignore、失败原因。
- Reconciliation：对账 backlog、retry、reorg、missing event。
- Support View：按 merchant/payment/txHash 快速定位全链路。

### 6.4 Admin Console

- Merchants：创建、审核、冻结、资料维护、商户状态流。
- Credentials：商户 API key、Webhook secret、IP allowlist、凭证审计。
- RBAC：权限点、角色模板、用户授权、商户 scope / 内部 scope。
- Providers：Coinbase、BitMart、Binance、MoonPay/Ramp/Banxa 配置状态。
- System：环境配置只读检查、feature flag、审计日志、后台任务状态。

## 7. 路由与部署

建议部署时按域名或路径隔离：

| 端 | 推荐地址 |
|----|----------|
| Checkout | `https://pay.example.com/checkout/:token` |
| Merchant Portal | `https://merchant.example.com` |
| Ops Console | `https://ops.example.com` |
| Admin Console | `https://admin.example.com` |

本地开发统一走 Vite dev server，通过 `/api` proxy 到 `flow-api`。生产构建可以输出到独立静态资源服务/CDN；当前 Spring Boot 托管路径已接入 Maven：先在 `frontend/` 执行 `npm.cmd run verify:e2e` 或 `npm.cmd run build`，再打包 `frontend-*` 模块，Maven 会把对应 `frontend/apps/*/dist-app` 作为 `static/app/` 资源带入 jar。

## 8. 实施阶段

| 阶段 | 目标 | 产出 |
|------|------|------|
| F0 | 文档和边界确认 | 本设计文档、roadmap 关联、legacy 静态页面定位 |
| F1 | 前端工程底座 | `frontend` workspace、Vue/Vite/TS、共享 UI、API client、路由、环境配置、Playwright 基础 |
| F2 | 身份与商户底座 | 登录会话、商户 profile、商户用户、商户级 API key、Webhook 持久化 |
| F3 | Merchant Portal MVP | 订单列表/详情、创建测试订单、退款、API key、Webhook 配置 |
| F4 | Checkout 产品化 | checkout token、品牌配置、移动端支付体验、异常状态、E2E |
| F5 | Ops Console 产品化 | 通道监控、orphan 处置、dead letter 处置、对账视图、审计 |
| F6 | Admin + RBAC | 商户生命周期、角色权限、内部用户、permission 服务接入 |
| F7 | 发布与质量 | CI 构建、E2E、跨浏览器回归、错误上报、前端访问日志、灰度发布 |

优先级建议：先补 R-16 商户体系，再做 Merchant Portal MVP；Checkout 可并行产品化，因为它不依赖商户登录，但依赖更安全的 checkout token；Ops/Admin 必须等身份和 RBAC 边界稳定后再做正式版。

## 9. 第一批可执行任务

1. 建立 `frontend` workspace，保留现有静态页为 legacy demo。
2. 定义 API client 的统一响应、错误、分页、鉴权处理。
3. 后端新增商户身份与会话 API，替代全局 `X-API-Key` 浏览器输入。
4. Merchant Portal 先做订单列表/详情、API key 管理、Webhook 配置三条闭环。
5. Checkout 增加 checkout token 与异常状态 E2E，停止依赖手填 API base 的演示模式。
6. Ops Console 的所有处置动作补操作原因和审计日志后再开放给正式前端。

---

## 10. 前端模块重组 Roadmap

### 10.1 迁移前状态（已解决的问题）

迁移前，所有端的页面堆在 `flow-cashier` 一个模块里：

```
flow-cashier/src/main/resources/static/
├── index.html         ← 入口导航（链接到下面三个）
├── checkout.html      ← 买家收银台
├── merchant.html      ← 商户端
├── ops.html           ← 运营端
├── app.html           ← 早期交互演示
└── pages/             ← 早期 demo（7 个子页面）
```

**已解决的问题：**
- 三个不同角色的页面混在一起，没有模块边界
- `flow-cashier` 名字暗示"收银台"，实际承载了所有端
- 共享同一个 Spring Boot 静态资源目录，无法独立部署
- 没有 Admin Console（权限管理）页面

### 10.2 目标状态

按产品边界拆为 4 个 Maven 模块，每个模块对应一个端：

```
nexusflow/
├── frontend-checkout/       ← 买家收银台（原 flow-cashier）
│   └── static/
│       ├── index.html
│       └── checkout.html
│
├── frontend-merchant/       ← 商户门户（新建）
│   └── static/
│       ├── index.html
│       └── merchant.html
│
├── frontend-ops/            ← 运营控制台（新建）
│   └── static/
│       ├── index.html
│       └── ops.html
│
├── frontend-admin/          ← 平台管理端（新建）
│   └── static/
│       ├── index.html
│       ├── admin-permissions.html
│       └── admin-users.html
│
└── frontend/                ← Vue/Vite workspace（正式前端源码主线）
    ├── apps/
    │   ├── checkout/
    │   ├── merchant/
    │   ├── ops/
    │   └── admin/
    └── packages/
        ├── ui/
        ├── api-client/
        └── auth/
```

### 10.3 分阶段实施

#### FR-0：建 frontend-admin 模块 ✅ DONE (2026-07-10)

**目标：** Admin Console 有独立归属，权限管理页面有地方放。

| 任务 | 产出 | 状态 |
|------|------|------|
| 新建 `frontend-admin/pom.xml` | 同 `frontend-*` 静态资源模块，只依赖 spring-boot-starter-web | ✅ |
| 新建 `frontend-admin/src/main/resources/static/index.html` | Admin 入口页，链接到权限管理和用户管理 | ✅ |
| 新建 `frontend-admin/src/main/resources/static/admin-permissions.html` | 角色列表/创建/删除、权限码查看、角色-权限映射、用户-角色分配 | ✅ |
| 根 `pom.xml` 加 `<module>frontend-admin</module>` | Maven 构建包含新模块 | ✅ |
| flow-api 新增 `PermissionManagementController` | 代理转发到 flow-permission-server API（roles/permissions/users） | ✅ |

**文件清单：** 5 个新建，1 个修改

#### FR-1：拆分 flow-cashier → frontend-checkout + frontend-merchant + frontend-ops ✅ DONE (2026-07-10)

**目标：** 每个端有独立模块，职责清晰。

| 任务 | 产出 | 状态 |
|------|------|------|
| `flow-cashier` 重命名为 `frontend-checkout` | 只保留 checkout.html + index.html + legacy pages | ✅ |
| 新建 `frontend-merchant/pom.xml` + `static/` | merchant.html 移入 + 新建 index.html | ✅ |
| 新建 `frontend-ops/pom.xml` + `static/` | ops.html 移入 + 新建 index.html | ✅ |
| 根 `pom.xml` 更新 module 列表 | 替换 flow-cashier 为 frontend-checkout/frontend-merchant/frontend-ops | ✅ |
| 各模块 `index.html` 更新 | 每个端有自己的入口页 | ✅ |
| 删除 `flow-cashier` 目录 | 清理旧模块 | ✅ |
| `frontend/` 目录清理 | 旧 demo 页面迁入 `legacy-static/`，为 Vue workspace 腾出根目录 | ✅ |

**文件清单：** 6 个新建/移动，3 个修改，1 个删除

#### FR-2：Admin Console 完善 ✅ DONE (2026-07-10)

**目标：** 权限管理能力完整。

| 任务 | 产出 | 状态 |
|------|------|------|
| `admin-users.html` | 用户角色查询、授权、撤销 | ✅ |
| 后端 `PermissionManagementController` 完善 | 角色 CRUD 代理、用户角色分配代理、权限码列表代理 | ✅ |
| 测试 | Controller 测试 + 页面手动验证 | ⬜ 后续补 |

**文件清单：** 2 个新建，1 个修改

#### FR-3：登录体系接入 ✅ 后端 DONE / 前端页面 DONE (2026-07-10)

**目标：** Merchant/Ops/Admin 三个端都有登录能力。

| 任务 | 产出 | 状态 |
|------|------|------|
| 后端 `AuthController` | `/auth/login`、`/auth/logout`、`/auth/me` | ✅ |
| 后端 `AuthService` + session 支持 | BCrypt 验密、session 写入、`ApiKeyAuthFilter` session 回退 | ✅ |
| `merchant-login.html` | 商户登录页 | ✅ |
| `ops-login.html` | 运营登录页 | ✅ |
| `admin-login.html` | 管理端登录页 | ✅ |
| 各端页面改造 | 移除 localStorage API key 配置，改用 session | ✅ merchant.html/ops.html 已改为 `/auth/login` + session cookie；admin 页面也通过 session 调用 `/admin/**` |
| Flyway V14 | Spring Session JDBC 表 | ✅ |

**文件清单：** 8 个新建，4 个修改

#### FR-4：前端工程化（DONE，后续补真实后端 E2E / 部署 / 观测，2026-07-11）

**目标：** 从静态 HTML 迁移到 Vue/Vite 工程。

| 任务 | 产出 |
|------|------|
| 建立 `frontend/` workspace | package.json、Vite、TypeScript、4 个 app 入口 |
| 共享包 | `packages/ui`、`packages/api-client`、`packages/config` |
| 逐端迁移 | Checkout、Merchant、Admin、Ops 已完成第一版 Vue 业务闭环 |
| CI/CD | 构建、测试、部署流水线 |

**当前状态：** workspace 已完成脚手架、共享包、`legacy-static/` 迁移，以及 4 个 app 的 `npm run verify:e2e` 验证；Checkout 已迁入 Vue，覆盖订单加载、地址生成、QR、复制、倒计时和状态轮询；Merchant Portal 已迁入 Vue，覆盖登录、商户切换、创建订单、订单查询、退款提交和最近订单本地缓存；Admin Console 已迁入 Vue，覆盖登录、角色管理、权限分配、用户角色查询、授权和撤销；Ops Console 已迁入 Vue，覆盖登录、通道/订单/对账 dashboard、orphan resolve/compensate/ignore、webhook dead letter replay/ignore；`frontend-*` Maven 模块已把对应 Vite `dist-app` 接入 `static/app/` 打包；已补 Node 内置 smoke tests 覆盖 workspace、API wiring、构建产物和 Maven handoff；已补 Playwright Chromium E2E，覆盖四端 Vue 构建产物能通过 HTTP 启动，并用 mock API 跑通 checkout 生成地址、merchant 登录建单、ops orphan resolve、admin 创建角色并分配权限；GitHub Actions 已在 Maven 构建前执行 `npm ci` + `npm run verify:e2e` 并上传 `dist-app` 产物。后续补真实后端联调 E2E 与正式部署流水线。

### 10.4 实施顺序与依赖

```
FR-0 frontend-admin 模块 ────────────────────────────┐
       ↓                                                │
FR-1 拆分 flow-cashier ────────────────────────────────┤
       ↓                                                │
FR-2 Admin Console 完善 ───────────────────────────────┤
       ↓                                                │
FR-3 登录体系（后端 AuthController + 前端登录页）────────┤
       ↓                                                │
FR-4 前端工程化（已完成底座，进入联调/部署阶段）──────────────┘
```

FR-0 和 FR-1 已完成模块拆分。FR-2/FR-3 已完成第一版权限与登录接入。FR-4 的工程底座、Vue 四端、mock E2E 和 CI 验证已完成，后续重点是真实后端联调 E2E、正式部署、灰度回滚和前端观测。

### 10.5 文件变更汇总

| 阶段 | 新建 | 修改 | 删除 | 合计 |
|------|------|------|------|------|
| FR-0 | 5 | 1 | 0 | 6 |
| FR-1 | 6 | 3 | 1 | 10 |
| FR-2 | 2 | 1 | 0 | 3 |
| FR-3 | 5-8 | 3-4 | 0 | 8-12 |
| **合计** | **18-21** | **8-9** | **1** | **27-31** |

### 10.6 各模块职责对照

| 模块 | 端 | 用户 | 认证方式 | 页面 |
|------|----|------|----------|------|
| `frontend-checkout` | 买家收银台 | 付款用户 | checkout token / payment_id | index, checkout |
| `frontend-merchant` | 商户门户 | 商户用户 | session (email/password) | index, merchant, login |
| `frontend-ops` | 运营控制台 | 内部运营 | session (内部账号) | index, ops, login |
| `frontend-admin` | 平台管理端 | 平台管理员 | session (高权限) | index, admin-permissions, admin-users, login |

### 10.7 后端 API 对应

| 模块 | 调用的后端 API |
|------|---------------|
| `frontend-checkout` | `/cashier/order/status`、`/cashier/pay/submit`（无需登录） |
| `frontend-merchant` | `/pay/*`、`/refund/*`、`/fiat/ramp/*`、`/auth/*`（MERCHANT scope） |
| `frontend-ops` | `/ops/*`、`/crypto/orphan-transactions`、`/ops/webhook-dead-letters`（SYSTEM scope） |
| `frontend-admin` | `/api/v1/role/*`、`/api/v1/user/*`、`/api/v1/permission/*`（SYSTEM scope，通过代理） |
