<template>
  <div class="scw-animate-in max-w-2xl mx-auto">
    <div class="flex items-center gap-3 mb-5">
      <Button icon="pi pi-arrow-left" severity="secondary" text rounded aria-label="Retour" @click="router.push('/clients')" />
      <h1 class="text-xl sm:text-2xl font-bold tracking-tight text-slate-100">
        {{ isEdit ? 'Modifier client' : 'Nouveau client' }}
      </h1>
    </div>

    <form class="scw-panel p-5 sm:p-6 space-y-5" @submit.prevent="submit">
      <div class="grid gap-5 sm:grid-cols-2">
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Nom complet <span class="text-red-400">*</span></label>
          <InputText v-model="form.name" class="w-full" placeholder="Jean Dupont" required />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Téléphone <span class="text-red-400">*</span></label>
          <InputText v-model="form.phone" type="tel" class="w-full" placeholder="+226 xx xx xx xx" required />
        </div>
      </div>

      <div>
        <label class="block text-sm font-medium text-slate-300 mb-2">Type d'abonnement <span class="text-red-400">*</span></label>
        <SelectButton v-model="form.type" :options="TYPES" option-label="label" option-value="value" :allow-empty="false" />
        <p class="text-xs text-slate-500 mt-1.5">{{ typeHint }}</p>
      </div>

      <div class="grid gap-5 sm:grid-cols-2">
        <div v-if="form.type === 'CARTE' || form.type === 'VIP'">
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Passages initiaux <span class="text-red-400">*</span></label>
          <InputNumber v-model="form.balance" :min="0" :max="100" class="w-full" placeholder="Ex: 10" />
        </div>
        <div v-if="form.type === 'BOUCLIER' || form.type === 'VIP'">
          <label class="block text-sm font-medium text-slate-300 mb-1.5">
            Date d'expiration <span v-if="form.type === 'BOUCLIER'" class="text-red-400">*</span>
          </label>
          <DatePicker v-model="form.expiresAt" date-format="dd/mm/yy" show-icon icon-display="input" class="w-full" />
        </div>
      </div>

      <div>
        <label class="block text-sm font-medium text-slate-300 mb-1.5">Tags</label>
        <InputText v-model="form.tags" class="w-full" placeholder="flotte, fidèle, entreprise" />
        <p class="text-xs text-slate-500 mt-1.5">Séparés par des virgules — pour filtrer et segmenter.</p>
      </div>

      <div>
        <label class="block text-sm font-medium text-slate-300 mb-1.5">Notes</label>
        <Textarea v-model="form.notes" rows="3" class="w-full" auto-resize placeholder="Remarques internes sur le client…" />
      </div>

      <Message v-if="error" severity="error" :closable="false" size="small">{{ error }}</Message>

      <div class="flex justify-end gap-2 pt-1">
        <Button label="Annuler" severity="secondary" text @click="router.push('/clients')" />
        <Button
          type="submit"
          :label="isEdit ? 'Mettre à jour' : 'Créer le client'"
          icon="pi pi-check"
          :loading="submitting"
        />
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useClientsStore } from '@/stores/clients'
import { CLIENT_TYPE_HINTS, PHONE_REGEX } from '@/constants'

import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import SelectButton from 'primevue/selectbutton'
import DatePicker from 'primevue/datepicker'
import Button from 'primevue/button'
import Message from 'primevue/message'

const route = useRoute()
const router = useRouter()
const store = useClientsStore()
const toast = useToast()

const isEdit = computed(() => !!route.params.id)
const error = ref('')
const submitting = ref(false)

const TYPES = [
  { value: 'CARTE', label: 'Carte passages' },
  { value: 'BOUCLIER', label: 'Bouclier' },
  { value: 'VIP', label: 'VIP' }
]

const form = ref({
  name: '', phone: '', type: 'CARTE', balance: 0, expiresAt: null, tags: '', notes: '', active: true
})

const typeHint = computed(() => CLIENT_TYPE_HINTS[form.value.type] ?? '')

onMounted(async () => {
  if (isEdit.value) {
    await store.loadById(route.params.id)
    const c = store.current
    if (c) {
      form.value = {
        name: c.name,
        phone: c.phone,
        type: c.type,
        balance: c.balance,
        expiresAt: c.expiresAt ? new Date(c.expiresAt) : null,
        tags: (c.tags ?? []).join(', '),
        notes: c.notes ?? '',
        active: c.active
      }
    }
  }
})

function toISODate(d) {
  if (!d) return null
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function submit() {
  if (submitting.value) return
  error.value = ''
  const trimmedPhone = form.value.phone.trim()
  if (!PHONE_REGEX.test(trimmedPhone)) {
    error.value = 'Numéro de téléphone invalide (ex : +22612345678)'
    return
  }
  submitting.value = true

  const payload = {
    name: form.value.name.trim(),
    phone: trimmedPhone,
    type: form.value.type,
    balance: form.value.type === 'BOUCLIER' ? 0 : form.value.balance,
    expiresAt: toISODate(form.value.expiresAt),
    tags: form.value.tags.split(',').map((t) => t.trim()).filter(Boolean),
    notes: form.value.notes.trim() || null,
    active: form.value.active
  }

  try {
    if (isEdit.value) {
      await store.update(route.params.id, payload)
      toast.add({ severity: 'success', summary: 'Client mis à jour', detail: payload.name, life: 3000 })
    } else {
      await store.create(payload)
      toast.add({ severity: 'success', summary: 'Client créé', detail: payload.name, life: 3000 })
    }
    router.push('/clients')
  } catch (err) {
    error.value = err.response?.data?.error ?? "Erreur lors de l'enregistrement"
  } finally {
    submitting.value = false
  }
}
</script>
