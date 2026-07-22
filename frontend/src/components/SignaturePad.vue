<template>
  <div class="space-y-2">
    <div ref="wrap" class="rounded-xl bg-white overflow-hidden border border-slate-300">
      <canvas
        ref="canvas"
        class="w-full touch-none block"
        style="height: 160px"
        @pointerdown="start"
        @pointermove="move"
        @pointerup="end"
        @pointerleave="end"
        @pointercancel="end"
      ></canvas>
    </div>
    <div class="flex justify-between items-center">
      <p class="text-xs text-slate-500">Signez ci-dessus pour valider l'état initial.</p>
      <button type="button" @click="clear" class="text-xs text-brand-400 hover:text-brand-300">Effacer</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const wrap = ref(null)
const canvas = ref(null)
let ctx = null
let drawing = false
const hasDrawn = ref(false)

function resize() {
  const el = canvas.value
  if (!el) return
  const ratio = window.devicePixelRatio || 1
  const w = wrap.value.clientWidth
  const h = 160
  el.width = w * ratio
  el.height = h * ratio
  ctx = el.getContext('2d')
  ctx.scale(ratio, ratio)
  ctx.lineWidth = 2.2
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  ctx.strokeStyle = '#0f172a'
}

function pos(e) {
  const rect = canvas.value.getBoundingClientRect()
  return { x: e.clientX - rect.left, y: e.clientY - rect.top }
}
function start(e) {
  drawing = true
  const p = pos(e)
  ctx.beginPath()
  ctx.moveTo(p.x, p.y)
}
function move(e) {
  if (!drawing) return
  const p = pos(e)
  ctx.lineTo(p.x, p.y)
  ctx.stroke()
  hasDrawn.value = true
}
function end() {
  drawing = false
}
function clear() {
  if (!ctx) return
  ctx.clearRect(0, 0, canvas.value.width, canvas.value.height)
  hasDrawn.value = false
}

function isEmpty() {
  return !hasDrawn.value
}
function toBlob() {
  return new Promise((resolve) => canvas.value.toBlob((b) => resolve(b), 'image/png'))
}

defineExpose({ isEmpty, toBlob, clear })

onMounted(() => {
  resize()
  window.addEventListener('resize', resize)
})
onBeforeUnmount(() => window.removeEventListener('resize', resize))
</script>
