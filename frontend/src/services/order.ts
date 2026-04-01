import api from './api'
import type { Order, CreateOrderRequest, UpdateOrderStatusRequest } from '@/types'

interface TransferProofPayload {
  orderIds: number[]
  bankCode: string
  bankName: string
  accountNumber: string
  accountHolder: string
  proofFile: File
}

export const orderService = {
  async getMyOrders(): Promise<Order[]> {
    const response = await api.get<Order[]>('/orders')
    return response.data
  },

  async getOrderById(id: number): Promise<Order> {
    const response = await api.get<Order>(`/orders/${id}`)
    return response.data
  },

  async createOrder(data: CreateOrderRequest): Promise<Order> {
    const response = await api.post<Order>('/orders', data)
    return response.data
  },

  async getAllOrders(): Promise<Order[]> {
    const response = await api.get<Order[]>('/orders/admin/all')
    return response.data
  },

  async getOrdersByUser(userId: number): Promise<Order[]> {
    const response = await api.get<Order[]>(`/orders/admin/user/${userId}`)
    return response.data
  },

  async updateOrderStatus(id: number, data: UpdateOrderStatusRequest): Promise<Order> {
    const response = await api.put<Order>(`/orders/admin/${id}/status`, data)
    return response.data
  },

  async submitTransferProof(payload: TransferProofPayload): Promise<void> {
    const formData = new FormData()
    payload.orderIds.forEach((orderId) => {
      formData.append('orderIds', String(orderId))
    })
    formData.append('bankCode', payload.bankCode)
    formData.append('bankName', payload.bankName)
    formData.append('accountNumber', payload.accountNumber)
    formData.append('accountHolder', payload.accountHolder)
    formData.append('proofFile', payload.proofFile)

    await api.post('/orders/transfer-proof', formData)
  },
}
