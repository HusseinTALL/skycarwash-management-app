<template>
  <div class="scw-animate-in max-w-2xl mx-auto pb-24">
    <PageHeader
      title="Rapport d'état du véhicule"
      icon="pi pi-camera"
      subtitle="Contrôle visuel avant lavage — protège le client et l'entreprise"
    />

    <div class="space-y-4">
      <!-- Véhicule -->
      <section class="scw-panel p-5 space-y-4">
        <h2 class="font-semibold text-slate-200">Véhicule</h2>
        <SelectButton v-model="form.vehicleType" :options="vehicleOptions" option-label="label" option-value="value" :allow-empty="false" class="flex-wrap">
          <template #option="{ option }">
            <span class="flex items-center gap-1.5"><span class="text-base">{{ VEHICLE_TYPE_ICONS[option.value] }}</span> {{ option.label }}</span>
          </template>
        </SelectButton>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <InputText v-model="form.plate" class="w-full" placeholder="Immatriculation (ex : 11 AA 1234)" />
          <InputText v-model="form.vehicleLabel" class="w-full" placeholder="Marque / modèle (optionnel)" />
        </div>
      </section>

      <!-- Client -->
      <section class="scw-panel p-5 space-y-3">
        <h2 class="font-semibold text-slate-200">Client</h2>
        <InputText v-model="form.customerName" class="w-full" placeholder="Nom du client" />
        <InputText v-model="form.customerPhone" type="tel" class="w-full" placeholder="Téléphone (accès au portail)" />
        <p class="text-xs text-slate-500">
          Le numéro donne au client l'accès à ses rapports. Code par défaut : les 4 derniers chiffres.
        </p>
      </section>

      <!-- Photos avant lavage -->
      <section class="scw-panel p-5 space-y-3">
        <h2 class="font-semibold text-slate-200">Photos avant lavage</h2>
        <p class="text-xs text-slate-500">Prenez une photo par zone. Elles sont horodatées automatiquement.</p>
        <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
          <div v-for="zone in DEFAULT_PHOTO_ZONES" :key="zone" class="space-y-1">
            <label class="block cursor-pointer">
              <input type="file" accept="image/*" capture="environment" class="hidden" @change="onZonePhoto(zone, $event)" />
              <div class="aspect-[4/3] rounded-xl border-2 border-dashed border-slate-700 bg-slate-800/40 grid place-items-center overflow-hidden hover:border-primary transition-colors">
                <img v-if="zonePhotos[zone]" :src="zonePhotos[zone].preview" class="w-full h-full object-cover" />
                <i v-else class="pi pi-plus text-xl text-slate-500" />
              </div>
            </label>
            <p class="text-xs text-center text-slate-400">{{ PHOTO_ZONE_LABELS[zone] }}</p>
          </div>
        </div>
      </section>

      <!-- Dommages -->
      <section class="scw-panel p-5 space-y-3">
        <div class="flex items-center justify-between">
          <h2 class="font-semibold text-slate-200">Dommages déjà présents</h2>
          <Button label="Ajouter" icon="pi pi-plus" text size="small" @click="addDamage" />
        </div>
        <p v-if="damages.length === 0" class="text-xs text-slate-500">Rayures, pare-chocs fissuré, jante abîmée, pare-brise fissuré…</p>
        <div v-for="(d, i) in damages" :key="i" class="rounded-xl bg-slate-800/60 p-3 space-y-2">
          <div class="flex items-start gap-2">
            <div class="flex-1 space-y-2">
              <InputText v-model="d.zoneLabel" class="w-full" placeholder="Zone (ex : Aile avant gauche)" />
              <Textarea v-model="d.description" rows="2" class="w-full" placeholder="Description du dommage" auto-resize />
            </div>
            <Button icon="pi pi-times" severity="danger" text rounded size="small" @click="damages.splice(i, 1)" />
          </div>
          <label class="inline-flex items-center gap-2 text-xs text-slate-400 cursor-pointer">
            <input type="file" accept="image/*" capture="environment" class="hidden" @change="onEntityPhoto(d, $event)" />
            <span class="px-2 py-1 rounded-lg bg-slate-700 text-slate-200 flex items-center gap-1"><i class="pi pi-camera text-[11px]" /> {{ d.preview ? 'Photo ajoutée' : 'Ajouter une photo' }}</span>
            <img v-if="d.preview" :src="d.preview" class="w-10 h-10 rounded object-cover" />
          </label>
        </div>
      </section>

      <!-- Objets retrouvés -->
      <section class="scw-panel p-5 space-y-3">
        <div class="flex items-center justify-between">
          <h2 class="font-semibold text-slate-200">Objets retrouvés</h2>
          <Button label="Ajouter" icon="pi pi-plus" text size="small" @click="addItem" />
        </div>
        <div v-for="(it, i) in items" :key="i" class="rounded-xl bg-slate-800/60 p-3 space-y-2">
          <div class="flex items-start gap-2">
            <div class="flex-1 space-y-2">
              <div class="flex gap-2">
                <InputText v-model="it.name" class="flex-1" placeholder="Nom de l'objet" />
                <InputNumber v-model="it.quantity" :min="1" class="w-20" show-buttons :input-style="{ width: '2.5rem' }" />
              </div>
              <div class="flex flex-wrap gap-1">
                <button v-for="p in FOUND_ITEM_PRESETS" :key="p" type="button" class="text-xs px-2 py-0.5 rounded-full bg-slate-700 text-slate-300 hover:bg-slate-600" @click="it.name = p">{{ p }}</button>
              </div>
              <Textarea v-model="it.description" rows="2" class="w-full" placeholder="Description (ex : 15 000 FCFA dans la boîte à gants)" auto-resize />
              <InputText v-model="it.remark" class="w-full" placeholder="Remarque (optionnel)" />
            </div>
            <Button icon="pi pi-times" severity="danger" text rounded size="small" @click="items.splice(i, 1)" />
          </div>
          <label class="inline-flex items-center gap-2 text-xs text-slate-400 cursor-pointer">
            <input type="file" accept="image/*" capture="environment" class="hidden" @change="onEntityPhoto(it, $event)" />
            <span class="px-2 py-1 rounded-lg bg-slate-700 text-slate-200 flex items-center gap-1"><i class="pi pi-camera text-[11px]" /> {{ it.preview ? 'Photo ajoutée' : 'Ajouter une photo' }}</span>
            <img v-if="it.preview" :src="it.preview" class="w-10 h-10 rounded object-cover" />
          </label>
        </div>
      </section>

      <!-- Remarques -->
      <section class="scw-panel p-5 space-y-2">
        <h2 class="font-semibold text-slate-200">Remarques générales</h2>
        <Textarea v-model="form.remarks" rows="3" class="w-full" placeholder="Observations complémentaires…" auto-resize />
      </section>

      <!-- Signature -->
      <section class="scw-panel p-5 space-y-2">
        <h2 class="font-semibold text-slate-200">Signature du client</h2>
        <p class="text-xs text-slate-500">Le client valide l'état initial du véhicule (facultatif).</p>
        <SignaturePad ref="signaturePad" />
      </section>

      <Message v-if="errorMsg" severity="error" :closable="false">{{ errorMsg }}</Message>
    </div>

    <!-- Sticky submit -->
    <div class="fixed bottom-0 inset-x-0 md:pl-64 bg-slate-950/90 border-t border-slate-800/80 backdrop-blur-xl p-3 z-20">
      <div class="max-w-2xl mx-auto flex gap-3">
        <Button label="Annuler" severity="secondary" outlined @click="router.back()" />
        <Button
          :label="saving ? progress : 'Enregistrer le rapport'"
          icon="pi pi-check"
          class="flex-1"
          :loading="saving"
          @click="submit"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useInspectionStore } from '@/stores/inspection'
import SignaturePad from '@/components/SignaturePad.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import {
  VEHICLE_TYPE_LABELS, VEHICLE_TYPE_ICONS,
  DEFAULT_PHOTO_ZONES, PHOTO_ZONE_LABELS, FOUND_ITEM_PRESETS
} from '@/constants'

import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import SelectButton from 'primevue/selectbutton'
import Message from 'primevue/message'

const route = useRoute()
const router = useRouter()
const store = useInspectionStore()

const vehicleOptions = Object.entries(VEHICLE_TYPE_LABELS).map(([value, label]) => ({ value, label }))

const form = reactive({
  transactionId: route.query.transactionId ? Number(route.query.transactionId) : null,
  clientId: route.query.clientId ? Number(route.query.clientId) : null,
  customerName: route.query.name || '',
  customerPhone: '',
  vehicleType: 'VOITURE',
  plate: '',
  vehicleLabel: '',
  remarks: ''
})

const zonePhotos = reactive({})
const damages = ref([])
const items = ref([])
const signaturePad = ref(null)
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
      clientId: form.clientId,
      customerName: form.customerName || null,
      customerPhone: form.customerPhone || null,
      vehicleType: form.vehicleType,
      plate: form.plate || null,
      vehicleLabel: form.vehicleLabel || null,
      remarks: form.remarks || null,
      damages: validDamages.map((d) => ({ zoneLabel: d.zoneLabel || null, description: d.description })),
      foundItems: validItems.map((i) => ({ name: i.name, description: i.description || null, quantity: i.quantity || 1, remark: i.remark || null }))
    })

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

    if (signaturePad.value && !signaturePad.value.isEmpty()) {
      progress.value = 'Signature…'
      const blob = await signaturePad.value.toBlob()
      if (blob) await store.uploadSignature(report.id, blob, form.customerName)
    }

    router.replace({ name: 'InspectionDetail', params: { id: report.id } })
  } catch (err) {
    errorMsg.value = err.response?.data?.error || "Échec de l'enregistrement. Vérifiez votre connexion."
    saving.value = false
  }
}

onBeforeUnmount(() => objectUrls.forEach((u) => URL.revokeObjectURL(u)))
</script>
