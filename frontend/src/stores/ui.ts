import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ToastType = 'success' | 'error' | 'info' | 'warning'

export interface ToastItem {
  id: number
  message: string
  type: ToastType
}

export interface ConfirmOptions {
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  danger?: boolean
}

export const useUiStore = defineStore('ui', () => {
  const toasts = ref<ToastItem[]>([])
  const confirmOpen = ref(false)
  const confirmTitle = ref('Confirmar')
  const confirmMessage = ref('')
  const confirmText = ref('Confirmar')
  const cancelText = ref('Cancelar')
  const confirmDanger = ref(false)

  let nextToastId = 1
  let confirmResolver: ((value: boolean) => void) | null = null

  function removeToast(id: number) {
    toasts.value = toasts.value.filter((item) => item.id !== id)
  }

  function addToast(message: string, type: ToastType = 'info', timeoutMs = 3800) {
    const id = nextToastId++
    toasts.value.push({ id, message, type })
    setTimeout(() => {
      removeToast(id)
    }, timeoutMs)
  }

  function success(message: string) {
    addToast(message, 'success')
  }

  function error(message: string) {
    addToast(message, 'error', 5000)
  }

  function info(message: string) {
    addToast(message, 'info')
  }

  function warning(message: string) {
    addToast(message, 'warning', 4500)
  }

  function openConfirm(options: ConfirmOptions): Promise<boolean> {
    if (confirmResolver) {
      confirmResolver(false)
      confirmResolver = null
    }

    confirmTitle.value = options.title || 'Confirmar'
    confirmMessage.value = options.message
    confirmText.value = options.confirmText || 'Confirmar'
    cancelText.value = options.cancelText || 'Cancelar'
    confirmDanger.value = !!options.danger
    confirmOpen.value = true

    return new Promise((resolve) => {
      confirmResolver = resolve
    })
  }

  function resolveConfirm(value: boolean) {
    confirmOpen.value = false
    if (confirmResolver) {
      confirmResolver(value)
      confirmResolver = null
    }
  }

  return {
    toasts,
    confirmOpen,
    confirmTitle,
    confirmMessage,
    confirmText,
    cancelText,
    confirmDanger,
    addToast,
    removeToast,
    success,
    error,
    info,
    warning,
    openConfirm,
    resolveConfirm,
  }
})
