<template>
  <div class="min-h-screen grid place-items-center px-4 py-10">
    <div class="w-full max-w-sm scw-animate-in">
      <div class="text-center mb-8">
        <div class="inline-grid place-items-center w-16 h-16 rounded-2xl bg-primary text-slate-950 mb-4 shadow-xl shadow-primary/25">
          <i class="pi pi-shield text-3xl" />
        </div>
        <h1 class="text-2xl font-bold tracking-tight text-slate-100">Rapports de lavage</h1>
        <p class="text-slate-400 text-sm mt-1">Consultez l'état de vos véhicules</p>
      </div>

      <div class="scw-panel p-6 sm:p-7">
        <form class="space-y-5" @submit.prevent="submit">
          <div>
            <label class="block text-sm font-medium text-slate-300 mb-1.5">Numéro de téléphone</label>
            <IconField>
              <InputIcon class="pi pi-phone" />
              <InputText v-model="phone" type="tel" autocomplete="tel" placeholder="70 12 34 56" class="w-full" :disabled="loading" :invalid="!!errorMsg" />
            </IconField>
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-300 mb-1.5">Code d'accès</label>
            <InputText v-model="code" inputmode="numeric" placeholder="••••" class="w-full tracking-[0.4em]" :disabled="loading" :invalid="!!errorMsg" />
            <p class="text-xs text-slate-500 mt-1.5">Par défaut : les 4 derniers chiffres de votre numéro.</p>
          </div>

          <Message v-if="errorMsg" severity="error" :closable="false" size="small">{{ errorMsg }}</Message>

          <Button type="submit" label="Se connecter" icon="pi pi-sign-in" class="w-full" :loading="loading" />
        </form>
      </div>

      <p class="text-center text-xs text-slate-600 mt-6">SkyCarWash · Espace client</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePortalStore } from '@/stores/portal'

import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Message from 'primevue/message'

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
