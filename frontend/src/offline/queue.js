/**
 * Offline transaction queue backed by IndexedDB.
 * When the device is offline, pending transactions are stored here
 * and synced to the API when connectivity is restored.
 */

const DB_NAME    = 'skycarwash-offline'
const DB_VERSION = 1
const STORE      = 'tx-queue'

function openDB() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)

    req.onupgradeneeded = (event) => {
      const db = event.target.result
      if (!db.objectStoreNames.contains(STORE)) {
        db.createObjectStore(STORE, { keyPath: 'localId', autoIncrement: true })
      }
    }

    req.onsuccess = () => resolve(req.result)
    req.onerror   = () => reject(req.error)
  })
}

/** Add a pending transaction to the queue. */
export async function enqueue(txRequest) {
  const db    = await openDB()
  const store = db.transaction(STORE, 'readwrite').objectStore(STORE)
  return new Promise((resolve, reject) => {
    const req = store.add({ ...txRequest, queuedAt: new Date().toISOString() })
    req.onsuccess = () => resolve(req.result)
    req.onerror   = () => reject(req.error)
  })
}

/** Return all pending queued transactions (oldest first). */
export async function getAll() {
  const db    = await openDB()
  const store = db.transaction(STORE, 'readonly').objectStore(STORE)
  return new Promise((resolve, reject) => {
    const req = store.getAll()
    req.onsuccess = () => resolve(req.result)
    req.onerror   = () => reject(req.error)
  })
}

/** Remove a queued item by its localId after successful sync. */
export async function remove(localId) {
  const db    = await openDB()
  const store = db.transaction(STORE, 'readwrite').objectStore(STORE)
  return new Promise((resolve, reject) => {
    const req = store.delete(localId)
    req.onsuccess = () => resolve()
    req.onerror   = () => reject(req.error)
  })
}

/** Count items in the queue. */
export async function count() {
  const db    = await openDB()
  const store = db.transaction(STORE, 'readonly').objectStore(STORE)
  return new Promise((resolve, reject) => {
    const req = store.count()
    req.onsuccess = () => resolve(req.result)
    req.onerror   = () => reject(req.error)
  })
}
