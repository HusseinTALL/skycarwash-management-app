<template>
  <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between mb-5">
    <div class="min-w-0">
      <div class="flex items-center gap-2.5">
        <span
          v-if="icon"
          class="grid place-items-center h-9 w-9 rounded-xl bg-primary/10 text-primary shrink-0"
        >
          <i :class="icon" />
        </span>
        <h1 class="text-xl sm:text-2xl font-bold tracking-tight text-slate-100 truncate">
          {{ title }}
        </h1>
      </div>
      <p v-if="subtitle" class="text-sm text-slate-500 mt-1">{{ subtitle }}</p>
    </div>

    <div v-if="$slots.actions" class="flex items-center gap-2 flex-wrap">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  icon: { type: String, default: '' }
})
</script>
