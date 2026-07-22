import axios from 'axios'

/**
 * Axios instance for the public client portal ("Rapports de lavage").
 * Uses a separate PORTAL token (localStorage) — never the staff JWT.
 */
const portalApi = axios.create({
  baseURL: (import.meta.env.VITE_API_BASE_URL || '') + '/api/portal',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000
})

export const PORTAL_TOKEN_KEY = 'scw_portal_token'
export const PORTAL_PHONE_KEY = 'scw_portal_phone'

portalApi.interceptors.request.use((config) => {
  const token = localStorage.getItem(PORTAL_TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default portalApi
