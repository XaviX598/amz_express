import axios from './api'

export interface CartItem {
  id?: number
  productName: string
  productPrice: number
  weight: number
  source: 'url' | 'manual'
}

export const cartService = {
  async getCart(): Promise<CartItem[]> {
    const response = await axios.get('/cart')
    return response.data
  },

  async addItem(item: CartItem): Promise<void> {
    await axios.post('/cart/add', {
      productName: item.productName,
      productPrice: item.productPrice,
      weight: item.weight,
      source: item.source
    })
  },

  async removeItem(cartId: number): Promise<void> {
    await axios.delete(`/cart/${cartId}`)
  },

  async clearCart(): Promise<void> {
    await axios.delete('/cart/clear')
  },

  async saveAllItems(items: CartItem[]): Promise<void> {
    await axios.post('/cart/save-all', items.map(item => ({
      productName: item.productName,
      productPrice: item.productPrice,
      weight: item.weight,
      source: item.source
    })))
  }
}