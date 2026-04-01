<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { useOrderStore } from '@/stores/order'
import OrderProgress from '@/components/OrderProgress.vue'
import FadeIn from '@/components/FadeIn.vue'
import type { OrderStatus } from '@/types'

const route = useRoute()
const orderStore = useOrderStore()

const orderId = computed(() => Number(route.params.id))
const order = computed(() => orderStore.currentOrder)
const loading = computed(() => orderStore.loading)

onMounted(() => {
  orderStore.fetchOrderById(orderId.value)
})

const statusLabels: Record<OrderStatus, string> = {
  PENDIENTE_PAGO: 'Pendiente de Pago',
  PAGADO: 'Pagado',
  PEDIDO_REALIZADO: 'Pedido Realizado',
  LLEGO_BODEGA_USA: 'En bodega USA',
  EN_TRANSITO_ECUADOR: 'En tránsito',
  LLEGO_BODEGA_ECUADOR: 'Llegó a Ecuador',
  LISTO_ENTREGA: 'Listo para entrega',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado',
}

const statusColors: Record<OrderStatus, string> = {
  PENDIENTE_PAGO: 'bg-amber-500/20 text-amber-400 border border-amber-500/30',
  PAGADO: 'bg-green-500/20 text-green-400 border border-green-500/30',
  PEDIDO_REALIZADO: 'bg-blue-500/20 text-blue-400 border border-blue-500/30',
  LLEGO_BODEGA_USA: 'bg-yellow-500/20 text-yellow-400 border border-yellow-500/30',
  EN_TRANSITO_ECUADOR: 'bg-orange-500/20 text-orange-400 border border-orange-500/30',
  LLEGO_BODEGA_ECUADOR: 'bg-purple-500/20 text-purple-400 border border-purple-500/30',
  LISTO_ENTREGA: 'bg-green-500/20 text-green-400 border border-green-500/30',
  ENTREGADO: 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30',
  CANCELADO: 'bg-red-500/20 text-red-400 border border-red-500/30',
}

function getStatusLabelForOrder(): string {
  if (!order.value) return ''

  const transferUnderReview = order.value.status === 'PENDIENTE_PAGO'
    && order.value.paymentMethod === 'TRANSFERENCIA'
    && (
      order.value.paymentReference?.includes('Comprobante')
      || order.value.notes?.includes('Comprobante enviado')
    )

  if (transferUnderReview) {
    return 'Comprobando pago'
  }

  return statusLabels[order.value.status]
}

const shippingLabels: Record<string, string> = {
  PICKUP: 'Retiro en bodega',
  UBER: 'Delivery Uber',
  SERVIENTREGA: 'Servientrega',
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('es-EC', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatCurrency(value: number | undefined): string {
  if (!value) return '$0.00'
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(value)
}
</script>

<template>
  <div class="min-h-screen pt-[64px] pb-16">
    <!-- Background -->
    <div class="absolute inset-0 bg-gradient-to-b from-zinc-900 via-zinc-900 to-zinc-950 pointer-events-none" />
    <div class="absolute top-0 left-1/2 -translate-x-1/2 w-full h-96 bg-teal-500/5 blur-3xl" />

    <div class="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Back Button -->
      <FadeIn direction="up">
        <RouterLink to="/ordenes" class="inline-flex items-center text-zinc-400 hover:text-white mb-6 transition-colors group">
          <svg class="w-5 h-5 mr-2 group-hover:-translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Volver a mis pedidos
        </RouterLink>
      </FadeIn>

      <!-- Loading -->
      <FadeIn v-if="loading" direction="up">
        <div class="flex justify-center py-12">
          <div class="w-12 h-12 border-4 border-teal-500/20 border-t-teal-500 rounded-full animate-spin" />
        </div>
      </FadeIn>

      <!-- Order Content -->
      <div v-else-if="order" class="space-y-6">
        <!-- Header -->
        <FadeIn direction="up">
          <div class="glass rounded-2xl p-6 sm:p-8 border border-white/5">
            <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
              <div>
                <h1 class="text-3xl font-bold mb-2 text-white">Pedido #{{ order.id }}</h1>
                <p class="text-zinc-500">{{ formatDate(order.createdAt) }}</p>
              </div>
              <span :class="['px-4 py-2 rounded-full font-medium text-sm', statusColors[order.status]]">
                {{ getStatusLabelForOrder() }}
              </span>
            </div>

            <!-- Progress -->
            <OrderProgress :current-status="order.status" />
          </div>
        </FadeIn>

        <!-- Product Info -->
        <FadeIn direction="up" :delay="100">
          <div class="glass rounded-2xl p-6 sm:p-8 border border-white/5">
            <h2 class="text-xl font-bold mb-6 text-white">Detalle del Producto</h2>
            <div class="grid sm:grid-cols-2 gap-6">
              <div>
                <p class="text-sm text-zinc-500 mb-1">Producto</p>
                <p class="font-medium text-white">{{ order.productName || 'Producto Amazon' }}</p>
              </div>
              <div v-if="order.productAsin">
                <p class="text-sm text-zinc-500 mb-1">ASIN</p>
                <p class="font-medium text-white">{{ order.productAsin }}</p>
              </div>
              <div>
                <p class="text-sm text-zinc-500 mb-1">Precio del Producto</p>
                <p class="font-medium text-white">{{ formatCurrency(order.productPrice) }}</p>
              </div>
              <div v-if="order.weight">
                <p class="text-sm text-zinc-500 mb-1">Peso</p>
                <p class="font-medium text-white">{{ order.weight }} lbs</p>
              </div>
              <div>
                <p class="text-sm text-zinc-500 mb-1">Opción de Entrega</p>
                <p class="font-medium text-white">{{ shippingLabels[order.shippingOption] || order.shippingOption }}</p>
              </div>
              <div class="sm:col-span-2">
                <p class="text-sm text-zinc-500 mb-1">URL de Amazon</p>
                <a :href="order.amazonUrl" target="_blank" class="text-teal-400 hover:text-teal-300 transition-colors break-all">
                  {{ order.amazonUrl || 'No proporcionada' }}
                </a>
              </div>
            </div>
          </div>
        </FadeIn>

        <!-- Pricing -->
        <FadeIn direction="up" :delay="150">
          <div class="glass rounded-2xl p-6 sm:p-8 border border-white/5">
            <h2 class="text-xl font-bold mb-6 text-white">Resumen de Pago</h2>
            <div class="flex justify-between items-center">
              <span class="text-zinc-400">Total Pagado</span>
              <span class="text-3xl font-bold text-teal-400">{{ formatCurrency(order.totalPrice) }}</span>
            </div>
          </div>
        </FadeIn>

        <!-- Notes -->
        <FadeIn v-if="order.notes" direction="up" :delay="200">
          <div class="glass rounded-2xl p-6 sm:p-8 border border-white/5">
            <h2 class="text-xl font-bold mb-4 text-white">Notas</h2>
            <p class="text-zinc-300 leading-relaxed">{{ order.notes }}</p>
          </div>
        </FadeIn>

        <!-- Status History -->
        <FadeIn direction="up" :delay="250">
          <div class="glass rounded-2xl p-6 sm:p-8 border border-white/5">
            <h2 class="text-xl font-bold mb-6 text-white">Historial de Estado</h2>
            <div class="space-y-4">
              <div
                v-for="(item, index) in order.statusHistory"
                :key="item.id"
                class="flex items-start gap-4"
              >
                <div class="w-3 h-3 rounded-full bg-teal-500 mt-1.5 flex-shrink-0" />
                <div class="flex-1 pb-4 border-b border-white/5 last:border-0 last:pb-0">
                  <p class="font-medium text-white">{{ statusLabels[item.status] }}</p>
                  <p class="text-sm text-zinc-500">{{ formatDate(item.createdAt) }}</p>
                </div>
              </div>
            </div>
          </div>
        </FadeIn>
      </div>

      <!-- Error -->
      <FadeIn v-else direction="up">
        <div class="glass rounded-2xl p-12 text-center border border-white/5">
          <div class="w-16 h-16 bg-zinc-800/50 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg class="w-8 h-8 text-zinc-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <p class="text-zinc-400">No se pudo cargar el pedido</p>
        </div>
      </FadeIn>
    </div>
  </div>
</template>

