<template>
  <button
    @click="$emit('select', service)"
    class="card text-left transition-all duration-150 active:scale-95"
    :class="selected
      ? 'ring-2 ring-brand-500 bg-slate-700'
      : 'hover:bg-slate-700'"
  >
    <div class="flex items-start justify-between gap-2">
      <span class="font-semibold text-sm leading-snug">{{ service.name }}</span>
      <span
        v-if="service.category"
        class="text-xs bg-slate-600 text-slate-300 rounded-full px-2 py-0.5 shrink-0"
      >{{ service.category }}</span>
    </div>
    <p class="mt-2 text-brand-400 font-bold text-lg">{{ formatPrice(service.price) }}</p>
  </button>
</template>

<script setup>
defineProps({
  service:  { type: Object,  required: true },
  selected: { type: Boolean, default: false }
})
defineEmits(['select'])

function formatPrice(fcfa) {
  return new Intl.NumberFormat('fr-FR').format(fcfa) + ' FCFA'
}
</script>
