<template>
  <div class="min-h-screen flex flex-col md:flex-row bg-slate-900">

    <!-- ── Desktop sidebar ────────────────────────────────────────────── -->
    <aside class="hidden md:flex md:flex-col md:w-56 md:fixed md:inset-y-0 md:border-r md:border-slate-700 md:bg-slate-800/80 md:backdrop-blur-sm z-10">
      <!-- Logo -->
      <div class="px-5 py-5 border-b border-slate-700">
        <span class="font-bold text-brand-400 text-lg tracking-tight">SkyCarWash</span>
        <p class="text-xs text-slate-500 mt-0.5">{{ auth.user?.name }}</p>
      </div>

      <!-- Nav links -->
      <nav class="flex-1 px-3 py-4 space-y-1 overflow-y-auto">

        <RouterLink
          v-if="auth.isEmployee || auth.isManager"
          to="/caisse"
          class="sidebar-item"
          active-class="sidebar-item-active"
        >
          <svg class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
          </svg>
          <span>Caisse</span>
        </RouterLink>

        <RouterLink
          v-if="auth.isManager || auth.isPartner"
          to="/dashboard"
          class="sidebar-item"
          active-class="sidebar-item-active"
        >
          <svg class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
          </svg>
          <span>Dashboard</span>
        </RouterLink>

        <RouterLink
          v-if="auth.isManager"
          to="/clients"
          class="sidebar-item"
          active-class="sidebar-item-active"
        >
          <svg class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <span>Clients</span>
        </RouterLink>

        <RouterLink
          v-if="auth.isManager"
          to="/stock"
          class="sidebar-item relative"
          active-class="sidebar-item-active"
        >
          <div class="relative shrink-0">
            <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10" />
            </svg>
            <span
              v-if="stock.lowStockCount > 0"
              class="absolute -top-1.5 -right-1.5 bg-red-500 text-white text-xs font-bold rounded-full min-w-[16px] h-4 flex items-center justify-center px-0.5 leading-none"
            >{{ stock.lowStockCount > 9 ? '9+' : stock.lowStockCount }}</span>
          </div>
          <span>Stock</span>
        </RouterLink>

        <RouterLink
          v-if="auth.isManager"
          to="/settings"
          class="sidebar-item"
          active-class="sidebar-item-active"
        >
          <svg class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <span>Réglages</span>
        </RouterLink>

      </nav>

      <!-- Logout -->
      <div class="px-3 py-4 border-t border-slate-700">
        <button
          @click="handleLogout"
          class="sidebar-item w-full text-red-400 hover:text-red-300 hover:bg-red-500/10"
        >
          <svg class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
          </svg>
          <span>Déconnexion</span>
        </button>
      </div>
    </aside>

    <!-- ── Mobile top bar ─────────────────────────────────────────────── -->
    <header class="md:hidden sticky top-0 z-10 bg-slate-800 border-b border-slate-700 px-4 py-3 flex items-center justify-between">
      <span class="font-bold text-brand-400 text-lg tracking-tight">SkyCarWash</span>
      <div class="flex items-center gap-3">
        <span class="text-xs text-slate-400">{{ auth.user?.name }}</span>
        <button @click="handleLogout" class="text-slate-400 hover:text-red-400 transition-colors p-1">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
          </svg>
        </button>
      </div>
    </header>

    <!-- ── Main content ───────────────────────────────────────────────── -->
    <main class="flex-1 md:ml-56 overflow-y-auto pb-20 md:pb-0">
      <div class="max-w-6xl mx-auto w-full">
        <RouterView />
      </div>
    </main>

    <!-- ── Mobile bottom nav ──────────────────────────────────────────── -->
    <AppNav class="md:hidden" />

  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useStockStore } from '@/stores/stock'
import AppNav from './AppNav.vue'

const auth   = useAuthStore()
const stock  = useStockStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.sidebar-item {
  @apply flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-slate-400
         hover:text-slate-100 hover:bg-slate-700/60 transition-colors duration-150 w-full;
}
.sidebar-item-active {
  @apply text-brand-400 bg-brand-500/10 hover:bg-brand-500/15 hover:text-brand-400;
}
</style>
