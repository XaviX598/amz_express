import axios, { type AxiosError, type AxiosResponse } from 'axios'
import type { ApiError } from '@/types'

const configuredApiUrl = (import.meta.env.VITE_API_URL || '').trim()
const API_BASE_URL = import.meta.env.PROD
  ? '/api'
  : configuredApiUrl || 'http://localhost:8080/api'

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    // Let the browser set multipart boundaries automatically for FormData
    if (config.data instanceof FormData && config.headers) {
      delete config.headers['Content-Type']
    }

    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor for error handling
api.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.data?.message) {
      throw new Error(error.response.data.message)
    }
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    throw error
  }
)

export default api
