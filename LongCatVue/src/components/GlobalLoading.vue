<template>
  <transition name="fade">
    <div v-if="loadingStore.isLoading" class="global-loading">
      <div class="loading-overlay"></div>
      <div class="loading-content">
        <div class="loading-spinner">
          <div class="spinner"></div>
        </div>
        <p v-if="loadingStore.globalMessage" class="loading-text">
          {{ loadingStore.globalMessage }}
        </p>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { useLoadingStore } from '../stores/loading'

const loadingStore = useLoadingStore()
</script>

<style scoped>
.global-loading {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(3px);
}

.loading-content {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
  background: var(--color-surface);
  padding: var(--space-6) var(--space-8);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  pointer-events: auto;
}

.loading-spinner {
  display: flex;
  align-items: center;
  justify-content: center;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--gray-200);
  border-top: 3px solid var(--primary-500);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
  text-align: center;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--duration-normal) var(--ease-out);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>