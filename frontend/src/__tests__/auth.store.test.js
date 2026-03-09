import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

// Mock the axios api module
vi.mock('@/api/axios', () => ({
  default: {
    post: vi.fn()
  }
}))

import api from '@/api/axios'

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('initialises unauthenticated when localStorage is empty', () => {
    const auth = useAuthStore()
    expect(auth.isAuthenticated).toBe(false)
    expect(auth.token).toBeNull()
    expect(auth.user).toBeNull()
  })

  it('sets token and user on successful login', async () => {
    api.post.mockResolvedValue({
      data: { token: 'test-jwt', userId: 1, name: 'Manager', role: 'MANAGER' }
    })

    const auth = useAuthStore()
    await auth.login('+22600000000', 'Admin1234!')

    expect(auth.token).toBe('test-jwt')
    expect(auth.user.role).toBe('MANAGER')
    expect(auth.isAuthenticated).toBe(true)
    expect(auth.isManager).toBe(true)
    expect(localStorage.getItem('scw_token')).toBe('test-jwt')
  })

  it('computes role flags correctly for EMPLOYEE', async () => {
    api.post.mockResolvedValue({
      data: { token: 'emp-jwt', userId: 2, name: 'John', role: 'EMPLOYEE' }
    })

    const auth = useAuthStore()
    await auth.login('+22600000001', 'pass')

    expect(auth.isEmployee).toBe(true)
    expect(auth.isManager).toBe(false)
    expect(auth.isPartner).toBe(false)
  })

  it('clears state and localStorage on logout', async () => {
    api.post.mockResolvedValue({
      data: { token: 'test-jwt', userId: 1, name: 'Manager', role: 'MANAGER' }
    })

    const auth = useAuthStore()
    await auth.login('+22600000000', 'Admin1234!')
    auth.logout()

    expect(auth.isAuthenticated).toBe(false)
    expect(auth.token).toBeNull()
    expect(auth.user).toBeNull()
    expect(localStorage.getItem('scw_token')).toBeNull()
  })

  it('propagates API errors on failed login', async () => {
    api.post.mockRejectedValue(new Error('Invalid credentials'))

    const auth = useAuthStore()
    await expect(auth.login('+22600000000', 'wrong')).rejects.toThrow('Invalid credentials')
    expect(auth.isAuthenticated).toBe(false)
  })
})
