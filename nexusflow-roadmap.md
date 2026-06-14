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
### 🔴 P0-S — 安全与正确性修复 ✅ 已完成 (6/6)
### ═══════════════════════════════════════════

代码审查发现的阻塞性缺陷，2026-06-08 修复：

| # | 任务 | 状态 |
|---|------|------|
| P0S-1 | JPA `toDomain()` 字段映射丢失 — 3 个 Repository 读取后 `status`/`txHash`/时间戳全为 null；添加 `reconstitute` 全字段 builder | ✅ |
| P0S-2 | `findByChannelRefundIdCustom` 方法名错误 — Spring Data 无法推导查询，退款查找运行时报错；改为 `@Query` JPQL | ✅ |
| P0S-3 | `updateConfirmations()` 双重状态转换 — 单次调用触发 DETECTED→CONFIRMING→CONFIRMED 两个事件；改为 `else if` | ✅ |
| P0S-4 | 回调 HMAC 签名验证 — `/callback/**` 端点无签名验证，攻击者可伪造支付回调；新增 `CallbackHmacFilter` + `HmacSignatureVerifier` | ✅ |
| P0S-5 | API Key 认证 — 所有商户 API 端点零认证；新增 `ApiKeyAuthFilter`（`X-API-Key` 头） | ✅ |
| P0S-6 | 加密密钥静默回退 — 未配置密钥时静默生成临时密钥，应用正常启动但数据不可恢复；改为生产环境必须配置，否则拒绝启动 | ✅ |

### ═══════════════════════════════════════════
### 🟡 P1 — 多通道 + 生产加固 ✅ 已完成 (10/10)
### ═══════════════════════════════════════════

2026-06-08 实施的生产加固：

| # | 任务 | 状态 |
|---|------|------|
| P1-1 | StubAdapter 加 `@Profile({"test","dev"})` — 防止生产环境创建假地址 | ✅ |
| P1-2 | JPA 实体加 `@Version` 乐观锁 — 防止并发回调数据丢失；新增 V3 Flyway 迁移 | ✅ |
| P1-3 | OrderStatus 修复 — `isTerminal()` 移除 `CONFIRMED`（有出边）；新增 `REFUND_FAILED→REFUND_PROCESSING` 退款重试 | ✅ |
| P1-4 | `flowNo` 碰撞修复 — `System.currentTimeMillis()` 改为 `UUID` | ✅ |
| P1-5 | HttpWebhookClient 改进 — 注入 RestTemplate（5s 连接/10s 读取超时）；重试间隔缩短为 {5,15,60,300}s；修复 InterruptedException；出站 Webhook HMAC 签名 | ✅ |
| P1-6 | WebhookService — JSON 构建改用 Jackson ObjectMapper；新增 SSRF 防护（拒绝非 HTTPS 和私有 IP） | ✅ |
| P1-7 | CallbackController — 日志降级为 DEBUG（只打印关键字段摘要）；BigDecimal 解析加 try-catch 校验 | ✅ |
| P1-8 | `decryptPrivateKey()` — 添加 WARN 级别审计日志 | ✅ |
| P1-9 | 单元测试 — 新增 OrderStatus(18)、FlowStatus(7)、RefundStatus(7)、AesGcmEncryption(9) 共 41 个测试 | ✅ |
| P1-10 | 总测试数从 48 增至 80，全绿 | ✅ |

### ═══════════════════════════════════════════
### 🟡 P1-F — 缺陷修复 + 测试补全 ✅ 已完成 (6/6)
### ═══════════════════════════════════════════

| # | 任务 | 状态 |
|---|------|------|
| P1F-1 | underpayment 防护 — 尘埃攻击（<10% 期望金额）自动拒绝 | ✅ |
| P1F-2 | `@Positive` 标注在 String 上无效 — 改为 `@Pattern` 正则校验 | ✅ |
| P1F-3 | `collectEvents().get(0)` 越界 — 构造函数不发事件，改为 `forEach` | ✅ |
| P1F-4 | `PaymentOrchestrator` 测试补全 — createOrder(3) + refund(3) + callback(1) 共 7 个新测试 | ✅ |
| P1F-5 | `PaymentApplicationService` 测试补全 — createPayment(3) + dust rejection(2) 共 5 个新测试 | ✅ |
| P1F-6 | 总测试数从 80 增至 92，全绿 | ✅ |

### ═══════════════════════════════════════════
### 🟡 P1-R — 剩余 P1 任务 ✅ 已完成 (6/6)
### ═══════════════════════════════════════════

| # | 任务 | 状态 |
|---|------|------|
| P1R-1 | BinancePayAdapter — stub 实现，非 `prod` profile 注册为 Bean | ✅ |
| P1R-2 | 动态路由策略 — 按汇率排序，最优汇率优先 | ✅ |
| P1R-3 | Redis 币种+汇率缓存 — `CurrencyRateCache` 端口 + `RedisCurrencyRateCache` / `NoOpCurrencyRateCache` 实现，手动 JSON 序列化避免耦合 Jackson 到 domain | ✅ |
| P1R-4 | 编排层 `PaymentOrder` 过期调度 — `OrderExpiryJob`，定时扫描 WAITING_PAYMENT/PARTIALLY_PAID 过期订单 | ✅ |
| P1R-5 | 限频 — `RateLimitFilter`，每 IP 每分钟 120 次（可配置），滑动窗口计数器 | ✅ |
| P1R-6 | 集成测试（Testcontainers/Docker）— `NexusFlowApplicationIT`（PostgreSQL 容器 + Spring Boot 上下文、JPA round trip、createPayment HTTP 幂等、地址池并发分配），无 Docker 时自动 skip；需在 Docker 环境实跑验证 | ✅/🟡 |

### ═══════════════════════════════════════════
### 🟢 P2 — 扩展 (4/4)
### ═══════════════════════════════════════════

| # | 任务 | 状态 |
|---|------|------|
| P2-1 | 数币版（商户用加密货币创建订单） | ✅ `/pay/order` 支持 `amountCrypto` + `currencyCrypto` + `network`，默认 USD 报价并补算 fiat 展示值 |
| P2-2 | 自建节点通道（PaymentOrder → CryptoPayment） | ✅ 充值委托链路完成；退款会生成 `SELF_HOSTED_NODE_REFUND_*` 任务并发布带静态 gas budget 的 `crypto.refund.requested`，链上签名/广播由外部 worker 或真实节点环境完成 |
| P2-3 | CoinbaseCommerceAdapter | ✅ 支持 Coinbase Commerce REST charge/rate 调用；无 API key 时保留非 prod 本地 stub fallback，prod profile 必须配置 API key 才注册 Bean |
| P2-4 | Kafka 事件总线 | ✅ `EVENT_PUBLISHER=kafka` 启用，按 `eventType()` 路由 topic，eventId 作为 key |

### ═══════════════════════════════════════════
### 🔵 P3 — 前端
### ═══════════════════════════════════════════

| # | 任务 | 状态 |
|---|------|------|
| P3-1 | 收银台（Cashier UI）— 买家支付页，二维码/地址展示，状态轮询 | ✅ `checkout.html` 接真实 `/cashier` API，支持 `payment_id` 加载、充值地址生成、Canvas 二维码、复制、倒计时和状态轮询 |
| P3-2 | 商户端（Merchant Portal）— 订单查询、退款申请、API Key / 回调配置 | ✅ `merchant.html` 静态控制台，支持本地 API key/回调配置、订单查询、创建订单和退款申请 |
| P3-3 | 运营端（Ops Dashboard）— 通道监控、订单看板、对账报表、风控告警 | ✅ `/ops/dashboard` 聚合 API + `ops.html` 静态运营台，覆盖通道健康、订单状态、对账 backlog、orphan 风险处理 |

### 📊 汇总

| 阶段 | 任务数 | 完成 | 进度 |
|------|--------|------|------|
| P0 | 10 | 10 | 100% |
| P0-S | 6 | 6 | 100% |
| P1 | 10 | 10 | 100% |
| P1-F | 6 | 6 | 100% |
| P1-R | 6 | 6 | 100% |
| P2 | 4 | 4 | 100% |
| P3 | 3 | 3 | 100% |
| **合计** | **45** | **45** | **100%** |

---

## 六、修复 / 进度记录（2026-06-07）

P0 编排骨架完成后，对照实际代码做了缺陷修复、生命周期补全与测试：

- **Bug 修复**
  - `PaymentOrchestrator.submitPayment`：原返回硬编码充值地址，改为按 `channelId` 解析适配器并真正调用 `ChannelAdapter.createDepositAddress`。
  - `PaymentOrchestrator.handlePaymentCallback`：新增 `eventId` 去重（`ProcessedEventStore` 端口 + 内存实现），且置于订单查询之后，避免「订单暂不存在」的重试被误吞。
  - `PaymentApplicationService.onPaymentDetected`（执行层）：原为死代码，改为真正按收款地址匹配 PENDING 支付并转 DETECTED。
- **生命周期补全**：`PaymentReconciliationJob`（确认对账 + 过期调度），对应 P1-4 的执行层部分。
- **链上能力（Tier 2）**：
  - `KeyGenerator` 真实地址派生（ETH = keccak→EIP-55；TRON = 0x41‖keccak→Base58Check；自带 `Base58` 工具），用私钥=1 测试向量验证。BTC 后续已在 P1 实施路线图中补齐为 BIP44 P2PKH；SOLANA 仍显式不支持。
  - `TronAdapter` 真实 `getCurrentBlockHeight`/`getConfirmations`/`isHealthy`（经 `TronGridClient`，解析有单测）；`scanNewBlocks` 通过 TronGrid contract event endpoint 按块查询 TRC20 `Transfer` 事件并转换为 `ScannedTransaction`（HTTP transport stubbed 单测，尚未 live-verified）。
  - Redis 幂等存储 `RedisProcessedEventStore`（`SET NX EX`，多实例安全），`nexusflow.idempotency.store=memory|redis` 切换，默认内存。
- **工程化（Tier 3）**：GitHub Actions CI（`mvn verify`）；单元测试增至 48 个全绿；父 POM 固定 `maven-surefire-plugin` 3.2.5（否则 JUnit 5 静默不执行）；新增 `README.md`、`CLAUDE.md`、`.gitignore`。

> 仍未做：真实 ETH/BTC/TRON 节点端到端验证。Testcontainers 集成测试已补充，但无 Docker 环境时会自动跳过。地址池补充已实现，但生产环境必须配置 `ADDRESS_POOL_SEED_MNEMONIC` 才会自动补池。Orphan transaction 告警与补偿建单已补齐，自动补偿需显式启用 `ORPHAN_AUTO_COMPENSATION_ENABLED=true`。

---

## 七、P0-S 安全与正确性修复记录（2026-06-08）

全面代码审查后修复的阻塞性缺陷：

- **JPA 持久化正确性**：`JpaOrderRepository`/`JpaFlowRepository`/`JpaRefundRepository` 的 `toDomain()` 方法使用 `@Builder` 构建域对象，但 `@Builder` 构造函数不包含 `status`、`createTime`、`txHash` 等字段。修复方案：在每个域类中添加 `@Builder(builderMethodName = "reconstitute")` 全字段私有构造函数，`toDomain()` 改用 `reconstitute()`。
- **Spring Data 方法名**：`JpaRefundRepository.findByChannelRefundIdCustom` 无法被 Spring Data 推导查询（实体字段为 `channelRefundId`，无 `channelRefundIdCustom`）。改为 `@Query` JPQL 查询。
- **状态机逻辑**：`CryptoPayment.updateConfirmations()` 用两个连续 `if` 而非 `else if`，当 DETECTED 状态收到足够确认数时会触发双重状态转换（DETECTED→CONFIRMING→CONFIRMED）并发出两个事件。改为 `else if`。
- **API 认证**：新增 `ApiKeyAuthFilter`（`X-API-Key` 头），保护 `/pay/**`、`/crypto/**`、`/refund/**`、`/ops/**` 端点。`/callback/**` 使用 HMAC、`/cashier/**` 面向终端用户、`/actuator/**` 面向运维，各自跳过。
- **回调签名验证**：新增 `CallbackHmacFilter` + `HmacSignatureVerifier`，验证 `/callback/**` 端点的 `X-Signature` 头（HMAC-SHA256），每个通道独立密钥。
- **加密密钥检查**：`AesGcmConfig` 在 `nexusflow.encryption.key` 未配置时默认拒绝启动，需显式设置 `nexusflow.encryption.allow-generated-key=true` 才允许生成临时密钥（仅限开发环境）。当前 `application.yml` 默认 `${ALLOW_GENERATED_KEY:false}`，生产仍需显式配置 `ENCRYPTION_KEY`。
- **ErrorCode 扩展**：新增 `UNAUTHORIZED`（NF-0004）和 `INVALID_SIGNATURE`（NF-0005）错误码。
- **配置清理**：移除 `application.yml` 中重复的 `spring.redis.*` 配置；新增 `nexusflow.api.key`、`nexusflow.callback.hmac-secret.*` 配置项。

---

## 八、P1 生产加固记录（2026-06-08）

- **StubAdapter 隔离**：添加 `@Profile({"test","dev"})`，防止在生产环境被 Spring 加载创建假地址。
- **JPA 乐观锁**：3 个实体（`PaymentOrderEntity`、`PaymentFlowEntity`、`RefundOrderEntity`）添加 `@Version private Long version;`，新增 `V3__add_version_columns.sql` 迁移。
- **OrderStatus 状态机修复**：(a) `isTerminal()` 移除 `CONFIRMED`（它有出边 `CONFIRMED→REFUND_PROCESSING`，语义矛盾）。(b) 新增 `REFUND_FAILED→REFUND_PROCESSING` 转换，支持退款重试。
- **flowNo 碰撞修复**：`"FLW" + System.currentTimeMillis()` 改为 `"FLW" + UUID.randomUUID()` 截取 16 字符。
- **HttpWebhookClient 重写**：(a) 注入 Spring 管理的 `RestTemplate`（5s 连接/10s 读取超时），替换自建实例。(b) 重试间隔从 {60,300,600,1800,3600,7200}s 缩短为 {5,15,60,300}s。(c) `InterruptedException` 恢复中断标志而非吞掉。(d) 出站 Webhook 添加 `X-Signature` HMAC-SHA256 签名头。
- **WebhookService 改进**：(a) JSON 构建从 `String.format` 改为 Jackson `ObjectMapper`，自动处理转义和 null 值。(b) 新增 SSRF 防护：拒绝非 HTTPS URL 和私有 IP（loopback、site-local、link-local、169.254.169.254）。
- **CallbackController 日志降级**：原始请求体从 INFO 降为 DEBUG，INFO 级别只打印 channel/channelOrderId/txHash 摘要。
- **BigDecimal 解析校验**：`handlePaymentCallback` 中 `new BigDecimal(paidCrypto/paidFiat)` 加 try-catch，非法输入抛 `INVALID_REQUEST` 异常而非未处理的 `NumberFormatException`。
- **私钥解密审计**：`WalletApplicationService.decryptPrivateKey()` 添加 `WARN` 级别审计日志。
- **测试补全**：新增 `OrderStatusTest`(18)、`FlowStatusTest`(7)、`RefundStatusTest`(7)、`AesGcmEncryptionTest`(9) 共 41 个测试。总测试数从 48 增至 80，全绿。
- **Jackson 依赖**：`flow-application` 模块新增 `jackson-databind` 依赖（`WebhookService` 需要）。
- **WebhookConfig**：新建配置类，注册 `RestTemplate` Bean（带超时）和 `HttpWebhookClient` Bean（带 HMAC 签名密钥）。

---

## 九、P1-F 缺陷修复 + 测试补全记录（2026-06-08）

- **underpayment 防护**：`PaymentApplicationService.onPaymentDetected()` 原先仅 log 警告但仍将尘埃交易标记为 DETECTED。修复：低于期望金额 10% 的支付自动拒绝，防止攻击者用 1 wei 占用支付地址。
- **@Positive 校验修复**：`CreateOrderRequest.amountFiat`、`CreatePaymentCommand.amount` 和 `RefundRequestDto.refundAmountFiat` 使用 `@Positive` 标注在 `String` 类型上无效。改为严格正数 `@Pattern(regexp = "^(?=.*[1-9])[0-9]+(\\.[0-9]+)?$")` 正则校验。
- **collectEvents 越界修复**：`PaymentOrchestrator.createOrder()` 中 `order.collectEvents().get(0)` 在构造函数不发事件时抛 `IndexOutOfBoundsException`。改为 `order.collectEvents().forEach(eventPublisher::publish)`。
- **测试补全**：
  - `PaymentOrchestratorTest`：新增 createOrder happy path / 重复拒绝 / 无通道(3)、refund happy path / 非确认拒绝 / 超额拒绝(3)、refundCallback unknown status(1) 共 7 个测试。
  - `PaymentApplicationServiceTest`：新增 createPayment happy path / 重复拒绝 / 无钱包(3)、dust rejection / underpayment accepted(2) 共 5 个测试。
  - 总测试数从 80 增至 92，全绿。

---

## 十、P1-R 剩余任务记录（2026-06-08 → 2026-06-09 全部完成）

- **BinancePayAdapter**：新增 `BinancePayAdapter` stub 实现（`flow-infra/.../adapter/binance/`），支持 USDT(TRC20/ERC20/BEP20) + BTC，非 `prod` profile 注册为 Spring Bean。与 `BitMartAdapter` 并列，由 `DefaultChannelRouter` 自动选择。
- **动态路由策略**：`DefaultChannelRouter` 增强为按汇率排序。从每个健康通道获取汇率，最优汇率（最高价格 = 买家获得更多加密货币）排在最前。汇率查询失败的通道降级到末尾。
- **Redis 币种+汇率缓存**：
  - 新增 `CurrencyRateCache` 端口（`flow-domain`），定义 `getExchangeRate` / `getSupportedCurrencies` 两个方法。
  - `NoOpCurrencyRateCache`（`flow-infra`）作为默认实现，直接透传适配器调用。
  - `RedisCurrencyRateCache`（`flow-infra`）为 Redis 实现：key 格式 `nexusflow:rate:{channel}:{token}:{network}:{quote}` / `nexusflow:currency:{channel}`；手动 `ObjectNode`/`ArrayNode` 序列化，避免在 domain `@Value` 类上耦合 Jackson 注解；Redis 不可用时自动降级到适配器。
  - `CurrencyRateCacheConfig` 条件装配：当 `nexusflow.cache.enabled=true` 时加载 Redis 实现，否则 fallback 到 NoOp。
  - `DefaultChannelRouter` 和 `PaymentOrchestrator.createOrder()` 均通过 `CurrencyRateCache` 获取汇率，避免重复上游调用。
  - `application.yml` 新增 `nexusflow.cache.enabled` / `rate-ttl-seconds` / `currency-ttl-seconds` 配置。
- **编排层过期调度**：新增 `OrderExpiryJob`（`flow-application`），`@Scheduled` 每 60s 扫描 `WAITING_PAYMENT`/`PARTIALLY_PAID` 状态的订单，超过 `expireTime` 的标记为 `EXPIRED`。`OrderRepository` 新增 `findByStatusIn` 方法，`JpaOrderRepository` 对应 `@Query` JPQL。`application.yml` 新增 `nexusflow.order.expiry.interval-ms` 配置。
- **限频**：新增 `RateLimitFilter`（`flow-api/.../security/`），基于 `ConcurrentHashMap` 的滑动窗口计数器，每 IP 每分钟最多 120 次请求（`nexusflow.api.rate-limit.per-minute` 可配置）。`/actuator` 和 `/cashier` 跳过。返回 429 状态码。`SecurityConfig` 注册为 order=0（最先执行）。
- **集成测试（Testcontainers）**：新增 `NexusFlowApplicationIT`（`flow-api`），`@SpringBootTest` + `@Testcontainers(disabledWithoutDocker=true)` + PostgreSQL 容器。验证 Spring 上下文加载和 `OrderRepository` JPA 读写；后续已扩展到执行层 JPA round trip、createPayment HTTP 幂等和地址池并发分配。无 Docker 环境时自动 skip。
- **前端页面**：从 `E:\gitee\channelpay-pages` 复制到 `frontend/` 目录和 `flow-cashier/src/main/resources/static/`，包含 7 个收银台页面和 1 个交互式演示；当前已补齐 `checkout.html` 真实收银台、`merchant.html` 商户控制台与 `ops.html` 运营台。

---

## 十一、P1-4 执行层持久化记录（2026-06-13）

- **执行层 JPA 仓储**：新增 `CryptoPaymentEntity` / `WalletEntity`，以及 `JpaPaymentRepository` / `JpaWalletRepository`，映射 `crypto_payments` 和 `wallets` 表。默认启用 `nexusflow.execution.persistence=jpa`。
- **内存仓储降级为显式选项**：`InMemoryPaymentRepository` / `InMemoryWalletRepository` 仅在 `nexusflow.execution.persistence=memory` 时装配，避免与 JPA 仓储产生多 Bean 冲突。
- **数据库迁移**：新增 `V4__add_execution_version_columns.sql`，为 `crypto_payments` / `wallets` 增加 `version` 乐观锁字段。
- **恢复构造器与乐观锁修复**：为 `PaymentOrder` / `PaymentFlow` / `RefundOrder` / `CryptoPayment` / `Wallet` 的 `reconstitute` builder 指定独立 `builderClassName`，避免与新建对象 builder 共用生成类导致持久化字段恢复丢失；所有 JPA 仓储保存前保留已有 `version`，避免更新被误判为新增。
- **测试**：新增 `JpaPaymentRepositoryTest`、`JpaWalletRepositoryTest`，并扩展 `NexusFlowApplicationIT` 覆盖执行层支付、钱包 JPA round trip、createPayment HTTP 幂等和地址池并发分配。`mvn test` 通过；本机无 Docker，当前 6 个 Testcontainers 用例自动跳过。

---

## 十二、P1 实施路线图完成记录（2026-06-13）

- **P1-1 EthereumAdapter**：实现 ERC20 `Transfer` 日志扫描、交易确认数查询和 block hash 查询；Spring 配置新增 `nexusflow.ethereum.rpc-url` / `usdt-contract`。
- **P1-2 BitcoinAdapter**：改为 Bitcoin Core JSON-RPC 实现，支持 `getblockcount`、`getblockhash`、`getblock`、`getrawtransaction`，可扫描 UTXO 输出并转换为 satoshi 金额；新增 `BitcoinAdapterTest`。
- **P1-3 HD Wallet**：`KeyGenerator` 改为 BIP39/BIP44 派生，支持 ETH/TRON/BTC 路径；新增 BTC P2PKH 地址派生；新增 `MnemonicStore` 端口和 `mnemonic_backups` JPA 表，`WalletService.createHotWallet()` 会保存加密助记词备份。
- **P1-5 地址池**：新增 `AddressPoolEntry` / `AddressPoolRepository` / `address_pool` 表；`PaymentApplicationService.createPayment()` 改为分配 `AVAILABLE` 地址并标记 `ASSIGNED`；`AddressPoolProvisioningService` 根据 `ADDRESS_POOL_SEED_MNEMONIC` 做低水位补充。
- **P1-6 Retry/Reorg**：新增 `ChainScanCursor` / `chain_scan_cursors` 表；`BlockchainScanner` 使用 block hash 检测 reorg 并回退扫描游标；`CryptoPayment` 新增 `detectedBlockNumber`、`retryCount`、`nextRetryAt`、`lastFailureReason`，支持 reorg 回滚到 `PENDING` 和失败退避；新增轻量 `BlockchainCircuitBreaker`。
- **数据库迁移**：新增 `V5__p1_wallet_pool_and_reorg.sql`，包含助记词备份、地址池、扫描游标和支付 retry/reorg 字段。
- **测试验证**：新增 `BlockchainCircuitBreakerTest`、`EthereumAdapterTest`、`BitcoinAdapterTest`、`BlockchainScannerTest`、`JpaAddressPoolRepositoryTest`、`JpaMnemonicStoreTest`；扩展 `KeyGeneratorTest` 和支付服务测试。2026-06-14 本机相关模块测试通过；当前 `NexusFlowApplicationIT` 共 6 个 Testcontainers 用例因本机 Docker 不可用自动跳过。

---

## 十三、生产前剩余风险 / 未验证项（2026-06-14）

P1 代码项已基本落地；下表记录生产前风险项的当前状态，未完成项仍不应按生产闭环处理：

| # | 项目 | 当前状态 | 后续动作 |
|---|------|----------|----------|
| R-1 | TRON 自动扫块 | ✅ `TronAdapter.scanNewBlocks()` 使用 TronGrid `/v1/contracts/{contract}/events` 按块查询 `Transfer` 事件，解析 txHash/from/to/value/block/timestamp/confirmations；离线单测覆盖响应解析；`LiveBlockchainAdapterTest.tronLiveNodeSmoke()` 可在设置 `LIVE_TRON_NODE_URL` 后执行真实节点 one-block smoke | 在真实 TronGrid 或全节点环境跑通 live smoke，并继续做长窗口扫描、限流与异常恢复验证 |
| R-2 | Docker 集成测试实跑 | `NexusFlowApplicationIT` 已扩展为 6 个用例，覆盖 PostgreSQL/Testcontainers/Flyway/Spring 上下文、JPA round trip、createPayment HTTP 幂等和地址池并发分配；本机无 Docker 时自动 skip | 在 CI 或本地 Docker 环境实际跑通这 6 个用例 |
| R-3 | 加密密钥生产默认值 | ✅ `application.yml` 默认 `${ALLOW_GENERATED_KEY:false}`，Testcontainers 测试显式注入测试 AES key | 生产强制配置 `ENCRYPTION_KEY` |
| R-4 | 地址池并发分配 | ✅ `findFirstAvailableByChain` 使用 PostgreSQL `FOR UPDATE SKIP LOCKED` 锁定可用地址；`NexusFlowApplicationIT.concurrentCreatePaymentRequestsAssignDistinctAddresses()` 已补 Docker-backed 并发 createPayment 验证 | 后续在 CI/真实 PostgreSQL 环境执行并扩展更高并发压力测试 |
| R-5 | 执行层回调投递 | ✅ `PaymentApplicationService` 状态变更后调用 `WebhookService.notifyCryptoPayment()`；`@EnableAsync` 启用异步投递；失败/SSRF 拦截会写入 `webhook_dead_letters`（V8/V9 Flyway + JPA store）；`/ops/webhook-dead-letters` 和 `ops.html` 支持查询、replay、ignore；新增 `HttpWebhookClientTest` 覆盖 HMAC/重试，新增 opt-in `LiveWebhookDeliveryTest` 可用 `LIVE_WEBHOOK_URL` 验证真实投递可达性 | 后续在生产环境执行 live webhook smoke，并验证商户 webhook 重放策略 |
| R-6 | createPayment 全量幂等 | ✅ `Idempotency-Key`/`X-Idempotency-Key` + 请求 hash + `idempotency_keys` 响应缓存/重放已实现；`PaymentControllerTest` 覆盖 HTTP 契约、幂等头透传、参数绑定和请求校验；`NexusFlowApplicationIT.createPaymentHttpPersistsAndReplaysIdempotentResponse()` 已补 PostgreSQL-backed HTTP 重放验证 | 后续在 Docker/CI 环境执行该 E2E，并按需补 Redis-backed HTTP E2E |
| R-7 | Missing-event catch-up | ✅ 扫链发现自有地址但无 PENDING payment 时会持久化 `orphan_transactions` 并发布 `crypto.orphan.detected`；`/crypto/orphan-transactions` 支持查询、resolve、ignore、compensate，`ORPHAN_AUTO_COMPENSATION_ENABLED=true` 可启用自动补偿建单 | 生产启用自动补偿前需按风控策略确认误打款处理规则 |
| R-8 | 真实节点端到端验证 | ETH/BTC/TRON 适配器均有离线解析单测；新增 opt-in `LiveBlockchainAdapterTest`，设置 `LIVE_ETH_RPC_URL` / `LIVE_BTC_RPC_URL` / `LIVE_TRON_NODE_URL` 后可验证高度、健康、块哈希（ETH/BTC）、one-block scan 和可选 tx confirmations | 在真实或本地节点环境执行 live smoke，并继续验证 reorg、异常恢复和长窗口扫描 |
| R-9 | Kafka broker 集成 | ✅ `KafkaDomainEventPublisherTest` 覆盖 envelope、eventId key 和 topic routing；新增 opt-in `LiveMessagingInfrastructureTest.kafkaLiveProducerSmoke()`，设置 `LIVE_KAFKA_BOOTSTRAP_SERVERS` 后可向 `LIVE_KAFKA_TOPIC` 写入 smoke event | 在真实 Kafka 环境执行 live smoke，并验证 topic ACL、auto-create 策略和消费者契约 |
| R-10 | Redis 集成 | ✅ Redis cache/idempotency 均有 mocked client 单测；新增 opt-in `LiveMessagingInfrastructureTest.redisLiveIdempotencySmoke()`，设置 `LIVE_REDIS_HOST` 后可验证真实 Redis `SET NX EX` 幂等语义 | 在真实 Redis/托管 Redis 环境执行 live smoke，并按生产拓扑验证认证、TLS/ACL、连接池和故障恢复 |
| R-11 | Coinbase Commerce live 验证 | ✅ `CoinbaseCommerceAdapter` 已支持带 `COINBASE_COMMERCE_API_KEY` 的 REST charge 创建和 exchange-rate 查询；无 key 时只在非 prod 保留 stub fallback，prod profile 不注册 no-key stub；`CoinbaseCommerceAdapterTest` 使用 mocked transport 覆盖请求头、响应解析、支持币种和 fallback；新增 opt-in `LiveCoinbaseCommerceAdapterTest`，可用 `LIVE_COINBASE_COMMERCE_API_KEY` 验证 live rate，且只有设置 `LIVE_COINBASE_COMMERCE_CREATE_CHARGE=true` 才创建真实 charge | 配置真实 Coinbase Commerce 凭证执行 live smoke，确认 webhook payload/签名语义，并明确退款由外部人工或运营流程处理 |
| R-12 | BitMart/Binance 真实适配器 | ✅ 当前 BitMart/Binance 仍是 stub，但 Spring Bean 已用 `@Profile("!prod")` 隔离，避免 `prod` profile 误路由到假地址 | 实现真实 BitMart/Binance REST 适配器、签名、回调语义和 live smoke 后再允许生产启用 |
| R-13 | Gas abstraction 生产化 | ✅ `GasEstimator` 端口、`StaticGasEstimator` 默认实现、自建节点退款 gas budget 事件字段、`GasBank` 端口、`GasBankPolicy` 阈值/低 gas 批处理建议，以及 `GasBankFundingService` 资金评估/触发应用服务已实现；`GasBankFundingConfig` 仅在真实 `GasBank` + `GasEstimator` bean 存在时注册；`StaticGasEstimatorTest` / `GasBankPolicyTest` / `GasBankFundingServiceTest` / `GasBankFundingConfigTest` 覆盖估算、top-up/deferral/batching 决策和条件装配 | 接入 live gas/fee oracle，实现真实 GasBank 资金适配器、告警和生产低 gas 批处理调度/live smoke |
| R-14 | MPC 钱包集成 | ✅ `MpcSigner` 端口、签名请求/结果 VO、`Wallet.mpcWalletId`、JPA 映射、V10 Flyway 迁移和 opt-in custom HTTP MPC signer adapter 已实现；钱包恢复/映射测试和 `HttpMpcSignerTest` 覆盖字段持久化与签名请求/响应 contract | 实现 Fireblocks/Copper-specific MPC 适配器，将外发交易签名流程切到 MPC，并执行 live signing smoke |
| R-15 | On/Off Ramp 集成 | ✅ `FiatGateway` 端口、quote/order request VO、`FiatRampOrder` 转换跟踪状态机、`FiatRampRepository` 端口、JPA 映射、V11 Flyway 迁移、`FiatRampApplicationService`、`/fiat/ramp` 商户 API、`/callback/{gatewayId}/fiat-ramp` HMAC 回调入口，以及 opt-in normalized HTTP fiat-ramp gateway 已实现；`HttpFiatRampGateway` + `FiatRampGatewayConfig` 由 `FIAT_RAMP_HTTP_BASE_URL` 启用，支持统一 JSON contract 的 quote/create/query、`X-API-Key` 请求头和响应映射；`FiatRampOrderTest` / `JpaFiatRampRepositoryTest` / `FiatRampApplicationServiceTest` / `FiatRampControllerTest` / `CallbackControllerTest` / `HttpFiatRampGatewayTest` / `FiatRampGatewayConfigTest` 覆盖 tracking、终态保护、持久化映射、API 编排、normalized callback 和 HTTP adapter contract | 实现 MoonPay/Ramp/Banxa 官方 provider-specific payload/signature/KYC/settlement mapping 和 live settlement smoke |
