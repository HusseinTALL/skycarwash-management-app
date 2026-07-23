<template>
  <div class="min-h-screen bg-transparent">

    <!-- ── Desktop sidebar ─────────────────────────────────────────────── -->
    <aside
      class="hidden md:flex md:flex-col md:w-64 md:fixed md:inset-y-0 md:z-30
             border-r border-slate-800/80 bg-slate-950/70 backdrop-blur-xl"
    >
      <div class="flex items-center gap-2.5 px-5 h-16 border-b border-slate-800/80">
        <span class="grid place-items-center h-9 w-9 rounded-xl bg-primary text-slate-950 shadow-lg shadow-primary/20">
          <i class="pi pi-sun text-lg" />
        </span>
        <div class="leading-tight">
          <p class="font-bold tracking-tight text-slate-100">SkyCarWash</p>
          <p class="text-[11px] text-slate-500">Management Suite</p>
        </div>
      </div>

      <nav class="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        <RouterLink
          v-for="item in items"
          :key="item.to"
          :to="item.to"
          v-ripple
          class="nav-link group"
          active-class="nav-link-active"
        >
          <i :class="item.icon" class="text-[17px] shrink-0" />
          <span class="flex-1">{{ item.label }}</span>
          <Badge
            v-if="item.badgeValue"
            :value="item.badgeValue"
            severity="danger"
            class="!text-[10px]"
          />
        </RouterLink>
      </nav>

      <div class="px-3 py-3 border-t border-slate-800/80">
        <div class="flex items-center gap-3 px-2 py-2 rounded-xl">
          <Avatar
            :label="userInitials"
            shape="circle"
            class="!bg-primary/15 !text-primary !font-semibold shrink-0"
          />
          <div class="min-w-0 flex-1 leading-tight">
            <p class="text-sm font-medium text-slate-200 truncate">{{ auth.user?.name }}</p>
            <p class="text-[11px] text-slate-500">{{ roleLabel }}</p>
          </div>
          <Button
            icon="pi pi-sign-out"
            severity="danger"
            text
            rounded
            aria-label="Déconnexion"
            v-tooltip.left="'Déconnexion'"
            @click="handleLogout"
          />
        </div>
      </div>
    </aside>

    <!-- ── Mobile top bar ──────────────────────────────────────────────── -->
    <header
      class="md:hidden sticky top-0 z-30 h-14 flex items-center justify-between px-3
             border-b border-slate-800/80 bg-slate-950/80 backdrop-blur-xl"
    >
      <div class="flex items-center gap-2">
        <Button icon="pi pi-bars" text rounded aria-label="Menu" @click="drawerOpen = true" />
        <span class="flex items-center gap-2 font-bold tracking-tight text-slate-100">
          <i class="pi pi-sun text-primary" /> SkyCarWash
        </span>
      </div>
      <Avatar :label="userInitials" shape="circle" class="!bg-primary/15 !text-primary !text-xs !font-semibold" />
    </header>

    <!-- ── Mobile navigation drawer ────────────────────────────────────── -->
    <Drawer v-model:visible="drawerOpen" class="!w-72">
      <template #header>
        <div class="flex items-center gap-2.5">
          <span class="grid place-items-center h-9 w-9 rounded-xl bg-primary text-slate-950">
            <i class="pi pi-sun text-lg" />
          </span>
          <div class="leading-tight">
            <p class="font-bold tracking-tight text-slate-100">SkyCarWash</p>
            <p class="text-[11px] text-slate-500">{{ auth.user?.name }} · {{ roleLabel }}</p>
          </div>
        </div>
      </template>

      <nav class="space-y-1">
        <RouterLink
          v-for="item in items"
          :key="item.to"
          :to="item.to"
          v-ripple
          class="nav-link"
          active-class="nav-link-active"
          @click="drawerOpen = false"
        >
          <i :class="item.icon" class="text-[17px] shrink-0" />
          <span class="flex-1">{{ item.label }}</span>
          <Badge v-if="item.badgeValue" :value="item.badgeValue" severity="danger" />
        </RouterLink>
      </nav>

      <template #footer>
        <Button
          label="Déconnexion"
          icon="pi pi-sign-out"
          severity="danger"
          text
          class="w-full !justify-start"
          @click="handleLogout"
        />
      </template>
    </Drawer>

    <!-- ── Main content ────────────────────────────────────────────────── -->
    <main class="md:pl-64 min-h-screen">
      <div class="mx-auto w-full max-w-7xl px-4 py-5 sm:px-6 lg:px-8 pb-24 md:pb-10">
        <RouterView v-slot="{ Component }">
          <Transition name="page" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </div>
    </main>

    <!-- ── Mobile bottom nav (quick access) ────────────────────────────── -->
    <nav
      class="md:hidden fixed bottom-0 inset-x-0 z-20 h-16 flex items-stretch
             border-t border-slate-800/80 bg-slate-950/90 backdrop-blur-xl no-scrollbar overflow-x-auto"
    >
      <RouterLink
        v-for="item in items"
        :key="item.to"
        :to="item.to"
        class="bottom-nav-item"
        active-class="bottom-nav-item-active"
      >
        <span class="relative">
          <i :class="item.icon" class="text-[19px]" />
          <Badge
            v-if="item.badgeValue"
            :value="item.badgeValue"
            severity="danger"
            class="!absolute !-top-2 !-right-3 !text-[9px] !min-w-4 !h-4"
          />
        </span>
        <span class="text-[10px] mt-0.5 whitespace-nowrap">{{ item.label }}</span>
      </RouterLink>
    </nav>

    <!-- Global services -->
    <Toast position="top-right" />
    <ConfirmDialog />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useStockStore } from '@/stores/stock'
import { useNavigation } from '@/composables/useNavigation'
import { ROLE_LABELS } from '@/constants'

import Avatar from 'primevue/avatar'
import Badge from 'primevue/badge'
import Button from 'primevue/button'
import Drawer from 'primevue/drawer'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'

const auth = useAuthStore()
const stock = useStockStore()
const router = useRouter()
const { items } = useNavigation()

const drawerOpen = ref(false)

const roleLabel = computed(() => ROLE_LABELS[auth.role] ?? auth.role ?? '')
const userInitials = computed(() => {
  const name = auth.user?.name?.trim() || '?'
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase())
    .join('')
})

// Load stock so the low-stock badge is accurate for managers.
if (auth.isManager && stock.products.length === 0) {
  stock.loadAll()
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.nav-link {
  @apply flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium
         text-slate-400 transition-colors duration-150;
}
.nav-link:hover {
  @apply text-slate-100 bg-slate-800/60;
}
.nav-link-active {
  @apply text-primary bg-primary/10;
}
.nav-link-active:hover {
  @apply text-primary bg-primary/15;
}

.bottom-nav-item {
  @apply flex flex-col items-center justify-center gap-0 min-w-[4.25rem] flex-1
         text-slate-500 transition-colors duration-150;
}
.bottom-nav-item-active {
  @apply text-primary;
}
</style>
