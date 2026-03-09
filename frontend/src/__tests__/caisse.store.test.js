import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// ── Mock dependencies ──────────────────────────────────────────────── //

vi.mock('@/api/axios', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

vi.mock('@/offline/queue', () => ({
  enqueue: vi.fn(),
  getAll:  vi.fn(),
  remove:  vi.fn(),
  count:   vi.fn()
}))

// useOnline ref — controllable in tests
let _online = true
vi.mock('@vueuse/core', () => ({
  useOnline: () => ({ value: _online })
}))

import api              from '@/api/axios'
import { enqueue, getAll, remove, count } from '@/offline/queue'
import { useCaisseStore } from '@/stores/caisse'

// ── Helpers ────────────────────────────────────────────────────────── //

const SERVICE  = { id: 1, name: 'Lavage extérieur', price: 2500 }
const CLIENT   = { id: 7, name: 'Mamadou Ba', balance: 4 }
const TX_DATA  = {
  id: 42, serviceName: 'Lavage extérieur', amount: 2500,
  paymentMethod: 'CASH', clientName: null, clientBalanceAfter: null,
  createdAt: new Date().toISOString(), cancelledAt: null
}

beforeEach(() => {
  setActivePinia(createPinia())
  _online = true
  vi.clearAllMocks()
  count.mockResolvedValue(0)
  getAll.mockResolvedValue([])
})

// ── Submit transaction ─────────────────────────────────────────────── //

describe('submitTransaction', () => {
  it('online: posts to API and sets lastTransaction', async () => {
    api.post.mockResolvedValue({ data: TX_DATA })
    const store = useCaisseStore()

    store.selectedService = SERVICE
    store.selectedPayment = 'CASH'

    await store.submitTransaction()

    expect(api.post).toHaveBeenCalledWith('/transactions', {
      serviceId:     1,
      clientId:      null,
      paymentMethod: 'CASH'
    })
    expect(store.lastTransaction.id).toBe(42)
    expect(store.lastTransaction.pending).toBe(false)
    expect(store.showReceipt).toBe(true)
  })

  it('online: sets 2-minute cancel deadline', async () => {
    api.post.mockResolvedValue({ data: TX_DATA })
    const store = useCaisseStore()

    store.selectedService = SERVICE
    store.selectedPayment = 'CASH'

    const before = Date.now()
    await store.submitTransaction()
    const after  = Date.now()

    const deadline = store.cancelDeadline.getTime()
    // Deadline should be ~2 minutes in the future
    expect(deadline).toBeGreaterThan(before + 110_000)
    expect(deadline).toBeLessThan(after  + 125_000)
  })

  it('online with ABONNEMENT: includes clientId in payload', async () => {
    api.post.mockResolvedValue({ data: { ...TX_DATA, paymentMethod: 'ABONNEMENT', clientBalanceAfter: 3 } })
    const store = useCaisseStore()

    store.selectedService = SERVICE
    store.selectedPayment = 'ABONNEMENT'
    store.selectedClient  = CLIENT

    await store.submitTransaction()

    expect(api.post).toHaveBeenCalledWith('/transactions', {
      serviceId:     1,
      clientId:      7,
      paymentMethod: 'ABONNEMENT'
    })
  })

  it('online: resets selected service/payment/client after submit', async () => {
    api.post.mockResolvedValue({ data: TX_DATA })
    const store = useCaisseStore()

    store.selectedService = SERVICE
    store.selectedPayment = 'CASH'

    await store.submitTransaction()

    expect(store.selectedService).toBeNull()
    expect(store.selectedPayment).toBeNull()
    expect(store.selectedClient).toBeNull()
  })

  it('offline: enqueues transaction and sets pending lastTransaction', async () => {
    _online = false
    enqueue.mockResolvedValue(1)
    count.mockResolvedValue(1)

    const store = useCaisseStore()
    store.selectedService = SERVICE
    store.selectedPayment = 'CASH'

    await store.submitTransaction()

    expect(enqueue).toHaveBeenCalledWith({
      serviceId:     1,
      clientId:      null,
      paymentMethod: 'CASH'
    })
    expect(api.post).not.toHaveBeenCalled()
    expect(store.lastTransaction.pending).toBe(true)
    expect(store.lastTransaction.id).toBeNull()
    expect(store.pendingCount).toBe(1)
    expect(store.showReceipt).toBe(true)
  })

  it('offline ABONNEMENT: estimates balance locally', async () => {
    _online = false
    enqueue.mockResolvedValue(1)
    count.mockResolvedValue(1)

    const store = useCaisseStore()
    store.selectedService = SERVICE
    store.selectedPayment = 'ABONNEMENT'
    store.selectedClient  = CLIENT  // balance = 4

    await store.submitTransaction()

    expect(store.lastTransaction.clientBalanceAfter).toBe(3) // 4 - 1
  })
})

// ── Cancel transaction ─────────────────────────────────────────────── //

describe('cancelLastTransaction', () => {
  it('posts cancel and updates lastTransaction', async () => {
    const cancelled = { ...TX_DATA, cancelledAt: new Date().toISOString(), cancelReason: 'erreur' }
    api.post.mockResolvedValue({ data: cancelled })

    const store = useCaisseStore()
    store.lastTransaction = { ...TX_DATA, pending: false }

    await store.cancelLastTransaction('erreur')

    expect(api.post).toHaveBeenCalledWith('/transactions/42/cancel', { reason: 'erreur' })
    expect(store.lastTransaction.cancelledAt).toBeTruthy()
    expect(store.showReceipt).toBe(true)
    expect(store.showCancelModal).toBe(false)
  })

  it('does nothing when lastTransaction has no id (pending offline tx)', async () => {
    const store = useCaisseStore()
    store.lastTransaction = { id: null, pending: true }

    await store.cancelLastTransaction('erreur')

    expect(api.post).not.toHaveBeenCalled()
  })
})

// ── Sync offline queue ─────────────────────────────────────────────── //

describe('syncOfflineQueue', () => {
  it('syncs all queued items and removes them on success', async () => {
    const q = [
      { localId: 1, queuedAt: '2026-01-01T00:00:00Z', serviceId: 1, paymentMethod: 'CASH' },
      { localId: 2, queuedAt: '2026-01-01T00:01:00Z', serviceId: 2, paymentMethod: 'ORANGE' }
    ]
    getAll.mockResolvedValue(q)
    api.post.mockResolvedValue({ data: TX_DATA })
    count.mockResolvedValue(0)

    const store = useCaisseStore()
    await store.syncOfflineQueue()

    expect(api.post).toHaveBeenCalledTimes(2)
    expect(remove).toHaveBeenCalledWith(1)
    expect(remove).toHaveBeenCalledWith(2)
    expect(store.pendingCount).toBe(0)
    expect(store.syncing).toBe(false)
  })

  it('stops syncing on first API error and leaves remaining items', async () => {
    const q = [
      { localId: 1, queuedAt: '2026-01-01T00:00:00Z', serviceId: 1, paymentMethod: 'CASH' },
      { localId: 2, queuedAt: '2026-01-01T00:01:00Z', serviceId: 2, paymentMethod: 'ORANGE' }
    ]
    getAll.mockResolvedValue(q)
    api.post.mockRejectedValue(new Error('Network error'))
    count.mockResolvedValue(2)

    const store = useCaisseStore()
    await store.syncOfflineQueue()

    // Only one API call made (broke on first error)
    expect(api.post).toHaveBeenCalledTimes(1)
    expect(remove).not.toHaveBeenCalled()
    expect(store.syncing).toBe(false)
  })

  it('does nothing when queue is empty', async () => {
    getAll.mockResolvedValue([])
    const store = useCaisseStore()
    await store.syncOfflineQueue()
    expect(api.post).not.toHaveBeenCalled()
  })

  it('does not run concurrently (syncing guard)', async () => {
    const store = useCaisseStore()
    store.syncing = true
    await store.syncOfflineQueue()
    expect(getAll).not.toHaveBeenCalled()
  })
})

// ── canSubmit computed ─────────────────────────────────────────────── //

describe('canSubmit', () => {
  it('is false when nothing is selected', () => {
    const store = useCaisseStore()
    expect(store.canSubmit).toBe(false)
  })

  it('is true when service + non-ABONNEMENT payment selected', () => {
    const store = useCaisseStore()
    store.selectedService = SERVICE
    store.selectedPayment = 'CASH'
    expect(store.canSubmit).toBe(true)
  })

  it('is false for ABONNEMENT without a client', () => {
    const store = useCaisseStore()
    store.selectedService = SERVICE
    store.selectedPayment = 'ABONNEMENT'
    expect(store.canSubmit).toBe(false)
  })

  it('is true for ABONNEMENT with a client', () => {
    const store = useCaisseStore()
    store.selectedService = SERVICE
    store.selectedPayment = 'ABONNEMENT'
    store.selectedClient  = CLIENT
    expect(store.canSubmit).toBe(true)
  })
})
