<template>
  <ErrorBoundary>
    <RouterView />
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
