# @nexusflow/checkout-proto

收银台设计原型（Checkout design prototype）— 多步支付流程的交互与视觉探索工程，全部数据为 mock（`src/composables/usePayment.ts`），不调用后端，也不参与生产构建和 jar 打包。

属于 `frontend/` npm workspace 的一个成员，与其它端共享同一套 Vue/Vite/TS 工具链。

## 命令（在 `frontend/` 根目录执行）

```bash
npm run dev:proto        # 启动 dev server（http://localhost:5177）
npm run build:proto      # 类型检查 + 独立打包到 dist-app/
npm run typecheck:proto  # 仅类型检查
```

## 结构

- `views/PaymentFlow.vue` — 四步流程壳（支付方式 → 选择网络 → 支付详情 → 成功）
- `views/HomePage.vue` — 演示入口页
- `components/layout/` — 步骤头、订单侧栏
- `components/payment/` — 金额卡、地址行、二维码区
- `assets/styles/variables.css` — 设计令牌（暗色优先，含亮色主题）
