<template>
<div class="page-container page-enter">
<div class="page-header-row">
  <div>
    <h2>职位管理</h2>
    <p class="page-subtitle">共 {{ total }} 个职位</p>
  </div>
  <el-button class="btn-primary" @click="showAddDialog"><el-icon><Plus /></el-icon>发布职位</el-button>
</div>
<div class="stats-row">
  <button class="stat-tab" :class="{ active: activeStatus === 'all' }" @click="setStatusFilter('all')">
    <span class="stat-count">{{ allCount }}</span><span class="stat-name">全部</span>
  </button>
  <button class="stat-tab" :class="{ active: activeStatus === 'recruiting' }" @click="setStatusFilter('recruiting')">
    <span class="stat-count">{{ recruitingCount }}</span><span class="stat-name">招聘中</span>
  </button>
  <button class="stat-tab" :class="{ active: activeStatus === 'offline' }" @click="setStatusFilter('offline')">
    <span class="stat-count">{{ offlineCount }}</span><span class="stat-name">已下线</span>
  </button>
</div>
<div class="filter-bar">
<el-input v-model="searchKeyword" placeholder="搜索职位名称" clearable @keyup.enter="fetchJobs"><template #prefix><el-icon><Search /></el-icon></template></el-input>
<el-button class="btn-primary" @click="fetchJobs">搜索</el-button>
</div>
<!-- 批量操作栏 -->
<div v-if="selectedJobs.length > 0" class="batch-bar">
  <div class="batch-bar-left">
    <el-icon><Check /></el-icon>
    <span>已选 <strong>{{ selectedJobs.length }}</strong> 个职位</span>
  </div>
  <div class="batch-bar-right">
    <el-button size="small" :loading="batchOperating" @click="batchOnline">
      <el-icon><VideoPlay /></el-icon> 批量上线
    </el-button>
    <el-button size="small" :loading="batchOperating" @click="batchOffline">
      <el-icon><VideoPause /></el-icon> 批量下线
    </el-button>
    <el-button size="small" type="danger" :loading="batchOperating" @click="batchDelete">
      <el-icon><Delete /></el-icon> 批量删除
    </el-button>
    <el-button size="small" @click="selectedJobs = []">取消选择</el-button>
  </div>
</div>

<div class="table-card">
<el-table :data="filteredJobs" v-loading="loading" stripe @selection-change="handleSelectionChange">
<el-table-column type="selection" width="45" />
<el-table-column prop="title" label="职位名称" min-width="160">
  <template #default="{ row }"><span class="job-title">{{ row.title }}</span></template>
</el-table-column>
<el-table-column label="行业分类" width="140">
  <template #default="{ row }"><el-tag size="small" :style="{ background: getCategoryColor(row.categoryId) + '15', color: getCategoryColor(row.categoryId), borderColor: getCategoryColor(row.categoryId) + '30' }">{{ getCategoryName(row.categoryId) }}</el-tag></template>
</el-table-column>
<el-table-column label="工作地点" width="110">
  <template #default="{ row }"><span class="info-cell"><el-icon><Location /></el-icon>{{ row.location }}</span></template>
</el-table-column>
<el-table-column label="薪资范围" width="160">
  <template #default="{ row }"><span class="salary-cell" :class="{ 'salary-high': isHighSalary(row) }">{{ formatSalary(row) }}</span></template>
</el-table-column>
<el-table-column label="经验要求" width="100">
  <template #default="{ row }"><span class="info-cell">{{ row.experienceRequired }}</span></template>
</el-table-column>
<el-table-column label="学历要求" width="100">
  <template #default="{ row }"><span class="info-cell">{{ row.educationRequired }}</span></template>
</el-table-column>
<el-table-column label="状态" width="90">
  <template #default="{ row }">
    <span class="status-dot" :class="row.status === 1 ? 'dot-success' : 'dot-default'"></span>
    <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">{{ row.status === 1 ? '招聘中' : '已下线' }}</el-tag>
  </template>
</el-table-column>
<el-table-column label="发布时间" width="110">
  <template #default="{ row }"><span class="info-cell">{{ formatDate(row.createTime) }}</span></template>
</el-table-column>
<el-table-column label="操作" width="260" fixed="right">
  <template #default="{ row }">
    <div class="action-cell">
      <el-button size="small" type="primary" @click="viewApplications(row)">投递</el-button>
      <el-button size="small" @click="editJob(row)">编辑</el-button>
      <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
        <el-button size="small" type="info" text>更多 <el-icon style="margin-left:2px"><ArrowDown /></el-icon></el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="forum"><el-icon><ChatDotRound /></el-icon> 论坛</el-dropdown-item>
            <el-dropdown-item command="export"><el-icon><Document /></el-icon> 导出投递</el-dropdown-item>
            <el-dropdown-item divided :command="row.status === 1 ? 'offline' : 'online'">
              <el-icon><SwitchButton /></el-icon> {{ row.status === 1 ? '下线' : '上线' }}
            </el-dropdown-item>
            <el-dropdown-item command="delete" divided style="color:var(--el-color-danger)">
              <el-icon><Delete /></el-icon> 删除
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </template>
</el-table-column></el-table>
<div class="pagination-wrapper"><el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="fetchJobs" @current-change="fetchJobs" /></div>
</div>
<el-dialog v-model="appDialogVisible" title="投递记录" width="850px" destroy-on-close>
<el-table :data="jobApplications" v-loading="appLoading" stripe>
<el-table-column prop="applicant_name" label="求职者" width="90" />
<el-table-column prop="resume_name" label="简历名称" min-width="130" />
<el-table-column prop="resume_education" label="学历" width="80" />
<el-table-column prop="resume_experience" label="经验" width="80" />
<el-table-column label="技能" min-width="180">
  <template #default="{ row }"><el-tag v-for="skill in parseSkills(row.resume_skills)" :key="skill" size="small" style="margin: 2px;" effect="plain">{{ skill }}</el-tag></template>
</el-table-column>
<el-table-column label="状态" width="100">
  <template #default="{ row }">
    <span class="status-dot" :class="'dot-' + row.status"></span>
    <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
  </template>
</el-table-column>
<el-table-column v-if="!isAdmin" label="操作" width="250" fixed="right">
  <template #default="{ row }">
    <div class="action-cell">
      <el-select :model-value="row.status" size="small" style="width: 90px;" @change="(val) => updateAppStatus(row, val)" :disabled="getStatusOptions(row.status).length === 0">
        <el-option v-for="opt in [{label: getStatusText(row.status), value: row.status}, ...getStatusOptions(row.status).filter(o => o.value !== row.status)]" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button size="small" type="primary" @click="openInterviewDialog(row)">发面试邀请</el-button>
    </div>
  </template>
</el-table-column>
</el-table>
</el-dialog>
<el-dialog v-model="interviewDialogVisible" title="发送面试邀请" width="500px">
<el-form :model="interviewForm" label-width="100px">
<el-form-item label="面试时间"><el-date-picker v-model="interviewForm.interviewTime" type="datetime" placeholder="选择面试时间" style="width: 100%" /></el-form-item>
<el-form-item label="面试地点"><el-input v-model="interviewForm.interviewLocation" placeholder="请输入面试地点" /></el-form-item>
<el-form-item label="面试类型"><el-select v-model="interviewForm.interviewType" style="width: 100%"><el-option label="现场面试" :value="1" /><el-option label="视频面试" :value="2" /><el-option label="电话面试" :value="3" /></el-select></el-form-item>
<el-form-item label="联系人"><el-input v-model="interviewForm.contactPerson" placeholder="请输入联系人" /></el-form-item>
<el-form-item label="联系电话"><el-input v-model="interviewForm.contactPhone" placeholder="请输入联系电话" /></el-form-item>
<el-form-item label="备注"><el-input v-model="interviewForm.notes" type="textarea" :rows="3" placeholder="面试备注（选填）" /></el-form-item>
</el-form>
<template #footer><el-button @click="interviewDialogVisible = false">取消</el-button><el-button class="btn-primary" @click="sendInterview" :loading="sendingInterview">发送邀请</el-button></template></el-dialog>
<el-dialog v-model="dialogVisible" :title="isEdit ? '编辑职位' : '发布职位'" width="600px">
<el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
<el-form-item label="职位名称" prop="title"><el-input v-model="form.title" placeholder="请输入职位名称" /></el-form-item>
<el-form-item label="行业分类" prop="categoryId"><el-select v-model="form.categoryId" placeholder="请选择行业分类" style="width: 100%"><el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" /></el-select></el-form-item>
<el-form-item label="工作地点" prop="location"><el-cascader v-model="form.location" :options="regionOptions" placeholder="请选择省/市" clearable filterable style="width: 100%" /></el-form-item>
<el-form-item label="薪资范围" required>
<el-col :span="11"><el-form-item prop="salaryMin"><el-input-number v-model="form.salaryMin" :min="0" :step="1000" placeholder="最低薪资" style="width: 100%" /></el-form-item></el-col>
<el-col :span="2" style="text-align:center;line-height:32px">至</el-col>
<el-col :span="11"><el-form-item prop="salaryMax"><el-input-number v-model="form.salaryMax" :min="0" :step="1000" placeholder="最高薪资" style="width: 100%" /></el-form-item></el-col>
</el-form-item>
<el-form-item label="经验要求" prop="experienceRequired"><el-select v-model="form.experienceRequired" placeholder="请选择经验要求">
<el-option label="不限" value="不限" /><el-option label="应届生" value="应届生" /><el-option label="1-3年" value="1-3年" /><el-option label="3-5年" value="3-5年" /><el-option label="5-10年" value="5-10年" /><el-option label="10年以上" value="10年以上" /></el-select></el-form-item>
<el-form-item label="学历要求" prop="educationRequired"><el-select v-model="form.educationRequired" placeholder="请选择学历要求">
<el-option label="不限" value="不限" /><el-option label="大专" value="大专" /><el-option label="本科" value="本科" /><el-option label="硕士" value="硕士" /><el-option label="博士" value="博士" /></el-select></el-form-item>
<el-form-item label="工作类型" prop="workType"><el-select v-model="form.workType" placeholder="请选择工作类型" clearable>
<el-option label="远程" value="remote" /><el-option label="现场" value="onsite" /><el-option label="混合" value="hybrid" /></el-select></el-form-item>
<el-form-item label="职位描述" prop="description"><el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入职位描述" /></el-form-item>
<el-form-item label="任职要求" prop="requirements"><el-input v-model="form.requirements" type="textarea" :rows="4" placeholder="请输入任职要求" /></el-form-item>
</el-form>
<template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button class="btn-primary" @click="submitForm" :loading="submitting">{{ isEdit ? '保存修改' : '发布职位' }}</el-button></template></el-dialog>
</div></template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Check, Close, Delete, VideoPlay, VideoPause, Location, Document, ArrowDown, ChatDotRound, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()

const userStore = useUserStore()
const isAdmin = computed(() => userStore.user?.userType === 'ADMIN')

// ── 状态筛选 ──────────────────────────────
const activeStatus = ref('all')
const filteredJobs = computed(() => {
  if (activeStatus.value === 'all') return jobs.value
  if (activeStatus.value === 'recruiting') return jobs.value.filter(j => j.status === 1)
  if (activeStatus.value === 'offline') return jobs.value.filter(j => j.status === 0)
  return jobs.value
})
const allCount = computed(() => total.value)
const recruitingCount = computed(() => jobs.value.filter(j => j.status === 1).length)
const offlineCount = computed(() => jobs.value.filter(j => j.status === 0).length)
const setStatusFilter = (status) => { activeStatus.value = status }

// ── 格式化与辅助函数 ──────────────────────
const CATEGORY_COLORS = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#00BCD4', '#9C27B0', '#FF5722', '#795548', '#607D8B']
const getCategoryColor = (categoryId) => CATEGORY_COLORS[(categoryId || 0) % CATEGORY_COLORS.length]
const formatSalary = (row) => {
  if (!row.salaryMin && !row.salaryMax) return '面议'
  const min = row.salaryMin?.toLocaleString() || '0'
  const max = row.salaryMax?.toLocaleString() || '0'
  return `${min} - ${max} 元/月`
}
const isHighSalary = (row) => row.salaryMin >= 20000 || row.salaryMax >= 30000
const handleCommand = (command, row) => {
  if (command === 'forum') router.push(`/forum/${row.id}`)
  else if (command === 'offline' || command === 'online') toggleJobStatus(row)
  else if (command === 'delete') deleteJob(row)
  else if (command === 'export') exportJobApplications(row)
}
const exportJobApplications = async (job) => {
  try {
    const res = await request.get(`/application/job/${job.id}`)
    const apps = res || []
    const statusMap = { 0: '待处理', 1: '已查看', 2: '邀请面试', 3: '已录用', 4: '已拒绝' }
    const header = '求职者,简历名称,学历,经验,技能,状态\n'
    const rows = apps.map(a => {
      const skills = parseSkills(a.resume_skills).join('/')
      return `${a.applicant_name},${a.resume_name},${a.resume_education},${a.resume_experience},${skills},${statusMap[a.status] || '未知'}`
    }).join('\n')
    const blob = new Blob(['\uFEFF' + header + rows], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${job.title}_投递记录.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
    console.error(error)
  }
}

// 投递记录相关
const appDialogVisible = ref(false)
const appLoading = ref(false)
const jobApplications = ref([])
const updatingStatus = ref(false) // 防止并发更新

// 面试邀请相关
const interviewDialogVisible = ref(false)
const sendingInterview = ref(false)
const selectedApplication = ref(null)
const interviewForm = ref({
  interviewTime: '',
  interviewLocation: '',
  interviewType: 1,
  contactPerson: '',
  contactPhone: '',
  notes: ''
})

// 状态
const jobs = ref([])
const categories = ref([])
const loading = ref(false)
const selectedJobs = ref([])
const batchOperating = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const editingId = ref(null)

// 地区数据：省份-城市级联（覆盖全国）
const regionOptions = [
  { value: '北京', label: '北京', children: [{ value: '北京', label: '北京' }] },
  { value: '上海', label: '上海', children: [{ value: '上海', label: '上海' }] },
  { value: '天津', label: '天津', children: [{ value: '天津', label: '天津' }] },
  { value: '重庆', label: '重庆', children: [{ value: '重庆', label: '重庆' }] },
  { value: '河北', label: '河北', children: [{ value: '石家庄', label: '石家庄' }, { value: '唐山', label: '唐山' }, { value: '秦皇岛', label: '秦皇岛' }, { value: '邯郸', label: '邯郸' }, { value: '邢台', label: '邢台' }, { value: '保定', label: '保定' }, { value: '张家口', label: '张家口' }, { value: '承德', label: '承德' }, { value: '沧州', label: '沧州' }, { value: '廊坊', label: '廊坊' }, { value: '衡水', label: '衡水' }] },
  { value: '山西', label: '山西', children: [{ value: '太原', label: '太原' }, { value: '大同', label: '大同' }, { value: '阳泉', label: '阳泉' }, { value: '长治', label: '长治' }, { value: '晋城', label: '晋城' }, { value: '朔州', label: '朔州' }, { value: '晋中', label: '晋中' }, { value: '运城', label: '运城' }, { value: '忻州', label: '忻州' }, { value: '临汾', label: '临汾' }, { value: '吕梁', label: '吕梁' }] },
  { value: '辽宁', label: '辽宁', children: [{ value: '沈阳', label: '沈阳' }, { value: '大连', label: '大连' }, { value: '鞍山', label: '鞍山' }, { value: '抚顺', label: '抚顺' }, { value: '本溪', label: '本溪' }, { value: '丹东', label: '丹东' }, { value: '锦州', label: '锦州' }, { value: '营口', label: '营口' }, { value: '阜新', label: '阜新' }, { value: '辽阳', label: '辽阳' }, { value: '盘锦', label: '盘锦' }, { value: '铁岭', label: '铁岭' }, { value: '朝阳', label: '朝阳' }, { value: '葫芦岛', label: '葫芦岛' }] },
  { value: '吉林', label: '吉林', children: [{ value: '长春', label: '长春' }, { value: '吉林', label: '吉林' }, { value: '四平', label: '四平' }, { value: '辽源', label: '辽源' }, { value: '通化', label: '通化' }, { value: '白山', label: '白山' }, { value: '松原', label: '松原' }, { value: '白城', label: '白城' }, { value: '延边', label: '延边' }] },
  { value: '黑龙江', label: '黑龙江', children: [{ value: '哈尔滨', label: '哈尔滨' }, { value: '齐齐哈尔', label: '齐齐哈尔' }, { value: '鸡西', label: '鸡西' }, { value: '鹤岗', label: '鹤岗' }, { value: '双鸭山', label: '双鸭山' }, { value: '大庆', label: '大庆' }, { value: '伊春', label: '伊春' }, { value: '佳木斯', label: '佳木斯' }, { value: '七台河', label: '七台河' }, { value: '牡丹江', label: '牡丹江' }, { value: '黑河', label: '黑河' }, { value: '绥化', label: '绥化' }, { value: '大兴安岭', label: '大兴安岭' }] },
  { value: '江苏', label: '江苏', children: [{ value: '南京', label: '南京' }, { value: '无锡', label: '无锡' }, { value: '徐州', label: '徐州' }, { value: '常州', label: '常州' }, { value: '苏州', label: '苏州' }, { value: '南通', label: '南通' }, { value: '连云港', label: '连云港' }, { value: '淮安', label: '淮安' }, { value: '盐城', label: '盐城' }, { value: '扬州', label: '扬州' }, { value: '镇江', label: '镇江' }, { value: '泰州', label: '泰州' }, { value: '宿迁', label: '宿迁' }] },
  { value: '浙江', label: '浙江', children: [{ value: '杭州', label: '杭州' }, { value: '宁波', label: '宁波' }, { value: '温州', label: '温州' }, { value: '嘉兴', label: '嘉兴' }, { value: '湖州', label: '湖州' }, { value: '绍兴', label: '绍兴' }, { value: '金华', label: '金华' }, { value: '衢州', label: '衢州' }, { value: '舟山', label: '舟山' }, { value: '台州', label: '台州' }, { value: '丽水', label: '丽水' }] },
  { value: '安徽', label: '安徽', children: [{ value: '合肥', label: '合肥' }, { value: '芜湖', label: '芜湖' }, { value: '蚌埠', label: '蚌埠' }, { value: '淮南', label: '淮南' }, { value: '马鞍山', label: '马鞍山' }, { value: '淮北', label: '淮北' }, { value: '铜陵', label: '铜陵' }, { value: '安庆', label: '安庆' }, { value: '黄山', label: '黄山' }, { value: '滁州', label: '滁州' }, { value: '阜阳', label: '阜阳' }, { value: '宿州', label: '宿州' }, { value: '六安', label: '六安' }, { value: '亳州', label: '亳州' }, { value: '池州', label: '池州' }, { value: '宣城', label: '宣城' }] },
  { value: '福建', label: '福建', children: [{ value: '福州', label: '福州' }, { value: '厦门', label: '厦门' }, { value: '莆田', label: '莆田' }, { value: '三明', label: '三明' }, { value: '泉州', label: '泉州' }, { value: '漳州', label: '漳州' }, { value: '南平', label: '南平' }, { value: '龙岩', label: '龙岩' }, { value: '宁德', label: '宁德' }] },
  { value: '江西', label: '江西', children: [{ value: '南昌', label: '南昌' }, { value: '景德镇', label: '景德镇' }, { value: '萍乡', label: '萍乡' }, { value: '九江', label: '九江' }, { value: '新余', label: '新余' }, { value: '鹰潭', label: '鹰潭' }, { value: '赣州', label: '赣州' }, { value: '吉安', label: '吉安' }, { value: '宜春', label: '宜春' }, { value: '抚州', label: '抚州' }, { value: '上饶', label: '上饶' }] },
  { value: '山东', label: '山东', children: [{ value: '济南', label: '济南' }, { value: '青岛', label: '青岛' }, { value: '淄博', label: '淄博' }, { value: '枣庄', label: '枣庄' }, { value: '东营', label: '东营' }, { value: '烟台', label: '烟台' }, { value: '潍坊', label: '潍坊' }, { value: '济宁', label: '济宁' }, { value: '泰安', label: '泰安' }, { value: '威海', label: '威海' }, { value: '日照', label: '日照' }, { value: '临沂', label: '临沂' }, { value: '德州', label: '德州' }, { value: '聊城', label: '聊城' }, { value: '滨州', label: '滨州' }, { value: '菏泽', label: '菏泽' }] },
  { value: '河南', label: '河南', children: [{ value: '郑州', label: '郑州' }, { value: '开封', label: '开封' }, { value: '洛阳', label: '洛阳' }, { value: '平顶山', label: '平顶山' }, { value: '安阳', label: '安阳' }, { value: '鹤壁', label: '鹤壁' }, { value: '新乡', label: '新乡' }, { value: '焦作', label: '焦作' }, { value: '濮阳', label: '濮阳' }, { value: '许昌', label: '许昌' }, { value: '漯河', label: '漯河' }, { value: '三门峡', label: '三门峡' }, { value: '南阳', label: '南阳' }, { value: '商丘', label: '商丘' }, { value: '信阳', label: '信阳' }, { value: '周口', label: '周口' }, { value: '驻马店', label: '驻马店' }, { value: '济源', label: '济源' }] },
  { value: '湖北', label: '湖北', children: [{ value: '武汉', label: '武汉' }, { value: '黄石', label: '黄石' }, { value: '十堰', label: '十堰' }, { value: '宜昌', label: '宜昌' }, { value: '襄阳', label: '襄阳' }, { value: '鄂州', label: '鄂州' }, { value: '荆门', label: '荆门' }, { value: '孝感', label: '孝感' }, { value: '荆州', label: '荆州' }, { value: '黄冈', label: '黄冈' }, { value: '咸宁', label: '咸宁' }, { value: '随州', label: '随州' }, { value: '恩施', label: '恩施' }, { value: '仙桃', label: '仙桃' }, { value: '潜江', label: '潜江' }, { value: '天门', label: '天门' }, { value: '神农架', label: '神农架' }] },
  { value: '湖南', label: '湖南', children: [{ value: '长沙', label: '长沙' }, { value: '株洲', label: '株洲' }, { value: '湘潭', label: '湘潭' }, { value: '衡阳', label: '衡阳' }, { value: '邵阳', label: '邵阳' }, { value: '岳阳', label: '岳阳' }, { value: '常德', label: '常德' }, { value: '张家界', label: '张家界' }, { value: '益阳', label: '益阳' }, { value: '郴州', label: '郴州' }, { value: '永州', label: '永州' }, { value: '怀化', label: '怀化' }, { value: '娄底', label: '娄底' }, { value: '湘西', label: '湘西' }] },
  { value: '广东', label: '广东', children: [{ value: '广州', label: '广州' }, { value: '韶关', label: '韶关' }, { value: '深圳', label: '深圳' }, { value: '珠海', label: '珠海' }, { value: '汕头', label: '汕头' }, { value: '佛山', label: '佛山' }, { value: '江门', label: '江门' }, { value: '湛江', label: '湛江' }, { value: '茂名', label: '茂名' }, { value: '肇庆', label: '肇庆' }, { value: '惠州', label: '惠州' }, { value: '梅州', label: '梅州' }, { value: '汕尾', label: '汕尾' }, { value: '河源', label: '河源' }, { value: '阳江', label: '阳江' }, { value: '清远', label: '清远' }, { value: '东莞', label: '东莞' }, { value: '中山', label: '中山' }, { value: '潮州', label: '潮州' }, { value: '揭阳', label: '揭阳' }, { value: '云浮', label: '云浮' }] },
  { value: '广西', label: '广西', children: [{ value: '南宁', label: '南宁' }, { value: '柳州', label: '柳州' }, { value: '桂林', label: '桂林' }, { value: '梧州', label: '梧州' }, { value: '北海', label: '北海' }, { value: '防城港', label: '防城港' }, { value: '钦州', label: '钦州' }, { value: '贵港', label: '贵港' }, { value: '玉林', label: '玉林' }, { value: '百色', label: '百色' }, { value: '贺州', label: '贺州' }, { value: '河池', label: '河池' }, { value: '来宾', label: '来宾' }, { value: '崇左', label: '崇左' }] },
  { value: '海南', label: '海南', children: [{ value: '海口', label: '海口' }, { value: '三亚', label: '三亚' }, { value: '三沙', label: '三沙' }, { value: '儋州', label: '儋州' }, { value: '琼海', label: '琼海' }, { value: '文昌', label: '文昌' }, { value: '万宁', label: '万宁' }, { value: '东方', label: '东方' }] },
  { value: '四川', label: '四川', children: [{ value: '成都', label: '成都' }, { value: '自贡', label: '自贡' }, { value: '攀枝花', label: '攀枝花' }, { value: '泸州', label: '泸州' }, { value: '德阳', label: '德阳' }, { value: '绵阳', label: '绵阳' }, { value: '广元', label: '广元' }, { value: '遂宁', label: '遂宁' }, { value: '内江', label: '内江' }, { value: '乐山', label: '乐山' }, { value: '南充', label: '南充' }, { value: '眉山', label: '眉山' }, { value: '宜宾', label: '宜宾' }, { value: '广安', label: '广安' }, { value: '达州', label: '达州' }, { value: '雅安', label: '雅安' }, { value: '巴中', label: '巴中' }, { value: '资阳', label: '资阳' }, { value: '阿坝', label: '阿坝' }, { value: '甘孜', label: '甘孜' }, { value: '凉山', label: '凉山' }] },
  { value: '贵州', label: '贵州', children: [{ value: '贵阳', label: '贵阳' }, { value: '六盘水', label: '六盘水' }, { value: '遵义', label: '遵义' }, { value: '安顺', label: '安顺' }, { value: '毕节', label: '毕节' }, { value: '铜仁', label: '铜仁' }, { value: '黔西南', label: '黔西南' }, { value: '黔东南', label: '黔东南' }, { value: '黔南', label: '黔南' }] },
  { value: '云南', label: '云南', children: [{ value: '昆明', label: '昆明' }, { value: '曲靖', label: '曲靖' }, { value: '玉溪', label: '玉溪' }, { value: '保山', label: '保山' }, { value: '昭通', label: '昭通' }, { value: '丽江', label: '丽江' }, { value: '普洱', label: '普洱' }, { value: '临沧', label: '临沧' }, { value: '楚雄', label: '楚雄' }, { value: '红河', label: '红河' }, { value: '文山', label: '文山' }, { value: '西双版纳', label: '西双版纳' }, { value: '大理', label: '大理' }, { value: '德宏', label: '德宏' }, { value: '怒江', label: '怒江' }, { value: '迪庆', label: '迪庆' }] },
  { value: '陕西', label: '陕西', children: [{ value: '西安', label: '西安' }, { value: '铜川', label: '铜川' }, { value: '宝鸡', label: '宝鸡' }, { value: '咸阳', label: '咸阳' }, { value: '渭南', label: '渭南' }, { value: '延安', label: '延安' }, { value: '汉中', label: '汉中' }, { value: '榆林', label: '榆林' }, { value: '安康', label: '安康' }, { value: '商洛', label: '商洛' }] },
  { value: '甘肃', label: '甘肃', children: [{ value: '兰州', label: '兰州' }, { value: '嘉峪关', label: '嘉峪关' }, { value: '金昌', label: '金昌' }, { value: '白银', label: '白银' }, { value: '天水', label: '天水' }, { value: '武威', label: '武威' }, { value: '张掖', label: '张掖' }, { value: '平凉', label: '平凉' }, { value: '酒泉', label: '酒泉' }, { value: '庆阳', label: '庆阳' }, { value: '定西', label: '定西' }, { value: '陇南', label: '陇南' }, { value: '临夏', label: '临夏' }, { value: '甘南', label: '甘南' }] },
  { value: '青海', label: '青海', children: [{ value: '西宁', label: '西宁' }, { value: '海东', label: '海东' }, { value: '海北', label: '海北' }, { value: '黄南', label: '黄南' }, { value: '海南', label: '海南' }, { value: '果洛', label: '果洛' }, { value: '玉树', label: '玉树' }, { value: '海西', label: '海西' }] },
  { value: '内蒙古', label: '内蒙古', children: [{ value: '呼和浩特', label: '呼和浩特' }, { value: '包头', label: '包头' }, { value: '乌海', label: '乌海' }, { value: '赤峰', label: '赤峰' }, { value: '通辽', label: '通辽' }, { value: '鄂尔多斯', label: '鄂尔多斯' }, { value: '呼伦贝尔', label: '呼伦贝尔' }, { value: '巴彦尔', label: '巴彦淖尔' }, { value: '乌兰察布', label: '乌兰察布' }, { value: '兴安', label: '兴安' }, { value: '锡林郭勒', label: '锡林郭勒' }, { value: '阿拉善', label: '阿拉善' }] },
  { value: '新疆', label: '新疆', children: [{ value: '乌鲁木齐', label: '乌鲁木齐' }, { value: '克拉玛依', label: '克拉玛依' }, { value: '吐鲁番', label: '吐鲁番' }, { value: '哈密', label: '哈密' }, { value: '昌吉', label: '昌吉' }, { value: '博尔塔拉', label: '博尔塔拉' }, { value: '巴音郭楞', label: '巴音郭楞' }, { value: '阿克苏', label: '阿克苏' }, { value: '克孜勒苏', label: '克孜勒苏' }, { value: '喀什', label: '喀什' }, { value: '和田', label: '和田' }, { value: '伊犁', label: '伊犁' }, { value: '塔城', label: '塔城' }, { value: '阿勒泰', label: '阿勒泰' }] },
  { value: '西藏', label: '西藏', children: [{ value: '拉萨', label: '拉萨' }, { value: '日喀则', label: '日喀则' }, { value: '昌都', label: '昌都' }, { value: '林芝', label: '林芝' }, { value: '山南', label: '山南' }, { value: '那曲', label: '那曲' }, { value: '阿里', label: '阿里' }] },
  { value: '宁夏', label: '宁夏', children: [{ value: '银川', label: '银川' }, { value: '石嘴山', label: '石嘴山' }, { value: '吴忠', label: '吴忠' }, { value: '固原', label: '固原' }, { value: '中卫', label: '中卫' }] }
]

// 表单数据
const form = ref({
  title: '',
  categoryId: null,
  location: [],
  salaryMin: null,
  salaryMax: null,
  experienceRequired: '',
  educationRequired: '',
  workType: '',
  description: '',
  requirements: ''
})

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入职位名称', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择行业分类', trigger: 'change' }
  ],
  location: [
    { required: true, message: '请选择工作地点', trigger: 'change', type: 'array' }
  ],
  salaryMin: [
    { required: true, message: '请输入最低薪资', trigger: 'blur' }
  ],
  salaryMax: [
    { required: true, message: '请输入最高薪资', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入职位描述', trigger: 'blur' }
  ]
}

// 获取职位列表
const fetchCategories = async () => {
  try {
    const res = await request.get('/category/list')
    // 兼容后端返回的数据结构，优先取 res.data
    categories.value = res.data || res || []
  } catch (error) {
    console.error('获取分类列表失败', error)
  }
}

const fetchJobs = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value
    }
    const res = await request.get('/job', { params })
    jobs.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    ElMessage.error('获取职位列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 获取分类名称
const getCategoryName = (categoryId) => {
  const cat = categories.value.find(c => c.id === categoryId)
  return cat ? cat.name : '未分类'
}

// 显示添加对话框
const showAddDialog = () => {
  isEdit.value = false
  editingId.value = null
  form.value = {
    title: '',
    categoryId: null,
    location: [],
    salaryMin: null,
    salaryMax: null,
    experienceRequired: '',
    educationRequired: '',
    workType: '',
    description: '',
    requirements: ''
  }
  dialogVisible.value = true
}

// 编辑职位
const editJob = (row) => {
  isEdit.value = true
  editingId.value = row.id
  form.value = {
    title: row.title,
    categoryId: row.categoryId || null,
    // 兼容旧数据：如果是字符串则转为数组，如果是数组则直接使用
    location: typeof row.location === 'string' ? [row.location, row.location] : (row.location || []),
    salaryMin: row.salaryMin,
    salaryMax: row.salaryMax,
    experienceRequired: row.experienceRequired,
    educationRequired: row.educationRequired,
    workType: row.workType || '',
    description: row.description,
    requirements: row.requirements,
    employerId: row.employerId // 保留发布者 ID
  }
  dialogVisible.value = true
}

// 提交表单
const submitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    // 将级联选择器的数组转换为城市名字符串
    const submitData = {
      ...form.value,
      location: Array.isArray(form.value.location) && form.value.location.length > 0 
        ? form.value.location[form.value.location.length - 1] 
        : ''
    }
    
    if (isEdit.value) {
      await request.put(`/job/${editingId.value}`, submitData)
      ElMessage.success('职位更新成功')
    } else {
      await request.post('/job', submitData)
      ElMessage.success('职位发布成功')
    }
    dialogVisible.value = false
    fetchJobs()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '发布失败')
    console.error(error)
  } finally {
    submitting.value = false
  }
}

// 切换职位状态
const toggleJobStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '上线' : '下线'

  try {
    await ElMessageBox.confirm(`确定要${action}该职位吗？`, '提示', {
      type: 'warning'
    })
    await request.put(`/job/${row.id}/status`, { status: newStatus })
    ElMessage.success(`职位已${action}`)
    fetchJobs()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
      console.error(error)
    }
  }
}

// 查看投递记录
const viewApplications = async (row) => {
  appDialogVisible.value = true
  appLoading.value = true
  try {
    const res = await request.get(`/application/job/${row.id}`)
    jobApplications.value = res || []
  } catch (error) {
    ElMessage.error('获取投递记录失败')
    console.error(error)
  } finally {
    appLoading.value = false
  }
}

// 更新投递状态
const updateAppStatus = async (row, status) => {
  const id = row.id
  // 防止 el-select 在初始化或清空时发送无效请求
  if (!id || status === undefined || status === null || status === '') {
    console.log('忽略无效的状态更新请求:', { id, status })
    return
  }

  // 防止并发更新
  if (updatingStatus.value) {
    console.log('正在更新中，忽略重复请求')
    return
  }

  const oldStatus = row.status

  try {
    const statusNum = typeof status === 'number' ? status : Number(status)
    if (isNaN(statusNum) || statusNum < 0 || statusNum > 4) {
      console.error('无效的状态值:', status)
      ElMessage.error('无效的状态值')
      return
    }

    updatingStatus.value = true
    console.log('更新投递状态:', { id, status: statusNum })
    await request.put(`/application/${id}/status`, { status: statusNum })
    row.status = statusNum
    ElMessage.success('状态更新成功')
  } catch (error) {
    // 回滚状态
    row.status = oldStatus
    const msg = error.response?.data?.error || '状态更新失败'
    ElMessage.error(msg)
    console.error(error)
  } finally {
    updatingStatus.value = false
  }
}

// 打开面试邀请对话框
const openInterviewDialog = (row) => {
  selectedApplication.value = row
  interviewForm.value = {
    interviewTime: '',
    interviewLocation: '',
    interviewType: 1,
    contactPerson: '',
    contactPhone: '',
    notes: ''
  }
  interviewDialogVisible.value = true
}

// 发送面试邀请
const sendInterview = async () => {
  if (!interviewForm.value.interviewTime) {
    ElMessage.warning('请选择面试时间')
    return
  }
  sendingInterview.value = true
  try {
    await request.post('/interview', {
      applicationId: selectedApplication.value.id,
      jobId: selectedApplication.value.job_id,
      userId: selectedApplication.value.user_id,
      interviewTime: interviewForm.value.interviewTime.toISOString(),
      interviewLocation: interviewForm.value.interviewLocation,
      interviewType: interviewForm.value.interviewType,
      contactPerson: interviewForm.value.contactPerson,
      contactPhone: interviewForm.value.contactPhone,
      notes: interviewForm.value.notes
    })
    ElMessage.success('面试邀请发送成功')
    interviewDialogVisible.value = false
    // 刷新投递列表
    viewApplications({ id: selectedApplication.value.job_id })
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '发送失败')
  } finally {
    sendingInterview.value = false
  }
}

// 解析技能JSON
const parseSkills = (skills) => {
  if (!skills) return []
  try {
    return typeof skills === 'string' ? JSON.parse(skills) : skills
  } catch {
    return []
  }
}

const getStatusType = (status) => {
  const types = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'success', 4: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '待处理', 1: '已查看', 2: '邀请面试', 3: '已录用', 4: '已拒绝' }
  return texts[status] || '未知'
}

// 状态流转规则（与后端一致）
const VALID_TRANSITIONS = {
  0: [1, 2, 4],
  1: [2, 4],
  2: [3, 4],
  3: [],
  4: []
}

const allStatusOptions = [
  { label: '待处理', value: 0 },
  { label: '已查看', value: 1 },
  { label: '邀请面试', value: 2 },
  { label: '已录用', value: 3 },
  { label: '已拒绝', value: 4 }
]

const getStatusOptions = (currentStatus) => {
  const allowed = VALID_TRANSITIONS[currentStatus] || []
  return allStatusOptions.filter(opt => allowed.includes(opt.value))
}

// 删除职位
const deleteJob = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该职位吗？删除后不可恢复', '警告', {
      type: 'error'
    })
    await request.delete(`/job/${row.id}`)
    ElMessage.success('职位已删除')
    fetchJobs()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error(error)
    }
  }
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN')
}

// 多选相关
const handleSelectionChange = (selection) => {
  selectedJobs.value = selection
}

// 批量上线
const batchOnline = async () => {
  const offlineJobs = selectedJobs.value.filter(j => j.status !== 1)
  if (offlineJobs.length === 0) {
    ElMessage.info('所选职位已全部在线')
    return
  }
  try {
    await ElMessageBox.confirm(`确定批量上线 ${offlineJobs.length} 个职位吗？`, '提示', { type: 'warning' })
    batchOperating.value = true
    await Promise.all(offlineJobs.map(j => request.put(`/job/${j.id}/status`, { status: 1 })))
    ElMessage.success(`已上线 ${offlineJobs.length} 个职位`)
    selectedJobs.value = []
    fetchJobs()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('批量上线失败')
  } finally {
    batchOperating.value = false
  }
}

// 批量下线
const batchOffline = async () => {
  const onlineJobs = selectedJobs.value.filter(j => j.status === 1)
  if (onlineJobs.length === 0) {
    ElMessage.info('所选职位已全部下线')
    return
  }
  try {
    await ElMessageBox.confirm(`确定批量下线 ${onlineJobs.length} 个职位吗？`, '提示', { type: 'warning' })
    batchOperating.value = true
    await Promise.all(onlineJobs.map(j => request.put(`/job/${j.id}/status`, { status: 0 })))
    ElMessage.success(`已下线 ${onlineJobs.length} 个职位`)
    selectedJobs.value = []
    fetchJobs()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('批量下线失败')
  } finally {
    batchOperating.value = false
  }
}

// 批量删除
const batchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定批量删除 ${selectedJobs.value.length} 个职位吗？删除后不可恢复`, '警告', { type: 'error' })
    batchOperating.value = true
    await Promise.all(selectedJobs.value.map(j => request.delete(`/job/${j.id}`)))
    ElMessage.success(`已删除 ${selectedJobs.value.length} 个职位`)
    selectedJobs.value = []
    fetchJobs()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('批量删除失败')
  } finally {
    batchOperating.value = false
  }
}

onMounted(() => {
  fetchCategories()
  fetchJobs()
})
</script>

<style scoped>
/* ── 表格内容样式 ──────────────────────────── */
.job-title {
  font-weight: 600;
  color: var(--gray-800);
  transition: color var(--duration-fast);
}
.job-title:hover {
  color: var(--primary-500);
}
.salary-cell {
  font-weight: 600;
  color: var(--gray-700);
  font-variant-numeric: tabular-nums;
}
.salary-high {
  color: var(--success-500, #67C23A);
}
.info-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--gray-600);
  font-size: var(--text-sm);
}
.info-cell .el-icon {
  color: var(--gray-400);
  font-size: var(--text-base);
}
/* ── 状态圆点 ─────────────────────────────── */
.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}
.dot-success { background: var(--success-500, #67C23A); }
.dot-default { background: var(--gray-400); }
.dot-0 { background: var(--warning-500, #E6A23C); }
.dot-1 { background: var(--primary-500); }
.dot-2 { background: var(--primary-500); }
.dot-3 { background: var(--success-500, #67C23A); }
.dot-4 { background: var(--danger-500, #F56C6C); }
/* ── 操作按钮布局 ─────────────────────────── */
.action-cell {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}
/* ── 筛选栏 ───────────────────────────────── */
.filter-bar .el-input { width: 300px; }
/* ── 批量操作栏 ───────────────────────────── */
.batch-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3) var(--space-4);
  margin-bottom: var(--space-4);
  background: linear-gradient(135deg, var(--primary-50) 0%, var(--primary-50) 100%);
  border: 1px solid var(--primary-200);
  border-radius: var(--radius-md);
}
.batch-bar-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--gray-700);
}
.batch-bar-left .el-icon {
  color: var(--primary-500);
  font-size: var(--text-base);
}
.batch-bar-left strong {
  color: var(--primary-500);
  font-size: var(--text-base);
}
.batch-bar-right {
  display: flex;
  gap: var(--space-2);
}
</style>