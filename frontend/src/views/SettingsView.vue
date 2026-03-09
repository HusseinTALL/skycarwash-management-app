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
    <div class="card space-y-3">
      <h3 class="font-semibold text-slate-300">Utilisateurs</h3>
      <p class="text-sm text-slate-400">
        La gestion des comptes utilisateurs (ajout, désactivation, réinitialisation de mot de passe)
        sera disponible dans la v2. Contactez votre administrateur pour toute modification.
      </p>
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

const showServiceModal = ref(false)
const editingService   = ref(null)
const saving           = ref(false)
const formError        = ref('')

const form = ref({ name: '', price: 0, category: '', active: true })

// ── Load services ───────────────────────────────────────────────────── //
async function loadServices() {
  loadingServices.value = true
  try {
    const { data } = await api.get('/services')
    services.value = data
  } catch {
    // silently fail — user sees empty list
  } finally {
    loadingServices.value = false
  }
}

onMounted(loadServices)

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
