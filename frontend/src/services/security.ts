import api from './api'

export const securityService = {
  async verifySecurityCode(securityCode: string): Promise<boolean> {
    const response = await api.post<{ valid: boolean }>('/security-code/verify', {
      securityCode,
    })
    return response.data.valid === true
  },
}
