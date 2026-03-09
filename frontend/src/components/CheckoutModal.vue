<template>
  <Teleport to="body">
    <!-- Backdrop -->
    <Transition name="fade">
      <div
        class="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm"
        @click="$emit('close')"
      />
    </Transition>

    <!-- Panel — bottom sheet on mobile, centered card on md+ -->
    <Transition name="slide-up">
      <div class="fixed inset-x-0 bottom-0 z-50 md:inset-0 md:flex md:items-center md:justify-center md:p-6">
        <div class="bg-slate-800 rounded-t-3xl md:rounded-3xl w-full md:max-w-md max-h-[92vh] flex flex-col shadow-2xl">

          <!-- Drag handle (mobile only) -->
          <div class="flex justify-center pt-3 pb-1 md:hidden shrink-0">
            <div class="w-10 h-1 bg-slate-600 rounded-full" />
          </div>

          <!-- Header -->
          <div class="px-5 pt-3 pb-4 border-b border-slate-700 shrink-0">
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <p class="text-xs text-slate-400 uppercase tracking-wider mb-0.5">Service</p>
                <h3 class="font-bold text-lg leading-tight truncate">{{ service.name }}</h3>
                <span
                  v-if="service.category"
                  class="text-xs bg-slate-700 text-slate-300 rounded-full px-2 py-0.5 mt-1 inline-block"
                >{{ service.category }}</span>
              </div>
              <div class="text-right shrink-0">
                <span class="text-2xl font-bold text-brand-400">{{ formatPrice(service.price) }}</span>
              </div>
            </div>
          </div>

          <!-- Scrollable body -->
          <div class="flex-1 overflow-y-auto px-5 py-4 space-y-5">

            <!-- Payment methods -->
            <div>
              <p class="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">Mode de paiement</p>
              <div class="grid grid-cols-2 gap-2.5">
                <button
                  v-for="method in METHODS"
                  :key="method.value"
                  @click="selectPayment(method.value)"
                  class="flex flex-col items-center justify-center py-4 rounded-2xl border-2 transition-all duration-150 active:scale-95"
                  :class="selectedPayment === method.value
                    ? 'border-brand-500 bg-brand-500/10 text-brand-400'
                    : 'border-slate-600 bg-slate-900/40 text-slate-300 hover:border-slate-500'"
                >
                  <span class="text-2xl mb-1">{{ method.icon }}</span>
                  <span class="font-semibold text-sm">{{ method.label }}</span>
                </button>
              </div>
            </div>

            <!-- Client section (ABONNEMENT only) -->
            <Transition name="expand">
              <div v-if="selectedPayment === 'ABONNEMENT'" class="space-y-3">
                <p class="text-xs font-semibold text-slate-400 uppercase tracking-wider">Client abonné</p>

                <!-- Selected client -->
                <div v-if="selectedClient" class="card bg-slate-900/60 flex items-center justify-between gap-2">
                  <div class="min-w-0">
                    <p class="font-semibold truncate">{{ selectedClient.name }}</p>
                    <p class="text-sm text-slate-400">{{ selectedClient.phone }}</p>
                  </div>
                  <div class="flex items-center gap-2 shrink-0">
                    <span
                      class="text-sm font-medium"
                      :class="selectedClient.balance <= 1 ? 'text-red-400' : 'text-green-400'"
                    >
                      {{ selectedClient.balance }} pass.
                    </span>
                    <button
                      @click="selectedClient = null"
                      class="text-slate-500 hover:text-slate-300 transition-colors p-1"
                    >
                      <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                      </svg>
                    </button>
                  </div>
                </div>

                <!-- Search -->
                <div v-else>
                  <div class="relative">
                    <svg class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400 pointer-events-none"
                         fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
                    </svg>
                    <input
                      v-model="clientQuery"
                      ref="clientInput"
                      type="search"
                      placeholder="Nom ou téléphone..."
                      class="input-field pl-9"
                      @input="onClientInput"
                    />
                  </div>

                  <!-- Results -->
                  <div class="mt-2 space-y-1.5 max-h-44 overflow-y-auto">
                    <p v-if="clientLoading" class="text-center text-slate-400 py-3 text-sm">Recherche...</p>
                    <p v-else-if="clientQuery.length >= 2 && clientResults.length === 0"
                       class="text-center text-slate-400 py-3 text-sm">Aucun client trouvé</p>
                    <p v-else-if="clientQuery.length < 2"
                       class="text-xs text-slate-500 text-center py-2">Tapez au moins 2 caractères</p>

                    <button
                      v-for="client in clientResults"
                      :key="client.id"
                      @click="pickClient(client)"
                      class="w-full card py-2.5 text-left hover:bg-slate-700 active:scale-[0.98] transition-all duration-100"
                    >
                      <div class="flex items-center justify-between">
                        <div class="min-w-0">
                          <p class="font-semibold text-sm truncate">{{ client.name }}</p>
                          <p class="text-xs text-slate-400">{{ client.phone }}</p>
                        </div>
                        <div class="shrink-0 text-right ml-3">
                          <span class="text-xs px-1.5 py-0.5 rounded-full"
                            :class="{
                              'bg-blue-900/60 text-blue-300':   client.type === 'CARTE',
                              'bg-purple-900/60 text-purple-300': client.type === 'BOUCLIER',
                              'bg-amber-900/60 text-amber-300':  client.type === 'VIP'
                            }"
                          >{{ client.type }}</span>
                          <p class="text-xs mt-0.5"
                             :class="client.balance <= 1 ? 'text-red-400' : 'text-green-400'">
                            {{ client.balance }} pass.
                          </p>
                        </div>
                      </div>
                    </button>
                  </div>
                </div>
              </div>
            </Transition>

          </div>

          <!-- Footer — confirm button -->
          <div class="px-5 pb-6 pt-3 border-t border-slate-700 shrink-0">
            <p v-if="error" class="text-red-400 text-sm text-center mb-2">{{ error }}</p>
            <button
              @click="confirm"
              :disabled="!canConfirm || confirming"
              class="btn-primary w-full py-4 text-base disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <span v-if="confirming" class="flex items-center justify-center gap-2">
                <svg class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
                </svg>
                Enregistrement...
              </span>
              <span v-else>
                Valider — {{ formatPrice(service.price) }}
              </span>
            </button>
            <button
              @click="$emit('close')"
              class="w-full mt-2 py-2.5 text-sm text-slate-400 hover:text-slate-200 transition-colors"
            >
              Annuler
            </button>
          </div>

        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import { useCaisseStore } from '@/stores/caisse'
import api from '@/api/axios'

const props = defineProps({
  service: { type: Object, required: true }
})
const emit = defineEmits(['close'])

const caisse = useCaisseStore()

const selectedPayment = ref(null)
const selectedClient  = ref(null)
const clientQuery     = ref('')
const clientResults   = ref([])
const clientLoading   = ref(false)
const clientInput     = ref(null)
const confirming      = ref(false)
const error           = ref('')

let debounce

const METHODS = [
  { value: 'CASH',        label: 'Espèces',      icon: '💵' },
  { value: 'ORANGE',      label: 'Orange Money', icon: '🟠' },
  { value: 'MOOV',        label: 'Moov Money',   icon: '🔵' },
  { value: 'ABONNEMENT',  label: 'Abonnement',   icon: '🎫' }
]

const canConfirm = computed(() => {
  if (!selectedPayment.value) return false
  if (selectedPayment.value === 'ABONNEMENT' && !selectedClient.value) return false
  return true
})

function selectPayment(method) {
  selectedPayment.value = method
  if (method !== 'ABONNEMENT') {
    selectedClient.value  = null
    clientQuery.value     = ''
    clientResults.value   = []
  } else {
    nextTick(() => clientInput.value?.focus())
  }
}

function pickClient(client) {
  selectedClient.value  = client
  clientQuery.value     = ''
  clientResults.value   = []
}

function onClientInput() {
  clearTimeout(debounce)
  if (clientQuery.value.length < 2) { clientResults.value = []; return }
  debounce = setTimeout(doSearch, 300)
}

async function doSearch() {
  clientLoading.value = true
  try {
    const { data } = await api.get('/clients/search', { params: { q: clientQuery.value } })
    clientResults.value = data
  } catch {
    clientResults.value = []
  } finally {
    clientLoading.value = false
  }
}

async function confirm() {
  error.value     = ''
  confirming.value = true
  try {
    caisse.selectedService = props.service
    caisse.selectedPayment = selectedPayment.value
    caisse.selectedClient  = selectedClient.value
    await caisse.submitTransaction()
    emit('close')
  } catch (err) {
    error.value = err.response?.data?.error ?? "Erreur lors de l'enregistrement"
  } finally {
    confirming.value = false
  }
}

function formatPrice(fcfa) {
  return new Intl.NumberFormat('fr-FR').format(fcfa) + ' FCFA'
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from,
.fade-leave-to     { opacity: 0; }

.slide-up-enter-active { transition: transform 0.3s cubic-bezier(0.32, 0.72, 0, 1), opacity 0.2s ease; }
.slide-up-leave-active { transition: transform 0.25s ease-in, opacity 0.2s ease; }
.slide-up-enter-from   { transform: translateY(100%); opacity: 0; }
.slide-up-leave-to     { transform: translateY(100%); opacity: 0; }

@media (min-width: 768px) {
  .slide-up-enter-from,
  .slide-up-leave-to { transform: scale(0.95) translateY(0); }
}

.expand-enter-active { transition: all 0.25s ease; overflow: hidden; }
.expand-leave-active { transition: all 0.2s ease; overflow: hidden; }
.expand-enter-from,
.expand-leave-to     { opacity: 0; transform: translateY(-6px); max-height: 0; }
.expand-enter-to,
.expand-leave-from   { max-height: 400px; }
</style>
