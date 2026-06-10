<template>
  <div class="data-table" :class="{ loading }">
    <div v-if="$slots.toolbar" class="table-toolbar">
      <slot name="toolbar" />
    </div>
    <div class="table-wrapper">
      <el-table
        :data="data"
        v-bind="$attrs"
        @selection-change="handleSelectionChange"
      >
        <slot />
      </el-table>
    </div>
    <div v-if="pagination" class="table-pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="currentPageSize"
        :total="total"
        :page-sizes="pageSizes"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  pagination: { type: Boolean, default: true },
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  pageSizes: { type: Array, default: () => [10, 20, 50, 100] }
})

const emit = defineEmits(['update:page', 'update:pageSize', 'pageChange', 'selectionChange'])

const currentPage = ref(props.page)
const currentPageSize = ref(props.pageSize)

watch(() => props.page, (val) => { currentPage.value = val })
watch(() => props.pageSize, (val) => { currentPageSize.value = val })

watch(currentPage, (val) => {
  emit('update:page', val)
  emit('pageChange', { page: val, pageSize: currentPageSize.value })
})

watch(currentPageSize, (val) => {
  emit('update:pageSize', val)
  emit('pageChange', { page: currentPage.value, pageSize: val })
})

const handleSizeChange = () => {
  currentPage.value = 1
}

const handleCurrentChange = () => {}

const handleSelectionChange = (selection) => {
  emit('selectionChange', selection)
}
</script>

<style scoped>
.data-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.table-toolbar {
  padding: var(--space-4);
  border-bottom: 1px solid var(--gray-100);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
}
.table-wrapper {
  padding: var(--space-2);
}
.table-pagination {
  padding: var(--space-4);
  border-top: 1px solid var(--gray-100);
  display: flex;
  justify-content: flex-end;
}
.data-table.loading {
  opacity: 0.7;
}
</style>
