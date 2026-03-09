import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/axios', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() }
}))

import api from '@/api/axios'
import { useStockStore } from '@/stores/stock'

const P1 = { id: 1, name: 'Shampooing', stock: '10', alertThreshold: '3', unit: 'L',    active: true }
const P2 = { id: 2, name: 'Cire',       stock: '2',  alertThreshold: '5', unit: 'unité', active: true }

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
})

describe('loadAll', () => {
  it('fetches products and stores them', async () => {
    api.get.mockResolvedValue({ data: [P1, P2] })
    const store = useStockStore()

    await store.loadAll()

    expect(api.get).toHaveBeenCalledWith('/products')
    expect(store.products).toHaveLength(2)
    expect(store.loading).toBe(false)
  })
})

describe('create', () => {
  it('posts and adds product sorted by name', async () => {
    const newProd = { id: 3, name: 'Alcool', stock: '5', alertThreshold: '2', unit: 'L', active: true }
    api.post.mockResolvedValue({ data: newProd })

    const store = useStockStore()
    store.products = [P1, P2]

    const result = await store.create({ name: 'Alcool', stock: 5, alertThreshold: 2, unit: 'L' })

    expect(api.post).toHaveBeenCalledWith('/products', expect.objectContaining({ name: 'Alcool' }))
    expect(result).toEqual(newProd)
    // List should be sorted alphabetically: Alcool, Cire, Shampooing
    expect(store.products[0].name).toBe('Alcool')
  })
})

describe('update', () => {
  it('puts and replaces the product in the list', async () => {
    const updated = { ...P1, stock: '20' }
    api.put.mockResolvedValue({ data: updated })

    const store = useStockStore()
    store.products = [P1, P2]

    await store.update(1, { stock: 20 })

    expect(api.put).toHaveBeenCalledWith('/products/1', { stock: 20 })
    expect(store.products.find(p => p.id === 1).stock).toBe('20')
  })
})

describe('restock', () => {
  it('posts restock and updates product stock', async () => {
    const restocked = { ...P1, stock: '15' }
    api.post.mockResolvedValue({ data: restocked })

    const store = useStockStore()
    store.products = [P1, P2]

    const result = await store.restock(1, 5)

    expect(api.post).toHaveBeenCalledWith('/products/1/restock', { quantity: 5 })
    expect(store.products[0].stock).toBe('15')
    expect(result.stock).toBe('15')
  })
})

describe('deactivate', () => {
  it('deletes and removes product from list', async () => {
    api.delete.mockResolvedValue({})

    const store = useStockStore()
    store.products = [P1, P2]

    await store.deactivate(1)

    expect(api.delete).toHaveBeenCalledWith('/products/1')
    expect(store.products).toHaveLength(1)
    expect(store.products[0]).toEqual(P2)
  })
})

// ── Computed helpers ────────────────────────────────────────────────── //

describe('isLow', () => {
  it('returns true when stock <= alertThreshold', () => {
    const store = useStockStore()
    expect(store.isLow(P2)).toBe(true)   // stock=2, threshold=5
    expect(store.isLow(P1)).toBe(false)  // stock=10, threshold=3
  })
})

describe('stockColor', () => {
  it('returns red when stock <= threshold', () => {
    const store = useStockStore()
    expect(store.stockColor(P2)).toBe('bg-red-500')
  })

  it('returns amber when stock <= 2× threshold', () => {
    const store = useStockStore()
    const p = { stock: '5', alertThreshold: '3' } // 5 <= 6 (2×3)
    expect(store.stockColor(p)).toBe('bg-amber-500')
  })

  it('returns green when stock > 2× threshold', () => {
    const store = useStockStore()
    expect(store.stockColor(P1)).toBe('bg-green-500') // 10 > 6
  })
})

describe('stockRatio', () => {
  it('returns 1 when threshold is 0', () => {
    const store = useStockStore()
    expect(store.stockRatio({ stock: '5', alertThreshold: '0' })).toBe(1)
  })

  it('clamps to 1 when stock exceeds 3× threshold', () => {
    const store = useStockStore()
    expect(store.stockRatio({ stock: '100', alertThreshold: '3' })).toBe(1)
  })

  it('calculates ratio between 0 and 1', () => {
    const store = useStockStore()
    // stock=3, threshold=3 → ratio = 3/(3*3) = 1/3
    const ratio = store.stockRatio({ stock: '3', alertThreshold: '3' })
    expect(ratio).toBeCloseTo(1 / 3)
  })
})

describe('lowStockCount', () => {
  it('counts products at or below their threshold', async () => {
    api.get.mockResolvedValue({ data: [P1, P2] })
    const store = useStockStore()
    await store.loadAll()
    // P1: stock=10, threshold=3 → not low
    // P2: stock=2,  threshold=5 → low
    expect(store.lowStockCount).toBe(1)
  })
})
