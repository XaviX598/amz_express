import api from './api'
import type { AmazonScrapeResponse, AmazonScrapeRequest } from '@/types'

export const amazonService = {
  async scrape(data: AmazonScrapeRequest): Promise<AmazonScrapeResponse> {
    const response = await api.post<AmazonScrapeResponse>('/amazon/scrape', data)
    return response.data
  },
}
