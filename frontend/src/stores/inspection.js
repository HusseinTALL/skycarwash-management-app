import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/axios'
import { compressImage } from '@/utils/image'

/**
 * Staff-side inspection reports (état des lieux véhicule).
 * Photos are compressed client-side then uploaded one by one (multipart).
 */
export const useInspectionStore = defineStore('inspection', () => {
  const recent = ref([])
  const loading = ref(false)

  async function createReport(payload) {
    const { data } = await api.post('/inspections', payload)
    return data
  }

  /**
   * Compress + upload one photo to a report.
   * @param {number} reportId
   * @param {{file: File, phase: string, zone: string, caption?: string,
   *          damageId?: number, foundItemId?: number}} p
   */
  async function uploadPhoto(reportId, p) {
    const blob = await compressImage(p.file)
    const form = new FormData()
    form.append('file', blob, 'photo.jpg')
    form.append('phase', p.phase)
    form.append('zone', p.zone)
    if (p.caption) form.append('caption', p.caption)
    if (p.damageId != null) form.append('damageId', p.damageId)
    if (p.foundItemId != null) form.append('foundItemId', p.foundItemId)

    const { data } = await api.post(`/inspections/${reportId}/photos`, form, {
      headers: { 'Content-Type': undefined }, // let the browser set the multipart boundary
      timeout: 30000
    })
    return data
  }

  /** Upload the client's signature (PNG blob) validating the initial state. */
  async function uploadSignature(reportId, blob, signerName) {
    const form = new FormData()
    form.append('file', blob, 'signature.png')
    if (signerName) form.append('signerName', signerName)
    await api.post(`/inspections/${reportId}/signature`, form, {
      headers: { 'Content-Type': undefined },
      timeout: 30000
    })
  }

  async function fetchReport(id) {
    const { data } = await api.get(`/inspections/${id}`)
    return data
  }

  async function fetchByTransaction(transactionId) {
    const res = await api.get('/inspections', { params: { transactionId } })
    return res.status === 204 ? null : res.data
  }

  async function fetchRecent() {
    loading.value = true
    try {
      const { data } = await api.get('/inspections')
      recent.value = data
    } finally {
      loading.value = false
    }
  }

  return { recent, loading, createReport, uploadPhoto, uploadSignature, fetchReport, fetchByTransaction, fetchRecent }
})
