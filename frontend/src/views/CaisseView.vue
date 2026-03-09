<template>
  <div class="p-4 space-y-5 pb-6">

    <!-- Offline banner -->
    <div
      v-if="!caisse.online"
      class="flex items-center gap-2 bg-amber-900/40 border border-amber-700 rounded-xl px-4 py-2 text-amber-300 text-sm"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
      </svg>
      Hors-ligne — les transactions seront synchronisées à la reconnexion.
      <span v-if="caisse.pendingCount > 0" class="font-bold ml-1">({{ caisse.pendingCount }} en attente)</span>
    </div>

    <!-- Syncing banner -->
    <div
      v-if="caisse.syncing"
      class="flex items-center gap-2 bg-brand-900/40 border border-brand-700 rounded-xl px-4 py-2 text-brand-300 text-sm"
    >
      Synchronisation en cours...
    </div>

    <!-- ── STEP 1: Select service ─────────────────────────────────────── -->
    <section>
      <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-3">
        1. Service
      </h2>

      <div v-if="caisse.servicesLoading" class="text-center py-8 text-slate-400 text-sm">
        Chargement des services...
      </div>

      <div v-else-if="caisse.services.length === 0"
           class="card text-center py-8 text-slate-400 text-sm">
        Aucun service disponible
      </div>

      <div v-else class="grid grid-cols-2 gap-3">
        <ServiceCard
          v-for="svc in caisse.services"
          :key="svc.id"
          :service="svc"
          :selected="caisse.selectedService?.id === svc.id"
          @select="caisse.selectedService = $event"
        />
      </div>
    </section>

    <!-- ── STEP 2: Select payment method ────────────────────────────── -->
    <section v-if="caisse.selectedService">
      <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-3">
        2. Paiement
      </h2>
      <PaymentSelector
        :selected="caisse.selectedPayment"
        @select="onPaymentSelect"
      />
    </section>

    <!-- ── STEP 3: Select client (ABONNEMENT only) ───────────────────── -->
    <section v-if="caisse.selectedPayment === 'ABONNEMENT'">
      <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-3">
        3. Client
      </h2>
      <button
        @click="showClientSearch = true"
        class="w-full card text-left hover:bg-slate-700 transition-colors"
      >
        <div v-if="caisse.selectedClient" class="flex items-center justify-between">
          <div>
            <p class="font-semibold">{{ caisse.selectedClient.name }}</p>
            <p class="text-sm text-slate-400">{{ caisse.selectedClient.phone }}</p>
          </div>
          <span class="text-green-400 text-sm font-medium">
            {{ caisse.selectedClient.balance }} passage(s)
          </span>
        </div>
        <div v-else class="flex items-center gap-3 text-slate-400">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <span>Rechercher un client...</span>
        </div>
      </button>
    </section>

    <!-- ── Summary + confirm ─────────────────────────────────────────── -->
    <section v-if="caisse.selectedService && caisse.selectedPayment">
      <div class="card bg-slate-700/50 space-y-2">
        <div class="flex justify-between text-sm">
          <span class="text-slate-400">Service</span>
          <span class="font-medium">{{ caisse.selectedService.name }}</span>
        </div>
        <div class="flex justify-between text-sm">
          <span class="text-slate-400">Paiement</span>
          <span class="font-medium">{{ paymentLabel(caisse.selectedPayment) }}</span>
        </div>
        <div v-if="caisse.selectedClient" class="flex justify-between text-sm">
          <span class="text-slate-400">Client</span>
          <span class="font-medium">{{ caisse.selectedClient.name }}</span>
        </div>
        <hr class="border-slate-600" />
        <div class="flex justify-between items-center">
          <span class="text-slate-400">Montant</span>
          <span class="text-xl font-bold text-brand-400">
            {{ formatPrice(caisse.selectedService.price) }}
          </span>
        </div>
      </div>

      <p v-if="submitError" class="text-red-400 text-sm text-center mt-2">{{ submitError }}</p>

      <button
        @click="submit"
        :disabled="!caisse.canSubmit || submitting"
        class="btn-primary w-full mt-3 py-4 text-base"
      >
        {{ submitting ? 'Enregistrement...' : 'Valider la transaction' }}
      </button>
    </section>
  </div>

  <!-- Full-screen client search -->
  <ClientSearchModal
    v-if="showClientSearch"
    @select="onClientSelected"
    @close="showClientSearch = false"
  />

  <!-- Receipt sheet -->
  <ReceiptOverlay v-if="caisse.showReceipt" />

  <!-- Cancel modal -->
  <CancelModal v-if="caisse.showCancelModal" />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useCaisseStore } from '@/stores/caisse'
import ServiceCard        from '@/components/ServiceCard.vue'
import PaymentSelector    from '@/components/PaymentSelector.vue'
import ClientSearchModal  from '@/components/ClientSearchModal.vue'
import ReceiptOverlay     from '@/components/ReceiptOverlay.vue'
import CancelModal        from '@/components/CancelModal.vue'

const caisse           = useCaisseStore()
const showClientSearch = ref(false)
const submitting       = ref(false)
const submitError      = ref('')

const PAYMENT_LABELS = {
  CASH:       'Espèces',
  ORANGE:     'Orange Money',
  MOOV:       'Moov Money',
  ABONNEMENT: 'Abonnement'
}

onMounted(() => caisse.loadServices())

function onPaymentSelect(method) {
  caisse.selectedPayment = method
  if (method !== 'ABONNEMENT') caisse.selectedClient = null
}

function onClientSelected(client) {
  caisse.selectedClient  = client
  showClientSearch.value = false
}

async function submit() {
  submitError.value = ''
  submitting.value  = true
  try {
    await caisse.submitTransaction()
  } catch (err) {
    submitError.value = err.response?.data?.error ?? 'Erreur lors de l\'enregistrement'
  } finally {
    submitting.value = false
  }
}

function formatPrice(fcfa) {
  return new Intl.NumberFormat('fr-FR').format(fcfa) + ' FCFA'
}

function paymentLabel(method) {
  return PAYMENT_LABELS[method] ?? method
}
</script>
