import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/axios', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() }
}))

import api from '@/api/axios'
import { useClientsStore } from '@/stores/clients'

const C1 = { id: 1, name: 'Alice Traoré',  phone: '+22601000001', type: 'CARTE',    balance: 10, active: true }
const C2 = { id: 2, name: 'Boubacar Koné', phone: '+22601000002', type: 'BOUCLIER', balance: 0,  active: true }

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
})

describe('loadAll', () => {
  it('fetches clients and stores them', async () => {
    api.get.mockResolvedValue({ data: [C1, C2] })
    const store = useClientsStore()

    await store.loadAll()

    expect(api.get).toHaveBeenCalledWith('/clients', { params: {} })
    expect(store.clients).toHaveLength(2)
    expect(store.clients[0].name).toBe('Alice Traoré')
    expect(store.loading).toBe(false)
  })

  it('passes filters (q, type, status, tag, sort) when provided', async () => {
    api.get.mockResolvedValue({ data: [C1] })
    const store = useClientsStore()

    await store.loadAll({ q: 'Al', type: 'VIP', status: 'all', tag: 'flotte', sort: 'spent' })

    expect(api.get).toHaveBeenCalledWith('/clients', {
      params: { q: 'Al', type: 'VIP', status: 'all', tag: 'flotte', sort: 'spent' }
    })
  })

  it('omits the search query when shorter than 2 chars', async () => {
    api.get.mockResolvedValue({ data: [C1, C2] })
    const store = useClientsStore()

    await store.loadAll({ q: 'A', sort: 'name' })

    expect(api.get).toHaveBeenCalledWith('/clients', { params: { sort: 'name' } })
  })

  it('sets loading false even on error', async () => {
    api.get.mockRejectedValue(new Error('Network error'))
    const store = useClientsStore()

    await expect(store.loadAll()).rejects.toThrow()
    expect(store.loading).toBe(false)
  })
})

describe('loadById / loadSummary', () => {
  it('loadById fetches and sets current', async () => {
    api.get.mockResolvedValue({ data: C1 })
    const store = useClientsStore()

    await store.loadById(1)

    expect(api.get).toHaveBeenCalledWith('/clients/1')
    expect(store.current).toEqual(C1)
  })

  it('loadSummary fetches the 360 profile', async () => {
    const summary = { client: C1, totalSpent: 5000, visitCount: 3, loyaltyPoints: 3, vehicles: [], history: [] }
    api.get.mockResolvedValue({ data: summary })
    const store = useClientsStore()

    await store.loadSummary(1)

    expect(api.get).toHaveBeenCalledWith('/clients/1/summary')
    expect(store.summary.totalSpent).toBe(5000)
  })
})

describe('create', () => {
  it('posts and returns the new client', async () => {
    api.post.mockResolvedValue({ data: C1 })
    const store = useClientsStore()

    const result = await store.create({ name: 'Alice', phone: '+22601000001', type: 'CARTE', balance: 10 })

    expect(api.post).toHaveBeenCalledWith('/clients', expect.objectContaining({ name: 'Alice' }))
    expect(result).toEqual(C1)
  })
})

describe('update', () => {
  it('puts and replaces current', async () => {
    const updated = { ...C1, name: 'Alice Updated', balance: 8 }
    api.put.mockResolvedValue({ data: updated })

    const store = useClientsStore()
    store.current = C1

    await store.update(1, { name: 'Alice Updated', balance: 8 })

    expect(api.put).toHaveBeenCalledWith('/clients/1', expect.objectContaining({ name: 'Alice Updated' }))
    expect(store.current.name).toBe('Alice Updated')
  })

  it('does not update current if it is a different client', async () => {
    api.put.mockResolvedValue({ data: { ...C2, name: 'Boubacar Updated' } })

    const store = useClientsStore()
    store.current = C1

    await store.update(2, { name: 'Boubacar Updated' })

    expect(store.current).toEqual(C1) // unchanged
  })
})

describe('addPassages', () => {
  it('posts and updates current balance', async () => {
    api.post.mockResolvedValue({ data: { ...C1, balance: 15 } })

    const store = useClientsStore()
    store.current = C1

    const result = await store.addPassages(1, 5)

    expect(api.post).toHaveBeenCalledWith('/clients/1/add-passages', { passages: 5 })
    expect(store.current.balance).toBe(15)
    expect(result.balance).toBe(15)
  })
})

describe('deactivate', () => {
  it('deletes and removes client from list', async () => {
    api.delete.mockResolvedValue({})

    const store = useClientsStore()
    store.clients = [C1, C2]
    store.current = C1

    await store.deactivate(1)

    expect(api.delete).toHaveBeenCalledWith('/clients/1')
    expect(store.clients).toHaveLength(1)
    expect(store.clients[0]).toEqual(C2)
    expect(store.current).toBeNull()
  })
})

describe('vehicles', () => {
  it('adds a vehicle to the loaded summary', async () => {
    const vehicle = { id: 9, type: 'VOITURE', plate: '11 AA 1234', label: 'Toyota' }
    api.post.mockResolvedValue({ data: vehicle })

    const store = useClientsStore()
    store.summary = { client: C1, vehicles: [], history: [] }

    const result = await store.addVehicle(1, { type: 'VOITURE', plate: '11 AA 1234' })

    expect(api.post).toHaveBeenCalledWith('/clients/1/vehicles', expect.objectContaining({ type: 'VOITURE' }))
    expect(store.summary.vehicles).toHaveLength(1)
    expect(result).toEqual(vehicle)
  })

  it('removes a vehicle from the loaded summary', async () => {
    api.delete.mockResolvedValue({})

    const store = useClientsStore()
    store.summary = { client: C1, vehicles: [{ id: 9, type: 'VOITURE' }], history: [] }

    await store.deleteVehicle(1, 9)

    expect(api.delete).toHaveBeenCalledWith('/clients/1/vehicles/9')
    expect(store.summary.vehicles).toHaveLength(0)
  })
})
