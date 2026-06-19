# NexusFlow

> 多链数字资产支付引擎 · **执行层 + 编排层** 一体化（Java 17 / Spring Boot 3.3.5 / DDD 模块化单体）

NexusFlow 是 Nexus 生态中的加密支付引擎。它同时承担两层职责：

- **执行层（Data Plane）** — 链上交易执行、钱包抽象、交易状态跟踪（ETH / TRON / BTC 适配器）
- **编排层（Orchestration）** — 商户法币/数币订单编排，对接收单通道（交易所 / 支付商 / 自建节点通道）

```
                          ┌──────────────────────────────┐
   商户 ──法币/数币订单──→ │  编排层  PaymentOrder/Router   │──→  收单通道 / 自建节点通道
                          │  ────────────────────────────│
   买家 ←── 收银台页面 ──  │  执行层  CryptoPayment/Wallet  │──→  区块链节点 (ETH/TRON/BTC)
                          └──────────────────────────────┘
```

详细设计见 [`nexusflow.md`](./nexusflow.md)，路线图见 [`nexusflow-roadmap.md`](./nexusflow-roadmap.md)，商户体系设计见 [`nexusflow-merchant-design.md`](./nexusflow-merchant-design.md)，前端产品规划见 [`nexusflow-frontend-design.md`](./nexusflow-frontend-design.md)。

> 当前状态口径：核心代码、端口、持久化、API 和离线测试已大量落地；生产可用性仍取决于真实节点、真实通道、Docker/Testcontainers、Redis/Kafka、MPC、GasBank 和 fiat-ramp provider 的 live 验证。不要把 roadmap 中的代码完成度直接等同于生产闭环完成。

---

## 模块结构

| 模块 | 职责 |
|------|------|
| `flow-common` | 共享工具：AES-256-GCM 加密、统一响应、错误码、异常 |
| `flow-domain` | 领域核心：聚合根、状态机、值对象、领域事件、仓储/通道端口 |
| `flow-application` | 用例编排：支付编排器、执行层支付服务、退款、Webhook |
| `flow-infra` | 基础设施：链上适配器、通道适配器、持久化、事件发布、Webhook 客户端 |
| `flow-listener` | 区块扫描 / 索引 / 对账 |
| `flow-wallet` | 钱包服务、地址派生 |
| `flow-api` | REST API、收银台 / 回调入口、Flyway 迁移 |
| `flow-cashier` | 收银台、商户、运营静态页面资源；当前不是完整产品级前端应用 |
| `frontend` | 早期收银台静态页面 / Demo；正式前端 workspace 规划见 `nexusflow-frontend-design.md` |

### 架构约束（DDD）

- 严格分层：`api → application → domain → infra`，领域层不依赖基础设施
- 所有状态变更必须事件驱动；外部系统只通过事件 / API 交互
- 所有入站接口幂等（`orderId` / `eventId` 去重）
- 区块链是事实源，系统状态由对账派生（最终一致）

---

## 技术栈

Java 17 · Spring Boot 3.3.5 · PostgreSQL + Flyway · Redis · web3j / tron4j / bitcoinj · MapStruct · Lombok · JUnit 5 / Mockito / AssertJ

---

## 构建与测试

```bash
# 编译
mvn -DskipTests compile

# 运行单元测试
mvn test

# 全量打包
mvn install
```

> 测试运行依赖 `maven-surefire-plugin` 3.2.5（已在父 POM 固定），以支持 JUnit 5。

### 本地运行

需要 PostgreSQL（`nexusflow` 库）。Redis 仅在启用缓存、Redis 幂等或相关 live smoke 时需要。敏感配置通过环境变量注入：

```bash
export DB_PASSWORD=...
export ENCRYPTION_KEY=...          # AES-256-GCM 密钥
export TRON_NODE_URL=https://api.trongrid.io
mvn -pl flow-api spring-boot:run
```

---

## 状态机

**执行层 CryptoPayment**

```
CREATED → PENDING → DETECTED → CONFIRMING → CONFIRMED
                                          → FAILED
                  → EXPIRED
```

**编排层 PaymentOrder**

```
WAITING_PAYMENT → CONFIRMED → REFUND_PROCESSING → REFUNDED / REFUND_FAILED
                → PARTIALLY_PAID → CONFIRMED
                → EXPIRED
```

转换由聚合根强制校验，禁止非法跳转与回退。

---

## 当前进度

- **编排引擎核心**：✅ 代码已落地（订单/退款/通道领域模型、编排服务、JPA 持久化、商户/收银台 API、收银台/商户/运营静态页面）
- **执行层核心**：✅ 代码已落地（ETH/BTC/TRON 适配器、HD 钱包、地址池、对账/过期调度、幂等、orphan transaction、Webhook dead letter）
- **真实通道**：🟡 Coinbase Commerce 有 REST charge/rate 实现但待 live 验证；BitMart/Binance 仍为非 prod stub
- **生产前缺口**：🟡 产品级前端/控制台体系（Checkout、Merchant Portal、Ops Console、Admin Console）、商户级认证/多租户、RBAC 权限服务接入、Docker-backed 集成测试、真实链节点、Redis/Kafka、生产 Webhook、Coinbase live、MPC provider、GasBank/live fee oracle、MoonPay/Ramp/Banxa 官方适配仍需补齐或验证
- 详见 [`nexusflow-roadmap.md`](./nexusflow-roadmap.md) 的“生产就绪度摘要”和“生产前剩余风险 / 未验证项”，[`nexusflow-merchant-design.md`](./nexusflow-merchant-design.md) 的商户体系规划，以及 [`nexusflow-frontend-design.md`](./nexusflow-frontend-design.md) 的前端分端规划

---

## 安全

- 私钥不得明文存储；支持外部 KMS（AWS KMS / Vault）
- 敏感数据静态加密，密钥仅经环境变量 / KMS 注入
- 签名与 API 层严格隔离；私钥不通过任何 API 暴露
