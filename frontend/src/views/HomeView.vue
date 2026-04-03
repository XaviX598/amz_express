<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import MotionButton from '@/components/MotionButton.vue'

gsap.registerPlugin(ScrollTrigger)

const homeRoot = ref<HTMLElement | null>(null)
const heroSection = ref<HTMLElement | null>(null)
const heroBackdrop = ref<HTMLElement | null>(null)

const particles = ref<{ id: number; left: string; delay: string; duration: string; size: string }[]>([])
let particleId = 0
let ctx: gsap.Context | null = null

const showDemoAccounts = import.meta.env.DEV
const currentYear = new Date().getFullYear()
const storySteps = [
  { id: 'story-risk', label: 'Riesgo' },
  { id: 'story-method', label: 'Metodo' },
  { id: 'story-value', label: 'Ventaja' },
  { id: 'story-trust', label: 'Confianza' },
]

function createParticles() {
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const width = window.innerWidth
  const particleCount = reduced ? 0 : width < 768 ? 14 : 32

  particles.value = Array.from({ length: particleCount }, () => ({
    id: particleId++,
    left: `${Math.random() * 100}%`,
    delay: `${Math.random() * 12}s`,
    duration: `${10 + Math.random() * 12}s`,
    size: `${2 + Math.random() * 4}px`,
  }))
}

function setupAnimations() {
  if (!homeRoot.value) return

  ctx = gsap.context(() => {
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    const mm = gsap.matchMedia()

    if (reduced) {
      gsap.set('.hero-reveal, .hero-panel, .section-title, .section-subtitle, .scroll-card, .reveal-cta', { clearProps: 'all' })
      return
    }

    gsap.defaults({ overwrite: 'auto', force3D: true })
    ScrollTrigger.config({ ignoreMobileResize: true })

    const setupSharedScrollAnimations = () => {
      const heroTimeline = gsap.timeline({ defaults: { ease: 'power3.out' } })
      heroTimeline
        .from('.hero-reveal', {
          autoAlpha: 0,
          y: 28,
          duration: 0.8,
          stagger: 0.1,
        })
        .from(
          '.hero-panel',
          {
            autoAlpha: 0,
            x: 28,
            duration: 0.95,
          },
          '-=0.45'
        )
        .from(
          '.story-indicator__item',
          {
            autoAlpha: 0,
            x: 18,
            stagger: 0.07,
            duration: 0.35,
            ease: 'power2.out',
          },
          '-=0.25'
        )

      if (heroSection.value && heroBackdrop.value) {
        gsap.to(heroBackdrop.value, {
          scale: 1.06,
          yPercent: 8,
          ease: 'none',
          scrollTrigger: {
            trigger: heroSection.value,
            start: 'top top',
            end: 'bottom top',
            scrub: 1,
            invalidateOnRefresh: true,
          },
        })
      }

      const indicatorItems = gsap.utils.toArray<HTMLElement>('.story-indicator__item')
      const setActiveIndicator = (index: number) => {
        indicatorItems.forEach((item, itemIndex) => item.classList.toggle('is-active', itemIndex === index))
      }

      setActiveIndicator(0)

      gsap.utils.toArray<HTMLElement>('.scroll-section').forEach((section) => {
        const title = section.querySelector<HTMLElement>('.section-title')
        const subtitle = section.querySelector<HTMLElement>('.section-subtitle')
        const kicker = section.querySelector<HTMLElement>('.section-kicker')
        const cards = gsap.utils.toArray<HTMLElement>('.scroll-card', section)
        const sectionBg = section.querySelector<HTMLElement>('.section-bg')

        if (title || subtitle || kicker) {
          const sectionTimeline = gsap.timeline({
            defaults: { ease: 'power2.out' },
            scrollTrigger: {
              trigger: section,
              start: 'top 82%',
              end: 'top 46%',
              toggleActions: 'play none none reverse',
              invalidateOnRefresh: true,
            },
          })

          if (kicker) {
            sectionTimeline.from(kicker, {
              autoAlpha: 0,
              y: 14,
              duration: 0.5,
            })
          }

          if (title) {
            sectionTimeline.from(title, {
              autoAlpha: 0,
              y: 26,
              duration: 0.6,
            }, kicker ? '-=0.3' : 0)
          }

          if (subtitle) {
            sectionTimeline.from(
              subtitle,
              {
                autoAlpha: 0,
                y: 18,
                duration: 0.5,
              },
              '-=0.28'
            )
          }
        }

        if (cards.length) {
          ScrollTrigger.batch(cards, {
            start: 'top 88%',
            once: true,
            onEnter: (batch) => {
              gsap.fromTo(
                batch,
                { autoAlpha: 0, y: 22 },
                { autoAlpha: 1, y: 0, duration: 0.55, stagger: 0.07, ease: 'power2.out' }
              )
            },
          })
        }

        if (sectionBg) {
          gsap.fromTo(
            sectionBg,
            {
              autoAlpha: 0.3,
              scale: 1.08,
              clipPath: 'inset(8% 0% 8% 0% round 1rem)',
            },
            {
              autoAlpha: 1,
              scale: 1,
              clipPath: 'inset(0% 0% 0% 0% round 0rem)',
              duration: 0.95,
              ease: 'power2.out',
              scrollTrigger: {
                trigger: section,
                start: 'top 80%',
                end: 'top 38%',
                toggleActions: 'play none none reverse',
                invalidateOnRefresh: true,
              },
            }
          )

          gsap.fromTo(
            sectionBg,
            { yPercent: 8 },
            {
              yPercent: -8,
              ease: 'none',
              scrollTrigger: {
                trigger: section,
                start: 'top bottom',
                end: 'bottom top',
                scrub: 0.9,
                invalidateOnRefresh: true,
              },
            }
          )
        }

        const stepIndexAttr = section.getAttribute('data-story-step')
        if (stepIndexAttr !== null) {
          const stepIndex = Number(stepIndexAttr)
          ScrollTrigger.create({
            trigger: section,
            start: 'top 60%',
            end: 'bottom 60%',
            onEnter: () => setActiveIndicator(stepIndex),
            onEnterBack: () => setActiveIndicator(stepIndex),
          })
        }
      })
    }

    mm.add('(min-width: 1024px)', () => {
      setupSharedScrollAnimations()

      gsap.to('.hero-panel', {
        y: -10,
        duration: 2.2,
        ease: 'sine.inOut',
        repeat: -1,
        yoyo: true,
      })

      gsap.to('.orb-a', {
        yPercent: 12,
        ease: 'none',
        scrollTrigger: {
          trigger: homeRoot.value,
          start: 'top top',
          end: 'bottom bottom',
          scrub: 1,
          invalidateOnRefresh: true,
        },
      })

      gsap.to('.orb-b', {
        yPercent: -10,
        ease: 'none',
        scrollTrigger: {
          trigger: homeRoot.value,
          start: 'top top',
          end: 'bottom bottom',
          scrub: 1,
          invalidateOnRefresh: true,
        },
      })

      gsap.to('.orb-c', {
        yPercent: 8,
        ease: 'none',
        scrollTrigger: {
          trigger: homeRoot.value,
          start: 'top top',
          end: 'bottom bottom',
          scrub: 1,
          invalidateOnRefresh: true,
        },
      })
    })

    mm.add('(max-width: 1023px)', () => {
      setupSharedScrollAnimations()

      gsap.to('.hero-panel', {
        y: -6,
        duration: 2.4,
        ease: 'sine.inOut',
        repeat: -1,
        yoyo: true,
      })
    })

    ScrollTrigger.refresh()
    return () => mm.revert()
  }, homeRoot.value)
}

onMounted(async () => {
  createParticles()
  await nextTick()
  setupAnimations()
})

onUnmounted(() => {
  if (ctx) {
    ctx.revert()
    ctx = null
  }
})

const painPoints = [
  {
    title: 'Costos inciertos',
    description: 'Evita comprar sin saber el total final. Antes de pagar ya tienes tu proyeccion completa.',
  },
  {
    title: 'Falta de visibilidad',
    description: 'Con seguimiento por etapas sabes si tu pedido esta en compra, transito o entrega.',
  },
  {
    title: 'Soporte tardio',
    description: 'Te respondemos por WhatsApp con personas reales cuando necesitas decidir rapido.',
  },
]

const features = [
  {
    title: 'Cotizacion inmediata',
    description: 'Simula el total de importacion en segundos con producto, peso y recargos.',
    icon: 'M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z',
  },
  {
    title: 'Operacion segura',
    description: 'Tu compra viaja protegida desde Estados Unidos hasta Ecuador.',
    icon: 'M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z',
  },
  {
    title: 'Estado del pedido',
    description: 'Visualiza tu progreso y manten el control sin llamadas ni correos innecesarios.',
    icon: 'M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z M15 11a3 3 0 11-6 0 3 3 0 016 0z',
  },
  {
    title: 'Acompanamiento humano',
    description: 'Nuestro equipo te ayuda en cada decision clave durante el proceso.',
    icon: 'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z',
  },
]

const steps = [
  {
    number: '01',
    title: 'Comparte tu producto',
    description: 'Toma el precio en Amazon y define el peso aproximado del paquete.',
  },
  {
    number: '02',
    title: 'Recibe tu cotizacion',
    description: 'Calculamos producto, impuestos, aduana, manejo y envio.',
  },
  {
    number: '03',
    title: 'Confirma con soporte',
    description: 'Aseguramos datos, pagos y detalles de recepcion antes del despacho.',
  },
  {
    number: '04',
    title: 'Recibe en Ecuador',
    description: 'Te entregamos por el metodo coordinado y con seguimiento activo.',
  },
]

const metrics = [
  { value: '24h', label: 'respuesta inicial promedio' },
  { value: '100%', label: 'costos visibles antes de confirmar' },
  { value: '1 panel', label: 'seguimiento por etapas del pedido' },
]

const testimonials = [
  {
    name: 'Maria Fernandez',
    location: 'Guayaquil',
    text: 'Importe una laptop y todo llego en excelente estado. El seguimiento me dio mucha tranquilidad.',
  },
  {
    name: 'Carlos Mendez',
    location: 'Quito',
    text: 'El proceso fue claro de inicio a fin. Tome decisiones con numeros, no con suposiciones.',
  },
  {
    name: 'Valentina Solis',
    location: 'Cuenca',
    text: 'Mi pedido llego rapido y bien protegido. La atencion por WhatsApp fue clave.',
  },
]

const faqs = [
  {
    question: 'Que incluye la cotizacion?',
    answer: 'Incluye valor del producto, impuestos, manejo, envio y aduana para que conozcas el total estimado antes de comprar.',
  },
  {
    question: 'Puedo rastrear mi pedido?',
    answer: 'Si. Veras cada etapa principal y recibiras actualizaciones para saber exactamente donde va tu compra.',
  },
  {
    question: 'Como me contacto con soporte?',
    answer: 'Puedes escribirnos por WhatsApp y recibiras acompanamiento humano para dudas, cambios y coordinacion de entrega.',
  },
]

const demoAccounts = [
  {
    role: 'SuperAdmin',
    color: 'yellow',
    email: 'admin@amzexpress.com',
    password: 'SuperAdmin123!',
  },
  {
    role: 'Admin',
    color: 'teal',
    email: 'admin.soporte@amzexpress.com',
    password: 'Admin123!',
  },
  {
    role: 'Usuario',
    color: 'blue',
    email: 'usuario@amzexpress.com',
    password: 'Usuario123!',
  },
]
</script>

<template>
  <div ref="homeRoot" class="home-root relative overflow-hidden">
    <nav class="story-indicator hidden xl:flex" aria-label="Progreso de historia">
      <a
        v-for="(step, index) in storySteps"
        :key="step.id"
        :href="`#${step.id}`"
        class="story-indicator__item"
        :data-story-link="index"
      >
        <span class="story-indicator__dot" />
        <span class="story-indicator__label">{{ step.label }}</span>
      </a>
    </nav>

    <div class="ambient-layer pointer-events-none absolute inset-0 z-0">
      <div class="orb orb-a" />
      <div class="orb orb-b" />
      <div class="orb orb-c" />
    </div>

    <section ref="heroSection" class="hero relative min-h-screen flex items-center overflow-hidden">
      <div
        ref="heroBackdrop"
        class="absolute inset-0 bg-cover bg-center bg-no-repeat"
        style="background-image: url('https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=1920&q=80');"
      />
      <div class="hero-overlay absolute inset-0" />

      <div class="particles pointer-events-none absolute inset-0">
        <div
          v-for="p in particles"
          :key="p.id"
          class="particle"
          :style="{
            left: p.left,
            width: p.size,
            height: p.size,
            animationDelay: p.delay,
            animationDuration: p.duration,
          }"
        />
      </div>

      <div class="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full pt-20 pb-16">
        <div class="grid lg:grid-cols-[1.2fr,0.8fr] gap-10 lg:gap-14 items-center">
          <div>
            <span class="hero-reveal inline-flex items-center px-4 py-1.5 rounded-full text-xs sm:text-sm uppercase tracking-[0.18em] text-teal-200 bg-teal-500/15 border border-teal-400/30">
              Importaciones Amazon para Ecuador
            </span>
            <h1 class="hero-reveal mt-6 font-display text-5xl sm:text-6xl xl:text-7xl leading-[0.95] text-white tracking-wide">
              Del caos de importar
              <span class="gradient-text">al control total</span>
            </h1>
            <p class="hero-reveal mt-6 text-lg text-zinc-300 max-w-2xl tracking-wide">
              Cotiza con claridad, confirma con soporte humano y recibe en Ecuador con seguimiento real.
              Convertimos una compra incierta en un proceso predecible.
            </p>

            <div class="hero-reveal mt-8 flex flex-wrap gap-3">
              <MotionButton to="/calculadora" label="Cotizar ahora" variant="primary" size="lg" />
              <MotionButton to="/register" label="Crear cuenta gratis" variant="secondary" size="lg" />
            </div>

            <div class="hero-reveal mt-7 flex flex-wrap gap-3 text-sm">
              <span class="service-pill">Precios transparentes</span>
              <span class="service-pill">Tracking por etapas</span>
              <span class="service-pill">Soporte por WhatsApp</span>
            </div>
          </div>

          <div class="hero-panel relative">
            <div class="panel-glow" />
            <div class="panel-card hero-panel-card relative rounded-2xl border border-white/20 bg-zinc-900/82 backdrop-blur-xl p-7">
              <p class="text-teal-300 text-sm tracking-[0.16em] uppercase mb-4">Ruta de compra recomendada</p>
              <div class="space-y-3 text-sm">
                <div class="row"><span>1. Cotiza en 60 segundos</span><strong>Rapido</strong></div>
                <div class="row"><span>2. Valida costos reales</span><strong>Claro</strong></div>
                <div class="row"><span>3. Confirma con soporte</span><strong>Seguro</strong></div>
                <div class="row"><span>4. Recibe en Ecuador</span><strong>Simple</strong></div>
                <div class="row total"><span>Resultado</span><strong>Menos riesgo al importar</strong></div>
              </div>
            </div>

            <div class="mt-4 grid grid-cols-1 sm:grid-cols-3 lg:grid-cols-1 xl:grid-cols-3 gap-3">
              <article v-for="metric in metrics" :key="metric.label" class="scroll-card hero-metric-card rounded-xl border border-white/20 bg-zinc-900/72 p-3">
                <p class="text-teal-300 text-xl font-display tracking-wide">{{ metric.value }}</p>
                <p class="text-zinc-400 text-xs leading-snug">{{ metric.label }}</p>
              </article>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section id="story-risk" data-story-step="0" class="scroll-section reveal-section relative z-10 py-24 sm:py-28">
      <div class="section-bg section-bg-crimson" />
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-14">
          <p class="section-kicker uppercase tracking-[0.2em] text-xs text-rose-300">Fase 01 - Riesgo</p>
          <h2 class="section-title font-display text-4xl sm:text-5xl text-white">Donde mas se complica importar</h2>
          <p class="section-subtitle mt-4 text-zinc-400 max-w-3xl mx-auto">
            Si no conoces costos, tiempos ni responsables, terminas comprando a ciegas. Por eso estructuramos todo en un flujo guiado.
          </p>
        </div>

        <div class="cards-grid grid md:grid-cols-3 gap-5">
          <article v-for="pain in painPoints" :key="pain.title" class="reveal-card scroll-card interactive-card pain-card rounded-2xl p-6 border border-white/10 bg-zinc-900/60">
            <h3 class="text-xl font-display text-white tracking-wide">{{ pain.title }}</h3>
            <p class="mt-2 text-zinc-400 text-sm leading-relaxed">{{ pain.description }}</p>
          </article>
        </div>
      </div>
    </section>

    <section id="story-method" data-story-step="1" class="scroll-section reveal-section relative z-10 py-20 sm:py-24">
      <div class="section-bg section-bg-cyan" />
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-12">
          <p class="section-kicker uppercase tracking-[0.2em] text-xs text-cyan-300">Fase 02 - Metodo</p>
          <h2 class="section-title font-display text-4xl sm:text-5xl text-white">Como funciona</h2>
          <p class="section-subtitle mt-4 text-zinc-400 max-w-2xl mx-auto">Seguimos un sistema en 4 pasos para evitar improvisaciones.</p>
        </div>

        <div class="cards-grid grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
          <article v-for="step in steps" :key="step.number" class="reveal-card scroll-card interactive-card step-card rounded-2xl p-6 border border-teal-500/20 bg-zinc-900/60">
            <div class="step-number">{{ step.number }}</div>
            <h3 class="mt-4 text-xl font-display text-white">{{ step.title }}</h3>
            <p class="mt-2 text-zinc-400 text-sm">{{ step.description }}</p>
          </article>
        </div>
      </div>
    </section>

    <section id="story-value" data-story-step="2" class="scroll-section reveal-section relative z-10 py-24 sm:py-28">
      <div class="section-bg section-bg-teal" />
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-14">
          <p class="section-kicker uppercase tracking-[0.2em] text-xs text-teal-300">Fase 03 - Ventaja</p>
          <h2 class="section-title font-display text-4xl sm:text-5xl text-white">Por que AMZ Express</h2>
          <p class="section-subtitle mt-4 text-zinc-400 max-w-2xl mx-auto">
            Combinamos experiencia operativa, tecnologia y atencion humana para que compres mejor.
          </p>
        </div>

        <div class="cards-grid grid sm:grid-cols-2 lg:grid-cols-4 gap-5">
          <article v-for="feature in features" :key="feature.title" class="reveal-card scroll-card interactive-card feature-card rounded-2xl p-6 border border-white/10 bg-zinc-900/60">
            <div class="icon-wrap">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="feature.icon" />
              </svg>
            </div>
            <h3 class="mt-4 text-xl font-display text-white tracking-wide">{{ feature.title }}</h3>
            <p class="mt-2 text-zinc-400 text-sm leading-relaxed">{{ feature.description }}</p>
          </article>
        </div>
      </div>
    </section>

    <section id="story-trust" data-story-step="3" class="scroll-section reveal-section relative z-10 py-20 sm:py-24">
      <div class="section-bg section-bg-amber" />
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-12">
          <p class="section-kicker uppercase tracking-[0.2em] text-xs text-amber-300">Fase 04 - Confianza</p>
          <h2 class="section-title font-display text-4xl sm:text-5xl text-white">Confianza construida en resultados</h2>
          <p class="section-subtitle mt-4 text-zinc-400 max-w-3xl mx-auto">
            Clientes en Ecuador nos eligen por claridad de costos, acompanamiento y cumplimiento.
          </p>
        </div>

        <div class="grid md:grid-cols-3 gap-4 mb-8">
          <article v-for="metric in metrics" :key="`trust-${metric.label}`" class="scroll-card interactive-card rounded-xl border border-white/10 bg-zinc-900/60 p-5 text-center">
            <p class="text-white text-3xl font-display tracking-wide">{{ metric.value }}</p>
            <p class="text-zinc-400 text-sm mt-2">{{ metric.label }}</p>
          </article>
        </div>

        <div class="cards-grid grid md:grid-cols-3 gap-5">
          <article v-for="t in testimonials" :key="t.name" class="reveal-card scroll-card interactive-card testimonial-card rounded-2xl p-6 border border-white/10 bg-zinc-900/65">
            <p class="text-zinc-300 italic leading-relaxed">"{{ t.text }}"</p>
            <div class="mt-5">
              <p class="text-white font-display tracking-wide">{{ t.name }}</p>
              <p class="text-zinc-500 text-sm">{{ t.location }}</p>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section class="scroll-section reveal-section relative z-10 py-20 sm:py-24">
      <div class="section-bg section-bg-teal-soft" />
      <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-12">
          <p class="section-kicker uppercase tracking-[0.2em] text-xs text-teal-300">Desbloquea objeciones</p>
          <h2 class="section-title font-display text-4xl sm:text-5xl text-white">Preguntas frecuentes</h2>
          <p class="section-subtitle mt-4 text-zinc-400">Respuestas rapidas para avanzar con seguridad.</p>
        </div>

        <div class="space-y-3">
          <details v-for="faq in faqs" :key="faq.question" class="scroll-card interactive-card faq-card rounded-xl border border-white/10 bg-zinc-900/60 p-5">
            <summary class="cursor-pointer text-white font-display tracking-wide text-xl">{{ faq.question }}</summary>
            <p class="mt-3 text-zinc-400 leading-relaxed">{{ faq.answer }}</p>
          </details>
        </div>
      </div>
    </section>

    <section v-if="showDemoAccounts" class="scroll-section reveal-section relative z-10 py-14 bg-zinc-900/40">
      <div class="section-bg section-bg-cyan-soft" />
      <div class="max-w-4xl mx-auto px-4">
        <h2 class="section-title text-2xl font-display text-white text-center mb-3">Entorno local de pruebas</h2>
        <p class="section-subtitle text-center text-zinc-500 text-sm mb-8">Credenciales visibles solo en modo desarrollo.</p>

        <div class="cards-grid grid md:grid-cols-3 gap-4">
          <div
            v-for="account in demoAccounts"
            :key="account.role"
            :class="[
              'reveal-card scroll-card interactive-card rounded-xl p-4 border bg-zinc-800/60',
              account.color === 'yellow' ? 'border-yellow-500/30' : account.color === 'teal' ? 'border-teal-500/30' : 'border-blue-500/30',
            ]"
          >
            <p
              :class="[
                'font-semibold mb-2',
                account.color === 'yellow' ? 'text-yellow-400' : account.color === 'teal' ? 'text-teal-400' : 'text-blue-400',
              ]"
            >
              {{ account.role }}
            </p>
            <p class="text-zinc-300 text-sm">Email: <span class="text-white">{{ account.email }}</span></p>
            <p class="text-zinc-300 text-sm">Password: <span class="text-white">{{ account.password }}</span></p>
          </div>
        </div>
      </div>
    </section>

    <section class="scroll-section reveal-cta relative z-10 py-20">
      <div class="section-bg section-bg-cyan-soft" />
      <div class="max-w-4xl mx-auto px-4 text-center">
        <p class="section-kicker uppercase tracking-[0.2em] text-xs text-cyan-300">Decision final</p>
        <h3 class="section-title font-display text-3xl sm:text-4xl text-white">Tu siguiente compra puede ser mas simple</h3>
        <p class="section-subtitle mt-3 text-zinc-400">Cotiza en menos de un minuto y decide con datos claros.</p>
        <div class="mt-8 flex flex-wrap items-center justify-center gap-3">
          <MotionButton to="/calculadora" label="Ir a la calculadora" variant="primary" size="lg" />
          <MotionButton to="/register" label="Crear cuenta" variant="secondary" size="lg" />
        </div>
      </div>
    </section>

    <footer class="relative z-20 py-8 border-t border-[#d5dfe2] bg-[#eef4f4]/95">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-2 text-center">
        <p class="text-[#667780] text-xs">{{ currentYear }} AMZ Express. Todos los derechos reservados.</p>
        <p class="text-[#667780] text-xs">Sitio desarrollado por <span class="text-[#35627A] font-medium">XpressDevelopment</span></p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.home-root {
  background:
    radial-gradient(circle at 8% 12%, rgba(229, 174, 169, 0.24), transparent 38%),
    radial-gradient(circle at 90% 18%, rgba(166, 169, 208, 0.22), transparent 42%),
    #f1f5f5;
}

.hero-overlay {
  background: linear-gradient(120deg, rgba(13, 33, 46, 0.82), rgba(25, 53, 69, 0.62));
}

.story-indicator {
  position: fixed;
  right: 1.2rem;
  top: 50%;
  transform: translateY(-50%);
  z-index: 25;
  flex-direction: column;
  gap: 0.55rem;
  padding: 0.65rem;
  border: 1px solid rgba(53, 98, 122, 0.22);
  border-radius: 0.9rem;
  background: rgba(245, 247, 247, 0.82);
  backdrop-filter: blur(8px);
}

.story-indicator__item {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  color: #5f7078;
  transition: color 220ms ease;
}

.story-indicator__dot {
  width: 0.56rem;
  height: 0.56rem;
  border-radius: 9999px;
  background: rgba(53, 98, 122, 0.64);
  transition: transform 220ms ease, background-color 220ms ease, box-shadow 220ms ease;
}

.story-indicator__label {
  font-family: 'Bebas Neue', sans-serif;
  letter-spacing: 0.08em;
  font-size: 0.92rem;
}

.story-indicator__item.is-active {
  color: #b46258;
}

.story-indicator__item.is-active .story-indicator__dot {
  transform: scale(1.22);
  background: #35627a;
  box-shadow: 0 0 0 4px rgba(166, 169, 208, 0.28);
}

.hero-reveal,
.hero-panel,
.section-title,
.section-subtitle,
.scroll-card,
.section-bg {
  will-change: transform, opacity;
}

.gradient-text {
  background: linear-gradient(135deg, #e5aea9 0%, #a6a9d0 50%, #8e9a98 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.ambient-layer {
  opacity: 0.55;
}

.scroll-section {
  position: relative;
  overflow: hidden;
}

.scroll-section > :not(.section-bg) {
  position: relative;
  z-index: 1;
}

.section-kicker {
  font-family: 'Manrope', sans-serif;
  margin-bottom: 0.8rem;
  font-weight: 700;
}

.section-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.section-bg-teal {
  background: radial-gradient(circle at 15% 35%, rgba(53, 98, 122, 0.1), transparent 56%);
}

.section-bg-cyan {
  background: radial-gradient(circle at 85% 40%, rgba(166, 169, 208, 0.16), transparent 58%);
}

.section-bg-amber {
  background: radial-gradient(circle at 50% 35%, rgba(142, 154, 152, 0.14), transparent 60%);
}

.section-bg-crimson {
  background: radial-gradient(circle at 50% 35%, rgba(180, 98, 88, 0.12), transparent 62%);
}

.section-bg-teal-soft {
  background: radial-gradient(circle at 18% 20%, rgba(229, 174, 169, 0.14), transparent 58%);
}

.section-bg-cyan-soft {
  background: radial-gradient(circle at 80% 50%, rgba(166, 169, 208, 0.13), transparent 58%);
}

.orb {
  position: absolute;
  width: 30rem;
  height: 30rem;
  border-radius: 9999px;
  filter: blur(80px);
  opacity: 0.16;
}

.orb-a {
  left: -12rem;
  top: -9rem;
  background: radial-gradient(circle, rgba(53, 98, 122, 0.9), rgba(53, 98, 122, 0));
}

.orb-b {
  right: -14rem;
  top: 26%;
  background: radial-gradient(circle, rgba(180, 98, 88, 0.66), rgba(180, 98, 88, 0));
}

.orb-c {
  left: 36%;
  bottom: -12rem;
  background: radial-gradient(circle, rgba(166, 169, 208, 0.62), rgba(166, 169, 208, 0));
}

.particles {
  z-index: 2;
}

.particle {
  position: absolute;
  bottom: -20px;
  border-radius: 9999px;
  background: linear-gradient(135deg, rgba(229, 174, 169, 0.9), rgba(166, 169, 208, 0.78));
  opacity: 0;
  animation: particleRise linear infinite;
}

@keyframes particleRise {
  0% {
    transform: translateY(0) scale(0.9);
    opacity: 0;
  }
  20% {
    opacity: 0.5;
  }
  100% {
    transform: translateY(-110vh) scale(1.15);
    opacity: 0;
  }
}

.service-pill {
  border: 1px solid rgba(255, 255, 255, 0.26);
  border-radius: 9999px;
  padding: 0.36rem 0.72rem;
  color: #f5f5f5;
  background: rgba(255, 255, 255, 0.08);
}

.panel-glow {
  position: absolute;
  inset: -20px;
  border-radius: 1.4rem;
  background: radial-gradient(circle at 40% 30%, rgba(229, 174, 169, 0.24), transparent 70%);
  filter: blur(22px);
  opacity: 0.8;
}

.hero-panel-card {
  background:
    linear-gradient(145deg, rgba(12, 32, 45, 0.9), rgba(20, 45, 60, 0.84)),
    rgba(15, 35, 49, 0.9) !important;
  border-color: rgba(245, 245, 245, 0.32) !important;
  box-shadow:
    0 14px 34px rgba(8, 20, 31, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.hero-panel-card > p {
  color: #d6e7f2 !important;
}

.panel-card .row {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  color: #d9e2e9;
}

.panel-card .row strong {
  color: #fff;
  font-weight: 600;
  white-space: nowrap;
}

.panel-card .row.total {
  margin-top: 0.7rem;
  padding-top: 0.75rem;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.panel-card .row.total strong {
  color: #ffd0cb;
}

.scroll-section .section-title {
  color: #17384c;
}

.scroll-section .section-subtitle {
  color: #60717a;
}

.scroll-section .section-kicker {
  color: #b46258;
}

.hero .hero-metric-card {
  background: rgba(10, 30, 44, 0.84) !important;
  border-color: rgba(245, 245, 245, 0.32) !important;
  box-shadow: 0 10px 22px rgba(8, 20, 31, 0.3);
}

.hero .hero-metric-card .text-teal-300 {
  color: #e5f1f7 !important;
}

.hero .hero-metric-card .text-zinc-400 {
  color: #d2dde4 !important;
}

.scroll-section .scroll-card {
  background:
    linear-gradient(155deg, rgba(14, 35, 49, 0.9), rgba(25, 51, 67, 0.86)),
    rgba(13, 33, 47, 0.9) !important;
  border-color: rgba(208, 226, 236, 0.24) !important;
  box-shadow: 0 14px 30px rgba(9, 23, 34, 0.28);
}

.scroll-section .scroll-card .text-white {
  color: #eef5f9 !important;
}

.scroll-section .scroll-card .text-zinc-400,
.scroll-section .scroll-card .text-zinc-500,
.scroll-section .scroll-card .text-zinc-300 {
  color: #c8d8e1 !important;
}

.scroll-section .scroll-card .text-teal-300,
.scroll-section .scroll-card .text-cyan-300,
.scroll-section .scroll-card .text-amber-300,
.scroll-section .scroll-card .text-rose-300 {
  color: #ffd0cb !important;
}

.hero .scroll-card {
  background: rgba(17, 41, 56, 0.66) !important;
  border-color: rgba(245, 245, 245, 0.2) !important;
  box-shadow: none;
}

.pain-card,
.feature-card,
.step-card,
.testimonial-card,
.faq-card {
  transition: transform 0.24s ease, border-color 0.24s ease, box-shadow 0.24s ease;
  position: relative;
  overflow: hidden;
  transform-style: preserve-3d;
}

.interactive-card::after {
  content: '';
  position: absolute;
  inset: -1px;
  background: linear-gradient(120deg, transparent 20%, rgba(229, 174, 169, 0.24), transparent 80%);
  transform: translateX(-120%);
  transition: transform 420ms ease;
  pointer-events: none;
}

.pain-card:hover,
.feature-card:hover,
.step-card:hover,
.testimonial-card:hover,
.faq-card:hover {
  transform: translateY(-4px);
  border-color: rgba(53, 98, 122, 0.38);
  box-shadow: 0 16px 30px rgba(26, 74, 97, 0.14);
}

.interactive-card:hover::after {
  transform: translateX(120%);
}

.icon-wrap {
  width: 2.6rem;
  height: 2.6rem;
  border-radius: 0.8rem;
  display: grid;
  place-items: center;
  color: #b46258;
  background: rgba(229, 174, 169, 0.2);
  border: 1px solid rgba(180, 98, 88, 0.3);
}

.step-number {
  width: 3rem;
  height: 3rem;
  border-radius: 9999px;
  display: grid;
  place-items: center;
  color: #b46258;
  font-weight: 700;
  border: 1px solid rgba(180, 98, 88, 0.34);
  background: rgba(229, 174, 169, 0.16);
}

.faq-card summary {
  list-style: none;
}

.faq-card summary::-webkit-details-marker {
  display: none;
}

.faq-card[open] {
  border-color: rgba(53, 98, 122, 0.4);
}

@media (max-width: 768px) {
  .orb {
    width: 22rem;
    height: 22rem;
    filter: blur(65px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .interactive-card,
  .interactive-card::after,
  .story-indicator__item,
  .story-indicator__dot {
    transition: none;
  }

  .interactive-card:hover {
    transform: none;
    box-shadow: none;
  }

  .interactive-card::after {
    display: none;
  }
}
</style>

