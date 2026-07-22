/**
 * Client-side image compression for inspection photos.
 *
 * Photos are resized and re-encoded to JPEG before upload — essential on
 * unstable/low-bandwidth connections (Ouaga) and to keep the database small,
 * since bytes are stored in Postgres.
 */

const DEFAULT_MAX_DIM = 1280
const DEFAULT_QUALITY = 0.7

/**
 * Compress a File/Blob to a JPEG Blob.
 * Falls back to the original file if the browser can't decode it.
 *
 * @param {File|Blob} file
 * @param {{maxDim?: number, quality?: number}} [opts]
 * @returns {Promise<Blob>}
 */
export async function compressImage(file, opts = {}) {
  const maxDim = opts.maxDim ?? DEFAULT_MAX_DIM
  const quality = opts.quality ?? DEFAULT_QUALITY

  const dataUrl = await readAsDataURL(file)
  const img = await loadImage(dataUrl)

  let { width, height } = img
  if (width > maxDim || height > maxDim) {
    const scale = Math.min(maxDim / width, maxDim / height)
    width = Math.round(width * scale)
    height = Math.round(height * scale)
  }

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')
  ctx.drawImage(img, 0, 0, width, height)

  const blob = await new Promise((resolve) =>
    canvas.toBlob((b) => resolve(b), 'image/jpeg', quality)
  )
  return blob || file
}

function readAsDataURL(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = reject
    img.src = src
  })
}
