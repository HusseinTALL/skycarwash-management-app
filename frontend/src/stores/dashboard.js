import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { Client as StompClient } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import api from '@/api/axios'
import { WS_EVENTS } from '@/constants'

export const useDashboardStore = defineStore('dashboard', () => {

  // ── State ──────────────────────────────────────────────────────────── //
  const daily         = ref(null)   // DailyDashboardDto
  const monthly       = ref(null)   // MonthlyDashboardDto
  const loadingDaily  = ref(false)
  const loadingMonthly = ref(false)

  const selectedDate  = ref(todayStr())
  const selectedMonth = ref(currentMonthStr())

  /** Whether the dashboard is receiving live WebSocket updates */
  const wsConnected   = ref(false)
  /** Set to true when a malformed WS message is received */
  const wsDataError   = ref(false)

  // ── Derived helpers ────────────────────────────────────────────────── //
  const revenueByMethodEntries = computed(() =>
    Object.entries(daily.value?.revenueByMethod ?? {})
          .sort((a, b) => b[1] - a[1])
  )

  const revenueByServiceEntries = computed(() =>
    Object.entries(daily.value?.revenueByService ?? {})
          .sort((a, b) => b[1] - a[1])
  )

  // ── API loads ──────────────────────────────────────────────────────── //
  async function loadDaily(date = selectedDate.value) {
    selectedDate.value = date
    loadingDaily.value = true
    try {
      const { data } = await api.get('/dashboard/daily', { params: { date } })
      daily.value = data
    } finally {
      loadingDaily.value = false
    }
  }

  async function loadMonthly(month = selectedMonth.value) {
    selectedMonth.value = month
    loadingMonthly.value = true
    try {
      const { data } = await api.get('/dashboard/monthly', { params: { month } })
      monthly.value = data
    } finally {
      loadingMonthly.value = false
    }
  }

  // ── WebSocket (STOMP over SockJS) ──────────────────────────────────── //
  let stompClient = null

  function connectWebSocket() {
    if (stompClient?.active) return

    stompClient = new StompClient({
      webSocketFactory: () => new SockJS((import.meta.env.VITE_API_BASE_URL || '') + '/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        wsConnected.value = true

        stompClient.subscribe('/topic/dashboard', (msg) => {
          try {
            const event = JSON.parse(msg.body)
            wsDataError.value = false
            // Refresh the current day's data on any transaction event
            if ([WS_EVENTS.TRANSACTION_CREATED, WS_EVENTS.TRANSACTION_CANCELLED].includes(event.event)) {
              loadDaily(selectedDate.value)
            }
          } catch (err) {
            console.warn('[WS] Malformed dashboard event — could not parse message:', err, msg.body)
            wsDataError.value = true
          }
        })
      },
      onDisconnect: () => { wsConnected.value = false },
      onStompError:  () => { wsConnected.value = false }
    })

    stompClient.activate()
  }

  function disconnectWebSocket() {
    stompClient?.deactivate()
    wsConnected.value = false
  }

  // ── Formatters (shared with view) ──────────────────────────────────── //
  function formatFcfa(amount) {
    return new Intl.NumberFormat('fr-FR').format(amount) + ' FCFA'
  }

  function formatDelta(delta) {
    if (delta === 0) return '='
    return (delta > 0 ? '+' : '') + new Intl.NumberFormat('fr-FR').format(delta)
  }

  return {
    daily, monthly, loadingDaily, loadingMonthly,
    selectedDate, selectedMonth, wsConnected, wsDataError,
    revenueByMethodEntries, revenueByServiceEntries,
    loadDaily, loadMonthly,
    connectWebSocket, disconnectWebSocket,
    formatFcfa, formatDelta
  }
})

// ── Helpers ──────────────────────────────────────────────────────────── //
function todayStr() {
  return new Date().toISOString().slice(0, 10)
}

function currentMonthStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}
