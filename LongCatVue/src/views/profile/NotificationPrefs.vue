<template>
<div class="content-card">
  <div class="card-title">通知偏好</div>
  <div v-loading="data.prefsLoading.value" class="prefs-list">
    <div class="pref-row">
      <div class="pref-left">
        <div class="pref-icon"><el-icon><Message /></el-icon></div>
        <div class="pref-text"><h4>邮件通知</h4><p>面试邀请、申请状态变更等重要通知通过邮件发送</p></div>
      </div>
      <el-switch v-model="data.notificationPrefs.emailNotify" @change="data.saveNotificationPrefs" :loading="data.prefsSaving.value" />
    </div>
    <div class="pref-divider"></div>
    <div class="pref-row">
      <div class="pref-left">
        <div class="pref-icon"><el-icon><Phone /></el-icon></div>
        <div class="pref-text"><h4>短信通知</h4><p>面试提醒、紧急通知通过短信发送</p></div>
      </div>
      <el-switch v-model="data.notificationPrefs.smsNotify" @change="data.saveNotificationPrefs" :loading="data.prefsSaving.value" />
    </div>
    <div class="pref-divider"></div>
    <div class="pref-row">
      <div class="pref-left">
        <div class="pref-icon"><el-icon><Star /></el-icon></div>
        <div class="pref-text"><h4>匹配推荐</h4><p>当有新的职位或人才匹配成功时通知我</p></div>
      </div>
      <el-switch v-model="data.notificationPrefs.jobMatchNotify" @change="data.saveNotificationPrefs" :loading="data.prefsSaving.value" />
    </div>
    <div class="pref-divider"></div>
    <div class="pref-row">
      <div class="pref-left">
        <div class="pref-icon"><el-icon><Calendar /></el-icon></div>
        <div class="pref-text"><h4>面试提醒</h4><p>面试前30分钟发送提醒通知</p></div>
      </div>
      <el-switch v-model="data.notificationPrefs.interviewNotify" @change="data.saveNotificationPrefs" :loading="data.prefsSaving.value" />
    </div>
    <div class="pref-divider"></div>
    <div class="pref-row">
      <div class="pref-left">
        <div class="pref-icon"><el-icon><Bell /></el-icon></div>
        <div class="pref-text"><h4>系统通知</h4><p>平台公告、功能更新等系统消息</p></div>
      </div>
      <el-switch v-model="data.notificationPrefs.systemNotify" @change="data.saveNotificationPrefs" :loading="data.prefsSaving.value" />
    </div>
  </div>
</div>
</template>

<script setup>
import { inject } from 'vue'
import { Message, Phone, Star, Calendar, Bell } from '@element-plus/icons-vue'

const data = inject('profileData')
</script>

<style scoped>
.content-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}
.card-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--gray-100);
}
.prefs-list { padding: var(--space-2) 0; }
.pref-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3) 0;
}
.pref-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.pref-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: var(--gray-100);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray-500);
  font-size: var(--text-lg);
  flex-shrink: 0;
}
.pref-text h4 {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  margin: 0 0 2px;
}
.pref-text p {
  font-size: var(--text-sm);
  color: var(--gray-500);
  margin: 0;
}
.pref-divider { height: 1px; background: var(--gray-100); }
</style>
