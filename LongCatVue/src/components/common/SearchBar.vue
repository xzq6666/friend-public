<template>
  <div class="search-bar">
    <div class="search-input-wrapper">
      <el-icon class="search-icon"><Search /></el-icon>
      <input
        v-model="inputValue"
        type="text"
        class="search-input"
        :placeholder="placeholder"
        @keyup.enter="handleSearch"
      />
      <button v-if="inputValue" class="clear-btn" @click="clearSearch">
        <el-icon :size="14"><Close /></el-icon>
      </button>
    </div>
    <el-button v-if="showButton" type="primary" @click="handleSearch">
      搜索
    </el-button>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Search, Close } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '搜索...' },
  showButton: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue', 'search', 'clear'])

const inputValue = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
  inputValue.value = val
})

watch(inputValue, (val) => {
  emit('update:modelValue', val)
})

const handleSearch = () => {
  emit('search', inputValue.value)
}

const clearSearch = () => {
  inputValue.value = ''
  emit('clear')
}
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: var(--space-2);
}
.search-input-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}
.search-icon {
  position: absolute;
  left: var(--space-3);
  color: var(--gray-400);
  z-index: 1;
}
.search-input {
  width: 100%;
  height: 36px;
  padding: 0 var(--space-8) 0 var(--space-8);
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--gray-800);
  background: var(--color-surface);
  transition: all var(--duration-fast) var(--ease-out);
  outline: none;
}
.search-input::placeholder {
  color: var(--gray-400);
}
.search-input:focus {
  border-color: var(--gray-400);
  box-shadow: 0 0 0 3px var(--gray-100);
}
.clear-btn {
  position: absolute;
  right: var(--space-2);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: none;
  background: var(--gray-200);
  border-radius: var(--radius-full);
  cursor: pointer;
  color: var(--gray-500);
  transition: all var(--duration-fast) var(--ease-out);
}
.clear-btn:hover {
  background: var(--gray-300);
  color: var(--gray-700);
}
</style>
