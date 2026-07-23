<template>
  <div class="p-4 space-y-4 pb-6 max-w-4xl mx-auto">
    <header class="flex items-center justify-between">
      <div>
        <h1 class="text-lg font-bold text-white">Rapports d'état</h1>
        <p class="text-sm text-slate-400">Contrôles véhicule avant / après lavage</p>
      </div>
      <RouterLink to="/inspections/new" class="btn-primary text-sm px-4 py-2">+ Nouveau</RouterLink>
    </header>

    <div v-if="store.loading" class="text-center py-12 text-slate-400 text-sm">Chargement…</div>

    <div v-else-if="store.recent.length === 0" class="card text-center py-12 text-slate-400 text-sm">
      Aucun rapport pour le moment.
    </div>

    <div v-else class="grid gap-3 sm:grid-cols-2">
      <RouterLink
        v-for="r in store.recent"
        :key="r.id"
        :to="{ name: 'InspectionDetail', params: { id: r.id } }"
        class="card block hover:bg-slate-700/60 transition-colors"
      >
        <div class="flex items-center justify-between gap-2">
          <div class="min-w-0">
            <p class="font-semibold text-slate-200 truncate">
              {{ VEHICLE_TYPE_ICONS[r.vehicleType] || '🚗' }}
              {{ r.plate || VEHICLE_TYPE_LABELS[r.vehicleType] || 'Véhicule' }}
            </p>
            <p class="text-xs text-slate-500">
              {{ formatDateTime(r.createdAt) }}
              <span v-if="r.customerName"> · {{ r.customerName }}</span>
            </p>
          </div>
          <span class="text-xs shrink-0 px-2 py-0.5 rounded-full"
                :class="r.status === 'COMPLETED' ? 'bg-green-900/50 text-green-300' : 'bg-brand-900/50 text-brand-300'">
            {{ INSPECTION_STATUS_LABELS[r.status] || r.status }}
          </span>
        </div>
        <div class="flex flex-wrap gap-x-4 gap-y-1 mt-2 text-xs text-slate-400">
          <span v-if="r.serviceName">{{ r.serviceName }}</span>
          <span v-if="r.amount != null">{{ formatPrice(r.amount) }}</span>
          <span>📷 {{ r.beforePhotoCount }} avant / {{ r.afterPhotoCount }} après</span>
          <span v-if="r.damageCount">⚠️ {{ r.damageCount }} dommage(s)</span>
          <span v-if="r.foundItemCount">🎒 {{ r.foundItemCount }} objet(s)</span>
        </div>
      </RouterLink>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useInspectionStore } from '@/stores/inspection'
import { VEHICLE_TYPE_LABELS, VEHICLE_TYPE_ICONS, INSPECTION_STATUS_LABELS } from '@/constants'

const store = useInspectionStore()

onMounted(() => store.fetchRecent())

function formatPrice(f) { return new Intl.NumberFormat('fr-FR').format(f) + ' FCFA' }
function formatDateTime(iso) {
  return iso ? new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }) : ''
}
</script>
