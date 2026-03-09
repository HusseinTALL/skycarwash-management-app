<template>
  <div class="p-4 space-y-4">
    <!-- Header -->
    <div class="flex items-center gap-3">
      <RouterLink to="/clients" class="text-slate-400 hover:text-slate-200 min-h-0 min-w-0 p-1">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </RouterLink>
      <h2 class="text-xl font-bold">{{ isEdit ? 'Modifier client' : 'Nouveau client' }}</h2>
    </div>

    <form @submit.prevent="submit" class="space-y-4">

      <!-- Name -->
      <div>
        <label class="block text-sm font-medium text-slate-300 mb-1">Nom complet <span class="text-red-400">*</span></label>
        <input v-model="form.name" type="text" class="input-field" placeholder="Jean Dupont" required />
      </div>

      <!-- Phone -->
      <div>
        <label class="block text-sm font-medium text-slate-300 mb-1">Téléphone <span class="text-red-400">*</span></label>
        <input v-model="form.phone" type="tel" class="input-field" placeholder="+226 xx xx xx xx" required />
      </div>

      <!-- Type -->
      <div>
        <label class="block text-sm font-medium text-slate-300 mb-2">Type d'abonnement <span class="text-red-400">*</span></label>
        <div class="grid grid-cols-3 gap-2">
          <button
            v-for="t in TYPES"
            :key="t.value"
            type="button"
            @click="form.type = t.value"
            class="py-3 rounded-xl border-2 text-sm font-medium transition-all duration-100"
            :class="form.type === t.value
              ? 'border-brand-500 bg-brand-500/10 text-brand-400'
              : 'border-slate-600 bg-slate-800 text-slate-400 hover:border-slate-500'"
          >
            {{ t.label }}
          </button>
        </div>
        <p class="text-xs text-slate-500 mt-1">{{ typeHint }}</p>
      </div>

      <!-- Balance (CARTE / VIP) -->
      <div v-if="form.type === 'CARTE' || form.type === 'VIP'">
        <label class="block text-sm font-medium text-slate-300 mb-1">
          Passages initiaux <span class="text-red-400">*</span>
        </label>
        <input
          v-model.number="form.balance"
          type="number" min="0" max="100"
          class="input-field"
          placeholder="Ex: 10"
          required
        />
      </div>

      <!-- ExpiresAt (BOUCLIER / VIP) -->
      <div v-if="form.type === 'BOUCLIER' || form.type === 'VIP'">
        <label class="block text-sm font-medium text-slate-300 mb-1">
          Date d'expiration <span v-if="form.type === 'BOUCLIER'" class="text-red-400">*</span>
        </label>
        <input
          v-model="form.expiresAt"
          type="date"
          class="input-field"
          :required="form.type === 'BOUCLIER'"
        />
      </div>

      <!-- Error -->
      <p v-if="error" class="text-red-400 text-sm text-center">{{ error }}</p>

      <button type="submit" class="btn-primary w-full py-4" :disabled="submitting">
        {{ submitting ? 'Enregistrement...' : (isEdit ? 'Mettre à jour' : 'Créer le client') }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { useClientsStore } from '@/stores/clients'
import { CLIENT_TYPE_HINTS, PHONE_REGEX } from '@/constants'

const route    = useRoute()
const router   = useRouter()
const store    = useClientsStore()

const isEdit   = computed(() => !!route.params.id)
const error    = ref('')
const submitting = ref(false)

const TYPES = [
  { value: 'CARTE',    label: 'Carte passages' },
  { value: 'BOUCLIER', label: 'Bouclier'       },
  { value: 'VIP',      label: 'VIP'            }
]


const form = ref({
  name:      '',
  phone:     '',
  type:      'CARTE',
  balance:   0,
  expiresAt: '',
  active:    true
})

const typeHint = computed(() => CLIENT_TYPE_HINTS[form.value.type] ?? '')

onMounted(async () => {
  if (isEdit.value) {
    await store.loadById(route.params.id)
    const c = store.current
    if (c) {
      form.value = {
        name:      c.name,
        phone:     c.phone,
        type:      c.type,
        balance:   c.balance,
        expiresAt: c.expiresAt ?? '',
        active:    c.active
      }
    }
  }
})

async function submit() {
  if (submitting.value) return
  error.value    = ''
  const trimmedPhone = form.value.phone.trim()
  if (!PHONE_REGEX.test(trimmedPhone)) {
    error.value = 'Numéro de téléphone invalide (ex : +22612345678)'
    return
  }
  submitting.value = true

  const payload = {
    name:      form.value.name.trim(),
    phone:     trimmedPhone,
    type:      form.value.type,
    balance:   form.value.type === 'BOUCLIER' ? 0 : form.value.balance,
    expiresAt: form.value.expiresAt || null,
    active:    form.value.active
  }

  try {
    if (isEdit.value) {
      await store.update(route.params.id, payload)
    } else {
      await store.create(payload)
    }
    router.push('/clients')
  } catch (err) {
    error.value = err.response?.data?.error ?? 'Erreur lors de l\'enregistrement'
  } finally {
    submitting.value = false
  }
}
</script>
