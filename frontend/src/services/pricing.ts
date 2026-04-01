import api from './api'
import type { PricingCalculation, PricingRequest } from '@/types'

export const pricingService = {
  async calculate(data: PricingRequest): Promise<PricingCalculation> {
    const response = await api.post<PricingCalculation>('/pricing/calculate', data)
    return response.data
  },

  async calculateGet(productPrice: number, weight?: number, shippingOption?: string): Promise<PricingCalculation> {
    const params = new URLSearchParams()
    params.append('productPrice', productPrice.toString())
    if (weight) params.append('weight', weight.toString())
    if (shippingOption) params.append('shippingOption', shippingOption)
    
    const response = await api.get<PricingCalculation>(`/pricing/calculate?${params.toString()}`)
    return response.data
  },
}
