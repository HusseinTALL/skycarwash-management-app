<template>
  <div
    ref="container"
    class="relative w-full select-none rounded-2xl overflow-hidden bg-slate-900 aspect-[4/3] cursor-ew-resize touch-none"
    @pointerdown="startDrag"
    @pointermove="onDrag"
    @pointerup="endDrag"
    @pointercancel="endDrag"
  >
    <!-- After (bottom layer, full) -->
    <AuthedImage
      :photo-id="afterPhotoId"
      :variant="variant"
      alt="Après lavage"
      wrapper-class="absolute inset-0 w-full h-full"
    />

    <!-- Before (top layer, clipped to the left of the handle) -->
    <div class="absolute inset-0" :style="{ clipPath: `inset(0 ${100 - pos}% 0 0)` }">
      <AuthedImage
        :photo-id="beforePhotoId"
        :variant="variant"
        alt="Avant lavage"
        wrapper-class="absolute inset-0 w-full h-full"
      />
    </div>

    <!-- Labels -->
    <span class="absolute top-2 left-2 text-xs font-semibold bg-black/60 text-white px-2 py-0.5 rounded-full pointer-events-none">
      📷 Avant
    </span>
    <span class="absolute top-2 right-2 text-xs font-semibold bg-black/60 text-white px-2 py-0.5 rounded-full pointer-events-none">
      Après 📷
    </span>

    <!-- Divider + handle -->
    <div class="absolute inset-y-0 pointer-events-none" :style="{ left: `${pos}%` }">
      <div class="absolute inset-y-0 -translate-x-1/2 w-0.5 bg-white/90"></div>
      <div
        role="slider"
        tabindex="0"
        :aria-valuenow="Math.round(pos)"
        aria-valuemin="0"
        aria-valuemax="100"
        aria-label="Comparer avant / après"
        class="absolute top-1/2 -translate-x-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-white shadow-lg
               flex items-center justify-center text-slate-800 pointer-events-auto"
        @keydown="onKey"
      >
        <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M8 7l-5 5 5 5M16 7l5 5-5 5" />
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AuthedImage from '@/components/AuthedImage.vue'

defineProps({
  beforePhotoId: { type: [Number, String], required: true },
  afterPhotoId:  { type: [Number, String], required: true },
  variant:       { type: String, default: 'staff' }
})

const container = ref(null)
const pos = ref(50)
const dragging = ref(false)

function setFromClientX(clientX) {
  const rect = container.value.getBoundingClientRect()
  const p = ((clientX - rect.left) / rect.width) * 100
  pos.value = Math.min(100, Math.max(0, p))
}

function startDrag(e) {
  dragging.value = true
  container.value.setPointerCapture?.(e.pointerId)
  setFromClientX(e.clientX)
}
function onDrag(e) {
  if (dragging.value) setFromClientX(e.clientX)
}
function endDrag() {
  dragging.value = false
}
function onKey(e) {
  if (e.key === 'ArrowLeft') { pos.value = Math.max(0, pos.value - 5); e.preventDefault() }
  if (e.key === 'ArrowRight') { pos.value = Math.min(100, pos.value + 5); e.preventDefault() }
}
</script>
