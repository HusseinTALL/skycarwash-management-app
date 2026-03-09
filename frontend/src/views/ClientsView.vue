<template>
  <div class="p-4 space-y-4">

    <!-- Header -->
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-bold">Clients</h2>
      <RouterLink to="/clients/new" class="btn-primary py-2 px-4 text-sm">+ Nouveau</RouterLink>
    </div>

    <!-- Search -->
    <input
      v-model="query"
      type="search"
      placeholder="Rechercher par nom ou téléphone..."
      class="input-field"
      @input="onSearch"
    />

    <!-- Loading -->
    <div v-if="store.loading" class="text-center py-10 text-slate-400 text-sm">
      Chargement...
    </div>

    <!-- Empty -->
    <div v-else-if="store.clients.length === 0" class="card text-center py-12 text-slate-400">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 mx-auto mb-3 opacity-40" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
      </svg>
      <p class="font-medium">Aucun client</p>
      <p class="text-sm mt-1">{{ query ? 'Aucun résultat pour cette recherche' : 'Ajoutez votre premier client' }}</p>
    </div>

    <!-- Client list -->
    <div v-else class="space-y-2">
      <RouterLink
        v-for="client in store.clients"
        :key="client.id"
        :to="`/clients/${client.id}`"
        class="card block hover:bg-slate-700 active:scale-[0.99] transition-all duration-100"
      >
        <div class="flex items-center justify-between">
          <div class="min-w-0">
            <div class="flex items-center gap-2">
              <p class="font-semibold truncate">{{ client.name }}</p>
              <span
                class="text-xs px-2 py-0.5 rounded-full shrink-0"
                :class="{
                  'bg-blue-900 text-blue-300':     client.type === 'CARTE',
                  'bg-purple-900 text-purple-300': client.type === 'BOUCLIER',
                  'bg-amber-900 text-amber-300':   client.type === 'VIP'
                }"
              >{{ client.type }}</span>
            </div>
            <p class="text-sm text-slate-400 mt-0.5">{{ client.phone }}</p>
          </div>

          <div class="text-right shrink-0 ml-3">
            <!-- Balance badge -->
            <p
              class="text-sm font-semibold"
              :class="client.balance <= 1 ? 'text-red-400' : 'text-green-400'"
            >
              {{ client.balance }} passage{{ client.balance !== 1 ? 's' : '' }}
            </p>
            <!-- Expiry -->
            <p v-if="client.expiresAt" class="text-xs mt-0.5" :class="expiryClass(client.expiresAt)">
              exp. {{ formatDate(client.expiresAt) }}
            </p>
          </div>
        </div>

        <!-- Alert strip -->
        <div
          v-if="needsAlert(client)"
          class="mt-2 text-xs text-amber-300 flex items-center gap-1"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
          </svg>
          {{ alertMessage(client) }}
        </div>
      </RouterLink>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useClientsStore } from '@/stores/clients'

const store = useClientsStore()
const query = ref('')
let debounce

onMounted(() => store.loadAll())

function onSearch() {
  clearTimeout(debounce)
  debounce = setTimeout(() => store.loadAll(query.value), 300)
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: '2-digit' })
}

function expiryClass(iso) {
  const days = Math.ceil((new Date(iso) - Date.now()) / 86_400_000)
  if (days <= 0) return 'text-red-400'
  if (days <= 5) return 'text-amber-400'
  return 'text-slate-400'
}

function needsAlert(client) {
  if (client.type === 'CARTE' && client.balance <= 1) return true
  if (client.expiresAt) {
    const days = Math.ceil((new Date(client.expiresAt) - Date.now()) / 86_400_000)
    if (days <= 5) return true
  }
  return false
}

function alertMessage(client) {
  const msgs = []
  if (client.type === 'CARTE' && client.balance <= 1) {
    msgs.push(client.balance === 0 ? 'Plus de passages' : '1 passage restant')
  }
  if (client.expiresAt) {
    const days = Math.ceil((new Date(client.expiresAt) - Date.now()) / 86_400_000)
    if (days <= 0) msgs.push('Abonnement expiré')
    else if (days <= 5) msgs.push(`Expire dans ${days} j`)
  }
  return msgs.join(' · ')
}
</script>
