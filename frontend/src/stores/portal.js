import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import portalApi, { PORTAL_TOKEN_KEY, PORTAL_PHONE_KEY } from '@/api/portalApi'

/**
 * Client portal ("Rapports de lavage") — login by phone + access code,
 * no staff account required.
 */
export const usePortalStore = defineStore('portal', () => {
  const token = ref(localStorage.getItem(PORTAL_TOKEN_KEY) || null)
  const phone = ref(localStorage.getItem(PORTAL_PHONE_KEY) || null)
  const mustChangeCode = ref(false)

  const isAuthenticated = computed(() => !!token.value)

  async function login(phoneInput, code) {
    const { data } = await portalApi.post('/login', { phone: phoneInput, code })
    token.value = data.token
    phone.value = data.phone
    mustChangeCode.value = data.mustChangeCode
    localStorage.setItem(PORTAL_TOKEN_KEY, data.token)
    localStorage.setItem(PORTAL_PHONE_KEY, data.phone)
    return data
  }

  async function changeCode(newCode) {
    await portalApi.post('/change-code', { newCode })
    mustChangeCode.value = false
  }

  async function fetchReports() {
    const { data } = await portalApi.get('/reports')
    return data
  }

  async function fetchReport(id) {
    const { data } = await portalApi.get(`/reports/${id}`)
    return data
  }

  function logout() {
    token.value = null
    phone.value = null
    mustChangeCode.value = false
    localStorage.removeItem(PORTAL_TOKEN_KEY)
    localStorage.removeItem(PORTAL_PHONE_KEY)
  }

  return {
    token, phone, mustChangeCode, isAuthenticated,
    login, changeCode, fetchReports, fetchReport, logout
  }
})
