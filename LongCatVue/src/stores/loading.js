import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useLoadingStore = defineStore('loading', () => {
  const loadingCount = ref(0)
  const globalMessage = ref('')
  const isLoading = computed(() => loadingCount.value > 0)

  const startLoading = (message = '') => {
    loadingCount.value++
    if (message) {
      globalMessage.value = message
    }
  }

  const stopLoading = () => {
    loadingCount.value = Math.max(0, loadingCount.value - 1)
    if (loadingCount.value === 0) {
      globalMessage.value = ''
    }
  }

  const resetLoading = () => {
    loadingCount.value = 0
    globalMessage.value = ''
  }

  return {
    loadingCount,
    globalMessage,
    isLoading,
    startLoading,
    stopLoading,
    resetLoading
  }
})