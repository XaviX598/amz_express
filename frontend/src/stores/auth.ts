import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginRequest, RegisterRequest } from '@/types'
import { authService } from '@/services/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN' || user.value?.role === 'SUPERADMIN')
  const isSuperAdmin = computed(() => user.value?.role === 'SUPERADMIN')

  function checkAuth() {
    const storedToken = localStorage.getItem('token')
    const storedUser = localStorage.getItem('user')
    
    if (storedToken && storedUser) {
      token.value = storedToken
      user.value = JSON.parse(storedUser)
    }
  }

  async function login(data: LoginRequest) {
    loading.value = true
    error.value = null
    
    try {
      const response = await authService.login(data)
      token.value = response.token
      user.value = {
        id: response.userId,
        name: response.name,
        email: response.email,
        role: response.role as User['role'],
      }
      
      localStorage.setItem('token', response.token)
      localStorage.setItem('user', JSON.stringify(user.value))
      
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Error al iniciar sesión'
      return false
    } finally {
      loading.value = false
    }
  }

  async function register(data: RegisterRequest) {
    loading.value = true
    error.value = null
    
    try {
      const response = await authService.register(data)
      // Returns requiresVerification flag, actual auth happens after verification
      return response.requiresVerification
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Error al registrarse'
      return false
    } finally {
      loading.value = false
    }
  }

  function logout() {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return {
    user,
    token,
    loading,
    error,
    isAuthenticated,
    isAdmin,
    isSuperAdmin,
    checkAuth,
    login,
    register,
    logout,
  }
})

