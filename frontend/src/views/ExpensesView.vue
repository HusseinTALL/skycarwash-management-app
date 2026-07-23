<template>
  <div class="scw-animate-in">
    <PageHeader title="Dépenses" icon="pi pi-arrow-down" subtitle="Suivi des charges mensuelles">
      <template #actions>
        <DatePicker v-model="monthDate" view="month" date-format="mm/yy" show-icon icon-display="input" class="w-44" @update:model-value="load" />
        <Button label="Dépense" icon="pi pi-plus" @click="openForm" />
      </template>
    </PageHeader>

    <!-- Summary strip -->
    <div class="grid grid-cols-2 sm:grid-cols-3 gap-3 sm:gap-4 mb-4">
      <KpiCard title="Total du mois" :value="`${totalAmount.toLocaleString('fr-FR')} F`" icon="pi pi-wallet" accent="#f87171" :loading="loading" />
      <KpiCard title="Nb. dépenses" :value="expenses.length" icon="pi pi-list" accent="#38bdf8" :loading="loading" />
      <KpiCard title="Dépense moyenne" :value="`${avgAmount.toLocaleString('fr-FR')} F`" icon="pi pi-chart-bar" accent="#c084fc" :loading="loading" class="col-span-2 sm:col-span-1" />
    </div>

    <Message v-if="error" severity="error" :closable="false" class="mb-4">
      <div class="flex items-center gap-3">
        <span>{{ error }}</span>
        <Button label="Réessayer" size="small" text @click="load" />
      </div>
    </Message>

    <DataTable
      :value="expenses"
      :loading="loading"
      data-key="id"
      paginator
      :rows="15"
      row-hover
      class="scw-panel overflow-hidden"
      :pt="{ table: { style: 'min-width: 40rem' } }"
    >
      <template #empty>
        <EmptyState icon="pi pi-wallet" text="Aucune dépense ce mois-ci" hint="Enregistrez votre première dépense">
          <template #action>
            <Button label="Ajouter une dépense" icon="pi pi-plus" @click="openForm" />
          </template>
        </EmptyState>
      </template>

      <Column header="Catégorie" style="width: 10rem">
        <template #body="{ data }">
          <Tag :severity="categorySeverity(data.category)" :value="CATEGORY_LABELS[data.category] || data.category" rounded />
        </template>
      </Column>
      <Column header="Description" style="min-width: 12rem">
        <template #body="{ data }">
          <span class="text-slate-200">{{ data.description || '—' }}</span>
        </template>
      </Column>
      <Column header="Date" style="width: 9rem">
        <template #body="{ data }">
          <span class="text-sm text-slate-400">{{ formatDate(data.expenseDate) }}</span>
        </template>
      </Column>
      <Column header="Montant" style="width: 9rem">
        <template #body="{ data }">
          <span class="font-bold text-red-400">{{ data.amount.toLocaleString('fr-FR') }} F</span>
        </template>
      </Column>
      <Column style="width: 4rem">
        <template #body="{ data }">
          <Button icon="pi pi-trash" severity="danger" text rounded size="small" v-tooltip.left="'Supprimer'" @click="deleteExpense(data)" />
        </template>
      </Column>
    </DataTable>

    <!-- Add expense dialog -->
    <Dialog v-model:visible="showForm" modal header="Nouvelle dépense" class="w-full max-w-md mx-3">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Catégorie</label>
          <Select v-model="form.category" :options="CATEGORY_OPTS" option-label="label" option-value="value" class="w-full" />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Montant (FCFA) <span class="text-red-400">*</span></label>
          <InputNumber v-model="form.amount" :min="1" class="w-full" placeholder="Ex : 15000" />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Description (optionnelle)</label>
          <InputText v-model="form.description" class="w-full" placeholder="Ex : Achat shampoing" />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Date</label>
          <DatePicker v-model="form.expenseDate" date-format="dd/mm/yy" show-icon icon-display="input" class="w-full" />
        </div>
        <Message v-if="formError" severity="error" :closable="false" size="small">{{ formError }}</Message>
      </div>
      <template #footer>
        <Button label="Annuler" severity="secondary" text @click="showForm = false" />
        <Button label="Enregistrer" icon="pi pi-check" :loading="saving" @click="createExpense" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/api/axios'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'
import PageHeader from '@/components/ui/PageHeader.vue'
import KpiCard from '@/components/ui/KpiCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import Message from 'primevue/message'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import DatePicker from 'primevue/datepicker'

const CATEGORY_OPTS = [
  { value: 'PRODUIT', label: 'Produits / Fournitures' },
  { value: 'SALAIRE', label: 'Salaires' },
  { value: 'AUTRE', label: 'Autre' }
]
const CATEGORY_LABELS = { PRODUIT: 'Produits', SALAIRE: 'Salaires', AUTRE: 'Autre' }
const categorySeverity = (c) => ({ PRODUIT: 'info', SALAIRE: 'help', AUTRE: 'secondary' }[c] ?? 'secondary')

const confirm = useConfirm()
const toast = useToast()

const monthDate = ref(new Date())
const expenses = ref([])
const loading = ref(false)
const error = ref('')

const showForm = ref(false)
const saving = ref(false)
const formError = ref('')
const form = ref({ category: 'PRODUIT', amount: null, description: '', expenseDate: new Date() })

const totalAmount = computed(() => expenses.value.reduce((sum, e) => sum + e.amount, 0))
const avgAmount = computed(() => (expenses.value.length ? Math.round(totalAmount.value / expenses.value.length) : 0))

function toISOMonth(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}
function toISODate(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get('/expenses', { params: { month: toISOMonth(monthDate.value) } })
    expenses.value = data
  } catch (err) {
    error.value = err.response?.data?.error || 'Impossible de charger les dépenses.'
  } finally {
    loading.value = false
  }
}

function openForm() {
  formError.value = ''
  form.value = { category: 'PRODUIT', amount: null, description: '', expenseDate: new Date() }
  showForm.value = true
}

async function createExpense() {
  if (saving.value) return
  formError.value = ''
  if (!form.value.amount || form.value.amount < 1) {
    formError.value = 'Le montant doit être > 0.'
    return
  }
  saving.value = true
  try {
    const { data } = await api.post('/expenses', {
      category: form.value.category,
      amount: form.value.amount,
      description: form.value.description.trim() || null,
      expenseDate: toISODate(form.value.expenseDate)
    })
    expenses.value.unshift(data)
    toast.add({ severity: 'success', summary: 'Dépense enregistrée', detail: `${data.amount.toLocaleString('fr-FR')} FCFA`, life: 3000 })
    showForm.value = false
  } catch (err) {
    formError.value = err.response?.data?.error || 'Une erreur est survenue.'
  } finally {
    saving.value = false
  }
}

function deleteExpense(exp) {
  confirm.require({
    header: 'Supprimer cette dépense ?',
    message: `Montant : ${exp.amount.toLocaleString('fr-FR')} FCFA`,
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Supprimer',
    rejectLabel: 'Annuler',
    acceptProps: { severity: 'danger' },
    accept: async () => {
      try {
        await api.delete(`/expenses/${exp.id}`)
        expenses.value = expenses.value.filter((e) => e.id !== exp.id)
        toast.add({ severity: 'info', summary: 'Dépense supprimée', life: 3000 })
      } catch (err) {
        toast.add({ severity: 'error', summary: 'Erreur', detail: err.response?.data?.error || 'Suppression impossible', life: 4000 })
      }
    }
  })
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

onMounted(load)
</script>
