<template>
  <div class="scw-animate-in max-w-5xl mx-auto">
    <!-- Header -->
    <div class="flex items-center justify-between mb-5">
      <div class="flex items-center gap-3">
        <Button icon="pi pi-arrow-left" severity="secondary" text rounded aria-label="Retour" @click="router.push('/clients')" />
        <h1 class="text-xl sm:text-2xl font-bold tracking-tight text-slate-100">Détail client</h1>
      </div>
      <Button label="Modifier" icon="pi pi-pencil" severity="secondary" outlined @click="router.push(`/clients/${route.params.id}/edit`)" />
    </div>

    <div v-if="store.loading && !store.current" class="grid gap-4 lg:grid-cols-3">
      <Skeleton height="12rem" border-radius="1rem" class="lg:col-span-1" />
      <Skeleton height="12rem" border-radius="1rem" class="lg:col-span-2" />
    </div>

    <template v-else-if="store.current">
      <div class="grid gap-4 lg:grid-cols-3 items-start">
        <!-- Identity card -->
        <div class="scw-panel p-5 lg:col-span-1 space-y-4">
          <div class="flex items-center gap-3">
            <Avatar :label="initials(store.current.name)" size="large" shape="circle" class="!bg-primary/15 !text-primary !font-semibold" />
            <div class="min-w-0">
              <h3 class="text-lg font-bold text-slate-100 truncate">{{ store.current.name }}</h3>
              <p class="text-slate-400 text-sm">{{ store.current.phone }}</p>
            </div>
          </div>
          <Tag :severity="typeSeverity(store.current.type)" :value="CLIENT_TYPE_LABELS[store.current.type] ?? store.current.type" rounded />

          <hr class="border-slate-800" />

          <dl class="space-y-2.5 text-sm">
            <div class="flex justify-between items-center">
              <dt class="text-slate-400">Passages restants</dt>
              <dd class="font-bold text-base" :class="store.current.balance <= 1 ? 'text-red-400' : 'text-emerald-400'">{{ store.current.balance }}</dd>
            </div>
            <div v-if="store.current.expiresAt" class="flex justify-between items-center gap-2">
              <dt class="text-slate-400">Expiration</dt>
              <dd class="flex items-center gap-1.5">
                <span :class="expiryClass(store.current.expiresAt)">{{ formatDate(store.current.expiresAt) }}</span>
                <Tag v-if="expiryStatus(store.current.expiresAt)" :severity="expiryStatus(store.current.expiresAt) === 'Expiré' ? 'danger' : 'warn'" :value="expiryStatus(store.current.expiresAt)" rounded />
              </dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-slate-400">Client depuis</dt>
              <dd class="text-slate-200">{{ formatDate(store.current.createdAt) }}</dd>
            </div>
          </dl>

          <div v-if="store.current.tags?.length || store.current.notes" class="pt-1 space-y-2">
            <div v-if="store.current.tags?.length" class="flex flex-wrap gap-1.5">
              <Chip v-for="t in store.current.tags" :key="t" :label="`#${t}`" class="!text-xs" />
            </div>
            <p v-if="store.current.notes" class="text-sm text-slate-300 whitespace-pre-line">{{ store.current.notes }}</p>
          </div>

          <Message v-if="needsAlert(store.current)" severity="warn" :closable="false" icon="pi pi-exclamation-triangle" size="small">
            {{ alertMessage(store.current) }}
          </Message>
        </div>

        <!-- Right column -->
        <div class="lg:col-span-2 space-y-4">
          <!-- Stats -->
          <div v-if="s" class="grid grid-cols-2 xl:grid-cols-4 gap-3">
            <KpiCard title="Total dépensé" :value="fmtFcfa(s.totalSpent)" icon="pi pi-money-bill" accent="#34d399" />
            <KpiCard title="Visites" :value="s.visitCount" icon="pi pi-check-circle" accent="#38bdf8" />
            <KpiCard title="Points fidélité" :value="s.loyaltyPoints" icon="pi pi-star" accent="#fbbf24" />
            <KpiCard title="Dernière visite" :value="s.lastVisitAt ? formatDate(s.lastVisitAt) : '—'" icon="pi pi-calendar" accent="#c084fc" />
          </div>

          <!-- Vehicles -->
          <div class="scw-panel p-5">
            <div class="flex items-center justify-between mb-3">
              <h4 class="font-semibold text-slate-200">Véhicules</h4>
              <Button label="Ajouter" icon="pi pi-plus" size="small" text @click="openVehicleForm()" />
            </div>
            <EmptyState v-if="!s?.vehicles?.length" icon="pi pi-car" text="Aucun véhicule enregistré" />
            <ul v-else class="space-y-2">
              <li v-for="v in s.vehicles" :key="v.id" class="flex items-center justify-between bg-slate-900/50 rounded-xl px-3 py-2.5">
                <div class="flex items-center gap-2.5 min-w-0">
                  <span class="text-lg">{{ VEHICLE_TYPE_ICONS[v.type] }}</span>
                  <div class="min-w-0">
                    <p class="text-sm font-medium text-slate-100 truncate">{{ v.label || VEHICLE_TYPE_LABELS[v.type] }}</p>
                    <p v-if="v.plate" class="text-xs text-slate-400">{{ v.plate }}</p>
                  </div>
                </div>
                <div class="flex gap-1 shrink-0">
                  <Button icon="pi pi-pencil" text rounded size="small" @click="openVehicleForm(v)" />
                  <Button icon="pi pi-trash" severity="danger" text rounded size="small" @click="removeVehicle(v)" />
                </div>
              </li>
            </ul>
          </div>

          <!-- Wash history timeline -->
          <div class="scw-panel p-5">
            <h4 class="font-semibold text-slate-200 mb-3">Historique des lavages</h4>
            <EmptyState v-if="!s?.history?.length" icon="pi pi-inbox" text="Aucun lavage enregistré" />
            <Timeline v-else :value="s.history" class="scw-timeline">
              <template #marker="{ item }">
                <span
                  class="grid place-items-center h-7 w-7 rounded-full text-xs"
                  :class="item.cancelled ? 'bg-red-500/15 text-red-400' : 'bg-emerald-500/15 text-emerald-400'"
                >
                  <i :class="item.cancelled ? 'pi pi-times' : 'pi pi-check'" />
                </span>
              </template>
              <template #content="{ item }">
                <div class="flex items-center justify-between gap-3 pb-4">
                  <div class="min-w-0">
                    <p class="text-sm" :class="{ 'line-through text-slate-500': item.cancelled }">{{ item.serviceName }}</p>
                    <p class="text-xs text-slate-500">{{ formatDateTime(item.createdAt) }} · {{ paymentLabel(item.paymentMethod) }}</p>
                  </div>
                  <span class="text-sm font-semibold shrink-0" :class="item.cancelled ? 'text-slate-500' : 'text-emerald-400'">{{ fmtFcfa(item.amount) }}</span>
                </div>
              </template>
            </Timeline>
          </div>

          <!-- Recharge (CARTE / VIP) -->
          <div v-if="store.current.type !== 'BOUCLIER'" class="scw-panel p-5 space-y-3">
            <h4 class="font-semibold text-slate-200">Recharger les passages</h4>
            <div class="flex gap-2">
              <InputNumber v-model="passagesToAdd" :min="1" :max="100" class="flex-1" placeholder="Nombre de passages" />
              <Button label="Ajouter" icon="pi pi-plus" :loading="recharging" :disabled="!passagesToAdd || passagesToAdd < 1" @click="recharge" />
            </div>
            <Message v-if="rechargeError" severity="error" :closable="false" size="small">{{ rechargeError }}</Message>
          </div>

          <!-- Actions -->
          <div class="flex flex-col sm:flex-row gap-2">
            <Button label="Carte QR client" icon="pi pi-qrcode" severity="secondary" outlined class="flex-1" @click="openQrCard" />
            <Button v-if="store.current.active" label="Désactiver ce client" icon="pi pi-ban" severity="danger" outlined class="flex-1" @click="confirmDeactivate" />
          </div>
        </div>
      </div>
    </template>

    <EmptyState v-else icon="pi pi-user" text="Client introuvable" />

    <!-- Vehicle form dialog -->
    <Dialog v-model:visible="showVehicleDialog" modal :header="vehicleForm?.id ? 'Modifier le véhicule' : 'Ajouter un véhicule'" class="w-full max-w-md mx-3">
      <div v-if="vehicleForm" class="space-y-4">
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-sm font-medium text-slate-300 mb-1.5">Type</label>
            <Select v-model="vehicleForm.type" :options="vehicleTypeOptions" option-label="label" option-value="value" class="w-full" />
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-300 mb-1.5">Plaque</label>
            <InputText v-model="vehicleForm.plate" class="w-full" placeholder="Optionnel" />
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Marque / modèle</label>
          <InputText v-model="vehicleForm.label" class="w-full" placeholder="Optionnel" />
        </div>
        <Message v-if="vehicleError" severity="error" :closable="false" size="small">{{ vehicleError }}</Message>
      </div>
      <template #footer>
        <Button label="Annuler" severity="secondary" text @click="showVehicleDialog = false" />
        <Button :label="vehicleForm?.id ? 'Enregistrer' : 'Ajouter'" icon="pi pi-check" :loading="vehicleSaving" @click="saveVehicle" />
      </template>
    </Dialog>

    <!-- QR card dialog -->
    <Dialog v-model:visible="showQrModal" modal header="Carte client" class="w-full max-w-sm mx-3" :pt="{ root: { id: 'qr-card-root' } }">
      <div v-if="store.current" id="qr-card-content" class="flex flex-col items-center gap-4 py-2">
        <p class="text-xl font-bold text-center">{{ store.current.name }}</p>
        <p class="text-slate-400 text-sm">{{ store.current.phone }}</p>
        <Tag :severity="typeSeverity(store.current.type)" :value="CLIENT_TYPE_LABELS[store.current.type] ?? store.current.type" rounded />
        <img v-if="qrDataUrl" :src="qrDataUrl" alt="QR Code" class="w-48 h-48 bg-white p-2 rounded-xl" />
        <Skeleton v-else width="12rem" height="12rem" />
        <p class="text-xs text-slate-500 text-center">Présentez cette carte à la caisse</p>
      </div>
      <template #footer>
        <Button label="Fermer" severity="secondary" text @click="showQrModal = false" />
        <Button label="Imprimer" icon="pi pi-print" @click="printCard" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useClientsStore } from '@/stores/clients'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'
import QRCode from 'qrcode'
import { PAYMENT_LABELS, VEHICLE_TYPE_LABELS, VEHICLE_TYPE_ICONS } from '@/constants'
import KpiCard from '@/components/ui/KpiCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Chip from 'primevue/chip'
import Avatar from 'primevue/avatar'
import Message from 'primevue/message'
import Skeleton from 'primevue/skeleton'
import Timeline from 'primevue/timeline'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'

const CLIENT_TYPE_LABELS = { CARTE: 'Carte passages', BOUCLIER: 'Bouclier', VIP: 'VIP' }
const typeSeverity = (t) => ({ CARTE: 'info', BOUCLIER: 'help', VIP: 'warn' }[t] ?? 'secondary')
const vehicleTypeOptions = Object.entries(VEHICLE_TYPE_LABELS).map(([value, label]) => ({ value, label }))

const route = useRoute()
const router = useRouter()
const store = useClientsStore()
const confirm = useConfirm()
const toast = useToast()

const s = computed(() => store.summary)

const passagesToAdd = ref(null)
const recharging = ref(false)
const rechargeError = ref('')
const showQrModal = ref(false)
const qrDataUrl = ref(null)

const vehicleForm = ref(null)
const showVehicleDialog = ref(false)
const vehicleSaving = ref(false)
const vehicleError = ref('')

function initials(name) {
  return (name || '?').split(/\s+/).slice(0, 2).map((w) => w[0]?.toUpperCase()).join('')
}
function openVehicleForm(v = null) {
  vehicleError.value = ''
  vehicleForm.value = v
    ? { id: v.id, type: v.type, plate: v.plate ?? '', label: v.label ?? '' }
    : { id: null, type: 'VOITURE', plate: '', label: '' }
  showVehicleDialog.value = true
}
async function saveVehicle() {
  if (vehicleSaving.value) return
  vehicleError.value = ''
  vehicleSaving.value = true
  const payload = {
    type: vehicleForm.value.type,
    plate: vehicleForm.value.plate.trim() || null,
    label: vehicleForm.value.label.trim() || null
  }
  try {
    if (vehicleForm.value.id) {
      await store.updateVehicle(route.params.id, vehicleForm.value.id, payload)
    } else {
      await store.addVehicle(route.params.id, payload)
    }
    showVehicleDialog.value = false
    toast.add({ severity: 'success', summary: 'Véhicule enregistré', life: 3000 })
  } catch (err) {
    vehicleError.value = err.response?.data?.error ?? "Erreur lors de l'enregistrement du véhicule"
  } finally {
    vehicleSaving.value = false
  }
}
function removeVehicle(v) {
  confirm.require({
    header: 'Supprimer ce véhicule ?',
    message: `${v.label || VEHICLE_TYPE_LABELS[v.type]}${v.plate ? ' · ' + v.plate : ''}`,
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Supprimer',
    rejectLabel: 'Annuler',
    acceptProps: { severity: 'danger' },
    accept: async () => {
      try {
        await store.deleteVehicle(route.params.id, v.id)
        toast.add({ severity: 'info', summary: 'Véhicule supprimé', life: 3000 })
      } catch (err) {
        toast.add({ severity: 'error', summary: 'Erreur', detail: err.response?.data?.error ?? 'Suppression impossible', life: 4000 })
      }
    }
  })
}

const paymentLabel = (m) => PAYMENT_LABELS[m] ?? m
function fmtFcfa(n) {
  return new Intl.NumberFormat('fr-FR').format(n ?? 0) + ' F'
}
function formatDateTime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  store.loadById(route.params.id)
  store.loadSummary(route.params.id)
})

function printCard() {
  window.print()
}
async function openQrCard() {
  showQrModal.value = true
  qrDataUrl.value = null
  try {
    qrDataUrl.value = await QRCode.toDataURL(store.current.phone, { width: 300, margin: 2, color: { dark: '#000000', light: '#ffffff' } })
  } catch (err) {
    console.error('QR generation failed', err)
  }
}

async function recharge() {
  if (recharging.value) return
  rechargeError.value = ''
  recharging.value = true
  try {
    const updated = await store.addPassages(route.params.id, passagesToAdd.value)
    toast.add({ severity: 'success', summary: 'Passages ajoutés', detail: `Nouveau solde : ${updated.balance}`, life: 3000 })
    passagesToAdd.value = null
  } catch (err) {
    rechargeError.value = err.response?.data?.error ?? 'Erreur lors de la recharge'
  } finally {
    recharging.value = false
  }
}

function confirmDeactivate() {
  confirm.require({
    header: 'Désactiver ce client ?',
    message: `${store.current?.name} ne pourra plus être utilisé pour des transactions.`,
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Désactiver',
    rejectLabel: 'Annuler',
    acceptProps: { severity: 'danger' },
    accept: async () => {
      await store.deactivate(route.params.id)
      toast.add({ severity: 'info', summary: 'Client désactivé', life: 3000 })
      router.push('/clients')
    }
  })
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}
function expiryClass(iso) {
  const days = Math.ceil((new Date(iso) - Date.now()) / 86_400_000)
  if (days <= 0) return 'text-red-400 font-semibold'
  if (days <= 5) return 'text-amber-400 font-semibold'
  return 'text-slate-200'
}
function expiryStatus(iso) {
  const days = Math.ceil((new Date(iso) - Date.now()) / 86_400_000)
  if (days <= 0) return 'Expiré'
  if (days <= 5) return 'Bientôt'
  return null
}
function needsAlert(client) {
  if (client.type === 'CARTE' && client.balance <= 1) return true
  if (client.expiresAt) {
    const days = Math.ceil((new Date(client.expiresAt) - Date.now()) / 86_400_000)
    if (days <= 5) return true
  }
  return false
}
function alertMessage(client) {
  const msgs = []
  if (client.type === 'CARTE' && client.balance <= 1) {
    msgs.push(client.balance === 0 ? 'Plus de passages disponibles' : 'Dernier passage')
  }
  if (client.expiresAt) {
    const days = Math.ceil((new Date(client.expiresAt) - Date.now()) / 86_400_000)
    if (days <= 0) msgs.push('Abonnement expiré')
    else if (days <= 5) msgs.push(`Abonnement expire dans ${days} jour${days > 1 ? 's' : ''}`)
  }
  return msgs.join(' · ')
}
</script>

<style scoped>
:deep(.scw-timeline .p-timeline-event-opposite) {
  display: none;
}
:deep(.scw-timeline .p-timeline-event) {
  min-height: 0;
}
</style>
