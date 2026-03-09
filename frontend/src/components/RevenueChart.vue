<template>
  <div class="relative" style="height: 180px;">
    <Line :data="chartData" :options="chartOptions" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale, LinearScale,
  PointElement, LineElement,
  Tooltip, Filler
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Filler)

const props = defineProps({
  /** Array of { date: 'yyyy-MM-dd', revenue: number } */
  points: { type: Array, required: true }
})

const chartData = computed(() => ({
  labels: props.points.map(p => {
    const [, , day] = p.date.split('-')
    return day   // show only day number on x-axis
  }),
  datasets: [{
    label: 'Recettes (FCFA)',
    data: props.points.map(p => p.revenue),
    borderColor: '#0ea5e9',
    backgroundColor: 'rgba(14,165,233,0.10)',
    borderWidth: 2,
    pointRadius: 2,
    pointHoverRadius: 5,
    tension: 0.35,
    fill: true
  }]
}))

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: ctx => new Intl.NumberFormat('fr-FR').format(ctx.raw) + ' FCFA'
      }
    }
  },
  scales: {
    x: {
      grid:  { color: 'rgba(255,255,255,0.05)' },
      ticks: { color: '#94a3b8', font: { size: 10 } }
    },
    y: {
      grid:  { color: 'rgba(255,255,255,0.05)' },
      ticks: {
        color: '#94a3b8',
        font: { size: 10 },
        callback: v => new Intl.NumberFormat('fr-FR', { notation: 'compact' }).format(v)
      },
      beginAtZero: true
    }
  }
}
</script>
