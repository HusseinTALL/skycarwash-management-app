<template>
  <div class="p-4 space-y-6 pb-10 max-w-2xl mx-auto">
    <div v-if="loading" class="text-center py-16 text-slate-400 text-sm">Chargement…</div>

    <template v-else-if="report">
      <header class="flex items-center justify-between">
        <div>
          <h1 class="text-lg font-bold text-white">Rapport d'état</h1>
          <p class="text-sm text-slate-400">Rapport #{{ report.id }}</p>
        </div>
        <button @click="showQr = !showQr" class="btn-secondary text-sm px-4 py-2">Accès client</button>
      </header>

      <!-- Accès client (QR + code) -->
      <section v-if="showQr" class="card space-y-3 text-center">
        <h2 class="font-semibold text-slate-200">Espace client « Rapports de lavage »</h2>
        <img v-if="qrDataUrl" :src="qrDataUrl" alt="QR portail client" class="mx-auto w-44 h-44 rounded-xl bg-white p-2" />
        <p class="text-sm text-slate-300">
          <a :href="portalUrl" class="text-brand-400 underline">{{ portalUrl }}</a>
        </p>
        <p v-if="report.customerPhone" class="text-sm text-slate-400">
          Téléphone : <span class="font-medium text-slate-200">{{ report.customerPhone }}</span><br />
          Code initial : les <span class="font-medium text-slate-200">4 derniers chiffres</span> du numéro
        </p>
        <p v-else class="text-xs text-amber-400">
          Aucun numéro enregistré — le client ne pourra pas accéder au portail.
        </p>
      </section>

      <InspectionReportContent :report="report" variant="staff" />

      <!-- Ajouter des photos après lavage -->
      <section class="card space-y-3">
        <h2 class="font-semibold text-slate-200">Ajouter des photos « après lavage »</h2>
        <p class="text-xs text-slate-500">Permet au client de comparer avant → après.</p>
        <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
          <label v-for="zone in DEFAULT_PHOTO_ZONES" :key="zone" class="block cursor-pointer">
            <input type="file" accept="image/*" capture="environment" class="hidden"
                   :disabled="uploading" @change="addAfter(zone, $event)" />
            <div class="aspect-[4/3] rounded-xl border-2 border-dashed border-slate-600 bg-slate-700/40
                        flex items-center justify-center hover:border-green-500 transition-colors">
              <span class="text-2xl text-slate-500">＋</span>
            </div>
            <p class="text-xs text-center text-slate-400 mt-1">{{ PHOTO_ZONE_LABELS[zone] }}</p>
          </label>
        </div>
        <p v-if="uploading" class="text-sm text-brand-300 text-center">Envoi de la photo…</p>
      </section>
    </template>

    <div v-else class="text-center py-16 text-slate-400 text-sm">Rapport introuvable.</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import QRCode from 'qrcode'
import { useInspectionStore } from '@/stores/inspection'
import InspectionReportContent from '@/components/InspectionReportContent.vue'
import { DEFAULT_PHOTO_ZONES, PHOTO_ZONE_LABELS } from '@/constants'

const route = useRoute()
const store = useInspectionStore()

const report = ref(null)
const loading = ref(true)
const uploading = ref(false)
const showQr = ref(false)
const qrDataUrl = ref('')
const portalUrl = `${window.location.origin}/rapports`

async function load() {
  loading.value = true
  try {
    report.value = await store.fetchReport(route.params.id)
  } finally {
    loading.value = false
  }
}

async function addAfter(zone, e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    await store.uploadPhoto(report.value.id, { file, phase: 'AFTER', zone })
    await load()
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

onMounted(async () => {
  await load()
  try {
    qrDataUrl.value = await QRCode.toDataURL(portalUrl, { margin: 1, width: 220 })
  } catch { /* ignore */ }
})
</script>
