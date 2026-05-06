<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import { role } from './user'

import AppHeader from './components/AppHeader.vue'
import AppSidebar from './components/AppSidebar.vue'

const route = useRoute()
const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')
const showSidebar = computed(() => !isAuthPage.value)
const appTitle = computed(() => (role.value === 'ADMIN' ? '管理端' : role.value === 'WORKER' ? '工人端' : '报修端'))
</script>

<template>
  <div class="app-shell" :data-title="appTitle">

    <div v-if="!isAuthPage" class="top">
      <AppHeader />
    </div>

    <div class="body" :class="{ auth: isAuthPage }">
      <AppSidebar v-if="showSidebar" class="side" />
      <main class="main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  position: relative;
  padding: 20px;
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 14px;
}

.top {
  position: relative;
  z-index: 2;
}

.body {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 14px;
  align-items: start;
}

.body.auth {
  grid-template-columns: 1fr;
}

.main {
  min-width: 0;
}

@media (max-width: 1100px) {
  .app-shell {
    padding: 12px;
  }
  .body {
    grid-template-columns: 1fr;
  }
}
</style>

