# NexusFlow 详细需求文档

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档名称 | NexusFlow 多链数字资产支付引擎需求文档 |
| 版本 | v2.0 |
| 更新日期 | 2026-07-03 |
| 文档状态 | 详细需求分析 |

---

## 1. 项目概述

### 1.1 项目背景与定位

NexusFlow 是 Nexus 生态中的**多链数字资产支付引擎**，采用执行层 + 编排层一体化架构设计。项目从纯执行层演进为执行 + 嵌入式编排引擎，同时处理链上支付执行、钱包操作、区块链集成、商户支付订单、收银台页面、通道路由、退款以及部分法币通道集成。

### 1.2 核心价值主张

1. **多链支持**：支持 Ethereum (ETH/ERC20)、Tron (TRC20)、Bitcoin (UTXO) 等主流区块链
2. **执行与编排解耦**：链上执行逻辑与业务编排逻辑分离，便于独立演进
3. **通道路由**：智能路由到最优收单通道（交易所/支付商/自建节点）
4. **完整产品体验**：提供收银台、商户控制台、运营监控等完整产品体系
5. **事件驱动架构**：所有状态变更通过事件驱动，支持异步处理和最终一致性

### 1.3 系统边界

**NexusFlow 负责：**
- 钱包管理与地址生成
- 区块链交易扫描与确认跟踪
- 加密支付生命周期管理
- 嵌入式支付订单编排
- 收银台、商户门户、运营控制台静态页面
- 通道回调接收与 Webhook 投递记录
- 法币通道转换跟踪

**NexusFlow 不负责：**
- 完整的 NexusPay-Core 控制面板、商户生命周期、定价、风控、合规或结算运营
- 提供官方供应商特定实现（除非适配器已明确实现并经过验证）
- 法币通道提供商的 KYC/AML 决策
- 超出本仓库中支付/退款/通道跟踪记录范围的商户余额托管或记账
- 未经过实时环境验证的存根或可选集成的生产就绪保证

---

## 2. 功能需求详细分解

### 2.1 执行层功能需求

#### 2.1.1 加密支付执行

**FR-EXE-001：支付指令生成**
- **描述**：系统应能为商户生成唯一的支付指令，包括收款地址、memo（如适用）和期望金额
- **输入**：商户ID、订单号、期望金额、币种、网络
- **输出**：支付ID、收款地址、memo、过期时间、支付URL
- **业务规则**：
  - 每个支付指令必须有全局唯一的 paymentId
  - merchantOrderNo 在同一商户下必须唯一（幂等性）
  - 支付指令默认 30 分钟过期
  - 地址从地址池分配，标记为 ASSIGNED 状态

**FR-EXE-002：链上交易检测**
- **描述**：系统应能检测区块链上的交易并匹配到对应的支付指令
- **触发条件**：区块扫描器检测到转账事件
- **处理逻辑**：
  1. 解析交易的 from/to/amount/block/timestamp/confirmations
  2. 按收款地址查找 PENDING 状态的支付
  3. 验证币种匹配
  4. 检查金额是否满足最低要求（防止尘埃攻击，<10% 期望金额自动拒绝）
  5. 状态转换：PENDING → DETECTED

**FR-EXE-003：支付确认跟踪**
- **描述**：系统应能跟踪交易确认数并在达到阈值时确认支付
- **处理逻辑**：
  1. 定期轮询已检测交易的确认数
  2. 当确认数达到配置阈值时，状态转换：DETECTED → CONFIRMING → CONFIRMED
  3. 发布 `crypto.payment.confirmed` 事件
  4. 触发 Webhook 回调通知商户

**FR-EXE-004：支付过期处理**
- **描述**：系统应能自动过期未支付的支付指令
- **触发条件**：定时任务（每 60 秒）
- **处理逻辑**：
  1. 查询 PENDING 状态且超过过期时间的支付
  2. 状态转换：PENDING → EXPIRED
  3. 发布过期事件
  4. 释放地址池中的地址（如适用）

#### 2.1.2 钱包管理

**FR-WAL-001：HD 钱包地址派生**
- **描述**：系统应支持基于 BIP39/BIP44 标准的 HD 钱包地址派生
- **支持链**：
  - Ethereum: m/44'/60'/0'/0/i
  - Tron: m/44'/195'/0'/0/i
  - Bitcoin: m/44'/0'/0'/0/i
- **地址格式**：
  - ETH: keccak256(pubkey)[12:] → EIP-55 checksummed hex
  - TRON: 0x41 + keccak256(pubkey)[12:] → Base58Check
  - BTC: compressed secp256k1 pubkey → HASH160 → Base58Check P2PKH

**FR-WAL-002：私钥加密存储**
- **描述**：私钥必须使用 AES-256-GCM 加密后存储，禁止明文存储
- **密钥管理**：
  - 支持外部 KMS（AWS KMS / HashiCorp Vault）
  - 未配置加密密钥时生产环境拒绝启动
  - 私钥解密操作必须记录 WARN 级别审计日志

**FR-WAL-003：助记词备份**
- **描述**：系统应加密存储助记词备份用于钱包恢复
- **存储**：`mnemonic_backups` 表，AES-256-GCM 加密
- **用途**：地址池补充、钱包恢复

**FR-WAL-004：地址池管理**
- **描述**：系统应维护预生成的地址池以提高支付创建效率
- **池管理**：
  - 地址状态：AVAILABLE → ASSIGNED
  - 低水位自动补充（基于 `ADDRESS_POOL_SEED_MNEMONIC`）
  - 并发分配使用 PostgreSQL `FOR UPDATE SKIP LOCKED` 避免冲突
  - 支持按链类型（ETH/TRON/BTC）分别管理

#### 2.1.3 区块链集成

**FR-BLK-001：多链适配器**
- **描述**：系统应通过可插拔适配器支持多条区块链
- **适配器接口**：
  ```java
  interface BlockchainAdapter {
      List<ScannedTransaction> scanNewBlocks();
      long getCurrentBlockHeight();
      int getConfirmations(String txHash);
      String getBlockHash(long blockNumber);
      boolean isHealthy();
  }
  ```
- **支持链**：
  - **Ethereum**：ERC20 Transfer 日志扫描、确认数查询、block hash 查询
  - **Tron**：TRC20 Transfer 事件扫描（TronGrid API）、确认数查询
  - **Bitcoin**：UTXO 输出扫描（Bitcoin Core JSON-RPC）、确认数查询

**FR-BLK-002：区块扫描与游标管理**
- **描述**：系统应持续扫描新区块并跟踪扫描进度
- **游标管理**：
  - `chain_scan_cursors` 表记录每个链的扫描高度和 block hash
  - 支持 reorg 检测：当 block hash 不匹配时回退扫描
  - 回退距离可配置（`nexusflow.scanner.reorg-rewind-blocks`）

**FR-BLK-003：链重组处理**
- **描述**：系统应能检测并处理区块链重组
- **处理逻辑**：
  1. 检测到 block hash 不匹配时回退扫描游标
  2. 受影响的 DETECTED/CONFIRMING 支付回滚到 PENDING
  3. 记录 reorg 事件用于审计

**FR-BLK-004：孤儿交易处理**
- **描述**：系统应处理扫描到但无对应 PENDING 支付的交易
- **处理逻辑**：
  1. 交易持久化到 `orphan_transactions` 表
  2. 发布 `crypto.orphan.detected` 事件
  3. 支持运营操作：resolve（关联到支付）、ignore（忽略）、compensate（补偿建单）
  4. 可配置自动补偿（`ORPHAN_AUTO_COMPENSATION_ENABLED=true`）

#### 2.1.4 对账与可靠性

**FR-REL-001：确认对账**
- **描述**：系统应定期重新检查未确认交易的确认状态
- **触发条件**：定时任务（可配置间隔 `nexusflow.reconciliation.interval-ms`）
- **处理逻辑**：
  1. 查询 DETECTED/CONFIRMING 状态的支付
  2. 重新查询区块链获取最新确认数
  3. 更新支付状态
  4. 失败时使用指数退避重试

**FR-REL-002：断路器保护**
- **描述**：系统应对链 RPC 调用实施断路器保护
- **实现**：`BlockchainCircuitBreaker`
- **行为**：连续失败达到阈值时打开断路器，避免雪崩

**FR-REL-003：幂等性保证**
- **描述**：所有入站 API 必须支持幂等性
- **实现**：
  - `Idempotency-Key` / `X-Idempotency-Key` 请求头
  - 请求 hash 一致性校验
  - `idempotency_keys` 表存储响应缓存
  - 支持 Redis 或内存存储后端

---

### 2.2 编排层功能需求

#### 2.2.1 支付订单管理

**FR-ORD-001：订单创建**
- **描述**：系统应支持商户创建支付订单
- **API**：`POST /pay/order`
- **请求参数**：
  ```json
  {
    "merchantId": "商户ID（必填）",
    "merchantOrderNo": "商户订单号（必填）",
    "amountFiat": "法币金额（与 amountCrypto 二选一）",
    "currencyFiat": "法币币种（如 USD）",
    "amountCrypto": "加密货币金额（与 amountFiat 二选一）",
    "currencyCrypto": "加密货币币种（如 USDT）",
    "network": "网络（如 TRC20、ERC20）",
    "preferredChannel": "首选通道（可选）",
    "notifyUrl": "回调地址（可选）",
    "returnUrl": "返回地址（可选）",
    "extend": "扩展数据（可选）"
  }
  ```
- **业务规则**：
  - merchantOrderNo 在同一商户下必须唯一
  - 支持法币计价或加密货币计价两种模式
  - 自动路由到最优通道（按汇率排序）
  - 默认 30 分钟过期
  - 返回支付 URL 供买家支付

**FR-ORD-002：订单查询**
- **描述**：系统应支持商户查询订单详情
- **API**：`GET /pay/order/{paymentId}`
- **返回**：订单完整信息，包括状态、金额、支付地址、交易哈希等
- **权限**：只能查询本商户的订单

**FR-ORD-003：订单过期处理**
- **描述**：系统应自动过期超时订单
- **触发条件**：定时任务（每 60 秒，`nexusflow.order.expiry.interval-ms`）
- **处理逻辑**：
  1. 查询 WAITING_PAYMENT/PARTIALLY_PAID 状态且超过过期时间的订单
  2. 状态转换：→ EXPIRED
  3. 发布过期事件
  4. 触发 Webhook 回调

#### 2.2.2 通道路由

**FR-CHN-001：通道适配器接口**
- **描述**：所有收单通道必须实现统一的适配器接口
- **接口定义**：
  ```java
  interface ChannelAdapter {
      String channelId();           // 通道标识，如 "BITMART", "BINANCE_PAY"
      String displayName();         // 显示名称
      
      // 用户管理
      ChannelUser openUser(String merchantId, String buyerId);
      
      // 充值地址
      DepositAddress createDepositAddress(CreateDepositRequest req);
      
      // 退款
      ChannelRefund refund(RefundRequest req);
      ChannelRefund queryRefund(String channelRefundId);
      
      // 币种与汇率
      List<CurrencyConfig> getSupportedCurrencies();
      ExchangeRate getExchangeRate(String token, String network, String quoteCurrency);
      
      // 健康检查
      boolean isHealthy();
  }
  ```

**FR-CHN-002：智能路由策略**
- **描述**：系统应根据汇率自动选择最优通道
- **路由逻辑**：
  1. 获取所有健康通道
  2. 查询每个通道的汇率
  3. 按汇率排序（最高价格 = 买家获得更多加密货币）
  4. 汇率查询失败的通道降级到末尾
  5. 返回排序后的通道列表

**FR-CHN-003：支持的通道**
- **BitMart**：非 prod profile 的 stub 实现
- **Binance Pay**：非 prod profile 的 stub 实现
- **Coinbase Commerce**：REST charge/rate 实现，支持 live 验证
- **自建节点**：委托给执行层 CryptoPayment

**FR-CHN-004：汇率缓存**
- **描述**：系统应缓存汇率数据以减少上游调用
- **缓存策略**：
  - `CurrencyRateCache` 端口抽象
  - Redis 实现：key 格式 `nexusflow:rate:{channel}:{token}:{network}:{quote}`
  - 可配置 TTL（`rate-ttl-seconds`、`currency-ttl-seconds`）
  - Redis 不可用时自动降级到适配器直调

#### 2.2.3 收银台服务

**FR-CSH-001：订单状态查询**
- **描述**：收银台应能查询订单的当前状态
- **API**：`GET /cashier/order/status?paymentId={paymentId}`
- **返回**：
  - 订单基本信息（金额、币种、网络）
  - 支付地址和 memo
  - 已付金额和待付金额
  - 交易哈希
  - 过期时间
  - 支付流水数量

**FR-CSH-002：支付提交**
- **描述**：收银台应能提交支付请求获取充值地址
- **API**：`POST /cashier/pay/submit`
- **请求参数**：
  ```json
  {
    "paymentId": "支付ID（必填）",
    "channelId": "通道ID（可选，默认使用订单路由的通道）",
    "token": "代币（必填）",
    "network": "网络（必填）"
  }
  ```
- **处理逻辑**：
  1. 验证订单状态（必须是 WAITING_PAYMENT 或 PARTIALLY_PAID）
  2. 取消之前的活跃流水
  3. 通过通道创建充值地址
  4. 创建支付流水记录
  5. 返回充值地址、memo、过期时间等

**FR-CSH-003：收银台前端页面**
- **描述**：系统应提供静态收银台页面
- **功能**：
  - 订单摘要展示（商户名、订单号、金额、币种、网络、过期时间）
  - 二维码生成（Canvas）
  - 地址复制功能
  - 金额复制功能
  - 倒计时展示
  - 状态轮询（直到终态）
  - 移动端钱包跳转

#### 2.2.4 退款管理

**FR-RFD-001：退款申请**
- **描述**：商户应能为已确认订单申请退款
- **API**：`POST /refund/order`
- **请求参数**：
  ```json
  {
    "merchantId": "商户ID（必填）",
    "merchantOrderNo": "商户订单号（必填）",
    "refundOrderNo": "退款单号（必填）",
    "refundAmountFiat": "退款法币金额（必填）",
    "toAddress": "退款地址（可选，默认退到原支付地址）",
    "notifyUrl": "回调地址（可选）"
  }
  ```
- **业务规则**：
  - 只有 CONFIRMED 状态的订单可以退款
  - 退款金额不能超过已付金额
  - 退款金额自动转换为加密货币
  - 自建节点退款会生成 gas budget 事件

**FR-RFD-002：退款回调处理**
- **描述**：系统应能接收通道的退款结果回调
- **API**：`POST /callback/{channelId}/refund`
- **处理逻辑**：
  1. 解析回调参数（channelRefundId、status、txHash）
  2. 更新退款单状态
  3. 更新订单状态（REFUNDED 或 REFUND_FAILED）
  4. 发布事件并触发 Webhook

**FR-RFD-003：退款重试**
- **描述**：系统应支持失败退款的重试
- **状态转换**：REFUND_FAILED → REFUND_PROCESSING
- **触发**：商户重新发起退款请求

#### 2.2.5 通道回调处理

**FR-CBK-001：支付回调**
- **描述**：系统应能接收通道的支付结果回调
- **API**：`POST /callback/{channelId}/payment`
- **回调参数**：
  ```json
  {
    "reference_order_no": "通道订单号",
    "tx_hash": "交易哈希",
    "cumulative_amount": "累计支付金额",
    "amount_fiat": "法币金额（可选）",
    "event_id": "事件ID（用于去重）"
  }
  ```
- **处理逻辑**：
  1. 事件 ID 去重（防止重复处理）
  2. 查找对应订单
  3. 验证支付金额
  4. 状态转换（CONFIRMED 或 PARTIALLY_PAID）
  5. 更新支付流水
  6. 发布事件并触发 Webhook

**FR-CBK-002：回调签名验证**
- **描述**：所有通道回调必须验证 HMAC 签名
- **实现**：`CallbackHmacFilter` + `HmacSignatureVerifier`
- **签名算法**：HMAC-SHA256
- **签名头**：`X-Signature`

#### 2.2.6 Webhook 通知

**FR-WHK-001：商户回调通知**
- **描述**：系统应在订单状态变更时通知商户
- **触发条件**：订单状态变更（排除 CREATED → PENDING）
- **Payload 格式**：
  ```json
  {
    "payment_id": "支付ID",
    "merchant_order_no": "商户订单号",
    "status": "订单状态",
    "amount": "订单金额",
    "paid_amount": "已付法币金额",
    "paid_crypto_amount": "已付加密货币金额",
    "tx_hash": "交易哈希",
    "currency": "币种",
    "timestamp": "时间戳"
  }
  ```
- **安全**：
  - SSRF 防护（拒绝非 HTTPS 和私有 IP）
  - HMAC-SHA256 签名（`X-Signature` 头）
  - 重试策略：{5, 15, 60, 300} 秒

**FR-WHK-002：执行层回调通知**
- **描述**：系统应在加密支付状态变更时通知外部系统
- **Payload 格式**：
  ```json
  {
    "event_id": "事件ID",
    "event_type": "事件类型",
    "payment_id": "支付ID",
    "order_id": "订单ID",
    "status": "新状态",
    "previous_status": "旧状态",
    "currency": "币种",
    "expected_amount": "期望金额",
    "received_amount": "已收金额",
    "receiving_address": "收款地址",
    "tx_hash": "交易哈希",
    "confirmations": "确认数",
    "detected_block_number": "检测区块号",
    "timestamp": "时间戳"
  }
  ```

**FR-WHK-003：Dead Letter 管理**
- **描述**：系统应记录投递失败的 Webhook 并支持重试
- **存储**：`webhook_dead_letters` 表
- **管理 API**：
  - `GET /ops/webhook-dead-letters`：查询 dead letter 列表
  - `POST /ops/webhook-dead-letters/{id}/replay`：重试投递
  - `POST /ops/webhook-dead-letters/{id}/ignore`：标记忽略

#### 2.2.7 法币通道集成（Fiat Ramp）

**FR-FIAT-001：报价查询**
- **描述**：系统应支持查询法币-加密货币兑换报价
- **API**：`POST /fiat/ramp/quote`
- **请求参数**：
  ```json
  {
    "merchantId": "商户ID",
    "direction": "方向（BUY/SELL）",
    "fiatAmount": "法币金额",
    "fiatCurrency": "法币币种",
    "cryptoAmount": "加密货币金额",
    "token": "代币",
    "network": "网络",
    "walletAddress": "钱包地址",
    "country": "国家",
    "paymentMethod": "支付方式",
    "preferredGateway": "首选网关"
  }
  ```
- **返回**：报价 ID、汇率、费用、过期时间等

**FR-FIAT-002：创建 Ramp 订单**
- **描述**：系统应支持创建法币-加密货币兑换订单
- **API**：`POST /fiat/ramp/orders`
- **请求参数**：类似报价请求，额外包含 merchantOrderNo、paymentId、notifyUrl、returnUrl
- **业务规则**：
  - merchantOrderNo 在同一商户下必须唯一
  - 自动选择健康且匹配的网关
  - 支持法币计价或加密货币计价

**FR-FIAT-003：Ramp 订单查询**
- **描述**：系统应支持查询 Ramp 订单详情
- **API**：`GET /fiat/ramp/orders/{rampOrderId}`
- **返回**：订单完整信息，包括状态、金额、汇率、交易哈希等

**FR-FIAT-004：Ramp 回调处理**
- **描述**：系统应能接收法币通道的状态回调
- **API**：`POST /callback/{gatewayId}/fiat-ramp`
- **状态**：PROCESSING、COMPLETED、FAILED、EXPIRED、CANCELLED
- **安全**：HMAC 签名验证

**FR-FIAT-005：Ramp 状态跟踪**
- **描述**：系统应跟踪 Ramp 订单的完整生命周期
- **状态机**：
  ```
  CREATED → PROCESSING → COMPLETED
                        → FAILED
                        → EXPIRED
                        → CANCELLED
  ```

---

### 2.3 商户体系需求

#### 2.3.1 商户身份管理

**FR-MER-001：商户主数据**
- **描述**：系统应维护商户的基本信息
- **数据模型**：`merchant_profiles` 表
  ```sql
  CREATE TABLE merchant_profiles (
      merchant_id VARCHAR(36) PRIMARY KEY,      -- 内部UUID
      merchant_code VARCHAR(64) UNIQUE NOT NULL, -- 外部编码
      display_name VARCHAR(128),                 -- 显示名
      legal_name VARCHAR(256),                   -- 主体名称
      status VARCHAR(32) NOT NULL,               -- PENDING/ACTIVE/SUSPENDED/CLOSED
      risk_level VARCHAR(32),                    -- LOW/MEDIUM/HIGH
      default_fiat_currency VARCHAR(16),         -- 默认法币
      metadata TEXT,                             -- 扩展资料
      created_at TIMESTAMPTZ NOT NULL,
      updated_at TIMESTAMPTZ NOT NULL,
      version BIGINT NOT NULL DEFAULT 0          -- 乐观锁
  );
  ```
- **状态流转**：
  - PENDING → ACTIVE（审核通过）
  - ACTIVE → SUSPENDED（冻结）
  - SUSPENDED → ACTIVE（解冻）
  - ACTIVE → CLOSED（关闭）

**FR-MER-002：商户管理 API**
- **创建商户**：`POST /admin/merchants`
- **商户列表**：`GET /admin/merchants`（支持状态/关键字分页）
- **商户详情**：`GET /admin/merchants/{merchantId}`
- **更新资料**：`PATCH /admin/merchants/{merchantId}`
- **激活**：`POST /admin/merchants/{merchantId}/activate`
- **冻结**：`POST /admin/merchants/{merchantId}/suspend`（必须传 reason）

#### 2.3.2 商户用户体系

**FR-USR-001：商户用户**
- **描述**：系统应支持商户门户的登录用户管理
- **数据模型**：`merchant_users` 表
  ```sql
  CREATE TABLE merchant_users (
      user_id VARCHAR(36) PRIMARY KEY,
      email VARCHAR(256) UNIQUE NOT NULL,
      password_hash VARCHAR(256),
      display_name VARCHAR(128),
      status VARCHAR(32) NOT NULL,  -- ACTIVE/INVITED/DISABLED
      last_login_at TIMESTAMPTZ,
      created_at TIMESTAMPTZ NOT NULL,
      updated_at TIMESTAMPTZ NOT NULL
  );
  ```

**FR-USR-002：用户-商户成员关系**
- **描述**：一个用户可以属于多个商户，每个商户有不同的角色
- **数据模型**：`merchant_user_memberships` 表
  ```sql
  CREATE TABLE merchant_user_memberships (
      merchant_id VARCHAR(36) NOT NULL,
      user_id VARCHAR(36) NOT NULL,
      role_code VARCHAR(64) NOT NULL,  -- OWNER/DEVELOPER/FINANCE/SUPPORT
      status VARCHAR(32) NOT NULL,     -- ACTIVE/DISABLED
      created_at TIMESTAMPTZ NOT NULL,
      PRIMARY KEY (merchant_id, user_id)
  );
  ```

**FR-USR-003：角色权限矩阵**

| 角色 | 权限 |
|------|------|
| OWNER | 全部权限，包括团队管理、API Key 管理、Webhook 配置 |
| DEVELOPER | 订单查看、API Key 管理、Webhook 配置、开发者文档 |
| FINANCE | 订单查看、退款管理、报表导出 |
| SUPPORT | 订单查看、退款查看 |

**FR-USR-004：登录会话**
- **描述**：系统应支持商户用户的登录会话管理
- **API**：
  - `POST /auth/login`：登录
  - `POST /auth/logout`：登出
  - `GET /auth/me`：获取当前用户信息
- **会话管理**：
  - 优先使用 HttpOnly + Secure + SameSite cookie
  - 如使用 Bearer token，必须有短有效期、refresh、退出和设备管理
  - 返回用户信息和商户 membership 列表

#### 2.3.3 商户级 API 认证

**FR-AUTH-001：API Key 管理**
- **描述**：系统应支持商户级 API Key 的创建和管理
- **数据模型**：`merchant_api_keys` 表
  ```sql
  CREATE TABLE merchant_api_keys (
      key_id VARCHAR(36) PRIMARY KEY,
      merchant_id VARCHAR(36) NOT NULL,
      key_prefix VARCHAR(16) NOT NULL,          -- 展示用前缀
      key_hash VARCHAR(256) NOT NULL,           -- HMAC/Argon2/bcrypt hash
      environment VARCHAR(16) NOT NULL,         -- TEST/LIVE
      status VARCHAR(32) NOT NULL,              -- ACTIVE/DISABLED/REVOKED
      scopes TEXT,                              -- 权限范围
      ip_allowlist TEXT,                        -- IP 白名单
      last_used_at TIMESTAMPTZ,
      expires_at TIMESTAMPTZ,
      created_by VARCHAR(36),
      created_at TIMESTAMPTZ NOT NULL,
      updated_at TIMESTAMPTZ NOT NULL
  );
  ```
- **API**：
  - `POST /merchant/api-keys`：创建 API Key（返回一次明文）
  - `GET /merchant/api-keys`：API Key 列表
  - `POST /merchant/api-keys/{keyId}/rotate`：轮换
  - `POST /merchant/api-keys/{keyId}/disable`：禁用
- **安全规则**：
  - API Key 只在创建时返回一次明文
  - 后续只能显示 key_prefix、状态、scope 和最近使用时间
  - 使用 SHA-256 hash 存储（加服务端 pepper）

**FR-AUTH-002：API Key 认证流程**
- **描述**：所有商户 API 请求必须通过 API Key 认证
- **认证流程**：
  1. 请求携带 `X-API-Key: nf_live_xxx`
  2. Filter 通过 key prefix 找候选记录
  3. 验证 hash、状态、过期时间、IP allowlist
  4. 解析出 merchant_id、merchant_code、key_id、scopes
  5. 写入 request attributes：
     - `authType=MERCHANT_API_KEY`
     - `merchantId=<内部 UUID>`
     - `merchantCode=<外部编码>`
     - `actorId=<key_id>`
     - `scopes=[...]`
  6. 记录 last_used_at 和审计日志

**FR-AUTH-003：请求商户校验**
- **描述**：所有商户 API 请求必须校验请求中的商户与认证上下文一致
- **实现**：`MerchantRequestGuard.requireMatchingMerchant()`
- **校验点**：
  - POST 请求：校验 body 中的 merchantId 与 API Key 绑定商户一致
  - GET 请求：校验返回数据的 merchantId 与当前商户一致
  - 运营/Admin 接口：限制商户 key 访问 `/ops/*` 和 `/crypto/*`

#### 2.3.4 Webhook 配置管理

**FR-WCF-001：Webhook 配置**
- **描述**：系统应支持商户配置 Webhook 回调
- **数据模型**：`merchant_webhook_configs` 表
  ```sql
  CREATE TABLE merchant_webhook_configs (
      webhook_id VARCHAR(36) PRIMARY KEY,
      merchant_id VARCHAR(36) NOT NULL,
      url VARCHAR(1024) NOT NULL,
      secret_hash VARCHAR(256),           -- 出站签名 secret
      event_types TEXT,                   -- 订阅事件
      status VARCHAR(32) NOT NULL,        -- ACTIVE/DISABLED
      retry_policy TEXT,                  -- 重试策略
      last_success_at TIMESTAMPTZ,
      last_failure_at TIMESTAMPTZ,
      created_at TIMESTAMPTZ NOT NULL,
      updated_at TIMESTAMPTZ NOT NULL
  );
  ```
- **API**：
  - `GET /merchant/webhooks`：Webhook 配置列表
  - `POST /merchant/webhooks`：新增配置
  - `PATCH /merchant/webhooks/{webhookId}`：更新配置
  - `POST /merchant/webhooks/{webhookId}/test`：发送测试事件

**FR-WCF-002：Webhook 策略**
- **描述**：系统应按以下策略管理 Webhook
- **策略**：
  1. 默认使用 `merchant_webhook_configs` 中的 active URL
  2. 订单级 `notifyUrl` 作为可选 override（默认关闭）
  3. 每个商户有独立 webhook secret
  4. Payload 必须包含 event_id、event_type、merchant_id、merchant_order_no、payment_id、occurred_at

#### 2.3.5 数据隔离

**FR-DIS-001：商户数据隔离**
- **描述**：系统应确保商户只能访问自己的数据
- **隔离范围**：
  - 订单查询：按 merchant_id 过滤
  - 退款查询：按 merchant_id 过滤
  - Fiat Ramp 查询：按 merchant_id 过滤
  - Webhook Dead Letter：按 merchant_id 过滤
- **实现**：
  - 商户 API key 请求不能信任 body 中的 merchantId，只能作为兼容字段校验
  - 所有商户侧查询必须带 merchant_id 条件
  - Ops/Admin 查询可以跨商户，但必须走内部权限

**FR-DIS-002：审计日志**
- **描述**：系统应记录所有关键操作的审计日志
- **数据模型**：`merchant_audit_logs` 表
  ```sql
  CREATE TABLE merchant_audit_logs (
      audit_id VARCHAR(36) PRIMARY KEY,
      merchant_id VARCHAR(36),
      actor_type VARCHAR(32) NOT NULL,  -- MERCHANT_USER/INTERNAL_USER/API_KEY/SYSTEM
      actor_id VARCHAR(64) NOT NULL,
      action VARCHAR(128) NOT NULL,
      target_type VARCHAR(64),
      target_id VARCHAR(64),
      reason VARCHAR(512),
      request_id VARCHAR(64),
      ip_address VARCHAR(64),
      before_data TEXT,
      after_data TEXT,
      created_at TIMESTAMPTZ NOT NULL
  );
  ```
- **记录场景**：
  - 商户状态变更
  - API Key 创建/轮换/禁用
  - Webhook 配置变更
  - 运营处置操作
  - 私钥解密操作

---

### 2.4 前端产品需求

#### 2.4.1 买家收银台（Checkout）

**FR-CHK-001：订单展示**
- **描述**：收银台应清晰展示订单信息
- **展示内容**：
  - 商户名称和 Logo
  - 订单号
  - 支付金额（法币 + 加密货币）
  - 币种和网络
  - 过期时间倒计时
  - 支付状态

**FR-CHK-002：支付操作**
- **描述**：收银台应提供便捷的支付操作
- **功能**：
  - 收款地址二维码（Canvas 生成）
  - 地址复制按钮
  - 金额复制按钮
  - 移动端钱包跳转链接
  - 网络切换限制（防止误操作）

**FR-CHK-003：状态跟踪**
- **描述**：收银台应实时跟踪支付状态
- **状态**：
  - 待支付：显示支付指引
  - 确认中：显示确认进度
  - 已完成：显示成功页面，支持自动跳转
  - 已过期：显示过期提示
  - 失败：显示失败原因和重试指引

**FR-CHK-004：安全与品牌**
- **描述**：收银台应确保安全性和品牌一致性
- **安全**：
  - checkout token 校验（防止未授权访问）
  - 敏感字段最小化暴露
  - HTTPS 强制
- **品牌**：
  - 商户 Logo 和名称展示
  - 可配置主题色

#### 2.4.2 商户门户（Merchant Portal）

**FR-MPT-001：Dashboard**
- **描述**：商户门户首页应展示关键业务指标
- **指标**：
  - 今日/本周/本月订单量
  - 支付成功率
  - 待处理退款数量
  - Webhook 失败数量
  - 通道可用性摘要
  - 最近订单列表

**FR-MPT-002：订单管理**
- **描述**：商户应能查看和管理订单
- **功能**：
  - 订单列表（分页、筛选、排序）
  - 订单详情（支付时间线、链上交易）
  - 订单导出（CSV/Excel）
  - 收银台链接生成

**FR-MPT-003：退款管理**
- **描述**：商户应能发起和管理退款
- **功能**：
  - 退款申请（选择订单、输入金额）
  - 退款列表和状态跟踪
  - 退款失败重试
  - 退款限制说明

**FR-MPT-004：开发者中心**
- **描述**：商户应能管理 API 集成配置
- **功能**：
  - API Key 管理（创建、查看、轮换、禁用）
  - Webhook 配置（URL、事件订阅、测试发送）
  - 签名文档和示例代码
  - 回调日志查看

**FR-MPT-005：团队管理**
- **描述**：商户应能管理团队成员
- **功能**：
  - 成员列表
  - 邀请成员
  - 角色分配
  - 禁用/启用成员
  - 最近登录时间

**FR-MPT-006：报表中心**
- **描述**：商户应能查看和导出业务报表
- **功能**：
  - 订单统计报表
  - 对账报表
  - Fiat Ramp 订单报表
  - 按时间范围筛选
  - CSV/Excel 导出

#### 2.4.3 运营控制台（Ops Console）

**FR-OPS-001：全局 Dashboard**
- **描述**：运营控制台首页应展示系统全局状态
- **指标**：
  - 通道健康状态
  - 订单状态分布
  - 异常趋势图
  - 积压任务数量
  - 风险告警

**FR-OPS-002：通道管理**
- **描述**：运营应能监控和管理收单通道
- **功能**：
  - 通道健康状态
  - 费率配置（只读或受控编辑）
  - 失败率统计
  - 通道禁用/恢复

**FR-OPS-003：风险管理**
- **描述**：运营应能处理异常交易
- **功能**：
  - Orphan Transaction 查询
  - Resolve 操作（关联到支付）
  - Ignore 操作（标记忽略）
  - Compensate 操作（补偿建单）
  - 操作原因和审计

**FR-OPS-004：Webhook DLQ 管理**
- **描述**：运营应能处理投递失败的 Webhook
- **功能**：
  - Dead Letter 列表（分页、筛选）
  - Payload 摘要查看
  - Replay 操作（重新投递）
  - Ignore 操作（标记忽略）
  - 失败原因分析

**FR-OPS-005：对账管理**
- **描述**：运营应能监控和处理对账异常
- **功能**：
  - 对账 backlog 查看
  - 重试操作
  - Reorg 处理
  - Missing event 补偿

**FR-OPS-006：支持视图**
- **描述**：运营应能快速定位问题订单
- **功能**：
  - 按 merchantId/paymentId/txHash 搜索
  - 全链路追踪
  - 订单时间线展示

#### 2.4.4 平台管理端（Admin Console）

**FR-ADM-001：商户生命周期管理**
- **描述**：管理员应能管理商户的完整生命周期
- **功能**：
  - 商户创建（审核流程）
  - 商户资料维护
  - 商户状态变更（激活/冻结/关闭）
  - 商户风险等级设置

**FR-ADM-002：凭证管理**
- **描述**：管理员应能管理商户的认证凭证
- **功能**：
  - API Key 管理（创建、禁用、轮换）
  - Webhook Secret 管理
  - IP 白名单管理
  - 凭证审计日志

**FR-ADM-003：RBAC 管理**
- **描述**：管理员应能管理角色和权限
- **功能**：
  - 权限点定义
  - 角色模板管理
  - 用户授权
  - 商户 scope / 内部 scope 管理

**FR-ADM-004：Provider 配置**
- **描述**：管理员应能查看和配置通道 Provider
- **功能**：
  - Provider 状态查看
  - 配置参数管理
  - 启用/禁用 Provider

**FR-ADM-005：系统管理**
- **描述**：管理员应能管理全局系统配置
- **功能**：
  - 环境配置只读检查
  - Feature flag 管理
  - 审计日志查看
  - 后台任务状态监控

#### 2.4.5 前端工程架构

**FR-FRT-001：工程结构**
- **描述**：前端应采用 monorepo 结构，支持多应用共享代码
- **结构**：
  ```
  frontend/
    package.json
    apps/
      checkout/          # 买家收银台
      merchant/          # 商户门户
      ops/               # 运营控制台
      admin/             # 平台管理端
    packages/
      ui/                # 组件库（表格、表单、状态标签、弹窗、布局、主题）
      api-client/        # 类型化 API client、错误处理、重试、分页模型
      auth/              # 会话、权限、路由守卫
      config/            # 环境变量、构建配置、lint/test 配置
  ```
- **技术栈**：React + TypeScript + Vite

**FR-FRT-002：API Client**
- **描述**：前端应有统一的 API 客户端
- **功能**：
  - 统一响应处理（成功/失败）
  - 错误处理和重试
  - 分页模型
  - 鉴权处理（自动携带 token）
  - 请求/响应拦截器

**FR-FRT-003：路由与权限**
- **描述**：前端应有统一的路由和权限管理
- **功能**：
  - 路由守卫（未登录重定向）
  - 权限控制（菜单和按钮级）
  - 动态路由（根据权限加载）

**FR-FRT-004：部署架构**
- **描述**：前端应支持独立部署
- **部署地址**：
  - Checkout: `https://pay.example.com/checkout/:token`
  - Merchant Portal: `https://merchant.example.com`
  - Ops Console: `https://ops.example.com`
  - Admin Console: `https://admin.example.com`

---

### 2.5 运营监控需求

**FR-OBS-001：运营 Dashboard API**
- **描述**：系统应提供运营 Dashboard 聚合数据
- **API**：`GET /ops/dashboard`
- **返回**：
  - 通道健康状态
  - 订单状态分布（各状态数量）
  - 对账 backlog
  - 风险告警（orphan transaction、webhook dead letter）

**FR-OBS-002：Orphan Transaction 管理**
- **描述**：系统应提供孤儿交易管理 API
- **API**：
  - `GET /crypto/orphan-transactions`：查询列表
  - `POST /crypto/orphan-transactions/{id}/resolve`：关联到支付
  - `POST /crypto/orphan-transactions/{id}/ignore`：标记忽略
  - `POST /crypto/orphan-transactions/{id}/compensate`：补偿建单

**FR-OBS-003：Webhook Dead Letter 管理**
- **描述**：系统应提供 Webhook Dead Letter 管理 API
- **API**：
  - `GET /ops/webhook-dead-letters`：查询列表
  - `POST /ops/webhook-dead-letters/{id}/replay`：重试投递
  - `POST /ops/webhook-dead-letters/{id}/ignore`：标记忽略

---

## 3. 非功能性需求

### 3.1 性能需求

**NFR-PERF-001：API 响应时间**
- 订单创建 API：< 500ms（P95）
- 订单查询 API：< 200ms（P95）
- 收银台状态查询：< 100ms（P95）

**NFR-PERF-002：吞吐量**
- 支持 1000+ TPS（订单创建）
- 支持 10000+ TPS（状态查询）

**NFR-PERF-003：区块扫描延迟**
- 新区块扫描延迟 < 30 秒
- 交易检测延迟 < 1 分钟

### 3.2 可用性需求

**NFR-AVL-001：系统可用性**
- 目标可用性：99.9%
- 计划内维护窗口：每月 4 小时

**NFR-AVL-002：故障恢复**
- RTO（恢复时间目标）：< 1 小时
- RPO（恢复点目标）：< 5 分钟

### 3.3 安全需求

**NFR-SEC-001：数据加密**
- 静态数据：AES-256-GCM 加密
- 传输数据：TLS 1.2+
- 私钥：支持外部 KMS（AWS KMS / HashiCorp Vault）

**NFR-SEC-002：认证与授权**
- API Key 认证：商户级隔离
- 会话管理：HttpOnly + Secure + SameSite cookie
- RBAC：角色权限控制

**NFR-SEC-003：安全防护**
- SSRF 防护：拒绝非 HTTPS 和私有 IP
- 限流：每 IP 每分钟 120 次
- 幂等性：orderId/eventId 去重
- SQL 注入防护：参数化查询
- XSS 防护：输入校验和输出编码

**NFR-SEC-004：审计日志**
- 所有关键操作必须记录审计日志
- 日志保留期限：至少 1 年
- 日志不可篡改

### 3.4 可扩展性需求

**NFR-SCA-001：水平扩展**
- 支持无状态服务水平扩展
- 数据库读写分离
- 缓存层（Redis）支持集群模式

**NFR-SCA-002：模块化架构**
- DDD 分层架构
- 通道适配器可插拔
- 区块链适配器可插拔

### 3.5 可观测性需求

**NFR-OBS-001：日志**
- 结构化日志（JSON 格式）
- 统一日志级别：ERROR/WARN/INFO/DEBUG
- 关键业务操作必须记录日志

**NFR-OBS-002：指标**
- Prometheus 指标暴露
- 关键指标：
  - API 响应时间
  - 订单创建/确认/退款数量
  - 通道健康状态
  - 区块扫描延迟

**NFR-OBS-003：链路追踪**
- OpenTelemetry 集成
- 分布式追踪（跨服务调用）
- 错误追踪和告警

### 3.6 可维护性需求

**NFR-MNT-001：代码质量**
- 单元测试覆盖率 > 80%
- 集成测试覆盖关键流程
- 代码审查流程

**NFR-MNT-002：文档**
- API 文档（OpenAPI/Swagger）
- 架构文档
- 部署文档
- 运维手册

---

## 4. 接口规范

### 4.1 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```

**错误码**：
- `NF-0001`：INVALID_REQUEST - 请求参数错误
- `NF-0002`：PAYMENT_NOT_FOUND - 支付不存在
- `NF-0003`：PAYMENT_ALREADY_EXISTS - 支付已存在
- `NF-0004`：UNAUTHORIZED - 未授权
- `NF-0005`：INVALID_SIGNATURE - 签名无效
- `NF-0006`：PAYMENT_EXPIRED - 支付已过期
- `NF-0007`：INVALID_STATE_TRANSITION - 无效状态转换
- `NF-0008`：NO_AVAILABLE_CHANNEL - 无可用通道
- `NF-0009`：REFUND_NOT_ALLOWED - 不允许退款
- `NF-0010`：REFUND_AMOUNT_EXCEEDED - 退款金额超限
- `NF-0011`：REFUND_NOT_FOUND - 退款不存在
- `NF-0012`：INTERNAL_ERROR - 内部错误

### 4.2 API 接口清单

#### 4.2.1 商户 API

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/pay/order` | 创建支付订单 | API Key |
| GET | `/pay/order/{paymentId}` | 查询订单详情 | API Key |
| POST | `/refund/order` | 申请退款 | API Key |
| POST | `/fiat/ramp/quote` | 查询 Ramp 报价 | API Key |
| POST | `/fiat/ramp/orders` | 创建 Ramp 订单 | API Key |
| GET | `/fiat/ramp/orders/{rampOrderId}` | 查询 Ramp 订单 | API Key |

#### 4.2.2 收银台 API

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| GET | `/cashier/order/status` | 查询订单状态 | 无（通过 paymentId） |
| POST | `/cashier/pay/submit` | 提交支付 | 无（通过 paymentId） |

#### 4.2.3 回调 API

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/callback/{channelId}/payment` | 支付回调 | HMAC 签名 |
| POST | `/callback/{channelId}/refund` | 退款回调 | HMAC 签名 |
| POST | `/callback/{gatewayId}/fiat-ramp` | Ramp 回调 | HMAC 签名 |

#### 4.2.4 运营 API

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| GET | `/ops/dashboard` | 运营 Dashboard | API Key |
| GET | `/crypto/orphan-transactions` | 孤儿交易列表 | API Key |
| POST | `/crypto/orphan-transactions/{id}/resolve` | 解决孤儿交易 | API Key |
| POST | `/crypto/orphan-transactions/{id}/ignore` | 忽略孤儿交易 | API Key |
| POST | `/crypto/orphan-transactions/{id}/compensate` | 补偿孤儿交易 | API Key |
| GET | `/ops/webhook-dead-letters` | Webhook DLQ 列表 | API Key |
| POST | `/ops/webhook-dead-letters/{id}/replay` | 重试 Webhook | API Key |
| POST | `/ops/webhook-dead-letters/{id}/ignore` | 忽略 Webhook | API Key |

#### 4.2.5 执行层 API（内部）

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/crypto/payments` | 创建加密支付 | API Key |

#### 4.2.6 商户管理 API（待实现）

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/admin/merchants` | 创建商户 | 内部会话 |
| GET | `/admin/merchants` | 商户列表 | 内部会话 |
| GET | `/admin/merchants/{merchantId}` | 商户详情 | 内部会话 |
| PATCH | `/admin/merchants/{merchantId}` | 更新商户 | 内部会话 |
| POST | `/admin/merchants/{merchantId}/activate` | 激活商户 | 内部会话 |
| POST | `/admin/merchants/{merchantId}/suspend` | 冻结商户 | 内部会话 |

#### 4.2.7 商户门户 API（待实现）

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| GET | `/merchant/me` | 当前用户信息 | 会话 |
| GET | `/merchant/profile` | 商户资料 | 会话 |
| PATCH | `/merchant/profile` | 更新资料 | 会话 |
| GET | `/merchant/api-keys` | API Key 列表 | 会话 |
| POST | `/merchant/api-keys` | 创建 API Key | 会话 |
| POST | `/merchant/api-keys/{keyId}/rotate` | 轮换 API Key | 会话 |
| POST | `/merchant/api-keys/{keyId}/disable` | 禁用 API Key | 会话 |
| GET | `/merchant/webhooks` | Webhook 配置 | 会话 |
| POST | `/merchant/webhooks` | 新增配置 | 会话 |
| PATCH | `/merchant/webhooks/{webhookId}` | 更新配置 | 会话 |
| POST | `/merchant/webhooks/{webhookId}/test` | 测试 Webhook | 会话 |

#### 4.2.8 认证 API（待实现）

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/auth/login` | 登录 | 无 |
| POST | `/auth/logout` | 登出 | 会话 |
| GET | `/auth/me` | 当前用户 | 会话 |

---

## 5. 数据库设计

### 5.1 执行层表

#### 5.1.1 crypto_payments
```sql
CREATE TABLE crypto_payments (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    expected_amount DECIMAL(20,8) NOT NULL,
    expected_currency VARCHAR(32) NOT NULL,
    received_amount DECIMAL(20,8),
    receiving_address VARCHAR(128) NOT NULL,
    callback_url VARCHAR(1024),
    tx_hash VARCHAR(128),
    confirmations INT,
    detected_block_number BIGINT,
    retry_count INT DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    last_failure_reason VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
```

#### 5.1.2 wallets
```sql
CREATE TABLE wallets (
    id VARCHAR(36) PRIMARY KEY,
    chain VARCHAR(32) NOT NULL,
    address VARCHAR(128) NOT NULL,
    encrypted_private_key TEXT NOT NULL,
    wallet_type VARCHAR(32) NOT NULL,
    mpc_wallet_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
```

#### 5.1.3 address_pool
```sql
CREATE TABLE address_pool (
    id VARCHAR(36) PRIMARY KEY,
    chain VARCHAR(32) NOT NULL,
    address VARCHAR(128) NOT NULL,
    encrypted_private_key TEXT NOT NULL,
    derivation_index INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    assigned_to_payment_id VARCHAR(36),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

#### 5.1.4 mnemonic_backups
```sql
CREATE TABLE mnemonic_backups (
    id VARCHAR(36) PRIMARY KEY,
    encrypted_mnemonic TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
```

#### 5.1.5 chain_scan_cursors
```sql
CREATE TABLE chain_scan_cursors (
    chain VARCHAR(32) PRIMARY KEY,
    last_scanned_block BIGINT NOT NULL,
    last_block_hash VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL
);
```

#### 5.1.6 orphan_transactions
```sql
CREATE TABLE orphan_transactions (
    id VARCHAR(36) PRIMARY KEY,
    chain VARCHAR(32) NOT NULL,
    tx_hash VARCHAR(128) NOT NULL,
    from_address VARCHAR(128),
    to_address VARCHAR(128) NOT NULL,
    amount DECIMAL(20,8) NOT NULL,
    currency VARCHAR(32) NOT NULL,
    block_number BIGINT,
    block_timestamp TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    resolved_payment_id VARCHAR(36),
    resolution_reason VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

### 5.2 编排层表

#### 5.2.1 payment_orders
```sql
CREATE TABLE payment_orders (
    payment_id VARCHAR(36) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    merchant_order_no VARCHAR(128) NOT NULL,
    amount_fiat DECIMAL(20,2),
    currency_fiat VARCHAR(16),
    amount_crypto DECIMAL(20,8),
    currency_crypto VARCHAR(32),
    network VARCHAR(32),
    exchange_rate DECIMAL(20,8),
    channel_id VARCHAR(32),
    channel_user_id VARCHAR(128),
    channel_order_id VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    pay_address VARCHAR(128),
    memo VARCHAR(256),
    paid_amount_crypto DECIMAL(20,8),
    paid_amount_fiat DECIMAL(20,2),
    tx_hash VARCHAR(128),
    notify_url VARCHAR(1024),
    return_url VARCHAR(1024),
    extend_data TEXT,
    expire_time TIMESTAMPTZ,
    pay_time TIMESTAMPTZ,
    confirm_time TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (merchant_id, merchant_order_no)
);
```

#### 5.2.2 payment_flows
```sql
CREATE TABLE payment_flows (
    flow_no VARCHAR(32) PRIMARY KEY,
    payment_id VARCHAR(36) NOT NULL,
    channel_id VARCHAR(32) NOT NULL,
    token VARCHAR(32) NOT NULL,
    network VARCHAR(32) NOT NULL,
    crypto_amount DECIMAL(20,8) NOT NULL,
    fiat_amount DECIMAL(20,2),
    fiat_currency VARCHAR(16),
    exchange_rate DECIMAL(20,8),
    pay_address VARCHAR(128),
    memo VARCHAR(256),
    status VARCHAR(32) NOT NULL,
    tx_hash VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
```

#### 5.2.3 refund_orders
```sql
CREATE TABLE refund_orders (
    refund_order_no VARCHAR(64) PRIMARY KEY,
    payment_id VARCHAR(36) NOT NULL,
    channel_refund_id VARCHAR(128),
    refund_amount_fiat DECIMAL(20,2) NOT NULL,
    refund_amount_crypto DECIMAL(20,8) NOT NULL,
    exchange_rate DECIMAL(20,8),
    token VARCHAR(32),
    network VARCHAR(32),
    to_address VARCHAR(128),
    notify_url VARCHAR(1024),
    status VARCHAR(32) NOT NULL,
    tx_hash VARCHAR(128),
    failure_reason VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
```

### 5.3 商户体系表

#### 5.3.1 merchant_profiles
```sql
CREATE TABLE merchant_profiles (
    merchant_id VARCHAR(36) PRIMARY KEY,
    merchant_code VARCHAR(64) UNIQUE NOT NULL,
    display_name VARCHAR(128),
    legal_name VARCHAR(256),
    status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(32),
    default_fiat_currency VARCHAR(16),
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
```

#### 5.3.2 merchant_users
```sql
CREATE TABLE merchant_users (
    user_id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(256) UNIQUE NOT NULL,
    password_hash VARCHAR(256),
    display_name VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

#### 5.3.3 merchant_user_memberships
```sql
CREATE TABLE merchant_user_memberships (
    merchant_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (merchant_id, user_id)
);
```

#### 5.3.4 merchant_api_keys
```sql
CREATE TABLE merchant_api_keys (
    key_id VARCHAR(36) PRIMARY KEY,
    merchant_id VARCHAR(36) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    key_hash VARCHAR(256) NOT NULL,
    environment VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    scopes TEXT,
    ip_allowlist TEXT,
    last_used_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_by VARCHAR(36),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

#### 5.3.5 merchant_webhook_configs
```sql
CREATE TABLE merchant_webhook_configs (
    webhook_id VARCHAR(36) PRIMARY KEY,
    merchant_id VARCHAR(36) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    secret_hash VARCHAR(256),
    event_types TEXT,
    status VARCHAR(32) NOT NULL,
    retry_policy TEXT,
    last_success_at TIMESTAMPTZ,
    last_failure_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

#### 5.3.6 merchant_audit_logs
```sql
CREATE TABLE merchant_audit_logs (
    audit_id VARCHAR(36) PRIMARY KEY,
    merchant_id VARCHAR(36),
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    action VARCHAR(128) NOT NULL,
    target_type VARCHAR(64),
    target_id VARCHAR(64),
    reason VARCHAR(512),
    request_id VARCHAR(64),
    ip_address VARCHAR(64),
    before_data TEXT,
    after_data TEXT,
    created_at TIMESTAMPTZ NOT NULL
);
```

### 5.4 支撑表

#### 5.4.1 idempotency_keys
```sql
CREATE TABLE idempotency_keys (
    key VARCHAR(128) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    response_body TEXT,
    status_code INT,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
```

#### 5.4.2 webhook_dead_letters
```sql
CREATE TABLE webhook_dead_letters (
    id VARCHAR(36) PRIMARY KEY,
    delivery_type VARCHAR(32) NOT NULL,
    target_url VARCHAR(1024) NOT NULL,
    payload TEXT,
    event_id VARCHAR(64),
    event_type VARCHAR(64),
    payment_id VARCHAR(36),
    order_id VARCHAR(64),
    failure_reason VARCHAR(512),
    attempts INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

#### 5.4.3 fiat_ramp_orders
```sql
CREATE TABLE fiat_ramp_orders (
    ramp_order_id VARCHAR(36) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    merchant_order_no VARCHAR(128) NOT NULL,
    payment_id VARCHAR(36),
    direction VARCHAR(16) NOT NULL,
    provider_id VARCHAR(32),
    provider_order_id VARCHAR(128),
    quote_id VARCHAR(64),
    fiat_amount DECIMAL(20,2),
    fiat_currency VARCHAR(16),
    crypto_amount DECIMAL(20,8),
    token VARCHAR(32),
    network VARCHAR(32),
    exchange_rate DECIMAL(20,8),
    fee_amount_fiat DECIMAL(20,2),
    wallet_address VARCHAR(128),
    checkout_url VARCHAR(1024),
    fiat_transfer_id VARCHAR(128),
    crypto_tx_hash VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(512),
    expire_time TIMESTAMPTZ,
    complete_time TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (merchant_id, merchant_order_no)
);
```

---

## 6. 状态机定义

### 6.1 执行层 CryptoPayment 状态机

```
CREATED → PENDING → DETECTED → CONFIRMING → CONFIRMED
                                          → FAILED
                  → EXPIRED
```

**允许的状态转换**：
- CREATED → PENDING
- PENDING → DETECTED
- PENDING → EXPIRED
- DETECTED → CONFIRMING
- CONFIRMING → CONFIRMED
- CONFIRMING → FAILED

**规则**：
- 禁止回退转换
- 状态变更必须事件驱动
- 禁止手动状态更新（除管理工具）

### 6.2 编排层 PaymentOrder 状态机

```
WAITING_PAYMENT → CONFIRMED → REFUND_PROCESSING → REFUNDED
                → PARTIALLY_PAID → CONFIRMED       → REFUND_FAILED → REFUND_PROCESSING
                → EXPIRED
                → FAILED
```

**允许的状态转换**：
- WAITING_PAYMENT → CONFIRMED
- WAITING_PAYMENT → PARTIALLY_PAID
- WAITING_PAYMENT → EXPIRED
- WAITING_PAYMENT → FAILED
- PARTIALLY_PAID → CONFIRMED
- PARTIALLY_PAID → EXPIRED
- PARTIALLY_PAID → FAILED
- CONFIRMED → REFUND_PROCESSING
- REFUND_PROCESSING → REFUNDED
- REFUND_PROCESSING → REFUND_FAILED
- REFUND_FAILED → REFUND_PROCESSING（重试）

**终态**：EXPIRED、REFUNDED、FAILED

### 6.3 编排层 RefundOrder 状态机

```
PENDING → PROCESSING → SUCCESS
                     → FAILED → PROCESSING（重试）
```

### 6.4 编排层 PaymentFlow 状态机

```
WAITING → CONFIRMED
        → CANCELLED
```

### 6.5 法币通道 FiatRampOrder 状态机

```
CREATED → PROCESSING → COMPLETED
                     → FAILED
                     → EXPIRED
                     → CANCELLED
```

### 6.6 孤儿交易 OrphanTransaction 状态机

```
DETECTED → RESOLVED
         → IGNORED
         → COMPENSATED
```

---

## 7. 事件定义

### 7.1 执行层事件

| 事件类型 | 触发条件 | Payload |
|----------|----------|---------|
| `crypto.payment.detected` | 交易检测到 | paymentId, txHash, receivedAmount |
| `crypto.payment.confirmed` | 支付确认 | paymentId, txHash, confirmations |
| `crypto.payment.failed` | 支付失败 | paymentId, reason |
| `crypto.orphan.detected` | 孤儿交易检测 | txHash, toAddress, amount, currency |

### 7.2 编排层事件

| 事件类型 | 触发条件 | Payload |
|----------|----------|---------|
| `order.created` | 订单创建 | paymentId, merchantOrderNo, merchantId |
| `order.confirmed` | 订单确认 | paymentId, txHash, paidAmount |
| `order.expired` | 订单过期 | paymentId |
| `order.refund_processing` | 退款处理中 | paymentId |
| `order.refunded` | 退款完成 | paymentId |
| `order.refund_failed` | 退款失败 | paymentId |
| `refund.requested` | 退款请求 | refundOrderNo, paymentId, amount |

---

## 8. 配置参数

### 8.1 核心配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `nexusflow.cashier.base-url` | `/checkout.html` | 收银台基础 URL |
| `nexusflow.payment.expiry-minutes` | `30` | 支付过期时间（分钟） |
| `nexusflow.order.expiry.interval-ms` | `60000` | 订单过期检查间隔（毫秒） |
| `nexusflow.reconciliation.interval-ms` | `30000` | 对账检查间隔（毫秒） |
| `nexusflow.scanner.reorg-rewind-blocks` | `100` | Reorg 回退区块数 |

### 8.2 安全配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `nexusflow.encryption.key` | - | AES-256-GCM 加密密钥（必填） |
| `nexusflow.encryption.allow-generated-key` | `false` | 是否允许生成临时密钥（仅开发） |
| `nexusflow.api.key` | - | 全局 API Key（dev/test fallback） |
| `nexusflow.callback.hmac-secret.*` | - | 通道回调 HMAC 密钥 |

### 8.3 区块链配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `nexusflow.ethereum.rpc-url` | - | Ethereum RPC URL |
| `nexusflow.ethereum.usdt-contract` | - | USDT 合约地址 |
| `nexusflow.tron.node-url` | - | Tron 节点 URL |
| `nexusflow.bitcoin.rpc-url` | - | Bitcoin RPC URL |

### 8.4 缓存配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `nexusflow.cache.enabled` | `false` | 是否启用 Redis 缓存 |
| `nexusflow.cache.rate-ttl-seconds` | `300` | 汇率缓存 TTL |
| `nexusflow.cache.currency-ttl-seconds` | `3600` | 币种缓存 TTL |

### 8.5 持久化配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `nexusflow.execution.persistence` | `jpa` | 执行层持久化方式（jpa/memory） |
| `nexusflow.idempotency.store` | `memory` | 幂等存储方式（memory/redis） |

### 8.6 限流配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `nexusflow.api.rate-limit.per-minute` | `120` | 每 IP 每分钟请求数 |

### 8.7 地址池配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `ADDRESS_POOL_SEED_MNEMONIC` | - | 地址池种子助记词 |
| `ORPHAN_AUTO_COMPENSATION_ENABLED` | `false` | 是否启用孤儿交易自动补偿 |

---

## 9. 技术架构

### 9.1 技术栈

| 类别 | 技术 |
|------|------|
| 运行时 | Java 17 |
| 框架 | Spring Boot 3.3.5 |
| 架构风格 | DDD 模块化单体、事件驱动架构 |
| 持久层 | PostgreSQL + Flyway |
| 缓存 | Redis |
| 消息队列 | Kafka（可选） |
| 区块链 | web3j（ETH）、tron4j（TRON）、bitcoinj（BTC） |
| 对象映射 | MapStruct |
| 代码简化 | Lombok |
| 测试 | JUnit 5、Mockito、AssertJ |
| 可观测性 | OpenTelemetry、Prometheus、SLF4J |

### 9.2 模块结构

```
nexusflow/
├── flow-common/          # 共享工具：加密、统一响应、错误码
├── flow-domain/          # 领域核心：聚合根、状态机、值对象、端口
├── flow-application/     # 用例编排：支付编排、退款、Webhook
├── flow-infra/           # 基础设施：适配器、持久化、事件发布
├── flow-listener/        # 区块扫描、索引、对账
├── flow-wallet/          # 钱包服务、地址派生
├── flow-api/             # REST API、收银台入口、Flyway 迁移
├── flow-cashier/         # 收银台静态资源
├── flow-permission/      # 权限服务（独立模块）
└── frontend/             # 前端工程
```

### 9.3 分层架构

```
API层 (flow-api)
    ↓
应用层 (flow-application)
    ↓
领域层 (flow-domain)
    ↓
基础设施层 (flow-infra)
```

**约束**：
- 领域层不依赖基础设施
- 业务逻辑禁止在 Controller 层
- 所有状态变更必须事件驱动
- 所有入站接口必须幂等

### 9.4 部署架构

```yaml
services:
  flow-api:
    ports: ["8080:8080"]
    depends_on: [postgresql, redis]
  
  flow-listener:
    depends_on: [postgresql, redis]
  
  flow-wallet:
    depends_on: [postgresql]
  
  postgresql:
    image: postgres:15
    ports: ["5432:5432"]
  
  redis:
    image: redis:7
    ports: ["6379:6379"]
  
  kafka:  # 可选
    image: confluentinc/cp-kafka:7.x
    ports: ["9092:9092"]
```

---

## 10. 测试需求

### 10.1 单元测试

- **覆盖率目标**：> 80%
- **测试框架**：JUnit 5、Mockito、AssertJ
- **测试对象**：
  - 状态机转换
  - 领域模型业务逻辑
  - 应用服务编排逻辑
  - 适配器请求/响应映射

### 10.2 集成测试

- **测试框架**：Testcontainers
- **测试对象**：
  - Spring 上下文加载
  - Flyway 迁移
  - JPA Repository CRUD
  - HTTP API 契约
  - 幂等性验证
  - 并发地址池分配

### 10.3 端到端测试

- **测试对象**：
  - 完整支付流程（创建 → 支付 → 确认）
  - 完整退款流程（申请 → 处理 → 完成）
  - Webhook 投递和重试
  - 通道回调处理

### 10.4 Live 测试（Opt-in）

- **触发条件**：设置环境变量
- **测试对象**：
  - 真实链节点连接（LIVE_ETH_RPC_URL、LIVE_BTC_RPC_URL、LIVE_TRON_NODE_URL）
  - 真实 Redis（LIVE_REDIS_HOST）
  - 真实 Kafka（LIVE_KAFKA_BOOTSTRAP_SERVERS）
  - 真实通道（LIVE_COINBASE_COMMERCE_API_KEY）
  - 真实 Webhook（LIVE_WEBHOOK_URL）

---

## 11. 部署与运维

### 11.1 环境要求

| 环境 | 要求 |
|------|------|
| JDK | 17+ |
| PostgreSQL | 15+ |
| Redis | 7+（可选） |
| Kafka | 3.x+（可选） |
| Docker | 20.10+（集成测试） |

### 11.2 必需环境变量

| 变量 | 说明 |
|------|------|
| `DB_PASSWORD` | 数据库密码 |
| `ENCRYPTION_KEY` | AES-256-GCM 加密密钥 |
| `TRON_NODE_URL` | Tron 节点 URL |

### 11.3 可选环境变量

| 变量 | 说明 |
|------|------|
| `ADDRESS_POOL_SEED_MNEMONIC` | 地址池种子助记词 |
| `COINBASE_COMMERCE_API_KEY` | Coinbase Commerce API Key |
| `LIVE_ETH_RPC_URL` | Ethereum RPC URL |
| `LIVE_BTC_RPC_URL` | Bitcoin RPC URL |
| `LIVE_REDIS_HOST` | Redis 主机 |
| `LIVE_KAFKA_BOOTSTRAP_SERVERS` | Kafka 地址 |

### 11.4 启动命令

```bash
# 编译
mvn -DskipTests compile

# 运行测试
mvn test

# 启动服务
mvn -pl flow-api spring-boot:run

# Docker 启动
docker-compose up -d
```

---

## 12. 路线图与优先级

### 12.1 已完成（代码已落地）

| 阶段 | 任务数 | 核心内容 | 状态 |
|------|--------|----------|------|
| P0 | 10 | 编排引擎核心 | ✅ |
| P0-S | 6 | 安全修复 | ✅ |
| P1 | 10 | 生产加固 | ✅ |
| P1-F | 6 | 缺陷修复 | ✅ |
| P1-R | 6 | 剩余 P1 | ✅ |
| P2 | 4 | 扩展功能 | ✅ |
| P3 | 3 | 前端静态页面 | ✅ |

### 12.2 进行中/待完成

| 优先级 | 功能 | 状态 | 说明 |
|--------|------|------|------|
| R-16 | 商户体系 | 🟡 M1-M3 已落地 | M4-M7 待完成 |
| R-17 | 产品级前端 | ⬜ | 需按四端架构重建 |
| R-18 | RBAC 权限服务 | 🟡 基础框架就绪 | 业务接口接入待完成 |
| P2 | MPC 钱包 | 🟡 端口就绪 | Provider 集成待完成 |
| P2 | Gas Abstraction | 🟡 核心就绪 | Live oracle 待完成 |
| P2 | On/Off Ramp | 🟡 核心就绪 | 官方适配待完成 |

### 12.3 生产前关键缺口

| 风险项 | 当前状态 | 后续动作 |
|--------|----------|----------|
| 真实链节点验证 | 🟡 离线测试覆盖 | 需真实节点验证 |
| Docker 集成测试 | 🟡 代码就绪 | 需 Docker 环境实跑 |
| 通道适配器 | ⬜ BitMart/Binance 为 stub | 需实现真实适配器 |
| 商户级认证 | 🟡 M1-M3 已落地 | 需完成 M4-M7 |
| 产品级前端 | ⬜ 静态页面 | 需按四端架构重建 |
| Redis/Kafka 集成 | 🟡 代码就绪 | 需生产环境验证 |
| MPC/Gas/Ramp | 🟡 核心端口就绪 | 需 Provider 集成 |

---

## 13. 附录

### 13.1 术语表

| 术语 | 说明 |
|------|------|
| 执行层 | 负责链上交易执行、钱包管理、区块链交互 |
| 编排层 | 负责商户订单管理、通道路由、退款处理 |
| 通道 | 收单通道，如 BitMart、Binance、Coinbase Commerce |
| 地址池 | 预生成的收款地址集合 |
| 孤儿交易 | 扫描到但无对应 PENDING 支付的交易 |
| Dead Letter | 投递失败的 Webhook 记录 |
| Reorg | 区块链重组 |
| Fiat Ramp | 法币-加密货币兑换 |

### 13.2 相关文档

- `nexusflow.md`：项目核心设计文档
- `nexusflow-roadmap.md`：项目路线图
- `nexusflow-merchant-design.md`：商户体系设计
- `nexusflow-frontend-design.md`：前端产品设计
- `AGENTS.md`：开发规范
- `TESTING.md`：测试指南

---

## 14. 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1.0 | 2026-06-07 | 初始版本 |
| v2.0 | 2026-07-03 | 详细需求分解，增加接口规范、数据库设计、状态机定义等 |
