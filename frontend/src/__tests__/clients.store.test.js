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

  it('passes search query when length >= 2', async () => {
    api.get.mockResolvedValue({ data: [C1] })
    const store = useClientsStore()

    await store.loadAll('Al')

    expect(api.get).toHaveBeenCalledWith('/clients', { params: { q: 'Al' } })
  })

  it('does not pass query when length < 2', async () => {
    api.get.mockResolvedValue({ data: [C1, C2] })
    const store = useClientsStore()

    await store.loadAll('A')

    expect(api.get).toHaveBeenCalledWith('/clients', { params: {} })
  })

  it('sets loading false even on error', async () => {
    api.get.mockRejectedValue(new Error('Network error'))
    const store = useClientsStore()

    await expect(store.loadAll()).rejects.toThrow()
    expect(store.loading).toBe(false)
  })
})

describe('loadById', () => {
  it('fetches and sets current', async () => {
    api.get.mockResolvedValue({ data: C1 })
    const store = useClientsStore()

    await store.loadById(1)

    expect(api.get).toHaveBeenCalledWith('/clients/1')
    expect(store.current).toEqual(C1)
    expect(store.loading).toBe(false)
  })
})

describe('create', () => {
  it('posts and prepends the new client to the list', async () => {
    api.get.mockResolvedValue({ data: [C2] })
    api.post.mockResolvedValue({ data: C1 })

    const store = useClientsStore()
    await store.loadAll()
    const result = await store.create({ name: 'Alice', phone: '+22601000001', type: 'CARTE', balance: 10 })

    expect(api.post).toHaveBeenCalledWith('/clients', expect.objectContaining({ name: 'Alice' }))
    expect(store.clients[0]).toEqual(C1)
    expect(result).toEqual(C1)
  })
})

describe('update', () => {
  it('puts and replaces the client in list and current', async () => {
    const updated = { ...C1, name: 'Alice Updated', balance: 8 }
    api.put.mockResolvedValue({ data: updated })

    const store = useClientsStore()
    store.clients = [C1, C2]
    store.current = C1

    await store.update(1, { name: 'Alice Updated', balance: 8 })

    expect(api.put).toHaveBeenCalledWith('/clients/1', expect.objectContaining({ name: 'Alice Updated' }))
    expect(store.clients[0].name).toBe('Alice Updated')
    expect(store.current.name).toBe('Alice Updated')
  })

  it('does not update current if it is a different client', async () => {
    const updated = { ...C2, name: 'Boubacar Updated' }
    api.put.mockResolvedValue({ data: updated })

    const store = useClientsStore()
    store.clients = [C1, C2]
    store.current = C1

    await store.update(2, { name: 'Boubacar Updated' })

    expect(store.current).toEqual(C1) // unchanged
  })
})

describe('addPassages', () => {
  it('posts and updates balance in list and current', async () => {
    const updated = { ...C1, balance: 15 }
    api.post.mockResolvedValue({ data: updated })

    const store = useClientsStore()
    store.clients = [C1]
    store.current = C1

    const result = await store.addPassages(1, 5)

    expect(api.post).toHaveBeenCalledWith('/clients/1/add-passages', { passages: 5 })
    expect(store.clients[0].balance).toBe(15)
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

  it('does not clear current if a different client is deactivated', async () => {
    api.delete.mockResolvedValue({})

    const store = useClientsStore()
    store.clients = [C1, C2]
    store.current = C1

    await store.deactivate(2)

    expect(store.current).toEqual(C1)
  })
})
