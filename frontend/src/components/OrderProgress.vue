<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import type { OrderStatus } from '@/types'

const props = defineProps<{
  currentStatus: OrderStatus
}>()

const statusOrder: OrderStatus[] = [
  'PEDIDO_REALIZADO',
  'LLEGO_BODEGA_USA',
  'EN_TRANSITO_ECUADOR',
  'LLEGO_BODEGA_ECUADOR',
  'LISTO_ENTREGA',
  'ENTREGADO',
]

const statusLabels: Record<OrderStatus, string> = {
  PEDIDO_REALIZADO: 'Pedido Realizado',
  LLEGO_BODEGA_USA: 'Llegó a USA',
  EN_TRANSITO_ECUADOR: 'En Tránsito',
  LLEGO_BODEGA_ECUADOR: 'Llegó a Ecuador',
  LISTO_ENTREGA: 'Listo para Entrega',
  ENTREGADO: 'Entregado',
}

const statusIcons: Record<OrderStatus, string> = {
  PEDIDO_REALIZADO: 'M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z',
  LLEGO_BODEGA_USA: 'M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
  EN_TRANSITO_ECUADOR: 'M13 16V6a1 1 0 00-1-1H4a1 1 0 00-1 1v10a1 1 0 001 1h1m8-1a1 1 0 01-1 1H9m4-1V8a1 1 0 011-1h2.586a1 1 0 01.707.293l3.414 3.414a1 1 0 01.293.707V16a1 1 0 01-1 1h-1m-6-1a1 1 0 001 1h1M5 17a2 2 0 104 0m-4 0a2 2 0 114 0m6 0a2 2 0 104 0m-4 0a2 2 0 114 0',
  LLEGO_BODEGA_ECUADOR: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4',
  LISTO_ENTREGA: 'M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4',
  ENTREGADO: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
}

// Progress starts at 0, animates when visible
const progressWidth = ref(0)
const isVisible = ref(false)
const elementRef = ref<HTMLElement | null>(null)

const currentIndex = computed(() => statusOrder.indexOf(props.currentStatus))

onMounted(() => {
  if (elementRef.value) {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting && !isVisible.value) {
            isVisible.value = true
            // Trigger animation after becoming visible
            setTimeout(() => {
              progressWidth.value = (currentIndex.value / (statusOrder.length - 1)) * 100
            }, 150)
          }
        })
      },
      { threshold: 0.2 }
    )
    observer.observe(elementRef.value)
  }
})

// Also watch for status changes to re-animate
watch(() => props.currentStatus, () => {
  if (isVisible.value) {
    progressWidth.value = 0
    setTimeout(() => {
      progressWidth.value = (currentIndex.value / (statusOrder.length - 1)) * 100
    }, 50)
  }
})

function getStepClass(status: OrderStatus): string {
  const stepIndex = statusOrder.indexOf(status)
  
  if (stepIndex < currentIndex.value) return 'completed'
  if (stepIndex === currentIndex.value) return 'active'
  return 'pending'
}
</script>

<template>
  <div ref="elementRef" class="w-full">
    <!-- Progress bar -->
    <div class="relative flex justify-between mb-3">
      <div class="absolute top-1/2 left-0 right-0 h-1 bg-zinc-700 -translate-y-1/2 rounded-full" />
      <div 
        class="absolute top-1/2 left-0 h-1 bg-teal-500 -translate-y-1/2 rounded-full transition-all duration-1000 ease-out"
        :style="{ width: `${progressWidth}%` }"
      />
      
      <!-- Steps -->
      <div
        v-for="(status, index) in statusOrder"
        :key="status"
        :class="[
          'relative z-10 flex flex-col items-center',
          getStepClass(status) === 'active' ? 'progress-step active' : ''
        ]"
      >
        <div 
          :class="[
            'w-8 h-8 rounded-full flex items-center justify-center transition-all duration-500',
            getStepClass(status) === 'completed' ? 'bg-teal-500 text-white' : '',
            getStepClass(status) === 'active' ? 'bg-teal-500 text-white progress-dot' : '',
            getStepClass(status) === 'pending' ? 'bg-zinc-700 text-zinc-500' : ''
          ]"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="statusIcons[status]" />
          </svg>
        </div>
        <span 
          :class="[
            'text-[10px] mt-1.5 text-center max-w-[60px] transition-colors duration-500',
            getStepClass(status) === 'pending' ? 'text-zinc-500' : 'text-zinc-300'
          ]"
        >
          {{ statusLabels[status] }}
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.progress-dot {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(20, 184, 166, 0.4);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(20, 184, 166, 0);
  }
}
</style>

