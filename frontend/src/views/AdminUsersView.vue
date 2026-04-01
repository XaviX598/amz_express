<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { userService } from '@/services/user'
import { useUiStore } from '@/stores/ui'
import type { UserResponse, Role } from '@/types'

const users = ref<UserResponse[]>([])
const uiStore = useUiStore()
const loading = ref(false)
const selectedUser = ref<UserResponse | null>(null)
const showRoleModal = ref(false)
const showDeleteModal = ref(false)
const showCreateModal = ref(false)
const newRole = ref<Role | null>(null)

// Form for creating admin
const newAdminForm = ref({
  name: '',
  email: '',
  password: '',
})
const createLoading = ref(false)

// Search query
const searchQuery = ref('')

// Filter: 'all' | 'admins' | 'users'
const filter = ref<'all' | 'admins' | 'users'>('all')

const filteredUsers = computed(() => {
  let result = users.value.filter(u => u.role !== 'SUPERADMIN') // Exclude SUPERADMIN from the list
  
  // Apply search filter
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(u => 
      u.name.toLowerCase().includes(query) || 
      u.email.toLowerCase().includes(query)
    )
  }
  
  // Apply role filter
  if (filter.value === 'all') return result
  if (filter.value === 'admins') {
    return result.filter(u => u.role === 'ADMIN')
  }
  return result.filter(u => u.role === 'USER')
})

const stats = computed(() => {
  const visibleUsers = users.value.filter(u => u.role !== 'SUPERADMIN')
  return {
    total: visibleUsers.length,
    admins: visibleUsers.filter(u => u.role === 'ADMIN').length,
    users: visibleUsers.filter(u => u.role === 'USER').length,
  }
})

const roleOptions: { value: Role; label: string }[] = [
  { value: 'USER', label: 'Usuario' },
  { value: 'ADMIN', label: 'Administrador' },
  { value: 'SUPERADMIN', label: 'Super Administrador' },
]

onMounted(async () => {
  loading.value = true
  try {
    users.value = await userService.getAllUsers()
  } finally {
    loading.value = false
  }
})

function openRoleModal(user: UserResponse) {
  selectedUser.value = user
  newRole.value = user.role
  showRoleModal.value = true
}

function openDeleteModal(user: UserResponse) {
  selectedUser.value = user
  showDeleteModal.value = true
}

async function updateRole() {
  if (!selectedUser.value || !newRole.value) return

  try {
    const updated = await userService.updateUserRole(selectedUser.value.id, newRole.value)
    const index = users.value.findIndex(u => u.id === selectedUser.value!.id)
    if (index !== -1) {
      users.value[index] = { ...users.value[index], role: updated.role }
    }
    showRoleModal.value = false
    selectedUser.value = null
  } catch (err) {
    uiStore.error(err instanceof Error ? err.message : 'Error al actualizar rol')
  }
}

async function deleteUser() {
  if (!selectedUser.value) return

  try {
    await userService.deleteUser(selectedUser.value.id)
    users.value = users.value.filter(u => u.id !== selectedUser.value!.id)
    showDeleteModal.value = false
    selectedUser.value = null
  } catch (err) {
    uiStore.error(err instanceof Error ? err.message : 'Error al eliminar usuario')
  }
}

async function createAdmin() {
  if (!newAdminForm.value.name || !newAdminForm.value.email || !newAdminForm.value.password) {
    uiStore.warning('Todos los campos son requeridos')
    return
  }

  createLoading.value = true
  try {
    const newAdmin = await userService.createAdmin(
      newAdminForm.value.name,
      newAdminForm.value.email,
      newAdminForm.value.password
    )
    users.value.unshift(newAdmin)
    showCreateModal.value = false
    newAdminForm.value = { name: '', email: '', password: '' }
    uiStore.success('Administrador creado correctamente')
  } catch (err) {
    uiStore.error(err instanceof Error ? err.message : 'Error al crear administrador')
  } finally {
    createLoading.value = false
  }
}

const roleColors: Record<Role, string> = {
  USER: 'bg-zinc-500/20 text-zinc-400 border border-zinc-500/30',
  ADMIN: 'bg-blue-500/20 text-blue-400 border border-blue-500/30',
  SUPERADMIN: 'bg-teal-500/20 text-teal-400 border border-teal-500/30',
}

const roleLabels: Record<Role, string> = {
  USER: 'Usuario',
  ADMIN: 'Administrador',
  SUPERADMIN: 'Super Admin',
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('es-EC', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function showOrderCount(role: Role): boolean {
  return role === 'USER'
}
</script>

<template>
  <div class="min-h-screen pt-[64px] pb-16">
    <!-- Background -->
    <div class="absolute inset-0 bg-gradient-to-b from-zinc-900 via-zinc-900 to-zinc-950 pointer-events-none" />
    <div class="absolute top-0 left-1/2 -translate-x-1/2 w-full h-96 bg-teal-500/5 blur-3xl" />

    <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <div class="animate-fade-in-up flex flex-col lg:flex-row lg:items-center justify-between gap-4 mb-8">
        <div>
          <h1 class="text-3xl font-bold text-white">Gestionar Usuarios</h1>
          <p class="text-zinc-400 mt-1">Administra los roles de los usuarios</p>
        </div>
        <button
          @click="showCreateModal = true"
          class="btn-primary flex items-center gap-2 group animate-bounce-subtle"
        >
          <svg class="w-5 h-5 transition-transform group-hover:rotate-90 animate-spin-once" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
          Crear Admin
        </button>
      </div>

      <!-- Stats & Filter Bar -->
      <div class="animate-fade-in-up glass rounded-2xl p-6 border border-white/5 mb-6" style="animation-delay: 0.1s">
        <div class="flex flex-col lg:flex-row lg:items-center gap-6">
          <!-- Stats -->
          <div class="flex items-center gap-4 lg:gap-6">
            <!-- Total -->
            <div class="relative group cursor-default">
              <div class="absolute inset-0 bg-teal-500/20 rounded-xl blur-lg opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
              <div class="relative bg-zinc-800/50 rounded-xl px-5 py-3 border border-white/5 transition-transform duration-300 group-hover:-translate-y-0.5">
                <p class="text-2xl font-bold text-white animate-count">{{ stats.total }}</p>
                <p class="text-xs text-zinc-500 uppercase tracking-wider">Total</p>
              </div>
            </div>
            
            <!-- Divider -->
            <div class="hidden lg:block w-px h-10 bg-zinc-700/50" />
            
            <!-- Admins -->
            <div class="relative group cursor-default">
              <div class="absolute inset-0 bg-blue-500/20 rounded-xl blur-lg opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
              <div class="relative bg-zinc-800/50 rounded-xl px-5 py-3 border border-blue-500/10 transition-transform duration-300 group-hover:-translate-y-0.5">
                <p class="text-2xl font-bold text-blue-400 animate-count">{{ stats.admins }}</p>
                <p class="text-xs text-zinc-500 uppercase tracking-wider">Admins</p>
              </div>
            </div>
            
            <!-- Divider -->
            <div class="hidden lg:block w-px h-10 bg-zinc-700/50" />
            
            <!-- Users -->
            <div class="relative group cursor-default">
              <div class="absolute inset-0 bg-zinc-500/20 rounded-xl blur-lg opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
              <div class="relative bg-zinc-800/50 rounded-xl px-5 py-3 border border-white/5 transition-transform duration-300 group-hover:-translate-y-0.5">
                <p class="text-2xl font-bold text-zinc-400 animate-count">{{ stats.users }}</p>
                <p class="text-xs text-zinc-500 uppercase tracking-wider">Users</p>
              </div>
            </div>
          </div>
          
          <!-- Search Bar -->
          <div class="flex-1 lg:max-w-sm lg:ml-auto">
            <div class="relative group">
              <div class="absolute inset-0 bg-teal-500/10 rounded-xl blur-sm opacity-0 group-focus-within:opacity-100 transition-opacity duration-300" />
              <div class="relative flex items-center bg-zinc-800/50 rounded-xl border border-zinc-700/50 focus-within:border-teal-500/50 transition-colors duration-300">
                <svg class="w-5 h-5 text-zinc-500 ml-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <input
                  v-model="searchQuery"
                  type="text"
                  placeholder="Buscar por nombre o email..."
                  class="w-full bg-transparent py-3 px-3 text-white placeholder-zinc-500 focus:outline-none"
                />
                <button 
                  v-if="searchQuery" 
                  @click="searchQuery = ''"
                  class="mr-3 text-zinc-500 hover:text-white transition-colors duration-200"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Filter Chips -->
        <div class="flex items-center gap-3 mt-6">
          <span class="text-xs text-zinc-600 uppercase tracking-wider font-medium">Filtrar:</span>
          <div class="flex items-center gap-2">
            <!-- All -->
            <button
              @click="filter = 'all'"
              :class="[
                'relative px-4 py-2 rounded-lg text-sm font-medium transition-all duration-300',
                filter === 'all'
                  ? 'bg-teal-500/20 text-teal-400 border border-teal-500/30 shadow-[0_0_15px_rgba(20,184,166,0.15)] scale-105'
                  : 'bg-zinc-800/50 text-zinc-400 border border-zinc-700/30 hover:text-white hover:border-zinc-600/50 hover:scale-105'
              ]"
            >
              <span class="flex items-center gap-2">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
                Todos
                <span :class="[
                  'px-1.5 py-0.5 rounded text-xs transition-colors duration-200',
                  filter === 'all' ? 'bg-teal-500/30' : 'bg-zinc-700/50'
                ]">
                  {{ stats.total }}
                </span>
              </span>
            </button>
            
            <!-- Admins -->
            <button
              @click="filter = 'admins'"
              :class="[
                'relative px-4 py-2 rounded-lg text-sm font-medium transition-all duration-300',
                filter === 'admins'
                  ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30 shadow-[0_0_15px_rgba(59,130,246,0.15)] scale-105'
                  : 'bg-zinc-800/50 text-zinc-400 border border-zinc-700/30 hover:text-white hover:border-zinc-600/50 hover:scale-105'
              ]"
            >
              <span class="flex items-center gap-2">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
                Administradores
                <span :class="[
                  'px-1.5 py-0.5 rounded text-xs transition-colors duration-200',
                  filter === 'admins' ? 'bg-blue-500/30' : 'bg-zinc-700/50'
                ]">
                  {{ stats.admins }}
                </span>
              </span>
            </button>
            
            <!-- Users -->
            <button
              @click="filter = 'users'"
              :class="[
                'relative px-4 py-2 rounded-lg text-sm font-medium transition-all duration-300',
                filter === 'users'
                  ? 'bg-zinc-500/20 text-zinc-300 border border-zinc-500/30 shadow-[0_0_15px_rgba(161,161,170,0.1)] scale-105'
                  : 'bg-zinc-800/50 text-zinc-400 border border-zinc-700/30 hover:text-white hover:border-zinc-600/50 hover:scale-105'
              ]"
            >
              <span class="flex items-center gap-2">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                Usuarios
                <span :class="[
                  'px-1.5 py-0.5 rounded text-xs transition-colors duration-200',
                  filter === 'users' ? 'bg-zinc-500/30' : 'bg-zinc-700/50'
                ]">
                  {{ stats.users }}
                </span>
              </span>
            </button>
          </div>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="animate-fade-in-up glass rounded-2xl p-12 border border-white/5" style="animation-delay: 0.2s">
        <div class="flex flex-col items-center justify-center">
          <div class="relative">
            <div class="w-16 h-16 border-4 border-zinc-800/50 rounded-full" />
            <div class="absolute inset-0 border-4 border-transparent border-t-teal-500 rounded-full animate-spin" />
          </div>
          <p class="text-zinc-500 mt-4 text-sm">Cargando usuarios...</p>
        </div>
      </div>

      <!-- Users Table -->
      <div v-else-if="filteredUsers.length > 0" class="animate-fade-in-up glass rounded-2xl overflow-hidden border border-white/5" style="animation-delay: 0.3s">
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead class="bg-zinc-800/50 border-b border-white/5">
              <tr>
                <th class="px-6 py-4 text-left text-xs font-semibold text-zinc-500 uppercase tracking-wider">ID</th>
                <th class="px-6 py-4 text-left text-xs font-semibold text-zinc-500 uppercase tracking-wider">Usuario</th>
                <th class="px-6 py-4 text-left text-xs font-semibold text-zinc-500 uppercase tracking-wider">Email</th>
                <th class="px-6 py-4 text-left text-xs font-semibold text-zinc-500 uppercase tracking-wider">Rol</th>
                <th class="px-6 py-4 text-left text-xs font-semibold text-zinc-500 uppercase tracking-wider">Pedidos</th>
                <th class="px-6 py-4 text-left text-xs font-semibold text-zinc-500 uppercase tracking-wider">Registrado</th>
                <th class="px-6 py-4 text-left text-xs font-semibold text-zinc-500 uppercase tracking-wider">Acciones</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-white/[0.03]">
              <tr 
                v-for="(user, index) in filteredUsers" 
                :key="user.id" 
                class="group hover:bg-white/[0.02] transition-all duration-200 animate-slide-in"
                :style="{ animationDelay: `${0.1 + index * 0.05}s` }"
              >
                <td class="px-6 py-4">
                  <span class="text-zinc-600 font-mono text-sm">#{{ user.id }}</span>
                </td>
                <td class="px-6 py-4">
                  <div class="flex items-center gap-3">
                    <div class="relative group/avatar">
                      <div class="absolute inset-0 bg-teal-500/30 rounded-full blur-md opacity-0 group-hover/avatar:opacity-100 transition-opacity duration-300" />
                      <div class="relative w-9 h-9 rounded-full bg-gradient-to-br from-teal-500 to-teal-600 flex items-center justify-center text-white text-sm font-bold ring-2 ring-zinc-800 transition-transform duration-300 group-hover/avatar:scale-110 group-hover/avatar:shadow-[0_0_15px_rgba(20,184,166,0.3)]">
                        {{ user.name.charAt(0).toUpperCase() }}
                      </div>
                    </div>
                    <span class="font-medium text-white">{{ user.name }}</span>
                  </div>
                </td>
                <td class="px-6 py-4">
                  <span class="text-zinc-400">{{ user.email }}</span>
                </td>
                <td class="px-6 py-4">
                  <span :class="['px-2.5 py-1 rounded-lg text-xs font-medium transition-transform duration-200 hover:scale-105', roleColors[user.role]]">
                    {{ roleLabels[user.role] }}
                  </span>
                </td>
                <td class="px-6 py-4">
                  <span v-if="showOrderCount(user.role)" class="text-zinc-400">
                    {{ user.orderCount }}
                  </span>
                  <span v-else class="text-zinc-700">—</span>
                </td>
                <td class="px-6 py-4">
                  <span class="text-zinc-500 text-sm">{{ formatDate(user.createdAt!) }}</span>
                </td>
                <td class="px-6 py-4">
                  <div class="flex items-center gap-2 opacity-50 group-hover:opacity-100 transition-opacity duration-200">
                    <!-- SUPERADMIN cannot edit or delete anyone (including themselves) -->
                    <template v-if="user.role !== 'SUPERADMIN'">
                      <button
                        @click="openRoleModal(user)"
                        class="p-2 rounded-lg bg-teal-500/10 hover:bg-teal-500/20 text-teal-400 border border-teal-500/20 hover:border-teal-500/30 transition-all duration-200 hover:scale-110 active:scale-95"
                        title="Editar rol"
                      >
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                        </svg>
                      </button>
                      <button
                        @click="openDeleteModal(user)"
                        class="p-2 rounded-lg bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 hover:border-red-500/30 transition-all duration-200 hover:scale-110 active:scale-95"
                        title="Eliminar usuario"
                      >
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                      </button>
                    </template>
                    <span v-else class="text-zinc-600 text-xs italic px-2">Protegido</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Table Footer -->
        <div class="px-6 py-4 bg-zinc-800/30 border-t border-white/5 flex items-center justify-between">
          <p class="text-zinc-500 text-sm">
            Mostrando <span class="text-white font-medium">{{ filteredUsers.length }}</span> de <span class="text-white font-medium">{{ users.length }}</span> usuarios
          </p>
          <p v-if="searchQuery" class="text-zinc-500 text-sm">
            Búsqueda: <span class="text-teal-400">"{{ searchQuery }}"</span>
          </p>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="animate-fade-in-up glass rounded-2xl p-16 border border-white/5 text-center" style="animation-delay: 0.3s">
        <div class="relative inline-block mb-6 animate-float">
          <div class="absolute inset-0 bg-teal-500/10 rounded-full blur-2xl" />
          <div class="relative w-20 h-20 bg-zinc-800/50 rounded-full flex items-center justify-center">
            <svg v-if="searchQuery" class="w-10 h-10 text-zinc-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <svg v-else class="w-10 h-10 text-zinc-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
        </div>
        <h3 class="text-xl font-semibold text-white mb-2">
          {{ searchQuery ? 'Sin resultados' : 'No hay usuarios' }}
        </h3>
        <p class="text-zinc-500 max-w-sm mx-auto">
          {{ searchQuery 
            ? `No encontramos usuarios que coincidan con "${searchQuery}"` 
            : filter === 'admins' 
              ? 'Aún no hay administradores registrados' 
              : filter === 'users' 
                ? 'Aún no hay usuarios registrados' 
                : 'Aún no hay usuarios registrados'
          }}
        </p>
        <button 
          v-if="searchQuery" 
          @click="searchQuery = ''"
          class="mt-6 px-4 py-2 rounded-lg bg-teal-500/10 text-teal-400 border border-teal-500/20 hover:bg-teal-500/20 transition-colors duration-200 text-sm hover:scale-105 active:scale-95"
        >
          Limpiar búsqueda
        </button>
      </div>
    </div>

    <!-- Create Admin Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showCreateModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="showCreateModal = false" />
          <div class="relative glass rounded-2xl p-6 sm:p-8 max-w-md w-full border border-white/10">
            <h3 class="text-xl font-bold mb-6 text-white">Crear Administrador</h3>
            
            <div class="space-y-4 mb-6">
              <div>
                <label class="block text-sm font-medium text-zinc-400 mb-2">Nombre</label>
                <input
                  v-model="newAdminForm.name"
                  type="text"
                  placeholder="Nombre completo"
                  class="w-full bg-zinc-800/50 border border-zinc-700/50 rounded-xl px-4 py-3 text-white placeholder-zinc-500 focus:outline-none focus:border-teal-500 transition-colors duration-200 focus:scale-[1.01]"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-zinc-400 mb-2">Email</label>
                <input
                  v-model="newAdminForm.email"
                  type="email"
                  placeholder="admin@email.com"
                  class="w-full bg-zinc-800/50 border border-zinc-700/50 rounded-xl px-4 py-3 text-white placeholder-zinc-500 focus:outline-none focus:border-teal-500 transition-colors duration-200 focus:scale-[1.01]"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-zinc-400 mb-2">Contraseña</label>
                <input
                  v-model="newAdminForm.password"
                  type="password"
                  placeholder="••••••••"
                  class="w-full bg-zinc-800/50 border border-zinc-700/50 rounded-xl px-4 py-3 text-white placeholder-zinc-500 focus:outline-none focus:border-teal-500 transition-colors duration-200 focus:scale-[1.01]"
                />
              </div>
            </div>

            <div class="flex gap-4">
              <button
                @click="showCreateModal = false"
                class="flex-1 bg-zinc-700/50 hover:bg-zinc-700 py-3 rounded-xl font-medium transition-all duration-200 text-white border border-zinc-600/50 hover:scale-[1.02] active:scale-[0.98]"
              >
                Cancelar
              </button>
              <button
                @click="createAdmin"
                :disabled="createLoading"
                class="flex-1 btn-primary disabled:opacity-50 transition-all duration-200 hover:scale-[1.02] active:scale-[0.98]"
              >
                {{ createLoading ? 'Creando...' : 'Crear' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Edit Role Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showRoleModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="showRoleModal = false" />
          <div class="relative glass rounded-2xl p-6 sm:p-8 max-w-md w-full border border-white/10">
            <h3 class="text-xl font-bold mb-2 text-white">Editar Usuario</h3>
            <p class="text-zinc-400 mb-6">{{ selectedUser?.name }}</p>
            
            <div class="space-y-3 mb-6">
              <label
                v-for="option in roleOptions"
                :key="option.value"
                :class="[
                  'flex items-center p-4 rounded-xl border cursor-pointer transition-all duration-200',
                  newRole === option.value
                    ? 'border-teal-500 bg-teal-500/10 scale-[1.02]'
                    : 'border-zinc-700/50 hover:border-zinc-600 bg-zinc-800/50 hover:scale-[1.01]'
                ]"
              >
                <input
                  v-model="newRole"
                  type="radio"
                  :value="option.value"
                  class="sr-only"
                />
                <div :class="[
                  'w-5 h-5 rounded-full border-2 mr-4 flex items-center justify-center transition-all duration-200',
                  newRole === option.value ? 'border-teal-500 scale-110' : 'border-zinc-600'
                ]">
                  <div v-if="newRole === option.value" class="w-2.5 h-2.5 rounded-full bg-teal-500 animate-pop-in" />
                </div>
                <span class="text-white">{{ option.label }}</span>
              </label>
            </div>

            <div class="flex gap-4">
              <button
                @click="showRoleModal = false"
                class="flex-1 bg-zinc-700/50 hover:bg-zinc-700 py-3 rounded-xl font-medium transition-all duration-200 text-white border border-zinc-600/50 hover:scale-[1.02] active:scale-[0.98]"
              >
                Cancelar
              </button>
              <button
                @click="updateRole"
                class="flex-1 btn-primary transition-all duration-200 hover:scale-[1.02] active:scale-[0.98]"
              >
                Guardar
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Delete Confirmation Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showDeleteModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="showDeleteModal = false" />
          <div class="relative glass rounded-2xl p-6 sm:p-8 max-w-md w-full border border-white/10">
            <div class="text-center mb-6">
              <div class="w-16 h-16 bg-red-500/20 rounded-full flex items-center justify-center mx-auto mb-4 animate-bounce-in">
                <svg class="w-8 h-8 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </div>
              <h3 class="text-xl font-bold mb-2 text-white">Eliminar Usuario</h3>
              <p class="text-zinc-400">
                ¿Estás seguro de eliminar a <span class="text-white font-medium">{{ selectedUser?.name }}</span>? Esta acción no se puede deshacer.
              </p>
            </div>

            <div class="flex gap-4">
              <button
                @click="showDeleteModal = false"
                class="flex-1 bg-zinc-700/50 hover:bg-zinc-700 py-3 rounded-xl font-medium transition-all duration-200 text-white border border-zinc-600/50 hover:scale-[1.02] active:scale-[0.98]"
              >
                Cancelar
              </button>
              <button
                @click="deleteUser"
                class="flex-1 bg-red-500 hover:bg-red-600 py-3 rounded-xl font-medium transition-all duration-200 text-white hover:scale-[1.02] active:scale-[0.98]"
              >
                Eliminar
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* Animations */
@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slide-in {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@keyframes bounce-in {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes pop-in {
  0% {
    transform: scale(0);
  }
  70% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

@keyframes count {
  0% {
    transform: scale(1.2);
    color: #14b8a6;
  }
  100% {
    transform: scale(1);
    color: inherit;
  }
}

@keyframes spin-once {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(90deg);
  }
}

.animate-fade-in-up {
  animation: fade-in-up 0.5s ease-out forwards;
}

.animate-slide-in {
  animation: slide-in 0.4s ease-out forwards;
  opacity: 0;
}

.animate-float {
  animation: float 2s ease-in-out infinite;
}

.animate-bounce-in {
  animation: bounce-in 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55) forwards;
}

.animate-pop-in {
  animation: pop-in 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55) forwards;
}

.animate-count {
  animation: count 0.3s ease-out forwards;
}

.animate-spin-once:hover svg {
  animation: spin-once 0.3s ease-out forwards;
}

/* Modal transitions */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from > div:last-child,
.modal-leave-to > div:last-child {
  transform: scale(0.9) translateY(20px);
}

.modal-enter-active > div:last-child,
.modal-leave-active > div:last-child {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
</style>

