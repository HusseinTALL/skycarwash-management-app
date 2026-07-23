<template>
  <div class="scw-panel p-4 sm:p-5 relative overflow-hidden transition-transform duration-200 hover:-translate-y-0.5">
    <div class="flex items-start justify-between gap-3">
      <div class="min-w-0">
        <p class="scw-eyebrow">{{ title }}</p>

        <Skeleton v-if="loading" width="6rem" height="2rem" class="mt-2" />
        <p v-else class="mt-1.5 text-xl sm:text-2xl font-bold tracking-tight text-slate-100 leading-tight tabular-nums">
          {{ value }}
        </p>

        <div v-if="!loading && (delta !== null && delta !== undefined)" class="mt-1.5 flex items-center gap-1 text-xs">
          <i :class="deltaIcon" class="text-[11px]" :style="{ color: deltaColor }" />
          <span :style="{ color: deltaColor }" class="font-medium">{{ deltaText }}</span>
          <span v-if="deltaLabel" class="text-slate-500">{{ deltaLabel }}</span>
        </div>
        <p v-else-if="!loading && hint" class="mt-1.5 text-xs text-slate-500">{{ hint }}</p>
      </div>

      <span
        class="grid place-items-center h-11 w-11 rounded-xl shrink-0"
        :style="{ background: `color-mix(in srgb, ${accent}, transparent 86%)`, color: accent }"
      >
        <i :class="icon" class="text-lg" />
      </span>
    </div>

    <!-- Decorative accent bar -->
    <span
      class="absolute left-0 top-0 h-full w-1 rounded-r"
      :style="{ background: accent, opacity: 0.55 }"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Skeleton from 'primevue/skeleton'

const props = defineProps({
  title: { type: String, required: true },
  value: { type: [String, Number], default: '' },
  icon: { type: String, default: 'pi pi-chart-bar' },
  accent: { type: String, default: 'var(--p-primary-color)' },
  delta: { type: Number, default: null },
  deltaText: { type: String, default: '' },
  deltaLabel: { type: String, default: '' },
  hint: { type: String, default: '' },
  loading: { type: Boolean, default: false }
})

const deltaColor = computed(() => {
  if (props.delta > 0) return '#34d399'
  if (props.delta < 0) return '#f87171'
  return '#94a3b8'
})
const deltaIcon = computed(() => {
  if (props.delta > 0) return 'pi pi-arrow-up'
  if (props.delta < 0) return 'pi pi-arrow-down'
  return 'pi pi-minus'
})
</script>
