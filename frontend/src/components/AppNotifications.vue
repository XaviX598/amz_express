<script setup lang="ts">
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()

function toastClass(type: string) {
  if (type === 'success') return 'border-emerald-500/40 bg-emerald-500/15 text-emerald-200'
  if (type === 'error') return 'border-red-500/40 bg-red-500/15 text-red-200'
  if (type === 'warning') return 'border-amber-500/40 bg-amber-500/15 text-amber-200'
  return 'border-sky-500/40 bg-sky-500/15 text-sky-200'
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed right-4 top-[78px] z-[120] w-[min(92vw,420px)] space-y-2 pointer-events-none">
      <TransitionGroup name="toast">
        <div
          v-for="toast in ui.toasts"
          :key="toast.id"
          :class="['pointer-events-auto rounded-xl border px-4 py-3 shadow-xl backdrop-blur-md', toastClass(toast.type)]"
        >
          <div class="flex items-start gap-3">
            <p class="text-sm font-medium leading-5">{{ toast.message }}</p>
            <button
              type="button"
              class="ml-auto rounded-md p-1 text-zinc-300 hover:text-white hover:bg-white/10 transition-colors"
              @click="ui.removeToast(toast.id)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
      </TransitionGroup>
    </div>

    <Transition name="confirm-fade">
      <div v-if="ui.confirmOpen" class="fixed inset-0 z-[130] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="ui.resolveConfirm(false)" />
        <div class="relative w-full max-w-md rounded-2xl border border-white/10 bg-zinc-900/95 p-6 shadow-2xl">
          <h3 class="text-lg font-semibold text-white mb-2">{{ ui.confirmTitle }}</h3>
          <p class="text-zinc-300 text-sm leading-6 mb-6">{{ ui.confirmMessage }}</p>

          <div class="flex gap-3">
            <button
              type="button"
              class="flex-1 rounded-xl border border-zinc-600 bg-zinc-800/80 py-2.5 text-sm font-medium text-zinc-200 hover:bg-zinc-700/80 transition-colors"
              @click="ui.resolveConfirm(false)"
            >
              {{ ui.cancelText }}
            </button>
            <button
              type="button"
              :class="[
                'flex-1 rounded-xl py-2.5 text-sm font-semibold text-white transition-colors',
                ui.confirmDanger ? 'bg-red-600 hover:bg-red-700' : 'bg-teal-600 hover:bg-teal-700'
              ]"
              @click="ui.resolveConfirm(true)"
            >
              {{ ui.confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 220ms ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.confirm-fade-enter-active,
.confirm-fade-leave-active {
  transition: opacity 180ms ease;
}

.confirm-fade-enter-from,
.confirm-fade-leave-to {
  opacity: 0;
}
</style>
