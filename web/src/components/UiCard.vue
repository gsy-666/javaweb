<template>
  <section class="card" :class="{ padded }">
    <header v-if="$slots.title || title || $slots.extra" class="hd">
      <div class="title">
        <slot name="title">{{ title }}</slot>
      </div>
      <div class="extra">
        <slot name="extra" />
      </div>
    </header>
    <div class="bd">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{ title?: string; padded?: boolean }>()
</script>

<style scoped>
.card {
  background: var(--sr-panel);
  border: 1px solid var(--sr-border);
  border-radius: 18px;
  box-shadow: var(--sr-shadow);
  backdrop-filter: blur(12px) saturate(150%);
}

.card.padded {
  padding: 16px;
}

.hd {
  padding: 14px 16px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--sr-border);
}

.title {
  font-family: 'Space Grotesk', 'Noto Sans SC', sans-serif;
  font-weight: 900;
  letter-spacing: 0.02em;
}

.extra {
  display: flex;
  gap: 8px;
  align-items: center;
}

.bd {
  padding: 14px 16px;
}
</style>
