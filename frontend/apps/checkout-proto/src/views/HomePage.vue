<script setup lang="ts">
const pages = [
  { path: '/payment', title: '启动交互式演示', desc: '完整的支付流程体验', featured: true },
  { path: '/payment?step=1', title: '支付方式选择', desc: '地址转账 / 链接钱包' },
  { path: '/payment?step=2', title: '网络选择', desc: '多网络列表与确认' },
  { path: '/payment?step=3', title: '支付详情', desc: '金额、地址、二维码' },
  { path: '/payment?step=4', title: '支付成功', desc: '交易完成确认' },
]
</script>

<template>
  <div class="home aurora-bg">
    <div class="container">
      <header class="header">
        <div class="logo">
          <div class="logo-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2L2 7l10 5 10-5-10-5z" />
              <path d="M2 17l10 5 10-5" />
              <path d="M2 12l10 5 10-5" />
            </svg>
          </div>
          <span class="logo-text">Nexus Flow</span>
        </div>
        <span class="badge">演示系统</span>
      </header>

      <main class="main">
        <div class="hero">
          <h1 class="title">
            <span class="title-accent">加密支付</span>
            <br />
            流程演示
          </h1>
          <p class="subtitle">
            基于 Vue 3 + TypeScript 构建的现代化支付流程
          </p>
        </div>

        <div class="grid">
          <router-link
            v-for="page in pages"
            :key="page.path"
            :to="page.path"
            class="card"
            :class="{ featured: page.featured }"
          >
            <div class="card-content">
              <h3 class="card-title">{{ page.title }}</h3>
              <p class="card-desc">{{ page.desc }}</p>
            </div>
            <svg class="card-arrow" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </router-link>
        </div>
      </main>

      <footer class="footer">
        <span>© 2024 Nexus Flow</span>
        <span class="footer-links">
          <a href="#">文档</a>
          <a href="#">GitHub</a>
        </span>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  transition: background 0.3s ease;
}

.container {
  width: 100%;
  max-width: 640px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 60px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--gradient-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 10px 22px -10px var(--color-brand-glow);
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.badge {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-dim);
  background: var(--bg-elevated);
  padding: 6px 12px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-subtle);
}

.hero {
  margin-bottom: 48px;
}

.title {
  font-size: 48px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1.1;
  letter-spacing: -2px;
  margin-bottom: 16px;
}

.title-accent {
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  font-size: 16px;
  color: var(--text-muted);
  line-height: 1.6;
}

.grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  transition: all 0.2s ease;
}

.card:hover {
  background: var(--bg-card-hover);
  border-color: var(--border-default);
  transform: translateX(4px);
}

.card.featured {
  background: var(--gradient-brand);
  border-color: transparent;
  box-shadow: 0 16px 40px -16px var(--color-brand-glow);
}

.card.featured:hover {
  transform: translateX(4px);
  box-shadow: 0 8px 32px var(--color-brand-glow);
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.card.featured .card-title {
  color: white;
}

.card-desc {
  font-size: 13px;
  color: var(--text-muted);
}

.card.featured .card-desc {
  color: rgba(255, 255, 255, 0.7);
}

.card-arrow {
  color: var(--text-dim);
  transition: all 0.2s ease;
}

.card:hover .card-arrow {
  color: var(--text-primary);
  transform: translateX(4px);
}

.card.featured .card-arrow {
  color: rgba(255, 255, 255, 0.7);
}

.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 60px;
  padding-top: 20px;
  border-top: 1px solid var(--border-subtle);
  font-size: 13px;
  color: var(--text-dim);
}

.footer-links {
  display: flex;
  gap: 16px;
}

.footer-links a {
  color: var(--text-muted);
  transition: color 0.15s ease;
}

.footer-links a:hover {
  color: var(--text-primary);
}
</style>
