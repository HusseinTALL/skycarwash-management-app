/**
 * Tests for src/offline/queue.js
 *
 * IndexedDB is not implemented in jsdom, so we provide a minimal in-memory
 * fake that mirrors the IDB request/event pattern used in queue.js.
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { enqueue, getAll, remove, count } from '@/offline/queue'

// ── Minimal in-memory IndexedDB fake ───────────────────────────────── //

function createFakeIDB() {
  /** All records, shared across store opens (simulates a persistent DB). */
  const data   = []
  let   nextId = 1

  function makeRequest(fn) {
    const req = { result: undefined, error: null, onsuccess: null, onerror: null }
    queueMicrotask(() => {
      try {
        req.result = fn()
        req.onsuccess?.()
      } catch (e) {
        req.error = e
        req.onerror?.()
      }
    })
    return req
  }

  const fakeStore = {
    add:    (item)   => makeRequest(() => { const id = nextId++; data.push({ ...item, localId: id }); return id }),
    getAll: ()       => makeRequest(() => [...data]),
    delete: (id)     => makeRequest(() => { const i = data.findIndex(d => d.localId === id); if (i !== -1) data.splice(i, 1) }),
    count:  ()       => makeRequest(() => data.length)
  }

  const fakeDB = {
    transaction:       () => ({ objectStore: () => fakeStore }),
    objectStoreNames:  { contains: () => false },
    createObjectStore: () => {}
  }

  return {
    open() {
      const req = { result: null, onupgradeneeded: null, onsuccess: null, onerror: null }
      queueMicrotask(() => {
        req.result = fakeDB
        if (req.onupgradeneeded) req.onupgradeneeded({ target: { result: fakeDB } })
        req.onsuccess?.()
      })
      return req
    }
  }
}

beforeEach(() => {
  // Install a fresh fake IDB before each test so state is isolated
  global.indexedDB = createFakeIDB()
})

// ── Tests ──────────────────────────────────────────────────────────── //

describe('offline queue', () => {
  it('enqueue() adds an item and returns its localId', async () => {
    const id = await enqueue({ serviceId: 1, paymentMethod: 'CASH' })
    expect(typeof id).toBe('number')
    expect(id).toBeGreaterThan(0)
  })

  it('getAll() returns all enqueued items', async () => {
    await enqueue({ serviceId: 1, paymentMethod: 'CASH' })
    await enqueue({ serviceId: 2, paymentMethod: 'ORANGE' })

    const items = await getAll()
    expect(items).toHaveLength(2)
    expect(items[0].serviceId).toBe(1)
    expect(items[1].serviceId).toBe(2)
  })

  it('enqueue() attaches a queuedAt timestamp', async () => {
    await enqueue({ serviceId: 3, paymentMethod: 'MOOV' })
    const [item] = await getAll()
    expect(item.queuedAt).toBeDefined()
    expect(typeof item.queuedAt).toBe('string')
    // Should parse as a valid ISO date
    expect(isNaN(Date.parse(item.queuedAt))).toBe(false)
  })

  it('remove() deletes an item by localId', async () => {
    const id1 = await enqueue({ serviceId: 1, paymentMethod: 'CASH' })
    const id2 = await enqueue({ serviceId: 2, paymentMethod: 'ORANGE' })

    await remove(id1)

    const items = await getAll()
    expect(items).toHaveLength(1)
    expect(items[0].localId).toBe(id2)
  })

  it('count() returns the number of items in the queue', async () => {
    expect(await count()).toBe(0)

    await enqueue({ serviceId: 1, paymentMethod: 'CASH' })
    expect(await count()).toBe(1)

    await enqueue({ serviceId: 2, paymentMethod: 'ORANGE' })
    expect(await count()).toBe(2)

    const [first] = await getAll()
    await remove(first.localId)
    expect(await count()).toBe(1)
  })

  it('getAll() returns empty array when queue is empty', async () => {
    const items = await getAll()
    expect(items).toEqual([])
  })

  it('remove() on a non-existent id leaves the queue intact', async () => {
    await enqueue({ serviceId: 1, paymentMethod: 'CASH' })
    await remove(9999)
    expect(await count()).toBe(1)
  })
})
