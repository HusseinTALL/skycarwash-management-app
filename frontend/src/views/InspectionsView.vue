<template>
  <div class="scw-animate-in">
    <PageHeader title="Rapports d'état" icon="pi pi-camera" subtitle="Contrôles véhicule avant / après lavage">
      <template #actions>
        <Button label="Nouveau" icon="pi pi-plus" @click="router.push('/inspections/new')" />
      </template>
    </PageHeader>

    <div v-if="store.loading" class="grid gap-3 sm:grid-cols-2">
      <Skeleton v-for="i in 4" :key="i" height="7rem" border-radius="1rem" />
    </div>

    <EmptyState
      v-else-if="store.recent.length === 0"
      icon="pi pi-camera"
      text="Aucun rapport pour le moment"
      hint="Créez un rapport d'état lors du prochain lavage"
    >
      <template #action>
        <Button label="Nouveau rapport" icon="pi pi-plus" @click="router.push('/inspections/new')" />
      </template>
    </EmptyState>

    <div v-else class="grid gap-3 sm:grid-cols-2">
      <RouterLink
        v-for="r in store.recent"
        :key="r.id"
        :to="{ name: 'InspectionDetail', params: { id: r.id } }"
        class="scw-panel p-4 block transition-all duration-150 hover:border-primary/40 hover:-translate-y-0.5"
      >
        <div class="flex items-center justify-between gap-2">
          <div class="flex items-center gap-3 min-w-0">
            <span class="grid place-items-center h-10 w-10 rounded-xl bg-slate-800 text-lg shrink-0">
              {{ VEHICLE_TYPE_ICONS[r.vehicleType] || '🚗' }}
            </span>
            <div class="min-w-0">
              <p class="font-semibold text-slate-100 truncate">
                {{ r.plate || VEHICLE_TYPE_LABELS[r.vehicleType] || 'Véhicule' }}
              </p>
              <p class="text-xs text-slate-500 truncate">
                {{ formatDateTime(r.createdAt) }}<span v-if="r.customerName"> · {{ r.customerName }}</span>
              </p>
            </div>
          </div>
          <Tag
            :severity="r.status === 'COMPLETED' ? 'success' : 'info'"
            :value="INSPECTION_STATUS_LABELS[r.status] || r.status"
            rounded
          />
        </div>
        <div class="flex flex-wrap gap-x-3 gap-y-1 mt-3 text-xs text-slate-400">
          <span v-if="r.serviceName" class="flex items-center gap-1"><i class="pi pi-tag text-[10px]" /> {{ r.serviceName }}</span>
          <span v-if="r.amount != null">{{ formatPrice(r.amount) }}</span>
          <span class="flex items-center gap-1"><i class="pi pi-images text-[10px]" /> {{ r.beforePhotoCount }}/{{ r.afterPhotoCount }}</span>
          <span v-if="r.damageCount" class="text-amber-400 flex items-center gap-1"><i class="pi pi-exclamation-triangle text-[10px]" /> {{ r.damageCount }}</span>
          <span v-if="r.foundItemCount" class="flex items-center gap-1"><i class="pi pi-briefcase text-[10px]" /> {{ r.foundItemCount }}</span>
        </div>
      </RouterLink>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useInspectionStore } from '@/stores/inspection'
import { VEHICLE_TYPE_LABELS, VEHICLE_TYPE_ICONS, INSPECTION_STATUS_LABELS } from '@/constants'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Skeleton from 'primevue/skeleton'

const store = useInspectionStore()
const router = useRouter()

onMounted(() => store.fetchRecent())

function formatPrice(f) {
  return new Intl.NumberFormat('fr-FR').format(f) + ' FCFA'
}
function formatDateTime(iso) {
  return iso ? new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }) : ''
}
</script>
