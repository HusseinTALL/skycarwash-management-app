/**
 * useFocusTrap(containerRef)
 *
 * Traps keyboard focus within a modal container:
 * - Tab wraps from last focusable → first
 * - Shift+Tab wraps from first focusable → last
 * - Focus is restored to the previously active element on unmount
 *
 * No external packages required — works with any Vue 3 component.
 */
import { onMounted, onBeforeUnmount } from 'vue'

const FOCUSABLE_SELECTORS = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])'
].join(', ')

export function useFocusTrap(containerRef) {
  let previouslyFocused = null

  function getFocusable() {
    return Array.from(containerRef.value?.querySelectorAll(FOCUSABLE_SELECTORS) ?? [])
  }

  function handleKeyDown(event) {
    if (event.key !== 'Tab') return
    const focusable = getFocusable()
    if (!focusable.length) return

    const first = focusable[0]
    const last  = focusable[focusable.length - 1]

    if (event.shiftKey) {
      if (document.activeElement === first) {
        event.preventDefault()
        last.focus()
      }
    } else {
      if (document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }
  }

  onMounted(() => {
    previouslyFocused = document.activeElement
    // Focus the first focusable element unless the container already manages focus
    const focusable = getFocusable()
    if (focusable.length && !containerRef.value?.contains(document.activeElement)) {
      focusable[0].focus()
    }
    document.addEventListener('keydown', handleKeyDown)
  })

  onBeforeUnmount(() => {
    document.removeEventListener('keydown', handleKeyDown)
    previouslyFocused?.focus()
  })
}
