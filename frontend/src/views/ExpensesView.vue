<template>
  <div class="p-4 space-y-4 pb-24">

    <h2 class="text-xl font-bold">Dépenses</h2>

    <!-- Month picker -->
    <div class="card space-y-3">
      <div class="flex items-center justify-between">
        <div>
          <label class="block text-xs text-slate-400 mb-1">Mois</label>
          <input v-model="selectedMonth" type="month" class="input-field" @change="load" />
        </div>
        <button @click="showForm = !showForm" class="btn-primary text-sm py-1.5 px-4 shrink-0 self-end">
          {{ showForm ? 'Annuler' : '+ Dépense' }}
        </button>
      </div>

      <!-- Add expense form -->
      <div v-if="showForm" class="bg-slate-700/50 rounded-xl p-4 space-y-3">
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-xs text-slate-400 mb-1">Catégorie</label>
            <select v-model="form.category" class="input-field">
              <option v-for="opt in CATEGORY_OPTS" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </div>
          <div>
            <label class="block text-xs text-slate-400 mb-1">Montant (FCFA)</label>
            <input v-model.number="form.amount" type="number" min="1" class="input-field" placeholder="Ex : 15000" />
          </div>
        </div>
        <div>
          <label class="block text-xs text-slate-400 mb-1">Description (optionnelle)</label>
          <input v-model="form.description" type="text" class="input-field" placeholder="Ex : Achat shampoing" />
        </div>
        <div>
          <label class="block text-xs text-slate-400 mb-1">Date</label>
          <input v-model="form.expenseDate" type="date" class="input-field" />
        </div>
        <p v-if="formError" class="text-red-400 text-sm">{{ formError }}</p>
        <button
          @click="createExpense"
          :disabled="saving"
          class="btn-primary w-full disabled:opacity-50"
        >
          {{ saving ? 'Enregistrement…' : 'Enregistrer la dépense' }}
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="space-y-2">
      <div v-for="n in 4" :key="n" class="card animate-pulse">
        <div class="flex justify-between">
          <div class="space-y-2 flex-1">
            <div class="h-4 bg-slate-700 rounded w-1/3"></div>
            <div class="h-3 bg-slate-700 rounded w-1/4"></div>
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
    <div v-else-if="expenses.length === 0" class="card text-center py-12 text-slate-400">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 mx-auto mb-3 opacity-40" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
      </svg>
      <p class="font-medium">Aucune dépense ce mois-ci</p>
    </div>

    <!-- Expense list -->
    <div v-else class="space-y-2">
      <div class="grid gap-2 md:grid-cols-2">
      <div
        v-for="exp in expenses"
        :key="exp.id"
        class="card flex items-start justify-between gap-3"
      >
        <div class="min-w-0">
          <div class="flex items-center gap-2 flex-wrap">
            <span
              class="text-xs px-2 py-0.5 rounded-full font-medium shrink-0"
              :class="categoryClass(exp.category)"
            >{{ CATEGORY_LABELS[exp.category] || exp.category }}</span>
            <p v-if="exp.description" class="text-sm text-slate-200 truncate">{{ exp.description }}</p>
          </div>
          <p class="text-xs text-slate-500 mt-1">
            {{ formatDate(exp.expenseDate) }} · {{ exp.createdByName }}
          </p>
        </div>
        <div class="flex items-center gap-2 shrink-0">
          <span class="font-bold text-red-400">{{ exp.amount.toLocaleString('fr-FR') }} FCFA</span>
          <button
            @click="deleteExpense(exp)"
            class="text-xs px-2 py-1 rounded-lg bg-slate-700 text-slate-400 hover:bg-red-900/40 hover:text-red-400 transition-colors"
            aria-label="Supprimer"
          >✕</button>
        </div>
      </div>
      </div>

      <!-- Monthly total -->
      <div class="card bg-slate-700/50 flex justify-between items-center">
        <span class="text-sm text-slate-400">Total du mois</span>
        <span class="font-bold text-red-400 text-lg">{{ totalAmount.toLocaleString('fr-FR') }} FCFA</span>
      </div>

      <p v-if="deleteError" class="text-red-400 text-sm text-center">{{ deleteError }}</p>
    </div>

  </div>

  <ConfirmModal
    v-if="pendingDeleteExpense"
    title="Supprimer cette dépense ?"
    :message="`Montant : ${pendingDeleteExpense.amount.toLocaleString('fr-FR')} FCFA`"
    confirm-label="Supprimer"
    @confirm="confirmDeleteExpense"
    @cancel="pendingDeleteExpense = null"
  />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/api/axios'
import ConfirmModal from '@/components/ConfirmModal.vue'

const CATEGORY_OPTS = [
  { value: 'PRODUIT', label: 'Produits / Fournitures' },
  { value: 'SALAIRE', label: 'Salaires' },
  { value: 'AUTRE',   label: 'Autre' }
]

const CATEGORY_LABELS = {
  PRODUIT: 'Produits',
  SALAIRE: 'Salaires',
  AUTRE:   'Autre'
}

const todayIso = new Date().toISOString().slice(0, 10)
const currentMonthIso = todayIso.slice(0, 7)

const selectedMonth = ref(currentMonthIso)
const expenses      = ref([])
const loading       = ref(false)
const error         = ref('')

const showForm    = ref(false)
const saving      = ref(false)
const formError   = ref('')
const form        = ref({ category: 'PRODUIT', amount: null, description: '', expenseDate: todayIso })

const totalAmount = computed(() => expenses.value.reduce((sum, e) => sum + e.amount, 0))

const pendingDeleteExpense = ref(null)
const deleteError          = ref('')

async function load() {
  loading.value = true
  error.value   = ''
  try {
    const { data } = await api.get('/expenses', { params: { month: selectedMonth.value } })
    expenses.value = data
  } catch (err) {
    error.value = err.response?.data?.error || 'Impossible de charger les dépenses.'
  } finally {
    loading.value = false
  }
}

async function createExpense() {
  if (saving.value) return
  formError.value = ''
  if (!form.value.amount || form.value.amount < 1) { formError.value = 'Le montant doit être > 0.'; return }

  saving.value = true
  try {
    const { data } = await api.post('/expenses', {
      category:    form.value.category,
      amount:      form.value.amount,
      description: form.value.description.trim() || null,
      expenseDate: form.value.expenseDate
    })
    expenses.value.unshift(data)
    showForm.value        = false
    form.value.amount      = null
    form.value.description = ''
    form.value.expenseDate = todayIso
  } catch (err) {
    formError.value = err.response?.data?.error || 'Une erreur est survenue.'
  } finally {
    saving.value = false
  }
}

function deleteExpense(exp) {
  deleteError.value = ''
  pendingDeleteExpense.value = exp
}

async function confirmDeleteExpense() {
  const exp = pendingDeleteExpense.value
  pendingDeleteExpense.value = null
  try {
    await api.delete(`/expenses/${exp.id}`)
    expenses.value = expenses.value.filter(e => e.id !== exp.id)
  } catch (err) {
    deleteError.value = err.response?.data?.error || 'Erreur lors de la suppression.'
  }
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

function categoryClass(cat) {
  return {
    PRODUIT: 'bg-blue-500/20 text-blue-300',
    SALAIRE: 'bg-purple-500/20 text-purple-300',
    AUTRE:   'bg-slate-600 text-slate-300'
  }[cat] || 'bg-slate-600 text-slate-300'
}

onMounted(load)
</script>
