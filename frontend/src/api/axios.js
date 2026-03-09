import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const api = axios.create({
  baseURL: (import.meta.env.VITE_API_BASE_URL || '') + '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000
})

// Attach JWT token to every request
api.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

// Handle 401 – token expired or invalid.
// When a logged-in user's session expires mid-use, show a "session expirée"
// modal (via auth.sessionExpired) instead of silently redirecting.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const auth = useAuthStore()
      if (auth.isAuthenticated) {
        // Token expired during an active session → surface the modal
        auth.sessionExpired = true
      } else {
        // Already logged out (e.g. login page 401) → redirect directly
        auth.logout()
        router.push('/login')
      }
    }
    return Promise.reject(error)
  }
)

export default api
