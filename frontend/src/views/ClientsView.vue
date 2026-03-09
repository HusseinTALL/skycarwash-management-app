<template>
  <div class="p-4 space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-bold">Clients</h2>
      <RouterLink to="/clients/new" class="btn-primary py-2 px-4 text-sm">+ Nouveau</RouterLink>
    </div>

    <!-- Placeholder – implemented in S5–S6 -->
    <div class="card text-center py-12 text-slate-400">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto mb-3 opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
      </svg>
      <p class="font-medium">Abonnements & Clients</p>
      <p class="text-sm mt-1">Disponible en S5–S6</p>
    </div>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router'
</script>
