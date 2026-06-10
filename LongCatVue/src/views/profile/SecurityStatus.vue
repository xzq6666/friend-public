<template>
<div class="content-card">
  <div class="card-title">账号安全</div>
  <div class="security-list">
    <div class="security-row">
      <div class="security-left">
        <div class="security-icon"><el-icon><Lock /></el-icon></div>
        <div class="security-text">
          <h4>登录密码</h4>
          <p>定期修改密码可以保护账号安全</p>
        </div>
      </div>
      <el-switch :model-value="true" disabled />
    </div>
    <div class="security-divider"></div>
    <div class="security-row">
      <div class="security-left">
        <div class="security-icon"><el-icon><Message /></el-icon></div>
        <div class="security-text">
          <h4>邮箱绑定</h4>
          <p>{{ data.user.value?.email ? '已绑定：' + data.user.value.email : '未绑定邮箱' }}</p>
        </div>
      </div>
      <el-tag v-if="data.user.value?.email" type="success" size="small">已绑定</el-tag>
      <el-button v-else type="primary" link @click="data.showEditDialog">绑定</el-button>
    </div>
    <div class="security-divider"></div>
    <div class="security-row">
      <div class="security-left">
        <div class="security-icon"><el-icon><Phone /></el-icon></div>
        <div class="security-text">
          <h4>手机绑定</h4>
          <p>{{ data.user.value?.phone ? '已绑定：' + data.user.value.phone : '未绑定手机号' }}</p>
        </div>
      </div>
      <el-tag v-if="data.user.value?.phone" type="success" size="small">已绑定</el-tag>
      <el-button v-else type="primary" link @click="data.showEditDialog">绑定</el-button>
    </div>
  </div>
</div>
</template>

<script setup>
import { inject } from 'vue'
import { Lock, Message, Phone } from '@element-plus/icons-vue'

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
.security-list { padding: var(--space-2) 0; }
.security-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3) 0;
}
.security-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.security-icon {
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
.security-text h4 {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  margin: 0 0 var(--space-1);
}
.security-text p {
  font-size: var(--text-sm);
  color: var(--gray-500);
  margin: 0;
}
.security-divider { height: 1px; background: var(--gray-100); }
</style>
