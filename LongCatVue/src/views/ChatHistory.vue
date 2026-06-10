<template>
  <div class="page-container">
    <div class="page-header-row">
      <h2>历史聊天</h2>
      <el-button
        v-if="chatList.length > 0"
        type="danger"
        plain
        size="small"
        @click="clearAllChats"
      >
        <el-icon><Delete /></el-icon>
        清空全部
      </el-button>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && chatList.length === 0" description="暂无聊天记录">
        <el-button type="primary" plain @click="$router.push(isEmployer ? '/talent-market' : '/browse-jobs')">
          {{ isEmployer ? '进入人才市场' : '浏览职位' }}
        </el-button>
      </el-empty>

      <div v-else class="chat-list">
        <div
          v-for="item in chatList"
          :key="item.id"
          class="chat-card"
          @click="goToChat(item.id)"
        >
          <div class="card-left">
            <div class="avatar">{{ getAvatarText(item) }}</div>
            <div class="chat-info">
              <div class="chat-title">
                <span class="title-text">{{ getChatTitle(item) }}</span>
              </div>
              <div class="chat-preview">{{ getLastMessage(item) }}</div>
            </div>
          </div>
          <div class="card-right">
            <el-button
              type="danger"
              link
              size="small"
              class="delete-btn"
              @click.stop="deleteChat(item)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ChatHistory'
}
</script>

<script setup>
import { ref, computed, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Delete } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()
const userStore = useUserStore()
const isEmployer = computed(() => userStore.user?.userType === 'EMPLOYER')
const loading = ref(false)
const chatList = ref([])

// 获取聊天标题
const getChatTitle = (item) => {
  try {
    return item?.job_title || item?.chat_partner_name || '沟通对话'
  } catch {
    return '沟通对话'
  }
}

// 获取头像文字
const getAvatarText = (item) => {
  try {
    const text = item?.job_title || item?.chat_partner_name || ''
    return text.charAt(0).toUpperCase()
  } catch {
    return '?'
  }
}

// 获取最后一条消息
const getLastMessage = (item) => {
  try {
    if (item?.last_message) {
      const msg = String(item.last_message)
      return msg.substring(0, 50)
    }
    return '暂无消息'
  } catch {
    return '暂无消息'
  }
}

// 获取聊天列表
const fetchChats = async () => {
  loading.value = true
  try {
    const res = await request.get('/interview/my-chats', { skipGlobalLoading: true })
    chatList.value = Array.isArray(res) ? res : []
  } catch (e) {
    console.error('获取聊天列表失败', e)
    chatList.value = []
  } finally {
    loading.value = false
  }
}

// 跳转到聊天页面
const goToChat = (id) => {
  if (id) {
    router.push(`/interview-chat/${id}`)
  }
}

// 删除单条聊天记录
const deleteChat = async (item) => {
  try {
    await ElMessageBox.confirm('确定删除该聊天记录？删除后将无法恢复。', '提示', { type: 'warning' })
    await request.delete(`/interview-chat/${item.id}`)
    ElMessage.success('已删除')
    // 从列表中移除
    chatList.value = chatList.value.filter(chat => chat.id !== item.id)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 清空全部聊天记录
const clearAllChats = async () => {
  try {
    await ElMessageBox.confirm('确定清空所有聊天记录？此操作不可恢复。', '警告', {
      type: 'warning',
      confirmButtonText: '确定清空',
      cancelButtonText: '取消'
    })
    loading.value = true
    // 遍历删除所有聊天记录
    const deletePromises = chatList.value.map(item =>
      request.delete(`/interview-chat/${item.id}`).catch(e => {
        console.error(`删除聊天 ${item.id} 失败:`, e)
      })
    )
    await Promise.all(deletePromises)
    ElMessage.success('已清空全部聊天记录')
    chatList.value = []
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空失败:', error)
      ElMessage.error('清空失败')
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  console.log('[ChatHistory] 组件已挂载')
  fetchChats()
})

// keep-alive 激活时刷新数据
onActivated(() => {
  console.log('[ChatHistory] 组件被激活')
  fetchChats()
})
</script>

<style scoped>
.chat-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.chat-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-5);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast);
}

.chat-card:hover {
  border-color: var(--gray-300);
  box-shadow: var(--shadow-sm);
}

.card-left {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex: 1;
  min-width: 0;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  background: var(--gray-100);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--gray-600);
  flex-shrink: 0;
}

.chat-info { flex: 1; min-width: 0; }

.chat-title {
  margin-bottom: var(--space-1);
}

.title-text {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.chat-preview {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-right {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.delete-btn {
  opacity: 0;
  transition: opacity var(--duration-fast);
}

.chat-card:hover .delete-btn {
  opacity: 1;
}

.arrow-icon {
  font-size: var(--text-base);
  color: var(--gray-400);
}
</style>
