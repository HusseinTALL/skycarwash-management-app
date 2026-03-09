// Global test setup for Vitest
// Runs before every test file

// Mock localStorage
const localStorageMock = (() => {
  let store = {}
  return {
    getItem: (key) => store[key] ?? null,
    setItem: (key, value) => { store[key] = String(value) },
    removeItem: (key) => { delete store[key] },
    clear: () => { store = {} }
  }
})()

Object.defineProperty(window, 'localStorage', { value: localStorageMock })

// Suppress noisy console.warn/error in tests unless explicitly tested
vi.spyOn(console, 'warn').mockImplementation(() => {})
