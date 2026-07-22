<template>
  <div class="p-4 space-y-4">

    <!-- Header -->
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-bold">Clients</h2>
      <div class="flex items-center gap-2">
        <button
          @click="exportCsv"
          :disabled="store.clients.length === 0"
          class="btn-secondary py-2 px-3 text-sm disabled:opacity-40"
          title="Exporter la liste filtrée en CSV"
        >
          ⭳ CSV
        </button>
        <RouterLink to="/clients/new" class="btn-primary py-2 px-4 text-sm">+ Nouveau</RouterLink>
      </div>
    </div>

    <!-- Search -->
    <input
      v-model="query"
      type="search"
      placeholder="Rechercher par nom ou téléphone..."
      class="input-field"
      @input="onSearch"
    />

    <!-- Segmentation filters -->
    <div class="grid grid-cols-3 gap-2">
      <select v-model="filterType" @change="reload" class="input-field py-2 text-sm">
        <option value="">Tous types</option>
        <option value="CARTE">Carte</option>
        <option value="BOUCLIER">Bouclier</option>
        <option value="VIP">VIP</option>
      </select>
      <select v-model="filterStatus" @change="reload" class="input-field py-2 text-sm">
        <option value="active">Actifs</option>
        <option value="inactive">Inactifs</option>
        <option value="all">Tous</option>
      </select>
      <select v-model="sortKey" @change="reload" class="input-field py-2 text-sm">
        <option value="name">A → Z</option>
        <option value="spent">Top dépenses</option>
        <option value="visits">Plus de visites</option>
        <option value="recent">Visite récente</option>
        <option value="created">Récemment ajoutés</option>
      </select>
    </div>

    <!-- Active tag filter chip -->
    <div v-if="filterTag" class="flex items-center gap-2 text-xs">
      <span class="text-slate-500">Tag :</span>
      <button
        @click="filterTag = ''; reload()"
        class="px-2 py-1 rounded-full bg-brand-500/20 text-brand-400 flex items-center gap-1"
      >
        #{{ filterTag }} <span aria-hidden="true">✕</span>
      </button>
    </div>

    <!-- Loading skeleton -->
    <div v-if="store.loading" class="space-y-2">
      <div v-for="n in 5" :key="n" class="card animate-pulse">
        <div class="flex items-center justify-between">
          <div class="space-y-2 flex-1 min-w-0">
            <div class="h-4 bg-slate-700 rounded w-2/5"></div>
            <div class="h-3 bg-slate-700 rounded w-1/3"></div>
          </div>
          <div class="h-4 bg-slate-700 rounded w-20 shrink-0 ml-3"></div>
        </div>
      </div>
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

    <!-- Client list + pagination -->
    <div v-else class="space-y-3">
      <p class="text-xs text-slate-500">{{ store.clients.length }} client{{ store.clients.length !== 1 ? 's' : '' }}</p>

      <div class="space-y-2">
        <RouterLink
          v-for="client in paginatedClients"
          :key="client.id"
          :to="`/clients/${client.id}`"
          class="card block hover:bg-slate-700 active:scale-[0.99] transition-all duration-100"
          :class="{ 'opacity-60': !client.active }"
        >
          <div class="flex items-center justify-between">
            <div class="min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <p class="font-semibold truncate">{{ client.name }}</p>
                <span
                  class="text-xs px-2 py-0.5 rounded-full shrink-0"
                  :class="{
                    'bg-blue-900 text-blue-300':     client.type === 'CARTE',
                    'bg-purple-900 text-purple-300': client.type === 'BOUCLIER',
                    'bg-amber-900 text-amber-300':   client.type === 'VIP'
                  }"
                >{{ CLIENT_TYPE_LABELS[client.type] ?? client.type }}</span>
                <span v-if="!client.active" class="text-xs px-2 py-0.5 rounded-full bg-slate-700 text-slate-400">Inactif</span>
              </div>
              <p class="text-sm text-slate-400 mt-0.5">{{ client.phone }}</p>

              <!-- Tags -->
              <div v-if="client.tags?.length" class="flex flex-wrap gap-1 mt-1.5">
                <button
                  v-for="t in client.tags"
                  :key="t"
                  @click.prevent="filterTag = t; reload()"
                  class="text-[11px] px-1.5 py-0.5 rounded bg-slate-700 text-slate-300 hover:bg-slate-600"
                >#{{ t }}</button>
              </div>
            </div>

            <div class="text-right shrink-0 ml-3">
              <p class="text-sm font-semibold text-green-400">{{ fmtFcfa(client.totalSpent) }}</p>
              <p class="text-xs text-slate-400 mt-0.5">
                {{ client.visitCount }} visite{{ client.visitCount !== 1 ? 's' : '' }}
                <span v-if="client.vehicleCount"> · {{ client.vehicleCount }} 🚗</span>
              </p>
              <p class="text-xs text-slate-500 mt-0.5">
                {{ client.lastVisitAt ? 'vu ' + formatDate(client.lastVisitAt) : 'jamais venu' }}
              </p>
            </div>
          </div>

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

      <!-- Pagination controls -->
      <div v-if="totalPages > 1" class="flex items-center justify-between text-sm pt-1">
        <button
          @click="page--"
          :disabled="page === 1"
          class="px-3 py-1.5 rounded-lg bg-slate-700 text-slate-300 disabled:opacity-40 hover:bg-slate-600 transition-colors"
        >
          ← Préc.
        </button>
        <span class="text-slate-400 text-xs">{{ page }} / {{ totalPages }}</span>
        <button
          @click="page++"
          :disabled="page === totalPages"
          class="px-3 py-1.5 rounded-lg bg-slate-700 text-slate-300 disabled:opacity-40 hover:bg-slate-600 transition-colors"
        >
          Suiv. →
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useClientsStore } from '@/stores/clients'

const CLIENT_TYPE_LABELS = {
  CARTE:    'Carte passages',
  BOUCLIER: 'Bouclier',
  VIP:      'VIP'
}

const store = useClientsStore()
const query = ref('')
let debounce

// ── Filters (server-driven) ──────────────────────────────────────── //
const filterType   = ref('')
const filterStatus = ref('active')
const filterTag    = ref('')
const sortKey      = ref('name')

// ── Pagination (client-side over filtered result) ────────────────── //
const PAGE_SIZE = 15
const page      = ref(1)

const totalPages = computed(() => Math.max(1, Math.ceil(store.clients.length / PAGE_SIZE)))
const paginatedClients = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return store.clients.slice(start, start + PAGE_SIZE)
})

// Clamp page if the list shrinks
watch(() => store.clients.length, () => {
  if (page.value > totalPages.value) page.value = totalPages.value
})

function reload() {
  page.value = 1
  store.loadAll({
    q: query.value,
    type: filterType.value,
    status: filterStatus.value,
    tag: filterTag.value,
    sort: sortKey.value
  })
}

onMounted(reload)

function onSearch() {
  clearTimeout(debounce)
  debounce = setTimeout(reload, 300)
}

// ── CSV export ───────────────────────────────────────────────────── //
function exportCsv() {
  const headers = ['Nom', 'Téléphone', 'Type', 'Solde', 'Visites', 'Total dépensé (FCFA)', 'Dernière visite', 'Véhicules', 'Tags', 'Statut']
  const rows = store.clients.map(c => [
    c.name,
    c.phone,
    CLIENT_TYPE_LABELS[c.type] ?? c.type,
    c.balance,
    c.visitCount,
    c.totalSpent,
    c.lastVisitAt ? formatDate(c.lastVisitAt) : '',
    c.vehicleCount,
    (c.tags ?? []).join(' '),
    c.active ? 'Actif' : 'Inactif'
  ])
  const esc = (v) => {
    const s = String(v ?? '')
    return /[",\n;]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
  }
  const csv = [headers, ...rows].map(r => r.map(esc).join(';')).join('\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `clients-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

// ── Formatting / alerts ──────────────────────────────────────────── //
function fmtFcfa(n) {
  return new Intl.NumberFormat('fr-FR').format(n ?? 0) + ' F'
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
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
