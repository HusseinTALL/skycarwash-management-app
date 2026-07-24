import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/axios'

export const useClientsStore = defineStore('clients', () => {
  const clients      = ref([])
  const loading      = ref(false)
  const current      = ref(null)   // edit form (plain ClientDto)
  const summary      = ref(null)   // Client 360 profile (stats + vehicles + history)
  const interactions = ref([])     // interaction journal of the loaded client
  const loyalty      = ref(null)   // { points, movements } of the loaded client
  const overview     = ref(null)   // CRM cockpit (segments, follow-ups, expiring…)

  // ── List / search / segmentation ─────────────────────────────────── //

  /**
   * @param {object} filters  { q, type, status, tag, segment, sort }
   */
  async function loadAll(filters = {}) {
    loading.value = true
    try {
      const params = {}
      if (filters.q && filters.q.length >= 2) params.q = filters.q
      if (filters.type)    params.type    = filters.type
      if (filters.status)  params.status  = filters.status
      if (filters.tag)     params.tag     = filters.tag
      if (filters.segment) params.segment = filters.segment
      if (filters.sort)    params.sort    = filters.sort
      const { data } = await api.get('/clients', { params })
      clients.value = data
    } finally {
      loading.value = false
    }
  }

  async function loadOverview() {
    const { data } = await api.get('/clients/crm/overview')
    overview.value = data
    return data
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

  // ── Interaction journal ──────────────────────────────────────────── //

  async function loadInteractions(clientId) {
    const { data } = await api.get(`/clients/${clientId}/interactions`)
    interactions.value = data
    return data
  }

  async function addInteraction(clientId, dto) {
    const { data } = await api.post(`/clients/${clientId}/interactions`, dto)
    interactions.value = [data, ...interactions.value]
    return data
  }

  async function markFollowUpDone(clientId, interactionId) {
    const { data } = await api.put(`/clients/${clientId}/interactions/${interactionId}/done`)
    interactions.value = interactions.value.map(i => (i.id === interactionId ? data : i))
    return data
  }

  async function deleteInteraction(clientId, interactionId) {
    await api.delete(`/clients/${clientId}/interactions/${interactionId}`)
    interactions.value = interactions.value.filter(i => i.id !== interactionId)
  }

  // ── Loyalty program ──────────────────────────────────────────────── //

  async function loadLoyalty(clientId) {
    const { data } = await api.get(`/clients/${clientId}/loyalty`)
    loyalty.value = data
    return data
  }

  async function redeemPoints(clientId, points, note) {
    const { data } = await api.post(`/clients/${clientId}/loyalty/redeem`, { points, note })
    loyalty.value = data
    if (summary.value?.client?.id === Number(clientId)) {
      summary.value = { ...summary.value, loyaltyPoints: data.points }
    }
    return data
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
    clients, loading, current, summary, interactions, loyalty, overview,
    loadAll, loadById, loadSummary, loadOverview,
    create, update, addPassages, deactivate,
    loadInteractions, addInteraction, markFollowUpDone, deleteInteraction,
    loadLoyalty, redeemPoints,
    addVehicle, updateVehicle, deleteVehicle
  }
})
