import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { usePortalStore } from '@/stores/portal'

const routes = [
  {
    path: '/landing',
    name: 'Landing',
    component: () => import('@/views/LandingView.vue'),
    meta: { public: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/components/AppLayout.vue'),
    children: [
      {
        path: '',
        redirect: () => {
          // Authenticated staff go straight to their workspace; anonymous
          // visitors land on the public marketing page (the business front door).
          const auth = useAuthStore()
          if (auth.isEmployee) return '/caisse'
          if (auth.isManager || auth.isPartner) return '/dashboard'
          return '/landing'
        }
      },
      {
        path: 'caisse',
        name: 'Caisse',
        component: () => import('@/views/CaisseView.vue'),
        meta: { roles: ['EMPLOYEE', 'MANAGER'] }
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { roles: ['MANAGER', 'PARTNER'] }
      },
      {
        path: 'clients',
        name: 'Clients',
        component: () => import('@/views/ClientsView.vue'),
        meta: { roles: ['MANAGER'] }
      },
      {
        path: 'clients/new',
        name: 'ClientNew',
        component: () => import('@/views/ClientFormView.vue'),
        meta: { roles: ['MANAGER'] }
      },
      {
        path: 'clients/:id',
        name: 'ClientDetail',
        component: () => import('@/views/ClientDetailView.vue'),
        meta: { roles: ['MANAGER'] }
      },
      {
        path: 'clients/:id/edit',
        name: 'ClientEdit',
        component: () => import('@/views/ClientFormView.vue'),
        meta: { roles: ['MANAGER'] }
      },
      {
        path: 'transactions',
        name: 'Transactions',
        component: () => import('@/views/TransactionsView.vue'),
        meta: { roles: ['MANAGER', 'PARTNER'] }
      },
      {
        path: 'inspections',
        name: 'Inspections',
        component: () => import('@/views/InspectionsView.vue'),
        meta: { roles: ['EMPLOYEE', 'MANAGER', 'PARTNER'] }
      },
      {
        path: 'inspections/new',
        name: 'InspectionNew',
        component: () => import('@/views/InspectionCaptureView.vue'),
        meta: { roles: ['EMPLOYEE', 'MANAGER'] }
      },
      {
        path: 'inspections/:id',
        name: 'InspectionDetail',
        component: () => import('@/views/InspectionDetailView.vue'),
        meta: { roles: ['EMPLOYEE', 'MANAGER', 'PARTNER'] }
      },
      {
        path: 'expenses',
        name: 'Expenses',
        component: () => import('@/views/ExpensesView.vue'),
        meta: { roles: ['MANAGER'] }
      },
      {
        path: 'stock',
        name: 'Stock',
        component: () => import('@/views/StockView.vue'),
        meta: { roles: ['MANAGER'] }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/SettingsView.vue'),
        meta: { roles: ['MANAGER'] }
      }
    ]
  },
  // ── Client portal « Rapports de lavage » (accès sans compte staff) ──
  {
    path: '/rapports',
    name: 'PortalLogin',
    component: () => import('@/views/PortalLoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/rapports/espace',
    name: 'PortalHome',
    component: () => import('@/views/PortalHomeView.vue'),
    meta: { portal: true }
  },
  {
    path: '/rapports/:id(\\d+)',
    name: 'PortalReport',
    component: () => import('@/views/PortalReportView.vue'),
    meta: { portal: true }
  },
  // Catch-all
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach((to) => {
  // Client portal — separate auth space (phone + code)
  if (to.meta.portal) {
    const portal = usePortalStore()
    if (!portal.isAuthenticated) return { name: 'PortalLogin' }
    return true
  }

  const auth = useAuthStore()

  if (to.meta.public) return true

  if (!auth.isAuthenticated) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  if (to.meta.roles && !to.meta.roles.includes(auth.role)) {
    // Redirect to appropriate home based on role
    if (auth.isEmployee) return { name: 'Caisse' }
    return { name: 'Dashboard' }
  }

  return true
})

export default router
