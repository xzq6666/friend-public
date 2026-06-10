<template>
<div class="profile-page animate-fade-in">
  <PageHeader
    icon="User"
    title="个人中心"
    description="管理您的账号信息和安全设置"
  />

  <div class="profile-layout">
    <div class="profile-left">
      <ProfileSidebar class="stagger-item" />
      <AnnouncementPanel v-if="!profileData.isAdmin.value" class="stagger-item" />
      <VisitHistoryPanel v-if="!profileData.isAdmin.value" class="stagger-item" />
      <ReportHistoryPanel v-if="!profileData.isAdmin.value" class="stagger-item" />
    </div>
    <div class="profile-content">
      <StatsOverview class="stagger-item" />
      <RecentActivity class="stagger-item" />
      <SecurityStatus class="stagger-item" />
      <NotificationPrefs class="stagger-item" />
      <QuickLinks class="stagger-item" />
    </div>
  </div>
</div>
</template>

<script setup>
import { provide, onMounted } from 'vue'
import { PageHeader } from '../../components/common'
import { useProfileData } from '../../composables/useProfileData'
import ProfileSidebar from './ProfileSidebar.vue'
import AnnouncementPanel from './AnnouncementPanel.vue'
import VisitHistoryPanel from './VisitHistoryPanel.vue'
import ReportHistoryPanel from './ReportHistoryPanel.vue'
import StatsOverview from './StatsOverview.vue'
import RecentActivity from './RecentActivity.vue'
import SecurityStatus from './SecurityStatus.vue'
import NotificationPrefs from './NotificationPrefs.vue'
import QuickLinks from './QuickLinks.vue'

const profileData = useProfileData()
provide('profileData', profileData)

onMounted(() => {
  profileData.initProfile()
})
</script>

<style scoped>
.profile-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--space-6) var(--space-5);
}
.profile-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: var(--space-5);
  align-items: start;
}
.profile-left {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}
.profile-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-5);
}
.profile-content > :first-child {
  grid-column: 1 / -1;
}

@media (max-width: 1100px) {
  .profile-content {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 900px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 640px) {
  .profile-page {
    padding: var(--space-3) var(--space-2);
  }
}
</style>
