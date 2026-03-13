<template>
  <div class="p-4 space-y-4 pb-6">

    <!-- Header -->
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-bold">Stock</h2>
      <button @click="openProductForm(null)" class="btn-primary py-2 px-4 text-sm">
        + Produit
      </button>
    </div>

    <!-- Low-stock alert banner -->
    <div
      v-if="stock.lowStockCount > 0"
      class="flex items-center gap-2 bg-red-900/40 border border-red-700 rounded-xl px-4 py-2 text-red-300 text-sm"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
      </svg>
      <span>
        <strong>{{ stock.lowStockCount }}</strong>
        produit{{ stock.lowStockCount > 1 ? 's' : '' }} en rupture ou stock bas
      </span>
    </div>

    <!-- Loading skeleton -->
    <div v-if="stock.loading" class="space-y-3">
      <div v-for="n in 4" :key="n" class="card animate-pulse space-y-3">
        <div class="flex justify-between items-center">
          <div class="h-4 bg-slate-700 rounded w-1/3"></div>
          <div class="flex gap-2">
            <div class="h-7 bg-slate-700 rounded w-16"></div>
            <div class="h-7 bg-slate-700 rounded w-20"></div>
          </div>
        </div>
        <div class="h-2 bg-slate-700 rounded-full"></div>
        <div class="h-3 bg-slate-700 rounded w-2/5"></div>
      </div>
    </div>

    <!-- Empty -->
    <div v-else-if="stock.products.length === 0" class="card text-center py-12 text-slate-400">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 mx-auto mb-3 opacity-40" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10" />
      </svg>
      <p class="font-medium">Aucun produit</p>
      <p class="text-sm mt-1">Ajoutez vos produits et définissez les seuils d'alerte</p>
    </div>

    <!-- Product list + sort + pagination -->
    <div v-else class="space-y-3">

      <!-- Sort controls -->
      <div class="flex items-center gap-2 text-xs">
        <span class="text-slate-500">Trier :</span>
        <button
          v-for="col in STOCK_SORT_COLS"
          :key="col.key"
          @click="toggleStockSort(col.key)"
          class="flex items-center gap-1 px-2 py-1 rounded-lg transition-colors"
          :class="stockSortKey === col.key ? 'bg-brand-500/20 text-brand-400' : 'bg-slate-700 text-slate-400 hover:text-slate-200'"
        >
          {{ col.label }}
          <span v-if="stockSortKey === col.key">{{ stockSortDir === 'asc' ? '↑' : '↓' }}</span>
        </button>
      </div>

      <div
        v-for="product in paginatedProducts"
        :key="product.id"
        class="card space-y-2"
        :class="stock.isLow(product) ? 'ring-1 ring-red-700' : ''"
      >
        <!-- Row 1: name + action buttons -->
        <div class="flex items-center justify-between gap-2">
          <div class="flex items-center gap-2 min-w-0">
            <span
              v-if="stock.isLow(product)"
              aria-hidden="true"
              class="w-2 h-2 rounded-full bg-red-500 shrink-0 animate-pulse"
            />
            <p class="font-semibold truncate">{{ product.name }}</p>
          </div>
          <div class="flex gap-2 shrink-0">
            <button
              @click="openProductForm(product)"
              class="text-slate-400 hover:text-slate-200 text-xs px-2 py-1 rounded-lg hover:bg-slate-700 transition-colors min-h-0 min-w-0"
            >
              Modifier
            </button>
            <button
              @click="openRestockModal(product)"
              class="btn-primary text-xs px-3 py-1.5 rounded-lg"
            >
              Réappro.
            </button>
          </div>
        </div>

        <!-- Row 2: stock level + progress bar -->
        <div class="space-y-1">
          <div class="flex justify-between text-xs">
            <span class="text-slate-400">Stock actuel</span>
            <span
              class="font-semibold"
              :class="stock.isLow(product) ? 'text-red-400' : 'text-slate-200'"
            >
              {{ formatQty(product.stock) }} {{ product.unit }}
            </span>
          </div>
          <div class="h-2 bg-slate-700 rounded-full overflow-hidden"
               :aria-label="`Niveau de stock : ${stockLevelLabel(product)}`" role="img">
            <div
              class="h-full rounded-full transition-all duration-500"
              :class="stock.stockColor(product)"
              :style="{ width: (stock.stockRatio(product) * 100).toFixed(0) + '%' }"
              aria-hidden="true"
            />
          </div>
          <div class="flex justify-between text-xs text-slate-500">
            <span>Seuil alerte : {{ formatQty(product.alertThreshold) }} {{ product.unit }}</span>
            <span
              class="font-medium"
              :class="{
                'text-red-400':   stock.isLow(product),
                'text-amber-400': !stock.isLow(product) && Number(product.stock) <= Number(product.alertThreshold) * 2,
                'text-green-400': Number(product.stock) > Number(product.alertThreshold) * 2
              }"
            >{{ stockLevelLabel(product) }}</span>
          </div>
        </div>
      </div>

      <!-- Stock pagination -->
      <div v-if="stockTotalPages > 1" class="flex items-center justify-between text-sm">
        <button
          @click="stockPage--"
          :disabled="stockPage === 1"
          class="px-3 py-1.5 rounded-lg bg-slate-700 text-slate-300 disabled:opacity-40 hover:bg-slate-600 transition-colors"
        >
          ← Préc.
        </button>
        <span class="text-slate-400 text-xs">{{ stockPage }} / {{ stockTotalPages }}</span>
        <button
          @click="stockPage++"
          :disabled="stockPage === stockTotalPages"
          class="px-3 py-1.5 rounded-lg bg-slate-700 text-slate-300 disabled:opacity-40 hover:bg-slate-600 transition-colors"
        >
          Suiv. →
        </button>
      </div>
    </div>

  </div>

  <!-- ═══ Restock modal ════════════════════════════════════════════════ -->
  <Teleport to="body">
  <div
    v-if="restockTarget"
    class="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60"
    @click.self="closeRestockModal"
  >
    <div class="w-full sm:max-w-md bg-slate-800 rounded-t-3xl sm:rounded-3xl p-6 space-y-4">
      <h3 class="text-lg font-bold text-center">Réapprovisionner</h3>
      <p class="text-center text-slate-400 text-sm">
        {{ restockTarget.name }} —
        stock actuel : <strong class="text-slate-200">{{ formatQty(restockTarget.stock) }} {{ restockTarget.unit }}</strong>
      </p>

      <div>
        <label class="block text-sm font-medium text-slate-300 mb-1">
          Quantité à ajouter ({{ restockTarget.unit }}) <span class="text-red-400">*</span>
        </label>
        <input
          v-model.number="restockQty"
          ref="restockInput"
          type="number"
          min="0.001"
          step="0.001"
          class="input-field text-center text-2xl font-bold"
          placeholder="0"
          @keyup.enter="confirmRestock"
        />
      </div>

      <p v-if="restockError" class="text-red-400 text-sm text-center">{{ restockError }}</p>

      <div class="flex gap-3">
        <button @click="closeRestockModal" class="btn-secondary flex-1">Annuler</button>
        <button
          @click="confirmRestock"
          :disabled="!restockQty || restockQty <= 0 || restocking"
          class="btn-primary flex-1"
        >
          {{ restocking ? 'Enregistrement...' : 'Confirmer' }}
        </button>
      </div>
    </div>
  </div>
  </Teleport>

  <!-- ═══ Add / Edit product modal ═════════════════════════════════════ -->
  <Teleport to="body">
  <div
    v-if="showProductForm"
    class="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60"
    @click.self="closeProductForm"
  >
    <div class="w-full sm:max-w-md bg-slate-800 rounded-t-3xl sm:rounded-3xl p-6 space-y-4 max-h-[90vh] overflow-y-auto">
      <h3 class="text-lg font-bold text-center">
        {{ editingProduct ? 'Modifier le produit' : 'Nouveau produit' }}
      </h3>

      <div>
        <label class="block text-sm font-medium text-slate-300 mb-1">Nom <span class="text-red-400">*</span></label>
        <input v-model="productForm.name" type="text" class="input-field" placeholder="Ex: Shampoing voiture" />
      </div>

      <div v-if="!editingProduct">
        <label class="block text-sm font-medium text-slate-300 mb-1">Stock initial</label>
        <input v-model.number="productForm.stock" type="number" min="0" step="0.001" class="input-field" placeholder="0" />
      </div>

      <div>
        <label class="block text-sm font-medium text-slate-300 mb-1">Seuil d'alerte <span class="text-red-400">*</span></label>
        <input v-model.number="productForm.alertThreshold" type="number" min="0" step="0.001" class="input-field" placeholder="Ex: 1.5" />
      </div>

      <div>
        <label class="block text-sm font-medium text-slate-300 mb-1">Unité <span class="text-red-400">*</span></label>
        <div class="grid grid-cols-4 gap-2">
          <button
            v-for="u in UNITS"
            :key="u"
            type="button"
            @click="productForm.unit = u"
            class="py-2 rounded-xl border-2 text-sm font-medium transition-all duration-100"
            :class="productForm.unit === u
              ? 'border-brand-500 bg-brand-500/10 text-brand-400'
              : 'border-slate-600 bg-slate-800 text-slate-400'"
          >{{ u }}</button>
        </div>
      </div>

      <p v-if="productFormError" class="text-red-400 text-sm text-center">{{ productFormError }}</p>

      <div class="flex gap-3">
        <button @click="closeProductForm" class="btn-secondary flex-1">Annuler</button>
        <button
          @click="submitProductForm"
          :disabled="!productForm.name || productFormSaving"
          class="btn-primary flex-1"
        >
          {{ productFormSaving ? 'Enregistrement...' : (editingProduct ? 'Mettre à jour' : 'Créer') }}
        </button>
      </div>

      <!-- Deactivate -->
      <button
        v-if="editingProduct"
        @click="showDeleteConfirm = true"
        class="w-full py-3 rounded-xl border border-red-700 text-red-400 hover:bg-red-900/20 text-sm font-medium transition-colors"
      >
        Supprimer ce produit
      </button>
    </div>
  </div>
  </Teleport>

  <ConfirmModal
    v-if="showDeleteConfirm"
    title="Supprimer ce produit ?"
    :message="`« ${editingProduct?.name} » sera retiré du stock.`"
    confirm-label="Supprimer"
    @confirm="doDeactivate"
    @cancel="showDeleteConfirm = false"
  />
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { useStockStore } from '@/stores/stock'
import ConfirmModal from '@/components/ConfirmModal.vue'
import { STOCK_UNITS } from '@/constants'

const stock = useStockStore()

// ── Sorting ──────────────────────────────────────────────────────── //
const STOCK_SORT_COLS = [
  { key: 'name',  label: 'Nom' },
  { key: 'stock', label: 'Stock' }
]
const stockSortKey = ref('name')
const stockSortDir = ref('asc')

function toggleStockSort(key) {
  if (stockSortKey.value === key) {
    stockSortDir.value = stockSortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    stockSortKey.value = key
    stockSortDir.value = 'asc'
  }
  stockPage.value = 1
}

// ── Pagination ───────────────────────────────────────────────────── //
const STOCK_PAGE_SIZE = 10
const stockPage       = ref(1)

const sortedProducts = computed(() => {
  const list = [...stock.products]
  list.sort((a, b) => {
    let cmp = 0
    if (stockSortKey.value === 'name')  cmp = a.name.localeCompare(b.name, 'fr')
    if (stockSortKey.value === 'stock') cmp = Number(a.stock) - Number(b.stock)
    return stockSortDir.value === 'asc' ? cmp : -cmp
  })
  return list
})

const stockTotalPages  = computed(() => Math.max(1, Math.ceil(sortedProducts.value.length / STOCK_PAGE_SIZE)))
const paginatedProducts = computed(() => {
  const start = (stockPage.value - 1) * STOCK_PAGE_SIZE
  return sortedProducts.value.slice(start, start + STOCK_PAGE_SIZE)
})

onMounted(() => stock.loadAll())

// ── Restock modal ─────────────────────────────────────────────────── //
const restockTarget = ref(null)
const restockQty    = ref(null)
const restocking    = ref(false)
const restockError  = ref('')
const restockInput  = ref(null)

function openRestockModal(product) {
  restockTarget.value = product
  restockQty.value    = null
  restockError.value  = ''
  nextTick(() => restockInput.value?.focus())
}

function closeRestockModal() {
  restockTarget.value = null
}

async function confirmRestock() {
  if (restocking.value) return
  if (!restockQty.value || restockQty.value <= 0) return
  restockError.value = ''
  restocking.value   = true
  try {
    await stock.restock(restockTarget.value.id, restockQty.value)
    closeRestockModal()
  } catch (err) {
    restockError.value = err.response?.data?.error ?? 'Erreur lors du réapprovisionnement'
  } finally {
    restocking.value = false
  }
}

// ── Add / Edit product form ───────────────────────────────────────── //
const UNITS = STOCK_UNITS

const showProductForm    = ref(false)
const editingProduct     = ref(null)
const productFormSaving  = ref(false)
const productFormError   = ref('')
const showDeleteConfirm  = ref(false)

const productForm = ref({ name: '', stock: 0, alertThreshold: 0, unit: 'L', active: true })

function openProductForm(product) {
  editingProduct.value = product
  productFormError.value = ''
  if (product) {
    productForm.value = {
      name:           product.name,
      stock:          Number(product.stock),
      alertThreshold: Number(product.alertThreshold),
      unit:           product.unit,
      active:         product.active
    }
  } else {
    productForm.value = { name: '', stock: 0, alertThreshold: 0, unit: 'L', active: true }
  }
  showProductForm.value = true
}

function closeProductForm() {
  showProductForm.value  = false
  editingProduct.value   = null
}

async function submitProductForm() {
  if (productFormSaving.value) return
  if (!productForm.value.name.trim()) return
  productFormError.value = ''
  productFormSaving.value = true
  try {
    const payload = {
      ...productForm.value,
      name: productForm.value.name.trim()
    }
    if (editingProduct.value) {
      await stock.update(editingProduct.value.id, payload)
    } else {
      await stock.create(payload)
    }
    closeProductForm()
  } catch (err) {
    productFormError.value = err.response?.data?.error ?? 'Erreur lors de l\'enregistrement'
  } finally {
    productFormSaving.value = false
  }
}

async function doDeactivate() {
  showDeleteConfirm.value = false
  await stock.deactivate(editingProduct.value.id)
  closeProductForm()
}

// ── Formatting ────────────────────────────────────────────────────── //
function formatQty(val) {
  const n = Number(val)
  return Number.isInteger(n) ? n : n.toFixed(3).replace(/\.?0+$/, '')
}

function stockLevelLabel(product) {
  const s = Number(product.stock)
  const t = Number(product.alertThreshold)
  if (s <= t)     return 'Critique'
  if (s <= t * 2) return 'Modéré'
  return 'Élevé'
}
</script>
