// 通用页面组件库
// 使用方式：import { PageHeader, EmptyState } from '@/components/common'

export { default as PageHeader } from './PageHeader.vue'
export { default as EmptyState } from './EmptyState.vue'
export { default as StatCard } from './StatCard.vue'
export { default as SearchBar } from './SearchBar.vue'
export { default as FilterPanel } from './FilterPanel.vue'
export { default as DataTable } from './DataTable.vue'
export { default as StatusBadge } from './StatusBadge.vue'
export { default as QuickActions } from './QuickActions.vue'

// 类型定义
export interface NavItem {
  path: string
  label: string
  icon: string
}

export interface StatItem {
  label: string
  value: number | string
  route?: string
}

export interface ActionItem {
  key: string
  label: string
  icon: string
  disabled?: boolean
  iconStyle?: Record<string, string>
}
