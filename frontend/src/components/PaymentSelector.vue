<template>
  <div class="grid grid-cols-2 gap-3">
    <button
      v-for="method in methods"
      :key="method.value"
      @click="$emit('select', method.value)"
      class="flex flex-col items-center justify-center py-4 rounded-2xl border-2 transition-all duration-150 active:scale-95"
      :class="selected === method.value
        ? 'border-brand-500 bg-brand-500/10 text-brand-400'
        : 'border-slate-600 bg-slate-800 text-slate-300 hover:border-slate-500'"
    >
      <span class="text-2xl mb-1">{{ method.icon }}</span>
      <span class="font-semibold text-sm">{{ method.label }}</span>
    </button>
  </div>
</template>

<script setup>
defineProps({
  selected: { type: String, default: null }
})
defineEmits(['select'])

const methods = [
  { value: 'CASH',        label: 'Espèces',      icon: '💵' },
  { value: 'ORANGE',      label: 'Orange Money', icon: '🟠' },
  { value: 'MOOV',        label: 'Moov Money',   icon: '🔵' },
  { value: 'ABONNEMENT',  label: 'Abonnement',   icon: '🎫' }
]
</script>
