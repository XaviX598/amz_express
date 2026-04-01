import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Order, CreateOrderRequest, UpdateOrderStatusRequest, PricingCalculation, PricingRequest } from '@/types'
import { orderService } from '@/services/order'
import { pricingService } from '@/services/pricing'

export const useOrderStore = defineStore('order', () => {
  const orders = ref<Order[]>([])
  const currentOrder = ref<Order | null>(null)
  const pricing = ref<PricingCalculation | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchMyOrders() {
    loading.value = true
    error.value = null
    
    try {
      orders.value = await orderService.getMyOrders()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Error al cargar órdenes'
    } finally {
      loading.value = false
    }
  }

  async function fetchOrderById(id: number) {
    loading.value = true
    error.value = null
    
    try {
      currentOrder.value = await orderService.getOrderById(id)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Error al cargar orden'
    } finally {
      loading.value = false
    }
  }

  async function createOrder(data: CreateOrderRequest) {
    loading.value = true
    error.value = null
    
    try {
      const order = await orderService.createOrder(data)
      orders.value.unshift(order)
      return order
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Error al crear orden'
      return null
    } finally {
      loading.value = false
    }
  }

  async function fetchAllOrders() {
    loading.value = true
    error.value = null
    
    try {
      orders.value = await orderService.getAllOrders()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Error al cargar órdenes'
    } finally {
      loading.value = false
    }
  }

  async function fetchOrdersByUser(userId: number) {
    loading.value = true
    error.value = null
    
    try {
      orders.value = await orderService.getOrdersByUser(userId)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Error al cargar órdenes'
    } finally {
      loading.value = false
    }
  }

  async function updateOrderStatus(orderId: number, data: UpdateOrderStatusRequest) {
    loading.value = true
    error.value = null
    
    try {
      const updated = await orderService.updateOrderStatus(orderId, data)
      const index = orders.value.findIndex(o => o.id === orderId)
      if (index !== -1) {
        orders.value[index] = updated
      }
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Error al actualizar estado'
      return null
    } finally {
      loading.value = false
    }
  }

  async function calculatePricing(data: PricingRequest) {
    try {
      pricing.value = await pricingService.calculate(data)
      return pricing.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Error al calcular precio'
      return null
    }
  }

  function clearPricing() {
    pricing.value = null
  }

  return {
    orders,
    currentOrder,
    pricing,
    loading,
    error,
    fetchMyOrders,
    fetchOrderById,
    createOrder,
    fetchAllOrders,
    fetchOrdersByUser,
    updateOrderStatus,
    calculatePricing,
    clearPricing,
  }
})

