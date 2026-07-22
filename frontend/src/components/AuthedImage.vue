<template>
  <div class="relative overflow-hidden bg-slate-900/60" :class="wrapperClass">
    <img
      v-if="objectUrl"
      :src="objectUrl"
      :alt="alt"
      class="w-full h-full"
      :class="fit === 'contain' ? 'object-contain' : 'object-cover'"
      draggable="false"
    />
    <div v-else-if="error" class="w-full h-full flex items-center justify-center text-slate-500 text-xs p-2 text-center">
      Image indisponible
    </div>
    <div v-else class="w-full h-full flex items-center justify-center text-slate-500 text-xs animate-pulse">
      …
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import api from '@/api/axios'
import portalApi from '@/api/portalApi'

const props = defineProps({
  photoId:      { type: [Number, String], required: true },
  variant:      { type: String, default: 'staff' }, // 'staff' | 'portal'
  alt:          { type: String, default: 'Photo du véhicule' },
  wrapperClass: { type: String, default: '' },
  fit:          { type: String, default: 'cover' } // 'cover' | 'contain'
})

const objectUrl = ref(null)
const error = ref(false)

function revoke() {
  if (objectUrl.value) {
    URL.revokeObjectURL(objectUrl.value)
    objectUrl.value = null
  }
}

async function load() {
  revoke()
  error.value = false
  try {
    const client = props.variant === 'portal' ? portalApi : api
    const path = props.variant === 'portal'
      ? `/photos/${props.photoId}`
      : `/inspections/photos/${props.photoId}`
    const { data } = await client.get(path, { responseType: 'blob' })
    objectUrl.value = URL.createObjectURL(data)
  } catch {
    error.value = true
  }
}

watch(() => props.photoId, load, { immediate: true })
onBeforeUnmount(revoke)
</script>
