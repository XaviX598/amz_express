<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <div class="text-center mb-16 animate-slide-up">
    <h2 class="text-3xl sm:text-4xl lg:text-5xl font-bold mb-4 text-white">
      {{ title }}
    </h2>
    <p v-if="description" class="text-lg text-zinc-400 max-w-2xl mx-auto">
      {{ description }}
    </p>
  </div>
</template>
