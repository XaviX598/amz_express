<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const props = withDefaults(defineProps<{
  delay?: number
  duration?: number
  direction?: 'up' | 'down' | 'left' | 'right' | 'scale'
  threshold?: number
}>(), {
  delay: 0,
  duration: 600,
  direction: 'up',
  threshold: 0.1
})

const isVisible = ref(false)
const elementRef = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null
let fallbackTimer: ReturnType<typeof setTimeout> | null = null

function reveal() {
  if (isVisible.value) return
  isVisible.value = true
}

onMounted(() => {
  // Safety fallback: avoid blank sections if observer callback is skipped during route transitions.
  fallbackTimer = setTimeout(() => {
    reveal()
  }, 400)

  if (!('IntersectionObserver' in window) || !elementRef.value) {
    reveal()
    return
  }

  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          reveal()
          observer?.unobserve(entry.target)
        }
      })
    },
    { threshold: props.threshold }
  )

  observer.observe(elementRef.value)
})

onUnmounted(() => {
  observer?.disconnect()
  if (fallbackTimer) {
    clearTimeout(fallbackTimer)
    fallbackTimer = null
  }
})

const transformStyles = {
  up: 'translateY(40px)',
  down: 'translateY(-40px)',
  left: 'translateX(-40px)',
  right: 'translateX(40px)',
  scale: 'scale(0.9)',
}

const transitionStyle = `${props.duration}ms cubic-bezier(0.4, 0, 0.2, 1)`
</script>

<template>
  <div
    ref="elementRef"
    class="transition-opacity"
    :style="{
      opacity: isVisible ? 1 : 0,
      transform: isVisible ? 'translate(0) scale(1)' : transformStyles[direction],
      transition: `opacity ${transitionStyle}, transform ${transitionStyle}`,
      transitionDelay: `${delay}ms`,
    }"
  >
    <slot />
  </div>
</template>
