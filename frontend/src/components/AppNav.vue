<template>
  <nav class="fixed bottom-0 inset-x-0 bg-slate-800 border-t border-slate-700 flex justify-around items-center h-16 z-10 safe-area-bottom">

    <!-- Caisse – employee + manager -->
    <RouterLink
      v-if="auth.isEmployee || auth.isManager"
      to="/caisse"
      class="nav-item"
      active-class="nav-item-active"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
      </svg>
      <span class="text-xs mt-0.5">Caisse</span>
    </RouterLink>

    <!-- Rapports d'état – employee + manager -->
    <RouterLink
      v-if="auth.isEmployee || auth.isManager"
      to="/inspections"
      class="nav-item"
      active-class="nav-item-active"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
      </svg>
      <span class="text-xs mt-0.5">Rapports</span>
    </RouterLink>

    <!-- Dashboard – manager + partner -->
    <RouterLink
      v-if="auth.isManager || auth.isPartner"
      to="/dashboard"
      class="nav-item"
      active-class="nav-item-active"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
      </svg>
      <span class="text-xs mt-0.5">Dashboard</span>
    </RouterLink>

    <!-- Transactions history – manager + partner -->
    <RouterLink
      v-if="auth.isManager || auth.isPartner"
      to="/transactions"
      class="nav-item"
      active-class="nav-item-active"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
      </svg>
      <span class="text-xs mt-0.5">Historique</span>
    </RouterLink>

    <!-- Clients – manager -->
    <RouterLink
      v-if="auth.isManager"
      to="/clients"
      class="nav-item"
      active-class="nav-item-active"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
      </svg>
      <span class="text-xs mt-0.5">Clients</span>
    </RouterLink>

    <!-- Stock – manager (with low-stock badge) -->
    <RouterLink
      v-if="auth.isManager"
      to="/stock"
      class="nav-item relative"
      active-class="nav-item-active"
    >
      <div class="relative">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10" />
        </svg>
        <!-- Low-stock alert badge -->
        <span
          v-if="stock.lowStockCount > 0"
          class="absolute -top-1.5 -right-1.5 bg-red-500 text-white text-xs font-bold rounded-full min-w-[16px] h-4 flex items-center justify-center px-0.5 leading-none"
        >{{ stock.lowStockCount > 9 ? '9+' : stock.lowStockCount }}</span>
      </div>
      <span class="text-xs mt-0.5">Stock</span>
    </RouterLink>

    <!-- Expenses – manager -->
    <RouterLink
      v-if="auth.isManager"
      to="/expenses"
      class="nav-item"
      active-class="nav-item-active"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
      </svg>
      <span class="text-xs mt-0.5">Dépenses</span>
    </RouterLink>

    <!-- Settings – manager -->
    <RouterLink
      v-if="auth.isManager"
      to="/settings"
      class="nav-item"
      active-class="nav-item-active"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
      </svg>
      <span class="text-xs mt-0.5">Réglages</span>
    </RouterLink>

  </nav>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useStockStore } from '@/stores/stock'

const auth  = useAuthStore()
const stock = useStockStore()

// Load product list for badge count when manager is authenticated
if (auth.isManager && stock.products.length === 0) {
  stock.loadAll()
}
</script>

<style scoped>
.nav-item {
  @apply flex flex-col items-center justify-center text-slate-400 hover:text-slate-200
         transition-colors duration-150 min-h-0 min-w-0 px-2 py-1 rounded-lg;
}
.nav-item-active {
  @apply text-brand-400;
}
</style>
