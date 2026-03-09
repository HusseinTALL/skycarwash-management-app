<template>
  <slot v-if="!hasError" />
  <div v-else class="min-h-screen flex items-center justify-center bg-slate-900 p-4">
    <div class="bg-slate-800 rounded-xl p-8 max-w-md w-full text-center shadow-xl">
      <div class="text-5xl mb-4">⚠️</div>
      <h2 class="text-xl font-semibold text-white mb-2">Something went wrong</h2>
      <p class="text-slate-400 text-sm mb-6">
        An unexpected error occurred. Please try refreshing the page.
        If the problem persists, contact support.
      </p>
      <p v-if="errorMessage" class="text-xs text-red-400 bg-slate-900 rounded p-3 mb-6 text-left font-mono break-all">
        {{ errorMessage }}
      </p>
      <button
        class="bg-sky-500 hover:bg-sky-600 text-white font-medium px-6 py-2 rounded-lg transition-colors"
        @click="reset"
      >
        Reload page
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'

const hasError = ref(false)
const errorMessage = ref('')

onErrorCaptured((err) => {
  hasError.value = true
  errorMessage.value = err?.message ?? String(err)

  // Forward to global error handler (e.g. Sentry) without re-throwing
  return false
})

function reset() {
  window.location.reload()
}
</script>
