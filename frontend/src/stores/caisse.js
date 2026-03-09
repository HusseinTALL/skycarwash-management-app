import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useOnline } from '@vueuse/core'
import api from '@/api/axios'
import { enqueue, getAll, remove, count } from '@/offline/queue'

export const useCaisseStore = defineStore('caisse', () => {
  const online       = useOnline()
  const services     = ref([])
  const servicesLoading = ref(false)

  // ── Caisse state ────────────────────────────────────────────────────── //
  const selectedService    = ref(null)  // ServiceDto
  const selectedPayment    = ref(null)  // 'CASH' | 'ORANGE' | 'MOOV' | 'ABONNEMENT'
  const selectedClient     = ref(null)  // ClientDto (only for ABONNEMENT)

  // ── Last completed transaction (for receipt + cancel window) ────────── //
  const lastTransaction    = ref(null)  // TransactionResponse
  const showReceipt        = ref(false)
  const showCancelModal    = ref(false)
  const cancelDeadline     = ref(null)  // Date – 2 min after createdAt

  // ── Offline queue info ───────────────────────────────────────────────── //
  const pendingCount       = ref(0)
  const syncing            = ref(false)

  const canSubmit = computed(() =>
    selectedService.value !== null &&
    selectedPayment.value !== null &&
    (selectedPayment.value !== 'ABONNEMENT' || selectedClient.value !== null)
  )

  const isWithinCancelWindow = computed(() => {
    if (!cancelDeadline.value || !lastTransaction.value) return false
    return new Date() < cancelDeadline.value
  })

  // ── Load services (cached via Workbox when offline) ─────────────────── //
  async function loadServices() {
    servicesLoading.value = true
    try {
      const { data } = await api.get('/services')
      services.value = data
    } catch (err) {
      // Workbox may serve from cache; ignore network errors
      console.warn('[caisse] services load failed:', err.message)
    } finally {
      servicesLoading.value = false
    }
  }

  // ── Submit transaction ───────────────────────────────────────────────── //
  async function submitTransaction() {
    const payload = {
      serviceId:     selectedService.value.id,
      clientId:      selectedClient.value?.id ?? null,
      paymentMethod: selectedPayment.value
    }

    if (!online.value) {
      // Queue for later sync
      await enqueue(payload)
      pendingCount.value = await count()
      // Show a local "receipt" with pending status
      lastTransaction.value = {
        id:            null,
        serviceName:   selectedService.value.name,
        servicePrice:  selectedService.value.price,
        amount:        selectedService.value.price,
        paymentMethod: selectedPayment.value,
        clientName:    selectedClient.value?.name ?? null,
        // Estimated locally; server will confirm the real value on sync
        clientBalanceAfter: selectedClient.value ? Math.max(0, selectedClient.value.balance - 1) : null,
        createdAt:     new Date().toISOString(),
        pending:       true
      }
    } else {
      const { data } = await api.post('/transactions', payload)
      lastTransaction.value = { ...data, pending: false }
      // Set 2-minute cancel window
      const createdAt = new Date(data.createdAt)
      cancelDeadline.value = new Date(createdAt.getTime() + 2 * 60 * 1000)
    }

    showReceipt.value = true
    resetSelection()
  }

  // ── Cancel last transaction ──────────────────────────────────────────── //
  async function cancelLastTransaction(reason) {
    if (!lastTransaction.value?.id) return
    const { data } = await api.post(`/transactions/${lastTransaction.value.id}/cancel`, { reason })
    lastTransaction.value = data
    showCancelModal.value = false
    showReceipt.value     = true
  }

  // ── Sync offline queue when back online ─────────────────────────────── //
  async function syncOfflineQueue() {
    if (syncing.value) return
    const queued = await getAll()
    if (queued.length === 0) return

    syncing.value = true
    for (const item of queued) {
      try {
        const { localId, queuedAt, ...payload } = item
        await api.post('/transactions', payload)
        await remove(localId)
      } catch (err) {
        console.error('[caisse] sync failed for item', item.localId, err.message)
        break // stop on first error; retry next time
      }
    }
    pendingCount.value = await count()
    syncing.value = false
  }

  function resetSelection() {
    selectedService.value = null
    selectedPayment.value = null
    selectedClient.value  = null
  }

  function closeReceipt() {
    showReceipt.value = false
  }

  function openCancelModal() {
    showCancelModal.value = true
  }

  function closeCancelModal() {
    showCancelModal.value = false
  }

  // Watch for connectivity restore
  if (typeof window !== 'undefined') {
    window.addEventListener('online', () => {
      syncOfflineQueue()
    })
    // Load initial pending count
    count().then(n => { pendingCount.value = n })
  }

  return {
    online, services, servicesLoading,
    selectedService, selectedPayment, selectedClient,
    lastTransaction, showReceipt, showCancelModal, cancelDeadline,
    pendingCount, syncing, canSubmit, isWithinCancelWindow,
    loadServices, submitTransaction, cancelLastTransaction,
    syncOfflineQueue, resetSelection, closeReceipt, openCancelModal, closeCancelModal
  }
})
