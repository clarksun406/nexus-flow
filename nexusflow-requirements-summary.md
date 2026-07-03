# NexusFlow 需求文档总结

## 1. 项目概述

### 1.1 项目定位
NexusFlow 是 Nexus 生态中的**多链数字资产支付引擎**，采用执行层 + 编排层一体化架构设计。

- **执行层（Data Plane）**：负责链上交易执行、钱包抽象、交易状态跟踪（ETH/TRON/BTC）
- **编排层（Orchestration）**：负责商户法币/数币订单编排，对接收单通道（交易所/支付商/自建节点）

### 1.2 核心价值
- 为商户提供加密货币支付处理能力
- 支持多链、多币种的支付执行
- 提供收银台、商户控制台、运营监控等完整产品体验
- 实现链上执行与业务编排的解耦

---

## 2. 核心功能需求

### 2.1 执行层功能

#### 2.1.1 加密支付执行
- 生成支付指令（地址/memo/金额）
- 处理链上交易检测与验证
- 支持多链适配器：Ethereum (ETH/ERC20)、Tron (TRC20)、Bitcoin (UTXO)

#### 2.1.2 钱包管理
- 热/冷钱包抽象
- HD钱包地址派生 (BIP39/BIP44)
- 私钥加密存储 (AES-256-GCM)
- 支持外部KMS集成

#### 2.1.3 交易生命周期
```
CREATED → PENDING → DETECTED → CONFIRMING → CONFIRMED
                                          → FAILED
                  → EXPIRED
```

#### 2.1.4 区块链集成
- 区块扫描与事件检测
- 交易确认数跟踪
- 链重组(reorg)检测与回滚处理
- 缺失事件补偿(orphan transactions)

### 2.2 编排层功能

#### 2.2.1 支付订单管理
- 商户订单创建与路由
- 多通道智能路由（按汇率排序）
- 订单状态跟踪与过期处理

#### 2.2.2 通道适配
- ChannelAdapter端口抽象
- 支持通道：BitMart、Binance、Coinbase Commerce、自建节点
- 通道健康监控与故障转移

#### 2.2.3 退款管理
- 退款订单创建与状态跟踪
- 通道退款委托
- 退款失败重试机制

#### 2.2.4 收银台服务
- 买家支付页面（地址/二维码展示）
- 支付状态轮询与更新
- 倒计时与过期处理

### 2.3 事件驱动架构
- 域事件发布：`crypto.payment.detected`、`crypto.payment.confirmed`、`crypto.payment.failed`
- 支持Kafka事件总线
- Webhook回调通知商户

---

## 3. 商户体系需求

### 3.1 商户身份管理
- **商户主数据**：`merchant_profiles`表
  - 内部UUID + 外部merchant_code映射
  - 商户状态：PENDING/ACTIVE/SUSPENDED/CLOSED
  - 风险等级：LOW/MEDIUM/HIGH

### 3.2 商户用户体系
- **用户表**：`merchant_users`（登录邮箱、密码hash、状态）
- **成员关系**：`merchant_user_memberships`（用户-商户多对多）
- **角色模板**：OWNER/DEVELOPER/FINANCE/SUPPORT

### 3.3 商户级API认证
- **API Key管理**：`merchant_api_keys`表
  - 前缀展示 + hash存储
  - 环境隔离：TEST/LIVE
  - 权限范围：scopes（payment:create、refund:create等）
  - IP白名单、过期时间、使用记录
- **认证流程**：X-API-Key → 解析商户 → 设置request attributes → 权限校验

### 3.4 Webhook配置
- **配置表**：`merchant_webhook_configs`
  - HTTPS回调地址、加密secret
  - 事件订阅、重试策略
  - 投递健康监控
- **审计日志**：`merchant_audit_logs`

### 3.5 数据隔离
- 商户API请求强制绑定商户ID
- 订单/退款/fiat ramp按商户隔离查询
- 运营/Admin跨商户需内部权限

---

## 4. 前端产品需求

### 4.1 四端分离架构

| 端 | 用户 | 核心职责 |
|----|------|----------|
| **买家收银台** | 付款用户 | 订单展示、币种选择、支付地址/二维码、状态跟踪 |
| **商户门户** | 商户管理员/开发者/财务 | 订单管理、退款、API Key、Webhook、团队、报表 |
| **运营控制台** | 内部运营/风控/技术支持 | 通道监控、异常处理、对账、风险处置 |
| **平台管理端** | 超级管理员 | 商户生命周期、权限管理、系统配置、审计 |

### 4.2 收银台功能
- 订单摘要：商户名、订单号、金额、币种、网络、过期时间
- 支付动作：二维码、地址复制、金额复制、钱包跳转
- 状态体验：待支付、确认中、已完成、已过期、失败
- 安全与品牌：商户品牌展示、checkout token校验

### 4.3 商户门户功能
- **Dashboard**：订单量、成功率、待处理退款、Webhook失败
- **订单管理**：列表、筛选、详情、支付时间线、链上交易
- **退款管理**：发起退款、状态跟踪、失败重试
- **开发者中心**：API Key管理、Webhook配置、签名文档、测试回调
- **团队管理**：成员、角色、邀请、禁用
- **报表中心**：订单导出、对账报表、fiat ramp订单

### 4.4 运营控制台功能
- **全局Dashboard**：通道健康、订单状态、异常趋势
- **通道管理**：provider健康、费率、失败率、配置
- **风险管理**：orphan transaction查询、resolve/ignore/compensate
- **Webhook DLQ**：dead letter查询、replay、ignore
- **对账管理**：对账backlog、retry、reorg处理
- **支持视图**：按merchant/payment/txHash快速定位

### 4.5 平台管理功能
- **商户管理**：创建、审核、冻结、资料维护
- **凭证管理**：API Key、Webhook secret、IP白名单
- **RBAC管理**：权限点、角色模板、用户授权
- **Provider配置**：Coinbase、BitMart、Binance等状态
- **系统管理**：环境配置、feature flag、审计日志

### 4.6 前端工程架构
```text
frontend/
  package.json
  apps/
    checkout/          # 买家收银台
    merchant/          # 商户门户
    ops/               # 运营控制台
    admin/             # 平台管理端
  packages/
    ui/                # 组件库
    api-client/        # API客户端
    auth/              # 认证与权限
    config/            # 配置管理
```

**技术栈**：React + TypeScript + Vite

---

## 5. 技术架构需求

### 5.1 技术栈
- **运行时**：Java 17、Spring Boot 3.3.5
- **架构风格**：DDD模块化单体、事件驱动架构
- **持久层**：PostgreSQL（主库）、Redis（缓存/幂等/限流）
- **消息队列**：Kafka（可选）、Spring Application Events
- **可观测性**：OpenTelemetry（tracing）、Prometheus（metrics）、结构化日志（JSON）

### 5.2 分层架构
```
API层 (flow-api) → 应用层 (flow-application) → 领域层 (flow-domain) → 基础设施层 (flow-infra)
```

**核心约束**：
- 领域层不依赖基础设施
- 业务逻辑禁止在Controller层
- 所有状态变更必须事件驱动
- 所有入站接口必须幂等

### 5.3 模块结构
| 模块 | 职责 |
|------|------|
| `flow-common` | 共享工具：加密、统一响应、错误码 |
| `flow-domain` | 领域核心：聚合根、状态机、值对象、端口 |
| `flow-application` | 用例编排：支付编排、退款、Webhook |
| `flow-infra` | 基础设施：适配器、持久化、事件发布 |
| `flow-listener` | 区块扫描、索引、对账 |
| `flow-wallet` | 钱包服务、地址派生 |
| `flow-api` | REST API、收银台入口、Flyway迁移 |
| `flow-cashier` | 收银台静态资源 |
| `flow-permission` | 权限服务（独立模块） |

### 5.4 数据库设计
- **执行层表**：crypto_payments、wallets、address_pool、mnemonic_backups、chain_scan_cursors、orphan_transactions
- **编排层表**：payment_orders、payment_flows、refund_orders
- **商户体系表**：merchant_profiles、merchant_credentials、merchant_webhook_configs、merchant_audit_logs
- **支撑表**：idempotency_keys、webhook_dead_letters、fiat_ramp_orders

---

## 6. 安全需求

### 6.1 数据安全
- 私钥不得明文存储，支持外部KMS（AWS KMS / Vault）
- 敏感数据静态加密（AES-256-GCM）
- 密钥仅通过环境变量/KMS注入
- 加密密钥未配置时生产环境拒绝启动

### 6.2 API安全
- 商户级API Key认证（X-API-Key）
- 回调签名验证（HMAC-SHA256）
- SSRF防护（拒绝非HTTPS和私有IP）
- 限流保护（每IP每分钟120次）
- 幂等性保证（orderId/eventId去重）

### 6.3 访问控制
- 数据隔离：商户只能访问自己的数据
- RBAC权限控制（待实现）
- 审计日志：关键操作必须记录
- 私钥解密审计日志

---

## 7. 路线图与优先级

### 7.1 已完成（代码已落地）

| 阶段 | 任务数 | 核心内容 |
|------|--------|----------|
| P0 | 10 | 编排引擎核心：订单/退款/通道领域模型、编排服务、JPA持久化、商户API |
| P0-S | 6 | 安全修复：JPA正确性、API认证、回调签名、加密密钥检查 |
| P1 | 10 | 生产加固：乐观锁、状态机修复、Webhook改进、限流、集成测试 |
| P1-F | 6 | 缺陷修复：尘埃攻击防护、校验修复、测试补全 |
| P1-R | 6 | 剩余P1：Binance适配器、动态路由、Redis缓存、过期调度 |
| P2 | 4 | 扩展：数币版、自建节点通道、Coinbase适配器、Kafka事件总线 |
| P3 | 3 | 前端：收银台、商户端、运营端静态页面 |

### 7.2 进行中/待完成

| 优先级 | 功能 | 状态 |
|--------|------|------|
| P2 | MPC钱包集成 | 🟡 端口和HTTP signer已实现，Fireblocks/Copper适配待完成 |
| P2 | Gas Abstraction | 🟡 静态估算已实现，live oracle和GasBank待完成 |
| P2 | On/Off Ramp | 🟡 通用HTTP gateway已实现，MoonPay/Ramp/Banxa官方适配待完成 |
| R-16 | 商户体系 | 🟡 M1-M3已落地，M4-M7待完成 |
| R-17 | 产品级前端 | ⬜ 需按四端架构重建 |
| R-18 | RBAC权限服务 | 🟡 基础框架已就绪，业务接口接入待完成 |

### 7.3 生产前关键缺口

| 风险项 | 当前状态 | 后续动作 |
|--------|----------|----------|
| 真实链节点验证 | 🟡 离线测试覆盖 | 需真实ETH/BTC/TRON节点验证 |
| Docker集成测试 | 🟡 代码已就绪 | 需Docker环境实跑 |
| 通道适配器 | ⬜ BitMart/Binance为stub | 需实现真实REST适配器 |
| 商户级认证 | 🟡 M1-M3已落地 | 需完成M4-M7 |
| 产品级前端 | ⬜ 静态页面 | 需按四端架构重建 |
| Redis/Kafka集成 | 🟡 代码就绪 | 需生产环境验证 |
| MPC/Gas/Ramp | 🟡 核心端口就绪 | 需provider集成和live验证 |

---

## 8. 实施建议

### 8.1 优先级排序
1. **商户体系完善**（R-16 M4-M7）：Webhook持久化、Merchant Portal会话、Ops/Admin RBAC
2. **真实通道适配**：Coinbase Commerce live验证、BitMart/Binance真实适配器
3. **前端产品化**：按四端架构重建前端工程
4. **生产环境验证**：Docker集成测试、真实链节点、Redis/Kafka
5. **高级功能**：MPC、Gas Abstraction、On/Off Ramp provider集成

### 8.2 技术债务
- 全局`nexusflow.api.key`需在生产禁用
- `merchant_id`类型统一（UUID vs external code）
- 前端从静态HTML迁移到React工程
- 补充E2E测试覆盖

### 8.3 部署架构建议
```
pay.example.com/checkout/:token    # 买家收银台
merchant.example.com               # 商户门户
ops.example.com                    # 运营控制台
admin.example.com                  # 平台管理端
```

---

## 9. 总结

NexusFlow 是一个功能完备的多链数字资产支付引擎，核心代码已基本落地。当前主要差距在于：

1. **生产环境验证**：真实节点、通道、中间件的live验证
2. **商户体系完善**：多租户认证、数据隔离、RBAC
3. **产品级前端**：从静态页面迁移到四端React工程
4. **高级功能集成**：MPC、Gas Abstraction、On/Off Ramp provider

项目采用DDD架构，代码质量良好，测试覆盖较完整。建议按优先级逐步推进生产就绪度验证和产品化改造。
