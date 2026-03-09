<template>
  <div class="min-h-screen flex flex-col bg-slate-900">
    <!-- Top bar (mobile) -->
    <header class="sticky top-0 z-10 bg-slate-800 border-b border-slate-700 px-4 py-3 flex items-center justify-between">
      <span class="font-bold text-brand-400 text-lg tracking-tight">SkyCarWash</span>
      <div class="flex items-center gap-3">
        <span class="text-xs text-slate-400">{{ auth.user?.name }}</span>
        <button @click="handleLogout" class="text-slate-400 hover:text-red-400 transition-colors min-h-0 min-w-0 p-1">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
          </svg>
        </button>
      </div>
    </header>

    <!-- Main content -->
    <main class="flex-1 overflow-y-auto pb-24">
      <RouterView />
    </main>

    <!-- Bottom navigation -->
    <AppNav />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppNav from './AppNav.vue'

const auth = useAuthStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>
