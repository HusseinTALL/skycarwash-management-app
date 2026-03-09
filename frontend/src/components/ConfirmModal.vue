<template>
  <Teleport to="body">
    <div
      class="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/70"
      @click.self="$emit('cancel')"
    >
      <div ref="trapRef" role="dialog" aria-modal="true" :aria-labelledby="`confirm-title-${uid}`"
           class="bg-slate-800 rounded-t-2xl sm:rounded-2xl w-full max-w-md p-6 space-y-4">
        <h3 :id="`confirm-title-${uid}`" class="font-semibold text-lg text-center">{{ title }}</h3>
        <p v-if="message" class="text-slate-400 text-sm text-center">{{ message }}</p>
        <div class="flex gap-3 pt-2">
          <button @click="$emit('cancel')" class="btn-secondary flex-1">Annuler</button>
          <button
            @click="$emit('confirm')"
            class="flex-1 py-3 rounded-xl font-medium transition-colors"
            :class="confirmClass"
          >
            {{ confirmLabel }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'
import { useFocusTrap } from '@/composables/useFocusTrap'

defineProps({
  title:        { type: String, required: true },
  message:      { type: String, default: '' },
  confirmLabel: { type: String, default: 'Confirmer' },
  confirmClass: { type: String, default: 'bg-red-600 hover:bg-red-700 text-white' }
})

defineEmits(['confirm', 'cancel'])

const uid     = Math.random().toString(36).slice(2, 7)
const trapRef = ref(null)

useFocusTrap(trapRef)
</script>
