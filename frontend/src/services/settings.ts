import api from './api'
import type { CalculatorSettings, UpdateSettingsRequest } from '@/types'

export const settingsService = {
  async getAll(): Promise<CalculatorSettings> {
    const response = await api.get<{ settings: CalculatorSettings }>('/admin/settings')
    return response.data.settings
  },

  async update(data: UpdateSettingsRequest): Promise<CalculatorSettings> {
    const response = await api.put<{ settings: CalculatorSettings }>('/admin/settings', data)
    return response.data.settings
  },
}
