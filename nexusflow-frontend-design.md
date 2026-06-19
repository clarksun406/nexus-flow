# NexusFlow 前端产品设计与实施规划

> 当前结论：现有前端是随 `flow-cashier` 打包的静态 HTML/CSS/JS 页面，以及 `frontend/` 下的早期页面 Demo。它可以用于本地演示和 API smoke，但还不是产品级前端体系。

## 1. 现状判断

| 位置 | 当前作用 | 主要问题 |
|------|----------|----------|
| `flow-cashier/src/main/resources/static/index.html` | 静态入口页，链接到 checkout / merchant / ops | 不是登录后的产品门户，没有路由、权限、会话 |
| `flow-cashier/src/main/resources/static/checkout.html` | 买家收银台，已调用 `/cashier/order/status` 和 `/cashier/pay/submit` | 仅按 `payment_id` 访问，缺签名 checkout token、品牌配置、异常页、E2E |
| `flow-cashier/src/main/resources/static/merchant.html` | 商户演示控制台，调用 `/pay/order`、`/pay/order/{paymentId}`、`/refund/order` | API key、callback URL 存在浏览器 `localStorage`；无商户账号、会话、团队、权限、持久化配置 |
| `flow-cashier/src/main/resources/static/ops.html` | 运营演示台，调用 `/ops/dashboard`、`/crypto/orphan-transactions`、`/ops/webhook-dead-letters` | 用全局 `X-API-Key` 访问；无内部账号、RBAC、审计视图、分页筛选、操作确认流 |
| `frontend/` | 早期收银台页面副本 / 静态设计稿 | 不是构建工程，没有包管理、组件体系、API client、测试和部署流水线 |

现在最大差距不是 UI 细节，而是产品前端需要依赖的身份、商户、权限、配置、审计和部署体系还没有形成。

## 2. 需要分几个端

建议按产品边界分 **4 个端**，不要把商户、运营、平台管理混在一个静态控制台里。

| 端 | 用户 | 登录要求 | 核心职责 | 当前状态 |
|----|------|----------|----------|----------|
| 买家收银台 Checkout | 付款用户 | 默认不登录，使用 checkout token / `payment_id` | 展示订单、币种/网络、收款地址/二维码、倒计时、支付状态、失败/过期处理 | 有静态页和真实 `/cashier` API |
| 商户门户 Merchant Portal | 商户管理员、开发者、财务、客服 | 必须登录，绑定 merchant | 订单、退款、API key、Webhook、回调日志、团队成员、结算/报表、fiat ramp | 只有 `merchant.html` 演示页 |
| 运营控制台 Ops Console | 内部运营、风控、技术支持 | 必须登录，内部 RBAC | 通道健康、全局订单监控、orphan transaction、webhook dead letter、对账 backlog、风险处置 | 只有 `ops.html` 演示页 |
| 平台管理端 Admin Console | 平台超级管理员、系统管理员 | 必须登录，高权限 RBAC | 商户生命周期、商户凭证、角色权限、通道/provider 配置、系统策略、审计 | 当前没有 |

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

实现上可以先用 React + TypeScript + Vite；是否使用 pnpm/npm workspaces 后续按团队习惯定。关键不是框架，而是要有构建、类型、API client、路由、权限、测试和部署边界。

`flow-cashier` 后续只负责打包或托管构建产物，不再作为前端源码的主工程。现有静态页面可以保留为 legacy demo，避免混淆时应改名或移入 `legacy-static/`。

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

本地开发可以统一走 Vite dev server，通过 `/api` proxy 到 `flow-api`。生产构建可以输出到独立静态资源服务/CDN；如需继续由 Spring Boot 托管，则由构建流程把产物复制进 `flow-cashier` 或 `flow-api` 的 static 目录。

## 8. 实施阶段

| 阶段 | 目标 | 产出 |
|------|------|------|
| F0 | 文档和边界确认 | 本设计文档、roadmap 关联、legacy 静态页面定位 |
| F1 | 前端工程底座 | `frontend` workspace、React/Vite/TS、共享 UI、API client、路由、环境配置、Playwright 基础 |
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
