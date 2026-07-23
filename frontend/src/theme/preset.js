import { definePreset } from '@primevue/themes'
import Aura from '@primevue/themes/aura'

/**
 * SkyCarWash design system — a custom PrimeVue preset built on Aura.
 *
 * The brand colour is a sky/cyan (matching the existing `#0ea5e9`) and the
 * application runs permanently in dark mode with a refined slate surface
 * palette inspired by modern SaaS dashboards (Linear, Vercel, Stripe).
 */
const SkyPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50:  '#f0f9ff',
      100: '#e0f2fe',
      200: '#bae6fd',
      300: '#7dd3fc',
      400: '#38bdf8',
      500: '#0ea5e9',
      600: '#0284c7',
      700: '#0369a1',
      800: '#075985',
      900: '#0c4a6e',
      950: '#082f49'
    },
    // Rounded, soft geometry throughout.
    borderRadius: {
      none: '0',
      xs: '4px',
      sm: '6px',
      md: '8px',
      lg: '12px',
      xl: '16px'
    },
    focusRing: {
      width: '2px',
      style: 'solid',
      color: '{primary.400}',
      offset: '2px'
    },
    colorScheme: {
      dark: {
        primary: {
          color: '{primary.400}',
          contrastColor: '#04121f',
          hoverColor: '{primary.300}',
          activeColor: '{primary.200}'
        },
        highlight: {
          background: 'color-mix(in srgb, {primary.400}, transparent 84%)',
          focusBackground: 'color-mix(in srgb, {primary.400}, transparent 76%)',
          color: '{primary.300}',
          focusColor: '{primary.200}'
        },
        // Slate-based neutral surfaces for an enterprise, low-glare feel.
        surface: {
          0:   '#ffffff',
          50:  '#f8fafc',
          100: '#f1f5f9',
          200: '#e2e8f0',
          300: '#cbd5e1',
          400: '#94a3b8',
          500: '#64748b',
          600: '#475569',
          700: '#334155',
          800: '#1e293b',
          900: '#0f172a',
          950: '#020617'
        },
        content: {
          background: '{surface.900}',
          hoverBackground: '{surface.800}',
          borderColor: '{surface.700}',
          color: '{surface.100}',
          hoverColor: '{surface.0}'
        },
        formField: {
          background: '{surface.800}',
          disabledBackground: '{surface.700}',
          filledBackground: '{surface.800}',
          borderColor: '{surface.700}',
          hoverBorderColor: '{surface.600}',
          focusBorderColor: '{primary.400}',
          color: '{surface.0}',
          placeholderColor: '{surface.400}'
        },
        text: {
          color: '{surface.100}',
          hoverColor: '{surface.0}',
          mutedColor: '{surface.400}',
          hoverMutedColor: '{surface.300}'
        }
      }
    }
  },
  components: {
    card: {
      colorScheme: {
        dark: {
          root: {
            background: '{surface.800}',
            color: '{surface.100}',
            shadow: '0 1px 2px rgba(0,0,0,0.35), 0 1px 3px rgba(0,0,0,0.25)'
          },
          subtitle: { color: '{surface.400}' }
        }
      }
    },
    datatable: {
      colorScheme: {
        dark: {
          headerCell: { background: '{surface.900}', color: '{surface.300}' },
          row: { background: '{surface.800}' }
        }
      }
    }
  }
})

export default SkyPreset
