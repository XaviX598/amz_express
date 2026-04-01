<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { navScrolled } from '@/composables/useNavScroll'

// Particles animation
const particles = ref<{ id: number; left: string; delay: string; duration: string }[]>([])
let particleId = 0

function createParticles() {
  const newParticles = []
  for (let i = 0; i < 60; i++) {
    newParticles.push({
      id: particleId++,
      left: `${Math.random() * 100}%`,
      bottom: '-20px',
      delay: `${Math.random() * 15}s`,
      duration: `${8 + Math.random() * 10}s`,
    })
  }
  particles.value = newParticles
}

// ---------------------------------------------
// Hero entrance + scroll-driven exit animation
// ---------------------------------------------
const heroScrollProgress = ref(0) // 0 = at top, 1 = past hero

// Refs for hero elements
const heroBadge = ref<HTMLElement | null>(null)
const heroHeadline = ref<HTMLElement | null>(null)
const heroSubtitle = ref<HTMLElement | null>(null)
const heroCTAs = ref<HTMLElement | null>(null)
const heroCard = ref<HTMLElement | null>(null)

// Features section refs
const featuresSection = ref<HTMLElement | null>(null)
const featuresHeader = ref<HTMLElement | null>(null)
const featureCards = ref<(HTMLElement | null)[]>([])

// Testimonials section refs
const testimonialsSection = ref<HTMLElement | null>(null)
const testimonialsHeader = ref<HTMLElement | null>(null)

// How it works section refs
const stepsSection = ref<HTMLElement | null>(null)
const stepsHeader = ref<HTMLElement | null>(null)
const stepCards = ref<(HTMLElement | null)[]>([])

// Delays for staggered entrance (ms)
const ENTRANCE_DELAYS = { badge: 0, headline: 150, subtitle: 300, ctas: 450, card: 200 }
const ENTRANCE_DURATION = 800

function applyHeroStyles() {
  const scroll = heroScrollProgress.value
  
  // After entrance completes, scroll-driven exit takes over
  const exitProgress = Math.max(0, (scroll - 0.3) / 0.7) // starts exiting after 30% scroll
  
  // Left-side elements
  const leftTransform = -120 * exitProgress
  const leftOpacity = Math.max(0, 1 - exitProgress * 1.5)
  
  if (heroBadge.value) {
    heroBadge.value.style.opacity = String(leftOpacity)
    heroBadge.value.style.transform = `translateX(${leftTransform}px)`
  }
  if (heroHeadline.value) {
    heroHeadline.value.style.opacity = String(leftOpacity)
    heroHeadline.value.style.transform = `translateX(${leftTransform}px)`
  }
  if (heroSubtitle.value) {
    heroSubtitle.value.style.opacity = String(leftOpacity)
    heroSubtitle.value.style.transform = `translateX(${leftTransform}px)`
  }
  if (heroCTAs.value) {
    heroCTAs.value.style.opacity = String(leftOpacity)
    heroCTAs.value.style.transform = `translateX(${leftTransform}px)`
  }
  
  // Right-side element (card)
  const rightTransform = 120 * exitProgress
  const rightOpacity = Math.max(0, 1 - exitProgress * 1.5)
  if (heroCard.value) {
    heroCard.value.style.opacity = String(rightOpacity)
    heroCard.value.style.transform = `translateX(${rightTransform}px)`
  }
}

// ---------------------------------------------
// Features & Testimonials scroll-driven animation
// ---------------------------------------------

function applySectionAnimation(section: HTMLElement | null, elements: (HTMLElement | null)[], exitDirection?: 'down' | 'alternate') {
  if (!section || !elements.length) return
  
  const rect = section.getBoundingClientRect()
  const vh = window.innerHeight
  const sectionTop = rect.top
  const sectionHeight = rect.height
  
  const progress = Math.max(0, Math.min(1, (vh - sectionTop) / (vh + sectionHeight)))
  
  const ease = (t: number) => 1 - Math.pow(1 - t, 3)
  const slideRange = 100
  
  elements.forEach((el, i) => {
    if (!el) return
    
    // Stagger for entrance and exit
    const enterStart = 0.20 + i * 0.05
    const exitStart = 0.50 + i * 0.03 // first element exits first
    
    // Calculate entrance progress
    const enterProgress = Math.max(0, Math.min(1, (progress - enterStart) / 0.25))
    const enterOpacity = ease(enterProgress)
    const enterY = (1 - enterOpacity) * slideRange
    
    // Calculate exit progress (staggered - first element exits first)
    const exitProgress = Math.max(0, Math.min(1, (progress - exitStart) / 0.25))
    
    // Direction: alternate (1,3 down / 2,4 up) or all down (default)
    let exitY: number
    if (exitDirection === 'alternate') {
      // Header goes up, cards alternate: 1 down, 2 up, 3 down, 4 up
      const cardIndex = i - 1 // 0-based for cards (header is -1)
      if (cardIndex < 0) {
        exitY = -ease(exitProgress) * slideRange // header goes up
      } else {
        exitY = cardIndex % 2 === 0 
          ? ease(exitProgress) * slideRange // odd cards go down
          : -ease(exitProgress) * slideRange // even cards go up
      }
    } else {
      exitY = ease(exitProgress) * slideRange // all go down
    }
    
    // Opacity fades slower so movement is visible
    const exitOpacity = 1 - Math.pow(exitProgress, 0.5) // fades slower than movement
    
    // Combine: entrance active first, exit active after 0.50
    const opacity = progress < exitStart ? enterOpacity : exitOpacity
    const y = progress < exitStart ? enterY : exitY
    
    el.style.opacity = String(opacity)
    el.style.transform = `translateY(${y}px)`
  })
}


let entranceRafId: number | null = null
let entranceStartTime: number | null = null
const entranceComplete = ref(false)

function runEntranceAnimation(timestamp: number) {
  if (entranceStartTime === null) entranceStartTime = timestamp
  
  const elapsed = timestamp - entranceStartTime
  
  // Calculate progress for each element based on its delay
  const progress: Record<string, number> = {}
  for (const [key, delay] of Object.entries(ENTRANCE_DELAYS)) {
    const elementProgress = Math.min(1, Math.max(0, (elapsed - delay) / ENTRANCE_DURATION))
    progress[key] = easeOutCubic(elementProgress)
  }
  
  // Apply entrance styles without positional offset to avoid initial layout jump
  const stableTransform = 'translateX(0px)'
  
  if (heroBadge.value) {
    heroBadge.value.style.opacity = String(progress.badge)
    heroBadge.value.style.transform = stableTransform
  }
  if (heroHeadline.value) {
    heroHeadline.value.style.opacity = String(progress.headline)
    heroHeadline.value.style.transform = stableTransform
  }
  if (heroSubtitle.value) {
    heroSubtitle.value.style.opacity = String(progress.subtitle)
    heroSubtitle.value.style.transform = stableTransform
  }
  if (heroCTAs.value) {
    heroCTAs.value.style.opacity = String(progress.ctas)
    heroCTAs.value.style.transform = stableTransform
  }
  if (heroCard.value) {
    heroCard.value.style.opacity = String(progress.card)
    heroCard.value.style.transform = stableTransform
  }
  
  // Check if animation is complete
  const maxDelay = Math.max(...Object.values(ENTRANCE_DELAYS))
  if (elapsed < maxDelay + ENTRANCE_DURATION) {
    entranceRafId = requestAnimationFrame(runEntranceAnimation)
  } else {
    entranceRafId = null
    entranceStartTime = null
    entranceComplete.value = true
  }
}

function easeOutCubic(t: number): number {
  return 1 - Math.pow(1 - t, 3)
}

// ---------------------------------------------
// Scroll-triggered visibility (features, CTA, steps header)
// ---------------------------------------------
const hoveredCard = ref<string | null>(null)
const isVisible = reactive<Record<string, boolean>>({})
let observers: IntersectionObserver[] = []

function setupIntersectionObserver() {
  const targets = document.querySelectorAll('[data-animate]')
  
  targets.forEach((el) => {
    const id = el.id
    if (!id) return
    isVisible[id] = false
    
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          isVisible[id] = entry.isIntersecting
        })
      },
      { threshold: 0.15 }
    )
    observer.observe(el)
    observers.push(observer)
  })
}

let lastScrollY = 0

function onScroll() {
  const currentScrollY = window.scrollY
  
  // Hero scroll-driven animation
  const heroHeight = window.innerHeight
  heroScrollProgress.value = Math.min(window.scrollY / heroHeight, 1)
  if (entranceComplete.value && heroScrollProgress.value > 0.3) {
    applyHeroStyles()
  }
  
  // Features & Testimonials scroll-driven animation
  if (featuresSection.value && featureCards.value.length) {
    const allFeatures = [featuresHeader.value, ...featureCards.value]
    applySectionAnimation(featuresSection.value, allFeatures, 'alternate')
  }
  if (testimonialsSection.value && testimonialsHeader.value) {
    applySectionAnimation(testimonialsSection.value, [testimonialsHeader.value])
  }
  if (stepsSection.value && stepsHeader.value && stepCards.value.length) {
    const allSteps = [stepsHeader.value, ...stepCards.value]
    applySectionAnimation(stepsSection.value, allSteps)
  }
}

onMounted(() => {
  entranceComplete.value = false
  // Initialize hero elements at starting position (invisible, displaced)
  const initStyle = (el: HTMLElement | null, x: number) => {
    if (el) {
      el.style.opacity = '0'
      el.style.transform = `translateX(${x}px)`
    }
  }
  initStyle(heroBadge.value, 0)
  initStyle(heroHeadline.value, 0)
  initStyle(heroSubtitle.value, 0)
  initStyle(heroCTAs.value, 0)
  initStyle(heroCard.value, 0)
  
  // Start entrance animation immediately to avoid first-paint flash
  entranceStartTime = null
  entranceRafId = requestAnimationFrame(runEntranceAnimation)
  
  createParticles()
  setupIntersectionObserver()
  lastScrollY = window.scrollY
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  observers.forEach((obs) => obs.disconnect())
  observers = []
  if (entranceRafId !== null) cancelAnimationFrame(entranceRafId)
})

const features = [
  {
    title: 'Precios Transparentes',
    description: 'Sin costos ocultos. Sabes exactamente cuánto pagarás antes de confirmar.',
    icon: 'M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z',
    color: 'teal',
  },
  {
    title: 'Envío Seguro',
    description: 'Tu producto viaja asegurado desde Estados Unidos hasta tu puerta.',
    icon: 'M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z',
    color: 'blue',
  },
  {
    title: 'Tracking en Tiempo Real',
    description: 'Seguimiento paso a paso de tu pedido desde que lo compras.',
    icon: 'M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z M15 11a3 3 0 11-6 0 3 3 0 016 0z',
    color: 'purple',
  },
  {
    title: 'Atención Personalizada',
    description: 'WhatsApp directo para resolver cualquier duda o consulta.',
    icon: 'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z',
    color: 'orange',
  },
]

const steps = [
  {
    number: '01',
    title: 'Encuentra un producto',
    description: 'Busca en Amazon el producto que quieres importar.',
  },
  {
    number: '02',
    title: 'Ingresa los datos',
    description: 'Ingresa el precio y el peso del producto en nuestra calculadora.',
  },
  {
    number: '03',
    title: 'Confirma tu pedido',
    description: 'Una vez aprobado el precio, nosotros lo compramos por ti.',
  },
  {
    number: '04',
    title: 'Recibe en casa',
    description: 'Te notificamos cuando llegue y coordinamos la entrega.',
  },
]

const testimonials = [
  {
    name: 'maria fernandez',
    initials: 'mf',
    location: 'guayaquil',
    rating: 5,
    text: 'Importé un macbook pro desde usa y llegó en 12 días a mi oficina. me mandaban tracking en cada paso, ni un detalle fuera de lugar.',
    avatarBg: 'bg-gradient-to-br from-teal-500 to-teal-600',
    accentColor: 'bg-teal-500',
    amazonBadge: 'text-teal-400/80 border-teal-500/20 bg-teal-500/10',
  },
  {
    name: 'carlos mendez',
    initials: 'cm',
    location: 'quito',
    rating: 5,
    text: 'pensé que la aduana iba a ser un problema pero me lo explican todo antes de confirmar. terminé pagando menos de lo que había calculado.',
    avatarBg: 'bg-gradient-to-br from-teal-400 to-teal-600',
    accentColor: 'bg-teal-500',
    amazonBadge: 'text-teal-400/80 border-teal-500/20 bg-teal-500/10',
  },
  {
    name: 'valentina solis',
    initials: 'vs',
    location: 'cuenca',
    rating: 5,
    text: 'le compré una dyson a mi mama y llegó muy bien embalada. el tracking en tiempo real te mantiene tranquila. ya hice otro pedido.',
    avatarBg: 'bg-gradient-to-br from-teal-500 to-emerald-600',
    accentColor: 'bg-teal-500',
    amazonBadge: 'text-teal-400/80 border-teal-500/20 bg-teal-500/10',
  },
  {
    name: 'andres reyes',
    initials: 'ar',
    location: 'ambato',
    rating: 5,
    text: 'Soy fotógrafo y necesitaba un objetivo sony. vino asegurado y llegó sin un rasguño. el cargo por manejo es muy bueno comparado con otros.',
    avatarBg: 'bg-gradient-to-br from-teal-600 to-teal-700',
    accentColor: 'bg-teal-500',
    amazonBadge: 'text-teal-400/80 border-teal-500/20 bg-teal-500/10',
  },
  {
    name: 'daniela torres',
    initials: 'dt',
    location: 'manta',
    rating: 4,
    text: 'pedí ropa y accesorios. tardó 15 días porque eran fiestas, pero me mantenían al tanto de todo. volvería a pedir sin problemas.',
    avatarBg: 'bg-gradient-to-br from-teal-400 to-teal-500',
    accentColor: 'bg-teal-500',
    amazonBadge: 'text-teal-400/80 border-teal-500/20 bg-teal-500/10',
  },
  {
    name: 'sebastian ochoa',
    initials: 'so',
    location: 'loja',
    rating: 5,
    text: 'importé piezas de electrónica para mi emprendimiento. me ayudaron con la declaración y todo entró bien por aduana. muy profesionales.',
    avatarBg: 'bg-gradient-to-br from-teal-500 to-teal-600',
    accentColor: 'bg-teal-500',
    amazonBadge: 'text-teal-400/80 border-teal-500/20 bg-teal-500/10',
  },
  {
    name: 'gabriela gutierrez',
    initials: 'gg',
    location: 'esmeraldas',
    rating: 5,
    text: 'me compré unos audífonos bose que en Ecuador salen el doble. tardó como 10 días, llegó en perfecto estado.',
    avatarBg: 'bg-gradient-to-br from-teal-600 to-teal-700',
    accentColor: 'bg-teal-500',
    amazonBadge: 'text-teal-400/80 border-teal-500/20 bg-teal-500/10',
  },
  {
    name: 'pablo villalba',
    initials: 'pv',
    location: 'tulcán',
    rating: 5,
    text: 'mandé a traer un drone dji. venía con seguro y todo. no me preocupé, me llegó a la puerta de mi casa. la atención por whatsapp es directa y clara.',
    avatarBg: 'bg-gradient-to-br from-teal-500 to-teal-600',
    accentColor: 'bg-teal-500',
    amazonBadge: 'text-teal-400/80 border-teal-500/20 bg-teal-500/10',
  },
]
</script>

<template>
  <div class="relative pb-16">
    <!-- Hero Section — navbar lives inside so it blends with hero -->
    <section class="relative min-h-screen flex items-center overflow-hidden">
      <!-- Warehouse Background Image -->
      <div 
        class="absolute inset-0 w-full h-full z-[0] bg-cover bg-center bg-no-repeat"
        style="background-image: url('https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=1920&q=80');"
      />
      <!-- Dark Overlay -->
      <div class="absolute inset-0 w-full h-full z-[1]" style="background-color: rgba(10, 10, 10, 0.97);" />
      <!-- Particles -->
      <div class="particles-container absolute inset-0 w-full h-full z-[2] pointer-events-none overflow-hidden">
        <div
          v-for="particle in particles"
          :key="particle.id"
          class="particle"
          :style="{
            left: particle.left,
            bottom: particle.bottom,
            animationDelay: particle.delay,
            animationDuration: particle.duration,
          }"
        />
      </div>

      <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full z-20">
        <div class="grid lg:grid-cols-2 gap-12 lg:gap-20 items-center">
          <!-- Left Side - Text -->
          <div>
            <span ref="heroBadge" class="badge inline-flex items-center px-4 py-1.5 text-teal-400 rounded-full text-sm font-display tracking-widest mb-6 border border-teal-500/20 uppercase badge-pulse">
              <span class="w-2 h-2 bg-teal-400 rounded-full mr-2" />
              Importación simplificada
            </span>
            <h1 ref="heroHeadline" class="headline font-display text-5xl sm:text-6xl lg:text-7xl xl:text-8xl mb-6 leading-none tracking-wide text-white">
              Tu tienda 
              <span class="gradient-text">Amazon</span> 
              en Ecuador
            </h1>
            <p ref="heroSubtitle" class="subtitle text-lg sm:text-xl text-zinc-300 mb-10 leading-relaxed max-w-xl font-display tracking-wide">
              Importa productos de Estados Unidos sin complicaciones. 
              Precios claros, envío seguro y tracking en tiempo real.
            </p>
            <div ref="heroCTAs" class="ctas flex flex-wrap gap-4">
              <RouterLink 
                to="/calculadora" 
                class="btn-primary font-display text-base px-6 py-3 rounded-xl inline-flex items-center group tracking-wider"
              >
                Cotiza tu pedido
                <svg class="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3" />
                </svg>
              </RouterLink>
              <RouterLink 
                to="/register" 
                class="btn-outline font-display text-base px-6 py-3 rounded-xl tracking-wider"
              >
                Crear Cuenta
              </RouterLink>
            </div>
          </div>

          <!-- Right Side - Calculation Example -->
          <div class="hidden lg:block">
            <div class="relative">
              <div class="absolute -inset-4 bg-gradient-to-r from-teal-500/20 to-teal-600/10 rounded-3xl blur-2xl" />
              <div ref="heroCard" class="card relative glass rounded-2xl p-8 border border-white/10">
                <div class="flex items-center space-x-4 mb-6">
                  <div class="w-14 h-14 bg-gradient-to-br from-teal-500 to-teal-600 rounded-xl flex items-center justify-center shadow-lg shadow-teal-500/30">
                    <svg class="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                  <div>
                    <p class="text-zinc-500 text-sm font-display tracking-wider uppercase">Ejemplo de cálculo</p>
                    <p class="text-2xl font-display tracking-wider text-white">$156.50 USD</p>
                  </div>
                </div>
                <div class="space-y-3 text-sm">
                  <div class="flex justify-between text-zinc-400">
                    <span class="font-display tracking-wide">Producto</span>
                    <span class="text-white font-display tracking-wider">$100.00</span>
                  </div>
                  <div class="flex justify-between text-zinc-400">
                    <span class="font-display tracking-wide">Impuestos (15%)</span>
                    <span class="text-white font-display tracking-wider">$15.00</span>
                  </div>
                  <div class="flex justify-between text-zinc-400">
                    <span class="font-display tracking-wide">Cargo por manejo (9.27%)</span>
                    <span class="text-white font-display tracking-wider">$9.27</span>
                  </div>
                  <div class="flex justify-between text-zinc-400">
                    <span class="font-display tracking-wide">Envío (3 lbs)</span>
                    <span class="text-white font-display tracking-wider">$15.00</span>
                  </div>
                  <div class="flex justify-between text-zinc-400">
                    <span class="font-display tracking-wide">Aduana</span>
                    <span class="text-white font-display tracking-wider">$21.00</span>
                  </div>
                  <div class="border-t border-white/10 pt-3 flex justify-between font-display text-xl tracking-wider">
                    <span class="text-white">Total</span>
                    <span class="text-teal-400">$155.27</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Scroll Indicator - inside hero, below content -->
      <div class="absolute bottom-[100px] left-1/2 -translate-x-1/2 hero-scroll-indicator z-30">
        <svg class="w-6 h-6 text-teal-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3" />
        </svg>
      </div>
    </section>

    <!-- Features Section -->
    <section ref="featuresSection" class="py-24 sm:py-32 relative z-10">
      <div class="absolute inset-0 bg-gradient-to-b from-transparent via-zinc-900/50 to-transparent pointer-events-none" />
      
      <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- Section Header -->
        <div 
          ref="featuresHeader"
          id="features-header"
          class="text-center mb-20"
        >
          <h2 class="font-display text-4xl sm:text-5xl lg:text-6xl mb-6 tracking-wide">
            ¿Por qué elegirnos?
          </h2>
          <p class="text-lg text-zinc-400 max-w-2xl mx-auto font-display tracking-wide">
            Hacemos que importar desde Amazon sea simple y seguro
          </p>
        </div>
        
        <!-- Feature Cards -->
        <div class="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
          <div
            v-for="(feature, index) in features"
            :key="feature.title"
            :ref="(el) => { if (el) featureCards[index] = el as HTMLElement }"
            class="feature-card group relative rounded-2xl border border-white/5 bg-zinc-900/50 backdrop-blur-sm cursor-pointer overflow-hidden"
            :class="[
              hoveredCard === feature.title ? 'expanded' : ''
            ]"
            :style="{
              maxHeight: hoveredCard === feature.title ? '300px' : '160px',
            }"
            @mouseenter="hoveredCard = feature.title"
            @mouseleave="hoveredCard = null"
          >
            <!-- Hover border glow -->
            <div :class="[
              'absolute inset-0 rounded-2xl pointer-events-none transition-opacity duration-300',
              hoveredCard === feature.title ? 'opacity-100' : 'opacity-0',
              feature.color === 'teal' ? 'shadow-[inset_0_0_40px_rgba(20,184,166,0.15),inset_0_0_1px_rgba(20,184,166,0.5)]' : '',
              feature.color === 'blue' ? 'shadow-[inset_0_0_40px_rgba(59,130,246,0.15),inset_0_0_1px_rgba(59,130,246,0.5)]' : '',
              feature.color === 'purple' ? 'shadow-[inset_0_0_40px_rgba(168,85,247,0.15),inset_0_0_1px_rgba(168,85,247,0.5)]' : '',
              feature.color === 'orange' ? 'shadow-[inset_0_0_40px_rgba(249,115,22,0.15),inset_0_0_1px_rgba(249,115,22,0.5)]' : '',
            ]" />
            
            <!-- Card content -->
            <div class="relative p-8 h-full flex flex-col">
              <!-- Icon -->
              <div :class="[
                'w-14 h-14 rounded-2xl flex items-center justify-center mb-4 transition-all duration-300',
                hoveredCard === feature.title ? 'scale-110' : 'scale-100',
                feature.color === 'teal' ? 'bg-teal-500/10 text-teal-400' : '',
                feature.color === 'blue' ? 'bg-blue-500/10 text-blue-400' : '',
                feature.color === 'purple' ? 'bg-purple-500/10 text-purple-400' : '',
                feature.color === 'orange' ? 'bg-orange-500/10 text-orange-400' : '',
              ]">
                <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="feature.icon" />
                </svg>
              </div>
              
              <!-- Title -->
              <h3 :class="[
                'font-display text-xl mb-2 tracking-wider transition-colors duration-300',
                feature.color === 'teal' ? 'text-teal-400' : '',
                feature.color === 'blue' ? 'text-blue-400' : '',
                feature.color === 'purple' ? 'text-purple-400' : '',
                feature.color === 'orange' ? 'text-orange-400' : '',
              ]">
                {{ feature.title }}
              </h3>
              
              <!-- Description (appears on hover) -->
                <p 
                  class="text-zinc-400 leading-relaxed text-sm transition-all duration-300 overflow-hidden font-display tracking-wide"
                  :style="{
                    maxHeight: hoveredCard === feature.title ? '100px' : '0px',
                    opacity: hoveredCard === feature.title ? 1 : 0,
                    marginTop: hoveredCard === feature.title ? '12px' : '0px',
                  }"
                >
                  {{ feature.description }}
                </p>
              
              <!-- Expand indicator -->
              <div :class="[
                'mt-auto flex items-center gap-2 text-sm transition-all duration-300',
                hoveredCard === feature.title ? 'text-zinc-300 opacity-100' : 'text-zinc-500 opacity-60'
              ]">
                <span class="font-display tracking-wider">{{ hoveredCard === feature.title ? 'Ocultar' : 'Ver más' }}</span>
                <svg :class="[
                  'w-4 h-4 transition-transform duration-300',
                  hoveredCard === feature.title ? '-translate-x-1' : 'translate-x-0'
                ]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16l-4-4m0 0l4-4m-4 4h18" />
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- How it works Section -->
    <section ref="stepsSection" class="py-24 sm:py-32 relative z-10 overflow-hidden">
      <!-- Animated background line -->
      <div class="absolute top-1/2 left-0 w-full h-px pointer-events-none">
        <div 
          class="h-full bg-gradient-to-r from-transparent via-teal-500/30 to-transparent"
          :style="{
            transform: 'scaleX(1)',
            transition: 'transform 1s ease-out'
          }"
        />
      </div>
      
      <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- Section Header -->
        <div 
          ref="stepsHeader"
          id="steps-header"
          class="text-center mb-20"
        >
          <h2 class="text-4xl sm:text-5xl lg:text-6xl font-bold mb-6">
            ¿Cómo funciona?
          </h2>
          <p class="text-lg text-zinc-400 max-w-2xl mx-auto">
            En solo 4 pasos recibe tu producto en Ecuador
          </p>
        </div>

        <!-- Steps -->
        <div class="grid sm:grid-cols-2 lg:grid-cols-4 gap-8 lg:gap-12">
          <div
            v-for="(step, index) in steps"
            :key="step.number"
            :ref="(el) => { if (el) stepCards[index] = el as HTMLElement }"
            class="step-card group relative"
          >
            <!-- Connector line (desktop only) -->
            <div 
              v-if="index < steps.length - 1"
              class="hidden lg:block absolute top-[42px] left-[60px] right-[-48px] h-0.5 z-10 overflow-hidden"
            >
              <div 
                class="h-full bg-gradient-to-r from-teal-500 to-teal-500/30"
                style="transform: scaleX(1); transform-origin: left; transition: transform 0.5s ease-out;"
              />
            </div>
            
            <!-- Step circle -->
            <div class="relative inline-flex lg:flex items-center justify-center mb-6">
              <!-- Glow effect -->
              <div 
                class="absolute inset-0 w-24 h-24 bg-teal-500/20 rounded-full blur-xl"
              />
              
              <!-- Circle -->
              <div 
                class="relative w-20 h-20 rounded-full bg-zinc-900/90 flex items-center justify-center transition-all duration-500 group-hover:scale-110 group-hover:shadow-[0_0_30px_rgba(20,184,166,0.3)]"
                style="border: 2px solid rgba(20, 184, 166, 0.5);"
              >
                <!-- Number -->
                <span 
                  class="text-3xl font-bold transition-all duration-500"
                  style="color: #14b8a6;"
                >
                  {{ step.number }}
                </span>
              </div>
            </div>
            
            <!-- Content -->
            <div class="text-center lg:text-left">
              <h3 class="font-display text-2xl mb-3 tracking-wide text-white transition-colors duration-300 group-hover:text-teal-400">
                {{ step.title }}
              </h3>
              <p class="text-zinc-400 leading-relaxed transition-colors duration-300 group-hover:text-zinc-300 font-display tracking-wide">
                {{ step.description }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Testimonials Banner Section -->
    <section ref="testimonialsSection" class="py-20 sm:py-28 relative z-10 overflow-hidden">
      <!-- Top fade gradient -->
      <div class="absolute top-0 left-0 right-0 h-32 bg-gradient-to-b from-zinc-900/80 to-transparent pointer-events-none z-10" />
      
      <!-- Bottom fade gradient -->
      <div class="absolute bottom-0 left-0 right-0 h-32 bg-gradient-to-t from-zinc-900/80 to-transparent pointer-events-none z-10" />
      
      <div class="relative">
        <!-- Header -->
        <div ref="testimonialsHeader" class="text-center mb-12 px-4">
          <div class="inline-flex items-center gap-2 px-4 py-1.5 bg-amber-500/10 text-amber-400 rounded-full text-sm font-medium mb-5 border border-amber-500/20">
            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
            </svg>
            opiniones de nuestro trabajo
          </div>
          <h2 class="font-display text-4xl sm:text-5xl lg:text-6xl text-white/90 tracking-wide">
            Lo que dicen de nosotros
          </h2>
        </div>

        <!-- Carousel -->
        <div class="relative carousel-wrapper">
          <div class="carousel-track">
            <!-- First set -->
            <div
              v-for="(t, i) in testimonials"
              :key="`a-${i}`"
              class="carousel-card group"
            >
              <div class="relative bg-zinc-900/80 backdrop-blur-sm rounded-2xl border border-white/5 p-6 sm:p-7 mx-3 overflow-hidden transition-all duration-300 hover:border-teal-500/30 hover:bg-zinc-900/95 hover:shadow-[0_0_40px_rgba(20,184,166,0.08)]">
                <!-- Accent top bar -->
                <div :class="['absolute top-0 left-0 right-0 h-1 rounded-t-2xl', t.accentColor]" />
                
                <!-- Stars -->
                <div class="flex items-center gap-0.5 mb-4">
                  <svg
                    v-for="star in 5"
                    :key="star"
                    class="w-4 h-4"
                    :class="star <= t.rating ? 'text-amber-400' : 'text-zinc-700'"
                    fill="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                  </svg>
                </div>

                <!-- Text -->
                <p class="text-zinc-300 text-sm sm:text-base leading-relaxed mb-6 italic font-display tracking-wide">
                  "{{ t.text }}"
                </p>

                <!-- Author -->
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-3">
                    <div class="relative">
                      <div :class="[
                        'w-10 h-10 rounded-full flex items-center justify-center text-white font-bold text-xs ring-2 ring-offset-1 ring-offset-zinc-900',
                        t.avatarBg
                      ]">
                        {{ t.initials }}
                      </div>
                      <div class="absolute -bottom-0.5 -right-0.5 w-4 h-4 bg-teal-500 rounded-full flex items-center justify-center ring-1 ring-zinc-900">
                        <svg class="w-2.5 h-2.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/>
                        </svg>
                      </div>
                    </div>
                    <div>
                      <p class="font-display text-base tracking-wider text-white">{{ t.name }}</p>
                      <p class="text-zinc-500 text-xs font-display tracking-wider uppercase">{{ t.location }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Duplicate set for seamless loop -->
            <div
              v-for="(t, i) in testimonials"
              :key="`b-${i}`"
              class="carousel-card group"
              aria-hidden="true"
            >
              <div class="relative bg-zinc-900/80 backdrop-blur-sm rounded-2xl border border-white/5 p-6 sm:p-7 mx-3 overflow-hidden transition-all duration-300 hover:border-teal-500/30 hover:bg-zinc-900/95">
                <div :class="['absolute top-0 left-0 right-0 h-1 rounded-t-2xl', t.accentColor]" />
                <div class="flex items-center gap-0.5 mb-4">
                  <svg
                    v-for="star in 5"
                    :key="star"
                    class="w-4 h-4"
                    :class="star <= t.rating ? 'text-amber-400' : 'text-zinc-700'"
                    fill="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                  </svg>
                </div>
                <p class="text-zinc-300 text-sm sm:text-base leading-relaxed mb-6 italic font-display tracking-wide">
                  "{{ t.text }}"
                </p>
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-3">
                    <div class="relative">
                      <div :class="[
                        'w-10 h-10 rounded-full flex items-center justify-center text-white font-bold text-xs ring-2 ring-offset-1 ring-offset-zinc-900',
                        t.avatarBg
                      ]">
                        {{ t.initials }}
                      </div>
                      <div class="absolute -bottom-0.5 -right-0.5 w-4 h-4 bg-teal-500 rounded-full flex items-center justify-center ring-1 ring-zinc-900">
                        <svg class="w-2.5 h-2.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/>
                        </svg>
                      </div>
                    </div>
                    <div>
                      <p class="font-display text-base tracking-wider text-white">{{ t.name }}</p>
                      <p class="text-zinc-500 text-xs font-display tracking-wider uppercase">{{ t.location }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Bottom CTA -->
        <div class="text-center mt-14 px-4">
          <RouterLink 
            to="/calculadora"
            class="group inline-flex items-center gap-3 bg-white text-teal-600 font-display text-lg px-8 py-3 rounded-xl transition-all duration-300 hover:bg-zinc-900 hover:text-white hover:scale-[1.02]"
          >
            Cotiza tu pedido
            <svg class="w-5 h-5 transition-transform group-hover:translate-x-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3" />
            </svg>
          </RouterLink>
          <p class="mt-4 text-zinc-500 text-sm font-display tracking-wider">Sin compromiso · Respuesta en menos de 24hs</p>
        </div>
      </div>
    </section>

    <!-- Test Credentials Section -->
    <section class="py-16 bg-zinc-900/50">
      <div class="max-w-4xl mx-auto px-4">
        <h2 class="text-2xl font-display font-bold text-white text-center mb-8">Cuentas de Prueba</h2>
        <div class="grid md:grid-cols-3 gap-4">
          <!-- SuperAdmin -->
          <div class="bg-zinc-800/50 rounded-xl p-4 border border-yellow-500/30">
            <p class="text-yellow-400 font-semibold mb-2">SuperAdmin</p>
            <p class="text-zinc-300 text-sm">Email: <span class="text-white">admin@amzexpress.com</span></p>
            <p class="text-zinc-300 text-sm">Password: <span class="text-white">SuperAdmin123!</span></p>
          </div>
          <!-- Admin -->
          <div class="bg-zinc-800/50 rounded-xl p-4 border border-teal-500/30">
            <p class="text-teal-400 font-semibold mb-2">Admin</p>
            <p class="text-zinc-300 text-sm">Email: <span class="text-white">admin.soporte@amzexpress.com</span></p>
            <p class="text-zinc-300 text-sm">Password: <span class="text-white">Admin123!</span></p>
          </div>
          <!-- Usuario Normal -->
          <div class="bg-zinc-800/50 rounded-xl p-4 border border-blue-500/30">
            <p class="text-blue-400 font-semibold mb-2">Usuario</p>
            <p class="text-zinc-300 text-sm">Email: <span class="text-white">usuario@amzexpress.com</span></p>
            <p class="text-zinc-300 text-sm">Password: <span class="text-white">Usuario123!</span></p>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer :class="[
      'fixed bottom-0 left-0 right-0 z-20 py-4 transition-all duration-300',
      navScrolled
        ? 'bg-zinc-950 border-t border-white/5'
        : 'bg-transparent border-t border-transparent'
    ]">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-2 text-center">
        <p class="text-zinc-600 text-xs">
          © 2024 amz express · todos los derechos reservados
        </p>
        <p class="text-zinc-600 text-xs">
          sitio web realizado por <span class="text-teal-400 font-medium">XpressDevelopment</span>
        </p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* Gradient text */
.gradient-text {
  background: linear-gradient(135deg, #14b8a6 0%, #0d9488 50%, #0f766e 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* Motion-style animations */
[data-animate] {
  will-change: transform, opacity;
}

/* Spring-like easing for smoother animations */
.spring-ease {
  transition-timing-function: cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* Pulse ring animation */
@keyframes pulse-ring {
  0% {
    transform: scale(1);
    opacity: 0.5;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}

/* Particles */
.particles-container {
  position: fixed;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.particle {
  position: absolute;
  width: 5px;
  height: 5px;
  background: linear-gradient(135deg, #14b8a6, #0d9488);
  border-radius: 50%;
  opacity: 0.6;
  animation: particle-rise linear infinite;
}

/* Badge pulse animation */
@keyframes badge-pulse {
  0%, 100% {
    background-color: rgba(20, 184, 166, 0.1);
    box-shadow: 0 0 0 0 rgba(20, 184, 166, 0);
  }
  50% {
    background-color: rgba(20, 184, 166, 0.25);
    box-shadow: 0 0 20px 2px rgba(20, 184, 166, 0.15);
  }
}

.badge-pulse {
  animation: badge-pulse 2s ease-in-out infinite;
}

/* Scroll indicator bounce */
.hero-scroll-indicator {
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateX(-50%) translateY(0);
  }
  40% {
    transform: translateX(-50%) translateY(-10px);
  }
  60% {
    transform: translateX(-50%) translateY(-5px);
  }
}

/* Carousel */
.carousel-wrapper {
  overflow: hidden;
  position: relative;
}

.carousel-wrapper::before,
.carousel-wrapper::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  width: 100px;
  z-index: 10;
  pointer-events: none;
}

.carousel-wrapper::before {
  left: 0;
  background: linear-gradient(to right, #09090b, transparent);
}

.carousel-wrapper::after {
  right: 0;
  background: linear-gradient(to left, #09090b, transparent);
}

.carousel-track {
  display: flex;
  gap: 0;
  animation: carousel-scroll 60s linear infinite;
  width: max-content;
}

.carousel-track:hover {
  animation-play-state: paused;
}

.carousel-card {
  width: 380px;
  flex-shrink: 0;
}

@media (min-width: 640px) {
  .carousel-card {
    width: 400px;
  }
}

@media (min-width: 1024px) {
  .carousel-card {
    width: 420px;
  }
}

@keyframes carousel-scroll {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-50%);
  }
}
</style>


