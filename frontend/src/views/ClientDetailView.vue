<template>
  <div class="p-4 space-y-4 pb-6">

    <!-- Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <RouterLink to="/clients" class="text-slate-400 hover:text-slate-200 min-h-0 min-w-0 p-1">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </RouterLink>
        <h2 class="text-xl font-bold">Détail client</h2>
      </div>
      <RouterLink :to="`/clients/${route.params.id}/edit`" class="btn-secondary text-sm py-2 px-4">
        Modifier
      </RouterLink>
    </div>

    <!-- Loading -->
    <div v-if="store.loading && !store.current" class="text-center py-10 text-slate-400 text-sm">
      Chargement...
    </div>

    <template v-else-if="store.current">
      <!-- Identity card -->
      <div class="card space-y-3">
        <div class="flex items-start justify-between">
          <div>
            <h3 class="text-lg font-bold">{{ store.current.name }}</h3>
            <p class="text-slate-400 text-sm">{{ store.current.phone }}</p>
          </div>
          <span
            class="text-xs px-2.5 py-1 rounded-full font-semibold"
            :class="{
              'bg-blue-900 text-blue-300':     store.current.type === 'CARTE',
              'bg-purple-900 text-purple-300': store.current.type === 'BOUCLIER',
              'bg-amber-900 text-amber-300':   store.current.type === 'VIP'
            }"
          >{{ store.current.type }}</span>
        </div>

        <hr class="border-slate-700" />

        <dl class="space-y-2 text-sm">
          <div class="flex justify-between">
            <dt class="text-slate-400">Passages restants</dt>
            <dd
              class="font-bold text-base"
              :class="store.current.balance <= 1 ? 'text-red-400' : 'text-green-400'"
            >
              {{ store.current.balance }}
            </dd>
          </div>
          <div v-if="store.current.expiresAt" class="flex justify-between">
            <dt class="text-slate-400">Expiration</dt>
            <dd :class="expiryClass(store.current.expiresAt)">
              {{ formatDate(store.current.expiresAt) }}
            </dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-slate-400">Client depuis</dt>
            <dd>{{ formatDate(store.current.createdAt) }}</dd>
          </div>
        </dl>
      </div>

      <!-- Alerts -->
      <div
        v-if="needsAlert(store.current)"
        class="flex items-center gap-2 bg-amber-900/40 border border-amber-700 rounded-xl px-4 py-2 text-amber-300 text-sm"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
        </svg>
        {{ alertMessage(store.current) }}
      </div>

      <!-- Add passages (CARTE / VIP) -->
      <div v-if="store.current.type !== 'BOUCLIER'" class="card space-y-3">
        <h4 class="font-semibold text-slate-300">Recharger les passages</h4>
        <div class="flex gap-2">
          <input
            v-model.number="passagesToAdd"
            type="number" min="1" max="100"
            class="input-field flex-1"
            placeholder="Nombre de passages"
          />
          <button
            @click="recharge"
            :disabled="!passagesToAdd || passagesToAdd < 1 || recharging"
            class="btn-primary px-5 shrink-0"
          >
            {{ recharging ? '...' : 'Ajouter' }}
          </button>
        </div>
        <p v-if="rechargeError" class="text-red-400 text-sm">{{ rechargeError }}</p>
        <p v-if="rechargeSuccess" class="text-green-400 text-sm">{{ rechargeSuccess }}</p>
      </div>

      <!-- Deactivate -->
      <button
        v-if="store.current.active"
        @click="confirmDeactivate"
        class="w-full py-3 rounded-xl border border-red-700 text-red-400 hover:bg-red-900/20 text-sm font-medium transition-colors"
      >
        Désactiver ce client
      </button>
    </template>

    <div v-else class="card text-center py-12 text-slate-400">
      Client introuvable
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { useClientsStore } from '@/stores/clients'

const route  = useRoute()
const router = useRouter()
const store  = useClientsStore()

const passagesToAdd  = ref(null)
const recharging     = ref(false)
const rechargeError  = ref('')
const rechargeSuccess = ref('')

onMounted(() => store.loadById(route.params.id))

async function recharge() {
  rechargeError.value   = ''
  rechargeSuccess.value = ''
  recharging.value      = true
  try {
    const updated = await store.addPassages(route.params.id, passagesToAdd.value)
    rechargeSuccess.value = `${passagesToAdd.value} passage(s) ajouté(s) — solde : ${updated.balance}`
    passagesToAdd.value   = null
  } catch (err) {
    rechargeError.value = err.response?.data?.error ?? 'Erreur lors de la recharge'
  } finally {
    recharging.value = false
  }
}

async function confirmDeactivate() {
  if (!confirm(`Désactiver ${store.current.name} ?`)) return
  await store.deactivate(route.params.id)
  router.push('/clients')
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

function expiryClass(iso) {
  const days = Math.ceil((new Date(iso) - Date.now()) / 86_400_000)
  if (days <= 0) return 'text-red-400 font-semibold'
  if (days <= 5) return 'text-amber-400 font-semibold'
  return 'text-slate-200'
}

function needsAlert(client) {
  if (client.type === 'CARTE' && client.balance <= 1) return true
  if (client.expiresAt) {
    const days = Math.ceil((new Date(client.expiresAt) - Date.now()) / 86_400_000)
    if (days <= 5) return true
  }
  return false
}

function alertMessage(client) {
  const msgs = []
  if (client.type === 'CARTE' && client.balance <= 1) {
    msgs.push(client.balance === 0 ? 'Plus de passages disponibles' : 'Dernier passage')
  }
  if (client.expiresAt) {
    const days = Math.ceil((new Date(client.expiresAt) - Date.now()) / 86_400_000)
    if (days <= 0) msgs.push('Abonnement expiré')
    else if (days <= 5) msgs.push(`Abonnement expire dans ${days} jour${days > 1 ? 's' : ''}`)
  }
  return msgs.join(' · ')
}
</script>
