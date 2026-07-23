import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useStockStore } from '@/stores/stock'

/**
 * Single source of truth for the application navigation.
 * Items are filtered by the current user's role and enriched with live
 * badges (e.g. the low-stock counter). Consumed by both the desktop
 * sidebar and the mobile drawer so the two never drift apart.
 */
export function useNavigation() {
  const auth = useAuthStore()
  const stock = useStockStore()

  const items = computed(() => {
    const role = auth.role
    const all = [
      { label: 'Caisse',       to: '/caisse',       icon: 'pi pi-wallet',       roles: ['EMPLOYEE', 'MANAGER'] },
      { label: 'Dashboard',    to: '/dashboard',    icon: 'pi pi-chart-line',   roles: ['MANAGER', 'PARTNER'] },
      { label: 'Rapports',     to: '/inspections',  icon: 'pi pi-camera',       roles: ['EMPLOYEE', 'MANAGER', 'PARTNER'] },
      { label: 'Historique',   to: '/transactions', icon: 'pi pi-history',      roles: ['MANAGER', 'PARTNER'] },
      { label: 'Clients',      to: '/clients',      icon: 'pi pi-users',        roles: ['MANAGER'] },
      { label: 'Stock',        to: '/stock',        icon: 'pi pi-box',          roles: ['MANAGER'],
        badge: () => (stock.lowStockCount > 0 ? stock.lowStockCount : null) },
      { label: 'Dépenses',     to: '/expenses',     icon: 'pi pi-arrow-down-right', roles: ['MANAGER'] },
      { label: 'Réglages',     to: '/settings',     icon: 'pi pi-cog',          roles: ['MANAGER'] }
    ]

    return all
      .filter((item) => item.roles.includes(role))
      .map((item) => ({
        ...item,
        badgeValue: typeof item.badge === 'function' ? item.badge() : null
      }))
  })

  return { items }
}
