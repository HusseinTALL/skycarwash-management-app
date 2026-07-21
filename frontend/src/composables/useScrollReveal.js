/**
 * v-reveal — lightweight scroll-driven reveal directive.
 *
 * Adds the `is-revealed` class the first time an element scrolls into view,
 * driving CSS transitions declared on `.reveal`. Optional modifiers/args let
 * callers stagger children or tune the trigger threshold without extra JS.
 *
 * Usage:
 *   <div v-reveal>…</div>                     // default fade + rise
 *   <div v-reveal:0.4>…</div>                 // custom threshold (0–1)
 *   <ul v-reveal.stagger>…</ul>               // stagger direct children
 *
 * Respects `prefers-reduced-motion`: matching users get the final state
 * immediately with no transition.
 */

const REDUCED_MOTION =
  typeof window !== 'undefined' &&
  window.matchMedia &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

function markStaggered(el) {
  const children = Array.from(el.children)
  children.forEach((child, i) => {
    child.style.setProperty('--reveal-delay', `${i * 90}ms`)
    child.classList.add('reveal')
  })
}

export const vReveal = {
  mounted(el, binding) {
    if (binding.modifiers.stagger) markStaggered(el)
    else el.classList.add('reveal')

    if (REDUCED_MOTION) {
      el.classList.add('is-revealed')
      el.querySelectorAll?.('.reveal').forEach((c) => c.classList.add('is-revealed'))
      return
    }

    const threshold =
      typeof binding.arg !== 'undefined' ? Number(binding.arg) : 0.15

    const observer = new IntersectionObserver(
      (entries, obs) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return
          if (binding.modifiers.stagger) {
            Array.from(el.children).forEach((c) => c.classList.add('is-revealed'))
          }
          el.classList.add('is-revealed')
          obs.unobserve(el)
        })
      },
      { threshold: isNaN(threshold) ? 0.15 : threshold, rootMargin: '0px 0px -8% 0px' }
    )

    observer.observe(el)
    el._revealObserver = observer
  },
  unmounted(el) {
    el._revealObserver?.disconnect()
  }
}

export default vReveal
