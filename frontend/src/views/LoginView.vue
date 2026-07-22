<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-900 px-4">
    <div class="w-full max-w-sm">
      <!-- Logo / brand -->
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-brand-500 mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-9 w-9 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
          </svg>
        </div>
        <h1 class="text-2xl font-bold text-white">SkyCarWash</h1>
        <p class="text-slate-400 text-sm mt-1">Connexion à votre espace</p>
      </div>

      <!-- Login form -->
      <form @submit.prevent="handleLogin" class="card space-y-4">
        <div>
          <label for="phone" class="block text-sm font-medium text-slate-300 mb-1">Numéro de téléphone</label>
          <input
            id="phone"
            v-model="phone"
            type="tel"
            autocomplete="tel"
            placeholder="+226 xx xx xx xx"
            class="input-field"
            :disabled="loading"
            required
          />
        </div>

        <div>
          <label for="password" class="block text-sm font-medium text-slate-300 mb-1">Mot de passe</label>
          <div class="relative">
            <input
              id="password"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="••••••••"
              class="input-field pr-12"
              :disabled="loading"
              required
            />
            <button
              type="button"
              @click="showPassword = !showPassword"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200 transition-colors"
              :aria-label="showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'"
            >
              <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            </button>
          </div>
        </div>

        <p v-if="errorMsg" class="text-red-400 text-sm text-center">{{ errorMsg }}</p>

        <button type="submit" class="btn-primary w-full" :disabled="loading">
          <span v-if="loading">Connexion...</span>
          <span v-else>Se connecter</span>
        </button>
      </form>

      <!-- Espace client (accès sans compte) -->
      <p class="text-center text-sm text-slate-500 mt-6">
        Client ?
        <RouterLink to="/rapports" class="text-brand-400 hover:text-brand-300 font-medium">
          Consulter mes rapports de lavage
        </RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { PHONE_REGEX } from '@/constants'

const auth    = useAuthStore()
const router  = useRouter()
const route   = useRoute()

const phone        = ref('')
const password     = ref('')
const showPassword = ref(false)
const loading      = ref(false)
const errorMsg     = ref('')

async function handleLogin() {
  if (loading.value) return
  errorMsg.value = ''
  if (!PHONE_REGEX.test(phone.value.trim())) {
    errorMsg.value = 'Numéro de téléphone invalide (ex : +22612345678)'
    return
  }
  loading.value  = true
  try {
    await auth.login(phone.value.trim(), password.value)
    const redirect = route.query.redirect || (auth.isEmployee ? '/caisse' : '/dashboard')
    router.push(redirect)
  } catch (err) {
    errorMsg.value = err.response?.data?.error || 'Erreur de connexion. Vérifiez vos identifiants.'
  } finally {
    loading.value = false
  }
}
</script>
