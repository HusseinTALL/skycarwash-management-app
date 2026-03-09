<template>
  <div class="p-4 space-y-6 pb-24">
    <h2 class="text-xl font-bold">Réglages</h2>

    <!-- ── Services ────────────────────────────────────────────────── -->
    <div class="card space-y-4">
      <div class="flex items-center justify-between">
        <h3 class="font-semibold text-slate-300">Services</h3>
        <button @click="openServiceForm(null)" class="btn-primary text-sm py-1.5 px-3">
          + Nouveau
        </button>
      </div>

      <p v-if="loadingServices" class="text-slate-500 text-sm">Chargement…</p>
      <p v-else-if="servicesError" class="text-red-400 text-sm">{{ servicesError }}</p>

      <ul v-else class="divide-y divide-slate-700">
        <li
          v-for="svc in services"
          :key="svc.id"
          class="flex items-center justify-between py-3"
        >
          <div>
            <p class="font-medium" :class="svc.active ? 'text-white' : 'text-slate-500 line-through'">
              {{ svc.name }}
            </p>
            <p class="text-xs text-slate-400">{{ svc.category || '—' }} · {{ svc.price.toLocaleString() }} FCFA</p>
          </div>
          <button
            @click="openServiceForm(svc)"
            class="text-xs text-sky-400 hover:text-sky-300 px-2 py-1"
          >
            Modifier
          </button>
        </li>
      </ul>
    </div>

    <!-- ── Utilisateurs ─────────────────────────────────────────────── -->
    <div class="card space-y-4">
      <div class="flex items-center justify-between">
        <h3 class="font-semibold text-slate-300">Utilisateurs</h3>
        <button @click="toggleUserForm" class="btn-primary text-sm py-1.5 px-3">
          {{ showUserForm ? 'Annuler' : '+ Nouvel utilisateur' }}
        </button>
      </div>

      <!-- Formulaire inline de création -->
      <div v-if="showUserForm" class="bg-slate-700/50 rounded-xl p-4 space-y-3">
        <div>
          <label class="block text-xs text-slate-400 mb-1">Nom complet</label>
          <input
            v-model="userForm.name"
            type="text"
            class="input w-full"
            placeholder="Ex : Amadou Diallo"
          />
        </div>
        <div>
          <label class="block text-xs text-slate-400 mb-1">Téléphone</label>
          <input
            v-model="userForm.phone"
            type="tel"
            class="input w-full"
            placeholder="Ex : +22612345678"
          />
        </div>
        <div>
          <label class="block text-xs text-slate-400 mb-1">Rôle</label>
          <select v-model="userForm.role" class="input w-full">
            <option value="EMPLOYEE">Employé</option>
            <option value="PARTNER">Partenaire</option>
          </select>
        </div>
        <p v-if="userFormError" class="text-red-400 text-sm">{{ userFormError }}</p>
        <button
          @click="createUser"
          :disabled="creatingUser"
          class="btn-primary w-full disabled:opacity-50"
        >
          {{ creatingUser ? 'Création…' : 'Créer le compte' }}
        </button>
      </div>

      <!-- Affichage du mot de passe temporaire (une seule fois) -->
      <div
        v-if="revealedPassword"
        class="bg-emerald-900/40 border border-emerald-700 rounded-xl p-4 space-y-2"
      >
        <p class="text-emerald-400 text-sm font-semibold">{{ revealedPasswordLabel }}</p>
        <p class="font-mono text-2xl text-white tracking-widest">{{ revealedPassword }}</p>
        <p class="text-xs text-slate-400">
          Communiquez ce mot de passe verbalement. Il ne sera plus affiché.
        </p>
        <button
          @click="revealedPassword = null"
          class="text-xs text-slate-400 hover:text-slate-300 underline"
        >
          Fermer
        </button>
      </div>

      <!-- Liste des utilisateurs -->
      <p v-if="loadingUsers" class="text-slate-500 text-sm">Chargement…</p>
      <p v-else-if="usersError" class="text-red-400 text-sm">{{ usersError }}</p>
      <ul v-else class="divide-y divide-slate-700">
        <li
          v-for="u in users"
          :key="u.id"
          class="flex items-center justify-between py-3 gap-2"
        >
          <div class="min-w-0">
            <p class="font-medium text-white truncate">{{ u.name }}</p>
            <div class="flex items-center flex-wrap gap-x-2 gap-y-0.5 mt-0.5">
              <span class="text-xs text-slate-400">{{ u.phone }}</span>
              <span class="text-xs text-slate-500">·</span>
              <span class="text-xs text-slate-400">{{ roleLabel(u.role) }}</span>
              <span
                :class="u.active
                  ? 'bg-emerald-500/20 text-emerald-400'
                  : 'bg-red-500/20 text-red-400'"
                class="text-xs px-1.5 py-0.5 rounded-full"
              >
                {{ u.active ? 'Actif' : 'Inactif' }}
              </span>
            </div>
          </div>
          <div v-if="u.role !== 'MANAGER'" class="flex gap-2 shrink-0">
            <button
              @click="toggleStatus(u)"
              class="text-xs px-2 py-1 rounded-lg bg-slate-700 text-slate-300"
              :class="u.active
                ? 'hover:bg-red-900/40 hover:text-red-400'
                : 'hover:bg-emerald-900/40 hover:text-emerald-400'"
            >
              {{ u.active ? 'Désactiver' : 'Réactiver' }}
            </button>
            <button
              @click="resetPassword(u)"
              class="text-xs px-2 py-1 rounded-lg bg-slate-700 text-slate-300 hover:bg-sky-900/40 hover:text-sky-400"
            >
              Reset MDP
            </button>
          </div>
        </li>
      </ul>
    </div>

    <!-- ── À propos ──────────────────────────────────────────────────── -->
    <div class="card text-center py-6 space-y-1">
      <p class="text-slate-300 font-semibold">SkyCarWash Manager</p>
      <p class="text-slate-500 text-sm">Version 1.0.0 — Mars 2026</p>
      <p class="text-slate-600 text-xs mt-2">Sprint S10 · Production ready</p>
    </div>

    <!-- ── Service Form Modal ────────────────────────────────────────── -->
    <Teleport to="body">
      <div
        v-if="showServiceModal"
        class="fixed inset-0 bg-black/70 flex items-end sm:items-center justify-center z-50"
        @click.self="closeServiceModal"
      >
        <div class="bg-slate-800 rounded-t-2xl sm:rounded-2xl w-full max-w-md p-6 space-y-4">
          <h3 class="font-semibold text-lg">
            {{ editingService ? 'Modifier le service' : 'Nouveau service' }}
          </h3>

          <div class="space-y-3">
            <div>
              <label class="block text-xs text-slate-400 mb-1">Nom</label>
              <input
                v-model="form.name"
                type="text"
                class="input w-full"
                placeholder="Ex : Lavage intérieur"
              />
            </div>

            <div>
              <label class="block text-xs text-slate-400 mb-1">Prix (FCFA)</label>
              <input
                v-model.number="form.price"
                type="number"
                min="0"
                class="input w-full"
                placeholder="Ex : 3500"
              />
            </div>

            <div>
              <label class="block text-xs text-slate-400 mb-1">Catégorie (optionnelle)</label>
              <input
                v-model="form.category"
                type="text"
                class="input w-full"
                placeholder="Ex : Extérieur"
              />
            </div>

            <div v-if="editingService" class="flex items-center gap-3">
              <input
                id="activeToggle"
                v-model="form.active"
                type="checkbox"
                class="w-4 h-4 accent-sky-500"
              />
              <label for="activeToggle" class="text-sm text-slate-300">Service actif</label>
            </div>
          </div>

          <p v-if="formError" class="text-red-400 text-sm">{{ formError }}</p>

          <div class="flex gap-3 pt-2">
            <button @click="closeServiceModal" class="btn-secondary flex-1">Annuler</button>
            <button
              @click="saveService"
              :disabled="saving"
              class="btn-primary flex-1 disabled:opacity-50"
            >
              {{ saving ? 'Enregistrement…' : 'Enregistrer' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'

// ── State ──────────────────────────────────────────────────────────── //
const services        = ref([])
const loadingServices = ref(false)

// ── Users state ─────────────────────────────────────────────────────── //
const users        = ref([])
const loadingUsers = ref(false)

const showUserForm  = ref(false)
const creatingUser  = ref(false)
const userFormError = ref('')
const userForm      = ref({ name: '', phone: '', role: 'EMPLOYEE' })

const revealedPassword      = ref(null)
const revealedPasswordLabel = ref('')

const servicesError = ref('')
const usersError    = ref('')

const showServiceModal = ref(false)
const editingService   = ref(null)
const saving           = ref(false)
const formError        = ref('')

const form = ref({ name: '', price: 0, category: '', active: true })

// ── Load services ───────────────────────────────────────────────────── //
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

// ── Load users ──────────────────────────────────────────────────────── //
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

// ── Modal helpers ────────────────────────────────────────────────────── //
function openServiceForm(svc) {
  editingService.value = svc
  formError.value = ''
  if (svc) {
    form.value = { name: svc.name, price: svc.price, category: svc.category || '', active: svc.active }
  } else {
    form.value = { name: '', price: 0, category: '', active: true }
  }
  showServiceModal.value = true
}

function closeServiceModal() {
  showServiceModal.value = false
}

// ── User helpers ─────────────────────────────────────────────────────── //
function roleLabel(role) {
  return role === 'MANAGER' ? 'Manager' : role === 'PARTNER' ? 'Partenaire' : 'Employé'
}

function toggleUserForm() {
  showUserForm.value = !showUserForm.value
  userFormError.value = ''
  userForm.value = { name: '', phone: '', role: 'EMPLOYEE' }
}

async function createUser() {
  userFormError.value = ''
  if (!userForm.value.name.trim())  { userFormError.value = 'Le nom est requis.';       return }
  if (!userForm.value.phone.trim()) { userFormError.value = 'Le téléphone est requis.'; return }

  creatingUser.value = true
  try {
    const { data } = await api.post('/manager/users', {
      name:  userForm.value.name.trim(),
      phone: userForm.value.phone.trim(),
      role:  userForm.value.role
    })
    revealedPassword.value      = data.temporaryPassword
    revealedPasswordLabel.value = `Compte créé — Mot de passe temporaire de ${data.user.name} :`
    showUserForm.value = false
    await loadUsers()
  } catch (err) {
    userFormError.value = err.response?.data?.error || 'Une erreur est survenue.'
  } finally {
    creatingUser.value = false
  }
}

async function toggleStatus(user) {
  try {
    const { data } = await api.patch(`/manager/users/${user.id}/status`, { active: !user.active })
    const idx = users.value.findIndex(u => u.id === user.id)
    if (idx !== -1) users.value[idx] = data
  } catch (err) {
    alert(err.response?.data?.error || 'Erreur lors de la mise à jour du statut.')
  }
}

async function resetPassword(user) {
  try {
    const { data } = await api.post(`/manager/users/${user.id}/reset-password`)
    revealedPassword.value      = data.temporaryPassword
    revealedPasswordLabel.value = `Nouveau mot de passe temporaire de ${user.name} :`
  } catch (err) {
    alert(err.response?.data?.error || 'Erreur lors du reset.')
  }
}

// ── Save (create or update) ──────────────────────────────────────────── //
async function saveService() {
  formError.value = ''
  if (!form.value.name.trim()) { formError.value = 'Le nom est requis.'; return }
  if (!form.value.price || form.value.price <= 0) { formError.value = 'Le prix doit être > 0.'; return }

  saving.value = true
  try {
    const payload = {
      name:     form.value.name.trim(),
      price:    form.value.price,
      category: form.value.category.trim() || null,
      active:   form.value.active
    }

    if (editingService.value) {
      await api.put(`/services/${editingService.value.id}`, payload)
    } else {
      await api.post('/services', payload)
    }

    await loadServices()
    closeServiceModal()
  } catch (err) {
    formError.value = err.response?.data?.message || 'Une erreur est survenue.'
  } finally {
    saving.value = false
  }
}
</script>
