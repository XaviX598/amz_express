<script setup lang="ts">
import { onMounted, computed, ref, watch } from 'vue'
import api from '@/services/api'
import { useOrderStore } from '@/stores/order'
import type { Order, OrderStatus, ShippingOption } from '@/types'
import FadeIn from '@/components/FadeIn.vue'
import MotionButton from '@/components/MotionButton.vue'

// User type from backend
interface AdminUser {
  id: number
  name: string
  email: string
  role: string
  createdAt: string
  orderCount: number
}

const orderStore = useOrderStore()

const users = ref<AdminUser[]>([])
const searchQuery = ref('')
const loadingUsers = ref(false)
const loadingOrders = ref(false)
const selectedUser = ref<AdminUser | null>(null)
const userOrders = ref<Order[]>([])
const showHistoryModal = ref(false)
const showStatusModal = ref(false)
const showObservationModal = ref(false)
const selectedOrder = ref<Order | null>(null)
const newStatus = ref<OrderStatus | null>(null)
const observationText = ref('')
const savingObservation = ref(false)

// Filtros para historial de pedidos
const orderSearchQuery = ref('')
const dateFrom = ref('')
const dateTo = ref('')

// Paginación
const currentPage = ref(1)
const itemsPerPage = 5

const totalPages = computed(() => Math.ceil(filteredOrders.value.length / itemsPerPage))

const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return filteredOrders.value.slice(start, end)
})

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

function goToPage(page: number) {
  currentPage.value = page
}

async function fetchUsers() {
  loadingUsers.value = true
  try {
    const response = await api.get<AdminUser[]>('/users')
    // Only regular users
    users.value = response.data.filter(u => u.role === 'USER')
  } catch (e) {
    console.error('Error fetching users:', e)
  } finally {
    loadingUsers.value = false
  }
}

const filteredUsers = computed(() => {
  if (!searchQuery.value.trim()) return users.value
  const query = searchQuery.value.toLowerCase().trim()
  return users.value.filter(u =>
    u.name.toLowerCase().includes(query) ||
    u.email.toLowerCase().includes(query)
  )
})

const filteredOrders = computed(() => {
  let result = userOrders.value
  
  // Filter by order ID
  if (orderSearchQuery.value.trim()) {
    const query = orderSearchQuery.value.trim()
    const orderId = parseInt(query)
    if (!isNaN(orderId)) {
      result = result.filter(o => o.id === orderId)
    } else {
      // If not a number, search by product name
      result = result.filter(o => 
        o.productName?.toLowerCase().includes(query.toLowerCase())
      )
    }
  }
  
  // Filter by date range
  if (dateFrom.value) {
    const fromDate = new Date(dateFrom.value)
    result = result.filter(o => new Date(o.createdAt) >= fromDate)
  }
  
  if (dateTo.value) {
    const toDate = new Date(dateTo.value)
    toDate.setHours(23, 59, 59, 999) // End of day
    result = result.filter(o => new Date(o.createdAt) <= toDate)
  }
  
  return result
})

async function openHistoryModal(user: AdminUser) {
  selectedUser.value = user
  showHistoryModal.value = true
  loadingOrders.value = true
  userOrders.value = []
  try {
    await orderStore.fetchOrdersByUser(user.id)
    userOrders.value = orderStore.orders
  } catch (e) {
    console.error('Error fetching user orders:', e)
  } finally {
    loadingOrders.value = false
  }
}

function closeHistoryModal() {
  showHistoryModal.value = false
  selectedUser.value = null
  userOrders.value = []
  // Clear filters
  orderSearchQuery.value = ''
  dateFrom.value = ''
  dateTo.value = ''
}

function clearFilters() {
  orderSearchQuery.value = ''
  dateFrom.value = ''
  dateTo.value = ''
  currentPage.value = 1
}

function openStatusModal(order: Order) {
  selectedOrder.value = order
  newStatus.value = order.status
  showStatusModal.value = true
}

async function updateStatus() {
  if (!selectedOrder.value || !newStatus.value) return

  await orderStore.updateOrderStatus(selectedOrder.value.id, {
    status: newStatus.value,
  })

  // Refresh user orders
  if (selectedUser.value) {
    await orderStore.fetchOrdersByUser(selectedUser.value.id)
    userOrders.value = orderStore.orders
  }

  showStatusModal.value = false
  selectedOrder.value = null
}

function openObservationModal(order: Order) {
  selectedOrder.value = order
  observationText.value = order.notes || ''
  showObservationModal.value = true
}

async function saveObservation() {
  if (!selectedOrder.value) return
  
  savingObservation.value = true
  try {
    await orderStore.updateOrderStatus(selectedOrder.value.id, {
      status: selectedOrder.value.status,
      notes: observationText.value,
    })
    
    // Refresh user orders
    if (selectedUser.value) {
      await orderStore.fetchOrdersByUser(selectedUser.value.id)
      userOrders.value = orderStore.orders
    }
    
    showObservationModal.value = false
  } catch (e) {
    console.error('Error saving observation:', e)
  } finally {
    savingObservation.value = false
  }
}

// Reset page when filters change
watch([orderSearchQuery, dateFrom, dateTo], () => {
  currentPage.value = 1
})

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

const statusLabels: Record<OrderStatus, string> = {
  PENDIENTE_PAGO: 'Pendiente de Pago',
  PAGADO: 'Pagado',
  PEDIDO_REALIZADO: 'Pedido Realizado',
  LLEGO_BODEGA_USA: 'Llegó a bodega USA',
  EN_TRANSITO_ECUADOR: 'En tránsito a Ecuador',
  LLEGO_BODEGA_ECUADOR: 'Llegó a Ecuador',
  LISTO_ENTREGA: 'Listo para entrega',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado',
}

const shippingLabels: Record<ShippingOption, string> = {
  PICKUP: 'Retiro en bodega',
  UBER: 'Delivery Uber',
  SERVIENTREGA: 'Servientrega',
}

const roleColors: Record<string, string> = {
  USER: 'bg-zinc-500/20 text-zinc-400 border border-zinc-500/30',
  ADMIN: 'bg-teal-500/20 text-teal-400 border border-teal-500/30',
  SUPERADMIN: 'bg-amber-500/20 text-amber-400 border border-amber-500/30',
}

const roleLabels: Record<string, string> = {
  USER: 'Usuario',
  ADMIN: 'Admin',
  SUPERADMIN: 'Superadmin',
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('es-EC', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function formatCurrency(value: number | undefined): string {
  if (!value) return '$0.00'
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(value)
}

const statusOptions: { value: OrderStatus; label: string }[] = [
  { value: 'PENDIENTE_PAGO', label: 'Pendiente de Pago' },
  { value: 'PAGADO', label: 'Pagado' },
  { value: 'PEDIDO_REALIZADO', label: 'Pedido Realizado' },
  { value: 'LLEGO_BODEGA_USA', label: 'Llegó a bodega USA' },
  { value: 'EN_TRANSITO_ECUADOR', label: 'En tránsito a Ecuador' },
  { value: 'LLEGO_BODEGA_ECUADOR', label: 'Llegó a Ecuador' },
  { value: 'LISTO_ENTREGA', label: 'Listo para entrega' },
  { value: 'ENTREGADO', label: 'Entregado' },
  { value: 'CANCELADO', label: 'Cancelado' },
]

onMounted(() => {
  fetchUsers()
})
</script>

<template>
  <div class="admin-orders-page min-h-screen pt-[64px] pb-16">
    <!-- Background -->
    <div class="absolute inset-0 bg-gradient-to-b from-zinc-900 via-zinc-900 to-zinc-950 pointer-events-none" />
    <div class="absolute top-0 left-1/2 -translate-x-1/2 w-full h-96 bg-teal-500/5 blur-3xl" />

    <div class="relative max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header centered -->
      <FadeIn direction="up">
        <div class="text-center mb-8">
          <p class="text-zinc-400">Todos los pedidos organizados por cliente</p>
        </div>
      </FadeIn>

      <!-- Search input -->
      <FadeIn direction="up" :delay="50">
        <div class="mb-6">
          <div class="relative max-w-md mx-auto">
            <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
              <svg class="w-5 h-5 text-zinc-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Buscar por nombre o correo..."
              class="w-full pl-12 pr-4 py-3 bg-zinc-800/50 border border-zinc-700/50 rounded-xl text-white placeholder-zinc-500 focus:outline-none focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/20 transition-all"
            />
          </div>
        </div>
      </FadeIn>

      <!-- Loading users -->
      <FadeIn v-if="loadingUsers" direction="up">
        <div class="flex justify-center py-12">
          <div class="w-12 h-12 border-4 border-teal-500/20 border-t-teal-500 rounded-full animate-spin" />
        </div>
      </FadeIn>

      <!-- Users list -->
      <FadeIn v-else direction="up" :delay="100">
        <div class="space-y-3">
          <div 
            v-for="user in filteredUsers" 
            :key="user.id"
            class="glass rounded-2xl overflow-hidden border border-white/5 hover:border-white/10 transition-colors"
          >
            <div class="flex items-center justify-between p-5">
              <div class="flex items-center gap-4">
                <!-- Avatar -->
                <div class="w-11 h-11 rounded-full bg-gradient-to-br from-teal-500 to-teal-600 flex items-center justify-center text-white font-bold">
                  {{ user.name.charAt(0).toUpperCase() }}
                </div>
                
                <!-- User info -->
                <div>
                  <h3 class="text-base font-semibold text-white">{{ user.name }}</h3>
                  <p class="text-sm text-zinc-500">{{ user.email }}</p>
                </div>
              </div>

              <div class="flex items-center gap-4">
                <!-- Role badge -->
                <span :class="['px-3 py-1 rounded-full text-xs font-medium', roleColors[user.role]]">
                  {{ roleLabels[user.role] }}
                </span>

                <!-- Order count -->
                <div class="text-right hidden sm:block">
                  <p class="text-lg font-bold text-white">{{ user.orderCount }}</p>
                  <p class="text-[10px] text-zinc-500 uppercase tracking-wide">pedidos</p>
                </div>
                
                <!-- History button -->
                <button
                  @click="openHistoryModal(user)"
                  class="bg-teal-500/10 hover:bg-teal-500/20 text-teal-400 px-5 py-2.5 rounded-xl text-sm font-medium transition-colors border border-teal-500/20 hover:border-teal-500/30"
                >
                  Historial de pedidos
                </button>
              </div>
            </div>
          </div>

          <!-- Empty state -->
          <div v-if="filteredUsers.length === 0" class="glass rounded-2xl p-12 text-center border border-white/5">
            <div class="w-16 h-16 bg-zinc-800/50 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg class="w-8 h-8 text-zinc-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
            </div>
            <p v-if="searchQuery" class="text-zinc-400">No se encontraron resultados</p>
            <p v-else class="text-zinc-400">No hay usuarios registrados</p>
          </div>
        </div>
      </FadeIn>
    </div>

    <!-- History Modal -->
    <Teleport to="body">
      <div v-if="showHistoryModal" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="closeHistoryModal" />
        <FadeIn direction="scale" :duration="300">
          <div class="relative glass admin-orders-modal rounded-2xl border border-white/10 w-full h-full max-w-6xl mx-4 my-4 flex flex-col">
            <!-- Modal header -->
            <div class="flex items-center justify-between p-6 border-b border-white/10 shrink-0">
              <div>
                <h3 class="text-xl font-bold text-white">Historial de pedidos</h3>
                <p class="text-sm text-zinc-400 mt-0.5">{{ selectedUser?.name }} · {{ selectedUser?.email }}</p>
              </div>
              <button
                @click="closeHistoryModal"
                class="w-9 h-9 rounded-full bg-zinc-800/50 hover:bg-zinc-700/50 flex items-center justify-center text-zinc-400 hover:text-white transition-colors"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- Content area -->
            <div class="flex-1 overflow-y-auto">
              <!-- Filters -->
              <div class="p-4 border-b border-white/10 bg-zinc-900/50">
                <div class="flex flex-wrap gap-4 items-center">
                  <!-- Search by order ID -->
                  <div class="relative flex-1 min-w-[200px]">
                    <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <svg class="w-4 h-4 text-zinc-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                      </svg>
                    </div>
                    <input
                      v-model="orderSearchQuery"
                      type="text"
                      placeholder="Buscar por #ID o producto..."
                      class="w-full pl-10 pr-4 py-2 bg-zinc-800/80 border border-zinc-700/50 rounded-lg text-sm text-white placeholder-zinc-500 focus:outline-none focus:border-teal-500/50"
                    />
                  </div>

                  <!-- Date range filters -->
                  <div class="flex items-center gap-2">
                    <input
                      v-model="dateFrom"
                      type="date"
                      class="px-3 py-2 bg-zinc-800/80 border border-zinc-700/50 rounded-lg text-sm text-white focus:outline-none focus:border-teal-500/50"
                    />
                    <span class="text-zinc-500">-</span>
                    <input
                      v-model="dateTo"
                      type="date"
                      class="px-3 py-2 bg-zinc-800/80 border border-zinc-700/50 rounded-lg text-sm text-white focus:outline-none focus:border-teal-500/50"
                    />
                  </div>

                  <!-- Clear filters -->
                  <button
                    @click="clearFilters"
                    class="px-3 py-2 text-sm text-zinc-400 hover:text-white transition-colors"
                  >
                    Limpiar
                  </button>

                  <!-- Results count -->
                  <div class="text-sm text-zinc-500 ml-auto">
                    {{ filteredOrders.length }} pedidos
                  </div>
                </div>
              </div>

              <!-- Loading orders -->
              <div v-if="loadingOrders" class="flex justify-center py-12">
                <div class="w-10 h-10 border-4 border-teal-500/20 border-t-teal-500 rounded-full animate-spin" />
              </div>

              <!-- Orders table -->
              <div v-else-if="paginatedOrders.length > 0" class="p-6">
                <table class="w-full">
                  <thead class="bg-zinc-800/30 sticky top-0">
                    <tr>
                      <th class="px-4 py-3 text-left text-xs font-semibold text-zinc-400 uppercase tracking-wider">ID</th>
                      <th class="px-4 py-3 text-left text-xs font-semibold text-zinc-400 uppercase tracking-wider">Producto</th>
                      <th class="px-4 py-3 text-left text-xs font-semibold text-zinc-400 uppercase tracking-wider">Precio</th>
                      <th class="px-4 py-3 text-left text-xs font-semibold text-zinc-400 uppercase tracking-wider">Entrega</th>
                      <th class="px-4 py-3 text-left text-xs font-semibold text-zinc-400 uppercase tracking-wider">Estado</th>
                      <th class="px-4 py-3 text-left text-xs font-semibold text-zinc-400 uppercase tracking-wider">Fecha</th>
                      <th class="px-4 py-3 text-center text-xs font-semibold text-zinc-400 uppercase tracking-wider">Obs.</th>
                      <th class="px-4 py-3 text-center text-xs font-semibold text-zinc-400 uppercase tracking-wider">Acciones</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-white/5">
                    <tr 
                      v-for="order in paginatedOrders" 
                      :key="order.id" 
                      class="hover:bg-white/5 transition-colors"
                    >
                      <td class="px-4 py-3 whitespace-nowrap">
                        <span class="font-medium text-white">#{{ order.id }}</span>
                      </td>
                      <td class="px-4 py-3">
                        <div class="max-w-xs">
                          <p class="text-zinc-300 truncate">{{ order.productName || 'Producto Amazon' }}</p>
                          <p v-if="order.productAsin" class="text-xs text-zinc-500 font-mono">{{ order.productAsin }}</p>
                        </div>
                      </td>
                      <td class="px-4 py-3 whitespace-nowrap">
                        <span class="font-medium text-teal-400">{{ formatCurrency(order.totalPrice) }}</span>
                      </td>
                      <td class="px-4 py-3 whitespace-nowrap text-sm text-zinc-300">
                        {{ shippingLabels[order.shippingOption] || order.shippingOption }}
                      </td>
                      <td class="px-4 py-3 whitespace-nowrap">
                        <span :class="['px-2.5 py-1 rounded-full text-xs font-medium', statusColors[order.status]]">
                          {{ statusLabels[order.status] }}
                        </span>
                      </td>
                      <td class="px-4 py-3 whitespace-nowrap text-zinc-500 text-sm">
                        {{ formatDate(order.createdAt) }}
                      </td>
                      <td class="px-4 py-3 whitespace-nowrap text-center">
                        <button
                          @click="openObservationModal(order)"
                          class="w-8 h-8 rounded-lg bg-zinc-700/50 hover:bg-zinc-600 flex items-center justify-center text-zinc-300 hover:text-white transition-colors"
                          :title="order.notes ? 'Ver observaciones' : 'Agregar observación'"
                        >
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                          </svg>
                        </button>
                      </td>
                      <td class="px-4 py-3 whitespace-nowrap text-center">
                        <button
                          @click="openStatusModal(order)"
                          class="w-8 h-8 rounded-lg bg-teal-500/10 hover:bg-teal-500/20 flex items-center justify-center text-teal-400 transition-colors border border-teal-500/20 hover:border-teal-500/30"
                          title="Actualizar estado"
                        >
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                          </svg>
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>

                <!-- Pagination -->
                <div v-if="totalPages > 1" class="flex items-center justify-between mt-6 pt-4 border-t border-white/10">
                  <div class="text-sm text-zinc-400">
                    Página {{ currentPage }} de {{ totalPages }}
                  </div>
                  <div class="flex items-center gap-2">
                    <button
                      @click="prevPage"
                      :disabled="currentPage === 1"
                      class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed bg-zinc-800 hover:bg-zinc-700 text-white"
                    >
                      Anterior
                    </button>
                    
                    <!-- Page numbers -->
                    <div class="flex gap-1">
                      <button
                        v-for="page in totalPages"
                        :key="page"
                        @click="goToPage(page)"
                        :class="[
                          'w-8 h-8 rounded-lg text-sm font-medium transition-colors',
                          page === currentPage 
                            ? 'bg-teal-500 text-white' 
                            : 'bg-zinc-800 hover:bg-zinc-700 text-zinc-300'
                        ]"
                      >
                        {{ page }}
                      </button>
                    </div>

                    <button
                      @click="nextPage"
                      :disabled="currentPage === totalPages"
                      class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed bg-zinc-800 hover:bg-zinc-700 text-white"
                    >
                      Siguiente
                    </button>
                  </div>
                </div>
              </div>

              <!-- No orders or no results -->
              <div v-else class="flex flex-col items-center justify-center py-16">
                <div class="w-14 h-14 bg-zinc-800/50 rounded-full flex items-center justify-center mb-3">
                  <svg class="w-7 h-7 text-zinc-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                  </svg>
                </div>
                <p v-if="orderSearchQuery || dateFrom || dateTo" class="text-zinc-400">No se encontraron pedidos con esos filtros</p>
                <p v-else class="text-zinc-400">Este usuario no tiene pedidos</p>
              </div>
            </div>
          </div>
        </FadeIn>
      </div>
    </Teleport>

    <!-- Status Modal -->
    <Teleport to="body">
      <div v-if="showStatusModal" class="fixed inset-0 z-[60] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="showStatusModal = false" />
        <FadeIn direction="scale" :duration="300">
          <div class="relative glass admin-orders-modal rounded-2xl p-6 sm:p-8 max-w-md w-full border border-white/10">
            <h3 class="text-xl font-bold mb-2 text-white">Actualizar Estado</h3>
            <p class="text-zinc-400 mb-6">Pedido #{{ selectedOrder?.id }}</p>
            
            <div class="space-y-3 mb-6">
              <label
                v-for="option in statusOptions"
                :key="option.value"
                :class="[
                  'flex items-center p-4 rounded-xl border cursor-pointer transition-all',
                  newStatus === option.value
                    ? 'border-teal-500 bg-teal-500/10'
                    : 'border-zinc-700/50 hover:border-zinc-600 bg-zinc-800/50'
                ]"
              >
                <input
                  v-model="newStatus"
                  type="radio"
                  :value="option.value"
                  class="sr-only"
                />
                <div :class="[
                  'w-5 h-5 rounded-full border-2 mr-4 flex items-center justify-center transition-all',
                  newStatus === option.value ? 'border-teal-500' : 'border-zinc-600'
                ]">
                  <div v-if="newStatus === option.value" class="w-2.5 h-2.5 rounded-full bg-teal-500" />
                </div>
                <span class="text-white">{{ option.label }}</span>
              </label>
            </div>

            <div class="flex gap-4">
              <button
                @click="showStatusModal = false"
                class="flex-1 bg-zinc-700/50 hover:bg-zinc-700 py-3 rounded-xl font-medium transition-colors text-white border border-zinc-600/50"
              >
                Cancelar
              </button>
              <MotionButton
                @click="updateStatus"
                :disabled="orderStore.loading"
                label="Guardar"
                variant="primary"
                size="md"
                block
                class="flex-1"
              />
            </div>
          </div>
        </FadeIn>
      </div>
    </Teleport>

    <!-- Observation Modal -->
    <Teleport to="body">
      <div v-if="showObservationModal" class="fixed inset-0 z-[70] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="showObservationModal = false" />
        <FadeIn direction="scale" :duration="300">
          <div class="relative glass admin-orders-modal rounded-2xl p-6 sm:p-8 max-w-lg w-full border border-white/10">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-xl font-bold text-white">Observaciones del Pedido</h3>
              <button
                @click="showObservationModal = false"
                class="w-8 h-8 rounded-full bg-zinc-800/50 hover:bg-zinc-700/50 flex items-center justify-center text-zinc-400 hover:text-white transition-colors"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            
            <p class="text-zinc-400 mb-4">Pedido #{{ selectedOrder?.id }}</p>
            
            <div class="mb-4">
              <textarea
                v-model="observationText"
                placeholder="Escribe una observación para el cliente..."
                rows="4"
                class="w-full bg-zinc-800/80 border border-zinc-700/60 rounded-xl py-3 px-4 text-white placeholder-zinc-500 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all resize-none"
              />
              <p class="text-xs text-zinc-500 mt-2">
                Esta observación será visible para el cliente y recibida por correo electrónico.
              </p>
            </div>

            <div class="flex gap-4">
              <button
                @click="showObservationModal = false"
                class="flex-1 bg-zinc-700/50 hover:bg-zinc-700 py-3 rounded-xl font-medium transition-colors text-white border border-zinc-600/50"
              >
                Cancelar
              </button>
              <MotionButton
                @click="saveObservation"
                :disabled="savingObservation"
                :label="savingObservation ? 'Guardando...' : 'Guardar'"
                variant="primary"
                size="md"
                block
                class="flex-1"
              />
            </div>
          </div>
        </FadeIn>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.admin-orders-page {
  background:
    radial-gradient(circle at 14% 10%, rgba(53, 98, 122, 0.2), transparent 36%),
    radial-gradient(circle at 84% 14%, rgba(166, 169, 208, 0.16), transparent 34%),
    linear-gradient(180deg, #091019 0%, #050b12 100%);
}

.admin-orders-page .glass {
  background:
    linear-gradient(145deg, rgba(8, 21, 31, 0.94), rgba(8, 18, 28, 0.84)),
    rgba(8, 19, 29, 0.78);
  border: 1px solid rgba(166, 169, 208, 0.26) !important;
  box-shadow: 0 16px 34px rgba(0, 0, 0, 0.34);
}

.admin-orders-modal.glass {
  background:
    linear-gradient(145deg, rgba(8, 21, 31, 0.96), rgba(8, 18, 28, 0.9)),
    rgba(8, 19, 29, 0.82) !important;
  border: 1px solid rgba(166, 169, 208, 0.28) !important;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.45);
}

.admin-orders-page .text-zinc-500 {
  color: #aabdc7 !important;
}

.admin-orders-page .text-zinc-400 {
  color: #b7c9d1 !important;
}

.admin-orders-page .text-zinc-300 {
  color: #d6e2e8 !important;
}

.admin-orders-page .text-zinc-600 {
  color: #9fb4bf !important;
}

.admin-orders-modal .text-zinc-500 {
  color: #aabdc7 !important;
}

.admin-orders-modal .text-zinc-400 {
  color: #b7c9d1 !important;
}

.admin-orders-modal .text-zinc-300 {
  color: #d6e2e8 !important;
}

.admin-orders-modal .text-zinc-600 {
  color: #9fb4bf !important;
}

.admin-orders-page .text-teal-400 {
  color: #e5aea9 !important;
}

.admin-orders-page .text-teal-300 {
  color: #f2cbc7 !important;
}

.admin-orders-modal .text-teal-400 {
  color: #e5aea9 !important;
}

.admin-orders-modal .text-teal-300 {
  color: #f2cbc7 !important;
}

.admin-orders-page .border-white\/5 {
  border-color: rgba(166, 169, 208, 0.24) !important;
}

.admin-orders-modal .border-white\/10,
.admin-orders-modal .border-white\/5 {
  border-color: rgba(166, 169, 208, 0.24) !important;
}

.admin-orders-page :is(input, textarea) {
  color: #eef5f8 !important;
}

.admin-orders-modal :is(input, textarea) {
  color: #eef5f8 !important;
}

.admin-orders-page :is(input, textarea)::placeholder {
  color: #8fa5b0 !important;
}

.admin-orders-modal :is(input, textarea)::placeholder {
  color: #8fa5b0 !important;
}

.admin-orders-page :is(input, textarea):focus {
  border-color: #e5aea9 !important;
  box-shadow: 0 0 0 3px rgba(229, 174, 169, 0.2) !important;
}

.admin-orders-modal :is(input, textarea):focus {
  border-color: #e5aea9 !important;
  box-shadow: 0 0 0 3px rgba(229, 174, 169, 0.2) !important;
}

.admin-orders-page :is(thead .text-zinc-400, tbody .text-zinc-500) {
  color: #b4c7d0 !important;
}

.admin-orders-modal :is(thead .text-zinc-400, tbody .text-zinc-500) {
  color: #b4c7d0 !important;
}
</style>

