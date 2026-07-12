<template>
  <div class="shell">
    <header class="shell__header">
      <div>
        <p class="shell__eyebrow">{{ eyebrow }}</p>
        <h1>{{ title }}</h1>
        <p class="shell__description">{{ description }}</p>
      </div>
      <slot name="actions" />
    </header>
    <main class="shell__body">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow: string;
  title: string;
  description: string;
}>();
</script>

<style scoped>
.shell {
  min-height: 100vh;
  padding: 32px;
  background:
    radial-gradient(circle at top right, rgba(56, 189, 248, 0.14), transparent 30%),
    linear-gradient(180deg, #f7fafc 0%, #eef2f7 100%);
  color: #17202a;
  font-family: "Segoe UI", sans-serif;
}

.shell__header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: start;
  margin: 0 auto 24px;
  max-width: 1180px;
}

.shell__eyebrow {
  margin: 0 0 8px;
  color: #617080;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

h1 {
  margin: 0;
  font-size: 32px;
}

.shell__description {
  margin: 10px 0 0;
  max-width: 720px;
  color: #617080;
}

.shell__body {
  max-width: 1180px;
  margin: 0 auto;
}

@media (max-width: 800px) {
  .shell {
    padding: 20px;
  }

  .shell__header {
    flex-direction: column;
  }

  h1 {
    font-size: 26px;
  }
}
</style>
