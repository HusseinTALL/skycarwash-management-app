import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/axios'

export const useClientsStore = defineStore('clients', () => {
  const clients     = ref([])
  const loading     = ref(false)
  const current     = ref(null)   // detail view client

  // ── List / search ────────────────────────────────────────────────── //

  async function loadAll(q = '') {
    loading.value = true
    try {
      const params = q.length >= 2 ? { q } : {}
      const { data } = await api.get('/clients', { params })
      clients.value = data
    } finally {
      loading.value = false
    }
  }

  async function loadById(id) {
    loading.value = true
    try {
      const { data } = await api.get(`/clients/${id}`)
      current.value = data
    } finally {
      loading.value = false
    }
  }

  // ── Mutations ────────────────────────────────────────────────────── //

  async function create(dto) {
    const { data } = await api.post('/clients', dto)
    clients.value.unshift(data)
    return data
  }

  async function update(id, dto) {
    const { data } = await api.put(`/clients/${id}`, dto)
    const idx = clients.value.findIndex(c => c.id === id)
    if (idx !== -1) clients.value[idx] = data
    if (current.value?.id === id) current.value = data
    return data
  }

  async function addPassages(id, passages) {
    const { data } = await api.post(`/clients/${id}/add-passages`, { passages })
    const idx = clients.value.findIndex(c => c.id === id)
    if (idx !== -1) clients.value[idx] = data
    if (current.value?.id === id) current.value = data
    return data
  }

  async function deactivate(id) {
    await api.delete(`/clients/${id}`)
    clients.value = clients.value.filter(c => c.id !== id)
    if (current.value?.id === id) current.value = null
  }

  return {
    clients, loading, current,
    loadAll, loadById, create, update, addPassages, deactivate
  }
})
