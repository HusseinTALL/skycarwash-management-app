<template>
  <div class="scw-animate-in">
    <PageHeader title="Dashboard" icon="pi pi-chart-line" :subtitle="headerSubtitle">
      <template #actions>
        <Tag
          :severity="dash.wsConnected ? 'success' : 'secondary'"
          :value="dash.wsConnected ? 'En direct' : 'Hors-ligne'"
          rounded
        >
          <template #icon>
            <span
              class="w-1.5 h-1.5 rounded-full mr-1.5"
              :class="dash.wsConnected ? 'bg-emerald-400 animate-pulse' : 'bg-slate-500'"
            />
          </template>
        </Tag>
        <SelectButton
          v-model="activeTab"
          :options="TABS"
          option-label="label"
          option-value="value"
          :allow-empty="false"
          aria-label="Période"
        />
      </template>
    </PageHeader>

    <!-- Stale cache / data-error banners -->
    <Message
      v-if="isCached"
      severity="warn"
      :closable="false"
      icon="pi pi-clock"
      class="mb-4"
    >
      Données en cache ({{ formatCacheTime(isCached) }}) — mode hors-ligne
    </Message>
    <Message v-if="dash.wsDataError" severity="error" :closable="false" class="mb-4">
      Données temps réel incorrectes reçues
    </Message>

    <!-- ══════════════════ DAILY ══════════════════ -->
    <template v-if="activeTab === 'daily'">
      <div class="flex flex-wrap items-center gap-3 mb-5">
        <DatePicker
          v-model="dailyDate"
          date-format="dd/mm/yy"
          show-icon
          icon-display="input"
          :max-date="new Date()"
          class="w-full sm:w-56"
          @update:model-value="onDailyDateChange"
        />
        <Button
          label="Clôture du jour"
          icon="pi pi-print"
          severity="secondary"
          outlined
          :disabled="!dash.daily"
          @click="showClosingReport = true"
        />
      </div>

      <!-- KPI cards -->
      <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3 sm:gap-4 mb-5">
        <KpiCard
          title="Véhicules lavés"
          :value="dash.daily?.vehiclesWashed ?? 0"
          icon="pi pi-check-circle"
          accent="#38bdf8"
          :delta="dash.daily?.vehiclesDelta"
          :delta-text="dash.formatDelta(dash.daily?.vehiclesDelta ?? 0)"
          delta-label="vs sem. passée"
          :loading="dash.loadingDaily"
        />
        <KpiCard
          title="Recettes"
          :value="dash.formatFcfa(dash.daily?.totalRevenue ?? 0)"
          icon="pi pi-money-bill"
          accent="#34d399"
          :delta="dash.daily?.revenueDelta"
          :delta-text="dash.formatFcfa(dash.daily?.revenueDelta ?? 0)"
          delta-label="vs sem. passée"
          :loading="dash.loadingDaily"
        />
        <KpiCard
          title="Panier moyen"
          :value="dash.formatFcfa(avgTicket)"
          icon="pi pi-tag"
          accent="#c084fc"
          hint="Recettes / véhicule"
          :loading="dash.loadingDaily"
        />
        <KpiCard
          title="Annulations"
          :value="dash.daily?.cancelledCount ?? 0"
          icon="pi pi-times-circle"
          accent="#f87171"
          :hint="dash.daily?.cancelledCount ? dash.formatFcfa(dash.daily.cancelledAmount) : 'Aucune'"
          :loading="dash.loadingDaily"
        />
      </div>

      <div class="grid gap-4 lg:grid-cols-5">
        <!-- Payment method doughnut -->
        <Card class="lg:col-span-2">
          <template #title><span class="text-base font-semibold">Par mode de paiement</span></template>
          <template #content>
            <div v-if="dash.loadingDaily" class="grid place-items-center h-64">
              <Skeleton shape="circle" size="10rem" />
            </div>
            <div v-else-if="methodChart" class="h-64">
              <Chart type="doughnut" :data="methodChart" :options="doughnutOptions()" class="h-full" />
            </div>
            <EmptyState v-else icon="pi pi-wallet" text="Aucune transaction" />
          </template>
        </Card>

        <!-- Revenue by service bar -->
        <Card class="lg:col-span-3">
          <template #title><span class="text-base font-semibold">Recettes par service</span></template>
          <template #content>
            <div v-if="dash.loadingDaily" class="h-64 flex flex-col gap-3 justify-center">
              <Skeleton v-for="i in 5" :key="i" height="1.6rem" />
            </div>
            <div v-else-if="serviceChart" class="h-64">
              <Chart type="bar" :data="serviceChart" :options="barOptions({ horizontal: true })" class="h-full" />
            </div>
            <EmptyState v-else icon="pi pi-chart-bar" text="Aucune transaction" />
          </template>
        </Card>
      </div>

      <!-- Recent activity -->
      <Card class="mt-4">
        <template #title>
          <div class="flex items-center justify-between">
            <span class="text-base font-semibold">Activité récente</span>
            <RouterLink to="/transactions" class="text-xs text-primary hover:underline">Tout voir</RouterLink>
          </div>
        </template>
        <template #content>
          <DataTable
            :value="dash.daily?.recentTransactions ?? []"
            :loading="dash.loadingDaily"
            data-key="id"
            size="small"
            :pt="{ table: { style: 'min-width: 30rem' } }"
          >
            <template #empty>
              <EmptyState icon="pi pi-inbox" text="Aucune activité aujourd'hui" />
            </template>
            <Column header="Service">
              <template #body="{ data }">
                <span :class="data.cancelledAt ? 'line-through text-slate-500' : 'font-medium text-slate-200'">
                  {{ data.serviceName }}
                </span>
              </template>
            </Column>
            <Column header="Heure">
              <template #body="{ data }">
                <span class="text-slate-400 text-sm">{{ formatTime(data.createdAt) }}</span>
              </template>
            </Column>
            <Column header="Paiement" style="width: 10rem">
              <template #body="{ data }">
                <Tag
                  :severity="data.cancelledAt ? 'danger' : methodSeverity(data.paymentMethod)"
                  :value="data.cancelledAt ? 'Annulée' : (METHOD_LABELS[data.paymentMethod] ?? data.paymentMethod)"
                  rounded
                />
              </template>
            </Column>
          </DataTable>
        </template>
      </Card>
    </template>

    <!-- ══════════════════ MONTHLY ══════════════════ -->
    <template v-else>
      <div class="mb-5">
        <DatePicker
          v-model="monthlyDate"
          view="month"
          date-format="mm/yy"
          show-icon
          icon-display="input"
          :max-date="new Date()"
          class="w-full sm:w-56"
          @update:model-value="onMonthlyDateChange"
        />
      </div>

      <!-- KPI cards -->
      <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3 sm:gap-4 mb-5">
        <KpiCard
          title="Recettes du mois"
          :value="dash.formatFcfa(dash.monthly?.totalRevenue ?? 0)"
          icon="pi pi-money-bill"
          accent="#34d399"
          :loading="dash.loadingMonthly"
        />
        <KpiCard
          title="Lavages"
          :value="dash.monthly?.totalVehicles ?? 0"
          icon="pi pi-check-circle"
          accent="#38bdf8"
          :loading="dash.loadingMonthly"
        />
        <KpiCard
          title="Profit estimé"
          :value="dash.formatFcfa(dash.monthly?.estimatedProfit ?? 0)"
          icon="pi pi-chart-line"
          accent="#c084fc"
          hint="≈ 40% de marge"
          :loading="dash.loadingMonthly"
        />
        <KpiCard
          title="Clients actifs"
          :value="dash.monthly?.activeClients ?? 0"
          icon="pi pi-users"
          accent="#fbbf24"
          :hint="dash.monthly?.expiringIn7Days ? `${dash.monthly.expiringIn7Days} expirent bientôt` : 'Abonnements en cours'"
          :loading="dash.loadingMonthly"
        />
      </div>

      <!-- Expenses banner widget -->
      <div class="scw-panel px-4 py-3.5 mb-4 flex items-center justify-between gap-3">
        <div class="flex items-center gap-3">
          <span class="grid place-items-center h-10 w-10 rounded-xl bg-red-500/10 text-red-400">
            <i class="pi pi-arrow-down" />
          </span>
          <div>
            <p class="scw-eyebrow">Dépenses du mois</p>
            <Skeleton v-if="loadingExpenses" width="7rem" height="1.5rem" class="mt-1" />
            <p v-else class="text-xl font-bold text-red-400 mt-0.5">{{ dash.formatFcfa(monthlyExpenseTotal) }}</p>
          </div>
        </div>
        <RouterLink to="/expenses">
          <Button label="Détail" icon="pi pi-arrow-right" icon-pos="right" text size="small" />
        </RouterLink>
      </div>

      <div class="grid gap-4 lg:grid-cols-3">
        <!-- Revenue trend -->
        <Card class="lg:col-span-2">
          <template #title><span class="text-base font-semibold">Tendance des recettes</span></template>
          <template #subtitle><span class="text-xs text-slate-500">30 derniers jours</span></template>
          <template #content>
            <div v-if="dash.loadingMonthly" class="h-72"><Skeleton height="100%" /></div>
            <div v-else-if="revenueTrend" class="h-72">
              <Chart type="line" :data="revenueTrend" :options="lineOptions()" class="h-full" />
            </div>
            <EmptyState v-else icon="pi pi-chart-line" text="Pas de données" />
          </template>
        </Card>

        <!-- Weekly activity -->
        <Card>
          <template #title><span class="text-base font-semibold">Activité hebdo</span></template>
          <template #subtitle><span class="text-xs text-slate-500">7 derniers jours</span></template>
          <template #content>
            <div v-if="dash.loadingMonthly" class="h-72"><Skeleton height="100%" /></div>
            <div v-else-if="weeklyActivity" class="h-72">
              <Chart type="line" :data="weeklyActivity" :options="lineOptions()" class="h-full" />
            </div>
            <EmptyState v-else icon="pi pi-calendar" text="Pas de données" />
          </template>
        </Card>
      </div>

      <!-- Top services -->
      <Card class="mt-4">
        <template #title><span class="text-base font-semibold">Top services</span></template>
        <template #content>
          <div v-if="dash.loadingMonthly" class="space-y-4">
            <Skeleton v-for="i in 4" :key="i" height="2rem" />
          </div>
          <div v-else-if="dash.monthly?.topServices?.length" class="space-y-4">
            <div v-for="(svc, i) in dash.monthly.topServices" :key="svc.name" class="flex items-center gap-4">
              <span
                class="grid place-items-center h-8 w-8 rounded-lg text-sm font-bold shrink-0"
                :style="{ background: rankBg(i), color: rankColor(i) }"
              >{{ i + 1 }}</span>
              <div class="flex-1 min-w-0">
                <div class="flex justify-between items-baseline gap-3 mb-1.5">
                  <span class="truncate font-medium text-slate-200">{{ svc.name }}</span>
                  <span class="text-primary font-semibold shrink-0 text-sm">{{ dash.formatFcfa(svc.revenue) }}</span>
                </div>
                <ProgressBar
                  :value="topServicePct(svc.revenue)"
                  :show-value="false"
                  style="height: 6px"
                />
                <p class="text-xs text-slate-500 mt-1">{{ svc.count }} lavage{{ svc.count > 1 ? 's' : '' }}</p>
              </div>
            </div>
          </div>
          <EmptyState v-else icon="pi pi-star" text="Pas encore de services" />
        </template>
      </Card>
    </template>

    <!-- ══════════════════ CLOSING REPORT DIALOG ══════════════════ -->
    <Dialog
      v-model:visible="showClosingReport"
      modal
      :header="`Clôture du ${dash.daily ? formatDateFr(dash.daily.date) : ''}`"
      class="w-full max-w-md mx-3"
      :dismissable-mask="true"
    >
      <div id="closing-report-content" v-if="dash.daily" class="space-y-4 text-sm">
        <div class="text-center space-y-0.5">
          <p class="text-lg font-bold">SkyCarWash</p>
          <p class="text-slate-400 text-xs">Clôture de caisse — {{ formatDateFr(dash.daily.date) }}</p>
        </div>
        <hr class="border-slate-700" />
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
        <div class="space-y-2">
          <p class="font-semibold text-slate-300">Recettes par mode de paiement</p>
          <div v-for="[method, amount] in dash.revenueByMethodEntries" :key="method" class="flex justify-between">
            <span class="text-slate-400">{{ METHOD_LABELS[method] ?? method }}</span>
            <span class="font-semibold">{{ dash.formatFcfa(amount) }}</span>
          </div>
          <p v-if="!dash.revenueByMethodEntries.length" class="text-slate-500 text-center">Aucune transaction</p>
        </div>
        <hr class="border-slate-700" />
        <div class="space-y-2">
          <p class="font-semibold text-slate-300">Recettes par service</p>
          <div v-for="[service, amount] in dash.revenueByServiceEntries" :key="service" class="flex justify-between">
            <span class="text-slate-400 truncate">{{ service }}</span>
            <span class="font-semibold shrink-0 ml-3">{{ dash.formatFcfa(amount) }}</span>
          </div>
        </div>
        <hr class="border-slate-700" />
        <div class="flex justify-between text-base font-bold">
          <span>TOTAL NET</span>
          <span class="text-primary">{{ dash.formatFcfa(dash.daily.totalRevenue) }}</span>
        </div>
        <p class="text-xs text-slate-500 text-center pt-2">Imprimé le {{ new Date().toLocaleString('fr-FR') }}</p>
      </div>

      <template #footer>
        <Button label="Fermer" severity="secondary" text @click="showClosingReport = false" />
        <Button label="Imprimer" icon="pi pi-print" @click="doPrint" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboard'
import { useChartTheme, METHOD_COLORS, CHART_PALETTE } from '@/composables/useChartTheme'
import PageHeader from '@/components/ui/PageHeader.vue'
import KpiCard from '@/components/ui/KpiCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import api from '@/api/axios'

import Card from 'primevue/card'
import Chart from 'primevue/chart'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Skeleton from 'primevue/skeleton'
import SelectButton from 'primevue/selectbutton'
import DatePicker from 'primevue/datepicker'
import ProgressBar from 'primevue/progressbar'
import Dialog from 'primevue/dialog'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

const dash = useDashboardStore()
const { lineOptions, barOptions, doughnutOptions } = useChartTheme()

const TABS = [
  { value: 'daily', label: "Aujourd'hui" },
  { value: 'monthly', label: 'Ce mois' }
]

const METHOD_LABELS = {
  CASH: 'Espèces',
  ORANGE: 'Orange Money',
  MOOV: 'Moov Money',
  ABONNEMENT: 'Abonnement'
}
const METHOD_SEVERITY = {
  CASH: 'success',
  ORANGE: 'warn',
  MOOV: 'info',
  ABONNEMENT: 'help'
}
const methodSeverity = (m) => METHOD_SEVERITY[m] ?? 'secondary'

const activeTab = ref('daily')
const dailyDate = ref(parseISODate(dash.selectedDate))
const monthlyDate = ref(parseISOMonth(dash.selectedMonth))
const showClosingReport = ref(false)

const headerSubtitle = computed(() =>
  activeTab.value === 'daily' ? "Vue d'ensemble du jour" : "Performance mensuelle"
)
const isCached = computed(() =>
  activeTab.value === 'daily' ? dash.dailyCachedAt : dash.monthlyCachedAt
)
const avgTicket = computed(() => {
  const n = dash.daily?.vehiclesWashed
  if (!n) return 0
  return Math.round((dash.daily.totalRevenue ?? 0) / n)
})

// ── Charts ─────────────────────────────────────────────────────────────── //
const methodChart = computed(() => {
  const entries = dash.revenueByMethodEntries
  if (!entries.length) return null
  return {
    labels: entries.map(([m]) => METHOD_LABELS[m] ?? m),
    datasets: [{
      data: entries.map(([, v]) => v),
      backgroundColor: entries.map(([m]) => METHOD_COLORS[m] ?? CHART_PALETTE.slate),
      borderWidth: 0,
      hoverOffset: 6
    }]
  }
})

const serviceChart = computed(() => {
  const entries = dash.revenueByServiceEntries.slice(0, 6)
  if (!entries.length) return null
  return {
    labels: entries.map(([s]) => s),
    datasets: [{
      data: entries.map(([, v]) => v),
      backgroundColor: CHART_PALETTE.primarySoft,
      hoverBackgroundColor: 'rgba(56, 189, 248, 0.32)',
      borderColor: CHART_PALETTE.primary,
      borderWidth: 1.5,
      borderRadius: 6,
      barThickness: 18
    }]
  }
})

const revenueTrend = computed(() => {
  const points = dash.monthly?.dailyCurve
  if (!points?.length) return null
  return {
    labels: points.map((p) => p.date.split('-')[2]),
    datasets: [{
      data: points.map((p) => p.revenue),
      borderColor: CHART_PALETTE.primary,
      backgroundColor: CHART_PALETTE.primarySoft,
      borderWidth: 2,
      pointRadius: 0,
      pointHoverRadius: 5,
      tension: 0.35,
      fill: true
    }]
  }
})

const weeklyActivity = computed(() => {
  const points = dash.monthly?.dailyCurve?.slice(-7)
  if (!points?.length) return null
  const days = ['Dim', 'Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam']
  return {
    labels: points.map((p) => days[new Date(p.date).getDay()]),
    datasets: [{
      data: points.map((p) => p.revenue),
      borderColor: CHART_PALETTE.green,
      backgroundColor: 'rgba(52, 211, 153, 0.14)',
      borderWidth: 2,
      pointRadius: 3,
      pointBackgroundColor: CHART_PALETTE.green,
      tension: 0.4,
      fill: true
    }]
  }
})

const maxTopService = computed(() =>
  Math.max(1, ...(dash.monthly?.topServices ?? []).map((s) => s.revenue))
)
const topServicePct = (rev) => Math.round((rev / maxTopService.value) * 100)

function rankBg(i) {
  const c = [CHART_PALETTE.amber, '#cbd5e1', '#f59e0b'][i] || CHART_PALETTE.primary
  return `color-mix(in srgb, ${c}, transparent 84%)`
}
function rankColor(i) {
  return [CHART_PALETTE.amber, '#cbd5e1', '#f59e0b'][i] || CHART_PALETTE.primary
}

// ── Monthly expenses widget ────────────────────────────────────────────── //
const monthlyExpenseTotal = ref(0)
const loadingExpenses = ref(false)

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

// ── Date handling ──────────────────────────────────────────────────────── //
function onDailyDateChange(val) {
  if (!val) return
  dash.loadDaily(toISODate(val))
}
function onMonthlyDateChange(val) {
  if (!val) return
  const month = toISOMonth(val)
  dash.loadMonthly(month)
  loadMonthlyExpenses(month)
}

watch(activeTab, (tab) => {
  if (tab === 'monthly') loadMonthlyExpenses(toISOMonth(monthlyDate.value))
})

onMounted(() => {
  dash.loadDaily()
  dash.loadMonthly()
  dash.connectWebSocket()
})
onUnmounted(() => {
  dash.disconnectWebSocket()
})

// ── Formatters / helpers ───────────────────────────────────────────────── //
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

function parseISODate(str) {
  if (!str) return new Date()
  const [y, m, d] = str.split('-').map(Number)
  return new Date(y, m - 1, d)
}
function parseISOMonth(str) {
  if (!str) return new Date()
  const [y, m] = str.split('-').map(Number)
  return new Date(y, m - 1, 1)
}
function toISODate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}
function toISOMonth(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
}
</script>
