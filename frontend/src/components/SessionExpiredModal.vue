<template>
  <Dialog :visible="true" modal :closable="false" :draggable="false" class="w-full max-w-sm mx-3" :show-header="false">
    <div class="text-center space-y-4 py-2">
      <div class="flex justify-center">
        <span class="grid place-items-center w-14 h-14 rounded-full bg-amber-500/15 text-amber-400">
          <i class="pi pi-lock text-2xl" />
        </span>
      </div>
      <div>
        <h3 class="text-lg font-bold text-slate-100">Session expirée</h3>
        <p class="text-slate-400 text-sm mt-1">
          Votre session a expiré. Veuillez vous reconnecter pour continuer.
        </p>
      </div>
      <Button label="Se reconnecter" icon="pi pi-sign-in" class="w-full" @click="reconnect" />
    </div>
  </Dialog>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'

const auth = useAuthStore()
const router = useRouter()

function reconnect() {
  auth.logout()
  router.push('/login')
}
</script>
