import api from './api'
import type { User, UserResponse } from '@/types'

export const userService = {
  async getAllUsers(): Promise<UserResponse[]> {
    const response = await api.get<UserResponse[]>('/users')
    return response.data
  },

  async getUserById(id: number): Promise<UserResponse> {
    const response = await api.get<UserResponse>(`/users/${id}`)
    return response.data
  },

  async updateUserRole(userId: number, role: string): Promise<UserResponse> {
    const response = await api.put<UserResponse>(`/users/${userId}/role?role=${role}`)
    return response.data
  },

  async deleteUser(userId: number): Promise<void> {
    await api.delete(`/users/${userId}`)
  },

  async createAdmin(name: string, email: string, password: string): Promise<UserResponse> {
    const response = await api.post<UserResponse>(`/users/admin?name=${encodeURIComponent(name)}&email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`)
    return response.data
  },
}
