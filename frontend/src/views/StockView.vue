<template>
  <div class="scw-animate-in">
    <PageHeader title="Stock" icon="pi pi-box" subtitle="Produits & consommables">
      <template #actions>
        <Button label="Produit" icon="pi pi-plus" @click="openProductForm(null)" />
      </template>
    </PageHeader>

    <Message v-if="stock.lowStockCount > 0" severity="error" :closable="false" icon="pi pi-exclamation-triangle" class="mb-4">
      <strong>{{ stock.lowStockCount }}</strong>
      produit{{ stock.lowStockCount > 1 ? 's' : '' }} en rupture ou stock bas
    </Message>

    <DataTable
      :value="stock.products"
      :loading="stock.loading"
      data-key="id"
      paginator
      :rows="10"
      :rows-per-page-options="[10, 20, 50]"
      removable-sort
      row-hover
      class="scw-panel overflow-hidden"
      :pt="{ table: { style: 'min-width: 46rem' } }"
    >
      <template #empty>
        <EmptyState icon="pi pi-box" text="Aucun produit" hint="Ajoutez vos produits et définissez les seuils d'alerte">
          <template #action>
            <Button label="Nouveau produit" icon="pi pi-plus" @click="openProductForm(null)" />
          </template>
        </EmptyState>
      </template>

      <Column header="Produit" sortable field="name" style="min-width: 12rem">
        <template #body="{ data }">
          <div class="flex items-center gap-2">
            <span v-if="stock.isLow(data)" class="w-2 h-2 rounded-full bg-red-500 animate-pulse shrink-0" />
            <span class="font-semibold text-slate-100">{{ data.name }}</span>
          </div>
        </template>
      </Column>

      <Column header="Niveau de stock" style="min-width: 15rem">
        <template #body="{ data }">
          <div class="space-y-1.5">
            <div class="flex justify-between text-xs">
              <span :class="stock.isLow(data) ? 'text-red-400 font-semibold' : 'text-slate-200'">
                {{ formatQty(data.stock) }} {{ data.unit }}
              </span>
              <span class="text-slate-500">seuil {{ formatQty(data.alertThreshold) }} {{ data.unit }}</span>
            </div>
            <ProgressBar
              :value="Math.round(stock.stockRatio(data) * 100)"
              :show-value="false"
              style="height: 6px"
              :style="{ '--p-progressbar-value-background': stockHex(data) }"
            />
          </div>
        </template>
      </Column>

      <Column header="État" sortable field="stock" style="width: 8rem">
        <template #body="{ data }">
          <Tag :severity="levelSeverity(data)" :value="stockLevelLabel(data)" rounded />
        </template>
      </Column>

      <Column style="width: 12rem">
        <template #body="{ data }">
          <div class="flex justify-end gap-2">
            <Button icon="pi pi-pencil" severity="secondary" text rounded size="small" v-tooltip.top="'Modifier'" @click="openProductForm(data)" />
            <Button label="Réappro." icon="pi pi-plus-circle" size="small" @click="openRestockModal(data)" />
          </div>
        </template>
      </Column>
    </DataTable>

    <!-- ═══ Restock dialog ═══ -->
    <Dialog v-model:visible="restockVisible" modal header="Réapprovisionner" class="w-full max-w-sm mx-3">
      <div v-if="restockTarget" class="space-y-4">
        <p class="text-center text-slate-400 text-sm">
          {{ restockTarget.name }} — stock actuel :
          <strong class="text-slate-200">{{ formatQty(restockTarget.stock) }} {{ restockTarget.unit }}</strong>
        </p>
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">
            Quantité à ajouter ({{ restockTarget.unit }}) <span class="text-red-400">*</span>
          </label>
          <InputNumber
            v-model="restockQty"
            :min="0"
            :max-fraction-digits="3"
            class="w-full"
            input-class="text-center text-2xl font-bold"
            placeholder="0"
            autofocus
            @keyup.enter="confirmRestock"
          />
        </div>
        <Message v-if="restockError" severity="error" :closable="false" size="small">{{ restockError }}</Message>
      </div>
      <template #footer>
        <Button label="Annuler" severity="secondary" text @click="restockVisible = false" />
        <Button label="Confirmer" icon="pi pi-check" :loading="restocking" :disabled="!restockQty || restockQty <= 0" @click="confirmRestock" />
      </template>
    </Dialog>

    <!-- ═══ Product form dialog ═══ -->
    <Dialog v-model:visible="showProductForm" modal :header="editingProduct ? 'Modifier le produit' : 'Nouveau produit'" class="w-full max-w-md mx-3">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Nom <span class="text-red-400">*</span></label>
          <InputText v-model="productForm.name" class="w-full" placeholder="Ex: Shampoing voiture" />
        </div>
        <div v-if="!editingProduct">
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Stock initial</label>
          <InputNumber v-model="productForm.stock" :min="0" :max-fraction-digits="3" class="w-full" placeholder="0" />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Seuil d'alerte <span class="text-red-400">*</span></label>
          <InputNumber v-model="productForm.alertThreshold" :min="0" :max-fraction-digits="3" class="w-full" placeholder="Ex: 1.5" />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-300 mb-1.5">Unité <span class="text-red-400">*</span></label>
          <SelectButton v-model="productForm.unit" :options="UNITS" :allow-empty="false" />
        </div>
        <Message v-if="productFormError" severity="error" :closable="false" size="small">{{ productFormError }}</Message>
        <Button
          v-if="editingProduct"
          label="Supprimer ce produit"
          icon="pi pi-trash"
          severity="danger"
          outlined
          class="w-full"
          @click="confirmDelete"
        />
      </div>
      <template #footer>
        <Button label="Annuler" severity="secondary" text @click="closeProductForm" />
        <Button
          :label="editingProduct ? 'Mettre à jour' : 'Créer'"
          icon="pi pi-check"
          :loading="productFormSaving"
          :disabled="!productForm.name"
          @click="submitProductForm"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'
import { useStockStore } from '@/stores/stock'
import { STOCK_UNITS } from '@/constants'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import ProgressBar from 'primevue/progressbar'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import SelectButton from 'primevue/selectbutton'

const stock = useStockStore()
const confirm = useConfirm()
const toast = useToast()

onMounted(() => stock.loadAll())

// ── Restock ──────────────────────────────────────────────────────────── //
const restockVisible = ref(false)
const restockTarget = ref(null)
const restockQty = ref(null)
const restocking = ref(false)
const restockError = ref('')

function openRestockModal(product) {
  restockTarget.value = product
  restockQty.value = null
  restockError.value = ''
  restockVisible.value = true
}
async function confirmRestock() {
  if (restocking.value || !restockQty.value || restockQty.value <= 0) return
  restockError.value = ''
  restocking.value = true
  try {
    await stock.restock(restockTarget.value.id, restockQty.value)
    toast.add({ severity: 'success', summary: 'Stock mis à jour', detail: restockTarget.value.name, life: 3000 })
    restockVisible.value = false
  } catch (err) {
    restockError.value = err.response?.data?.error ?? 'Erreur lors du réapprovisionnement'
  } finally {
    restocking.value = false
  }
}

// ── Product form ─────────────────────────────────────────────────────── //
const UNITS = STOCK_UNITS
const showProductForm = ref(false)
const editingProduct = ref(null)
const productFormSaving = ref(false)
const productFormError = ref('')
const productForm = ref({ name: '', stock: 0, alertThreshold: 0, unit: 'L', active: true })

function openProductForm(product) {
  editingProduct.value = product
  productFormError.value = ''
  productForm.value = product
    ? { name: product.name, stock: Number(product.stock), alertThreshold: Number(product.alertThreshold), unit: product.unit, active: product.active }
    : { name: '', stock: 0, alertThreshold: 0, unit: 'L', active: true }
  showProductForm.value = true
}
function closeProductForm() {
  showProductForm.value = false
  editingProduct.value = null
}
async function submitProductForm() {
  if (productFormSaving.value || !productForm.value.name.trim()) return
  productFormError.value = ''
  productFormSaving.value = true
  try {
    const payload = { ...productForm.value, name: productForm.value.name.trim() }
    if (editingProduct.value) {
      await stock.update(editingProduct.value.id, payload)
      toast.add({ severity: 'success', summary: 'Produit mis à jour', detail: payload.name, life: 3000 })
    } else {
      await stock.create(payload)
      toast.add({ severity: 'success', summary: 'Produit créé', detail: payload.name, life: 3000 })
    }
    closeProductForm()
  } catch (err) {
    productFormError.value = err.response?.data?.error ?? "Erreur lors de l'enregistrement"
  } finally {
    productFormSaving.value = false
  }
}
function confirmDelete() {
  const target = editingProduct.value
  confirm.require({
    header: 'Supprimer ce produit ?',
    message: `« ${target?.name} » sera retiré du stock.`,
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Supprimer',
    rejectLabel: 'Annuler',
    acceptProps: { severity: 'danger' },
    accept: async () => {
      await stock.deactivate(target.id)
      toast.add({ severity: 'info', summary: 'Produit supprimé', detail: target.name, life: 3000 })
      closeProductForm()
    }
  })
}

// ── Formatting ───────────────────────────────────────────────────────── //
function formatQty(val) {
  const n = Number(val)
  return Number.isInteger(n) ? n : n.toFixed(3).replace(/\.?0+$/, '')
}
function stockLevelLabel(product) {
  const s = Number(product.stock)
  const t = Number(product.alertThreshold)
  if (s <= t) return 'Critique'
  if (s <= t * 2) return 'Modéré'
  return 'Élevé'
}
function levelSeverity(product) {
  const s = Number(product.stock)
  const t = Number(product.alertThreshold)
  if (s <= t) return 'danger'
  if (s <= t * 2) return 'warn'
  return 'success'
}
function stockHex(product) {
  const s = Number(product.stock)
  const t = Number(product.alertThreshold)
  if (s <= t) return '#ef4444'
  if (s <= t * 2) return '#f59e0b'
  return '#22c55e'
}
</script>
