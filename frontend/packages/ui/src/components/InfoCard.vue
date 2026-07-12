<template>
  <section class="card">
    <p class="card__label">{{ label }}</p>
    <h2>{{ title }}</h2>
    <p class="card__body">{{ body }}</p>
    <slot />
  </section>
</template>

<script setup lang="ts">
defineProps<{
  label: string;
  title: string;
  body: string;
}>();
</script>

<style scoped>
.card {
  border: 1px solid #dce3ea;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 14px 34px rgba(23, 32, 42, 0.07);
  padding: 22px;
}

.card__label {
  margin: 0 0 10px;
  color: #617080;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

h2 {
  margin: 0;
  font-size: 20px;
}

.card__body {
  margin: 10px 0 0;
  color: #617080;
}
</style>
