<template>
  <div class="p-4 space-y-4 pb-6">

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

    <!-- Section header -->
    <div class="flex items-center justify-between">
      <h2 class="text-base font-semibold text-slate-200">Choisir un service</h2>
      <span class="text-xs text-slate-500">{{ caisse.services.length }} services</span>
    </div>

    <!-- Loading -->
    <div v-if="caisse.servicesLoading" class="text-center py-12 text-slate-400 text-sm">
      Chargement des services...
    </div>

    <!-- Empty -->
    <div v-else-if="caisse.services.length === 0"
         class="card text-center py-12 text-slate-400 text-sm">
      Aucun service disponible
    </div>

    <!-- Service grid — responsive: 2 cols → 3 on sm → 4 on lg -->
    <div v-else class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
      <button
        v-for="svc in caisse.services"
        :key="svc.id"
        @click="openCheckout(svc)"
        class="card text-left transition-all duration-150 active:scale-95 hover:bg-slate-700 hover:ring-2 hover:ring-brand-500/50 group"
      >
        <div class="flex items-start justify-between gap-2 mb-2">
          <span class="font-semibold text-sm leading-snug group-hover:text-brand-300 transition-colors">
            {{ svc.name }}
          </span>
          <span
            v-if="svc.category"
            class="text-xs bg-slate-600 text-slate-300 rounded-full px-2 py-0.5 shrink-0"
          >{{ svc.category }}</span>
        </div>
        <p class="text-brand-400 font-bold text-lg">{{ formatPrice(svc.price) }}</p>
        <p class="text-xs text-slate-500 mt-1.5 flex items-center gap-1">
          <svg class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          Appuyer pour sélectionner
        </p>
      </button>
    </div>

  </div>

  <!-- Checkout modal -->
  <CheckoutModal
    v-if="checkoutService"
    :service="checkoutService"
    @close="checkoutService = null"
  />

  <!-- Receipt sheet -->
  <ReceiptOverlay v-if="caisse.showReceipt" />

  <!-- Cancel modal -->
  <CancelModal v-if="caisse.showCancelModal" />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useCaisseStore } from '@/stores/caisse'
import CheckoutModal  from '@/components/CheckoutModal.vue'
import ReceiptOverlay from '@/components/ReceiptOverlay.vue'
import CancelModal    from '@/components/CancelModal.vue'

const caisse          = useCaisseStore()
const checkoutService = ref(null)

onMounted(() => caisse.loadServices())

function openCheckout(svc) {
  caisse.selectedService = null
  caisse.selectedPayment = null
  caisse.selectedClient  = null
  checkoutService.value  = svc
}

function formatPrice(fcfa) {
  return new Intl.NumberFormat('fr-FR').format(fcfa) + ' FCFA'
}
</script>
