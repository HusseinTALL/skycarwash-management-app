<template>
  <div class="min-h-screen bg-slate-900">
    <header class="sticky top-0 z-10 bg-slate-800 border-b border-slate-700 px-4 py-3 flex items-center gap-3">
      <RouterLink to="/rapports/espace" class="text-slate-400 hover:text-slate-200" aria-label="Retour">
        <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
        </svg>
      </RouterLink>
      <h1 class="font-bold text-white">Détail du rapport</h1>
    </header>

    <div class="p-4 max-w-2xl mx-auto pb-10">
      <div v-if="loading" class="text-center py-16 text-slate-400 text-sm">Chargement…</div>
      <InspectionReportContent v-else-if="report" :report="report" variant="portal" />
      <div v-else class="text-center py-16 text-slate-400 text-sm">Rapport introuvable.</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { usePortalStore } from '@/stores/portal'
import InspectionReportContent from '@/components/InspectionReportContent.vue'

const route = useRoute()
const portal = usePortalStore()

const report = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    report.value = await portal.fetchReport(route.params.id)
  } catch { /* not found / not owned */ } finally {
    loading.value = false
  }
})
</script>
