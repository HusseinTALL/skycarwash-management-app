<template>
  <ErrorBoundary>
    <RouterView v-slot="{ Component }">
      <Transition name="page" mode="out-in">
        <component :is="Component" />
      </Transition>
    </RouterView>
    <SessionExpiredModal v-if="auth.sessionExpired" />
  </ErrorBoundary>
</template>

<script setup>
import { RouterView } from 'vue-router'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import SessionExpiredModal from '@/components/SessionExpiredModal.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>

<style>
.page-enter-active,
.page-leave-active {
  transition: opacity 0.15s ease;
}
.page-enter-from,
.page-leave-to {
  opacity: 0;
}
</style>
