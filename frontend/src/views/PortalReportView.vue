<template>
  <div class="min-h-screen">
    <header class="sticky top-0 z-10 h-14 flex items-center gap-2 px-3 border-b border-slate-800/80 bg-slate-950/80 backdrop-blur-xl">
      <Button icon="pi pi-arrow-left" severity="secondary" text rounded aria-label="Retour" @click="router.push('/rapports/espace')" />
      <h1 class="font-bold text-slate-100">Détail du rapport</h1>
    </header>

    <div class="p-4 max-w-2xl mx-auto pb-10 scw-animate-in">
      <div v-if="loading" class="space-y-3">
        <Skeleton height="3rem" />
        <Skeleton height="14rem" border-radius="1rem" />
      </div>
      <InspectionReportContent v-else-if="report" :report="report" variant="portal" />
      <EmptyState v-else icon="pi pi-file" text="Rapport introuvable" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePortalStore } from '@/stores/portal'
import InspectionReportContent from '@/components/InspectionReportContent.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

import Button from 'primevue/button'
import Skeleton from 'primevue/skeleton'

const route = useRoute()
const router = useRouter()
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
