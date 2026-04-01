import api from './api'
import type { AuthResponse, LoginRequest, RegisterRequest } from '@/types'

export interface RegisterResponse {
  requiresVerification: boolean
  email: string
  message: string
}

export interface VerifyResponse {
  token: string
  userId: number
  name: string
  email: string
  role: string
}

export interface ValidateResetTokenResponse {
  valid: boolean
  message: string
}

export const authService = {
  async login(data: LoginRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/login', data)
    return response.data
  },

  async register(data: RegisterRequest): Promise<RegisterResponse> {
    const response = await api.post<RegisterResponse>('/auth/register', data)
    return response.data
  },

  async verifyCode(email: string, code: string): Promise<VerifyResponse> {
    const response = await api.post<VerifyResponse>('/auth/verify', { email, code })
    return response.data
  },

  async resendCode(email: string): Promise<{ message: string }> {
    const response = await api.post<{ message: string }>('/auth/resend', { email })
    return response.data
  },

  async forgotPassword(email: string): Promise<{ message: string }> {
    const response = await api.post<{ message: string }>('/auth/forgot-password', { email })
    return response.data
  },

  async validateResetToken(token: string): Promise<ValidateResetTokenResponse> {
    const response = await api.get<ValidateResetTokenResponse>('/auth/reset-password/validate', {
      params: { token },
    })
    return response.data
  },

  async resetPassword(data: { token: string; newPassword: string; confirmPassword: string }): Promise<{ message: string }> {
    const response = await api.post<{ message: string }>('/auth/reset-password', data)
    return response.data
  },
}
