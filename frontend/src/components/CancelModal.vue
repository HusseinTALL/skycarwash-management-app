<template>
  <div class="fixed inset-0 z-50 flex items-end bg-black/60" @click.self="caisse.closeCancelModal()">
    <div class="w-full bg-slate-800 rounded-t-3xl p-6 space-y-4">
      <h3 class="text-lg font-bold text-center">Annuler la transaction</h3>

      <p class="text-slate-400 text-sm text-center">
        La transaction #{{ caisse.lastTransaction?.id }} sera annulée.
        <span v-if="caisse.lastTransaction?.paymentMethod === 'ABONNEMENT'">
          Le passage sera recrédité.
        </span>
      </p>

      <!-- Reason -->
      <div>
        <label class="block text-sm font-medium text-slate-300 mb-1">Motif d'annulation <span class="text-red-400">*</span></label>
        <textarea
          v-model="reason"
          rows="3"
          placeholder="Ex: erreur de service sélectionné..."
          class="input-field resize-none"
        />
      </div>

      <p v-if="error" class="text-red-400 text-sm text-center">{{ error }}</p>

      <div class="flex gap-3">
        <button @click="caisse.closeCancelModal()" class="btn-secondary flex-1">
          Retour
        </button>
        <button
          @click="doCancel"
          :disabled="!reason.trim() || loading"
          class="btn-primary flex-1 bg-red-600 hover:bg-red-700"
        >
          {{ loading ? 'Annulation...' : 'Confirmer' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useCaisseStore } from '@/stores/caisse'

const caisse  = useCaisseStore()
const reason  = ref('')
const loading = ref(false)
const error   = ref('')

async function doCancel() {
  if (!reason.value.trim()) return
  error.value   = ''
  loading.value = true
  try {
    await caisse.cancelLastTransaction(reason.value.trim())
  } catch (err) {
    error.value = err.response?.data?.error ?? 'Erreur lors de l\'annulation'
  } finally {
    loading.value = false
  }
}
</script>
