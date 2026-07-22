import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/axios'

export const useClientsStore = defineStore('clients', () => {
  const clients     = ref([])
  const loading     = ref(false)
  const current     = ref(null)   // edit form (plain ClientDto)
  const summary     = ref(null)   // Client 360 profile (stats + vehicles + history)

  // ── List / search / segmentation ─────────────────────────────────── //

  /**
   * @param {object} filters  { q, type, status, tag, sort }
   */
  async function loadAll(filters = {}) {
    loading.value = true
    try {
      const params = {}
      if (filters.q && filters.q.length >= 2) params.q = filters.q
      if (filters.type)   params.type   = filters.type
      if (filters.status) params.status = filters.status
      if (filters.tag)    params.tag    = filters.tag
      if (filters.sort)   params.sort   = filters.sort
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

  async function loadSummary(id) {
    loading.value = true
    try {
      const { data } = await api.get(`/clients/${id}/summary`)
      summary.value = data
    } finally {
      loading.value = false
    }
  }

  // ── Mutations ────────────────────────────────────────────────────── //

  async function create(dto) {
    const { data } = await api.post('/clients', dto)
    return data
  }

  async function update(id, dto) {
    const { data } = await api.put(`/clients/${id}`, dto)
    if (current.value?.id === id) current.value = data
    if (summary.value?.client?.id === id) summary.value = { ...summary.value, client: data }
    return data
  }

  async function addPassages(id, passages) {
    const { data } = await api.post(`/clients/${id}/add-passages`, { passages })
    if (current.value?.id === id) current.value = data
    if (summary.value?.client?.id === id) summary.value = { ...summary.value, client: data }
    return data
  }

  async function deactivate(id) {
    await api.delete(`/clients/${id}`)
    clients.value = clients.value.filter(c => c.id !== id)
    if (current.value?.id === id) current.value = null
  }

  // ── Vehicles ─────────────────────────────────────────────────────── //

  async function addVehicle(clientId, dto) {
    const { data } = await api.post(`/clients/${clientId}/vehicles`, dto)
    if (summary.value?.client?.id === clientId) {
      summary.value = { ...summary.value, vehicles: [...summary.value.vehicles, data] }
    }
    return data
  }

  async function updateVehicle(clientId, vehicleId, dto) {
    const { data } = await api.put(`/clients/${clientId}/vehicles/${vehicleId}`, dto)
    if (summary.value?.client?.id === clientId) {
      summary.value = {
        ...summary.value,
        vehicles: summary.value.vehicles.map(v => (v.id === vehicleId ? data : v))
      }
    }
    return data
  }

  async function deleteVehicle(clientId, vehicleId) {
    await api.delete(`/clients/${clientId}/vehicles/${vehicleId}`)
    if (summary.value?.client?.id === clientId) {
      summary.value = {
        ...summary.value,
        vehicles: summary.value.vehicles.filter(v => v.id !== vehicleId)
      }
    }
  }

  return {
    clients, loading, current, summary,
    loadAll, loadById, loadSummary,
    create, update, addPassages, deactivate,
    addVehicle, updateVehicle, deleteVehicle
  }
})
