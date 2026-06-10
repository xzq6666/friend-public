<template>
  <EmployerDashboard v-if="isEmployer" />
  <AdminDashboard v-else-if="isAdmin" />
  <EmployeeDashboard v-else />
</template>

<script>
export default { name: 'UserDashboard' }
</script>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '../stores/user'
import EmployerDashboard from './dashboard/EmployerDashboard.vue'
import AdminDashboard from './dashboard/AdminDashboard.vue'
import EmployeeDashboard from './dashboard/EmployeeDashboard.vue'

const userStore = useUserStore()
const isEmployer = computed(() => userStore.user?.userType === 'EMPLOYER')
const isAdmin = computed(() => userStore.user?.userType === 'ADMIN')
</script>
