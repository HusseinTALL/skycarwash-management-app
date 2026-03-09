<template>
  <div class="p-4 space-y-4 pb-6">

    <!-- Stale cache banner (feature 8) -->
    <div
      v-if="(activeTab === 'daily' && dash.dailyCachedAt) || (activeTab === 'monthly' && dash.monthlyCachedAt)"
      class="flex items-center gap-2 bg-amber-900/30 border border-amber-700/50 rounded-xl px-4 py-2 text-amber-300 text-xs"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      Données en cache ({{ formatCacheTime(activeTab === 'daily' ? dash.dailyCachedAt : dash.monthlyCachedAt) }}) — mode hors-ligne
    </div>

    <!-- Header + WS indicator -->
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-bold">Dashboard</h2>
      <div class="flex items-center gap-2">
        <span
          v-if="dash.wsDataError"
          class="text-xs text-amber-400 flex items-center gap-1"
          title="Données temps réel malformées"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
          </svg>
          Données incorrectes
        </span>
        <div class="flex items-center gap-1.5 text-xs"
             :class="dash.wsConnected ? 'text-green-400' : 'text-slate-500'">
          <span class="w-2 h-2 rounded-full"
                :class="dash.wsConnected ? 'bg-green-400 animate-pulse' : 'bg-slate-500'"/>
          {{ dash.wsConnected ? 'En direct' : 'Hors-ligne' }}
        </div>
      </div>
    </div>

    <!-- Tab switcher -->
    <div class="flex bg-slate-800 rounded-xl p-1 gap-1">
      <button
        v-for="tab in TABS" :key="tab.value"
        @click="activeTab = tab.value"
        class="flex-1 py-2 rounded-lg text-sm font-medium transition-colors duration-150"
        :class="activeTab === tab.value
          ? 'bg-brand-500 text-white'
          : 'text-slate-400 hover:text-slate-200'"
      >{{ tab.label }}</button>
    </div>

    <!-- ═══════════════ DAILY TAB ═══════════════ -->
    <template v-if="activeTab === 'daily'">

      <!-- Date picker -->
      <input
        v-model="selectedDate"
        type="date"
        class="input-field"
        @change="dash.loadDaily(selectedDate)"
      />

      <!-- Loading -->
      <div v-if="dash.loadingDaily" class="text-center py-10 text-slate-400 text-sm">
        Chargement...
      </div>

      <template v-else-if="dash.daily">

        <!-- KPI cards -->
        <div class="grid grid-cols-2 gap-3">
          <!-- Vehicles -->
          <div class="card">
            <p class="text-xs text-slate-400 uppercase tracking-wide">Véhicules</p>
            <p class="text-3xl font-bold mt-1">{{ dash.daily.vehiclesWashed }}</p>
            <p class="text-xs mt-1" :class="deltaClass(dash.daily.vehiclesDelta)">
              {{ dash.formatDelta(dash.daily.vehiclesDelta) }} vs sem. passée
            </p>
          </div>

          <!-- Revenue -->
          <div class="card">
            <p class="text-xs text-slate-400 uppercase tracking-wide">Recettes</p>
            <p class="text-2xl font-bold mt-1 text-brand-400">
              {{ dash.formatFcfa(dash.daily.totalRevenue) }}
            </p>
            <p class="text-xs mt-1" :class="deltaClass(dash.daily.revenueDelta)">
              {{ dash.formatDelta(dash.daily.revenueDelta) }} vs sem. passée
            </p>
          </div>
        </div>

        <!-- Revenue by payment method -->
        <div class="card space-y-3">
          <h3 class="text-sm font-semibold text-slate-300">Par mode de paiement</h3>
          <div
            v-for="[method, amount] in dash.revenueByMethodEntries"
            :key="method"
            class="space-y-1"
          >
            <div class="flex justify-between text-xs">
              <span class="text-slate-400">{{ METHOD_LABELS[method] ?? method }}</span>
              <span class="font-medium">{{ dash.formatFcfa(amount) }}</span>
            </div>
            <div class="h-1.5 bg-slate-700 rounded-full overflow-hidden">
              <div
                class="h-full rounded-full transition-all duration-500"
                :class="METHOD_COLORS[method] ?? 'bg-brand-500'"
                :style="{ width: pct(amount, dash.daily.totalRevenue) + '%' }"
              />
            </div>
          </div>
          <p v-if="!dash.revenueByMethodEntries.length" class="text-slate-500 text-sm text-center py-2">
            Aucune transaction
          </p>
        </div>

        <!-- Revenue by service -->
        <div class="card space-y-2">
          <h3 class="text-sm font-semibold text-slate-300">Par service</h3>
          <div
            v-for="[service, amount] in dash.revenueByServiceEntries"
            :key="service"
            class="flex justify-between text-sm"
          >
            <span class="text-slate-300 truncate">{{ service }}</span>
            <span class="font-semibold text-brand-400 shrink-0 ml-3">
              {{ dash.formatFcfa(amount) }}
            </span>
          </div>
          <p v-if="!dash.revenueByServiceEntries.length" class="text-slate-500 text-sm text-center py-2">
            Aucune transaction
          </p>
        </div>

        <!-- Recent transactions -->
        <div v-if="dash.daily.recentTransactions?.length" class="card space-y-2">
          <h3 class="text-sm font-semibold text-slate-300">Activité récente</h3>
          <div
            v-for="tx in dash.daily.recentTransactions"
            :key="tx.id"
            class="flex items-center justify-between text-sm py-1 border-b border-slate-700/50 last:border-0"
          >
            <div>
              <span
                class="font-medium"
                :class="tx.cancelledAt ? 'line-through text-slate-500' : ''"
              >{{ tx.serviceName }}</span>
              <span class="text-slate-500 text-xs ml-2">{{ formatTime(tx.createdAt) }}</span>
            </div>
            <span
              class="text-xs px-2 py-0.5 rounded-full"
              :class="tx.cancelledAt
                ? 'bg-red-900/50 text-red-300'
                : METHOD_BADGE[tx.paymentMethod]"
            >{{ tx.cancelledAt ? 'Annulée' : METHOD_LABELS[tx.paymentMethod] }}</span>
          </div>
        </div>

        <!-- Closing report button (feature 5) -->
        <button
          @click="showClosingReport = true"
          class="w-full flex items-center justify-center gap-2 py-3 rounded-xl border border-slate-600 text-slate-300 hover:bg-slate-700 text-sm font-medium transition-colors"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z" />
          </svg>
          Imprimer la clôture du jour
        </button>

      </template>
    </template>

    <!-- ═══════════════ MONTHLY TAB ═══════════════ -->
    <template v-if="activeTab === 'monthly'">

      <!-- Month picker -->
      <input
        v-model="selectedMonth"
        type="month"
        class="input-field"
        @change="dash.loadMonthly(selectedMonth)"
      />

      <div v-if="dash.loadingMonthly" class="text-center py-10 text-slate-400 text-sm">
        Chargement...
      </div>

      <template v-else-if="dash.monthly">

        <!-- KPI cards -->
        <div class="grid grid-cols-2 gap-3">
          <div class="card">
            <p class="text-xs text-slate-400 uppercase tracking-wide">Recettes</p>
            <p class="text-2xl font-bold mt-1 text-brand-400">
              {{ dash.formatFcfa(dash.monthly.totalRevenue) }}
            </p>
          </div>
          <div class="card">
            <p class="text-xs text-slate-400 uppercase tracking-wide">Lavages</p>
            <p class="text-3xl font-bold mt-1">{{ dash.monthly.totalVehicles }}</p>
          </div>
          <div class="card">
            <p class="text-xs text-slate-400 uppercase tracking-wide">Profit estimé</p>
            <p class="text-2xl font-bold mt-1 text-green-400">
              {{ dash.formatFcfa(dash.monthly.estimatedProfit) }}
            </p>
            <p class="text-xs text-slate-500 mt-0.5">≈40% marge</p>
          </div>
          <div class="card">
            <p class="text-xs text-slate-400 uppercase tracking-wide">Clients actifs</p>
            <p class="text-3xl font-bold mt-1">{{ dash.monthly.activeClients }}</p>
            <p v-if="dash.monthly.expiringIn7Days > 0"
               class="text-xs text-amber-400 mt-0.5">
              {{ dash.monthly.expiringIn7Days }} expirent bientôt
            </p>
          </div>

          <!-- Expenses card (feature 9) -->
          <div class="card col-span-2">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-xs text-slate-400 uppercase tracking-wide">Dépenses du mois</p>
                <p v-if="loadingExpenses" class="text-slate-500 text-sm mt-1">Chargement...</p>
                <p v-else class="text-2xl font-bold mt-1 text-red-400">
                  {{ dash.formatFcfa(monthlyExpenseTotal) }}
                </p>
              </div>
              <RouterLink to="/expenses" class="text-xs text-sky-400 underline">Voir détail</RouterLink>
            </div>
          </div>
        </div>

        <!-- 30-day revenue curve -->
        <div class="card">
          <h3 class="text-sm font-semibold text-slate-300 mb-3">Courbe de recettes</h3>
          <RevenueChart :points="dash.monthly.dailyCurve" />
        </div>

        <!-- Top services -->
        <div v-if="dash.monthly.topServices?.length" class="card space-y-3">
          <h3 class="text-sm font-semibold text-slate-300">Top services</h3>
          <div
            v-for="(svc, i) in dash.monthly.topServices"
            :key="svc.name"
            class="flex items-center gap-3"
          >
            <span class="text-slate-500 font-bold text-sm w-5 text-center">{{ i + 1 }}</span>
            <div class="flex-1 min-w-0">
              <div class="flex justify-between text-sm">
                <span class="truncate font-medium">{{ svc.name }}</span>
                <span class="text-brand-400 font-semibold shrink-0 ml-2">
                  {{ dash.formatFcfa(svc.revenue) }}
                </span>
              </div>
              <p class="text-xs text-slate-500 mt-0.5">{{ svc.count }} lavage{{ svc.count > 1 ? 's' : '' }}</p>
            </div>
          </div>
        </div>

      </template>
    </template>

  </div>

  <!-- ═══════════════ CLOSING REPORT MODAL (feature 5) ═══════════════ -->
  <Teleport to="body">
    <div
      v-if="showClosingReport && dash.daily"
      class="fixed inset-0 bg-black/80 flex items-end sm:items-center justify-center z-50 print:hidden"
      @click.self="showClosingReport = false"
    >
      <div class="bg-slate-800 rounded-t-2xl sm:rounded-2xl w-full max-w-md max-h-[90vh] overflow-y-auto">
        <!-- Modal header (screen only) -->
        <div class="flex items-center justify-between p-4 border-b border-slate-700 print:hidden">
          <h3 class="font-semibold">Clôture du {{ formatDateFr(dash.daily.date) }}</h3>
          <div class="flex gap-2">
            <button
              @click="doPrint"
              class="btn-primary text-sm py-1.5 px-4"
            >Imprimer</button>
            <button @click="showClosingReport = false" class="btn-secondary text-sm py-1.5 px-3">✕</button>
          </div>
        </div>

        <!-- Report content -->
        <div id="closing-report-content" class="p-5 space-y-4 text-sm">
          <div class="text-center space-y-0.5">
            <p class="text-lg font-bold">SkyCarWash</p>
            <p class="text-slate-400 text-xs">Clôture de caisse — {{ formatDateFr(dash.daily.date) }}</p>
          </div>

          <hr class="border-slate-700" />

          <!-- Summary -->
          <div class="space-y-2">
            <div class="flex justify-between">
              <span class="text-slate-400">Véhicules lavés</span>
              <span class="font-bold">{{ dash.daily.vehiclesWashed }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400">Transactions annulées</span>
              <span :class="dash.daily.cancelledCount > 0 ? 'text-red-400 font-medium' : 'font-bold'">
                {{ dash.daily.cancelledCount }}
                <span v-if="dash.daily.cancelledCount > 0" class="text-xs">({{ dash.formatFcfa(dash.daily.cancelledAmount) }})</span>
              </span>
            </div>
          </div>

          <hr class="border-slate-700" />

          <!-- By payment method -->
          <div class="space-y-2">
            <p class="font-semibold text-slate-300">Recettes par mode de paiement</p>
            <div
              v-for="[method, amount] in dash.revenueByMethodEntries"
              :key="method"
              class="flex justify-between"
            >
              <span class="text-slate-400">{{ METHOD_LABELS[method] ?? method }}</span>
              <span class="font-semibold">{{ dash.formatFcfa(amount) }}</span>
            </div>
            <p v-if="!dash.revenueByMethodEntries.length" class="text-slate-500 text-center">Aucune transaction</p>
          </div>

          <hr class="border-slate-700" />

          <!-- By service -->
          <div class="space-y-2">
            <p class="font-semibold text-slate-300">Recettes par service</p>
            <div
              v-for="[service, amount] in dash.revenueByServiceEntries"
              :key="service"
              class="flex justify-between"
            >
              <span class="text-slate-400 truncate">{{ service }}</span>
              <span class="font-semibold shrink-0 ml-3">{{ dash.formatFcfa(amount) }}</span>
            </div>
          </div>

          <hr class="border-slate-700" />

          <!-- Total -->
          <div class="flex justify-between text-base font-bold">
            <span>TOTAL NET</span>
            <span class="text-brand-400">{{ dash.formatFcfa(dash.daily.totalRevenue) }}</span>
          </div>

          <p class="text-xs text-slate-500 text-center pt-2">
            Imprimé le {{ new Date().toLocaleString('fr-FR') }}
          </p>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboard'
import RevenueChart from '@/components/RevenueChart.vue'
import api from '@/api/axios'

const dash = useDashboardStore()

const TABS = [
  { value: 'daily',   label: "Aujourd'hui" },
  { value: 'monthly', label: 'Ce mois'     }
]

const METHOD_LABELS = {
  CASH:       'Espèces',
  ORANGE:     'Orange Money',
  MOOV:       'Moov Money',
  ABONNEMENT: 'Abonnement'
}

const METHOD_COLORS = {
  CASH:       'bg-green-500',
  ORANGE:     'bg-orange-500',
  MOOV:       'bg-blue-500',
  ABONNEMENT: 'bg-purple-500'
}

const METHOD_BADGE = {
  CASH:       'bg-green-900/50 text-green-300',
  ORANGE:     'bg-orange-900/50 text-orange-300',
  MOOV:       'bg-blue-900/50 text-blue-300',
  ABONNEMENT: 'bg-purple-900/50 text-purple-300'
}

const activeTab          = ref('daily')
const selectedDate       = ref(dash.selectedDate)
const selectedMonth      = ref(dash.selectedMonth)
const showClosingReport  = ref(false)

// ── Expenses (feature 9 - monthly widget) ─── //
const monthlyExpenseTotal = ref(0)
const loadingExpenses     = ref(false)

async function loadMonthlyExpenses(month) {
  loadingExpenses.value = true
  try {
    const { data } = await api.get('/expenses', { params: { month } })
    monthlyExpenseTotal.value = data.reduce((sum, e) => sum + e.amount, 0)
  } catch {
    monthlyExpenseTotal.value = 0
  } finally {
    loadingExpenses.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'monthly') loadMonthlyExpenses(selectedMonth.value)
})

onMounted(() => {
  dash.loadDaily()
  dash.loadMonthly()
  dash.connectWebSocket()
})

onUnmounted(() => {
  dash.disconnectWebSocket()
})

function pct(value, total) {
  if (!total) return 0
  return Math.round((value / total) * 100)
}

function deltaClass(delta) {
  if (delta > 0) return 'text-green-400'
  if (delta < 0) return 'text-red-400'
  return 'text-slate-500'
}

function formatTime(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
}

function formatDateFr(iso) {
  return new Date(iso).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
}

function formatCacheTime(iso) {
  return new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
}

function doPrint() {
  window.print()
}
</script>

<style>
@media print {
  body > * { display: none !important; }
  #closing-report-content {
    display: block !important;
    position: fixed;
    inset: 0;
    background: white;
    color: black;
    padding: 2rem;
    font-size: 14px;
  }
  #closing-report-content .text-slate-400,
  #closing-report-content .text-slate-300,
  #closing-report-content .text-slate-500 {
    color: #555 !important;
  }
  #closing-report-content .text-brand-400,
  #closing-report-content .text-green-400 {
    color: #000 !important;
    font-weight: bold;
  }
  #closing-report-content hr {
    border-color: #ccc;
  }
}
</style>
