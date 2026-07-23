/**
 * Shared Chart.js theming so every chart in the app reads as one system:
 * muted slate gridlines, consistent typography, and a brand-aligned palette.
 */
const FONT = "'Inter', ui-sans-serif, system-ui, sans-serif"
const TICK = '#94a3b8'
const GRID = 'rgba(148, 163, 184, 0.10)'

export const CHART_PALETTE = {
  primary: '#38bdf8',
  primarySoft: 'rgba(56, 189, 248, 0.14)',
  green: '#34d399',
  orange: '#fb923c',
  blue: '#60a5fa',
  purple: '#c084fc',
  pink: '#f472b6',
  amber: '#fbbf24',
  slate: '#64748b'
}

/** Categorical colours for the four payment methods. */
export const METHOD_COLORS = {
  CASH: CHART_PALETTE.green,
  ORANGE: CHART_PALETTE.orange,
  MOOV: CHART_PALETTE.blue,
  ABONNEMENT: CHART_PALETTE.purple
}

/** A rotating series used for arbitrary categorical charts. */
export const SERIES_COLORS = [
  CHART_PALETTE.primary,
  CHART_PALETTE.green,
  CHART_PALETTE.orange,
  CHART_PALETTE.purple,
  CHART_PALETTE.pink,
  CHART_PALETTE.amber,
  CHART_PALETTE.blue,
  CHART_PALETTE.slate
]

const fcfa = (v) => new Intl.NumberFormat('fr-FR').format(v) + ' FCFA'
const compact = (v) => new Intl.NumberFormat('fr-FR', { notation: 'compact' }).format(v)

export function useChartTheme() {
  const baseTooltip = {
    backgroundColor: 'rgba(15, 23, 42, 0.96)',
    borderColor: 'rgba(148,163,184,0.18)',
    borderWidth: 1,
    titleColor: '#e2e8f0',
    bodyColor: '#cbd5e1',
    padding: 12,
    cornerRadius: 10,
    titleFont: { family: FONT, weight: '600' },
    bodyFont: { family: FONT }
  }

  /** Options for a currency line/area chart. */
  function lineOptions({ currency = true } = {}) {
    return {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { display: false },
        tooltip: {
          ...baseTooltip,
          callbacks: currency ? { label: (ctx) => fcfa(ctx.raw) } : {}
        }
      },
      scales: {
        x: { grid: { display: false }, ticks: { color: TICK, font: { family: FONT, size: 10 } } },
        y: {
          grid: { color: GRID, drawBorder: false },
          border: { display: false },
          ticks: { color: TICK, font: { family: FONT, size: 10 }, callback: (v) => (currency ? compact(v) : v) },
          beginAtZero: true
        }
      }
    }
  }

  /** Options for a horizontal/vertical bar chart. */
  function barOptions({ currency = true, horizontal = false } = {}) {
    const valueAxis = {
      type: 'linear',
      beginAtZero: true,
      grid: { color: GRID },
      border: { display: false },
      ticks: { color: TICK, font: { family: FONT, size: 10 }, callback: currency ? (v) => compact(v) : undefined }
    }
    const categoryAxis = {
      type: 'category',
      grid: { display: false },
      border: { display: false },
      ticks: { color: TICK, font: { family: FONT, size: 10 } }
    }
    return {
      responsive: true,
      maintainAspectRatio: false,
      indexAxis: horizontal ? 'y' : 'x',
      plugins: {
        legend: { display: false },
        tooltip: {
          ...baseTooltip,
          callbacks: currency ? { label: (ctx) => fcfa(ctx.raw) } : {}
        }
      },
      scales: horizontal ? { x: valueAxis, y: categoryAxis } : { x: categoryAxis, y: valueAxis }
    }
  }

  /** Options for a doughnut/pie chart. */
  function doughnutOptions({ currency = true } = {}) {
    return {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '68%',
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            color: TICK,
            font: { family: FONT, size: 11 },
            usePointStyle: true,
            pointStyle: 'circle',
            padding: 14,
            boxWidth: 8,
            boxHeight: 8
          }
        },
        tooltip: {
          ...baseTooltip,
          callbacks: currency ? { label: (ctx) => `${ctx.label}: ${fcfa(ctx.raw)}` } : {}
        }
      }
    }
  }

  return { lineOptions, barOptions, doughnutOptions, fcfa, compact }
}
