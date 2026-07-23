<template>
  <div class="scw-animate-in">
    <PageHeader title="Clients" icon="pi pi-users" :subtitle="`${store.clients.length} client${store.clients.length !== 1 ? 's' : ''}`">
      <template #actions>
        <Button
          label="CSV"
          icon="pi pi-download"
          severity="secondary"
          outlined
          :disabled="store.clients.length === 0"
          @click="exportCsv"
        />
        <Button label="Nouveau" icon="pi pi-plus" @click="router.push('/clients/new')" />
      </template>
    </PageHeader>

    <!-- Filters toolbar -->
    <div class="scw-panel p-3 sm:p-4 mb-4">
      <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <IconField class="md:col-span-2 xl:col-span-1">
          <InputIcon class="pi pi-search" />
          <InputText v-model="query" placeholder="Nom ou téléphone…" class="w-full" @input="onSearch" />
        </IconField>
        <Select v-model="filterType" :options="typeOptions" option-label="label" option-value="value" class="w-full" @change="reload" />
        <Select v-model="filterStatus" :options="statusOptions" option-label="label" option-value="value" class="w-full" @change="reload" />
        <Select v-model="sortKey" :options="sortOptions" option-label="label" option-value="value" class="w-full" @change="reload" />
      </div>

      <div v-if="filterTag" class="flex items-center gap-2 mt-3">
        <span class="text-xs text-slate-500">Filtre tag :</span>
        <Chip :label="`#${filterTag}`" removable @remove="clearTag" />
      </div>
    </div>

    <!-- Table -->
    <DataTable
      :value="store.clients"
      :loading="store.loading"
      data-key="id"
      paginator
      :rows="15"
      :rows-per-page-options="[15, 30, 50]"
      removable-sort
      row-hover
      class="scw-panel overflow-hidden"
      :pt="{ table: { style: 'min-width: 48rem' } }"
      @row-click="onRowClick"
    >
      <template #empty>
        <EmptyState
          icon="pi pi-users"
          :text="query ? 'Aucun résultat' : 'Aucun client'"
          :hint="query ? 'Essayez une autre recherche' : 'Ajoutez votre premier client pour commencer'"
        >
          <template #action>
            <Button v-if="!query" label="Nouveau client" icon="pi pi-plus" @click="router.push('/clients/new')" />
          </template>
        </EmptyState>
      </template>

      <Column header="Client" style="min-width: 16rem">
        <template #body="{ data }">
          <div :class="{ 'opacity-60': !data.active }">
            <div class="flex items-center gap-2 flex-wrap">
              <Avatar :label="initials(data.name)" shape="circle" class="!bg-primary/15 !text-primary !text-xs !font-semibold" />
              <span class="font-semibold text-slate-100">{{ data.name }}</span>
              <Tag :severity="typeSeverity(data.type)" :value="CLIENT_TYPE_LABELS[data.type] ?? data.type" rounded />
              <Tag v-if="!data.active" severity="secondary" value="Inactif" rounded />
            </div>
            <div class="flex items-center gap-2 mt-1 ml-9">
              <span class="text-sm text-slate-400">{{ data.phone }}</span>
              <span v-if="data.vehicleCount" class="text-xs text-slate-500">· {{ data.vehicleCount }} véhicule{{ data.vehicleCount > 1 ? 's' : '' }}</span>
            </div>
            <div v-if="data.tags?.length" class="flex flex-wrap gap-1 mt-1.5 ml-9">
              <button
                v-for="t in data.tags"
                :key="t"
                class="text-[11px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 hover:bg-slate-700"
                @click.stop="applyTag(t)"
              >#{{ t }}</button>
            </div>
            <div v-if="needsAlert(data)" class="mt-1.5 ml-9 text-xs text-amber-300 flex items-center gap-1">
              <i class="pi pi-exclamation-triangle text-[11px]" />
              {{ alertMessage(data) }}
            </div>
          </div>
        </template>
      </Column>

      <Column header="Total dépensé" sortable field="totalSpent" style="width: 10rem">
        <template #body="{ data }">
          <span class="font-semibold text-emerald-400">{{ fmtFcfa(data.totalSpent) }}</span>
        </template>
      </Column>

      <Column header="Visites" sortable field="visitCount" style="width: 7rem">
        <template #body="{ data }">
          <span class="text-slate-200">{{ data.visitCount }}</span>
        </template>
      </Column>

      <Column header="Dernière visite" style="width: 10rem">
        <template #body="{ data }">
          <span class="text-sm text-slate-400">{{ data.lastVisitAt ? formatDate(data.lastVisitAt) : 'Jamais' }}</span>
        </template>
      </Column>

      <Column style="width: 3rem">
        <template #body>
          <i class="pi pi-chevron-right text-slate-600 text-sm" />
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useClientsStore } from '@/stores/clients'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Chip from 'primevue/chip'
import Avatar from 'primevue/avatar'

const CLIENT_TYPE_LABELS = { CARTE: 'Carte', BOUCLIER: 'Bouclier', VIP: 'VIP' }

const store = useClientsStore()
const router = useRouter()
const toast = useToast()
const query = ref('')
let debounce

const filterType = ref('')
const filterStatus = ref('active')
const filterTag = ref('')
const sortKey = ref('name')

const typeOptions = [
  { label: 'Tous types', value: '' },
  { label: 'Carte', value: 'CARTE' },
  { label: 'Bouclier', value: 'BOUCLIER' },
  { label: 'VIP', value: 'VIP' }
]
const statusOptions = [
  { label: 'Actifs', value: 'active' },
  { label: 'Inactifs', value: 'inactive' },
  { label: 'Tous', value: 'all' }
]
const sortOptions = [
  { label: 'A → Z', value: 'name' },
  { label: 'Top dépenses', value: 'spent' },
  { label: 'Plus de visites', value: 'visits' },
  { label: 'Visite récente', value: 'recent' },
  { label: 'Récemment ajoutés', value: 'created' }
]

function reload() {
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
function applyTag(t) {
  filterTag.value = t
  reload()
}
function clearTag() {
  filterTag.value = ''
  reload()
}
function onRowClick(e) {
  router.push(`/clients/${e.data.id}`)
}

const typeSeverity = (t) => ({ CARTE: 'info', BOUCLIER: 'help', VIP: 'warn' }[t] ?? 'secondary')
function initials(name) {
  return (name || '?').split(/\s+/).slice(0, 2).map((w) => w[0]?.toUpperCase()).join('')
}

function exportCsv() {
  const headers = ['Nom', 'Téléphone', 'Type', 'Solde', 'Visites', 'Total dépensé (FCFA)', 'Dernière visite', 'Véhicules', 'Tags', 'Statut']
  const rows = store.clients.map((c) => [
    c.name, c.phone, CLIENT_TYPE_LABELS[c.type] ?? c.type, c.balance, c.visitCount,
    c.totalSpent, c.lastVisitAt ? formatDate(c.lastVisitAt) : '', c.vehicleCount,
    (c.tags ?? []).join(' '), c.active ? 'Actif' : 'Inactif'
  ])
  const esc = (v) => {
    const s = String(v ?? '')
    return /[",\n;]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
  }
  const csv = [headers, ...rows].map((r) => r.map(esc).join(';')).join('\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `clients-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  toast.add({ severity: 'success', summary: 'Export réussi', detail: `${store.clients.length} clients exportés`, life: 3000 })
}

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

<style scoped>
:deep(.p-datatable-tbody > tr) {
  cursor: pointer;
}
</style>
