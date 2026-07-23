<template>
  <Dialog
    :visible="true"
    modal
    :dismissable-mask="true"
    :show-header="false"
    class="w-full max-w-sm mx-3"
    :pt="{ root: { class: 'receipt-print-area' } }"
    @update:visible="(v) => !v && caisse.closeReceipt()"
  >
    <div class="space-y-4 pt-2">
      <!-- Status -->
      <div class="flex justify-center">
        <Tag
          :severity="tx.cancelledAt ? 'danger' : tx.pending ? 'warn' : 'success'"
          :value="tx.cancelledAt ? 'Annulée' : tx.pending ? 'En attente (hors-ligne)' : 'Transaction validée'"
          :icon="tx.cancelledAt ? 'pi pi-times-circle' : tx.pending ? 'pi pi-clock' : 'pi pi-check-circle'"
          rounded
        />
      </div>

      <!-- Service + amount -->
      <div class="text-center">
        <p class="text-slate-400 text-sm">{{ tx.serviceName }}</p>
        <p class="text-4xl font-bold text-slate-100 mt-1">{{ formatPrice(tx.amount) }}</p>
        <p class="text-slate-400 text-sm mt-1">{{ paymentLabel(tx.paymentMethod) }}</p>
      </div>

      <hr class="border-slate-700" />

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
          <dd :class="tx.clientBalanceAfter <= 1 ? 'text-red-400 font-semibold' : 'text-emerald-400'">
            {{ tx.clientBalanceAfter }}
          </dd>
        </div>
        <div v-if="tx.cancelledAt" class="flex justify-between">
          <dt class="text-slate-400">Raison d'annulation</dt>
          <dd class="text-red-300">{{ tx.cancelReason }}</dd>
        </div>
      </dl>

      <div class="space-y-2 no-print">
        <Button
          v-if="!tx.cancelledAt && !tx.pending && caisse.isWithinCancelWindow"
          label="Annuler cette transaction"
          icon="pi pi-times-circle"
          severity="danger"
          outlined
          class="w-full"
          @click="caisse.openCancelModal(); caisse.closeReceipt()"
        />
        <Button
          v-if="tx.id && !tx.cancelledAt"
          label="Rapport d'état du véhicule"
          icon="pi pi-camera"
          severity="info"
          outlined
          class="w-full"
          @click="openInspection()"
        />
        <div class="flex gap-3">
          <Button label="Imprimer" icon="pi pi-print" severity="secondary" outlined class="flex-1" @click="print()" />
          <Button label="Fermer" icon="pi pi-check" class="flex-1" @click="caisse.closeReceipt()" />
        </div>
      </div>
    </div>
  </Dialog>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCaisseStore } from '@/stores/caisse'
import { PAYMENT_LABELS } from '@/constants'

import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Tag from 'primevue/tag'

const caisse = useCaisseStore()
const router = useRouter()
const tx = computed(() => caisse.lastTransaction)

function openInspection() {
  const query = { transactionId: tx.value.id }
  if (tx.value.clientId) query.clientId = tx.value.clientId
  if (tx.value.clientName) query.name = tx.value.clientName
  caisse.closeReceipt()
  router.push({ name: 'InspectionNew', query })
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
function print() {
  window.print()
}
</script>

<style>
@media print {
  body * { visibility: hidden; }
  .receipt-print-area,
  .receipt-print-area * { visibility: visible; }
  .receipt-print-area {
    position: fixed;
    inset: 0;
    background: white;
    color: black;
  }
  .no-print { display: none !important; }
}
</style>
