<template>
  <div class="min-h-screen grid place-items-center px-4 py-10">
    <div class="w-full max-w-sm scw-animate-in">
      <!-- Brand -->
      <div class="text-center mb-8">
        <div class="inline-grid place-items-center w-16 h-16 rounded-2xl bg-primary text-slate-950 mb-4 shadow-xl shadow-primary/25">
          <i class="pi pi-sun text-3xl" />
        </div>
        <h1 class="text-2xl font-bold tracking-tight text-slate-100">SkyCarWash</h1>
        <p class="text-slate-400 text-sm mt-1">Connexion à votre espace</p>
      </div>

      <div class="scw-panel p-6 sm:p-7">
        <form @submit.prevent="handleLogin" class="space-y-5">
          <div>
            <label for="phone" class="block text-sm font-medium text-slate-300 mb-1.5">Numéro de téléphone</label>
            <IconField>
              <InputIcon class="pi pi-phone" />
              <InputText
                id="phone"
                v-model="phone"
                type="tel"
                autocomplete="tel"
                placeholder="+226 xx xx xx xx"
                class="w-full"
                :disabled="loading"
                :invalid="!!errorMsg"
              />
            </IconField>
          </div>

          <div>
            <label for="password" class="block text-sm font-medium text-slate-300 mb-1.5">Mot de passe</label>
            <Password
              id="password"
              v-model="password"
              :feedback="false"
              toggle-mask
              autocomplete="current-password"
              placeholder="••••••••"
              class="w-full"
              input-class="w-full"
              :disabled="loading"
              :invalid="!!errorMsg"
            />
          </div>

          <Message v-if="errorMsg" severity="error" :closable="false" size="small">{{ errorMsg }}</Message>

          <Button
            type="submit"
            label="Se connecter"
            icon="pi pi-sign-in"
            class="w-full"
            :loading="loading"
          />
        </form>
      </div>

      <p class="text-center text-sm text-slate-500 mt-6">
        Client ?
        <RouterLink to="/rapports" class="text-primary hover:underline font-medium">
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

import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const phone = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  if (loading.value) return
  errorMsg.value = ''
  if (!PHONE_REGEX.test(phone.value.trim())) {
    errorMsg.value = 'Numéro de téléphone invalide (ex : +22612345678)'
    return
  }
  loading.value = true
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
