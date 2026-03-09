<template>
  <div class="fixed inset-0 z-50 flex flex-col bg-slate-900">
    <!-- Header -->
    <div class="flex items-center gap-3 px-4 py-3 bg-slate-800 border-b border-slate-700">
      <button @click="emit('close')" class="text-slate-400 hover:text-slate-200 min-h-0 min-w-0 p-1">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <h3 class="font-semibold">Sélectionner un client</h3>
    </div>

    <!-- Search input -->
    <div class="px-4 pt-4 pb-2">
      <input
        v-model="query"
        ref="searchInput"
        type="search"
        placeholder="Nom ou numéro de téléphone..."
        class="input-field"
        @input="onInput"
      />
    </div>

    <!-- Results -->
    <div class="flex-1 overflow-y-auto px-4 py-2 space-y-2">
      <p v-if="loading" class="text-center text-slate-400 py-8 text-sm">Recherche...</p>

      <p v-else-if="query.length >= 2 && results.length === 0"
         class="text-center text-slate-400 py-8 text-sm">
        Aucun client trouvé
      </p>

      <p v-else-if="query.length < 2"
         class="text-center text-slate-400 py-8 text-sm">
        Tapez au moins 2 caractères
      </p>

      <button
        v-for="client in results"
        :key="client.id"
        @click="emit('select', client)"
        class="w-full card text-left hover:bg-slate-700 active:scale-[0.98] transition-all duration-100"
      >
        <div class="flex items-center justify-between">
          <div>
            <p class="font-semibold">{{ client.name }}</p>
            <p class="text-sm text-slate-400">{{ client.phone }}</p>
          </div>
          <div class="text-right">
            <span class="text-xs px-2 py-0.5 rounded-full"
              :class="{
                'bg-blue-900 text-blue-300':   client.type === 'CARTE',
                'bg-purple-900 text-purple-300': client.type === 'BOUCLIER',
                'bg-amber-900 text-amber-300':  client.type === 'VIP'
              }"
            >{{ client.type }}</span>
            <p class="text-sm mt-1" :class="client.balance <= 1 ? 'text-red-400' : 'text-green-400'">
              {{ client.balance }} passage{{ client.balance !== 1 ? 's' : '' }}
            </p>
          </div>
        </div>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'

const emit = defineEmits(['select', 'close'])

const query       = ref('')
const results     = ref([])
const loading     = ref(false)
const searchInput = ref(null)
let debounce

onMounted(() => searchInput.value?.focus())

function onInput() {
  clearTimeout(debounce)
  if (query.value.length < 2) { results.value = []; return }
  debounce = setTimeout(doSearch, 300)
}

async function doSearch() {
  loading.value = true
  try {
    const { data } = await api.get('/clients/search', { params: { q: query.value } })
    results.value = data
  } catch {
    results.value = []
  } finally {
    loading.value = false
  }
}
</script>
