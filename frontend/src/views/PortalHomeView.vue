<template>
  <div class="min-h-screen">
    <header class="sticky top-0 z-10 h-14 flex items-center justify-between px-4 border-b border-slate-800/80 bg-slate-950/80 backdrop-blur-xl">
      <div class="flex items-center gap-2">
        <span class="grid place-items-center h-8 w-8 rounded-lg bg-primary text-slate-950"><i class="pi pi-sun" /></span>
        <div class="leading-tight">
          <h1 class="font-bold text-slate-100 text-sm">Mes rapports de lavage</h1>
          <p class="text-[11px] text-slate-500">{{ portal.phone }}</p>
        </div>
      </div>
      <Button label="Déconnexion" icon="pi pi-sign-out" text size="small" @click="logout" />
    </header>

    <div class="p-4 space-y-4 max-w-2xl mx-auto pb-10 scw-animate-in">
      <!-- Change access code -->
      <div v-if="portal.mustChangeCode || showChangeCode" class="scw-panel p-5 space-y-3 border-amber-700/40">
        <div class="flex items-center gap-2 text-amber-300">
          <i class="pi pi-shield" />
          <h2 class="font-semibold">Sécurisez votre espace</h2>
        </div>
        <p class="text-xs text-slate-400">Choisissez un nouveau code d'accès (4 à 6 chiffres).</p>
        <form class="space-y-3" @submit.prevent="changeCode">
          <InputText v-model="newCode" inputmode="numeric" placeholder="Nouveau code" class="w-full tracking-[0.3em]" required />
          <Message v-if="codeMsg" :severity="codeOk ? 'success' : 'error'" :closable="false" size="small">{{ codeMsg }}</Message>
          <Button type="submit" label="Enregistrer le code" icon="pi pi-check" class="w-full" :loading="savingCode" />
        </form>
      </div>

      <Button
        v-if="!portal.mustChangeCode && !showChangeCode"
        label="Modifier mon code d'accès"
        icon="pi pi-key"
        text
        size="small"
        @click="showChangeCode = true"
      />

      <div v-if="loading" class="grid gap-3">
        <Skeleton v-for="i in 3" :key="i" height="5.5rem" border-radius="1rem" />
      </div>

      <EmptyState v-else-if="reports.length === 0" icon="pi pi-inbox" text="Aucun rapport pour ce numéro" />

      <div v-else class="space-y-3">
        <RouterLink
          v-for="r in reports"
          :key="r.id"
          :to="{ name: 'PortalReport', params: { id: r.id } }"
          class="scw-panel p-4 block transition-all duration-150 hover:border-primary/40 hover:-translate-y-0.5"
        >
          <div class="flex items-center justify-between gap-2">
            <p class="font-semibold text-slate-100 truncate">
              {{ VEHICLE_TYPE_ICONS[r.vehicleType] || '🚗' }}
              {{ r.plate || VEHICLE_TYPE_LABELS[r.vehicleType] || 'Véhicule' }}
            </p>
            <Tag :severity="r.status === 'COMPLETED' ? 'success' : 'info'" :value="INSPECTION_STATUS_LABELS[r.status] || r.status" rounded />
          </div>
          <p class="text-xs text-slate-500 mt-1">{{ formatDateTime(r.createdAt) }}</p>
          <div class="flex flex-wrap gap-x-3 gap-y-1 mt-2 text-xs text-slate-400">
            <span v-if="r.serviceName" class="flex items-center gap-1"><i class="pi pi-tag text-[10px]" /> {{ r.serviceName }}</span>
            <span class="flex items-center gap-1"><i class="pi pi-images text-[10px]" /> {{ r.beforePhotoCount }}/{{ r.afterPhotoCount }}</span>
          </div>
        </RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { usePortalStore } from '@/stores/portal'
import { VEHICLE_TYPE_LABELS, VEHICLE_TYPE_ICONS, INSPECTION_STATUS_LABELS } from '@/constants'
import EmptyState from '@/components/ui/EmptyState.vue'

import Button from 'primevue/button'
import Tag from 'primevue/tag'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import Skeleton from 'primevue/skeleton'

const router = useRouter()
const portal = usePortalStore()

const reports = ref([])
const loading = ref(true)
const showChangeCode = ref(false)
const newCode = ref('')
const savingCode = ref(false)
const codeMsg = ref('')
const codeOk = ref(false)

async function load() {
  loading.value = true
  try {
    reports.value = await portal.fetchReports()
  } finally {
    loading.value = false
  }
}

async function changeCode() {
  if (savingCode.value) return
  codeMsg.value = ''
  if (!/^[0-9]{4,6}$/.test(newCode.value)) {
    codeMsg.value = 'Le code doit contenir 4 à 6 chiffres.'
    codeOk.value = false
    return
  }
  savingCode.value = true
  try {
    await portal.changeCode(newCode.value)
    codeOk.value = true
    codeMsg.value = 'Code mis à jour.'
    newCode.value = ''
    showChangeCode.value = false
  } catch (err) {
    codeOk.value = false
    codeMsg.value = err.response?.data?.error || 'Échec de la mise à jour.'
  } finally {
    savingCode.value = false
  }
}

function logout() {
  portal.logout()
  router.replace('/rapports')
}

function formatDateTime(iso) {
  return iso ? new Date(iso).toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' }) : ''
}

onMounted(load)
</script>
