<template>
  <div class="match-search-bar" v-if="visible">
    <div class="search-bar-inner">
      <!-- 搜索图标 -->
      <div class="search-icon">
        <el-icon><Search /></el-icon>
      </div>

      <!-- 搜索输入框 -->
      <input
        v-model="searchKeyword"
        type="text"
        class="search-input"
        :placeholder="isEmployee ? '搜索职位名称、公司...' : '搜索候选人姓名、技能...'"
        @keyup.enter="handleSearch"
        @input="handleInput"
      />

      <!-- 清除按钮 -->
      <button v-if="searchKeyword" class="clear-btn" @click="clearSearch">
        <el-icon><Close /></el-icon>
      </button>

      <!-- 筛选按钮 -->
      <button class="filter-btn" :class="{ 'active': showFilters }" @click="toggleFilters">
        <el-icon><Filter /></el-icon>
        <span>筛选</span>
        <el-tag v-if="activeFilterCount > 0" type="primary" size="small" effect="dark">{{ activeFilterCount }}</el-tag>
      </button>
    </div>

    <!-- 筛选面板 -->
    <div v-show="showFilters" class="filter-dropdown">
      <!-- 求职者筛选 -->
      <template v-if="isEmployee">
        <div class="filter-section">
          <div class="filter-section-title">工作地点</div>
          <div class="filter-options">
            <el-cascader
              v-model="filters.location"
              :options="regionOptions"
              placeholder="请选择城市"
              clearable
              filterable
              size="small"
              style="width: 100%;"
              @change="applyFilters"
            />
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-section-title">薪资范围</div>
          <div class="filter-options">
            <el-select v-model="filters.salaryRange" placeholder="不限" clearable size="small" style="width: 100%;" @change="applyFilters">
              <el-option label="5K以下" value="0-5" />
              <el-option label="5K-10K" value="5-10" />
              <el-option label="10K-20K" value="10-20" />
              <el-option label="20K-30K" value="20-30" />
              <el-option label="30K-50K" value="30-50" />
              <el-option label="50K以上" value="50-" />
            </el-select>
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-section-title">经验要求</div>
          <div class="filter-options">
            <el-select v-model="filters.experience" placeholder="不限" clearable size="small" style="width: 100%;" @change="applyFilters">
              <el-option label="应届生" value="应届生" />
              <el-option label="1-3年" value="1-3年" />
              <el-option label="3-5年" value="3-5年" />
              <el-option label="5-10年" value="5-10年" />
              <el-option label="10年以上" value="10年以上" />
            </el-select>
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-section-title">学历要求</div>
          <div class="filter-options">
            <el-select v-model="filters.education" placeholder="不限" clearable size="small" style="width: 100%;" @change="applyFilters">
              <el-option label="大专" value="大专" />
              <el-option label="本科" value="本科" />
              <el-option label="硕士" value="硕士" />
              <el-option label="博士" value="博士" />
            </el-select>
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-section-title">技能关键词</div>
          <div class="filter-options">
            <el-select
              v-model="filters.skills"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="输入或选择技能"
              size="small"
              style="width: 100%;"
              @change="applyFilters"
            >
              <el-option v-for="skill in skillOptions" :key="skill" :label="skill" :value="skill" />
            </el-select>
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-section-title">最低匹配度</div>
          <div class="filter-options">
            <el-select v-model="filters.minScore" placeholder="不限" clearable size="small" style="width: 100%;" @change="applyFilters">
              <el-option label="≥90分" value="90" />
              <el-option label="≥80分" value="80" />
              <el-option label="≥70分" value="70" />
              <el-option label="≥60分" value="60" />
              <el-option label="≥50分" value="50" />
            </el-select>
          </div>
        </div>
      </template>

      <!-- 企业端筛选 -->
      <template v-else>
        <div class="filter-section">
          <div class="filter-section-title">候选人学历</div>
          <div class="filter-options">
            <el-select v-model="filters.education" placeholder="不限" clearable size="small" style="width: 100%;" @change="applyFilters">
              <el-option label="大专及以上" value="大专" />
              <el-option label="本科及以上" value="本科" />
              <el-option label="硕士及以上" value="硕士" />
              <el-option label="博士" value="博士" />
            </el-select>
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-section-title">工作经验</div>
          <div class="filter-options">
            <el-select v-model="filters.experience" placeholder="不限" clearable size="small" style="width: 100%;" @change="applyFilters">
              <el-option label="应届生" value="应届生" />
              <el-option label="1-3年" value="1-3" />
              <el-option label="3-5年" value="3-5" />
              <el-option label="5-10年" value="5-10" />
              <el-option label="10年以上" value="10-" />
            </el-select>
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-section-title">期望薪资</div>
          <div class="filter-options">
            <el-select v-model="filters.salaryRange" placeholder="不限" clearable size="small" style="width: 100%;" @change="applyFilters">
              <el-option label="10K以下" value="0-10" />
              <el-option label="10K-20K" value="10-20" />
              <el-option label="20K-30K" value="20-30" />
              <el-option label="30K-50K" value="30-50" />
              <el-option label="50K以上" value="50-" />
            </el-select>
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-section-title">技能要求</div>
          <div class="filter-options">
            <el-select
              v-model="filters.skills"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="输入或选择技能"
              size="small"
              style="width: 100%;"
              @change="applyFilters"
            >
              <el-option v-for="skill in skillOptions" :key="skill" :label="skill" :value="skill" />
            </el-select>
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-section-title">最低匹配度</div>
          <div class="filter-options">
            <el-select v-model="filters.minScore" placeholder="不限" clearable size="small" style="width: 100%;" @change="applyFilters">
              <el-option label="≥90分" value="90" />
              <el-option label="≥80分" value="80" />
              <el-option label="≥70分" value="70" />
              <el-option label="≥60分" value="60" />
              <el-option label="≥50分" value="50" />
            </el-select>
          </div>
        </div>
      </template>

      <div class="filter-actions">
        <el-button size="small" @click="resetFilters">重置</el-button>
        <el-button type="primary" size="small" @click="closeFilters">确定</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { Search, Close, Filter } from '@element-plus/icons-vue'

const props = defineProps({
  isEmployee: {
    type: Boolean,
    default: true
  },
  modelValue: {
    type: Object,
    default: () => ({
      keyword: '',
      location: [],
      salaryRange: '',
      experience: '',
      education: '',
      skills: [],
      minScore: ''
    })
  }
})

const emit = defineEmits(['update:modelValue', 'search', 'filter'])

const visible = ref(true)
const showFilters = ref(false)
const searchKeyword = ref('')

const filters = ref({
  location: [],
  salaryRange: '',
  experience: '',
  education: '',
  skills: [],
  minScore: ''
})

// 地区选项
const regionOptions = [
  { value: '北京', label: '北京', children: [{ value: '朝阳区', label: '朝阳区' }, { value: '海淀区', label: '海淀区' }, { value: '东城区', label: '东城区' }, { value: '西城区', label: '西城区' }, { value: '丰台区', label: '丰台区' }] },
  { value: '上海', label: '上海', children: [{ value: '浦东新区', label: '浦东新区' }, { value: '徐汇区', label: '徐汇区' }, { value: '静安区', label: '静安区' }, { value: '黄浦区', label: '黄浦区' }, { value: '长宁区', label: '长宁区' }] },
  { value: '广州', label: '广州', children: [{ value: '天河区', label: '天河区' }, { value: '越秀区', label: '越秀区' }, { value: '海珠区', label: '海珠区' }, { value: '荔湾区', label: '荔湾区' }] },
  { value: '深圳', label: '深圳', children: [{ value: '南山区', label: '南山区' }, { value: '福田区', label: '福田区' }, { value: '罗湖区', label: '罗湖区' }, { value: '龙岗区', label: '龙岗区' }] },
  { value: '杭州', label: '杭州', children: [{ value: '西湖区', label: '西湖区' }, { value: '滨江区', label: '滨江区' }, { value: '余杭区', label: '余杭区' }] },
  { value: '成都', label: '成都', children: [{ value: '武侯区', label: '武侯区' }, { value: '锦江区', label: '锦江区' }, { value: '高新区', label: '高新区' }] }
]

// 技能选项
const skillOptions = [
  'Java', 'Python', 'Go', 'JavaScript', 'TypeScript', 'Vue', 'React', 'Angular',
  'Node.js', 'Spring Boot', 'MySQL', 'PostgreSQL', 'MongoDB', 'Redis', 'Docker',
  'Kubernetes', 'Linux', 'Git', 'AWS', '阿里云', '微服务', '大数据', 'AI', '机器学习'
]

const activeFilterCount = computed(() => {
  let count = 0
  if (filters.value.location?.length > 0) count++
  if (filters.value.salaryRange) count++
  if (filters.value.experience) count++
  if (filters.value.education) count++
  if (filters.value.skills?.length > 0) count++
  if (filters.value.minScore) count++
  return count
})

const handleSearch = () => {
  emit('update:modelValue', { ...props.modelValue, keyword: searchKeyword.value })
  emit('search', { keyword: searchKeyword.value, ...filters.value })
}

const handleInput = () => {
  emit('update:modelValue', { ...props.modelValue, keyword: searchKeyword.value })
}

const clearSearch = () => {
  searchKeyword.value = ''
  emit('update:modelValue', { ...props.modelValue, keyword: '' })
  emit('search', { keyword: '', ...filters.value })
}

const toggleFilters = () => {
  showFilters.value = !showFilters.value
}

const closeFilters = () => {
  showFilters.value = false
}

const applyFilters = () => {
  emit('filter', { ...filters.value })
  emit('update:modelValue', { ...props.modelValue, ...filters.value })
}

const resetFilters = () => {
  filters.value = {
    location: [],
    salaryRange: '',
    experience: '',
    education: '',
    skills: [],
    minScore: ''
  }
  emit('filter', { ...filters.value })
  emit('update:modelValue', { keyword: searchKeyword.value, ...filters.value })
}

// 点击外部关闭筛选面板
const handleClickOutside = (e) => {
  const target = e.target
  if (!target.closest('.match-search-bar')) {
    showFilters.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 同步外部传入的值
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    searchKeyword.value = newVal.keyword || ''
    filters.value = {
      location: newVal.location || [],
      salaryRange: newVal.salaryRange || '',
      experience: newVal.experience || '',
      education: newVal.education || '',
      skills: newVal.skills || [],
      minScore: newVal.minScore || ''
    }
  }
}, { deep: true })
</script>

<style scoped>
.match-search-bar {
  position: relative;
  background: var(--color-surface);
  border-bottom: 1px solid var(--gray-200);
}

.search-bar-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: var(--space-3) var(--space-5);
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.search-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: var(--gray-400);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  height: 36px;
  padding: 0 var(--space-3);
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--gray-900);
  background: var(--gray-50);
  outline: none;
  transition: all var(--duration-fast) var(--ease-out);
}

.search-input:focus {
  border-color: var(--gray-400);
  background: var(--color-surface);
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.04);
}

.search-input::placeholder {
  color: var(--gray-400);
}

.clear-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: var(--gray-400);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out);
}

.clear-btn:hover {
  background: var(--gray-100);
  color: var(--gray-600);
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 36px;
  padding: 0 var(--space-3);
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--gray-600);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  white-space: nowrap;
}

.filter-btn:hover {
  border-color: var(--gray-300);
  background: var(--gray-50);
}

.filter-btn.active {
  border-color: var(--gray-400);
  background: var(--gray-100);
  color: var(--gray-900);
}

.filter-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--color-surface);
  border: 1px solid var(--gray-200);
  border-top: none;
  border-radius: 0 0 var(--radius-md) var(--radius-md);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  z-index: 100;
  padding: var(--space-4);
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: var(--space-4);
}

.filter-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.filter-section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--gray-600);
}

.filter-options {
  width: 100%;
}

.filter-actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  padding-top: var(--space-3);
  border-top: 1px solid var(--gray-100);
}

/* 响应式 */
@media (max-width: 768px) {
  .search-bar-inner {
    padding: var(--space-2) var(--space-3);
  }

  .filter-dropdown {
    grid-template-columns: 1fr;
  }
}
</style>
