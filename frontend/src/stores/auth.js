import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'

export const useAuthStore = defineStore('auth', () => {
  // ── P5.2: validated reads from localStorage ──────────────────────── //
  const token = ref(null)
  const user  = ref(null)

  try {
    const stored = localStorage.getItem('scw_token')
    if (stored) token.value = stored
  } catch { /* corrupted — ignore */ }

  try {
    const stored = localStorage.getItem('scw_user')
    if (stored) {
      const parsed = JSON.parse(stored)
      if (parsed && typeof parsed === 'object') user.value = parsed
    }
  } catch {
    localStorage.removeItem('scw_user')
  }

  // ── Computed roles ────────────────────────────────────────────────── //
  const isAuthenticated = computed(() => !!token.value)
  const role       = computed(() => user.value?.role || null)
  const isManager  = computed(() => role.value === 'MANAGER')
  const isEmployee = computed(() => role.value === 'EMPLOYEE')
  const isPartner  = computed(() => role.value === 'PARTNER')

  /** P5.1: set to true by the 401 interceptor; cleared on login */
  const sessionExpired = ref(false)

  // ── Actions ───────────────────────────────────────────────────────── //
  async function login(phone, password) {
    sessionExpired.value = false
    const { data } = await api.post('/auth/login', { phone, password })
    token.value = data.token
    user.value  = { userId: data.userId, name: data.name, role: data.role }
    localStorage.setItem('scw_token', data.token)
    localStorage.setItem('scw_user',  JSON.stringify(user.value))
  }

  function logout() {
    token.value  = null
    user.value   = null
    localStorage.removeItem('scw_token')
    localStorage.removeItem('scw_user')
  }

  return { token, user, isAuthenticated, role, isManager, isEmployee, isPartner, sessionExpired, login, logout }
})
