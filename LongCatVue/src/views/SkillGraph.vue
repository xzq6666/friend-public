<template>
  <div class="skill-graph-page">
    <!-- 顶部区域 -->
    <div class="graph-header">
      <div class="header-left">
        <h1>能力图谱</h1>
        <p class="header-desc">可视化展示技能体系</p>
      </div>
      <div class="header-right">
        <el-select v-model="graphType" placeholder="类型" style="width:130px" @change="switchGraphType">
          <el-option label="个人" value="personal" />
          <el-option label="职位" value="job" />
        </el-select>
        <el-select v-if="selectedIndustry && industries.length" v-model="selectedIndustry" style="width:120px" @change="onIndustryChange">
          <el-option label="全部" value="" />
          <el-option v-for="ind in industries" :key="ind" :label="ind" :value="ind" />
        </el-select>
        <el-button v-if="isAdmin" @click="syncSkills" :loading="syncing" plain>
          <el-icon><Download /></el-icon>
        </el-button>
        <el-button @click="refreshGraph" :loading="loading" type="primary">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row" v-if="graphData.nodes.length">
      <div class="stat-card">
        <div class="stat-icon" style="background: #e8f4fd; color: #1890ff;">
          <el-icon :size="24"><Connection /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ graphData.nodes.length }}</div>
          <div class="stat-label">技能总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #f6ffed; color: #52c41a;">
          <el-icon :size="24"><Grid /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ graphData.categories.length }}</div>
          <div class="stat-label">技能分类</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #fff7e6; color: #fa8c16;">
          <el-icon :size="24"><Star /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ coreSkillsCount }}</div>
          <div class="stat-label">核心技能</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #f9f0ff; color: #722ed1;">
          <el-icon :size="24"><Share /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ graphData.links.length }}</div>
          <div class="stat-label">技能关联</div>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="graph-body">
      <!-- 左侧：图谱 -->
      <div class="graph-main">
        <div class="graph-card">
          <div class="card-header">
            <div class="header-info">
              <h3>{{ graphType === 'personal' ? '个人能力图谱' : '职位能力图谱' }}</h3>
              <div class="header-actions-group">
                <el-input 
                  v-model="searchKeyword" 
                  placeholder="搜索技能..." 
                  prefix-icon="Search"
                  style="width: 200px"
                  clearable
                />
                <el-radio-group v-model="layoutMode" size="small" @change="updateLayout">
                  <el-radio-button value="force">力导向</el-radio-button>
                  <el-radio-button value="circular">环形</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </div>
          
          <el-alert v-if="errorMessage" :title="errorMessage" type="warning" :closable="true" show-icon class="error-alert" />
          
          <div ref="chartRef" class="chart-container" v-loading="loading"></div>
          
          <!-- 底部操作栏 -->
          <div class="chart-footer">
            <div class="footer-tip">
              <el-icon><InfoFilled /></el-icon>
              <span>拖拽节点可调整位置，滚轮缩放，点击节点高亮关联</span>
            </div>
            <div class="footer-actions">
              <el-button size="small" @click="resetView">
                <el-icon><Refresh /></el-icon>
                重置视图
              </el-button>
              <el-button size="small" @click="expandAll">
                <el-icon><Sort /></el-icon>
                展开全部
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：面板 -->
      <div class="graph-sidebar">
        <!-- 技能详情 -->
        <div class="sidebar-card" v-if="selectedSkill">
          <div class="card-title">
            <span>技能详情</span>
            <el-button text size="small" @click="selectedSkill = null">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          <div class="skill-detail">
            <div class="detail-header">
              <span class="detail-dot" :style="{backgroundColor: getNodeColor(selectedSkill.category)}"></span>
              <h4>{{ selectedSkill.name }}</h4>
            </div>
            <div class="detail-info">
              <div class="detail-item">
                <span class="detail-label">分类</span>
                <span class="detail-value">{{ getCategoryName(selectedSkill.category) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">重要度</span>
                <el-progress :percentage="selectedSkill.symbolSize || 30" :show-text="false" :stroke-width="6" />
              </div>
              <div class="detail-item">
                <span class="detail-label">关联数</span>
                <span class="detail-value">{{ getRelatedCount(selectedSkill.name) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 技能列表 -->
        <div class="sidebar-card">
          <div class="card-title">技能列表</div>
          <div class="skill-list">
            <div v-for="(node,index) in graphData.nodes" :key="index" 
                 class="skill-item" 
                 :class="{'active': selectedSkill?.name === node.name}"
                 @click="selectSkill(node)">
              <span class="skill-dot" :style="{backgroundColor: getNodeColor(node.category)}"></span>
              <span class="skill-name">{{ node.name }}</span>
              <el-tag v-if="node.symbolSize>40" size="small" type="success" effect="plain">核心</el-tag>
            </div>
            <div v-if="!graphData.nodes.length" class="empty-tip">暂无技能数据</div>
          </div>
        </div>

        <!-- 职位选择 -->
        <div v-if="graphType==='job'" class="sidebar-card">
          <div class="card-title">选择职位</div>
          <el-select v-model="selectedJobId" placeholder="请选择职位" filterable @change="loadJobGraph" style="width:100%">
            <el-option v-for="job in jobList" :key="job.id" :label="job.title" :value="job.id" />
          </el-select>
        </div>

        <!-- 分类统计 -->
        <div class="sidebar-card">
          <div class="card-title">分类统计</div>
          <div class="category-stats">
            <div v-for="(cat,index) in graphData.categories" :key="index" class="category-item">
              <div class="category-header">
                <span class="category-dot" :style="{backgroundColor: colorList[index]}"></span>
                <span class="category-name">{{ cat.name }}</span>
                <span class="category-count">{{ getCategoryCount(index) }}</span>
              </div>
              <el-progress :percentage="getCategoryPercentage(index)" :show-text="false" :stroke-width="4" :color="colorList[index]" />
            </div>
          </div>
        </div>

        <!-- 图例说明 -->
        <div class="sidebar-card">
          <div class="card-title">分类图例</div>
          <div class="legend-list">
            <div v-for="(cat,index) in graphData.categories" :key="index" class="legend-item">
              <span class="legend-dot" :style="{backgroundColor: colorList[index]}"></span>
              <span>{{ cat.name }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { Refresh, Download, Connection, Grid, Star, Share, Search, Sort, InfoFilled, Close } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const isAdmin = userStore.isAdmin

const chartRef = ref(null)
const loading = ref(false)
const graphType = ref('personal')
const selectedJobId = ref(null)
const jobList = ref([])
const errorMessage = ref('')
const selectedIndustry = ref('')
const industries = ref([])
const autoBuilding = ref(false)
const syncing = ref(false)
const graphMeta = ref({ source: '', resumeCount: 0, jobCount: 0 })
const searchKeyword = ref('')
const layoutMode = ref('force')
const selectedSkill = ref(null)
let chart = null

// 计算属性
const coreSkillsCount = computed(() => {
  return graphData.value.nodes.filter(n => n.symbolSize > 40).length
})

const graphData = ref({
  nodes: [],
  links: [],
  categories: []
})

const colorList = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452',
  '#9a60b4', '#ea7ccc', '#48b8d0', '#f5a623', '#8b5cf6', '#06b6d4', '#84cc16'
]

// 技能分类与颜色映射
const categoryColorMap = {
  '编程语言': '#5470c6',
  '前端框架': '#91cc75',
  '后端框架': '#fac858',
  '数据库': '#ee6666',
  '开发工具': '#73c0de',
  '软技能': '#3ba272',
  '核心技能': '#9a60b4',
  '工具技能': '#48b8d0',
  '设计工具': '#ea7ccc',
  '办公软件': '#f5a623',
  '技术技能': '#8b5cf6',
  '管理技能': '#06b6d4',
  '专业技能': '#84cc16',
  '其他': '#fc8452'
}

// 获取节点颜色
const getNodeColor = (categoryIndex) => {
  return colorList[categoryIndex] || colorList[0]
}

// 初始化图表
const initChart = () => {
  if (chartRef.value) {
    chart = echarts.init(chartRef.value)
    window.addEventListener('resize', handleResize)
  }
}

// 处理窗口大小变化
const handleResize = () => {
  chart?.resize()
}

// 渲染图谱
const renderGraph = () => {
  if (!chart || !graphData.value.nodes.length) return

  // 为分类添加颜色
  const categoriesWithColor = graphData.value.categories.map((cat, idx) => ({
    ...cat,
    itemStyle: { color: categoryColorMap[cat.name] || colorList[idx % colorList.length] }
  }))

  const isCircular = layoutMode.value === 'circular'
  const containerWidth = chartRef.value?.offsetWidth || 800
  const containerHeight = chartRef.value?.offsetHeight || 650
  const centerX = containerWidth / 2
  const centerY = containerHeight / 2
  const radius = Math.min(containerWidth, containerHeight) * 0.3
  
  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(0, 0, 0, 0.85)',
      borderColor: 'transparent',
      borderRadius: 8,
      padding: [12, 16],
      textStyle: { 
        color: '#fff',
        fontSize: 13
      },
      formatter: (params) => {
        if (params.dataType === 'node') {
          const catName = categoriesWithColor[params.data.category]?.name || '未知'
          const isCenter = params.name === '我的技能' || params.name === '职位要求'
          return `
            <div style="padding: 4px 0;">
              <div style="font-size: 15px; font-weight: 600; margin-bottom: 8px; color: #fff;">${params.name}</div>
              <div style="display: flex; align-items: center; gap: 6px; font-size: 12px; color: #b0b0b0;">
                <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: ${categoryColorMap[catName] || '#fff'};"></span>
                <span>${catName}</span>
              </div>
              ${isCenter ? '<div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid rgba(255,255,255,0.1); font-size: 11px; color: #888; text-align: center;">核心能力中心</div>' : ''}
            </div>
          `
        }
        return ''
      }
    },
    animationDuration: isCircular ? 800 : 1200,
    animationEasingUpdate: 'cubicInOut',
    series: [
      {
        type: 'graph',
        layout: 'force',
        force: {
          repulsion: isCircular ? 0 : 600,
          gravity: isCircular ? 0 : 0.08,
          edgeLength: isCircular ? 0 : [180, 280],
          layoutAnimation: !isCircular,
          friction: isCircular ? 0 : 0.6,
          initLayout: isCircular ? 'circular' : undefined
        },
        data: graphData.value.nodes.map((node, index) => {
          const isCenter = node.name === '我的技能' || node.name === '职位要求'
          const catIdx = node.category || 0
          const catColor = categoriesWithColor[catIdx]?.itemStyle?.color || colorList[catIdx]
          
          return {
            ...node,
            id: String(index),
            category: catIdx,
            symbolSize: isCenter ? 70 : (node.symbolSize || 35),
            itemStyle: {
              color: {
                type: 'radial',
                x: 0.3,
                y: 0.3,
                r: 0.7,
                colorStops: [
                  { offset: 0, color: isCenter ? '#667eea' : catColor },
                  { offset: 1, color: isCenter ? '#764ba2' : catColor + 'cc' }
                ]
              },
              shadowBlur: isCenter ? 25 : 15,
              shadowColor: isCenter ? 'rgba(102, 126, 234, 0.5)' : catColor + '66',
              shadowOffsetX: 0,
              shadowOffsetY: 4,
              borderColor: '#fff',
              borderWidth: 3
            },
            label: {
              show: true,
              position: 'bottom',
              distance: 8,
              formatter: '{b}',
              fontSize: 12,
              fontWeight: '500',
              color: '#333',
              backgroundColor: 'rgba(255, 255, 255, 0.95)',
              padding: [4, 10],
              borderRadius: 6,
              borderWidth: 1,
              borderColor: '#e8e8e8',
              lineHeight: 20,
              rich: {}
            },
            emphasis: {
              scaleSize: 10,
              itemStyle: {
                shadowBlur: 20,
                shadowColor: catColor + '66'
              },
              label: {
                show: true,
                fontSize: 13,
                fontWeight: 'bold',
                backgroundColor: 'rgba(255, 255, 255, 1)',
                borderColor: catColor,
                color: '#111'
              }
            }
          }
        }),
        links: graphData.value.links.map((link, idx) => {
          const sourceNode = graphData.value.nodes.find(n => n.name === link.source)
          const targetNode = graphData.value.nodes.find(n => n.name === link.target)
          const sourceCatIdx = sourceNode?.category || 0
          const lineColor = categoriesWithColor[sourceCatIdx]?.itemStyle?.color || colorList[sourceCatIdx]
          
          return {
            ...link,
            id: `link-${idx}`,
            source: String(graphData.value.nodes.findIndex(n => n.name === link.source)),
            target: String(graphData.value.nodes.findIndex(n => n.name === link.target)),
            lineStyle: {
              width: link.value ? Math.max(2, link.value / 3) : 2,
              curveness: 0.3,
              opacity: 0.4,
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 1,
                y2: 0,
                colorStops: [
                  { offset: 0, color: lineColor + '88' },
                  { offset: 1, color: lineColor + '44' }
                ]
              },
              shadowBlur: 4,
              shadowColor: lineColor + '44'
            }
          }
        }),
        categories: categoriesWithColor,
        roam: true,
        draggable: true,
        lineStyle: {
          curveness: 0.3,
          width: 2
        },
        emphasis: {
          focus: 'adjacency',
          scale: true,
          scaleSize: 12,
          lineStyle: {
            width: 4,
            opacity: 0.8
          }
        },
        blur: {
          itemStyle: { opacity: 0.15 },
          lineStyle: { opacity: 0.05 }
        }
      }
    ],
    graphic: [
      {
        type: 'circle',
        left: 'center',
        top: 'center',
        shape: { r: 200 },
        style: {
          fill: {
            type: 'radial',
            x: 0.5,
            y: 0.5,
            r: 0.5,
            colorStops: [
              { offset: 0, color: 'rgba(102, 126, 234, 0.03)' },
              { offset: 1, color: 'rgba(102, 126, 234, 0)' }
            ]
          }
        },
        silent: true
      }
    ]
  }

  chart.setOption(option, true)
}

// 初始化技能图谱数据
const initSkillGraphData = async () => {
  try {
    const res = await request.post('/skill-graph/init')
    ElMessage.success(res?.message || '技能图谱数据已初始化')
    return true
  } catch (error) {
    console.error('初始化技能图谱失败:', error)
    const errorMsg = error.response?.data?.error || error.message || '未知错误'
    ElMessage.error('初始化技能图谱失败: ' + errorMsg)
    return false
  }
}

// 加载行业列表
const loadIndustries = async () => {
  try {
    const res = await request.get('/skill-graph/industries')
    industries.value = res || []
  } catch {
    industries.value = []
  }
}

// 行业切换
const onIndustryChange = () => {
  refreshGraph()
}

// 同步所有简历和职位的技能到图谱
const syncSkills = async () => {
  syncing.value = true
  try {
    const res = await request.post('/skill-graph/sync-skills')
    const newAdded = res.newSkillsAdded || 0
    const fromResumes = res.newFromResumes || 0
    const fromJobs = res.newFromJobs || 0
    const totalSkills = res.totalSkills || 0
    
    if (newAdded > 0) {
      ElMessage.success(`同步完成！新增 ${newAdded} 个技能（简历 ${fromResumes} 个，职位 ${fromJobs} 个），当前共 ${totalSkills} 个技能`)
      graphMeta.value = { source: 'sync', resumeCount: fromResumes, jobCount: fromJobs }
    } else {
      ElMessage.info(`同步完成！当前技能库共 ${totalSkills} 个技能`)
      graphMeta.value = { source: 'sync', resumeCount: 0, jobCount: 0 }
    }
    
    // 重新加载行业列表和图谱
    await loadIndustries()
    await refreshGraph()
  } catch (error) {
    ElMessage.error('同步失败: ' + (error.response?.data?.error || error.message))
  } finally {
    syncing.value = false
  }
}

// 加载个人图谱
const loadPersonalGraph = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const params = selectedIndustry.value ? { industry: selectedIndustry.value } : {}
    const res = await request.get('/skill-graph/personal', { params })
    if (res && res.nodes) {
      graphData.value = res
      await nextTick()
      renderGraph()
    } else {
      errorMessage.value = '图谱数据为空'
      ElMessage.warning('图谱数据为空')
    }
  } catch (error) {
    console.error('加载个人能力图谱失败:', error)
    const errorMsg = error.response?.data?.error || error.message || ''

    if (errorMsg.includes('请先创建简历')) {
      errorMessage.value = '请先创建简历后再查看个人能力图谱'
      ElMessage.warning('请先创建简历后再查看个人能力图谱')
    } else if (error.response?.status === 500) {
      if (isAdmin) {
        errorMessage.value = '数据库表不存在，正在自动构建...'
        ElMessage.warning('正在自动构建技能图谱...')
        try {
          const buildRes = await request.post('/skill-graph/auto-build')
          graphMeta.value = { source: buildRes.source, resumeCount: buildRes.resumeCount || 0, jobCount: buildRes.jobCount || 0 }
        } catch {
          await initSkillGraphData()
          graphMeta.value = { source: 'preset', resumeCount: 0, jobCount: 0 }
        }
        try {
          const res = await request.get('/skill-graph/personal')
          if (res && res.nodes) {
            errorMessage.value = ''
            graphData.value = res
            await nextTick()
            renderGraph()
          }
        } catch (retryError) {
          errorMessage.value = '加载失败，请联系管理员初始化技能图谱'
        }
      } else {
        errorMessage.value = '技能图谱尚未初始化，请联系管理员'
      }
    } else {
      errorMessage.value = errorMsg || '加载个人能力图谱失败'
      ElMessage.error('加载个人能力图谱失败: ' + errorMsg)
    }
  } finally {
    loading.value = false
  }
}

// 加载职位图谱
const loadJobGraph = async (jobId) => {
  if (!jobId) return
  loading.value = true
  try {
    const params = selectedIndustry.value ? { industry: selectedIndustry.value } : {}
    const res = await request.get(`/skill-graph/job/${jobId}`, { params })
    graphData.value = res
    await nextTick()
    renderGraph()
  } catch (error) {
    ElMessage.error('加载职位能力图谱失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 加载职位列表
const loadJobList = async () => {
  try {
    const res = await request.get('/job')
    jobList.value = res.records || res || []
  } catch (error) {
    console.error('加载职位列表失败', error)
  }
}

// 切换图谱类型
const switchGraphType = (type) => {
  if (type === 'personal') {
    loadPersonalGraph()
  } else {
    loadJobList()
    if (selectedJobId.value) {
      loadJobGraph(selectedJobId.value)
    }
  }
}

// 刷新图谱
const refreshGraph = () => {
  if (graphType.value === 'personal') {
    loadPersonalGraph()
  } else if (selectedJobId.value) {
    loadJobGraph(selectedJobId.value)
  }
}

// 高亮技能节点
const highlightSkill = (node) => {
  if (!chart) return
  chart.dispatchAction({
    type: 'highlight',
    name: node.name
  })
  setTimeout(() => {
    chart.dispatchAction({
      type: 'downplay',
      name: node.name
    })
  }, 2000)
}

// 选择技能
const selectSkill = (node) => {
  selectedSkill.value = node
  highlightSkill(node)
}

// 获取分类名称
const getCategoryName = (categoryIndex) => {
  return graphData.value.categories[categoryIndex]?.name || '未知'
}

// 获取关联数量
const getRelatedCount = (nodeName) => {
  return graphData.value.links.filter(l => l.source === nodeName || l.target === nodeName).length
}

// 获取分类数量
const getCategoryCount = (categoryIndex) => {
  return graphData.value.nodes.filter(n => n.category === categoryIndex).length
}

// 获取分类百分比
const getCategoryPercentage = (categoryIndex) => {
  const total = graphData.value.nodes.length
  if (!total) return 0
  const count = getCategoryCount(categoryIndex)
  return Math.round((count / total) * 100)
}

// 更新布局
const updateLayout = () => {
  if (!chart || !graphData.value.nodes.length) return
  
  // 直接重新渲染整个图表
  renderGraph()
}

// 重置视图
const resetView = () => {
  if (!chart) return
  chart.dispatchAction({
    type: 'restore'
  })
  selectedSkill.value = null
}

// 展开全部
const expandAll = () => {
  if (!chart) return
  chart.dispatchAction({
    type: 'downplay'
  })
}

// 诊断数据库状态
const diagnose = async () => {
  try {
    const res = await request.get('/skill-graph/diagnose')
    if (!res.skillTableExists || !res.relationTableExists) {
      errorMessage.value = '数据库表不存在，请执行 update.sql 脚本创建表'
      return false
    }
    if (res.skillCount === 0) {
      if (isAdmin) {
        try {
          const buildRes = await request.post('/skill-graph/auto-build')
          graphMeta.value = { source: buildRes.source, resumeCount: buildRes.resumeCount || 0, jobCount: buildRes.jobCount || 0 }
          ElMessage.success(buildRes.message || '技能图谱已自动构建')
        } catch {
          await initSkillGraphData()
          graphMeta.value = { source: 'preset', resumeCount: 0, jobCount: 0 }
        }
      } else {
        errorMessage.value = '技能图谱数据为空，请联系管理员初始化'
      }
    }
    return true
  } catch (error) {
    console.error('诊断失败:', error)
    errorMessage.value = '无法连接到后端服务，请确保服务已启动'
    return false
  }
}

onMounted(async () => {
  initChart()
  const isReady = await diagnose()
  if (isReady) {
    loadIndustries()
    loadPersonalGraph()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.skill-graph-page {
  padding: 24px;
  background: #f8f9fa;
  min-height: 100vh;
}

/* 顶部 */
.graph-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px 24px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}
.header-left h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #111;
}
.header-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: #666;
}
.header-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.2s;
}
.stat-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-content {
  flex: 1;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #111;
  line-height: 1;
  margin-bottom: 4px;
}
.stat-label {
  font-size: 13px;
  color: #666;
}

/* 主内容 */
.graph-body {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;
}

/* 图谱卡片 */
.graph-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}
.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}
.header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-info h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #111;
}
.header-actions-group {
  display: flex;
  gap: 12px;
  align-items: center;
}
.error-alert {
  margin: 16px;
}

/* 图表容器 */
.chart-container {
  width: 100%;
  height: 650px;
  position: relative;
  background: radial-gradient(circle at center, #fafbfc 0%, #ffffff 100%);
}

/* 添加动画效果 */
.chart-container::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.05) 0%, transparent 70%);
  border-radius: 50%;
  animation: pulse 4s ease-in-out infinite;
  pointer-events: none;
  z-index: 0;
}

@keyframes pulse {
  0%, 100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.5;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.2);
    opacity: 0.8;
  }
}

/* 确保图表在动画之上 */
.chart-container :deep(.echarts-container) {
  position: relative;
  z-index: 1;
}

/* 底部操作栏 */
.chart-footer {
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fafafa;
}
.footer-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #999;
}
.footer-tip .el-icon {
  font-size: 14px;
}
.footer-actions {
  display: flex;
  gap: 8px;
}

/* 侧边栏 */
.graph-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.sidebar-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}
.card-title {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  font-weight: 600;
  color: #111;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 技能详情 */
.skill-detail {
  padding: 16px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.detail-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #111;
}
.detail-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}
.detail-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.detail-label {
  font-size: 12px;
  color: #999;
}
.detail-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

/* 技能列表 */
.skill-list {
  max-height: 350px;
  overflow-y: auto;
  padding: 8px;
}
.skill-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}
.skill-item:hover {
  background: #f8f9fa;
}
.skill-item.active {
  background: #e8f4fd;
  border-left: 3px solid #1890ff;
}
.skill-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.skill-name {
  flex: 1;
  font-size: 13px;
  color: #333;
}
.empty-tip {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 13px;
}

/* 分类统计 */
.category-stats {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 300px;
  overflow-y: auto;
}
.category-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.category-header {
  display: flex;
  align-items: center;
  gap: 6px;
}
.category-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.category-name {
  flex: 1;
  font-size: 12px;
  color: #666;
}
.category-count {
  font-size: 12px;
  color: #999;
  font-weight: 600;
}

/* 图例列表 */
.legend-list {
  padding: 12px 16px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #666;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 1000px) {
  .graph-body {
    grid-template-columns: 1fr;
  }
  .graph-sidebar {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 700px) {
  .skill-graph-page {
    padding: 16px;
  }
  .graph-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
  .header-right {
    width: 100%;
    flex-wrap: wrap;
  }
  .stats-row {
    grid-template-columns: 1fr;
  }
  .chart-container {
    height: 500px;
  }
  .graph-sidebar {
    grid-template-columns: 1fr;
  }
}
</style>