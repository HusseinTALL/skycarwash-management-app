<template>
  <div class="scw-animate-in">
    <PageHeader title="Historique" icon="pi pi-clock" subtitle="Transactions encaissées" />

    <!-- Filters -->
    <div class="scw-panel p-3 sm:p-4 mb-4">
      <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4 items-end">
        <div>
          <label class="block text-xs text-slate-400 mb-1.5">Du</label>
          <DatePicker v-model="fromDate" date-format="dd/mm/yy" show-icon icon-display="input" class="w-full" @update:model-value="resetAndLoad" />
        </div>
        <div>
          <label class="block text-xs text-slate-400 mb-1.5">Au</label>
          <DatePicker v-model="toDate" date-format="dd/mm/yy" show-icon icon-display="input" class="w-full" @update:model-value="resetAndLoad" />
        </div>
        <div class="sm:col-span-2 lg:col-span-1">
          <label class="block text-xs text-slate-400 mb-1.5">Paiement</label>
          <Select v-model="method" :options="METHOD_OPTS" option-label="label" option-value="value" class="w-full" @change="resetAndLoad" />
        </div>
        <Button label="Réinitialiser" icon="pi pi-filter-slash" severity="secondary" outlined class="w-full" @click="resetFilters" />
      </div>
    </div>

    <Message v-if="error" severity="error" :closable="false" class="mb-4">
      <div class="flex items-center gap-3">
        <span>{{ error }}</span>
        <Button label="Réessayer" size="small" text @click="load" />
      </div>
    </Message>

    <DataTable
      :value="transactions"
      :loading="loading"
      lazy
      paginator
      :rows="20"
      :total-records="totalElements"
      :first="page * 20"
      data-key="id"
      row-hover
      class="scw-panel overflow-hidden"
      :pt="{ table: { style: 'min-width: 44rem' } }"
      @page="onPage"
    >
      <template #empty>
        <EmptyState icon="pi pi-credit-card" text="Aucune transaction" hint="Modifiez les filtres pour voir d'autres résultats" />
      </template>

      <Column header="Service" style="min-width: 14rem">
        <template #body="{ data }">
          <div :class="{ 'opacity-60': data.cancelledAt }">
            <div class="flex items-center gap-2">
              <span class="font-semibold" :class="data.cancelledAt ? 'line-through text-slate-400' : 'text-slate-100'">
                {{ data.serviceName }}
              </span>
              <Tag v-if="data.cancelledAt" severity="danger" value="Annulée" rounded />
            </div>
            <div class="flex items-center gap-2 mt-0.5 text-xs text-slate-500 flex-wrap">
              <span>{{ formatDateTime(data.createdAt) }}</span>
              <span>· {{ data.userName }}</span>
              <span v-if="data.clientName" class="text-primary">· {{ data.clientName }}</span>
            </div>
            <p v-if="data.cancelledAt && data.cancelReason" class="text-xs text-red-400 mt-1">
              Motif : {{ data.cancelReason }}
            </p>
          </div>
        </template>
      </Column>

      <Column header="Paiement" style="width: 11rem">
        <template #body="{ data }">
          <Tag :severity="methodSeverity(data.paymentMethod)" :value="PAYMENT_LABELS[data.paymentMethod] || data.paymentMethod" rounded />
        </template>
      </Column>

      <Column header="Montant" style="width: 9rem">
        <template #body="{ data }">
          <span class="font-bold" :class="data.cancelledAt ? 'text-slate-500 line-through' : 'text-slate-100'">
            {{ data.amount.toLocaleString('fr-FR') }} F
          </span>
        </template>
      </Column>

      <template #footer>
        <div v-if="transactions.length" class="flex justify-between items-center px-2">
          <span class="text-sm text-slate-400">Total encaissé (cette page)</span>
          <span class="font-bold text-emerald-400">{{ pageTotal.toLocaleString('fr-FR') }} FCFA</span>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/api/axios'
import { PAYMENT_LABELS } from '@/constants'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import Message from 'primevue/message'
import DatePicker from 'primevue/datepicker'

const METHOD_OPTS = [
  { value: '', label: 'Tous' },
  { value: 'CASH', label: 'Espèces' },
  { value: 'ORANGE', label: 'Orange Money' },
  { value: 'MOOV', label: 'Moov Money' },
  { value: 'ABONNEMENT', label: 'Abonnement' }
]
const METHOD_SEVERITY = { CASH: 'success', ORANGE: 'warn', MOOV: 'info', ABONNEMENT: 'help' }
const methodSeverity = (m) => METHOD_SEVERITY[m] ?? 'secondary'

const fromDate = ref(new Date())
const toDate = ref(new Date())
const method = ref('')

const transactions = ref([])
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const loading = ref(false)
const error = ref('')

const pageTotal = computed(() =>
  transactions.value.filter((tx) => !tx.cancelledAt).reduce((sum, tx) => sum + tx.amount, 0)
)

function toISO(d) {
  if (!d) return undefined
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { from: toISO(fromDate.value), to: toISO(toDate.value), page: page.value, size: 20 }
    if (method.value) params.method = method.value
    const { data } = await api.get('/transactions/history', { params })
    transactions.value = data.content
    totalPages.value = data.totalPages
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
function resetFilters() {
  fromDate.value = new Date()
  toDate.value = new Date()
  method.value = ''
  resetAndLoad()
}
function onPage(e) {
  page.value = e.page
  load()
}
function formatDateTime(iso) {
  return new Date(iso).toLocaleString('fr-FR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
  })
}

onMounted(load)
</script>
