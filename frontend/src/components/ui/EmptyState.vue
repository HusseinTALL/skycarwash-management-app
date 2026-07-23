<template>
  <div class="flex flex-col items-center justify-center text-center py-10 px-4">
    <span class="grid place-items-center h-14 w-14 rounded-2xl bg-slate-800/70 text-slate-500 mb-3">
      <i :class="icon" class="text-2xl" />
    </span>
    <p class="text-slate-300 font-medium">{{ text }}</p>
    <p v-if="hint" class="text-sm text-slate-500 mt-1 max-w-xs">{{ hint }}</p>
    <div v-if="$slots.action" class="mt-4">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  icon: { type: String, default: 'pi pi-inbox' },
  text: { type: String, default: 'Aucune donnée' },
  hint: { type: String, default: '' }
})
</script>
