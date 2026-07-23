<template>
  <div class="scw-animate-in">
    <PageHeader title="Caisse" icon="pi pi-wallet" subtitle="Sélectionnez un service pour encaisser">
      <template #actions>
        <Tag :value="`${caisse.services.length} services`" severity="secondary" rounded />
      </template>
    </PageHeader>

    <Message v-if="!caisse.online" severity="warn" :closable="false" icon="pi pi-wifi" class="mb-4">
      Hors-ligne — les transactions seront synchronisées à la reconnexion.
      <strong v-if="caisse.pendingCount > 0"> ({{ caisse.pendingCount }} en attente)</strong>
    </Message>
    <Message v-if="caisse.syncing" severity="info" :closable="false" icon="pi pi-refresh" class="mb-4">
      Synchronisation en cours…
    </Message>

    <!-- Loading skeletons -->
    <div v-if="caisse.servicesLoading" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4">
      <Skeleton v-for="i in 8" :key="i" height="7.5rem" border-radius="1rem" />
    </div>

    <EmptyState v-else-if="caisse.services.length === 0" icon="pi pi-inbox" text="Aucun service disponible" />

    <!-- Service grid -->
    <div v-else class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4">
      <button
        v-for="svc in caisse.services"
        :key="svc.id"
        v-ripple
        class="service-card group"
        @click="openCheckout(svc)"
      >
        <div class="flex items-start justify-between gap-2 w-full mb-2">
          <span class="font-semibold text-sm leading-snug text-slate-100 group-hover:text-primary transition-colors text-left">
            {{ svc.name }}
          </span>
          <Tag v-if="svc.category" :value="svc.category" severity="secondary" class="!text-[10px] shrink-0" />
        </div>
        <p class="text-primary font-bold text-lg">{{ formatPrice(svc.price) }}</p>
        <p class="text-xs text-slate-500 mt-1.5 flex items-center gap-1">
          <i class="pi pi-plus-circle text-[11px]" /> Encaisser
        </p>
      </button>
    </div>
  </div>

  <CheckoutModal v-if="checkoutService" :service="checkoutService" @close="checkoutService = null" />
  <ReceiptOverlay v-if="caisse.showReceipt" />
  <CancelModal v-if="caisse.showCancelModal" />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useCaisseStore } from '@/stores/caisse'
import CheckoutModal from '@/components/CheckoutModal.vue'
import ReceiptOverlay from '@/components/ReceiptOverlay.vue'
import CancelModal from '@/components/CancelModal.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

import Tag from 'primevue/tag'
import Message from 'primevue/message'
import Skeleton from 'primevue/skeleton'

const caisse = useCaisseStore()
const checkoutService = ref(null)

onMounted(() => caisse.loadServices())

function openCheckout(svc) {
  caisse.selectedService = null
  caisse.selectedPayment = null
  caisse.selectedClient = null
  checkoutService.value = svc
}

function formatPrice(fcfa) {
  return new Intl.NumberFormat('fr-FR').format(fcfa) + ' FCFA'
}
</script>

<style scoped>
.service-card {
  @apply flex flex-col items-start rounded-2xl border border-slate-800/80 bg-slate-800/70 p-4
         text-left transition-all duration-150;
}
.service-card:hover {
  @apply border-primary/50 bg-slate-800 -translate-y-0.5;
}
.service-card:active {
  @apply scale-[0.98];
}
</style>
