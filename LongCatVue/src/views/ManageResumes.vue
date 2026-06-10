<template>
<div class="page-container page-enter">
  <div class="page-header-row">
    <div>
      <h2>{{ isAdmin ? '简历管理' : '候选人管理' }}</h2>
      <p class="page-subtitle">共 {{ total }} 份简历</p>
    </div>
    <div class="header-actions">
      <el-input v-model="searchKeyword" :placeholder="isEmployee ? '搜索感兴趣的简历...' : '搜索姓名、技能...'" :prefix-icon="Search" clearable class="search-input" @clear="fetchResumes" @keyup.enter="fetchResumes"/>
    </div>
  </div>
  <!-- AI 分析状态筛选 -->
  <div class="stats-row">
    <button class="stat-tab" :class="{ active: analysisFilter === 'all' }" @click="setAnalysisFilter('all')">
      <span class="stat-count">{{ analysisCounts.all }}</span><span class="stat-name">全部</span>
    </button>
    <button class="stat-tab" :class="{ active: analysisFilter === 'analyzed' }" @click="setAnalysisFilter('analyzed')">
      <span class="stat-count">{{ analysisCounts.analyzed }}</span><span class="stat-name">已分析</span>
    </button>
    <button class="stat-tab" :class="{ active: analysisFilter === 'pending' }" @click="setAnalysisFilter('pending')">
      <span class="stat-count">{{ analysisCounts.pending }}</span><span class="stat-name">未分析</span>
    </button>
  </div>
  <div class="table-card">
    <el-table :data="filteredResumes" v-loading="loading" stripe size="default" style="width:100%">
      <el-table-column prop="id" label="ID" width="60"/>
      <el-table-column label="姓名" width="120">
        <template #default="{ row }"><strong class="resume-name">{{ row.name || '-' }}</strong></template>
      </el-table-column>
      <el-table-column prop="age" label="年龄" width="65"/>
      <el-table-column label="技能" min-width="220">
        <template #default="{ row }">
          <div class="skill-tags-cell">
            <el-tag v-for="skill in parseSkills(row.skills).slice(0, 4)" :key="skill" size="small" effect="plain" class="skill-mini-tag">{{ skill }}</el-tag>
            <el-tag v-if="parseSkills(row.skills).length > 4" size="small" type="info" effect="plain">+{{ parseSkills(row.skills).length - 4 }}</el-tag>
            <span v-if="parseSkills(row.skills).length === 0" class="info-cell">-</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="学历" width="90"><template #default="{ row }"><span class="info-cell">{{ row.education || '-' }}</span></template></el-table-column>
      <el-table-column label="期望薪资" width="140">
        <template #default="{ row }"><span class="salary-cell">{{ formatSalary(row.expectedSalary) }}</span></template>
      </el-table-column>
      <el-table-column label="AI分析" width="100" align="center">
        <template #default="{ row }">
          <span class="status-dot" :class="row.aiAnalysis ? 'dot-success' : 'dot-default'"></span>
          <el-tag :type="row.aiAnalysis?'success':'info'" size="small" effect="light">{{ row.aiAnalysis?'已分析':'未分析' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上传时间" width="110">
        <template #default="{ row }"><span class="info-cell">{{ formatDate(row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <div class="action-cell">
            <el-button type="primary" link size="small" @click="viewResume(row)"><el-icon><View /></el-icon> 查看</el-button>
            <template v-if="!isEmployee">
              <el-dropdown trigger="click" @command="(cmd) => handleResumeCommand(cmd, row)">
                <el-button type="info" link size="small">更多 <el-icon style="margin-left:2px"><ArrowDown /></el-icon></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="analyze"><el-icon><MagicStick /></el-icon> {{ row.aiAnalysis?'重新分析':'AI分析' }}</el-dropdown-item>
                    <el-dropdown-item command="delete" divided style="color:var(--el-color-danger)"><el-icon><Delete /></el-icon> 删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination-wrapper"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @size-change="fetchResumes" @current-change="fetchResumes"/></div>
  </div>
  <el-dialog v-model="detailVisible" width="720px" destroy-on-close class="resume-detail-dialog">
    <template #header>
      <div v-if="currentResume" class="detail-custom-header">
        <div class="detail-avatar">{{ (currentResume.name || '?').charAt(0) }}</div>
        <div class="detail-header-info">
          <div class="detail-name">{{ currentResume.name || '-' }}</div>
          <div class="detail-meta">
            <span v-if="currentResume.age">{{ currentResume.age }}岁</span>
            <span v-if="currentResume.education" class="meta-sep">·</span>
            <span v-if="currentResume.education">{{ currentResume.education }}</span>
            <span v-if="currentResume.experience" class="meta-sep">·</span>
            <span v-if="currentResume.experience">{{ currentResume.experience }}</span>
          </div>
        </div>
      </div>
    </template>
    <div v-if="currentResume" class="resume-detail">
      <!-- 基本信息卡片 -->
      <div class="detail-info-grid">
        <div class="detail-info-card">
          <div class="info-card-icon" style="color:#409eff">&#128176;</div>
          <div class="info-card-content">
            <span class="info-card-label">期望薪资</span>
            <span class="info-card-value">{{ formatSalary(currentResume.expectedSalary) }}</span>
          </div>
        </div>
        <div class="detail-info-card">
          <div class="info-card-icon" style="color:#67c23a">&#128197;</div>
          <div class="info-card-content">
            <span class="info-card-label">工作年限</span>
            <span class="info-card-value">{{ currentResume.experience || '-' }}</span>
          </div>
        </div>
      </div>
      <!-- 技能标签 -->
      <div v-if="detailSkills.length" class="detail-skills-section">
        <div class="section-title">技能标签</div>
        <div class="detail-skill-tags">
          <el-tag v-for="skill in detailSkills" :key="skill" size="small" effect="plain" class="detail-skill-tag">{{ skill }}</el-tag>
        </div>
      </div>
      <!-- 工作经历 - 时间轴 -->
      <div v-if="currentResume.workExperience" class="work-exp-section">
        <div class="section-title">工作经历</div>
        <div class="timeline">
          <div v-for="(exp,i) in parseWorkExp(currentResume.workExperience)" :key="i" class="timeline-item">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <div class="exp-header"><strong>{{ exp.company||'未知公司' }}</strong><el-tag size="small" effect="plain">{{ exp.position||'-' }}</el-tag></div>
              <div class="exp-date">{{ exp.startDate||'' }} ~ {{ exp.current?'至今':(exp.endDate||'') }}</div>
              <p v-if="exp.description" class="exp-desc">{{ exp.description }}</p>
            </div>
          </div>
        </div>
      </div>
      <!-- AI 分析报告 -->
      <div v-if="currentResume.aiAnalysis" class="ai-section">
        <div class="section-title">AI 智能分析报告</div>
        <div v-if="parsedAnalysis" class="analysis-content">
          <div class="score-banner">
            <div class="score-circle" :style="{borderColor:getScoreColor(parsedAnalysis.total_score)}">
              <span class="score-num" :style="{color:getScoreColor(parsedAnalysis.total_score)}">{{ parsedAnalysis.total_score||0 }}</span>
              <span class="score-unit">分</span>
            </div>
            <div class="score-info">
              <el-tag :type="getScoreTagType(parsedAnalysis.total_score)" size="large" effect="dark">{{ parsedAnalysis.score_level||'-' }}</el-tag>
              <p class="score-desc">综合评分基于5个维度的深度分析</p>
            </div>
          </div>
          <div class="radar-section"><div ref="radarChartRef" class="radar-chart"></div></div>
          <div class="analysis-block">
            <h5>维度评分明细</h5>
            <div class="dim-grid">
              <div v-for="(dim,key) in parsedAnalysis.dimensions" :key="key" class="dim-card">
                <div class="dim-header"><span class="dim-name">{{ getDimLabel(key) }}</span><span class="dim-score">{{ dim.score }}/{{ dim.max }}</span></div>
                <el-progress :percentage="Math.round(dim.score/dim.max*100)" :color="getDimColor(key)" :stroke-width="6"/>
              </div>
            </div>
          </div>
          <div class="analysis-sections">
            <div class="analysis-block" v-if="parsedAnalysis.strengths?.length">
              <h5 class="block-strength">核心优势</h5>
              <ul class="strength-list"><li v-for="item in parsedAnalysis.strengths" :key="item">{{ item }}</li></ul>
            </div>
            <div class="analysis-block" v-if="parsedAnalysis.weaknesses?.length">
              <h5 class="block-weakness">待提升</h5>
              <ul class="weakness-list"><li v-for="item in parsedAnalysis.weaknesses" :key="item">{{ item }}</li></ul>
            </div>
          </div>
          <div class="analysis-block"><h5>综合评价</h5><p class="comment">{{ parsedAnalysis.overall_comment }}</p></div>
        </div>
        <pre v-else class="raw-json">{{ formatJsonDisplay(currentResume.aiAnalysis) }}</pre>
      </div>
    </div>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
      <el-button type="primary" :loading="exportingPdf" @click="handleExportPdf"><el-icon><Download /></el-icon>导出PDF</el-button>
    </template>
  </el-dialog>
</div></template>

<script setup>
import { ref, onMounted, computed, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, View, MagicStick, Delete, Download, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { exportResumeToPdf } from '../utils/pdfExport'

const resumes = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')

// ── AI 分析状态筛选 ────────────────────────
const analysisFilter = ref('all')
const analysisCounts = computed(() => ({
  all: total.value,
  analyzed: resumes.value.filter(r => r.aiAnalysis).length,
  pending: resumes.value.filter(r => !r.aiAnalysis).length
}))
const filteredResumes = computed(() => {
  if (analysisFilter.value === 'all') return resumes.value
  if (analysisFilter.value === 'analyzed') return resumes.value.filter(r => r.aiAnalysis)
  if (analysisFilter.value === 'pending') return resumes.value.filter(r => !r.aiAnalysis)
  return resumes.value
})
const setAnalysisFilter = (filter) => {
  analysisFilter.value = filter
  page.value = 1
  fetchResumes()
}

// ── 表格辅助函数 ────────────────────────────
const parseSkills = (skills) => {
  if (!skills) return []
  try {
    const parsed = typeof skills === 'string' ? JSON.parse(skills) : skills
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return skills.split(/[,，、;；\s]+/).filter(s => s.trim())
  }
}
const formatSalary = (salary) => {
  if (!salary) return '面议'
  return salary.toLocaleString() + ' 元/月'
}
const formatDate = (t) => {
  if (!t) return '-'
  return new Date(t).toLocaleDateString('zh-CN')
}
const handleResumeCommand = (command, row) => {
  if (command === 'analyze') analyzeResume(row)
  else if (command === 'delete') deleteResume(row)
}

const detailVisible = ref(false)
const currentResume = ref(null)
const detailSkills = computed(() => parseSkills(currentResume.value?.skills))
const exportingPdf = ref(false)

const userStore = useUserStore()
const isEmployee = computed(() => userStore.userType === 'EMPLOYEE')
const isEmployer = computed(() => userStore.userType === 'EMPLOYER')
const isAdmin = computed(() => userStore.userType === 'ADMIN')

// 解析AI分析结果 - 多维度增强版 v3
const radarChartRef = ref(null)
let radarChart = null

// 修复字符串值内的真实换行符（AI经常在长文本中插入\n）
const fixNewlinesInStrings = (str) => {
  const result = []
  let inString = false
  let escaped = false
  for (let i = 0; i < str.length; i++) {
    const ch = str[i]
    if (escaped) {
      result.push(ch)
      escaped = false
      continue
    }
    if (ch === '\\') {
      result.push(ch)
      escaped = true
      continue
    }
    if (ch === '"') {
      inString = !inString
      result.push(ch)
      continue
    }
    if (inString && (ch === '\n' || ch === '\r')) {
      // 字符串内的真实换行替换为转义换行
      result.push('\\n')
      continue
    }
    result.push(ch)
  }
  return result.join('')
}

// 修复单引号包裹的键或值，同时保留值中的撇号（如 it's）
const fixSingleQuotes = (str) => {
  const result = []
  let i = 0
  while (i < str.length) {
    const ch = str[i]
    if (ch === '"') {
      result.push(ch); i++
      while (i < str.length && str[i] !== '"') {
        if (str[i] === '\\') { result.push(str[i]); i++ }
        result.push(str[i]); i++
      }
      if (i < str.length) { result.push(str[i]); i++ }
      continue
    }
    if (ch === "'") {
      let j = i + 1, closed = false
      while (j < str.length) {
        if (str[j] === '\\') { j += 2; continue }
        if (str[j] === "'") { closed = true; break }
        j++
      }
      if (closed) {
        const inner = str.substring(i + 1, j)
        if (!inner.includes('"')) {
          result.push('"'); result.push(inner); result.push('"')
          i = j + 1; continue
        }
      }
    }
    result.push(ch); i++
  }
  return result.join('')
}

// 修复无引号的属性名（只在字符串外部操作）
const fixUnquotedKeys = (str) => {
  const result = []
  let i = 0
  while (i < str.length) {
    const ch = str[i]
    // 跳过双引号字符串
    if (ch === '"') {
      result.push(ch); i++
      while (i < str.length && str[i] !== '"') {
        if (str[i] === '\\') { result.push(str[i]); i++ }
        result.push(str[i]); i++
      }
      if (i < str.length) { result.push(str[i]); i++ }
      continue
    }
    // 检测无引号的属性名模式：逗号/花括号后的空白+字母+冒号
    if (ch === ',' || ch === '{') {
      result.push(ch); i++
      // 跳过空白
      let ws = ''
      while (i < str.length && /\s/.test(str[i])) { ws += str[i]; i++ }
      // 检查是否是 word: 模式（但不是 "word":）
      if (i < str.length && /[a-zA-Z_]/.test(str[i])) {
        let word = ''
        let j = i
        while (j < str.length && /\w/.test(str[j])) { word += str[j]; j++ }
        // 确认后面是冒号，且前面没有引号
        while (j < str.length && /\s/.test(str[j])) { j++ }
        if (j < str.length && str[j] === ':') {
          // 这是无引号的属性名
          result.push(ws)
          result.push('"')
          result.push(word)
          result.push('"')
          i += word.length
          continue
        }
      }
      result.push(ws)
      continue
    }
    result.push(ch); i++
  }
  return result.join('')
}

// 修复未闭合的数组和对象（AI输出截断常见问题）
const fixUnclosedBrackets = (str) => {
  const stack = []
  let inString = false
  let escape = false
  for (let i = 0; i < str.length; i++) {
    const ch = str[i]
    if (escape) { escape = false; continue }
    if (ch === '\\' && inString) { escape = true; continue }
    if (ch === '"') { inString = !inString; continue }
    if (inString) continue
    if (ch === '{' || ch === '[') { stack.push(ch === '{' ? '}' : ']') }
    else if (ch === '}' || ch === ']') {
      if (stack.length > 0 && stack[stack.length - 1] === ch) stack.pop()
    }
  }
  // 从内到外闭合，先去掉尾部多余逗号
  let result = str.replace(/\s*$/, '')
  while (stack.length > 0) {
    result = result.replace(/,\s*$/, '')
    result += stack.pop()
  }
  return result
}

// 规范化雷达图数据：修正AI返回的格式问题
const normalizeRadarData = (radar_data, dimensions) => {
  const defaultLabels = ['基本信息', '技能水平', '工作经历', '教育背景', '职业潜力']
  const defaultMax = [15, 35, 30, 10, 10]
  const dimKeys = ['basic_info', 'skill', 'experience', 'education', 'potential']

  if (!radar_data) radar_data = {}

  let labels = Array.isArray(radar_data.labels) && radar_data.labels.length === 5 ? radar_data.labels : defaultLabels
  let max_values = Array.isArray(radar_data.max_values) && radar_data.max_values.length === 5 ? radar_data.max_values.map(Number) : defaultMax
  let values = Array.isArray(radar_data.values) ? radar_data.values.map(Number) : [0, 0, 0, 0, 0]

  // 补齐或截断到5个元素
  while (values.length < 5) values.push(0)
  values = values.slice(0, 5)

  // 判断是否为百分制：如果所有非零值都 > 对应 max（且 max < 100），说明AI返回的是百分制
  const nonZeroIndices = values.map((v, i) => v > 0 ? i : -1).filter(i => i >= 0)
  const isPercentage = nonZeroIndices.length > 0 && nonZeroIndices.every(i => values[i] > max_values[i]) && max_values.some(m => m < 100)

  if (isPercentage) {
    values = values.map((v, i) => Math.round(v * max_values[i] / 100))
  }

  // 如果大部分值为0且 dimensions 有分数，从 dimensions 补充
  const zeroCount = values.filter(v => v === 0).length
  if (zeroCount >= 3 && dimensions && Object.keys(dimensions).length > 0) {
    for (let i = 0; i < 5; i++) {
      const dim = dimensions[dimKeys[i]]
      if (dim && typeof dim.score === 'number' && dim.score > 0 && values[i] === 0) {
        values[i] = dim.score
      }
    }
  }

  // 确保每个值不超过对应的 max
  values = values.map((v, i) => Math.min(Math.max(0, v), max_values[i]))

  return { labels, values, max_values }
}

const parsedAnalysis = computed(() => {
  if (!currentResume.value?.aiAnalysis) return null
  try {
    let jsonStr = currentResume.value.aiAnalysis.trim()

    // 去掉markdown代码块
    const codeBlockMatch = jsonStr.match(/```(?:json)?\s*([\s\S]*?)```/)
    if (codeBlockMatch) {
      jsonStr = codeBlockMatch[1].trim()
    }

    // 提取第一个完整的JSON对象（忽略前后多余文字）
    const jsonStart = jsonStr.indexOf('{')
    const jsonEnd = jsonStr.lastIndexOf('}')
    if (jsonStart !== -1 && jsonEnd > jsonStart) {
      jsonStr = jsonStr.substring(jsonStart, jsonEnd + 1)
    }

    // 容错：去掉JSON中的行注释（但不破坏字符串内的//）
    jsonStr = jsonStr.replace(/^\s*\/\/.*$/gm, '')
    // 容错：去掉尾部逗号
    jsonStr = jsonStr.replace(/,\s*([}\]])/g, '$1')
    // 容错：去掉字符串值内的换行符（AI经常在长文本中插入真实换行）
    jsonStr = fixNewlinesInStrings(jsonStr)
    // 容错：修复单引号包裹的键或值
    jsonStr = fixSingleQuotes(jsonStr)
    // 容错：给没有引号的属性名加引号（只在字符串外部）
    jsonStr = fixUnquotedKeys(jsonStr)
    // 容错：修复未闭合的数组/对象（AI输出截断）
    jsonStr = fixUnclosedBrackets(jsonStr)

    const parsed = JSON.parse(jsonStr)
    
    // 增强容错：从 dimensions 中提取优势/劣势
    const dimensions = parsed.dimensions || {}
    const strengths = parsed.strengths || []
    const weaknesses = parsed.weaknesses || []
    
    // 如果 strengths/weaknesses 为空，尝试从 dimensions 的 comment 中提取
    if (strengths.length === 0 || weaknesses.length === 0) {
      Object.values(dimensions).forEach(dim => {
        if (dim?.comment) {
          const comment = dim.comment
          // 简单判断：包含正面词汇的归为优势，负面词汇的归为劣势
          if (comment.includes('优秀') || comment.includes('丰富') || comment.includes('完整') || comment.includes('良好') || comment.includes('强')) {
            if (strengths.length === 0) strengths.push(comment)
          } else if (comment.includes('不足') || comment.includes('缺乏') || comment.includes('矛盾') || comment.includes('不符') || comment.includes('缺失')) {
            if (weaknesses.length === 0) weaknesses.push(comment)
          }
        }
      })
    }
    
    return {
      total_score: parsed.total_score || 0,
      score_level: parsed.score_level || '-',
      radar_data: normalizeRadarData(parsed.radar_data, dimensions),
      dimensions: dimensions,
      strengths: strengths,
      weaknesses: weaknesses,
      competitive_analysis: parsed.competitive_analysis || { market_position: '-', core_competitiveness: '-', improvement_potential: '-' },
      career_suggestions: parsed.career_suggestions || { recommended_positions: [], salary_range: '-', development_path: '-' },
      learning_plan: parsed.learning_plan || { short_term: [], long_term: [] },
      interview_tips: parsed.interview_tips || { focus_areas: [], suggested_questions: [] },
      overall_comment: parsed.overall_comment || generateOverallComment(dimensions),
      basic_info: parsed.basic_info || { skills: [], experience_level: '-', education_level: '-', potential_score: 0, potential_level: '-' }
    }
  } catch (e) {
    console.error('AI分析解析失败:', e)
    // 最后手段：用正则提取关键字段，避免完全无显示
    try {
      const raw = currentResume.value.aiAnalysis
      const scoreMatch = raw.match(/"?total_score"?\s*[:：]\s*(\d+)/)
      const levelMatch = raw.match(/"?score_level"?\s*[:：]\s*"([^"]+)"/)
      const commentMatch = raw.match(/"?overall_comment"?\s*[:：]\s*"([^"]*?)"/)
      if (scoreMatch) {
        // 尝试用正则提取 radar_data values
        const valuesMatch = raw.match(/"?values"?\s*[:：]\s*\[([^\]]*)\]/)
        let radarValues = [0, 0, 0, 0, 0]
        if (valuesMatch) {
          const nums = valuesMatch[1].split(/[,，]/).map(s => parseFloat(s.trim())).filter(n => !isNaN(n))
          if (nums.length >= 3) radarValues = nums
        }
        return {
          total_score: parseInt(scoreMatch[1]) || 0,
          score_level: levelMatch?.[1] || '-',
          radar_data: normalizeRadarData({ labels: ['基本信息','技能水平','工作经历','教育背景','职业潜力'], values: radarValues, max_values: [15,35,30,10,10] }),
          dimensions: {}, strengths: [], weaknesses: [],
          competitive_analysis: { market_position: '-', core_competitiveness: '-', improvement_potential: '-' },
          career_suggestions: { recommended_positions: [], salary_range: '-', development_path: '-' },
          learning_plan: { short_term: [], long_term: [] },
          interview_tips: { focus_areas: [], suggested_questions: [] },
          overall_comment: commentMatch?.[1] || '-',
          basic_info: { skills: [], experience_level: '-', education_level: '-', potential_score: 0, potential_level: '-' }
        }
      }
    } catch { /* ignore */ }
    return null
  }
})

// 从维度评分生成综合评价
const generateOverallComment = (dimensions) => {
  if (!dimensions || Object.keys(dimensions).length === 0) return '-'
  const comments = []
  Object.entries(dimensions).forEach(([key, dim]) => {
    if (dim?.comment) {
      comments.push(dim.comment)
    }
  })
  return comments.join('；') || '-'
}

const getScoreColor = (score) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#409eff'
  if (score >= 40) return '#e6a23c'
  return '#f56c6c'
}

const getScoreTagType = (score) => {
  if (score >= 80) return 'success'
  if (score >= 60) return ''
  if (score >= 40) return 'warning'
  return 'danger'
}

const dimLabelMap = { basic_info: '基本信息', skill: '技能水平', experience: '工作经历', education: '教育背景', potential: '职业潜力' }
const getDimLabel = (key) => dimLabelMap[key] || key

const dimColorMap = { basic_info: '#5470c6', skill: '#91cc75', experience: '#fac858', education: '#73c0de', potential: '#ee6666' }
const getDimColor = (key) => dimColorMap[key] || '#409eff'

const renderRadarChart = () => {
  if (!radarChartRef.value || !parsedAnalysis.value?.radar_data) return
  if (radarChart) radarChart.dispose()
  radarChart = echarts.init(radarChartRef.value)
  const { labels, values, max_values } = parsedAnalysis.value.radar_data
  const indicator = labels.map((label, i) => ({ name: label, max: max_values[i] || 100 }))
  radarChart.setOption({
    radar: { indicator, shape: 'polygon', radius: '70%', splitNumber: 4 },
    series: [{ type: 'radar', data: [{ value: values, areaStyle: { color: 'rgba(64, 158, 255, 0.2)' }, lineStyle: { color: '#409eff', width: 2 }, itemStyle: { color: '#409eff' } }] }]
  })
}

watch(() => detailVisible.value, async (val) => {
  if (val && parsedAnalysis.value?.radar_data) {
    await nextTick()
    setTimeout(renderRadarChart, 300)
  }
})

const fetchResumes = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value

    const res = await request.get('/resume/page', { params })

    resumes.value = res.records || []
    total.value = res.total || 0
  } catch {
    resumes.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const viewResume = (row) => {
  currentResume.value = row
  detailVisible.value = true
}

const analyzeResume = async (row) => {
  // 检查用户是否有AI分析权限
  if (isEmployee.value) {
    ElMessage.warning('求职者用户无法进行AI分析')
    return
  }

  try {
    await request.post(`/resume/${row.id}/ai-analyze`)
    ElMessage.success('AI 分析完成')
    fetchResumes()
  } catch {
    ElMessage.error('AI 分析失败')
  }
}

const deleteResume = async (row) => {
  if (isEmployee.value) {
    ElMessage.warning('求职者用户无法删除简历')
    return
  }
  try {
    await ElMessageBox.confirm('确定删除该简历吗？删除后不可恢复', '警告', { type: 'warning' })
    await request.delete(`/resume/${row.id}`)
    ElMessage.success('删除成功')
    fetchResumes()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const parseWorkExp = (json) => {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

const formatTime = (t) => {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

// 格式化JSON显示
const formatJsonDisplay = (jsonStr) => {
  if (!jsonStr) return '-'
  try {
    let cleanStr = jsonStr.trim()
    // 去掉markdown代码块
    const codeBlockMatch = cleanStr.match(/```(?:json)?\s*([\s\S]*?)```/)
    if (codeBlockMatch) {
      cleanStr = codeBlockMatch[1].trim()
    }
    // 提取JSON对象
    const jsonStart = cleanStr.indexOf('{')
    const jsonEnd = cleanStr.lastIndexOf('}')
    if (jsonStart !== -1 && jsonEnd > jsonStart) {
      cleanStr = cleanStr.substring(jsonStart, jsonEnd + 1)
    }
    // 清理
    cleanStr = cleanStr.replace(/^\s*\/\/.*$/gm, '')
    cleanStr = cleanStr.replace(/,\s*([}\]])/g, '$1')
    cleanStr = fixNewlinesInStrings(cleanStr)
    cleanStr = fixSingleQuotes(cleanStr)
    cleanStr = fixUnquotedKeys(cleanStr)
    return JSON.stringify(JSON.parse(cleanStr), null, 2)
  } catch {
    return jsonStr
  }
}

// 导出PDF
const handleExportPdf = async () => {
  if (!currentResume.value) return
  exportingPdf.value = true
  try {
    await exportResumeToPdf({
      name: currentResume.value.name,
      age: currentResume.value.age,
      education: currentResume.value.education,
      skills: currentResume.value.skills,
      experience: currentResume.value.experience,
      expectedSalary: currentResume.value.expectedSalary,
      workExperience: currentResume.value.workExperience
    })
    ElMessage.success('PDF导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('PDF导出失败，请重试')
  } finally {
    exportingPdf.value = false
  }
}

onMounted(() => {
  fetchResumes()
})
</script>

<style scoped>
/* ── 页面布局 ─────────────────────────────── */
.table-card { padding: var(--space-3); margin-bottom: 0; }
.header-actions { display: flex; align-items: center; gap: var(--space-3); }
.search-input { width: 260px; }

/* ── 表格候选人姓名 ───────────────────────── */
.resume-name {
  font-weight: 600;
  color: var(--gray-800);
  transition: color var(--duration-fast);
}
.resume-name:hover { color: var(--primary-500); }

/* ── 表格单元格样式 ───────────────────────── */
.skill-tags-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}
.skill-mini-tag { font-size: 11px; }
.salary-cell {
  font-weight: 600;
  color: var(--gray-700);
  font-variant-numeric: tabular-nums;
}
.info-cell {
  color: var(--gray-600);
  font-size: var(--text-sm);
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
.dot-success { background: var(--success-500, #67c23a); }
.dot-default { background: var(--gray-400); }

/* ── 操作按钮布局 ─────────────────────────── */
.action-cell {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

/* ── 简历详情弹窗 ─────────────────────────── */
.resume-detail { display: flex; flex-direction: column; gap: var(--space-5); }

/* 自定义头部 */
.detail-custom-header {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}
.detail-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #67c23a);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
  flex-shrink: 0;
}
.detail-header-info { display: flex; flex-direction: column; gap: 4px; }
.detail-name { font-size: 20px; font-weight: 700; color: var(--gray-900); }
.detail-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--gray-500);
}
.meta-sep { color: var(--gray-300); }

/* 基本信息卡片 */
.detail-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3);
}
.detail-info-card {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: var(--gray-50);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}
.info-card-icon { font-size: 24px; flex-shrink: 0; }
.info-card-content { display: flex; flex-direction: column; }
.info-card-label { font-size: 11px; color: var(--color-text-muted); }
.info-card-value { font-size: var(--text-sm); font-weight: 600; color: var(--gray-800); }

/* 技能标签区 */
.detail-skills-section {
  padding: var(--space-3) var(--space-4);
  background: var(--gray-50);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-800);
  margin-bottom: var(--space-3);
  padding-left: var(--space-3);
  border-left: 3px solid var(--primary-500);
}
.detail-skill-tags { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.detail-skill-tag { font-size: 12px; }

/* ── 工作经历 - 时间轴 ─────────────────────── */
.work-exp-section { margin-top: 0; }
.timeline { position: relative; padding-left: 24px; }
.timeline::before {
  content: '';
  position: absolute;
  left: 6px;
  top: 4px;
  bottom: 4px;
  width: 2px;
  background: var(--gray-200);
}
.timeline-item {
  position: relative;
  padding-bottom: var(--space-4);
}
.timeline-item:last-child { padding-bottom: 0; }
.timeline-dot {
  position: absolute;
  left: -20px;
  top: 6px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--primary-500);
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px var(--primary-200);
  z-index: 1;
}
.timeline-content {
  background: var(--gray-50);
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--color-border);
}
.exp-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-1);
}
.exp-header strong { color: var(--gray-900); font-size: var(--text-sm); }
.exp-date { font-size: var(--text-xs); color: var(--gray-400); margin-bottom: var(--space-2); }
.exp-desc {
  font-size: var(--text-sm);
  color: var(--gray-700);
  line-height: 1.6;
  margin: 0;
}

/* ── AI 分析区域 ──────────────────────────── */
.ai-section {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: var(--radius-md);
  padding: var(--space-4);
}
.analysis-content { display: flex; flex-direction: column; gap: var(--space-4); }
.analysis-sections {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3);
}
.analysis-block h5 {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--gray-700);
  margin-bottom: var(--space-2);
  padding-bottom: var(--space-1);
  border-bottom: 1px solid var(--color-border);
}
.block-strength { color: #67c23a; border-color: #67c23a; }
.block-weakness { color: #f56c6c; border-color: #f56c6c; }
.strength-list, .weakness-list {
  margin: 0;
  padding-left: var(--space-5);
  font-size: var(--text-sm);
  line-height: var(--leading-relaxed);
}
.strength-list li { color: #555; }
.weakness-list li { color: #555; }
.comment, .raw-json {
  font-size: var(--text-sm);
  color: var(--gray-700);
  line-height: 1.6;
  margin: 0;
  background: var(--gray-50);
  padding: var(--space-3);
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
}
.raw-json {
  white-space: pre-wrap;
  max-height: 400px;
  overflow-y: auto;
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

/* ── 评分横幅 ─────────────────────────────── */
.score-banner {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  padding: var(--space-5);
  background: var(--gray-50);
  border-radius: var(--radius-md);
}
.score-circle {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  border: 5px solid var(--gray-400);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--color-surface);
  flex-shrink: 0;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.score-num { font-size: 32px; font-weight: 800; line-height: 1; }
.score-unit { font-size: var(--text-xs); color: var(--gray-400); margin-top: 2px; }
.score-info { display: flex; flex-direction: column; gap: var(--space-2); }
.score-desc { font-size: var(--text-xs); color: var(--gray-400); margin: 0; }

/* ── 雷达图 ───────────────────────────────── */
.radar-section { padding: var(--space-2) 0; }
.radar-chart { width: 100%; height: 260px; }

/* ── 维度卡片网格 ───────────────────────────── */
.dim-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-2);
}
.dim-card {
  background: var(--gray-50);
  border-radius: var(--radius-md);
  padding: var(--space-3);
  border: 1px solid var(--color-border);
}
.dim-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-2);
}
.dim-name { font-size: var(--text-sm); font-weight: var(--weight-semibold); color: var(--gray-700); }
.dim-score { font-size: var(--text-sm); font-weight: var(--weight-bold); color: var(--gray-600); }
</style>