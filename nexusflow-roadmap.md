# nexusflow — 多通道支付编排引擎 实现计划与 Roadmap

> 基于 PRD `1.1- 收银台产品 PRD（法币版）.md` 和 UI `channelpay-pages`
> 整合进现有 nexusflow 项目，在原有链上执行层之上增加编排层
> 生成时间：2026-06-07

---

## 一、定位升级

nexusflow 从纯执行层（Data Plane）升级为**执行+编排一体化引擎**：

```
                          ┌──────────────────────────────┐
                          │         nexusflow             │
  商户 ──法币/数币订单──→  │                              │
                          │  ┌──────────────────────────┐ │
                          │  │  编排层 (Orchestration)    │ │
                          │  │  PaymentOrder / Router    │ │      ┌──────────┐
                          │  │  ChannelAdapter 端口       │─┼─────→│ BitMart  │
                          │  └──────────┬───────────────┘ │      │ Binance  │
                          │             │                  │      │ Coinbase │
                          │  ┌──────────▼───────────────┐ │      │ 自建节点 │
                          │  │  执行层 (Execution)        │ │      └──────────┘
                          │  │  CryptoPayment / Wallet   │ │
                          │  │  链上适配器 ETH/TRON/BTC  │ │
                          │  └──────────────────────────┘ │
                          │                              │
  买家 ←──收银台页面────   │  ┌──────────────────────────┐ │
                          │  │  Cashier UI               │ │
                          │  └──────────────────────────┘ │
                          └──────────────────────────────┘
```

---

## 二、模块规划（在现有项目上扩展）

```
nexusflow/                              # 已存在
│
├── flow-common/                         # 已存在（不变）
│
├── flow-domain/                         # 扩展
│   ├── payment/                         #   已存在：CryptoPayment(执行层)
│   │   ├── CryptoPayment.java
│   │   ├── PaymentStatus.java
│   │   └── PaymentRepository.java
│   ├── order/                           #   新增：编排层
│   │   ├── PaymentOrder.java            #     支付订单聚合根
│   │   ├── PaymentFlow.java             #     支付流水实体
│   │   ├── OrderStatus.java             #     订单状态机
│   │   ├── FlowStatus.java              #     流水状态机
│   │   └── OrderRepository.java         #     仓储端口
│   ├── refund/                          #   新增
│   │   ├── RefundOrder.java             #     退款单聚合根
│   │   ├── RefundStatus.java            #     退款状态机
│   │   └── RefundRepository.java        #     仓储端口
│   ├── channel/                         #   新增：通道抽象（核心）
│   │   ├── ChannelAdapter.java          #     通道适配器端口
│   │   ├── ChannelRouter.java           #     路由端口
│   │   ├── CurrencyConfig.java          #     币种配置 VO
│   │   ├── DepositAddress.java          #     充值地址 VO
│   │   ├── ExchangeRate.java            #     汇率 VO
│   │   ├── ChannelUser.java             #     通道用户 VO
│   │   └── ChannelRefund.java           #     通道退款 VO
│   ├── event/                           #   已存在 + 扩展
│   │   ├── DomainEvent.java
│   │   ├── PaymentStateChangedEvent.java #    执行层事件(已存在)
│   │   ├── OrderEvent.java              #     编排层事件(新增)
│   │   └── DomainEventPublisher.java
│   ├── wallet/                          #   已存在
│   └── shared/                          #   已存在
│
├── flow-application/                    # 扩展
│   ├── PaymentOrchestrator.java         #   新增：支付编排（核心）
│   ├── CashierService.java              #   新增：收银台服务
│   ├── OrderService.java                #   新增：订单查询
│   ├── RefundService.java               #   新增：退款服务
│   ├── WebhookService.java              #   新增：商户回调
│   ├── PaymentApplicationService.java   #   已存在：执行层支付
│   ├── WalletApplicationService.java    #   已存在
│   └── dto/                             #   扩展
│
├── flow-infra/                          # 扩展
│   ├── adapter/                         #   新增：通道适配器实现
│   │   ├── bitmart/
│   │   │   ├── BitMartAdapter.java
│   │   │   └── BitMartClient.java
│   │   └── stub/
│   │       └── StubAdapter.java
│   ├── router/
│   │   └── DefaultChannelRouter.java    #   新增
│   ├── webhook/
│   │   └── HttpWebhookClient.java       #   新增
│   ├── persistence/                     #   扩展
│   └── ...
│
├── flow-api/                            # 扩展
│   ├── controller/
│   │   ├── PayController.java           #   新增：POST /pay/order
│   │   ├── RefundController.java        #   新增：POST /refund/order
│   │   ├── CashierController.java       #   新增：GET /cashier/...
│   │   ├── CallbackController.java      #   新增：通道回调入口
│   │   └── ...
│   └── resources/db/migration/
│       ├── V1__init_schema.sql          #   已存在
│       ├── V2__order_tables.sql         #   新增
│       └── V3__refund_tables.sql        #   新增
│
└── flow-cashier/                        # 新增：收银台前端
    ├── pom.xml
    └── src/main/resources/static/       # Spring Boot 静态资源
        ├── checkout.html
        ├── app.js
        └── styles/
```

---

## 三、核心接口

### ChannelAdapter（通道适配器端口）

```java
public interface ChannelAdapter {
    String channelId();              // "BITMART", "BINANCE_PAY"
    String displayName();
    ChannelUser openUser(String merchantId, String buyerId);
    DepositAddress createDepositAddress(CreateDepositRequest req);
    ChannelRefund refund(RefundRequest req);
    ChannelRefund queryRefund(String channelRefundId);
    List<CurrencyConfig> getSupportedCurrencies();
    ExchangeRate getExchangeRate(String token, String network, String quoteCurrency);
    boolean isHealthy();
}
```

### ChannelRouter（路由器端口）

```java
public interface ChannelRouter {
    List<ChannelAdapter> route(RouteRequest request);
}
```

---

## 四、两层支付模型关系

| | 执行层 (CryptoPayment) | 编排层 (PaymentOrder) |
|------|----------------------|------------------------|
| 定位 | 链上交易执行 | 商户订单编排 |
| 通道 | BlockchainAdapter(ETH/TRON/BTC) | ChannelAdapter(BitMart/Binance/...) |
| 对手方 | 区块链节点 | 收单通道(交易所/支付商) |
| 状态 | CREATED→PENDING→DETECTED→CONFIRMING→CONFIRMED | WAITING_PAYMENT→CONFIRMED→REFUND_PROCESSING→REFUNDED |
| 自建节点模式 | 直接对接 | PaymentOrder 委托给 CryptoPayment |

---

## 五、Roadmap

### ═══════════════════════════════════════════
### 🔴 P0 — 编排引擎核心 ✅ 已完成 (10/10)
### ═══════════════════════════════════════════

| # | 任务 | 状态 |
|---|------|------|
| P0-1 | flow-domain 新增 order/refund/channel 包（状态机+聚合根+ChannelAdapter端口+事件+仓储端口） | ✅ |
| P0-2 | flow-application 新增编排服务（PaymentOrchestrator+WebhookService+DTO） | ✅ |
| P0-3 | flow-infra BitMartAdapter（桩实现） | ✅ |
| P0-4 | flow-infra StubAdapter + DefaultChannelRouter | ✅ |
| P0-5 | flow-infra JPA 持久化（3组Entity+Repository） | ✅ |
| P0-6 | flow-api 商户 API（PayController, RefundController） | ✅ |
| P0-7 | flow-api 收银台 + 回调 API（CashierController, CallbackController） | ✅ |
| P0-8 | Flyway 迁移脚本（V2__orchestration_tables.sql） | ✅ |
| P0-9 | flow-cashier 收银台前端模块（checkout.html） | ✅ |
| P0-10 | 编译验证（mvn install -DskipTests） | ✅ |

### ═══════════════════════════════════════════
### 🟡 P1 — 多通道 + 生产加固 (0/6)
### ═══════════════════════════════════════════

| # | 任务 | 状态 |
|---|------|------|
| P1-1 | BinancePayAdapter | ⬜ |
| P1-2 | 动态路由策略（费率/汇率/权重） | ⬜ |
| P1-3 | Redis 币种+汇率缓存 | ⬜ |
| P1-4 | 过期调度 + 对账 Job | 🟡 执行层已完成（`PaymentReconciliationJob`：确认对账 + 过期）；编排层 `PaymentOrder` 过期/对账待做 |
| P1-5 | 安全加固（限频/UUID/HTTPS） | ⬜ |
| P1-6 | 单元测试 + 集成测试 | 🟡 单元测试 48 个全绿（含 KeyGenerator/Base58/TronAdapter 解析/Redis 幂等）；集成测试（Testcontainers/Docker）待做 |

### ═══════════════════════════════════════════
### 🟢 P2 — 扩展 (0/4)
### ═══════════════════════════════════════════

| # | 任务 | 状态 |
|---|------|------|
| P2-1 | 数币版（商户用加密货币创建订单） | ⬜ |
| P2-2 | 自建节点通道（PaymentOrder → CryptoPayment） | ⬜ |
| P2-3 | CoinbaseCommerceAdapter | ⬜ |
| P2-4 | Kafka 事件总线 | ⬜ |

### 📊 汇总

| 阶段 | 任务数 | 完成 | 进度 |
|------|--------|------|------|
| P0 | 10 | 10 | 100% |
| P1 | 6 | 0（2 项部分完成） | ~15% |
| P2 | 4 | 0 | 0% |
| **合计** | **20** | **10** | **~50%** |

---

## 六、修复 / 进度记录（2026-06-07）

P0 编排骨架完成后，对照实际代码做了缺陷修复、生命周期补全与测试：

- **Bug 修复**
  - `PaymentOrchestrator.submitPayment`：原返回硬编码充值地址，改为按 `channelId` 解析适配器并真正调用 `ChannelAdapter.createDepositAddress`。
  - `PaymentOrchestrator.handlePaymentCallback`：新增 `eventId` 去重（`ProcessedEventStore` 端口 + 内存实现），且置于订单查询之后，避免「订单暂不存在」的重试被误吞。
  - `PaymentApplicationService.onPaymentDetected`（执行层）：原为死代码，改为真正按收款地址匹配 PENDING 支付并转 DETECTED。
- **生命周期补全**：`PaymentReconciliationJob`（确认对账 + 过期调度），对应 P1-4 的执行层部分。
- **链上能力（Tier 2）**：
  - `KeyGenerator` 真实地址派生（ETH = keccak→EIP-55；TRON = 0x41‖keccak→Base58Check；自带 `Base58` 工具），用私钥=1 测试向量验证。BTC/SOLANA 暂抛异常。
  - `TronAdapter` 真实 `getCurrentBlockHeight`/`getConfirmations`/`isHealthy`（经 `TronGridClient`，解析有单测）；`scanNewBlocks` 仍为带说明的显式 stub（TronGrid TRC20 接口与按块扫描抽象不匹配）。
  - Redis 幂等存储 `RedisProcessedEventStore`（`SET NX EX`，多实例安全），`nexusflow.idempotency.store=memory|redis` 切换，默认内存。
- **工程化（Tier 3）**：GitHub Actions CI（`mvn verify`）；单元测试增至 48 个全绿；父 POM 固定 `maven-surefire-plugin` 3.2.5（否则 JUnit 5 静默不执行）；新增 `README.md`、`CLAUDE.md`、`.gitignore`。

> 仍未做：编排层 `PaymentOrder` 的过期/对账、商户 Webhook 实际投递、TronAdapter 真实扫块（`scanNewBlocks`）、`createPayment` 全量幂等、执行层支付/钱包的持久化、钱包播种、集成测试（需 Docker）。以上「真实链上/Redis」改动均未经真实环境端到端验证。