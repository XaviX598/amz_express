import axios from './api'

export interface Address {
  id?: number
  addressType: 'SHIPPING' | 'BILLING'
  street: string
  city: string
  state: string
  postalCode: string
  country: string
  phone: string
  isDefault: boolean
}

export const addressService = {
  async getAll(): Promise<Address[]> {
    const response = await axios.get('/addresses')
    return response.data
  },

  async getShipping(): Promise<Address[]> {
    const response = await axios.get('/addresses/shipping')
    return response.data
  },

  async getBilling(): Promise<Address[]> {
    const response = await axios.get('/addresses/billing')
    return response.data
  },

  async getDefaultShipping(): Promise<Address | null> {
    try {
      const response = await axios.get('/addresses/default-shipping')
      return response.data
    } catch {
      return null
    }
  },

  async create(address: Omit<Address, 'id'>): Promise<Address> {
    const response = await axios.post('/addresses', address)
    return response.data
  },

  async update(id: number, address: Omit<Address, 'id'>): Promise<Address> {
    const response = await axios.put(`/addresses/${id}`, address)
    return response.data
  },

  async delete(id: number): Promise<void> {
    await axios.delete(`/addresses/${id}`)
  }
}