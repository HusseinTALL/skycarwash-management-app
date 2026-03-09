<template>
  <div class="fixed inset-0 z-50 flex items-end bg-black/60" @click.self="caisse.closeReceipt()">
    <div class="w-full bg-slate-800 rounded-t-3xl p-6 space-y-4 animate-slide-up">

      <!-- Status badge -->
      <div class="flex justify-center">
        <span
          class="inline-flex items-center gap-2 text-sm font-semibold px-4 py-1.5 rounded-full"
          :class="tx.cancelledAt
            ? 'bg-red-900/50 text-red-300'
            : tx.pending
              ? 'bg-amber-900/50 text-amber-300'
              : 'bg-green-900/50 text-green-300'"
        >
          <span v-if="tx.cancelledAt">Annulée</span>
          <span v-else-if="tx.pending">En attente (hors-ligne)</span>
          <span v-else>Transaction validée</span>
        </span>
      </div>

      <!-- Service + amount -->
      <div class="text-center">
        <p class="text-slate-400 text-sm">{{ tx.serviceName }}</p>
        <p class="text-4xl font-bold text-white mt-1">{{ formatPrice(tx.amount) }}</p>
        <p class="text-slate-400 text-sm mt-1">{{ paymentLabel(tx.paymentMethod) }}</p>
      </div>

      <hr class="border-slate-700" />

      <!-- Details -->
      <dl class="space-y-2 text-sm">
        <div v-if="tx.id" class="flex justify-between">
          <dt class="text-slate-400">N° transaction</dt>
          <dd class="font-mono">#{{ tx.id }}</dd>
        </div>
        <div class="flex justify-between">
          <dt class="text-slate-400">Heure</dt>
          <dd>{{ formatTime(tx.createdAt) }}</dd>
        </div>
        <div v-if="tx.clientName" class="flex justify-between">
          <dt class="text-slate-400">Client</dt>
          <dd>{{ tx.clientName }}</dd>
        </div>
        <div v-if="tx.clientBalanceAfter !== null && tx.clientBalanceAfter !== undefined" class="flex justify-between">
          <dt class="text-slate-400">Passages restants</dt>
          <dd :class="tx.clientBalanceAfter <= 1 ? 'text-red-400 font-semibold' : 'text-green-400'">
            {{ tx.clientBalanceAfter }}
          </dd>
        </div>
        <div v-if="tx.cancelledAt" class="flex justify-between">
          <dt class="text-slate-400">Raison d'annulation</dt>
          <dd class="text-red-300">{{ tx.cancelReason }}</dd>
        </div>
      </dl>

      <!-- Cancel button – shown within 2-minute window for non-cancelled, non-pending transactions -->
      <button
        v-if="!tx.cancelledAt && !tx.pending && caisse.isWithinCancelWindow"
        @click="caisse.openCancelModal(); caisse.closeReceipt()"
        class="w-full py-3 rounded-xl border border-red-700 text-red-400 hover:bg-red-900/20 text-sm font-medium transition-colors"
      >
        Annuler cette transaction
      </button>

      <!-- Close -->
      <button @click="caisse.closeReceipt()" class="btn-primary w-full">
        Fermer
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useCaisseStore } from '@/stores/caisse'

const caisse = useCaisseStore()
const tx     = computed(() => caisse.lastTransaction)

const PAYMENT_LABELS = {
  CASH:        'Espèces',
  ORANGE:      'Orange Money',
  MOOV:        'Moov Money',
  ABONNEMENT:  'Abonnement'
}

function formatPrice(fcfa) {
  return new Intl.NumberFormat('fr-FR').format(fcfa) + ' FCFA'
}

function paymentLabel(method) {
  return PAYMENT_LABELS[method] ?? method
}

function formatTime(iso) {
  if (!iso) return '--'
  return new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style>
@keyframes slide-up {
  from { transform: translateY(100%); }
  to   { transform: translateY(0); }
}
.animate-slide-up {
  animation: slide-up 0.25s ease-out;
}
</style>
