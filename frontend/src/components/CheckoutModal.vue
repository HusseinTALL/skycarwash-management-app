<template>
  <Dialog
    :visible="true"
    modal
    :dismissable-mask="true"
    class="w-full max-w-md mx-3"
    :pt="{ root: { class: 'scw-checkout' } }"
    @update:visible="(v) => !v && $emit('close')"
  >
    <template #header>
      <div class="flex items-start justify-between gap-3 w-full">
        <div class="min-w-0">
          <p class="scw-eyebrow">Service</p>
          <h3 class="font-bold text-lg leading-tight text-slate-100">{{ service.name }}</h3>
          <Tag v-if="service.category" :value="service.category" severity="secondary" class="mt-1" />
        </div>
        <span class="text-2xl font-bold text-primary shrink-0">{{ formatPrice(service.price) }}</span>
      </div>
    </template>

    <div class="space-y-5">
      <!-- Payment methods -->
      <div>
        <p class="scw-eyebrow mb-3">Mode de paiement</p>
        <div class="grid grid-cols-2 gap-2.5">
          <button
            v-for="m in METHODS"
            :key="m.value"
            v-ripple
            class="pay-btn"
            :class="{ 'pay-btn-active': selectedPayment === m.value }"
            @click="selectPayment(m.value)"
          >
            <span class="text-2xl mb-1">{{ m.icon }}</span>
            <span class="font-semibold text-sm">{{ m.label }}</span>
          </button>
        </div>
      </div>

      <!-- Client section (ABONNEMENT only) -->
      <Transition name="expand">
        <div v-if="selectedPayment === 'ABONNEMENT'" class="space-y-3">
          <p class="scw-eyebrow">Client abonné</p>

          <div v-if="selectedClient" class="scw-panel p-3 flex items-center justify-between gap-2">
            <div class="min-w-0">
              <p class="font-semibold text-slate-100 truncate">{{ selectedClient.name }}</p>
              <p class="text-sm text-slate-400">{{ selectedClient.phone }}</p>
            </div>
            <div class="flex items-center gap-2 shrink-0">
              <Tag
                :severity="selectedClient.balance <= 1 ? 'danger' : 'success'"
                :value="`${selectedClient.balance} pass.`"
                rounded
              />
              <Button icon="pi pi-times" text rounded size="small" @click="selectedClient = null" />
            </div>
          </div>

          <AutoComplete
            v-else
            v-model="clientQuery"
            :suggestions="clientResults"
            :loading="clientLoading"
            option-label="name"
            placeholder="Nom ou téléphone…"
            class="w-full"
            input-class="w-full"
            :delay="300"
            @complete="doSearch"
            @item-select="pickClient"
          >
            <template #option="{ option }">
              <div class="flex items-center justify-between gap-3 w-full">
                <div class="min-w-0">
                  <p class="font-medium text-sm truncate">{{ option.name }}</p>
                  <p class="text-xs text-slate-400">{{ option.phone }}</p>
                </div>
                <div class="text-right shrink-0">
                  <Tag :value="option.type" severity="secondary" class="!text-[10px]" />
                  <p class="text-xs mt-0.5" :class="option.balance <= 1 ? 'text-red-400' : 'text-emerald-400'">
                    {{ option.balance }} pass.
                  </p>
                </div>
              </div>
            </template>
            <template #empty>
              <span class="text-sm text-slate-400 px-2">Aucun client trouvé</span>
            </template>
          </AutoComplete>
        </div>
      </Transition>

      <Message v-if="error" severity="error" :closable="false" size="small">{{ error }}</Message>
    </div>

    <template #footer>
      <div class="w-full space-y-2">
        <Button
          :label="`Valider — ${formatPrice(service.price)}`"
          icon="pi pi-check"
          class="w-full"
          size="large"
          :loading="confirming"
          :disabled="!canConfirm"
          @click="confirm"
        />
        <Button label="Annuler" text class="w-full" @click="$emit('close')" />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useCaisseStore } from '@/stores/caisse'
import { useToast } from 'primevue/usetoast'
import api from '@/api/axios'

import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import AutoComplete from 'primevue/autocomplete'

const props = defineProps({ service: { type: Object, required: true } })
const emit = defineEmits(['close'])

const caisse = useCaisseStore()
const toast = useToast()

const selectedPayment = ref(null)
const selectedClient = ref(null)
const clientQuery = ref('')
const clientResults = ref([])
const clientLoading = ref(false)
const confirming = ref(false)
const error = ref('')

const METHODS = [
  { value: 'CASH', label: 'Espèces', icon: '💵' },
  { value: 'ORANGE', label: 'Orange Money', icon: '🟠' },
  { value: 'MOOV', label: 'Moov Money', icon: '🔵' },
  { value: 'ABONNEMENT', label: 'Abonnement', icon: '🎫' }
]

const canConfirm = computed(() => {
  if (!selectedPayment.value) return false
  if (selectedPayment.value === 'ABONNEMENT' && !selectedClient.value) return false
  return true
})

function selectPayment(method) {
  selectedPayment.value = method
  if (method !== 'ABONNEMENT') {
    selectedClient.value = null
    clientQuery.value = ''
    clientResults.value = []
  }
}

function pickClient(event) {
  selectedClient.value = event.value
  clientQuery.value = ''
  clientResults.value = []
}

async function doSearch(event) {
  const q = event.query
  if (q.length < 2) {
    clientResults.value = []
    return
  }
  clientLoading.value = true
  try {
    const { data } = await api.get('/clients/search', { params: { q } })
    clientResults.value = data
  } catch {
    clientResults.value = []
  } finally {
    clientLoading.value = false
  }
}

async function confirm() {
  error.value = ''
  confirming.value = true
  try {
    caisse.selectedService = props.service
    caisse.selectedPayment = selectedPayment.value
    caisse.selectedClient = selectedClient.value
    await caisse.submitTransaction()
    toast.add({ severity: 'success', summary: 'Transaction enregistrée', detail: props.service.name, life: 2500 })
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
.pay-btn {
  @apply flex flex-col items-center justify-center py-4 rounded-2xl border-2 border-slate-700
         bg-slate-900/40 text-slate-300 transition-all duration-150;
}
.pay-btn:hover {
  @apply border-slate-500;
}
.pay-btn-active {
  @apply border-primary bg-primary/10 text-primary;
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}
.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  transform: translateY(-6px);
  max-height: 0;
}
.expand-enter-to,
.expand-leave-from {
  max-height: 400px;
}
</style>
