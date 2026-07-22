<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { RouterLink } from 'vue-router'
import { vReveal } from '@/composables/useScrollReveal'

/* ── Photography (bundled + optimized) ───────────────────────────── */
import imgExteriorFoam from '@/assets/landing/wash-exterior-foam.jpg'
import imgInterior from '@/assets/landing/wash-interior.jpg'
import imgFoamBlue from '@/assets/landing/wash-foam-blue.jpg'
import imgWheel from '@/assets/landing/wash-wheel.jpg'

/* ── Business identity ───────────────────────────────────────────── */
const BUSINESS = {
  name: 'Sky CarWash',
  city: 'Ouagadougou',
  year: '2026',
  phone: '+226 70 00 00 00',
  hours: 'Lun – Dim · 7h30 → 20h00',
  address: 'Ouaga 2000, Ouagadougou · Burkina Faso'
}

/* ── Vehicle segmentation ────────────────────────────────────────── */
const vehicles = [
  { key: 'moto',    label: 'Moto',       icon: '🏍️' },
  { key: 'voiture', label: 'Voiture',    icon: '🚗' },
  { key: 'suv',     label: '4x4 · SUV',  icon: '🚙' },
  { key: 'pickup',  label: 'Pick-up',    icon: '🛻' }
]
const activeVehicle = ref('voiture')

/* ── Pricing grid (FCFA) ─────────────────────────────────────────── */
const lavages = [
  {
    name: 'Lavage extérieur',
    desc: 'Carrosserie, jantes, vitres',
    icon: '💧',
    prices: { moto: 1000, voiture: 2000, suv: 2500, pickup: 3000 }
  },
  {
    name: 'Lavage intérieur',
    desc: 'Habitacle, plastiques, tapis',
    icon: '🧴',
    prices: { moto: null, voiture: 2500, suv: 3000, pickup: 3500 }
  },
  {
    name: 'Lavage complet',
    desc: 'Extérieur + intérieur',
    icon: '✨',
    popular: true,
    prices: { moto: 1500, voiture: 4000, suv: 5000, pickup: 6000 }
  },
  {
    name: 'Aspiration',
    desc: 'Sièges, moquettes, coffre',
    icon: '🌀',
    prices: { moto: null, voiture: 1500, suv: 2000, pickup: 2500 }
  }
]

const premium = [
  {
    name: 'Polissage / Brillance',
    desc: "Rénovation de l'éclat d'origine",
    icon: '🪩',
    prices: { moto: null, voiture: 15000, suv: 20000, pickup: 25000 }
  },
  {
    name: 'Protection / Cire',
    desc: 'Barrière hydrophobe longue durée',
    icon: '🛡️',
    prices: { moto: null, voiture: 10000, suv: 12000, pickup: 15000 }
  }
]

function priceFor(item) {
  const v = item.prices[activeVehicle.value]
  if (v === null || v === undefined) return null
  return new Intl.NumberFormat('fr-FR').format(v)
}

const activeVehicleLabel = computed(
  () => vehicles.find((v) => v.key === activeVehicle.value)?.label ?? ''
)

/* ── Services storytelling panels ────────────────────────────────── */
const services = [
  {
    tag: 'Extérieur',
    title: 'Une carrosserie qui capte la lumière.',
    body: "Prélavage, mousse active et rinçage sous pression décollent poussière et boue de l'harmattan sans rayer le vernis. Jantes et vitres finies à la main.",
    icon: '💧',
    img: imgFoamBlue,
    alt: 'Carrosserie sportive bleue recouverte de mousse active lors du lavage extérieur'
  },
  {
    tag: 'Intérieur',
    title: "Un habitacle qui respire le neuf.",
    body: 'Aspiration profonde, nettoyage des plastiques et traitement des tapis. On efface la poussière rouge pour ne laisser qu’une odeur de propre.',
    icon: '🧴',
    img: imgInterior,
    alt: 'Nettoyage détaillé du volant en cuir avec une microfibre'
  },
  {
    tag: 'Premium',
    title: 'Le polissage qui réveille la teinte.',
    body: "Traitement correcteur qui gomme les micro-rayures et ravive la profondeur de la peinture. Votre véhicule retrouve son éclat de première main.",
    icon: '🪩',
    img: imgWheel,
    alt: 'Jante et carrosserie brillantes sous le jet, finition premium'
  },
  {
    tag: 'Protection',
    title: 'Une brillance qui dure des semaines.',
    body: 'Cire et scellant hydrophobe : la pluie perle, la poussière adhère moins, chaque lavage suivant devient plus simple. La protection invisible qui change tout.',
    icon: '🛡️',
    img: imgExteriorFoam,
    alt: 'Voiture noire couverte de mousse, gouttes perlant sur la peinture protégée'
  }
]

/* ── Process steps ───────────────────────────────────────────────── */
const steps = [
  { n: '01', t: 'Accueil & diagnostic', d: 'On inspecte le véhicule et on recommande la formule juste.' },
  { n: '02', t: 'Traitement dédié', d: 'Produits pro, gestes maîtrisés, zéro rayure.' },
  { n: '03', t: 'Contrôle qualité', d: 'Double vérification avant la remise des clés.' },
  { n: '04', t: 'Brillance livrée', d: 'Vous repartez avec un véhicule qui en impose.' }
]

/* ── Animated stats ──────────────────────────────────────────────── */
const stats = [
  { key: 'cars', target: 12000, suffix: '+', label: 'Véhicules lavés' },
  { key: 'min',  target: 25,    suffix: 'min', label: 'Lavage express' },
  { key: 'note', target: 4.9,   suffix: '/5', label: 'Note clients', decimals: 1 },
  { key: 'day',  target: 7,     suffix: 'j/7', label: 'Ouvert' }
]
const counts = ref(Object.fromEntries(stats.map((s) => [s.key, 0])))
let statsAnimated = false

function animateStats() {
  if (statsAnimated) return
  statsAnimated = true
  const duration = 1600
  const start = performance.now()
  const tick = (now) => {
    const p = Math.min((now - start) / duration, 1)
    const eased = 1 - Math.pow(1 - p, 3)
    stats.forEach((s) => {
      const v = s.target * eased
      counts.value[s.key] = s.decimals ? v.toFixed(s.decimals) : Math.round(v)
    })
    if (p < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

function fmtStat(s) {
  const raw = counts.value[s.key]
  if (s.decimals) return raw
  return new Intl.NumberFormat('fr-FR').format(raw)
}

/* ── Scroll-driven chrome (progress bar + nav) ───────────────────── */
const scrollProgress = ref(0)
const navSolid = ref(false)
const heroParallax = ref(0)

function onScroll() {
  const doc = document.documentElement
  const max = doc.scrollHeight - doc.clientHeight
  const y = window.scrollY
  scrollProgress.value = max > 0 ? (y / max) * 100 : 0
  navSolid.value = y > window.innerHeight * 0.6
  heroParallax.value = Math.min(y * 0.4, 400)
}

let statsObserver = null
onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })

  const el = document.getElementById('stats-section')
  if (el) {
    statsObserver = new IntersectionObserver(
      (entries) => entries.forEach((e) => e.isIntersecting && animateStats()),
      { threshold: 0.35 }
    )
    statsObserver.observe(el)
  }
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  statsObserver?.disconnect()
})

function scrollTo(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<template>
  <div class="landing">
    <!-- Scroll progress -->
    <div class="progress-bar" :style="{ width: scrollProgress + '%' }" aria-hidden="true" />

    <!-- Navigation -->
    <header class="nav" :class="{ 'nav--solid': navSolid }">
      <div class="nav__inner">
        <a class="brand" href="#top" @click.prevent="scrollTo('top')">
          <span class="brand__mark">◈</span>
          <span class="brand__name">SKY<span class="brand__accent">CARWASH</span></span>
        </a>
        <nav class="nav__links">
          <button @click="scrollTo('services')">Services</button>
          <button @click="scrollTo('pricing')">Tarifs</button>
          <button @click="scrollTo('process')">Process</button>
          <button @click="scrollTo('contact')">Contact</button>
        </nav>
        <RouterLink to="/login" class="nav__cta">Espace pro →</RouterLink>
      </div>
    </header>

    <!-- ══ HERO ═══════════════════════════════════════════════════ -->
    <section id="top" class="hero">
      <div class="hero__bg" :style="{ transform: `translateY(${heroParallax * 0.5}px)` }">
        <div class="hero__photo" :style="{ backgroundImage: `url(${imgExteriorFoam})` }" />
        <div class="blob blob--1" />
        <div class="blob blob--2" />
        <div class="blob blob--3" />
        <div class="hero__grid" />
        <div class="hero__shine" />
      </div>

      <div class="hero__content" :style="{ transform: `translateY(${heroParallax}px)`, opacity: 1 - heroParallax / 500 }">
        <p class="hero__eyebrow" v-reveal>
          {{ BUSINESS.city }} · {{ BUSINESS.year }} — Propreté &amp; Brillance
        </p>
        <h1 class="hero__title">
          <span class="line" style="--i:0">Votre véhicule,</span>
          <span class="line line--accent" style="--i:1">comme au premier jour.</span>
        </h1>
        <p class="hero__sub" v-reveal:0.05>
          Le carwash premium de {{ BUSINESS.city }}. Lavage, soin et protection —
          exécutés avec la précision d'un atelier, la brillance d'un showroom.
        </p>
        <div class="hero__actions" v-reveal:0.05>
          <button class="btn btn--primary" @click="scrollTo('pricing')">
            Voir la grille tarifaire
          </button>
          <button class="btn btn--ghost" @click="scrollTo('services')">
            Découvrir l'expérience
          </button>
        </div>
      </div>

      <button class="hero__scroll" @click="scrollTo('manifesto')" aria-label="Défiler">
        <span>Défilez</span>
        <span class="hero__scroll-dot" />
      </button>
    </section>

    <!-- ══ MANIFESTO ══════════════════════════════════════════════ -->
    <section id="manifesto" class="manifesto">
      <p class="manifesto__lead" v-reveal.stagger>
        <span>Chaque véhicule</span>
        <span>mérite de <em>briller</em>.</span>
        <span class="manifesto__small">Pas seulement propre. Impeccable.</span>
      </p>
    </section>

    <!-- ══ SERVICES ═══════════════════════════════════════════════ -->
    <section id="services" class="services">
      <div class="section-head" v-reveal>
        <span class="section-kicker">Nos prestations</span>
        <h2 class="section-title">Quatre gestes. Un résultat qui se remarque.</h2>
      </div>

      <div
        class="service-panel"
        :class="{ 'service-panel--reverse': i % 2 === 1 }"
        v-for="(s, i) in services"
        :key="s.tag"
        v-reveal:0.15
      >
        <figure class="service-panel__media">
          <img :src="s.img" :alt="s.alt" loading="lazy" decoding="async" />
          <span class="service-panel__num">{{ String(i + 1).padStart(2, '0') }}</span>
          <span class="service-panel__chip">{{ s.icon }} {{ s.tag }}</span>
        </figure>
        <div class="service-panel__text">
          <span class="service-panel__tag">{{ s.tag }}</span>
          <h3 :class="['service-panel__title', `text-grad-${i}`]">{{ s.title }}</h3>
          <p class="service-panel__body">{{ s.body }}</p>
        </div>
      </div>
    </section>

    <!-- ══ PRICING ════════════════════════════════════════════════ -->
    <section id="pricing" class="pricing">
      <div class="section-head" v-reveal>
        <span class="section-kicker">Grille tarifaire</span>
        <h2 class="section-title">Des tarifs clairs. Une brillance garantie.</h2>
        <p class="section-note">Prix en FCFA · {{ BUSINESS.city }} {{ BUSINESS.year }}</p>
      </div>

      <!-- Vehicle selector -->
      <div class="vehicle-tabs" v-reveal>
        <button
          v-for="v in vehicles"
          :key="v.key"
          class="vehicle-tab"
          :class="{ 'vehicle-tab--active': activeVehicle === v.key }"
          @click="activeVehicle = v.key"
        >
          <span class="vehicle-tab__icon">{{ v.icon }}</span>
          <span>{{ v.label }}</span>
        </button>
      </div>

      <!-- Lavages -->
      <div class="price-block" v-reveal>
        <h3 class="price-block__title">Lavages</h3>
        <div class="price-grid">
          <div
            v-for="item in lavages"
            :key="item.name"
            class="price-card"
            :class="{ 'price-card--popular': item.popular }"
          >
            <span v-if="item.popular" class="price-card__badge">🌟 Populaire</span>
            <div class="price-card__icon">{{ item.icon }}</div>
            <h4 class="price-card__name">{{ item.name }}</h4>
            <p class="price-card__desc">{{ item.desc }}</p>
            <div class="price-card__price">
              <template v-if="priceFor(item)">
                <span class="amount">{{ priceFor(item) }}</span>
                <span class="cur">FCFA</span>
              </template>
              <span v-else class="price-card__na">Non applicable · {{ activeVehicleLabel }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Premium -->
      <div class="price-block price-block--premium" v-reveal>
        <h3 class="price-block__title price-block__title--gold">Soins Premium</h3>
        <div class="price-grid price-grid--2">
          <div v-for="item in premium" :key="item.name" class="price-card price-card--gold">
            <div class="price-card__icon">{{ item.icon }}</div>
            <h4 class="price-card__name">{{ item.name }}</h4>
            <p class="price-card__desc">{{ item.desc }}</p>
            <div class="price-card__price">
              <template v-if="priceFor(item)">
                <span class="amount amount--gold">{{ priceFor(item) }}</span>
                <span class="cur">FCFA</span>
              </template>
              <span v-else class="price-card__na">Réservé auto · 4x4 · pick-up</span>
            </div>
          </div>
        </div>
      </div>

      <p class="pricing__foot" v-reveal>
        Tarifs indicatifs pour véhicules standards. État très encrassé ou options
        supplémentaires sur devis. Abonnements &amp; flottes entreprise disponibles.
      </p>
    </section>

    <!-- ══ PROCESS ════════════════════════════════════════════════ -->
    <section id="process" class="process">
      <div class="section-head" v-reveal>
        <span class="section-kicker">Le déroulé</span>
        <h2 class="section-title">Quatre étapes, zéro compromis.</h2>
      </div>
      <div class="steps" v-reveal.stagger>
        <div v-for="s in steps" :key="s.n" class="step">
          <div class="step__n">{{ s.n }}</div>
          <h3 class="step__t">{{ s.t }}</h3>
          <p class="step__d">{{ s.d }}</p>
        </div>
      </div>
    </section>

    <!-- ══ STATS ══════════════════════════════════════════════════ -->
    <section id="stats-section" class="stats">
      <div class="stats__grid">
        <div v-for="s in stats" :key="s.key" class="stat">
          <div class="stat__num">{{ fmtStat(s) }}<span class="stat__suffix">{{ s.suffix }}</span></div>
          <div class="stat__label">{{ s.label }}</div>
        </div>
      </div>
    </section>

    <!-- ══ CTA / CONTACT ══════════════════════════════════════════ -->
    <section id="contact" class="cta">
      <div class="cta__glow" />
      <div class="cta__inner" v-reveal>
        <h2 class="cta__title">Prêt à faire briller votre véhicule ?</h2>
        <p class="cta__sub">
          Passez sans rendez-vous ou réservez votre créneau. On s'occupe du reste.
        </p>
        <div class="cta__actions">
          <a :href="`tel:${BUSINESS.phone.replace(/\s/g, '')}`" class="btn btn--primary">
            📞 {{ BUSINESS.phone }}
          </a>
          <button class="btn btn--ghost" @click="scrollTo('pricing')">Revoir les tarifs</button>
        </div>
        <div class="cta__meta">
          <div><span>📍</span>{{ BUSINESS.address }}</div>
          <div><span>🕑</span>{{ BUSINESS.hours }}</div>
        </div>
      </div>
    </section>

    <!-- ══ FOOTER ═════════════════════════════════════════════════ -->
    <footer class="foot">
      <div class="foot__brand">
        <span class="brand__mark">◈</span> SKY<span class="brand__accent">CARWASH</span>
      </div>
      <p class="foot__tag">Propreté &amp; Brillance — {{ BUSINESS.city }} · {{ BUSINESS.year }}</p>
      <RouterLink to="/login" class="foot__link">Accès équipe →</RouterLink>
      <p class="foot__copy">© {{ BUSINESS.year }} {{ BUSINESS.name }}. Tous droits réservés.</p>
    </footer>
  </div>
</template>

<style scoped>
/* ── Base ─────────────────────────────────────────────────────────── */
.landing {
  --bg: #05070d;
  --bg-2: #0a0f1c;
  --ink: #eef4ff;
  --muted: #8ea3c0;
  --sky: #38bdf8;
  --sky-deep: #0ea5e9;
  --gold: #f4c964;
  background: var(--bg);
  color: var(--ink);
  overflow-x: hidden;
  font-family: 'Inter', ui-sans-serif, system-ui, sans-serif;
  -webkit-font-smoothing: antialiased;
}
.landing :is(h1, h2, h3, h4) { letter-spacing: -0.03em; line-height: 1.05; }

/* Reveal directive base state */
:deep(.reveal) {
  opacity: 0;
  transform: translateY(38px);
  transition: opacity 0.9s cubic-bezier(0.16, 1, 0.3, 1),
              transform 0.9s cubic-bezier(0.16, 1, 0.3, 1);
  transition-delay: var(--reveal-delay, 0ms);
  will-change: opacity, transform;
}
:deep(.reveal.is-revealed) { opacity: 1; transform: none; }

/* ── Progress + nav ───────────────────────────────────────────────── */
.progress-bar {
  position: fixed; top: 0; left: 0; height: 3px; z-index: 60;
  background: linear-gradient(90deg, var(--sky), var(--gold));
  box-shadow: 0 0 12px rgba(56, 189, 248, 0.7);
  transition: width 0.1s linear;
}
.nav {
  position: fixed; top: 0; left: 0; right: 0; z-index: 50;
  transition: background 0.4s ease, backdrop-filter 0.4s ease, border-color 0.4s ease;
  border-bottom: 1px solid transparent;
}
.nav--solid {
  background: rgba(5, 7, 13, 0.72);
  backdrop-filter: blur(18px) saturate(140%);
  border-bottom-color: rgba(255, 255, 255, 0.07);
}
.nav__inner {
  max-width: 1200px; margin: 0 auto; padding: 16px 24px;
  display: flex; align-items: center; justify-content: space-between; gap: 24px;
}
.brand { display: flex; align-items: center; gap: 9px; font-weight: 800; letter-spacing: 0.02em; }
.brand__mark { color: var(--sky); font-size: 1.1rem; filter: drop-shadow(0 0 8px rgba(56,189,248,0.6)); }
.brand__name, .brand { color: var(--ink); }
.brand__accent { color: var(--sky); }
.nav__links { display: flex; gap: 6px; }
.nav__links button {
  color: var(--muted); font-size: 0.9rem; font-weight: 500;
  padding: 8px 14px; border-radius: 999px; transition: color 0.2s, background 0.2s;
}
.nav__links button:hover { color: var(--ink); background: rgba(255,255,255,0.06); }
.nav__cta {
  color: var(--ink); font-size: 0.88rem; font-weight: 600;
  padding: 9px 18px; border-radius: 999px;
  border: 1px solid rgba(56,189,248,0.4);
  background: rgba(56,189,248,0.08); transition: background 0.2s, transform 0.2s;
}
.nav__cta:hover { background: rgba(56,189,248,0.2); transform: translateY(-1px); }
@media (max-width: 760px) { .nav__links { display: none; } }

/* ── Buttons ──────────────────────────────────────────────────────── */
.btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  font-weight: 600; font-size: 1rem; padding: 15px 30px; border-radius: 999px;
  transition: transform 0.2s ease, box-shadow 0.3s ease, background 0.2s ease;
  cursor: pointer; white-space: nowrap;
}
.btn--primary {
  color: #04121f;
  background: linear-gradient(135deg, #7dd3fc, var(--sky-deep));
  box-shadow: 0 10px 40px -8px rgba(14, 165, 233, 0.65);
}
.btn--primary:hover { transform: translateY(-3px); box-shadow: 0 18px 55px -10px rgba(14,165,233,0.8); }
.btn--ghost {
  color: var(--ink); border: 1px solid rgba(255,255,255,0.18);
  background: rgba(255,255,255,0.03);
}
.btn--ghost:hover { background: rgba(255,255,255,0.09); transform: translateY(-3px); }

/* ── Hero ─────────────────────────────────────────────────────────── */
.hero {
  position: relative; min-height: 100vh;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  text-align: center; padding: 120px 24px 80px; overflow: hidden;
}
.hero__bg { position: absolute; inset: -10% -10% 0; z-index: 0; }
.hero__photo {
  position: absolute; inset: 0;
  background-size: cover; background-position: center 35%;
  opacity: 0.42;
  /* Fade the photo out toward the edges/bottom so the headline stays legible */
  -webkit-mask-image: radial-gradient(ellipse 90% 85% at 60% 42%, #000 25%, transparent 78%);
  mask-image: radial-gradient(ellipse 90% 85% at 60% 42%, #000 25%, transparent 78%);
  filter: saturate(112%) contrast(104%);
}
.blob { position: absolute; border-radius: 50%; filter: blur(90px); opacity: 0.55; animation: drift 16s ease-in-out infinite; }
.blob--1 { width: 46vw; height: 46vw; top: -6%; left: -4%; background: radial-gradient(circle, #0ea5e9, transparent 70%); }
.blob--2 { width: 40vw; height: 40vw; bottom: -12%; right: -6%; background: radial-gradient(circle, #22d3ee, transparent 70%); animation-delay: -5s; }
.blob--3 { width: 34vw; height: 34vw; top: 30%; left: 42%; background: radial-gradient(circle, #6366f1, transparent 70%); opacity: 0.4; animation-delay: -9s; }
@keyframes drift {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(4%, -5%) scale(1.08); }
  66% { transform: translate(-4%, 4%) scale(0.95); }
}
.hero__grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.035) 1px, transparent 1px);
  background-size: 64px 64px;
  mask-image: radial-gradient(ellipse 70% 60% at 50% 40%, #000 30%, transparent 75%);
}
.hero__shine {
  position: absolute; inset: 0;
  background: linear-gradient(115deg, transparent 40%, rgba(255,255,255,0.06) 50%, transparent 60%);
  background-size: 300% 300%; animation: shine 9s linear infinite;
}
@keyframes shine { 0% { background-position: 120% 0; } 100% { background-position: -120% 0; } }
.hero::after {
  content: ''; position: absolute; inset: 0; z-index: 1; pointer-events: none;
  background:
    radial-gradient(ellipse 62% 55% at 50% 42%, transparent 38%, rgba(5,7,13,0.5) 100%),
    linear-gradient(to bottom, rgba(5,7,13,0.35) 0%, transparent 28%, transparent 55%, var(--bg) 98%);
}

.hero__content { position: relative; z-index: 2; max-width: 900px; }
.hero__eyebrow {
  display: inline-block; font-size: 0.82rem; font-weight: 600; letter-spacing: 0.28em;
  text-transform: uppercase; color: var(--sky);
  padding: 8px 18px; border-radius: 999px; margin-bottom: 28px;
  border: 1px solid rgba(56,189,248,0.28); background: rgba(56,189,248,0.06);
}
.hero__title {
  font-size: clamp(2.8rem, 8vw, 6rem); font-weight: 800; margin: 0 0 26px;
}
.hero__title .line {
  display: block; opacity: 0; transform: translateY(40px);
  animation: heroRise 1s cubic-bezier(0.16,1,0.3,1) forwards;
  animation-delay: calc(0.15s + var(--i) * 0.14s);
}
.line--accent {
  background: linear-gradient(120deg, #7dd3fc 10%, #38bdf8 40%, #818cf8 90%);
  -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
}
@keyframes heroRise { to { opacity: 1; transform: none; } }
.hero__sub {
  font-size: clamp(1.05rem, 2.2vw, 1.35rem); color: var(--muted);
  max-width: 620px; margin: 0 auto 40px; line-height: 1.6;
}
.hero__actions { display: flex; gap: 16px; justify-content: center; flex-wrap: wrap; }

.hero__scroll {
  position: absolute; bottom: 30px; left: 50%; transform: translateX(-50%); z-index: 3;
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  color: var(--muted); font-size: 0.72rem; letter-spacing: 0.2em; text-transform: uppercase;
}
.hero__scroll-dot {
  width: 22px; height: 36px; border: 1.5px solid rgba(255,255,255,0.25); border-radius: 999px;
  position: relative;
}
.hero__scroll-dot::after {
  content: ''; position: absolute; top: 7px; left: 50%; transform: translateX(-50%);
  width: 4px; height: 8px; border-radius: 999px; background: var(--sky);
  animation: scrollDot 1.6s ease-in-out infinite;
}
@keyframes scrollDot { 0%,100% { opacity: 0; transform: translate(-50%, 0); } 50% { opacity: 1; transform: translate(-50%, 12px); } }

/* ── Manifesto ────────────────────────────────────────────────────── */
.manifesto {
  min-height: 82vh; display: flex; align-items: center; justify-content: center;
  padding: 100px 24px; text-align: center;
  background: radial-gradient(ellipse at center, rgba(14,165,233,0.08), transparent 65%);
}
.manifesto__lead {
  font-size: clamp(2rem, 6.5vw, 4.6rem); font-weight: 800; line-height: 1.06;
  max-width: 950px; letter-spacing: -0.03em;
}
.manifesto__lead span { display: block; }
.manifesto__lead em {
  font-style: normal;
  background: linear-gradient(120deg, #7dd3fc, #38bdf8 55%, #a78bfa);
  -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
}
.manifesto__small {
  font-size: clamp(1rem, 2.4vw, 1.4rem); font-weight: 500; color: var(--muted);
  margin-top: 26px; letter-spacing: 0;
}

/* ── Section heads ────────────────────────────────────────────────── */
.section-head { max-width: 780px; margin: 0 auto 64px; text-align: center; }
.section-kicker {
  display: inline-block; font-size: 0.78rem; font-weight: 700; letter-spacing: 0.24em;
  text-transform: uppercase; color: var(--sky); margin-bottom: 18px;
}
.section-title { font-size: clamp(1.9rem, 4.6vw, 3.2rem); font-weight: 800; margin: 0; }
.section-note { color: var(--muted); margin-top: 16px; font-size: 0.95rem; }

/* ── Services (image-driven, alternating) ─────────────────────────── */
.services { max-width: 1140px; margin: 0 auto; padding: 110px 24px; }
.service-panel {
  display: grid; grid-template-columns: 1fr 1fr; align-items: center; gap: 56px;
  padding: 46px 0;
}
.service-panel--reverse .service-panel__media { order: 2; }

.service-panel__media {
  position: relative; aspect-ratio: 4 / 3; border-radius: 26px; overflow: hidden;
  box-shadow: 0 34px 80px -30px rgba(0,0,0,0.85), inset 0 0 0 1px rgba(255,255,255,0.08);
}
.service-panel__media img {
  width: 100%; height: 100%; object-fit: cover; display: block;
  transition: transform 0.9s cubic-bezier(0.16, 1, 0.3, 1);
}
.service-panel__media::after {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(200deg, transparent 45%, rgba(5,7,13,0.6) 100%);
}
.service-panel:hover .service-panel__media img { transform: scale(1.06); }
.service-panel__num {
  position: absolute; top: 16px; left: 20px; z-index: 1;
  font-size: 2.4rem; font-weight: 800; color: rgba(255,255,255,0.9);
  text-shadow: 0 2px 20px rgba(0,0,0,0.6);
}
.service-panel__chip {
  position: absolute; bottom: 16px; left: 20px; z-index: 1;
  font-size: 0.78rem; font-weight: 700; letter-spacing: 0.02em; color: var(--ink);
  padding: 7px 14px; border-radius: 999px;
  background: rgba(5,7,13,0.5); backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.16);
}
.service-panel__tag {
  display: inline-block; font-size: 0.72rem; font-weight: 700; letter-spacing: 0.2em;
  text-transform: uppercase; color: var(--muted); margin-bottom: 14px;
}
.service-panel__title { font-size: clamp(1.5rem, 3vw, 2.3rem); font-weight: 800; margin: 0 0 16px; }
.text-grad-0, .text-grad-1, .text-grad-2, .text-grad-3 {
  -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
}
.text-grad-0 { background-image: linear-gradient(120deg, #bae6fd, #38bdf8); }
.text-grad-1 { background-image: linear-gradient(120deg, #bbf7d0, #34d399); }
.text-grad-2 { background-image: linear-gradient(120deg, #fde68a, #f4c964); }
.text-grad-3 { background-image: linear-gradient(120deg, #f5d0fe, #a78bfa); }
.service-panel__body { color: var(--muted); font-size: 1.08rem; line-height: 1.7; max-width: 520px; }
@media (max-width: 760px) {
  .service-panel { grid-template-columns: 1fr; gap: 24px; padding: 34px 0; }
  .service-panel--reverse .service-panel__media { order: 0; }
  .service-panel__media { aspect-ratio: 16 / 10; }
}

/* ── Pricing ──────────────────────────────────────────────────────── */
.pricing {
  max-width: 1120px; margin: 0 auto; padding: 100px 24px;
  background:
    radial-gradient(ellipse 80% 50% at 50% 0%, rgba(14,165,233,0.08), transparent 70%);
}
.vehicle-tabs {
  display: flex; gap: 8px; justify-content: center; flex-wrap: wrap;
  margin: 0 auto 56px; padding: 8px; max-width: fit-content;
  background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08);
  border-radius: 999px;
}
.vehicle-tab {
  display: flex; align-items: center; gap: 9px; padding: 12px 22px; border-radius: 999px;
  color: var(--muted); font-weight: 600; font-size: 0.95rem;
  transition: color 0.2s, background 0.2s, transform 0.2s;
}
.vehicle-tab__icon { font-size: 1.15rem; }
.vehicle-tab:hover { color: var(--ink); }
.vehicle-tab--active {
  color: #04121f; background: linear-gradient(135deg, #7dd3fc, var(--sky-deep));
  box-shadow: 0 8px 24px -8px rgba(14,165,233,0.7);
}

.price-block { margin-bottom: 56px; }
.price-block__title { font-size: 1.5rem; font-weight: 800; margin: 0 0 24px; }
.price-block__title--gold {
  background: linear-gradient(120deg, #fde68a, var(--gold));
  -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
}
.price-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px;
}
.price-grid--2 { grid-template-columns: repeat(2, 1fr); max-width: 720px; }
@media (max-width: 900px) { .price-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 520px) { .price-grid, .price-grid--2 { grid-template-columns: 1fr; } }

.price-card {
  position: relative; padding: 26px 22px; border-radius: 22px;
  background: linear-gradient(180deg, rgba(255,255,255,0.05), rgba(255,255,255,0.02));
  border: 1px solid rgba(255,255,255,0.08);
  transition: transform 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;
}
.price-card:hover {
  transform: translateY(-6px);
  border-color: rgba(56,189,248,0.4);
  box-shadow: 0 24px 60px -24px rgba(14,165,233,0.5);
}
.price-card--popular {
  border-color: rgba(56,189,248,0.55);
  background: linear-gradient(180deg, rgba(56,189,248,0.14), rgba(56,189,248,0.03));
  box-shadow: 0 20px 60px -26px rgba(14,165,233,0.7);
}
.price-card--gold:hover { border-color: rgba(244,201,100,0.5); box-shadow: 0 24px 60px -24px rgba(244,201,100,0.35); }
.price-card__badge {
  position: absolute; top: -12px; left: 22px; font-size: 0.72rem; font-weight: 700;
  padding: 5px 12px; border-radius: 999px; color: #04121f;
  background: linear-gradient(135deg, #7dd3fc, var(--sky-deep));
}
.price-card__icon { font-size: 1.8rem; margin-bottom: 14px; }
.price-card__name { font-size: 1.1rem; font-weight: 700; margin: 0 0 6px; }
.price-card__desc { color: var(--muted); font-size: 0.85rem; line-height: 1.45; margin: 0 0 20px; min-height: 2.5em; }
.price-card__price { display: flex; align-items: baseline; gap: 6px; }
.amount { font-size: 1.7rem; font-weight: 800; color: var(--ink); }
.amount--gold { color: var(--gold); }
.cur { font-size: 0.8rem; font-weight: 600; color: var(--muted); }
.price-card__na { font-size: 0.82rem; color: rgba(142,163,192,0.6); font-style: italic; }

.pricing__foot {
  text-align: center; color: var(--muted); font-size: 0.9rem; line-height: 1.6;
  max-width: 640px; margin: 40px auto 0;
}

/* ── Process ──────────────────────────────────────────────────────── */
.process { max-width: 1100px; margin: 0 auto; padding: 100px 24px; }
.steps { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
@media (max-width: 860px) { .steps { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 460px) { .steps { grid-template-columns: 1fr; } }
.step {
  padding: 30px 24px; border-radius: 22px;
  background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.07);
  transition: transform 0.3s, border-color 0.3s;
}
.step:hover { transform: translateY(-5px); border-color: rgba(56,189,248,0.35); }
.step__n {
  font-size: 2rem; font-weight: 800; margin-bottom: 16px;
  background: linear-gradient(120deg, #7dd3fc, #818cf8);
  -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
}
.step__t { font-size: 1.1rem; font-weight: 700; margin: 0 0 10px; }
.step__d { color: var(--muted); font-size: 0.92rem; line-height: 1.55; }

/* ── Stats ────────────────────────────────────────────────────────── */
.stats {
  padding: 90px 24px; margin: 40px auto; max-width: 1100px;
  border-top: 1px solid rgba(255,255,255,0.08);
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
.stats__grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; text-align: center; }
@media (max-width: 640px) { .stats__grid { grid-template-columns: repeat(2, 1fr); gap: 40px 20px; } }
.stat__num {
  font-size: clamp(2.2rem, 5vw, 3.4rem); font-weight: 800; letter-spacing: -0.04em;
  background: linear-gradient(120deg, #eef4ff, #7dd3fc);
  -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
}
.stat__suffix { font-size: 0.5em; margin-left: 3px; -webkit-text-fill-color: var(--sky); }
.stat__label { color: var(--muted); font-size: 0.9rem; margin-top: 10px; }

/* ── CTA ──────────────────────────────────────────────────────────── */
.cta {
  position: relative; padding: 130px 24px; text-align: center; overflow: hidden;
}
.cta__glow {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  width: 700px; height: 700px; max-width: 120vw;
  background: radial-gradient(circle, rgba(14,165,233,0.22), transparent 65%);
  filter: blur(30px);
}
.cta__inner { position: relative; z-index: 1; max-width: 720px; margin: 0 auto; }
.cta__title { font-size: clamp(2rem, 5.5vw, 3.6rem); font-weight: 800; margin: 0 0 20px; }
.cta__sub { color: var(--muted); font-size: 1.15rem; line-height: 1.6; margin: 0 0 38px; }
.cta__actions { display: flex; gap: 16px; justify-content: center; flex-wrap: wrap; margin-bottom: 44px; }
.cta__meta {
  display: flex; gap: 32px; justify-content: center; flex-wrap: wrap;
  color: var(--muted); font-size: 0.95rem;
}
.cta__meta div { display: flex; align-items: center; gap: 9px; }

/* ── Footer ───────────────────────────────────────────────────────── */
.foot {
  text-align: center; padding: 60px 24px 80px; border-top: 1px solid rgba(255,255,255,0.07);
}
.foot__brand { font-weight: 800; font-size: 1.2rem; letter-spacing: 0.02em; margin-bottom: 12px; }
.foot__tag { color: var(--muted); font-size: 0.92rem; margin-bottom: 20px; }
.foot__link {
  display: inline-block; color: var(--sky); font-weight: 600; font-size: 0.92rem; margin-bottom: 26px;
}
.foot__link:hover { text-decoration: underline; }
.foot__copy { color: rgba(142,163,192,0.55); font-size: 0.82rem; }

/* ── Reduced motion ───────────────────────────────────────────────── */
@media (prefers-reduced-motion: reduce) {
  .blob, .hero__shine, .hero__scroll-dot::after { animation: none !important; }
  .hero__title .line { animation: none; opacity: 1; transform: none; }
}
</style>
