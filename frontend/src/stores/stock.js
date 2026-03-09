import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'

export const useStockStore = defineStore('stock', () => {

  const products  = ref([])
  const loading   = ref(false)

  // ── Derived ──────────────────────────────────────────────────────── //

  const lowStockProducts = computed(() =>
    products.value.filter(p => Number(p.stock) <= Number(p.alertThreshold))
  )

  const lowStockCount = computed(() => lowStockProducts.value.length)

  /** Stock level ratio [0–1] for a product's progress bar. */
  function stockRatio(product) {
    const threshold = Number(product.alertThreshold)
    if (threshold === 0) return 1
    // Full bar = 3× threshold; below threshold = red zone
    return Math.min(Number(product.stock) / (threshold * 3), 1)
  }

  /** Tailwind colour class based on stock vs threshold. */
  function stockColor(product) {
    const stock     = Number(product.stock)
    const threshold = Number(product.alertThreshold)
    if (stock <= threshold)         return 'bg-red-500'
    if (stock <= threshold * 2)     return 'bg-amber-500'
    return 'bg-green-500'
  }

  function isLow(product) {
    return Number(product.stock) <= Number(product.alertThreshold)
  }

  // ── API ───────────────────────────────────────────────────────────── //

  async function loadAll() {
    loading.value = true
    try {
      const { data } = await api.get('/products')
      products.value = data
    } finally {
      loading.value = false
    }
  }

  async function create(dto) {
    const { data } = await api.post('/products', dto)
    products.value.push(data)
    // Keep sorted by name
    products.value.sort((a, b) => a.name.localeCompare(b.name))
    return data
  }

  async function update(id, dto) {
    const { data } = await api.put(`/products/${id}`, dto)
    const idx = products.value.findIndex(p => p.id === id)
    if (idx !== -1) products.value[idx] = data
    return data
  }

  async function restock(id, quantity) {
    const { data } = await api.post(`/products/${id}/restock`, { quantity })
    const idx = products.value.findIndex(p => p.id === id)
    if (idx !== -1) products.value[idx] = data
    return data
  }

  async function deactivate(id) {
    await api.delete(`/products/${id}`)
    products.value = products.value.filter(p => p.id !== id)
  }

  return {
    products, loading,
    lowStockProducts, lowStockCount,
    stockRatio, stockColor, isLow,
    loadAll, create, update, restock, deactivate
  }
})
