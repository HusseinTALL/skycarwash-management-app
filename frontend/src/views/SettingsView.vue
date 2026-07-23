<template>
  <div class="scw-animate-in">
    <PageHeader title="Réglages" icon="pi pi-cog" subtitle="Services, équipe et paramètres" />

    <Tabs value="services">
      <TabList>
        <Tab value="services"><i class="pi pi-tags mr-2" />Services</Tab>
        <Tab value="users"><i class="pi pi-users mr-2" />Utilisateurs</Tab>
        <Tab value="about"><i class="pi pi-info-circle mr-2" />À propos</Tab>
      </TabList>

      <TabPanels>
        <!-- ═══ Services ═══ -->
        <TabPanel value="services">
          <div class="flex justify-end mb-3">
            <Button label="Nouveau service" icon="pi pi-plus" @click="openServiceForm(null)" />
          </div>
          <Message v-if="servicesError" severity="error" :closable="false" class="mb-3">{{ servicesError }}</Message>
          <DataTable :value="services" :loading="loadingServices" data-key="id" row-hover class="scw-panel overflow-hidden" :pt="{ table: { style: 'min-width: 32rem' } }">
            <template #empty><EmptyState icon="pi pi-tags" text="Aucun service" /></template>
            <Column header="Service" sortable field="name">
              <template #body="{ data }">
                <span :class="data.active ? 'font-medium text-slate-100' : 'text-slate-500 line-through'">{{ data.name }}</span>
              </template>
            </Column>
            <Column header="Catégorie" field="category">
              <template #body="{ data }"><span class="text-slate-400">{{ data.category || '—' }}</span></template>
            </Column>
            <Column header="Prix" sortable field="price">
              <template #body="{ data }"><span class="font-semibold text-primary">{{ data.price.toLocaleString('fr-FR') }} F</span></template>
            </Column>
            <Column header="Statut">
              <template #body="{ data }"><Tag :severity="data.active ? 'success' : 'secondary'" :value="data.active ? 'Actif' : 'Inactif'" rounded /></template>
            </Column>
            <Column style="width: 4rem">
              <template #body="{ data }"><Button icon="pi pi-pencil" text rounded size="small" @click="openServiceForm(data)" /></template>
            </Column>
          </DataTable>
        </TabPanel>

        <!-- ═══ Utilisateurs ═══ -->
        <TabPanel value="users">
          <div class="flex flex-wrap items-center justify-between gap-3 mb-3">
            <IconField class="flex-1 min-w-[12rem] max-w-xs">
              <InputIcon class="pi pi-search" />
              <InputText v-model="userSearch" placeholder="Rechercher…" class="w-full" />
            </IconField>
            <Button label="Nouvel utilisateur" icon="pi pi-user-plus" @click="openUserForm" />
          </div>

          <Message v-if="revealedPassword" severity="success" :closable="true" class="mb-3" @close="revealedPassword = null">
            <div>
              <p class="font-semibold">{{ revealedPasswordLabel }}</p>
              <p class="font-mono text-2xl tracking-widest my-1">{{ revealedPassword }}</p>
              <p class="text-xs opacity-80">Communiquez ce mot de passe verbalement. Il ne sera plus affiché.</p>
            </div>
          </Message>
          <Message v-if="usersError" severity="error" :closable="false" class="mb-3">{{ usersError }}</Message>
          <Message v-if="actionError" severity="error" :closable="false" class="mb-3">{{ actionError }}</Message>

          <DataTable
            :value="filteredUsers"
            :loading="loadingUsers"
            data-key="id"
            paginator
            :rows="10"
            row-hover
            class="scw-panel overflow-hidden"
            :pt="{ table: { style: 'min-width: 34rem' } }"
          >
            <template #empty><EmptyState icon="pi pi-users" text="Aucun utilisateur" /></template>
            <Column header="Utilisateur" sortable field="name">
              <template #body="{ data }">
                <div class="flex items-center gap-2">
                  <Avatar :label="initials(data.name)" shape="circle" class="!bg-primary/15 !text-primary !text-xs !font-semibold" />
                  <div>
                    <p class="font-medium text-slate-100">{{ data.name }}</p>
                    <p class="text-xs text-slate-400">{{ data.phone }}</p>
                  </div>
                </div>
              </template>
            </Column>
            <Column header="Rôle">
              <template #body="{ data }"><Tag :severity="data.role === 'MANAGER' ? 'warn' : data.role === 'PARTNER' ? 'help' : 'info'" :value="roleLabel(data.role)" rounded /></template>
            </Column>
            <Column header="Statut">
              <template #body="{ data }"><Tag :severity="data.active ? 'success' : 'danger'" :value="data.active ? 'Actif' : 'Inactif'" rounded /></template>
            </Column>
            <Column style="width: 8rem">
              <template #body="{ data }">
                <div v-if="data.role !== 'MANAGER'" class="flex justify-end gap-1">
                  <Button :icon="data.active ? 'pi pi-ban' : 'pi pi-check-circle'" :severity="data.active ? 'danger' : 'success'" text rounded size="small" v-tooltip.top="data.active ? 'Désactiver' : 'Réactiver'" @click="toggleStatus(data)" />
                  <Button icon="pi pi-key" severity="secondary" text rounded size="small" v-tooltip.top="'Reset mot de passe'" @click="resetPassword(data)" />
                </div>
              </template>
            </Column>
          </DataTable>
        </TabPanel>

        <!-- ═══ À propos ═══ -->
        <TabPanel value="about">
          <div class="scw-panel p-8 text-center space-y-2 max-w-md mx-auto">
            <span class="inline-grid place-items-center w-14 h-14 rounded-2xl bg-primary text-slate-950 mb-2">
              <i class="pi pi-car text-2xl" />
            </span>
            <p class="text-slate-100 font-semibold text-lg">SkyCarWash Manager</p>
            <p class="text-slate-500 text-sm">Version 1.0.0 — Mars 2026</p>
            <p class="text-slate-600 text-xs">Production ready</p>
          </div>
        </TabPanel>
      </TabPanels>
    </Tabs>

    <!-- ═══ Service form dialog ═══ -->
    <Dialog v-model:visible="showServiceModal" modal :header="editingService ? 'Modifier le service' : 'Nouveau service'" class="w-full max-w-md mx-3">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Nom <span class="text-red-400">*</span></label>
          <InputText v-model="form.name" class="w-full" placeholder="Ex : Lavage intérieur" />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Prix (FCFA) <span class="text-red-400">*</span></label>
          <InputNumber v-model="form.price" :min="0" class="w-full" placeholder="Ex : 3500" />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Catégorie (optionnelle)</label>
          <InputText v-model="form.category" class="w-full" placeholder="Ex : Extérieur" />
        </div>
        <div v-if="editingService" class="flex items-center gap-2">
          <ToggleSwitch v-model="form.active" input-id="activeToggle" />
          <label for="activeToggle" class="text-sm text-slate-300">Service actif</label>
        </div>
        <Message v-if="formError" severity="error" :closable="false" size="small">{{ formError }}</Message>
      </div>
      <template #footer>
        <Button label="Annuler" severity="secondary" text @click="closeServiceModal" />
        <Button label="Enregistrer" icon="pi pi-check" :loading="saving" @click="saveService" />
      </template>
    </Dialog>

    <!-- ═══ User form dialog ═══ -->
    <Dialog v-model:visible="showUserForm" modal header="Nouvel utilisateur" class="w-full max-w-md mx-3">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Nom complet</label>
          <InputText v-model="userForm.name" class="w-full" placeholder="Ex : Amadou Diallo" />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Téléphone</label>
          <InputText v-model="userForm.phone" type="tel" class="w-full" placeholder="Ex : +22612345678" />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Rôle</label>
          <Select v-model="userForm.role" :options="ROLE_OPTS" option-label="label" option-value="value" class="w-full" />
        </div>
        <Message v-if="userFormError" severity="error" :closable="false" size="small">{{ userFormError }}</Message>
      </div>
      <template #footer>
        <Button label="Annuler" severity="secondary" text @click="showUserForm = false" />
        <Button label="Créer le compte" icon="pi pi-check" :loading="creatingUser" @click="createUser" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/api/axios'
import { PHONE_REGEX } from '@/constants'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Avatar from 'primevue/avatar'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import Select from 'primevue/select'
import Message from 'primevue/message'
import ToggleSwitch from 'primevue/toggleswitch'

const confirm = useConfirm()
const toast = useToast()

const ROLE_OPTS = [
  { value: 'EMPLOYEE', label: 'Employé' },
  { value: 'PARTNER', label: 'Partenaire' }
]

const services = ref([])
const loadingServices = ref(false)
const servicesError = ref('')

const users = ref([])
const loadingUsers = ref(false)
const usersError = ref('')

const showUserForm = ref(false)
const creatingUser = ref(false)
const userFormError = ref('')
const userForm = ref({ name: '', phone: '', role: 'EMPLOYEE' })
const userSearch = ref('')

const filteredUsers = computed(() => {
  const q = userSearch.value.trim().toLowerCase()
  return q ? users.value.filter((u) => u.name.toLowerCase().includes(q) || u.phone.toLowerCase().includes(q)) : users.value
})

const revealedPassword = ref(null)
const revealedPasswordLabel = ref('')
const actionError = ref('')

const showServiceModal = ref(false)
const editingService = ref(null)
const saving = ref(false)
const formError = ref('')
const form = ref({ name: '', price: 0, category: '', active: true })

async function loadServices() {
  loadingServices.value = true
  servicesError.value = ''
  try {
    const { data } = await api.get('/services')
    services.value = data
  } catch (err) {
    servicesError.value = err.response?.data?.error || 'Impossible de charger les services.'
  } finally {
    loadingServices.value = false
  }
}
async function loadUsers() {
  loadingUsers.value = true
  usersError.value = ''
  try {
    const { data } = await api.get('/manager/users')
    users.value = data
  } catch (err) {
    usersError.value = err.response?.data?.error || 'Impossible de charger les utilisateurs.'
  } finally {
    loadingUsers.value = false
  }
}
onMounted(() => { loadServices(); loadUsers() })

function openServiceForm(svc) {
  editingService.value = svc
  formError.value = ''
  form.value = svc
    ? { name: svc.name, price: svc.price, category: svc.category || '', active: svc.active }
    : { name: '', price: 0, category: '', active: true }
  showServiceModal.value = true
}
function closeServiceModal() {
  showServiceModal.value = false
}

function roleLabel(role) {
  return role === 'MANAGER' ? 'Manager' : role === 'PARTNER' ? 'Partenaire' : 'Employé'
}
function initials(name) {
  return (name || '?').split(/\s+/).slice(0, 2).map((w) => w[0]?.toUpperCase()).join('')
}
function openUserForm() {
  userFormError.value = ''
  userForm.value = { name: '', phone: '', role: 'EMPLOYEE' }
  showUserForm.value = true
}

async function createUser() {
  if (creatingUser.value) return
  userFormError.value = ''
  if (!userForm.value.name.trim()) { userFormError.value = 'Le nom est requis.'; return }
  if (!PHONE_REGEX.test(userForm.value.phone.trim())) { userFormError.value = 'Numéro invalide (ex : +22612345678)'; return }
  creatingUser.value = true
  try {
    const { data } = await api.post('/manager/users', {
      name: userForm.value.name.trim(),
      phone: userForm.value.phone.trim(),
      role: userForm.value.role
    })
    revealedPassword.value = data.temporaryPassword
    revealedPasswordLabel.value = `Compte créé — Mot de passe temporaire de ${data.user.name} :`
    showUserForm.value = false
    await loadUsers()
  } catch (err) {
    userFormError.value = err.response?.data?.error || 'Une erreur est survenue.'
  } finally {
    creatingUser.value = false
  }
}

function toggleStatus(user) {
  confirm.require({
    header: user.active ? `Désactiver ${user.name} ?` : `Réactiver ${user.name} ?`,
    message: user.active ? 'Ce compte ne pourra plus se connecter.' : 'Ce compte pourra de nouveau se connecter.',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: user.active ? 'Désactiver' : 'Réactiver',
    rejectLabel: 'Annuler',
    acceptProps: { severity: user.active ? 'danger' : 'success' },
    accept: async () => {
      actionError.value = ''
      try {
        const { data } = await api.patch(`/manager/users/${user.id}/status`, { active: !user.active })
        const idx = users.value.findIndex((u) => u.id === user.id)
        if (idx !== -1) users.value[idx] = data
        toast.add({ severity: 'success', summary: 'Statut mis à jour', detail: user.name, life: 3000 })
      } catch (err) {
        actionError.value = err.response?.data?.error || 'Erreur lors de la mise à jour du statut.'
      }
    }
  })
}

async function resetPassword(user) {
  actionError.value = ''
  try {
    const { data } = await api.post(`/manager/users/${user.id}/reset-password`)
    revealedPassword.value = data.temporaryPassword
    revealedPasswordLabel.value = `Nouveau mot de passe temporaire de ${user.name} :`
  } catch (err) {
    actionError.value = err.response?.data?.error || 'Erreur lors du reset.'
  }
}

async function saveService() {
  if (saving.value) return
  formError.value = ''
  if (!form.value.name.trim()) { formError.value = 'Le nom est requis.'; return }
  if (!form.value.price || form.value.price <= 0) { formError.value = 'Le prix doit être > 0.'; return }
  saving.value = true
  try {
    const payload = {
      name: form.value.name.trim(),
      price: form.value.price,
      category: form.value.category.trim() || null,
      active: form.value.active
    }
    if (editingService.value) {
      await api.put(`/services/${editingService.value.id}`, payload)
    } else {
      await api.post('/services', payload)
    }
    toast.add({ severity: 'success', summary: 'Service enregistré', detail: payload.name, life: 3000 })
    await loadServices()
    closeServiceModal()
  } catch (err) {
    formError.value = err.response?.data?.message || 'Une erreur est survenue.'
  } finally {
    saving.value = false
  }
}
</script>
