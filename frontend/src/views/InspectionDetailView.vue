<template>
  <div class="scw-animate-in max-w-2xl mx-auto">
    <div v-if="loading" class="space-y-3">
      <Skeleton height="3rem" />
      <Skeleton height="16rem" border-radius="1rem" />
    </div>

    <template v-else-if="report">
      <div class="flex items-center justify-between mb-5">
        <div class="flex items-center gap-3">
          <Button icon="pi pi-arrow-left" severity="secondary" text rounded aria-label="Retour" @click="router.push('/inspections')" />
          <div>
            <h1 class="text-xl font-bold tracking-tight text-slate-100">Rapport d'état</h1>
            <p class="text-sm text-slate-500">Rapport #{{ report.id }}</p>
          </div>
        </div>
        <Button label="Accès client" icon="pi pi-qrcode" severity="secondary" outlined @click="showQr = true" />
      </div>

      <InspectionReportContent :report="report" variant="staff" />

      <!-- Signature -->
      <section v-if="!report.signed" class="scw-panel p-5 space-y-3 mt-4">
        <h2 class="font-semibold text-slate-200">Faire signer le client</h2>
        <p class="text-xs text-slate-500">Le client valide l'état initial du véhicule sur l'écran.</p>
        <SignaturePad ref="signaturePad" />
        <Button label="Enregistrer la signature" icon="pi pi-check" class="w-full" :loading="signing" @click="saveSignature" />
      </section>

      <!-- After photos -->
      <section class="scw-panel p-5 space-y-3 mt-4">
        <h2 class="font-semibold text-slate-200">Ajouter des photos « après lavage »</h2>
        <p class="text-xs text-slate-500">Permet au client de comparer avant → après.</p>
        <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
          <label v-for="zone in DEFAULT_PHOTO_ZONES" :key="zone" class="block cursor-pointer">
            <input type="file" accept="image/*" capture="environment" class="hidden" :disabled="uploading" @change="addAfter(zone, $event)" />
            <div class="aspect-[4/3] rounded-xl border-2 border-dashed border-slate-700 bg-slate-800/40 grid place-items-center hover:border-emerald-500 transition-colors">
              <i class="pi pi-plus text-xl text-slate-500" />
            </div>
            <p class="text-xs text-center text-slate-400 mt-1">{{ PHOTO_ZONE_LABELS[zone] }}</p>
          </label>
        </div>
        <div v-if="uploading" class="flex items-center gap-2 text-sm text-primary justify-center">
          <i class="pi pi-spin pi-spinner" /> Envoi de la photo…
        </div>
      </section>
    </template>

    <EmptyState v-else icon="pi pi-file" text="Rapport introuvable" />

    <!-- QR access dialog -->
    <Dialog v-model:visible="showQr" modal header="Accès client" class="w-full max-w-sm mx-3">
      <div class="text-center space-y-3">
        <h2 class="font-semibold text-slate-200">Espace « Rapports de lavage »</h2>
        <img v-if="qrDataUrl" :src="qrDataUrl" alt="QR portail client" class="mx-auto w-44 h-44 rounded-xl bg-white p-2" />
        <a :href="portalUrl" class="text-primary hover:underline text-sm break-all">{{ portalUrl }}</a>
        <div v-if="report?.customerPhone" class="text-sm text-slate-400">
          Téléphone : <span class="font-medium text-slate-200">{{ report.customerPhone }}</span><br />
          Code initial : les <span class="font-medium text-slate-200">4 derniers chiffres</span> du numéro
        </div>
        <Message v-else severity="warn" :closable="false" size="small">
          Aucun numéro enregistré — le client ne pourra pas accéder au portail.
        </Message>
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import QRCode from 'qrcode'
import { useToast } from 'primevue/usetoast'
import { useInspectionStore } from '@/stores/inspection'
import InspectionReportContent from '@/components/InspectionReportContent.vue'
import SignaturePad from '@/components/SignaturePad.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { DEFAULT_PHOTO_ZONES, PHOTO_ZONE_LABELS } from '@/constants'

import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Message from 'primevue/message'
import Skeleton from 'primevue/skeleton'

const route = useRoute()
const router = useRouter()
const store = useInspectionStore()
const toast = useToast()

const report = ref(null)
const loading = ref(true)
const uploading = ref(false)
const signing = ref(false)
const signaturePad = ref(null)
const showQr = ref(false)
const qrDataUrl = ref('')
const portalUrl = `${window.location.origin}/rapports`

async function load() {
  loading.value = true
  try {
    report.value = await store.fetchReport(route.params.id)
  } finally {
    loading.value = false
  }
}

async function saveSignature() {
  if (signing.value) return
  if (signaturePad.value.isEmpty()) {
    toast.add({ severity: 'warn', summary: 'Signature vide', detail: 'Demandez au client de signer.', life: 3000 })
    return
  }
  signing.value = true
  try {
    const blob = await signaturePad.value.toBlob()
    if (blob) {
      await store.uploadSignature(report.value.id, blob, report.value.customerName)
      toast.add({ severity: 'success', summary: 'Signature enregistrée', life: 3000 })
      await load()
    }
  } finally {
    signing.value = false
  }
}

async function addAfter(zone, e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    await store.uploadPhoto(report.value.id, { file, phase: 'AFTER', zone })
    toast.add({ severity: 'success', summary: 'Photo ajoutée', detail: PHOTO_ZONE_LABELS[zone], life: 2500 })
    await load()
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

onMounted(async () => {
  await load()
  try {
    qrDataUrl.value = await QRCode.toDataURL(portalUrl, { margin: 1, width: 220 })
  } catch { /* ignore */ }
})
</script>
