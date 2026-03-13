<template>
  <div class="p-4 space-y-4 pb-6">

    <!-- Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <RouterLink to="/clients" aria-label="Retour à la liste des clients"
                    class="text-slate-400 hover:text-slate-200 min-h-0 min-w-0 p-1">
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

    <!-- Loading skeleton -->
    <div v-if="store.loading && !store.current" class="card animate-pulse space-y-4">
      <div class="flex justify-between">
        <div class="space-y-2">
          <div class="h-5 bg-slate-700 rounded w-36"></div>
          <div class="h-3 bg-slate-700 rounded w-24"></div>
        </div>
        <div class="h-6 bg-slate-700 rounded w-16"></div>
      </div>
      <hr class="border-slate-700" />
      <div class="space-y-3">
        <div class="flex justify-between">
          <div class="h-3 bg-slate-700 rounded w-28"></div>
          <div class="h-4 bg-slate-700 rounded w-8"></div>
        </div>
        <div class="flex justify-between">
          <div class="h-3 bg-slate-700 rounded w-24"></div>
          <div class="h-3 bg-slate-700 rounded w-20"></div>
        </div>
      </div>
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
          >{{ CLIENT_TYPE_LABELS[store.current.type] ?? store.current.type }}</span>
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
            <dd :class="expiryClass(store.current.expiresAt)" class="flex items-center gap-1.5">
              {{ formatDate(store.current.expiresAt) }}
              <span v-if="expiryStatus(store.current.expiresAt)"
                    class="text-xs px-1.5 py-0.5 rounded font-semibold"
                    :class="{
                      'bg-red-900/50 text-red-300':    expiryStatus(store.current.expiresAt) === 'Expiré',
                      'bg-amber-900/50 text-amber-300': expiryStatus(store.current.expiresAt) === 'Bientôt'
                    }">
                {{ expiryStatus(store.current.expiresAt) }}
              </span>
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

      <!-- QR Card button (feature 6) -->
      <button
        @click="openQrCard"
        class="w-full flex items-center justify-center gap-2 py-3 rounded-xl border border-slate-600 text-slate-300 hover:bg-slate-700 text-sm font-medium transition-colors"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" />
        </svg>
        Carte QR client
      </button>

      <!-- Deactivate -->
      <button
        v-if="store.current.active"
        @click="showDeactivateModal = true"
        class="w-full py-3 rounded-xl border border-red-700 text-red-400 hover:bg-red-900/20 text-sm font-medium transition-colors"
      >
        Désactiver ce client
      </button>
    </template>

    <!-- QR Card modal (feature 6) -->
    <Teleport to="body">
      <div
        v-if="showQrModal && store.current"
        class="fixed inset-0 bg-black/80 flex items-end sm:items-center justify-center z-50 print:hidden"
        @click.self="showQrModal = false"
      >
        <div class="bg-slate-800 rounded-t-2xl sm:rounded-2xl w-full max-w-sm">
          <div class="flex items-center justify-between p-4 border-b border-slate-700 print:hidden">
            <h3 class="font-semibold">Carte client</h3>
            <div class="flex gap-2">
              <button @click="printCard" class="btn-primary text-sm py-1.5 px-4">Imprimer</button>
              <button @click="showQrModal = false" class="btn-secondary text-sm py-1.5 px-3">✕</button>
            </div>
          </div>

          <!-- Card content -->
          <div id="qr-card-content" class="p-6 flex flex-col items-center gap-4">
            <p class="text-xl font-bold text-center">{{ store.current.name }}</p>
            <p class="text-slate-400 text-sm">{{ store.current.phone }}</p>
            <span
              class="text-xs px-3 py-1 rounded-full font-semibold"
              :class="{
                'bg-blue-900 text-blue-300':     store.current.type === 'CARTE',
                'bg-purple-900 text-purple-300': store.current.type === 'BOUCLIER',
                'bg-amber-900 text-amber-300':   store.current.type === 'VIP'
              }"
            >{{ CLIENT_TYPE_LABELS[store.current.type] ?? store.current.type }}</span>
            <img v-if="qrDataUrl" :src="qrDataUrl" alt="QR Code" class="w-48 h-48 bg-white p-2 rounded-xl" />
            <p v-else class="text-slate-500 text-sm">Génération du QR...</p>
            <p class="text-xs text-slate-500 text-center">Présentez cette carte à la caisse</p>
          </div>
        </div>
      </div>
    </Teleport>

    <ConfirmModal
      v-if="showDeactivateModal"
      title="Désactiver ce client ?"
      :message="`${store.current?.name} ne pourra plus être utilisé pour des transactions.`"
      confirm-label="Désactiver"
      @confirm="doDeactivate"
      @cancel="showDeactivateModal = false"
    />

    <div v-else class="card text-center py-12 text-slate-400">
      Client introuvable
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { useClientsStore } from '@/stores/clients'
import ConfirmModal from '@/components/ConfirmModal.vue'
import QRCode from 'qrcode'

const CLIENT_TYPE_LABELS = {
  CARTE:    'Carte passages',
  BOUCLIER: 'Bouclier',
  VIP:      'VIP'
}

const route  = useRoute()
const router = useRouter()
const store  = useClientsStore()

const passagesToAdd      = ref(null)
const recharging         = ref(false)
const rechargeError      = ref('')
const rechargeSuccess    = ref('')
const showDeactivateModal = ref(false)
const showQrModal        = ref(false)
const qrDataUrl          = ref(null)

onMounted(() => store.loadById(route.params.id))

function printCard() {
  window.print()
}

async function openQrCard() {
  showQrModal.value = true
  qrDataUrl.value   = null
  try {
    qrDataUrl.value = await QRCode.toDataURL(store.current.phone, {
      width: 300, margin: 2,
      color: { dark: '#000000', light: '#ffffff' }
    })
  } catch (err) {
    console.error('QR generation failed', err)
  }
}

async function recharge() {
  if (recharging.value) return
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

async function doDeactivate() {
  showDeactivateModal.value = false
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

function expiryStatus(iso) {
  const days = Math.ceil((new Date(iso) - Date.now()) / 86_400_000)
  if (days <= 0) return 'Expiré'
  if (days <= 5) return 'Bientôt'
  return null
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
