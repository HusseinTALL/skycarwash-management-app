<template>
  <div class="min-h-screen bg-slate-900">
    <!-- Top bar -->
    <header class="sticky top-0 z-10 bg-slate-800 border-b border-slate-700 px-4 py-3 flex items-center justify-between">
      <div>
        <h1 class="font-bold text-white">Mes rapports de lavage</h1>
        <p class="text-xs text-slate-400">{{ portal.phone }}</p>
      </div>
      <button @click="logout" class="text-sm text-slate-400 hover:text-slate-200">Déconnexion</button>
    </header>

    <div class="p-4 space-y-4 max-w-2xl mx-auto pb-10">
      <!-- Changer le code (par défaut) -->
      <section v-if="portal.mustChangeCode || showChangeCode" class="card space-y-3 border border-amber-700/50">
        <h2 class="font-semibold text-amber-300">Sécurisez votre espace</h2>
        <p class="text-xs text-slate-400">Choisissez un nouveau code d'accès (4 à 6 chiffres).</p>
        <form @submit.prevent="changeCode" class="space-y-2">
          <input v-model="newCode" inputmode="numeric" placeholder="Nouveau code" class="input-field tracking-widest" required />
          <p v-if="codeMsg" class="text-xs" :class="codeOk ? 'text-green-400' : 'text-red-400'">{{ codeMsg }}</p>
          <button type="submit" class="btn-primary w-full text-sm" :disabled="savingCode">
            {{ savingCode ? 'Enregistrement…' : 'Enregistrer le code' }}
          </button>
        </form>
      </section>

      <button v-if="!portal.mustChangeCode && !showChangeCode" @click="showChangeCode = true"
              class="text-xs text-brand-400">Modifier mon code d'accès</button>

      <!-- Liste -->
      <div v-if="loading" class="text-center py-12 text-slate-400 text-sm">Chargement…</div>

      <div v-else-if="reports.length === 0" class="card text-center py-12 text-slate-400 text-sm">
        Aucun rapport pour ce numéro.
      </div>

      <div v-else class="space-y-3">
        <RouterLink
          v-for="r in reports"
          :key="r.id"
          :to="{ name: 'PortalReport', params: { id: r.id } }"
          class="card block hover:bg-slate-700/60 transition-colors"
        >
          <div class="flex items-center justify-between gap-2">
            <p class="font-semibold text-slate-200 truncate">
              {{ VEHICLE_TYPE_ICONS[r.vehicleType] || '🚗' }}
              {{ r.plate || VEHICLE_TYPE_LABELS[r.vehicleType] || 'Véhicule' }}
            </p>
            <span class="text-xs shrink-0 px-2 py-0.5 rounded-full"
                  :class="r.status === 'COMPLETED' ? 'bg-green-900/50 text-green-300' : 'bg-brand-900/50 text-brand-300'">
              {{ INSPECTION_STATUS_LABELS[r.status] || r.status }}
            </span>
          </div>
          <p class="text-xs text-slate-500 mt-1">{{ formatDateTime(r.createdAt) }}</p>
          <div class="flex flex-wrap gap-x-4 gap-y-1 mt-2 text-xs text-slate-400">
            <span v-if="r.serviceName">{{ r.serviceName }}</span>
            <span>📷 {{ r.beforePhotoCount }} avant / {{ r.afterPhotoCount }} après</span>
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

onMounted(load)
</script>
