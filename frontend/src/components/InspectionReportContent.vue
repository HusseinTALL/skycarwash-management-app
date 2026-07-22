<template>
  <div class="space-y-6">

    <!-- ── Résumé commande / véhicule ─────────────────────────── -->
    <section class="card space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="font-semibold text-slate-200">{{ vehicleLabel }}</h2>
        <span class="text-xs px-2 py-0.5 rounded-full"
              :class="report.status === 'COMPLETED' ? 'bg-green-900/50 text-green-300' : 'bg-brand-900/50 text-brand-300'">
          {{ statusLabel }}
        </span>
      </div>
      <dl class="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
        <div v-if="report.transactionId"><dt class="text-slate-500 text-xs">N° commande</dt><dd class="font-mono">#{{ report.transactionId }}</dd></div>
        <div><dt class="text-slate-500 text-xs">Date</dt><dd>{{ formatDateTime(report.createdAt) }}</dd></div>
        <div v-if="report.agency"><dt class="text-slate-500 text-xs">Agence</dt><dd>{{ report.agency }}</dd></div>
        <div v-if="report.plate"><dt class="text-slate-500 text-xs">Immatriculation</dt><dd>{{ report.plate }}</dd></div>
        <div v-if="report.serviceName"><dt class="text-slate-500 text-xs">Service</dt><dd>{{ report.serviceName }}</dd></div>
        <div v-if="report.amount != null"><dt class="text-slate-500 text-xs">Montant</dt><dd>{{ formatPrice(report.amount) }}</dd></div>
        <div v-if="report.paymentMethod"><dt class="text-slate-500 text-xs">Paiement</dt><dd>{{ paymentLabel(report.paymentMethod) }}</dd></div>
        <div v-if="report.createdByName"><dt class="text-slate-500 text-xs">Employé</dt><dd>{{ report.createdByName }}</dd></div>
        <div v-if="report.customerName"><dt class="text-slate-500 text-xs">Client</dt><dd>{{ report.customerName }}</dd></div>
      </dl>
    </section>

    <!-- ── Comparateur avant / après ──────────────────────────── -->
    <section v-if="comparePairs.length" class="card space-y-3">
      <h2 class="font-semibold text-slate-200">Comparaison avant / après</h2>
      <BeforeAfterSlider
        :key="activePair"
        :before-photo-id="comparePairs[activePair].before"
        :after-photo-id="comparePairs[activePair].after"
        :variant="variant"
      />
      <div v-if="comparePairs.length > 1" class="flex flex-wrap gap-2">
        <button v-for="(p, i) in comparePairs" :key="i" type="button" @click="activePair = i"
                class="text-xs px-2 py-1 rounded-full border transition-colors"
                :class="activePair === i ? 'bg-brand-600 border-brand-500 text-white' : 'bg-slate-700 border-slate-600 text-slate-300'">
          {{ zoneLabel(p.zone) }}
        </button>
      </div>
    </section>

    <!-- ── Photos avant ───────────────────────────────────────── -->
    <PhotoGallery v-if="beforePhotos.length" title="Photos avant lavage" :photos="beforePhotos" :variant="variant" />

    <!-- ── Photos après ───────────────────────────────────────── -->
    <PhotoGallery v-if="afterPhotos.length" title="Photos après lavage" :photos="afterPhotos" :variant="variant" />

    <!-- ── Dommages ───────────────────────────────────────────── -->
    <section v-if="report.damages?.length" class="card space-y-3">
      <h2 class="font-semibold text-slate-200">Dommages constatés avant lavage</h2>
      <div v-for="d in report.damages" :key="d.id" class="flex gap-3 items-start border-b border-slate-700 last:border-0 pb-3 last:pb-0">
        <AuthedImage v-if="d.photoId" :photo-id="d.photoId" :variant="variant" wrapper-class="w-16 h-16 rounded-lg shrink-0" />
        <div class="text-sm">
          <p v-if="d.zoneLabel" class="font-medium text-slate-200">{{ d.zoneLabel }}</p>
          <p class="text-slate-400">{{ d.description }}</p>
        </div>
      </div>
    </section>

    <!-- ── Objets retrouvés ───────────────────────────────────── -->
    <section v-if="report.foundItems?.length" class="card space-y-3">
      <h2 class="font-semibold text-slate-200">Objets retrouvés dans le véhicule</h2>
      <div v-for="it in report.foundItems" :key="it.id" class="flex gap-3 items-start border-b border-slate-700 last:border-0 pb-3 last:pb-0">
        <AuthedImage v-if="it.photoId" :photo-id="it.photoId" :variant="variant" wrapper-class="w-16 h-16 rounded-lg shrink-0" />
        <div class="text-sm flex-1">
          <p class="font-medium text-slate-200">{{ it.name }} <span v-if="it.quantity > 1" class="text-slate-500">×{{ it.quantity }}</span></p>
          <p v-if="it.description" class="text-slate-400">{{ it.description }}</p>
          <p v-if="it.remark" class="text-slate-500 text-xs italic">{{ it.remark }}</p>
        </div>
      </div>
    </section>

    <!-- ── Remarques ──────────────────────────────────────────── -->
    <section v-if="report.remarks" class="card space-y-2">
      <h2 class="font-semibold text-slate-200">Remarques</h2>
      <p class="text-sm text-slate-400 whitespace-pre-line">{{ report.remarks }}</p>
    </section>

    <!-- ── Signature du client ────────────────────────────────── -->
    <section v-if="report.signed" class="card space-y-2">
      <h2 class="font-semibold text-slate-200">Signature du client</h2>
      <AuthedImage :path="signaturePath" :variant="variant" fit="contain" wrapper-class="h-28 bg-white rounded-xl" />
      <p class="text-xs text-green-400">
        ✓ État initial validé par {{ report.signerName || 'le client' }} le {{ formatDateTime(report.signedAt) }}.
      </p>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import AuthedImage from '@/components/AuthedImage.vue'
import BeforeAfterSlider from '@/components/BeforeAfterSlider.vue'
import PhotoGallery from '@/components/PhotoGallery.vue'
import { PHOTO_ZONE_LABELS, VEHICLE_TYPE_LABELS, INSPECTION_STATUS_LABELS, PAYMENT_LABELS } from '@/constants'

const props = defineProps({
  report:  { type: Object, required: true },
  variant: { type: String, default: 'staff' }
})

const activePair = ref(0)

const beforePhotos = computed(() => (props.report.photos || []).filter((p) => p.phase === 'BEFORE'))
const afterPhotos = computed(() => (props.report.photos || []).filter((p) => p.phase === 'AFTER'))

/** Pair each "after" photo with a "before" photo of the same zone (fallback: first before). */
const comparePairs = computed(() => {
  if (!beforePhotos.value.length || !afterPhotos.value.length) return []
  return afterPhotos.value.map((a) => {
    const match = beforePhotos.value.find((b) => b.zone === a.zone) || beforePhotos.value[0]
    return { zone: a.zone, before: match.id, after: a.id }
  })
})

const vehicleLabel = computed(() => {
  const type = VEHICLE_TYPE_LABELS[props.report.vehicleType] || 'Véhicule'
  return props.report.vehicleLabel ? `${type} — ${props.report.vehicleLabel}` : type
})
const statusLabel = computed(() => INSPECTION_STATUS_LABELS[props.report.status] || props.report.status)
const signaturePath = computed(() => props.variant === 'portal'
  ? `/reports/${props.report.id}/signature`
  : `/inspections/${props.report.id}/signature`)

function zoneLabel(z) { return PHOTO_ZONE_LABELS[z] || z }
function paymentLabel(m) { return PAYMENT_LABELS[m] || m }
function formatPrice(f) { return new Intl.NumberFormat('fr-FR').format(f) + ' FCFA' }
function formatDateTime(iso) {
  if (!iso) return '--'
  return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' })
}
</script>
