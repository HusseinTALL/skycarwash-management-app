import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/main.css'

// ── Environment validation ─────────────────────────────────────────────────
// Warn early in development if critical env vars are missing.
if (import.meta.env.PROD && !import.meta.env.VITE_API_BASE_URL) {
  console.warn(
    '[SkyCarWash] VITE_API_BASE_URL is not set. ' +
    'API calls will fall back to relative /api paths. ' +
    'Set this variable in your Vercel project settings.'
  )
}

// ── Vue app setup ──────────────────────────────────────────────────────────
const app = createApp(App)

app.use(createPinia())
app.use(router)

// ── Global error handler ───────────────────────────────────────────────────
// Catches unhandled Vue component errors. Wire Sentry (or similar) here.
app.config.errorHandler = (err, instance, info) => {
  console.error('[SkyCarWash] Unhandled Vue error:', err, '\nComponent info:', info)

  // Sentry integration point:
  // if (window.Sentry) Sentry.captureException(err, { extra: { info } })
}

// Catch unhandled promise rejections (network errors, etc.)
window.addEventListener('unhandledrejection', (event) => {
  console.error('[SkyCarWash] Unhandled promise rejection:', event.reason)
  // Sentry integration point:
  // if (window.Sentry) Sentry.captureException(event.reason)
})

app.mount('#app')
