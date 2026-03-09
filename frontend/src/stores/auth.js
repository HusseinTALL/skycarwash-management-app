import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('scw_token') || null)
  const user  = ref(JSON.parse(localStorage.getItem('scw_user') || 'null'))

  const isAuthenticated = computed(() => !!token.value)
  const role = computed(() => user.value?.role || null)
  const isManager  = computed(() => role.value === 'MANAGER')
  const isEmployee = computed(() => role.value === 'EMPLOYEE')
  const isPartner  = computed(() => role.value === 'PARTNER')

  async function login(phone, password) {
    const { data } = await api.post('/auth/login', { phone, password })
    token.value = data.token
    user.value  = { userId: data.userId, name: data.name, role: data.role }
    localStorage.setItem('scw_token', data.token)
    localStorage.setItem('scw_user',  JSON.stringify(user.value))
  }

  function logout() {
    token.value = null
    user.value  = null
    localStorage.removeItem('scw_token')
    localStorage.removeItem('scw_user')
  }

  return { token, user, isAuthenticated, role, isManager, isEmployee, isPartner, login, logout }
})
