<template>
  <Dialog
    :visible="true"
    modal
    header="Annuler la transaction"
    class="w-full max-w-md mx-3"
    @update:visible="(v) => !v && caisse.closeCancelModal()"
  >
    <div class="space-y-4">
      <p class="text-slate-400 text-sm">
        La transaction #{{ caisse.lastTransaction?.id }} sera annulée.
        <span v-if="caisse.lastTransaction?.paymentMethod === 'ABONNEMENT'">Le passage sera recrédité.</span>
      </p>

      <div>
        <label class="block text-sm font-medium text-slate-300 mb-1.5">
          Motif d'annulation <span class="text-red-400">*</span>
        </label>
        <Textarea v-model="reason" rows="3" class="w-full" auto-resize placeholder="Ex : erreur de service sélectionné…" />
      </div>

      <Message v-if="error" severity="error" :closable="false" size="small">{{ error }}</Message>
    </div>

    <template #footer>
      <Button label="Retour" severity="secondary" text @click="caisse.closeCancelModal()" />
      <Button
        label="Confirmer l'annulation"
        icon="pi pi-times-circle"
        severity="danger"
        :loading="loading"
        :disabled="!reason.trim()"
        @click="doCancel"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useCaisseStore } from '@/stores/caisse'

import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Textarea from 'primevue/textarea'
import Message from 'primevue/message'

const caisse = useCaisseStore()
const reason = ref('')
const loading = ref(false)
const error = ref('')

async function doCancel() {
  if (!reason.value.trim()) return
  error.value = ''
  loading.value = true
  try {
    await caisse.cancelLastTransaction(reason.value.trim())
  } catch (err) {
    error.value = err.response?.data?.error ?? "Erreur lors de l'annulation"
  } finally {
    loading.value = false
  }
}
</script>
