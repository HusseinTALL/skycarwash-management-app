<template>
  <Teleport to="body">
    <div class="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 px-4">
      <div class="bg-slate-800 rounded-2xl w-full max-w-sm p-6 space-y-4 text-center shadow-xl">

        <!-- Icon -->
        <div class="flex justify-center">
          <div class="w-14 h-14 rounded-full bg-amber-900/40 flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-7 w-7 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 15v2m0 0v2m0-2h2m-2 0H10m2-6V9m0 0V7m0 2h2m-2 0H10m10 3a8 8 0 11-16 0 8 8 0 0116 0z" />
            </svg>
          </div>
        </div>

        <div>
          <h3 class="text-lg font-bold text-white">Session expirée</h3>
          <p class="text-slate-400 text-sm mt-1">
            Votre session a expiré. Veuillez vous reconnecter pour continuer.
          </p>
        </div>

        <button
          @click="reconnect"
          class="btn-primary w-full py-3"
        >
          Se reconnecter
        </button>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth   = useAuthStore()
const router = useRouter()

function reconnect() {
  auth.logout()
  router.push('/login')
}
</script>
