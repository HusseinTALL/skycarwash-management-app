<template>
  <div class="p-4 space-y-4 pb-24">

    <h2 class="text-xl font-bold">Historique des transactions</h2>

    <!-- Filters -->
    <div class="card space-y-3">
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="block text-xs text-slate-400 mb-1">Du</label>
          <input v-model="filters.from" type="date" class="input-field" @change="resetAndLoad" />
        </div>
        <div>
          <label class="block text-xs text-slate-400 mb-1">Au</label>
          <input v-model="filters.to" type="date" class="input-field" @change="resetAndLoad" />
        </div>
      </div>

      <div>
        <label class="block text-xs text-slate-400 mb-1">Mode de paiement</label>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="opt in METHOD_OPTS"
            :key="opt.value"
            @click="selectMethod(opt.value)"
            class="text-xs px-3 py-1.5 rounded-full transition-colors"
            :class="filters.method === opt.value
              ? 'bg-brand-500 text-white'
              : 'bg-slate-700 text-slate-300 hover:bg-slate-600'"
          >
            {{ opt.label }}
          </button>
        </div>
      </div>

      <div class="flex justify-end">
        <button
          @click="resetFilters"
          class="text-xs text-slate-400 hover:text-slate-200 underline transition-colors"
        >
          Réinitialiser les filtres
        </button>
      </div>
    </div>

    <!-- Loading skeleton -->
    <div v-if="loading" class="space-y-2">
      <div v-for="n in 5" :key="n" class="card animate-pulse">
        <div class="flex justify-between items-start">
          <div class="space-y-2 flex-1">
            <div class="h-4 bg-slate-700 rounded w-2/5"></div>
            <div class="h-3 bg-slate-700 rounded w-1/3"></div>
          </div>
          <div class="h-5 bg-slate-700 rounded w-20"></div>
        </div>
      </div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="card text-center py-8 text-red-400">
      <p>{{ error }}</p>
      <button @click="load" class="mt-3 text-sm text-sky-400 underline">Réessayer</button>
    </div>

    <!-- Empty -->
    <div v-else-if="transactions.length === 0" class="card text-center py-12 text-slate-400">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 mx-auto mb-3 opacity-40" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
      </svg>
      <p class="font-medium">Aucune transaction</p>
      <p class="text-sm mt-1">Modifiez les filtres pour voir d'autres résultats</p>
    </div>

    <!-- Transaction list -->
    <div v-else class="space-y-2">
      <div
        v-for="tx in transactions"
        :key="tx.id"
        class="card space-y-1.5"
        :class="tx.cancelledAt ? 'opacity-60' : ''"
      >
        <div class="flex items-start justify-between gap-2">
          <div class="min-w-0">
            <div class="flex items-center gap-2 flex-wrap">
              <p class="font-semibold" :class="tx.cancelledAt ? 'line-through text-slate-400' : 'text-white'">
                {{ tx.serviceName }}
              </p>
              <span
                v-if="tx.cancelledAt"
                class="text-xs px-2 py-0.5 rounded-full bg-red-500/20 text-red-400 shrink-0"
              >Annulée</span>
            </div>
            <div class="flex items-center gap-2 mt-0.5 text-xs text-slate-400 flex-wrap">
              <span>{{ formatDateTime(tx.createdAt) }}</span>
              <span>·</span>
              <span>{{ tx.userName }}</span>
              <template v-if="tx.clientName">
                <span>·</span>
                <span class="text-sky-400">{{ tx.clientName }}</span>
              </template>
            </div>
          </div>

          <div class="text-right shrink-0">
            <p class="font-bold text-white">{{ tx.amount.toLocaleString('fr-FR') }} FCFA</p>
            <span
              class="text-xs px-2 py-0.5 rounded-full"
              :class="methodClass(tx.paymentMethod)"
            >{{ PAYMENT_LABELS[tx.paymentMethod] || tx.paymentMethod }}</span>
          </div>
        </div>

        <p v-if="tx.cancelledAt && tx.cancelReason" class="text-xs text-red-400 border-t border-slate-700 pt-1.5">
          Motif : {{ tx.cancelReason }}
        </p>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-between text-sm pt-1">
        <button
          @click="prevPage"
          :disabled="page === 0"
          class="px-3 py-1.5 rounded-lg bg-slate-700 text-slate-300 disabled:opacity-40 hover:bg-slate-600 transition-colors"
        >
          ← Préc.
        </button>
        <span class="text-slate-400 text-xs">{{ page + 1 }} / {{ totalPages }} · {{ totalElements }} transaction{{ totalElements !== 1 ? 's' : '' }}</span>
        <button
          @click="nextPage"
          :disabled="page >= totalPages - 1"
          class="px-3 py-1.5 rounded-lg bg-slate-700 text-slate-300 disabled:opacity-40 hover:bg-slate-600 transition-colors"
        >
          Suiv. →
        </button>
      </div>

      <!-- Total summary -->
      <div v-if="transactions.length > 0" class="card bg-slate-700/50 flex justify-between items-center">
        <span class="text-sm text-slate-400">Total (cette page)</span>
        <span class="font-bold text-white">{{ pageTotal.toLocaleString('fr-FR') }} FCFA</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/api/axios'
import { PAYMENT_LABELS } from '@/constants'

const METHOD_OPTS = [
  { value: '',           label: 'Tous' },
  { value: 'CASH',       label: 'Espèces' },
  { value: 'ORANGE',     label: 'Orange Money' },
  { value: 'MOOV',       label: 'Moov Money' },
  { value: 'ABONNEMENT', label: 'Abonnement' }
]

// Default: today
const todayIso = new Date().toISOString().slice(0, 10)

const filters = ref({ from: todayIso, to: todayIso, method: '' })
const transactions  = ref([])
const page          = ref(0)
const totalPages    = ref(0)
const totalElements = ref(0)
const loading       = ref(false)
const error         = ref('')

const pageTotal = computed(() =>
  transactions.value
    .filter(tx => !tx.cancelledAt)
    .reduce((sum, tx) => sum + tx.amount, 0)
)

async function load() {
  loading.value = true
  error.value   = ''
  try {
    const params = {
      from: filters.value.from || undefined,
      to:   filters.value.to   || undefined,
      page: page.value,
      size: 20
    }
    if (filters.value.method) params.method = filters.value.method

    const { data } = await api.get('/transactions/history', { params })
    transactions.value  = data.content
    totalPages.value    = data.totalPages
    totalElements.value = data.totalElements
  } catch (err) {
    error.value = err.response?.data?.error || 'Impossible de charger les transactions.'
  } finally {
    loading.value = false
  }
}

function resetAndLoad() {
  page.value = 0
  load()
}

function selectMethod(val) {
  filters.value.method = val
  resetAndLoad()
}

function resetFilters() {
  filters.value = { from: todayIso, to: todayIso, method: '' }
  resetAndLoad()
}

function prevPage() {
  if (page.value > 0) { page.value--; load() }
}

function nextPage() {
  if (page.value < totalPages.value - 1) { page.value++; load() }
}

function formatDateTime(iso) {
  return new Date(iso).toLocaleString('fr-FR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

function methodClass(method) {
  return {
    CASH:       'bg-emerald-500/20 text-emerald-400',
    ORANGE:     'bg-orange-500/20 text-orange-400',
    MOOV:       'bg-blue-500/20 text-blue-400',
    ABONNEMENT: 'bg-purple-500/20 text-purple-400'
  }[method] || 'bg-slate-700 text-slate-300'
}

onMounted(load)
</script>
