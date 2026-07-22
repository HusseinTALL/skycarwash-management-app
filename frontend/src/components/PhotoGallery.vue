<template>
  <section class="card space-y-3">
    <h2 class="font-semibold text-slate-200">{{ title }}</h2>
    <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
      <figure v-for="p in photos" :key="p.id" class="space-y-1">
        <button type="button" @click="lightbox = p" class="block w-full">
          <AuthedImage :photo-id="p.id" :variant="variant" wrapper-class="aspect-[4/3] rounded-xl" />
        </button>
        <figcaption class="text-xs text-center text-slate-400">
          {{ zoneLabel(p.zone) }}<span v-if="p.caption"> · {{ p.caption }}</span>
        </figcaption>
      </figure>
    </div>

    <!-- Lightbox -->
    <div v-if="lightbox" class="fixed inset-0 z-50 bg-black/90 flex items-center justify-center p-4" @click="lightbox = null">
      <div class="max-w-3xl w-full">
        <AuthedImage :photo-id="lightbox.id" :variant="variant" fit="contain" wrapper-class="max-h-[80vh] rounded-xl bg-black" />
        <p class="text-center text-slate-300 text-sm mt-2">
          {{ zoneLabel(lightbox.zone) }} · {{ formatTime(lightbox.createdAt) }}
        </p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import AuthedImage from '@/components/AuthedImage.vue'
import { PHOTO_ZONE_LABELS } from '@/constants'

defineProps({
  title:   { type: String, required: true },
  photos:  { type: Array, required: true },
  variant: { type: String, default: 'staff' }
})

const lightbox = ref(null)

function zoneLabel(z) { return PHOTO_ZONE_LABELS[z] || z }
function formatTime(iso) {
  return iso ? new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }) : ''
}
</script>
