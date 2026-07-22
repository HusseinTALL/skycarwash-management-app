<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-900 px-4 py-8">
    <div class="w-full max-w-sm">
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-brand-500 mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-9 w-9 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <h1 class="text-2xl font-bold text-white">Rapports de lavage</h1>
        <p class="text-slate-400 text-sm mt-1">Consultez l'état de vos véhicules</p>
      </div>

      <form @submit.prevent="submit" class="card space-y-4">
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1">Numéro de téléphone</label>
          <input v-model="phone" type="tel" autocomplete="tel" placeholder="70 12 34 56" class="input-field" :disabled="loading" required />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1">Code d'accès</label>
          <input v-model="code" inputmode="numeric" placeholder="••••" class="input-field tracking-widest" :disabled="loading" required />
          <p class="text-xs text-slate-500 mt-1">Par défaut : les 4 derniers chiffres de votre numéro.</p>
        </div>

        <p v-if="errorMsg" class="text-red-400 text-sm text-center">{{ errorMsg }}</p>

        <button type="submit" class="btn-primary w-full" :disabled="loading">
          <span v-if="loading">Connexion…</span>
          <span v-else>Se connecter</span>
        </button>
      </form>

      <p class="text-center text-xs text-slate-600 mt-6">SkyCarWash · Espace client</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePortalStore } from '@/stores/portal'

const router = useRouter()
const portal = usePortalStore()

const phone = ref('')
const code = ref('')
const loading = ref(false)
const errorMsg = ref('')

onMounted(() => {
  if (portal.isAuthenticated) router.replace('/rapports/espace')
})

async function submit() {
  if (loading.value) return
  errorMsg.value = ''
  loading.value = true
  try {
    await portal.login(phone.value.trim(), code.value.trim())
    router.replace('/rapports/espace')
  } catch (err) {
    errorMsg.value = err.response?.data?.error || 'Numéro ou code incorrect.'
  } finally {
    loading.value = false
  }
}
</script>
