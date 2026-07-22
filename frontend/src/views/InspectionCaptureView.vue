<template>
  <div class="p-4 space-y-6 pb-24 max-w-2xl mx-auto">

    <header class="space-y-1">
      <h1 class="text-lg font-bold text-white">Rapport d'état du véhicule</h1>
      <p class="text-sm text-slate-400">
        Contrôle visuel avant lavage — protège le client et l'entreprise.
      </p>
    </header>

    <!-- ── Véhicule ─────────────────────────────────────────── -->
    <section class="card space-y-4">
      <h2 class="font-semibold text-slate-200">Véhicule</h2>
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-2">
        <button
          v-for="(label, key) in VEHICLE_TYPE_LABELS"
          :key="key"
          type="button"
          @click="form.vehicleType = key"
          class="rounded-xl py-3 text-sm font-medium border transition-colors"
          :class="form.vehicleType === key
            ? 'bg-brand-600 border-brand-500 text-white'
            : 'bg-slate-700 border-slate-600 text-slate-300 hover:bg-slate-600'"
        >
          <span class="text-lg">{{ VEHICLE_TYPE_ICONS[key] }}</span>
          <span class="block">{{ label }}</span>
        </button>
      </div>
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <input v-model="form.plate" class="input-field" placeholder="Immatriculation (ex : 11 AA 1234)" />
        <input v-model="form.vehicleLabel" class="input-field" placeholder="Marque / modèle (optionnel)" />
      </div>
    </section>

    <!-- ── Client ───────────────────────────────────────────── -->
    <section class="card space-y-3">
      <h2 class="font-semibold text-slate-200">Client</h2>
      <input v-model="form.customerName" class="input-field" placeholder="Nom du client" />
      <input v-model="form.customerPhone" type="tel" class="input-field" placeholder="Téléphone (accès au portail)" />
      <p class="text-xs text-slate-500">
        Le numéro donne au client l'accès à ses rapports. Code par défaut : les 4 derniers chiffres.
      </p>
    </section>

    <!-- ── Photos avant lavage ──────────────────────────────── -->
    <section class="card space-y-3">
      <h2 class="font-semibold text-slate-200">Photos avant lavage</h2>
      <p class="text-xs text-slate-500">Prenez une photo par zone. Elles sont horodatées automatiquement.</p>
      <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
        <div v-for="zone in DEFAULT_PHOTO_ZONES" :key="zone" class="space-y-1">
          <label class="block cursor-pointer">
            <input type="file" accept="image/*" capture="environment" class="hidden"
                   @change="onZonePhoto(zone, $event)" />
            <div class="aspect-[4/3] rounded-xl border-2 border-dashed border-slate-600 bg-slate-700/40
                        flex items-center justify-center overflow-hidden hover:border-brand-500 transition-colors">
              <img v-if="zonePhotos[zone]" :src="zonePhotos[zone].preview" class="w-full h-full object-cover" />
              <span v-else class="text-2xl text-slate-500">＋</span>
            </div>
          </label>
          <p class="text-xs text-center text-slate-400">{{ PHOTO_ZONE_LABELS[zone] }}</p>
        </div>
      </div>
    </section>

    <!-- ── Dommages existants ───────────────────────────────── -->
    <section class="card space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="font-semibold text-slate-200">Dommages déjà présents</h2>
        <button type="button" @click="addDamage" class="text-brand-400 text-sm font-medium">+ Ajouter</button>
      </div>
      <p v-if="damages.length === 0" class="text-xs text-slate-500">
        Rayures, pare-chocs fissuré, jante abîmée, pare-brise fissuré…
      </p>
      <div v-for="(d, i) in damages" :key="i" class="rounded-xl bg-slate-700/50 p-3 space-y-2">
        <div class="flex items-start gap-2">
          <div class="flex-1 space-y-2">
            <input v-model="d.zoneLabel" class="input-field text-sm" placeholder="Zone (ex : Aile avant gauche)" />
            <textarea v-model="d.description" rows="2" class="input-field text-sm" placeholder="Description du dommage"></textarea>
          </div>
          <button type="button" @click="damages.splice(i, 1)" class="text-red-400 text-xl leading-none px-1" aria-label="Supprimer">×</button>
        </div>
        <label class="inline-flex items-center gap-2 text-xs text-slate-400 cursor-pointer">
          <input type="file" accept="image/*" capture="environment" class="hidden" @change="onEntityPhoto(d, $event)" />
          <span class="px-2 py-1 rounded-lg bg-slate-600 text-slate-200">📷 {{ d.preview ? 'Photo ajoutée' : 'Ajouter une photo' }}</span>
          <img v-if="d.preview" :src="d.preview" class="w-10 h-10 rounded object-cover" />
        </label>
      </div>
    </section>

    <!-- ── Objets retrouvés ─────────────────────────────────── -->
    <section class="card space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="font-semibold text-slate-200">Objets retrouvés</h2>
        <button type="button" @click="addItem" class="text-brand-400 text-sm font-medium">+ Ajouter</button>
      </div>
      <div v-for="(it, i) in items" :key="i" class="rounded-xl bg-slate-700/50 p-3 space-y-2">
        <div class="flex items-start gap-2">
          <div class="flex-1 space-y-2">
            <div class="flex gap-2">
              <input v-model="it.name" class="input-field text-sm flex-1" placeholder="Nom de l'objet" />
              <input v-model.number="it.quantity" type="number" min="1" class="input-field text-sm w-16" />
            </div>
            <div class="flex flex-wrap gap-1">
              <button v-for="p in FOUND_ITEM_PRESETS" :key="p" type="button" @click="it.name = p"
                      class="text-xs px-2 py-0.5 rounded-full bg-slate-600 text-slate-300 hover:bg-slate-500">{{ p }}</button>
            </div>
            <textarea v-model="it.description" rows="2" class="input-field text-sm"
                      placeholder="Description (ex : 15 000 FCFA dans la boîte à gants)"></textarea>
            <input v-model="it.remark" class="input-field text-sm" placeholder="Remarque (optionnel)" />
          </div>
          <button type="button" @click="items.splice(i, 1)" class="text-red-400 text-xl leading-none px-1" aria-label="Supprimer">×</button>
        </div>
        <label class="inline-flex items-center gap-2 text-xs text-slate-400 cursor-pointer">
          <input type="file" accept="image/*" capture="environment" class="hidden" @change="onEntityPhoto(it, $event)" />
          <span class="px-2 py-1 rounded-lg bg-slate-600 text-slate-200">📷 {{ it.preview ? 'Photo ajoutée' : 'Ajouter une photo' }}</span>
          <img v-if="it.preview" :src="it.preview" class="w-10 h-10 rounded object-cover" />
        </label>
      </div>
    </section>

    <!-- ── Remarques ────────────────────────────────────────── -->
    <section class="card space-y-2">
      <h2 class="font-semibold text-slate-200">Remarques générales</h2>
      <textarea v-model="form.remarks" rows="3" class="input-field" placeholder="Observations complémentaires…"></textarea>
    </section>

    <p v-if="errorMsg" class="text-red-400 text-sm text-center">{{ errorMsg }}</p>

    <!-- Sticky submit -->
    <div class="fixed bottom-0 inset-x-0 bg-slate-900/95 border-t border-slate-700 p-3 z-20">
      <div class="max-w-2xl mx-auto flex gap-3">
        <button type="button" @click="router.back()" class="btn-secondary px-5">Annuler</button>
        <button type="button" @click="submit" class="btn-primary flex-1" :disabled="saving">
          <span v-if="saving">{{ progress }}</span>
          <span v-else>Enregistrer le rapport</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useInspectionStore } from '@/stores/inspection'
import {
  VEHICLE_TYPE_LABELS, VEHICLE_TYPE_ICONS,
  DEFAULT_PHOTO_ZONES, PHOTO_ZONE_LABELS, FOUND_ITEM_PRESETS
} from '@/constants'

const route = useRoute()
const router = useRouter()
const store = useInspectionStore()

const form = reactive({
  transactionId: route.query.transactionId ? Number(route.query.transactionId) : null,
  clientId:      route.query.clientId ? Number(route.query.clientId) : null,
  customerName:  route.query.name || '',
  customerPhone: '',
  vehicleType:   'VOITURE',
  plate:         '',
  vehicleLabel:  '',
  remarks:       ''
})

const zonePhotos = reactive({}) // zone -> { file, preview }
const damages = ref([])
const items = ref([])
const objectUrls = []

const saving = ref(false)
const progress = ref('')
const errorMsg = ref('')

function preview(file) {
  const url = URL.createObjectURL(file)
  objectUrls.push(url)
  return url
}

function onZonePhoto(zone, e) {
  const file = e.target.files?.[0]
  if (file) zonePhotos[zone] = { file, preview: preview(file) }
}
function onEntityPhoto(entity, e) {
  const file = e.target.files?.[0]
  if (file) { entity.file = file; entity.preview = preview(file) }
}
function addDamage() { damages.value.push({ zoneLabel: '', description: '', file: null, preview: null }) }
function addItem() { items.value.push({ name: '', description: '', quantity: 1, remark: '', file: null, preview: null }) }

async function submit() {
  if (saving.value) return
  errorMsg.value = ''

  const validDamages = damages.value.filter((d) => d.description?.trim())
  const validItems = items.value.filter((i) => i.name?.trim())

  saving.value = true
  try {
    progress.value = 'Enregistrement…'
    const report = await store.createReport({
      transactionId: form.transactionId,
      clientId:      form.clientId,
      customerName:  form.customerName || null,
      customerPhone: form.customerPhone || null,
      vehicleType:   form.vehicleType,
      plate:         form.plate || null,
      vehicleLabel:  form.vehicleLabel || null,
      remarks:       form.remarks || null,
      damages:       validDamages.map((d) => ({ zoneLabel: d.zoneLabel || null, description: d.description })),
      foundItems:    validItems.map((i) => ({ name: i.name, description: i.description || null, quantity: i.quantity || 1, remark: i.remark || null }))
    })

    // Build the list of photo uploads.
    const uploads = []
    for (const zone of DEFAULT_PHOTO_ZONES) {
      if (zonePhotos[zone]) uploads.push({ file: zonePhotos[zone].file, phase: 'BEFORE', zone })
    }
    validDamages.forEach((d, idx) => {
      if (d.file) uploads.push({ file: d.file, phase: 'BEFORE', zone: 'DOMMAGE', damageId: report.damages[idx].id })
    })
    validItems.forEach((it, idx) => {
      if (it.file) uploads.push({ file: it.file, phase: 'BEFORE', zone: 'AUTRE', foundItemId: report.foundItems[idx].id })
    })

    for (let i = 0; i < uploads.length; i++) {
      progress.value = `Photos ${i + 1}/${uploads.length}…`
      await store.uploadPhoto(report.id, uploads[i])
    }

    router.replace({ name: 'InspectionDetail', params: { id: report.id } })
  } catch (err) {
    errorMsg.value = err.response?.data?.error || 'Échec de l\'enregistrement. Vérifiez votre connexion.'
    saving.value = false
  }
}

onBeforeUnmount(() => objectUrls.forEach((u) => URL.revokeObjectURL(u)))
</script>
