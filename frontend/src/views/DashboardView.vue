<template>
  <div class="p-4 space-y-4 pb-6">

    <!-- Header + WS indicator -->
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-bold">Dashboard</h2>
      <div class="flex items-center gap-1.5 text-xs"
           :class="dash.wsConnected ? 'text-green-400' : 'text-slate-500'">
        <span class="w-2 h-2 rounded-full"
              :class="dash.wsConnected ? 'bg-green-400 animate-pulse' : 'bg-slate-500'"/>
        {{ dash.wsConnected ? 'En direct' : 'Hors-ligne' }}
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
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import RevenueChart from '@/components/RevenueChart.vue'

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

const activeTab     = ref('daily')
const selectedDate  = ref(dash.selectedDate)
const selectedMonth = ref(dash.selectedMonth)

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
</script>
