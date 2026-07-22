# NexusFlow 结算体系设计

## 1. 目标

在 NexusFlow 内部实现完整的记账结算能力，消除对外部系统（原 NexusPay-Core）的依赖：

- **复式记账**：每笔余额变动产生借贷两条 `ledger_entries`，共享 `journal_id`
- **商户余额**：按 (merchant_id, token, network) 粒度管理可用/冻结余额
- **手续费**：支付确认时自动扣收，费率可配置
- **提现结算**：商户发起提现 → 余额冻结 → 链上转账 → 余额扣减
- **事件驱动**：通过 `@TransactionalEventListener` 监听现有域事件，自动触发记账

---

## 2. 当前状态

| 能力 | 现状 |
|------|------|
| 支付确认事件 | ✅ `OrderEvent`（编排层）和 `PaymentStateChangedEvent`（执行层）已发布，无内部消费者 |
| 退款事件 | ✅ `OrderEvent`（REFUND_PROCESSING/REFUNDED/REFUND_FAILED）已发布 |
| Fiat Ramp 事件 | ⬜ `FiatRampApplicationService` 未发布任何事件，需补 `FiatRampStatusChangedEvent` |
| 事件发布机制 | ✅ Spring `ApplicationEventPublisher`（默认）和 Kafka（可选）均已实现 |
| 事件监听器 | ⬜ 代码中无任何 `@EventListener` 或 `@TransactionalEventListener` |
| 余额/记账/提现 | ⬜ 全部为空 |

---

## 3. 领域模型

```
flow-domain/src/main/java/com/nexusflow/domain/settlement/
├── MerchantBalance.java          # 聚合根
├── LedgerEntry.java              # 值对象（不可变）
├── LedgerEntryType.java          # 枚举
├── LedgerDirection.java          # 枚举：DEBIT / CREDIT
├── SettlementRequest.java        # 聚合根
├── SettlementStatus.java         # 枚举
├── FeeRule.java                  # 值对象
├── BalanceRepository.java        # 端口
├── LedgerRepository.java         # 端口
└── SettlementRepository.java     # 端口
```

### 3.1 MerchantBalance（聚合根）

```java
@Value @Builder
public class MerchantBalance {
    String balanceId;           // UUID
    String merchantId;
    String token;               // USDT, BTC, ETH
    String network;             // TRC20, ERC20, BTC
    BigDecimal available;       // 可用余额
    BigDecimal frozen;          // 冻结余额
    BigDecimal totalCredited;   // 累计入账
    BigDecimal totalDebited;    // 累计出账

    // 操作方法
    public MerchantBalance credit(BigDecimal amount, BigDecimal fee) { ... }
    public MerchantBalance freeze(BigDecimal amount) { ... }
    public MerchantBalance unfreeze(BigDecimal amount) { ... }
    public MerchantBalance debitFrozen(BigDecimal amount) { ... }
    public MerchantBalance unfreezeToAvailable(BigDecimal amount) { ... }
}
```

**业务约束**：
- `available` 不得为负
- `frozen` 不得为负
- 所有操作通过乐观锁（`version`）保护并发

### 3.2 LedgerEntry（值对象）

```java
@Value @Builder
public class LedgerEntry {
    String entryId;             // UUID
    String journalId;           // 同一笔交易的借贷两条共享
    String merchantId;
    String token;
    String network;
    LedgerEntryType entryType;  // PAYMENT_IN / REFUND_FREEZE / REFUND_RELEASE / FEE / SETTLEMENT_FREEZE / SETTLEMENT_DEBIT / ADJUSTMENT
    LedgerDirection direction;  // DEBIT / CREDIT
    BigDecimal amount;
    BigDecimal balanceAfter;    // 该笔变动后的余额快照
    String refType;             // PAYMENT_ORDER / CRYPTO_PAYMENT / REFUND_ORDER / SETTLEMENT / FIAT_RAMP
    String refId;
    String description;
    Instant createdAt;
}
```

### 3.3 LedgerEntryType（枚举）

| 值 | 场景 |
|----|------|
| `PAYMENT_IN` | 支付确认入账 |
| `REFUND_FREEZE` | 退款发起冻结 |
| `REFUND_RELEASE` | 退款完成释放 |
| `REFUND_ROLLBACK` | 退款失败回滚 |
| `FEE` | 手续费扣收 |
| `SETTLEMENT_FREEZE` | 提现请求冻结 |
| `SETTLEMENT_DEBIT` | 提现完成扣减 |
| `SETTLEMENT_ROLLBACK` | 提现失败回滚 |
| `SETTLEMENT_CANCEL` | 提现取消回滚 |
| `FIAT_RAMP_IN` | Fiat Ramp ON_RAMP 入账 |
| `FIAT_RAMP_OUT` | Fiat Ramp OFF_RAMP 出账 |
| `ADJUSTMENT` | 运营手动调账 |

### 3.4 SettlementRequest（聚合根）

```java
@Value @Builder
public class SettlementRequest {
    String settlementId;        // UUID
    String merchantId;
    String token;
    String network;
    BigDecimal amount;
    BigDecimal feeAmount;
    String toAddress;
    SettlementStatus status;
    String txHash;
    String failureReason;
    String requestedBy;

    public SettlementRequest startProcessing() { ... }
    public SettlementRequest complete(String txHash) { ... }
    public SettlementRequest fail(String reason) { ... }
    public SettlementRequest cancel() { ... }
}
```

### 3.5 SettlementStatus 状态机

```
PENDING → PROCESSING → COMPLETED
                     → FAILED
        → CANCELLED
```

| 转换 | 条件 |
|------|------|
| PENDING → PROCESSING | 开始链上转账 |
| PENDING → CANCELLED | 商户取消（仅 PENDING 可取消） |
| PROCESSING → COMPLETED | 链上转账成功 |
| PROCESSING → FAILED | 链上转账失败 |

终态：`COMPLETED`、`FAILED`、`CANCELLED`

### 3.6 FeeRule（值对象）

```java
@Value @Builder
public class FeeRule {
    BigDecimal feeRate;         // 0.003 = 0.3%
    BigDecimal minFee;          // 最低手续费
    String feeToken;            // 手续费币种（默认同支付币种）

    public BigDecimal calculateFee(BigDecimal amount) {
        BigDecimal fee = amount.multiply(feeRate);
        return fee.max(minFee);
    }
}
```

---

## 4. 数据模型

### 4.1 merchant_balances

```sql
CREATE TABLE merchant_balances (
    balance_id VARCHAR(36) PRIMARY KEY,
    merchant_id VARCHAR(36) NOT NULL,
    token VARCHAR(32) NOT NULL,
    network VARCHAR(32) NOT NULL,
    available DECIMAL(20,8) NOT NULL DEFAULT 0,
    frozen DECIMAL(20,8) NOT NULL DEFAULT 0,
    total_credited DECIMAL(20,8) NOT NULL DEFAULT 0,
    total_debited DECIMAL(20,8) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (merchant_id, token, network)
);
```

### 4.2 ledger_entries

```sql
CREATE TABLE ledger_entries (
    entry_id VARCHAR(36) PRIMARY KEY,
    journal_id VARCHAR(36) NOT NULL,
    merchant_id VARCHAR(36) NOT NULL,
    token VARCHAR(32) NOT NULL,
    network VARCHAR(32) NOT NULL,
    entry_type VARCHAR(32) NOT NULL,
    direction VARCHAR(8) NOT NULL,
    amount DECIMAL(20,8) NOT NULL,
    balance_after DECIMAL(20,8) NOT NULL,
    ref_type VARCHAR(32),
    ref_id VARCHAR(64),
    description VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_ledger_journal ON ledger_entries(journal_id);
CREATE INDEX idx_ledger_merchant ON ledger_entries(merchant_id, token, network, created_at);
```

### 4.3 settlement_requests

```sql
CREATE TABLE settlement_requests (
    settlement_id VARCHAR(36) PRIMARY KEY,
    merchant_id VARCHAR(36) NOT NULL,
    token VARCHAR(32) NOT NULL,
    network VARCHAR(32) NOT NULL,
    amount DECIMAL(20,8) NOT NULL,
    fee_amount DECIMAL(20,8),
    to_address VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    tx_hash VARCHAR(128),
    failure_reason VARCHAR(512),
    requested_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_settlement_merchant ON settlement_requests(merchant_id, status, created_at);
```

### 4.4 Flyway 迁移

`V13__add_settlement_tables.sql`：创建上述 3 张表 + 索引。

---

## 5. 复式记账规则

每笔交易产生一个 `journal_id`（UUID），关联两条 `ledger_entries`：

| 场景 | DEBIT（借方） | CREDIT（贷方） | 余额变动 |
|------|-------------|---------------|----------|
| **支付确认** | platform_revenue | merchant_balance | `available += amount − fee` |
| **退款发起** | merchant_balance (available) | refund_reserve (frozen) | `frozen += amount` |
| **退款完成** | refund_reserve (frozen) | — | `frozen -= amount` |
| **退款失败** | refund_reserve (frozen) | merchant_balance (available) | `frozen -= amount, available += amount` |
| **提现请求** | merchant_balance (available) | settlement_pending (frozen) | `available -= amount, frozen += amount` |
| **提现完成** | settlement_pending (frozen) | — | `frozen -= amount` |
| **提现失败** | settlement_pending (frozen) | merchant_balance (available) | `frozen -= amount, available += amount` |
| **提现取消** | settlement_pending (frozen) | merchant_balance (available) | `frozen -= amount, available += amount` |

> 简化实现：不维护独立的 platform 账户表，DEBIT 侧的 `platform_revenue` 等仅作为 `ledger_entries` 中的语义标记，实际操作集中在 `merchant_balances` 的 available/frozen 字段。

---

## 6. 事件驱动接入

### 6.1 监听器

```java
@Component
public class SettlementEventListener {

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onOrderEvent(OrderEvent event) {
        switch (event.getNewStatus()) {
            case CONFIRMED:
                settlementService.creditOnPaymentConfirmed(event);
                break;
            case REFUND_PROCESSING:
                settlementService.freezeOnRefund(event);
                break;
            case REFUNDED:
                settlementService.releaseOnRefundCompleted(event);
                break;
            case REFUND_FAILED:
                settlementService.rollbackOnRefundFailed(event);
                break;
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onPaymentStateChanged(PaymentStateChangedEvent event) {
        if (event.getNewStatus() == PaymentStatus.CONFIRMED) {
            settlementService.creditOnCryptoPaymentConfirmed(event);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onFiatRampStatusChanged(FiatRampStatusChangedEvent event) {
        if (event.getNewStatus() == FiatRampStatus.COMPLETED) {
            settlementService.handleFiatRampCompleted(event);
        }
    }
}
```

### 6.2 接入点汇总

| 监听事件 | 条件 | 动作 |
|----------|------|------|
| `OrderEvent` | `newStatus == CONFIRMED` | 入账：`available += paidAmount − fee`，写 PAYMENT_IN + FEE ledger |
| `PaymentStateChangedEvent` | `newStatus == CONFIRMED` | 入账（自建节点路径） |
| `OrderEvent` | `newStatus == REFUND_PROCESSING` | 冻结：`available → frozen` |
| `OrderEvent` | `newStatus == REFUNDED` | 释放：`frozen -= amount` |
| `OrderEvent` | `newStatus == REFUND_FAILED` | 回滚：`frozen → available` |
| `FiatRampStatusChangedEvent`（新增） | `newStatus == COMPLETED` | ON_RAMP 入账 / OFF_RAMP 出账 |

### 6.3 FiatRamp 事件补全

当前 `FiatRampApplicationService` 不发布任何事件。需要：

1. 在 `FiatRampOrder` 聚合根中添加 `domainEvents` 列表和 `collectEvents()` 方法
2. 新增 `FiatRampStatusChangedEvent extends DomainEvent`
3. 在 `handleProviderCallback()` 中 `save` 后发布事件

---

## 7. SettlementService 核心逻辑

```java
@Service
public class SettlementService {

    /**
     * 支付确认入账
     * 1. 计算手续费
     * 2. 更新余额：available += amount - fee
     * 3. 写两条 ledger_entries：PAYMENT_IN (CREDIT) + FEE (DEBIT)
     */
    public void creditOnPaymentConfirmed(String merchantId, String token, String network,
                                          BigDecimal paidAmount, String refType, String refId) { ... }

    /**
     * 退款冻结
     * available -= refundAmount, frozen += refundAmount
     */
    public void freezeOnRefund(String merchantId, String token, String network,
                                BigDecimal refundAmount, String refId) { ... }

    /**
     * 退款完成释放
     * frozen -= refundAmount
     */
    public void releaseOnRefundCompleted(String merchantId, String token, String network,
                                          BigDecimal refundAmount, String refId) { ... }

    /**
     * 退款失败回滚
     * frozen -= refundAmount, available += refundAmount
     */
    public void rollbackOnRefundFailed(String merchantId, String token, String network,
                                        BigDecimal refundAmount, String refId) { ... }

    /**
     * 创建提现请求
     * 1. 校验 available >= amount
     * 2. 冻结：available -= amount, frozen += amount
     * 3. 创建 SettlementRequest (PENDING)
     */
    public SettlementRequest createSettlement(String merchantId, String token, String network,
                                               BigDecimal amount, String toAddress, String requestedBy) { ... }

    /**
     * 提现完成
     * frozen -= amount, totalDebited += amount
     */
    public void completeSettlement(String settlementId, String txHash) { ... }

    /**
     * 提现失败
     * frozen -= amount, available += amount
     */
    public void failSettlement(String settlementId, String reason) { ... }

    /**
     * 提现取消
     * frozen -= amount, available += amount
     */
    public void cancelSettlement(String settlementId) { ... }
}
```

---

## 8. 手续费引擎

### 8.1 配置来源

优先级（从高到低）：
1. 商户级费率（`merchant_profiles.fee_rate`，待加字段）
2. 全局配置（`nexusflow.settlement.fee-rate`，默认 0.003）

### 8.2 计算逻辑

```java
BigDecimal fee = paidAmount.multiply(feeRule.getFeeRate());
if (fee.compareTo(feeRule.getMinFee()) < 0) {
    fee = feeRule.getMinFee();
}
BigDecimal netAmount = paidAmount.subtract(fee);
```

### 8.3 记账

入账时生成 3 条 ledger_entries（同一 journal_id）：

| direction | entry_type | amount | 说明 |
|-----------|-----------|--------|------|
| CREDIT | PAYMENT_IN | netAmount | 商户实际到账 |
| CREDIT | FEE | fee | 手续费（语义上 DEBIT 侧为 platform） |
| — | — | — | balance_after 记录的是 available 总额 |

> 简化为 2 条：PAYMENT_IN (CREDIT, amount=paidAmount) + FEE (CREDIT, amount=fee, balance_after 反映扣费后净值)。具体实现可调整。

---

## 9. REST API

### 9.1 结算 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/merchant/balances` | 查询商户全部币种余额 | 会话 / API Key |
| GET | `/merchant/balances/{token}/{network}` | 查询指定币种余额 | 会话 / API Key |
| GET | `/merchant/ledger-entries` | 记账流水查询（分页、按时间/类型/币种筛选） | 会话 / API Key |
| POST | `/merchant/settlements` | 创建提现请求 | 会话 |
| GET | `/merchant/settlements` | 提现记录列表 | 会话 / API Key |
| GET | `/merchant/settlements/{settlementId}` | 提现详情 | 会话 / API Key |
| POST | `/merchant/settlements/{id}/cancel` | 取消提现（仅 PENDING） | 会话 |

### 9.2 请求/响应示例

**创建提现请求：**

```http
POST /merchant/settlements
Authorization: Bearer <session-token>

{
  "token": "USDT",
  "network": "TRC20",
  "amount": "1000.00",
  "toAddress": "TR..."
}
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "settlementId": "uuid",
    "merchantId": "uuid",
    "token": "USDT",
    "network": "TRC20",
    "amount": "1000.00",
    "feeAmount": "0.00",
    "toAddress": "TR...",
    "status": "PENDING",
    "createdAt": "2026-07-03T10:00:00Z"
  }
}
```

**查询余额：**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "token": "USDT",
      "network": "TRC20",
      "available": "5000.00",
      "frozen": "1000.00",
      "totalCredited": "10000.00",
      "totalDebited": "4000.00"
    },
    {
      "token": "BTC",
      "network": "BTC",
      "available": "0.5",
      "frozen": "0.00",
      "totalCredited": "1.0",
      "totalDebited": "0.5"
    }
  ]
}
```

---

## 10. 配置参数

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `nexusflow.settlement.fee-rate` | `0.003` | 默认手续费率（0.3%） |
| `nexusflow.settlement.min-fee` | `0` | 最低手续费 |
| `nexusflow.settlement.withdrawal-fee-rate` | `0` | 提现手续费率 |
| `nexusflow.settlement.auto-settle-enabled` | `false` | 是否启用自动结算 |

---

## 11. 实施任务（P4）

| # | 任务 | 依赖 |
|---|------|------|
| P4-1 | Flyway V13 迁移：3 张结算表 | 无 |
| P4-2 | 领域模型：MerchantBalance + LedgerEntry | P4-1 |
| P4-3 | 领域模型：SettlementRequest + 状态机 | P4-1 |
| P4-4 | JPA 持久化：Entity + Repository | P4-2, P4-3 |
| P4-5 | SettlementService 核心逻辑 | P4-4 |
| P4-6 | 事件监听器：@TransactionalEventListener | P4-5 |
| P4-7 | FiatRamp 事件补全 | P4-5 |
| P4-8 | 手续费引擎 | P4-5 |
| P4-9 | 提现 REST API | P4-5 |
| P4-10 | 余额/流水查询 API | P4-5 |
| P4-11 | 单元测试 | P4-5 |
| P4-12 | 集成测试 | P4-9, P4-10 |

### 验证方式

```bash
# 单元测试
mvn test

# 验证事件监听器
mvn -pl flow-api,flow-application -am test

# 编译
mvn -pl flow-api -am package

# 手动验证
# 1. 创建支付订单 → 模拟确认 → 检查 merchant_balances 入账 + ledger_entries 两条记录
# 2. 创建提现 → 检查余额冻结 → 模拟完成 → 检查余额扣减
```

---

## 12. 业务场景走查

以下场景展示完整的业务链路，包括每一步的 API 调用、状态变更、余额变动和记账流水。

### 场景 1：正常支付 → 入账 → 查余额

**背景**：商户 A 创建了一笔 100 USDT (TRC20) 的支付订单，买家完成支付。

```
步骤 1：商户创建订单
────────────────────
API:    POST /pay/order
请求:   { merchantId: "A", merchantOrderNo: "ORD-001", amountCrypto: "100", currencyCrypto: "USDT", network: "TRC20" }
响应:   { paymentId: "pay-001", payUrl: "/checkout.html?payment_id=pay-001" }

订单状态: WAITING_PAYMENT
余额:     (无变化)
记账:     (无)
```

```
步骤 2：买家在收银台提交支付，获取充值地址
──────────────────────────────────────────
API:    POST /cashier/pay/submit
请求:   { paymentId: "pay-001", token: "USDT", network: "TRC20" }
响应:   { payAddress: "TR...", memo: null, expireTime: "..." }

订单状态: WAITING_PAYMENT（不变）
PaymentFlow 状态: WAITING
余额:     (无变化)
```

```
步骤 3：通道回调确认支付（全额）
──────────────────────────────────
触发:   POST /callback/COINBASE_COMMERCE/payment
        { reference_order_no: "CB-xxx", tx_hash: "0xabc...", cumulative_amount: "100", event_id: "evt-001" }

业务层处理（PaymentOrchestrator.handlePaymentCallback）:
  1. eventId 去重 → 通过
  2. order.markConfirmed("0xabc...", 100 USDT, 100 USD)
  3. orderRepository.save(order)
  4. eventPublisher.publish(OrderEvent(CONFIRMED))     ← 事件发布

结算层自动触发（SettlementEventListener）:
  5. 计算手续费: fee = 100 × 0.3% = 0.3 USDT
  6. 净入账: netAmount = 100 - 0.3 = 99.7 USDT
  7. 生成 journal_id = "jnl-001"
  8. 写 ledger_entries:
     ┌─────────────┬───────────┬──────────┬────────┬────────────────────┐
     │ direction   │ entry_type│ amount   │ ref_id │ description        │
     ├─────────────┼───────────┼──────────┼────────┼────────────────────┤
     │ CREDIT      │ PAYMENT_IN│ 99.7     │ pay-001│ 支付确认入账       │
     │ CREDIT      │ FEE       │ 0.3      │ pay-001│ 手续费扣收         │
     └─────────────┴───────────┴──────────┴────────┴────────────────────┘
  9. 更新 merchant_balances:
     available: 0 → 99.7
     totalCredited: 0 → 99.7

订单状态: CONFIRMED
余额:     available=99.7, frozen=0
```

```
步骤 4：商户查询余额
──────────────────────
API:    GET /merchant/balances
响应:
{
  "data": [{
    "token": "USDT",
    "network": "TRC20",
    "available": "99.70000000",
    "frozen": "0.00000000",
    "totalCredited": "99.70000000",
    "totalDebited": "0.00000000"
  }]
}
```

```
步骤 5：商户查看记账流水
──────────────────────────
API:    GET /merchant/ledger-entries?token=USDT&network=TRC20
响应:
{
  "data": [
    {
      "journalId": "jnl-001",
      "entryType": "PAYMENT_IN",
      "direction": "CREDIT",
      "amount": "99.70000000",
      "balanceAfter": "99.70000000",
      "refType": "PAYMENT_ORDER",
      "refId": "pay-001",
      "description": "支付确认入账 ORD-001"
    },
    {
      "journalId": "jnl-001",
      "entryType": "FEE",
      "direction": "CREDIT",
      "amount": "0.30000000",
      "balanceAfter": "99.70000000",
      "refType": "PAYMENT_ORDER",
      "refId": "pay-001",
      "description": "手续费 0.3%"
    }
  ]
}
```

---

### 场景 2：退款成功

**背景**：商户 A 对已确认的订单 ORD-001 发起 50 USDT 的部分退款。

```
步骤 1：商户发起退款
──────────────────────
API:    POST /refund/order
请求:   { merchantId: "A", merchantOrderNo: "ORD-001", refundOrderNo: "REF-001", refundAmountFiat: "50" }

业务层处理（PaymentOrchestrator.refund）:
  1. 校验订单状态 = CONFIRMED → 通过
  2. 计算 refundCrypto = 50 / exchangeRate = 50 USDT
  3. 调用 channel.refund(...) → 返回 channelRefundId
  4. order.markRefundProcessing()
  5. eventPublisher.publish(OrderEvent(REFUND_PROCESSING))  ← 事件发布

结算层自动触发:
  6. 生成 journal_id = "jnl-002"
  7. 写 ledger_entries:
     ┌─────────────┬───────────────┬──────────┬─────────┬──────────────────┐
     │ direction   │ entry_type    │ amount   │ ref_id  │ description      │
     ├─────────────┼───────────────┼──────────┼─────────┼──────────────────┤
     │ DEBIT       │ REFUND_FREEZE │ 50       │ REF-001 │ 退款冻结         │
     │ CREDIT      │ REFUND_FREEZE │ 50       │ REF-001 │ 退款冻结(frozen) │
     └─────────────┴───────────────┴──────────┴─────────┴──────────────────┘
  8. 更新 merchant_balances:
     available: 99.7 → 49.7
     frozen: 0 → 50

订单状态: REFUND_PROCESSING
余额:     available=49.7, frozen=50
```

```
步骤 2：通道回调退款成功
──────────────────────────
触发:   POST /callback/COINBASE_COMMERCE/refund
        { channel_refund_id: "CRF-xxx", status: "SUCCESS", tx_hash: "0xdef..." }

业务层处理:
  1. refund.markSuccess("0xdef...")
  2. order.markRefunded()
  3. eventPublisher.publish(OrderEvent(REFUNDED))  ← 事件发布

结算层自动触发:
  4. 生成 journal_id = "jnl-003"
  5. 写 ledger_entries:
     ┌─────────────┬────────────────┬──────────┬─────────┬──────────────────┐
     │ direction   │ entry_type     │ amount   │ ref_id  │ description      │
     ├─────────────┼────────────────┼──────────┼─────────┼──────────────────┤
     │ DEBIT       │ REFUND_RELEASE │ 50       │ REF-001 │ 退款完成释放冻结 │
     └─────────────┴────────────────┴──────────┴─────────┴──────────────────┘
  6. 更新 merchant_balances:
     frozen: 50 → 0
     totalDebited: 0 → 50

订单状态: REFUNDED
余额:     available=49.7, frozen=0, totalDebited=50
```

**最终状态**：商户入账 99.7，退款 50，净余额 49.7。

---

### 场景 3：退款失败 → 余额回滚

**背景**：商户 A 发起退款，但通道退款失败。

```
步骤 1：商户发起退款（同场景 2 步骤 1）
─────────────────────────────────────────
余额:     available=49.7, frozen=50
```

```
步骤 2：通道回调退款失败
──────────────────────────
触发:   POST /callback/COINBASE_COMMERCE/refund
        { channel_refund_id: "CRF-xxx", status: "FAILED" }

业务层处理:
  1. refund.markFailed()
  2. order.markRefundFailed()
  3. eventPublisher.publish(OrderEvent(REFUND_FAILED))  ← 事件发布

结算层自动触发:
  4. 生成 journal_id = "jnl-004"
  5. 写 ledger_entries:
     ┌─────────────┬─────────────────┬──────────┬─────────┬────────────────────┐
     │ direction   │ entry_type      │ amount   │ ref_id  │ description        │
     ├─────────────┼─────────────────┼──────────┼─────────┼────────────────────┤
     │ DEBIT       │ REFUND_ROLLBACK │ 50       │ REF-001 │ 退款失败释放冻结   │
     │ CREDIT      │ REFUND_ROLLBACK │ 50       │ REF-001 │ 退款失败回滚到可用 │
     └─────────────┴─────────────────┴──────────┴─────────┴────────────────────┘
  6. 更新 merchant_balances:
     frozen: 50 → 0
     available: 49.7 → 99.7

订单状态: REFUND_FAILED
余额:     available=99.7, frozen=0（完全恢复）
```

**关键点**：退款失败后余额完全恢复到退款前状态，商户可以重新发起退款。

---

### 场景 4：提现 → 链上转账成功

**背景**：商户 A 当前余额 99.7 USDT (TRC20)，想提现 80 USDT 到自己的钱包。

```
步骤 1：商户创建提现请求
──────────────────────────
API:    POST /merchant/settlements
请求:   { token: "USDT", network: "TRC20", amount: "80", toAddress: "TR..." }

业务层处理（SettlementService.createSettlement）:
  1. 查询 merchant_balances → available=99.7
  2. 校验 available >= 80 → 通过
  3. 冻结余额:
     available: 99.7 → 19.7
     frozen: 0 → 80
  4. 创建 SettlementRequest:
     settlement_id: "stl-001", status: PENDING
  5. 生成 journal_id = "jnl-005"
  6. 写 ledger_entries:
     ┌─────────────┬─────────────────────┬──────────┬─────────┬────────────────────┐
     │ direction   │ entry_type          │ amount   │ ref_id  │ description        │
     ├─────────────┼─────────────────────┼──────────┼─────────┼────────────────────┤
     │ DEBIT       │ SETTLEMENT_FREEZE   │ 80       │ stl-001 │ 提现冻结           │
     │ CREDIT      │ SETTLEMENT_FREEZE   │ 80       │ stl-001 │ 提现冻结(frozen)   │
     └─────────────┴─────────────────────┴──────────┴─────────┴────────────────────┘

余额:     available=19.7, frozen=80
提现状态: PENDING
```

```
步骤 2：外部 worker 执行链上转账并回调
────────────────────────────────────────
触发:   POST /internal/settlements/stl-001/complete
        { txHash: "0xghi..." }

业务层处理:
  1. settlement.startProcessing() → status: PROCESSING
  2. settlement.complete("0xghi...") → status: COMPLETED
  3. 生成 journal_id = "jnl-006"
  4. 写 ledger_entries:
     ┌─────────────┬──────────────────┬──────────┬─────────┬──────────────────┐
     │ direction   │ entry_type       │ amount   │ ref_id  │ description      │
     ├─────────────┼──────────────────┼──────────┼─────────┼──────────────────┤
     │ DEBIT       │ SETTLEMENT_DEBIT │ 80       │ stl-001 │ 提现完成扣减冻结 │
     └─────────────┴──────────────────┴──────────┴─────────┴──────────────────┘
  5. 更新 merchant_balances:
     frozen: 80 → 0
     totalDebited: 0 → 80

余额:     available=19.7, frozen=0, totalDebited=80
提现状态: COMPLETED
```

---

### 场景 5：提现失败 → 余额回滚

```
步骤 1：商户创建提现（同场景 4 步骤 1）
─────────────────────────────────────────
余额:     available=19.7, frozen=80
提现状态: PENDING
```

```
步骤 2：链上转账失败
──────────────────────
触发:   POST /internal/settlements/stl-001/fail
        { reason: "Insufficient gas" }

业务层处理:
  1. settlement.fail("Insufficient gas") → status: FAILED
  2. 生成 journal_id = "jnl-007"
  3. 写 ledger_entries:
     ┌─────────────┬─────────────────────┬──────────┬─────────┬────────────────────────┐
     │ direction   │ entry_type          │ amount   │ ref_id  │ description            │
     ├─────────────┼─────────────────────┼──────────┼─────────┼────────────────────────┤
     │ DEBIT       │ SETTLEMENT_ROLLBACK │ 80       │ stl-001 │ 提现失败释放冻结       │
     │ CREDIT      │ SETTLEMENT_ROLLBACK │ 80       │ stl-001 │ 提现失败回滚到可用余额 │
     └─────────────┴─────────────────────┴──────────┴─────────┴────────────────────────┘
  4. 更新 merchant_balances:
     frozen: 80 → 0
     available: 19.7 → 99.7

余额:     available=99.7, frozen=0（完全恢复）
提现状态: FAILED
```

---

### 场景 6：提现取消（PENDING 状态）

```
步骤 1：商户创建提现（同场景 4 步骤 1）
─────────────────────────────────────────
余额:     available=19.7, frozen=80
提现状态: PENDING
```

```
步骤 2：商户取消提现
──────────────────────
API:    POST /merchant/settlements/stl-001/cancel

业务层处理:
  1. 校验 status == PENDING → 通过
  2. settlement.cancel() → status: CANCELLED
  3. 生成 journal_id = "jnl-008"
  4. 写 ledger_entries:
     ┌─────────────┬────────────────────┬──────────┬─────────┬──────────────────┐
     │ direction   │ entry_type         │ amount   │ ref_id  │ description      │
     ├─────────────┼────────────────────┼──────────┼─────────┼──────────────────┤
     │ DEBIT       │ SETTLEMENT_CANCEL  │ 80       │ stl-001 │ 提现取消释放冻结 │
     │ CREDIT      │ SETTLEMENT_CANCEL  │ 80       │ stl-001 │ 提现取消回滚可用 │
     └─────────────┴────────────────────┴──────────┴─────────┴──────────────────┘
  5. 更新 merchant_balances:
     frozen: 80 → 0
     available: 19.7 → 99.7

余额:     available=99.7, frozen=0
提现状态: CANCELLED
```

---

### 场景 7：部分支付 → 全额确认后入账

**背景**：买家先付了 60 USDT，后又补了 40 USDT。

```
步骤 1：第一笔 60 USDT 到账
──────────────────────────────
触发:   POST /callback/.../payment  { cumulative_amount: "60" }

业务层: 60 < 100 → order.markPartiallyPaid(...)
        eventPublisher.publish(OrderEvent(PARTIALLY_PAID))

结算层: PARTIALLY_PAID 不在监听条件内 → 不记账

余额:     (无变化)
```

```
步骤 2：补付 40 USDT，累计 100 USDT
──────────────────────────────────────
触发:   POST /callback/.../payment  { cumulative_amount: "100" }

业务层: 100 >= 100 → order.markConfirmed(...)
        eventPublisher.publish(OrderEvent(CONFIRMED))

结算层: CONFIRMED → 入账
        按 cumulative_amount = 100 计算:
        fee = 100 × 0.3% = 0.3
        available += 99.7

余额:     available=99.7
```

**关键点**：只有 `CONFIRMED` 才触发入账，`PARTIALLY_PAID` 不记账。

---

### 场景 8：自建节点路径（直接链上支付）

**背景**：商户通过自建节点通道创建了一笔 BTC 支付，链上扫描器检测到交易并确认。

```
步骤 1：区块链扫描器检测到交易
──────────────────────────────
触发:   BlockchainScanner → PaymentApplicationService.onPaymentDetected(...)
        payment.markDetected(txHash, 0.005 BTC, blockNumber)
        eventPublisher.publish(PaymentStateChangedEvent(DETECTED))

结算层: DETECTED 不在监听条件内 → 不记账
```

```
步骤 2：确认数达到阈值
────────────────────────
触发:   BlockchainScanner → PaymentApplicationService.confirmPayment(paymentId, 12)
        payment.updateConfirmations(12) → CONFIRMED
        eventPublisher.publish(PaymentStateChangedEvent(CONFIRMED))

结算层:
  1. 通过 paymentId 查到关联的 PaymentOrder（如有），获取 merchantId
  2. 如无关联 PaymentOrder（纯执行层支付），使用回调 URL 关联的商户
  3. 入账: available += 0.005 BTC - fee
  4. 写 ledger_entries (PAYMENT_IN + FEE)

余额:     available += 0.005 BTC - fee (BTC 网络)
```

---

### 场景 9：Fiat Ramp ON_RAMP（法币买入加密货币）

**背景**：买家通过 MoonPay 用 100 USD 购买 USDT，Ramp 订单完成。

```
步骤 1：Ramp provider 回调完成
──────────────────────────────
触发:   POST /callback/MOONPAY/fiat-ramp  { status: "COMPLETED" }

业务层: FiatRampApplicationService.handleProviderCallback(...)
        rampOrder.markCompleted()
        eventPublisher.publish(FiatRampStatusChangedEvent(COMPLETED))  ← 需新增

结算层:
  direction = ON_RAMP → 入账
  available += cryptoAmount - fee (按 ramp 订单的 token/network)
  写 ledger_entries (FIAT_RAMP_IN)

余额:     available += cryptoAmount - fee
```

---

### 场景 10：余额不足提现

```
API:    POST /merchant/settlements
请求:   { token: "USDT", network: "TRC20", amount: "200", toAddress: "TR..." }

结算层:
  1. 查询 merchant_balances → available=99.7
  2. 校验 available >= 200 → 失败
  3. 抛出 InsufficientBalanceException

响应:   { code: "NF-0013", message: "Insufficient balance: available=99.7, requested=200" }
```

---

### 场景 11：并发操作保护

```
商户同时发起两笔提现（各 60 USDT，当前 available=99.7）:

请求 1: POST /merchant/settlements  { amount: "60" }
请求 2: POST /merchant/settlements  { amount: "60" }

处理:
  请求 1: available 99.7 >= 60 → 通过, available → 39.7, frozen → 60
  请求 2: available 39.7 < 60 → InsufficientBalanceException

保护机制:
  - 乐观锁 (@Version): 两个请求并发修改同一行时，一个成功一个抛 OptimisticLockException
  - 余额校验: available >= amount 在事务内检查
```

---

### 场景 12：对账 — 余额与链上真值校验

```
运营 API: GET /ops/settlement/reconciliation?merchantId=A&token=USDT&network=TRC20

返回:
{
  "ledgerBalance": "99.7",         // ledger_entries 累计净额
  "chainBalance": "99.7",          // 链上钱包地址余额
  "status": "MATCHED"              // MISMATCHED 时告警
}

对账逻辑:
  1. SUM(ledger_entries WHERE merchant_id=A AND token=USDT) → ledgerBalance
  2. 查询链上归集地址余额 → chainBalance
  3. 比较两者是否一致
```

---

## 13. 风险与决策点

| 风险 | 决策 |
|------|------|
| 并发余额操作 | 乐观锁 (`@Version`) + 业务层 `available >= 0` 校验 |
| Fiat Ramp 记账 | 需先补 `FiatRampStatusChangedEvent`，ON_RAMP 入账 / OFF_RAMP 出账 |
| 提现链上转账 | 本期只做余额操作 + 状态管理，链上签名/广播委托给外部 worker（与自建节点退款模式一致） |
| 平台收入账户 | 不建独立账户表，ledger_entries 中语义标记即可 |
| 手续费配置来源 | 一期用全局配置，后续可扩展商户级费率 |
| 事件监听器事务边界 | `BEFORE_COMMIT` 保证记账与状态变更同事务；如果记账失败，整个支付确认回滚 |
