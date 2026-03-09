<template>
  <div class="p-4 space-y-4">
    <div class="flex items-center gap-3">
      <RouterLink to="/clients" class="text-slate-400 hover:text-slate-200">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </RouterLink>
      <h2 class="text-xl font-bold">Nouveau client</h2>
    </div>
    <div class="card text-center py-12 text-slate-400">
      <p class="text-sm">Disponible en S5–S6</p>
    </div>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router'
</script>
