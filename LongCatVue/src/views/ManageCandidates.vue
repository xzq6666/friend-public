<template>
  <div class="page-container page-enter">
    <!-- 页面头部 -->
    <div class="page-header-row">
      <div>
        <h2>{{ isAdmin ? '投递记录' : '候选人管理' }}</h2>
        <p class="page-subtitle">{{ isAdmin ? '管理平台投递数据，审核候选人' : '共 ' + total + ' 位候选人' }}</p>
      </div>
    </div>

    <!-- 状态筛选 Tab -->
    <div class="stats-row">
      <button class="stat-tab" :class="{ active: activeStatusTab === 'all' }" @click="setStatusTab('all')">
        <span class="stat-count">{{ statCounts.all }}</span><span class="stat-name">全部</span>
      </button>
      <button class="stat-tab" :class="{ active: activeStatusTab === 0 }" @click="setStatusTab(0)">
        <span class="stat-count">{{ statCounts.pending }}</span><span class="stat-name">待处理</span>
      </button>
      <button class="stat-tab" :class="{ active: activeStatusTab === 1 }" @click="setStatusTab(1)">
        <span class="stat-count">{{ statCounts.viewed }}</span><span class="stat-name">已查看</span>
      </button>
      <button class="stat-tab" :class="{ active: activeStatusTab === 2 }" @click="setStatusTab(2)">
        <span class="stat-count">{{ statCounts.interview }}</span><span class="stat-name">邀请面试</span>
      </button>
      <button class="stat-tab" :class="{ active: activeStatusTab === 3 }" @click="setStatusTab(3)">
        <span class="stat-count">{{ statCounts.hired }}</span><span class="stat-name">已录用</span>
      </button>
      <button class="stat-tab" :class="{ active: activeStatusTab === 4 }" @click="setStatusTab(4)">
        <span class="stat-count">{{ statCounts.rejected }}</span><span class="stat-name">已拒绝</span>
      </button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select
        v-model="filterJobId"
        placeholder="按职位筛选"
        clearable
        style="width: 220px;"
        @change="fetchCandidates"
      >
        <el-option
          v-for="job in jobList"
          :key="job.id"
          :label="job.title + ' - ' + job.location"
          :value="job.id"
        />
      </el-select>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索候选人姓名、技能..."
        :prefix-icon="Search"
        clearable
        style="width: 280px;"
        @clear="fetchCandidates"
        @keyup.enter="fetchCandidates"
      />
      <el-button type="primary" @click="fetchCandidates" :icon="Search">搜索</el-button>
    </div>

    <!-- 对比操作栏 -->
    <div v-if="selectedCandidates.length > 0" class="compare-bar">
      <div class="compare-bar-left">
        <el-icon><Rank /></el-icon>
        <span>已选 <strong>{{ selectedCandidates.length }}</strong> 位候选人</span>
        <span v-if="selectedCandidates.length < 2" class="compare-hint">（至少选择 2 位进行对比）</span>
        <span v-else-if="selectedCandidates.length > 5" class="compare-hint">（最多选择 5 位）</span>
      </div>
      <div class="compare-bar-right">
        <template v-if="!isAdmin">
          <el-button type="success" size="small" @click="batchAdmit"><el-icon><Check /></el-icon> 批量通过</el-button>
          <el-button type="danger" size="small" @click="batchReject"><el-icon><Close /></el-icon> 批量拒绝</el-button>
          <div class="action-divider"></div>
        </template>
        <el-button size="small" @click="clearSelection">清空</el-button>
        <el-button
          type="primary"
          size="small"
          :disabled="selectedCandidates.length < 2 || selectedCandidates.length > 5"
          @click="openCompareDialog"
        >
          <el-icon><Rank /></el-icon> 开始对比
        </el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table
        :data="candidates"
        v-loading="loading"
        stripe
        size="default"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="45" :selectable="isSelectable" />
        <el-table-column label="候选人" width="140">
          <template #default="{ row }">
            <div class="table-candidate-info">
              <strong class="table-candidate-name">{{ row.resume_name || '-' }}</strong>
              <span class="candidate-age">{{ row.resume_age || '-' }}岁</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="job_title" label="投递职位" min-width="140" show-overflow-tooltip />
        <el-table-column label="学历" width="90">
          <template #default="{ row }"><span class="info-cell">{{ row.resume_education || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="期望薪资" width="130">
          <template #default="{ row }"><span class="salary-cell">{{ formatTableSalary(row.expected_salary) }}</span></template>
        </el-table-column>
        <el-table-column label="技能" min-width="200">
          <template #default="{ row }">
            <div class="skill-tags-cell">
              <el-tag v-for="skill in getTableSkills(row.resume_skills).slice(0, 3)" :key="skill" size="small" effect="plain" class="skill-mini-tag">{{ skill }}</el-tag>
              <el-tag v-if="getTableSkills(row.resume_skills).length > 3" size="small" type="info" effect="plain">+{{ getTableSkills(row.resume_skills).length - 3 }}</el-tag>
              <span v-if="getTableSkills(row.resume_skills).length === 0" class="info-cell">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="AI 分析" width="90" align="center">
          <template #default="{ row }">
            <span class="status-dot" :class="row.ai_analysis ? 'dot-success' : 'dot-default'"></span>
            <el-tag :type="row.ai_analysis ? 'success' : 'info'" size="small" effect="light">{{ row.ai_analysis ? '已分析' : '未分析' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-dot" :class="'dot-status-' + row.status"></span>
            <el-tag :type="getStatusType(row.status)" size="small" effect="light">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="投递时间" width="110">
          <template #default="{ row }"><span class="info-cell">{{ formatDate(row.create_time) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" :width="isAdmin ? 100 : 200" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button type="primary" link size="small" @click="viewCandidate(row)">
                <el-icon><View /></el-icon> {{ isAdmin ? '查看详情' : '查看' }}
              </el-button>
              <template v-if="!isAdmin">
                <el-button type="info" link size="small" @click="viewCandidateGraph(row)">
                  <el-icon><Share /></el-icon> 图谱
                </el-button>
                <el-dropdown trigger="click" @command="(cmd) => handleCandidateCommand(cmd, row)">
                  <el-button type="info" link size="small">更多 <el-icon style="margin-left:2px"><ArrowDown /></el-icon></el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-if="row.status < 2" command="interview"><el-icon><Calendar /></el-icon> 邀请面试</el-dropdown-item>
                      <el-dropdown-item v-if="row.status === 0 || row.status === 1" command="admit"><el-icon><Check /></el-icon> 录用</el-dropdown-item>
                      <el-dropdown-item v-if="row.status < 3" command="reject" divided style="color:var(--el-color-danger)"><el-icon><Close /></el-icon> 拒绝</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchCandidates"
          @current-change="fetchCandidates"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" width="720px" destroy-on-close class="detail-dialog">
      <template #header>
        <div class="detail-header">
          <el-avatar :size="52" class="detail-avatar">{{ (currentCandidate?.resume_name || '?').charAt(0) }}</el-avatar>
          <div class="detail-header-info">
            <div class="detail-name">{{ currentCandidate?.resume_name || '-' }}</div>
            <div class="detail-meta">
              <el-tag :type="getStatusType(currentCandidate?.status)" size="small">{{ getStatusText(currentCandidate?.status) }}</el-tag>
              <span>{{ currentCandidate?.job_title || '-' }}</span>
            </div>
          </div>
        </div>
      </template>
      <div v-if="currentCandidate" class="candidate-detail">
        <!-- 基本信息卡片 -->
        <div class="detail-info-grid">
          <div class="detail-info-card">
            <div class="info-card-icon" style="color: #409eff;">&#128100;</div>
            <div class="info-card-content">
              <span class="info-card-label">年龄</span>
              <span class="info-card-value">{{ currentCandidate.resume_age || '-' }}岁</span>
            </div>
          </div>
          <div class="detail-info-card">
            <div class="info-card-icon" style="color: #67c23a;">&#127891;</div>
            <div class="info-card-content">
              <span class="info-card-label">学历</span>
              <span class="info-card-value">{{ currentCandidate.resume_education || '-' }}</span>
            </div>
          </div>
          <div class="detail-info-card">
            <div class="info-card-icon" style="color: #e6a23c;">&#128176;</div>
            <div class="info-card-content">
              <span class="info-card-label">期望薪资</span>
              <span class="info-card-value">{{ (currentCandidate.expected_salary || 0).toLocaleString() }}</span>
            </div>
          </div>
          <div class="detail-info-card">
            <div class="info-card-icon" style="color: #909399;">&#128197;</div>
            <div class="info-card-content">
              <span class="info-card-label">投递时间</span>
              <span class="info-card-value">{{ formatTime(currentCandidate.create_time) }}</span>
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

        <div v-if="currentCandidate.cover_letter" class="detail-section detail-cover">
          <div class="section-title">求职信</div>
          <p>{{ currentCandidate.cover_letter }}</p>
        </div>
        <div v-if="currentCandidate.reject_reason" class="detail-section detail-reject">
          <div class="section-title">拒绝原因</div>
          <p>{{ currentCandidate.reject_reason }}</p>
        </div>
        <div v-if="currentCandidate.ai_analysis" class="detail-section detail-ai">
          <div class="section-title">AI 分析报告</div>
          <div v-if="parsedAnalysis" class="analysis-content">
            <div class="analysis-row">
              <div class="analysis-card">
                <div class="analysis-card-header">经验等级</div>
                <el-tag size="small">{{ parsedAnalysis.basic_info?.experience_level || '-' }}</el-tag>
              </div>
              <div class="analysis-card">
                <div class="analysis-card-header">潜力评分</div>
                <el-progress :percentage="parsedAnalysis.basic_info?.potential_score || 0" :color="potentialColor" :stroke-width="12" style="width: 120px;" />
              </div>
              <div class="analysis-card" v-if="parsedAnalysis.career_suggestions?.salary_range">
                <div class="analysis-card-header">推荐薪资</div>
                <el-tag size="small" type="success">{{ parsedAnalysis.career_suggestions.salary_range }}</el-tag>
              </div>
            </div>
            <div v-if="parsedAnalysis.basic_info?.skills?.length" class="analysis-skills">
              <el-tag v-for="skill in parsedAnalysis.basic_info.skills" :key="skill" size="small" class="ai-skill-tag">{{ skill }}</el-tag>
            </div>
            <div class="analysis-sections">
              <div class="analysis-block">
                <h5 class="block-title block-strength">核心优势</h5>
                <ul class="strength-list">
                  <li v-for="item in parsedAnalysis.strengths" :key="item">{{ item }}</li>
                </ul>
              </div>
              <div class="analysis-block" v-if="parsedAnalysis.weaknesses?.length">
                <h5 class="block-title block-weakness">待提升</h5>
                <ul class="weakness-list">
                  <li v-for="item in parsedAnalysis.weaknesses" :key="item">{{ item }}</li>
                </ul>
              </div>
            </div>
            <div class="analysis-block" v-if="parsedAnalysis.career_suggestions?.recommended_positions?.length">
              <h5 class="block-title block-career">职业建议</h5>
              <div class="career-tags">
                <el-tag v-for="pos in parsedAnalysis.career_suggestions.recommended_positions" :key="pos" size="small" type="warning">{{ pos }}</el-tag>
              </div>
              <p v-if="parsedAnalysis.career_suggestions.development_path" class="dev-path">
                <strong>发展路径：</strong>{{ parsedAnalysis.career_suggestions.development_path }}
              </p>
            </div>
            <div class="analysis-block">
              <h5 class="block-title">综合评价</h5>
              <p class="comment">{{ parsedAnalysis.overall_comment }}</p>
            </div>
          </div>
          <pre v-else class="raw-json">{{ formatJsonDisplay(currentCandidate.ai_analysis) }}</pre>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="graphVisible" title="候选人能力图谱" width="900px" destroy-on-close>
      <div ref="chartRef" class="graph-chart-container" v-loading="graphLoading"></div>
      <template #footer>
        <el-button @click="graphVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="interviewVisible" width="560px" destroy-on-close class="interview-dialog" :show-close="false">
      <template #header>
        <div class="interview-banner">
          <button class="dialog-close" @click="interviewVisible = false">&times;</button>
          <div class="banner-icon">
            <el-icon :size="28"><Calendar /></el-icon>
          </div>
          <div class="banner-title">邀请面试</div>
          <div class="banner-subtitle">向候选人发送面试邀请</div>
        </div>
      </template>
      <div class="interview-content">
        <!-- 候选人信息卡片 -->
        <div class="candidate-card">
          <div class="candidate-avatar">{{ (interviewForm.candidateName || '?').charAt(0) }}</div>
          <div class="candidate-info">
            <div class="candidate-name">{{ interviewForm.candidateName || '-' }}</div>
          </div>
        </div>
        <!-- 表单 -->
        <el-form :model="interviewForm" label-position="top" class="interview-form">
          <el-form-item label="面试职位" required>
            <el-select
              v-model="interviewForm.jobId"
              placeholder="选择面试职位"
              style="width: 100%;"
              filterable
            >
              <el-option
                v-for="job in jobList"
                :key="job.id"
                :label="job.title + (job.location ? ' - ' + job.location : '')"
                :value="job.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="面试时间" required>
            <el-date-picker
              v-model="interviewForm.interviewTime"
              type="datetime"
              placeholder="选择面试时间"
              style="width: 100%;"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm:ss"
              :shortcuts="dateShortcuts"
            />
          </el-form-item>
          <el-form-item label="面试方式">
            <el-radio-group v-model="interviewForm.interviewType" class="type-radio">
              <el-radio-button value="onsite">
                <el-icon><OfficeBuilding /></el-icon> 线下面试
              </el-radio-button>
              <el-radio-button value="online">
                <el-icon><Monitor /></el-icon> 线上面试
              </el-radio-button>
              <el-radio-button value="phone">
                <el-icon><Phone /></el-icon> 电话面试
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="interviewForm.interviewType === 'online' ? '会议链接' : '面试地点'">
            <el-input
              v-model="interviewForm.location"
              :placeholder="interviewForm.interviewType === 'online' ? '请输入会议链接（如腾讯会议、Zoom）' : '请输入面试地点'"
            >
              <template #prefix>
                <el-icon><Location /></el-icon>
              </template>
            </el-input>
            <div v-if="interviewForm.interviewType === 'onsite'" class="quick-locations">
              <el-tag
                v-for="loc in quickLocations"
                :key="loc"
                size="small"
                effect="plain"
                class="location-tag"
                @click="interviewForm.location = loc"
              >{{ loc }}</el-tag>
            </div>
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="interviewForm.remark"
              type="textarea"
              :rows="3"
              placeholder="可选：面试注意事项、需携带材料等"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="interview-footer">
          <el-button @click="interviewVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmInterview" :loading="interviewLoading">
            <el-icon><Check /></el-icon> 发送邀请
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 候选人对比对话框 -->
    <el-dialog
      v-model="compareVisible"
      title="候选人对比"
      width="960px"
      destroy-on-close
      class="compare-dialog"
    >
      <div v-loading="compareLoading" class="compare-container">
        <!-- 表头：候选人卡片 -->
        <div class="compare-header">
          <div class="compare-label-col">
            <div class="compare-label">综合评分</div>
          </div>
          <div
            v-for="item in compareData"
            :key="item.id"
            class="compare-candidate-col"
            :class="{ top: topCandidate?.id === item.id }"
          >
            <div class="candidate-avatar">
              <el-avatar :size="48">
                {{ (item.resume_name || item.name || '?').charAt(0).toUpperCase() }}
              </el-avatar>
              <el-icon v-if="topCandidate?.id === item.id" :size="16" color="#e6a23c" class="top-badge"><Trophy /></el-icon>
            </div>
            <div class="candidate-name">{{ item.resume_name || item.name || '未知' }}</div>
            <div class="candidate-job">{{ item.job_title || '-' }}</div>
            <el-tag :type="getStatusType(item.status)" size="small">{{ getStatusText(item.status) }}</el-tag>
          </div>
        </div>

        <!-- 评分对比行 -->
        <div class="compare-row">
          <div class="compare-row-label">AI 综合评分</div>
          <div
            v-for="item in compareData"
            :key="item.id + '-score'"
            class="compare-row-cell"
          >
            <div v-if="calcOverallScore(item)" class="score-display">
              <el-progress
                :percentage="calcOverallScore(item)"
                :color="getScoreColor(calcOverallScore(item))"
                :stroke-width="14"
                style="width: 100px"
              />
              <span class="score-num" :style="{ color: getScoreColor(calcOverallScore(item)) }">
                {{ calcOverallScore(item) }}分
              </span>
            </div>
            <span v-else class="no-data">暂无评分</span>
          </div>
        </div>

        <!-- 基本信息 -->
        <div class="compare-row">
          <div class="compare-row-label">年龄</div>
          <div v-for="item in compareData" :key="item.id + '-age'" class="compare-row-cell">
            {{ item.resume_age || item.age || '-' }}岁
          </div>
        </div>
        <div class="compare-row">
          <div class="compare-row-label">学历</div>
          <div v-for="item in compareData" :key="item.id + '-edu'" class="compare-row-cell">
            {{ item.resume_education || item.education || '-' }}
          </div>
        </div>
        <div class="compare-row">
          <div class="compare-row-label">期望薪资</div>
          <div v-for="item in compareData" :key="item.id + '-salary'" class="compare-row-cell">
            {{ item.expected_salary ? `¥${item.expected_salary.toLocaleString()}` : '-' }}
          </div>
        </div>
        <div class="compare-row">
          <div class="compare-row-label">经验等级</div>
          <div v-for="item in compareData" :key="item.id + '-exp'" class="compare-row-cell">
            {{ parseCandidateAnalysis(item)?.basic_info?.experience_level || '-' }}
          </div>
        </div>

        <!-- 技能标签 -->
        <div class="compare-row compare-row-block">
          <div class="compare-row-label">技能标签</div>
          <div
            v-for="item in compareData"
            :key="item.id + '-skills'"
            class="compare-row-cell compare-row-block"
          >
            <div class="skill-tags-compare">
              <el-tag
                v-for="skill in getSkillsArray(item)"
                :key="skill"
                size="small"
                class="skill-tag-compare"
              >{{ skill }}</el-tag>
              <span v-if="getSkillsArray(item).length === 0" class="no-data">暂无数据</span>
            </div>
          </div>
        </div>

        <!-- 核心优势 -->
        <div class="compare-row compare-row-block">
          <div class="compare-row-label">核心优势</div>
          <div
            v-for="item in compareData"
            :key="item.id + '-strengths'"
            class="compare-row-cell compare-row-block"
          >
            <ul v-if="parseCandidateAnalysis(item)?.strengths?.length" class="compare-list">
              <li v-for="s in parseCandidateAnalysis(item).strengths" :key="s">{{ s }}</li>
            </ul>
            <span v-else class="no-data">暂无数据</span>
          </div>
        </div>

        <!-- 待提升 -->
        <div class="compare-row compare-row-block">
          <div class="compare-row-label">待提升</div>
          <div
            v-for="item in compareData"
            :key="item.id + '-weaknesses'"
            class="compare-row-cell compare-row-block"
          >
            <ul v-if="parseCandidateAnalysis(item)?.weaknesses?.length" class="compare-list weakness">
              <li v-for="w in parseCandidateAnalysis(item).weaknesses" :key="w">{{ w }}</li>
            </ul>
            <span v-else class="no-data">暂无数据</span>
          </div>
        </div>

        <!-- 推荐职位 -->
        <div class="compare-row compare-row-block">
          <div class="compare-row-label">推荐职位</div>
          <div
            v-for="item in compareData"
            :key="item.id + '-positions'"
            class="compare-row-cell compare-row-block"
          >
            <div v-if="parseCandidateAnalysis(item)?.career_suggestions?.recommended_positions?.length" class="skill-tags-compare">
              <el-tag
                v-for="pos in parseCandidateAnalysis(item).career_suggestions.recommended_positions"
                :key="pos"
                size="small"
                type="warning"
              >{{ pos }}</el-tag>
            </div>
            <span v-else class="no-data">暂无数据</span>
          </div>
        </div>

        <!-- 综合评价 -->
        <div class="compare-row compare-row-block">
          <div class="compare-row-label">综合评价</div>
          <div
            v-for="item in compareData"
            :key="item.id + '-comment'"
            class="compare-row-cell compare-row-block"
          >
            {{ parseCandidateAnalysis(item)?.overall_comment || '-' }}
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="compareVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, View, Calendar, Close, Share, Rank, Trophy, Check, Location, OfficeBuilding, Monitor, Phone, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.user?.userType === 'ADMIN')

// ── 状态筛选 Tab ──────────────────────────────
const activeStatusTab = ref('all')
const statCounts = computed(() => ({
  all: total.value,
  pending: candidates.value.filter(c => c.status === 0).length,
  viewed: candidates.value.filter(c => c.status === 1).length,
  interview: candidates.value.filter(c => c.status === 2).length,
  hired: candidates.value.filter(c => c.status === 3).length,
  rejected: candidates.value.filter(c => c.status === 4).length
}))
const setStatusTab = (tab) => {
  activeStatusTab.value = tab
  filterStatus.value = tab === 'all' ? null : tab
  page.value = 1
  fetchCandidates()
}

// ── 表格辅助函数 ──────────────────────────────
const getTableSkills = (skills) => {
  if (!skills) return []
  try {
    const parsed = typeof skills === 'string' ? JSON.parse(skills) : skills
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return skills.split(/[,，、;；\s]+/).filter(s => s.trim())
  }
}
const formatTableSalary = (salary) => {
  if (!salary) return '面议'
  return salary.toLocaleString() + ' 元/月'
}
const formatDate = (t) => {
  if (!t) return '-'
  return new Date(t).toLocaleDateString('zh-CN')
}
const handleCandidateCommand = (command, row) => {
  if (command === 'interview') inviteInterview(row)
  else if (command === 'admit') handleAdmit(row)
  else if (command === 'reject') handleReject(row)
}

const candidates = ref([])
const jobList = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const filterJobId = ref(null)
const filterStatus = ref(null)

const detailVisible = ref(false)
const currentCandidate = ref(null)
const interviewVisible = ref(false)
const selectedCandidates = ref([])
const compareVisible = ref(false)
const compareLoading = ref(false)
const compareData = ref([])
const graphVisible = ref(false)
const graphData = ref({ nodes: [], links: [], categories: [] })
const graphLoading = ref(false)
let chart = null
const interviewLoading = ref(false)
const interviewForm = ref({
  applicationId: null,
  jobId: null,
  userId: null,
  candidateName: '',
  jobTitle: '',
  interviewTime: '',
  interviewType: 'onsite',
  location: '',
  remark: ''
})

const dateShortcuts = [
  { text: '明天上午10点', value: () => { const d = new Date(); d.setDate(d.getDate() + 1); d.setHours(10, 0, 0, 0); return d } },
  { text: '明天下午2点', value: () => { const d = new Date(); d.setDate(d.getDate() + 1); d.setHours(14, 0, 0, 0); return d } },
  { text: '后天上午10点', value: () => { const d = new Date(); d.setDate(d.getDate() + 2); d.setHours(10, 0, 0, 0); return d } },
  { text: '下周一上午10点', value: () => { const d = new Date(); const day = d.getDay(); const diff = day === 0 ? 1 : 8 - day; d.setDate(d.getDate() + diff); d.setHours(10, 0, 0, 0); return d } }
]

const quickLocations = ['公司会议室A', '公司会议室B', '线上面试', '电话面试']

const detailSkills = computed(() => {
  if (!currentCandidate.value?.resume_skills) return []
  try {
    return typeof currentCandidate.value.resume_skills === 'string'
      ? JSON.parse(currentCandidate.value.resume_skills)
      : currentCandidate.value.resume_skills
  } catch {
    return currentCandidate.value.resume_skills.split(/[,，、;；\s]+/).filter(s => s.trim())
  }
})

// 修复字符串值内的真实换行符
const fixNewlinesInStrings = (str) => {
  const result = []
  let inString = false
  let escaped = false
  for (let i = 0; i < str.length; i++) {
    const ch = str[i]
    if (escaped) { result.push(ch); escaped = false; continue }
    if (ch === '\\') { result.push(ch); escaped = true; continue }
    if (ch === '"') { inString = !inString; result.push(ch); continue }
    if (inString && (ch === '\n' || ch === '\r')) { result.push('\\n'); continue }
    result.push(ch)
  }
  return result.join('')
}
// 修复单引号包裹的键或值，同时保留值中的撇号
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
    if (ch === '"') {
      result.push(ch); i++
      while (i < str.length && str[i] !== '"') {
        if (str[i] === '\\') { result.push(str[i]); i++ }
        result.push(str[i]); i++
      }
      if (i < str.length) { result.push(str[i]); i++ }
      continue
    }
    if (ch === ',' || ch === '{') {
      result.push(ch); i++
      let ws = ''
      while (i < str.length && /\s/.test(str[i])) { ws += str[i]; i++ }
      if (i < str.length && /[a-zA-Z_]/.test(str[i])) {
        let word = '', j = i
        while (j < str.length && /\w/.test(str[j])) { word += str[j]; j++ }
        while (j < str.length && /\s/.test(str[j])) { j++ }
        if (j < str.length && str[j] === ':') {
          result.push(ws); result.push('"'); result.push(word); result.push('"')
          i += word.length; continue
        }
      }
      result.push(ws); continue
    }
    result.push(ch); i++
  }
  return result.join('')
}

const parsedAnalysis = computed(() => {
  if (!currentCandidate.value?.ai_analysis) return null
  try {
    let jsonStr = currentCandidate.value.ai_analysis.trim()

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

    // 容错：去掉JSON中的行注释
    jsonStr = jsonStr.replace(/^\s*\/\/.*$/gm, '')
    // 容错：去掉尾部逗号
    jsonStr = jsonStr.replace(/,\s*([}\]])/g, '$1')
    // 容错：去掉字符串值内的换行符
    jsonStr = fixNewlinesInStrings(jsonStr)
    // 容错：修复单引号
    jsonStr = fixSingleQuotes(jsonStr)
    // 容错：给没有引号的属性名加引号（只在字符串外部）
    jsonStr = fixUnquotedKeys(jsonStr)

    const parsed = JSON.parse(jsonStr)
    // 兼容后端两种数据结构：dimensions 格式 vs basic_info 格式
    const totalScore = parsed.total_score || 0
    const scoreLevel = parsed.score_level || '-'
    const potentialDim = parsed.dimensions?.potential?.score
    const experienceDim = parsed.dimensions?.experience?.score
    // 经验等级：根据 experience 维度得分推算（满分30）
    let experienceLevel = '-'
    if (experienceDim !== undefined) {
      const ratio = experienceDim / (parsed.dimensions.experience.max || 30)
      experienceLevel = ratio >= 0.8 ? '高级' : ratio >= 0.5 ? '中级' : ratio >= 0.2 ? '初级' : '实习'
    }
    // 潜力评分：优先用 total_score（0-100），否则用 potential 维度换算
    const potentialScore = totalScore || (potentialDim !== undefined ? Math.round(potentialDim / (parsed.dimensions.potential.max || 10) * 100) : 0)

    return {
      basic_info: parsed.basic_info || { skills: [], experience_level: experienceLevel, education_level: '-', potential_score: potentialScore, potential_level: scoreLevel },
      strengths: parsed.strengths || [],
      weaknesses: parsed.weaknesses || [],
      career_suggestions: parsed.career_suggestions || { recommended_positions: [], salary_range: '-', development_path: '-' },
      learning_plan: parsed.learning_plan || { short_term: [], long_term: [] },
      overall_comment: parsed.overall_comment || '-'
    }
  } catch {
    // 最后手段：用正则提取关键字段
    try {
      const raw = currentCandidate.value?.ai_analysis
      const scoreMatch = raw.match(/"?total_score"?\s*[:：]\s*(\d+)/)
      const levelMatch = raw.match(/"?score_level"?\s*[:：]\s*"([^"]+)"/)
      const commentMatch = raw.match(/"?overall_comment"?\s*[:：]\s*"([^"]*?)"/)
      if (scoreMatch) {
        const totalScore = parseInt(scoreMatch[1]) || 0
        return {
          basic_info: { skills: [], experience_level: '-', education_level: '-', potential_score: totalScore, potential_level: levelMatch?.[1] || '-' },
          strengths: [], weaknesses: [],
          career_suggestions: { recommended_positions: [], salary_range: '-', development_path: '-' },
          learning_plan: { short_term: [], long_term: [] },
          overall_comment: commentMatch?.[1] || '-'
        }
      }
    } catch { /* ignore */ }
    return null
  }
})

const potentialColor = computed(() => {
  const score = parsedAnalysis.value?.basic_info?.potential_score || 0
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
})

const getStatusText = (status) => {
  const map = { 0: '待处理', 1: '已查看', 2: '邀请面试', 3: '已录用', 4: '已拒绝' }
  return map[status] || '未知'
}

const getStatusType = (status) => {
  const map = { 0: 'info', 1: 'primary', 2: 'warning', 3: 'success', 4: 'danger' }
  return map[status] || 'info'
}

const loadJobList = async () => {
  try {
    const res = await request.get('/job', { params: { page: 1, size: 100 } })
    jobList.value = res.records || res || []
  } catch (error) {
    console.error('加载职位列表失败', error)
  }
}

const fetchCandidates = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filterJobId.value) params.jobId = filterJobId.value
    if (filterStatus.value !== null && filterStatus.value !== '') params.status = filterStatus.value
    if (searchKeyword.value) params.keyword = searchKeyword.value
    const res = await request.get('/application/page', { params })
    candidates.value = res.records || []
    total.value = res.total || 0
  } catch {
    candidates.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const viewCandidate = async (row) => {
  if (!isAdmin.value && row.status === 0) {
    try {
      await request.put(`/application/${row.id}/status`, { status: 1 })
    } catch (e) {
      console.error('更新查看状态失败:', e)
    }
  }
  try {
    const res = await request.get(`/resume/${row.resume_id}`)
    currentCandidate.value = { ...row, ...res }
  } catch {
    currentCandidate.value = row
  }
  detailVisible.value = true
}

const inviteInterview = (row) => {
  interviewForm.value = {
    applicationId: row.id,
    jobId: row.job_id,
    userId: row.user_id,
    candidateName: row.resume_name || '-',
    jobTitle: row.job_title || '-',
    interviewTime: '',
    interviewType: 'onsite',
    location: '',
    remark: ''
  }
  interviewVisible.value = true
}

const confirmInterview = async () => {
  if (!interviewForm.value.jobId) {
    ElMessage.warning('请选择面试职位')
    return
  }
  if (!interviewForm.value.interviewTime) {
    ElMessage.warning('请选择面试时间')
    return
  }
  interviewLoading.value = true
  try {
    await request.post('/interview', {
      applicationId: interviewForm.value.applicationId,
      jobId: interviewForm.value.jobId,
      userId: interviewForm.value.userId,
      interviewTime: interviewForm.value.interviewTime,
      interviewLocation: interviewForm.value.location,
      notes: interviewForm.value.remark,
      interviewType: interviewForm.value.interviewType
    })
    await request.put(`/application/${interviewForm.value.applicationId}/status`, { status: 2 })
    ElMessage.success('面试邀请已发送')
    interviewVisible.value = false
    fetchCandidates()
  } catch (error) {
    ElMessage.error('邀请失败: ' + (error.response?.data?.error || error.message))
    console.error('面试邀请失败:', error)
  } finally {
    interviewLoading.value = false
  }
}

const handleAdmit = async (row) => {
  try {
    await ElMessageBox.confirm(`确定录用 ${row.resume_name} 吗？`, '确认录用', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })
    await request.put(`/application/${row.id}/status`, { status: 3 })
    ElMessage.success('录用成功')
    fetchCandidates()
  } catch {
    // 用户取消
  }
}

const handleReject = async (row) => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因（可选）', '拒绝候选人', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：技能不匹配、经验不足等'
    })
    await request.put(`/application/${row.id}/status`, { status: 4, rejectReason: reason || '未说明' })
    ElMessage.success('已拒绝')
    fetchCandidates()
  } catch {
    // 用户取消
  }
}

const viewCandidateGraph = async (row) => {
  graphVisible.value = true
  graphLoading.value = true
  try {
    const res = await request.get(`/skill-graph/personal/${row.resume_id}`)
    graphData.value = res
    await nextTick()
    renderCandidateGraph()
  } catch (error) {
    ElMessage.error('加载能力图谱失败')
    console.error(error)
  } finally {
    graphLoading.value = false
  }
}

const renderCandidateGraph = () => {
  const chartEl = document.querySelector('.graph-chart-container')
  if (!chartEl) return
  if (chart) chart.dispose()
  chart = echarts.init(chartEl)
  const colorList = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452']
  const categoriesWithColor = graphData.value.categories.map((cat, idx) => ({
    ...cat,
    itemStyle: { color: colorList[idx % colorList.length] }
  }))
  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e0e0e0',
      borderWidth: 1,
      textStyle: { color: '#333' },
      formatter: (params) => {
        if (params.dataType === 'node') {
          const catName = categoriesWithColor[params.data.category]?.name || '未知'
          return `<div style="padding: 6px 10px;">
            <div style="font-size: 16px; font-weight: bold; margin-bottom: 6px;">${params.name}</div>
            <div style="font-size: 13px; color: #666;">分类：${catName}</div>
          </div>`
        }
        return ''
      }
    },
    animationDuration: 1500,
    animationEasingUpdate: 'quinticInOut',
    series: [{
      type: 'graph',
      layout: 'force',
      data: graphData.value.nodes.map((node, index) => {
        const catIdx = node.category || 0
        const isCenter = node.name === '候选人技能'
        return {
          ...node,
          id: String(index),
          category: catIdx,
          symbolSize: isCenter ? 70 : (node.symbolSize || 30),
          itemStyle: {
            color: isCenter ? '#409eff' : colorList[catIdx % colorList.length],
            shadowBlur: isCenter ? 25 : 15,
            shadowColor: 'rgba(0, 0, 0, 0.25)',
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: true,
            position: isCenter ? 'inside' : 'right',
            fontSize: isCenter ? 15 : 12,
            fontWeight: isCenter ? 'bold' : 'normal',
            color: isCenter ? '#fff' : '#444'
          }
        }
      }),
      links: graphData.value.links.map((link, idx) => ({
        ...link,
        source: String(graphData.value.nodes.findIndex(n => n.name === link.source)),
        target: String(graphData.value.nodes.findIndex(n => n.name === link.target)),
        lineStyle: {
          width: link.value ? (link.value / 3) : 1.5,
          curveness: 0.2,
          opacity: 0.6
        }
      })),
      categories: categoriesWithColor,
      roam: true,
      draggable: true,
      emphasis: { focus: 'adjacency' },
      force: { repulsion: 500, gravity: 0.05, edgeLength: [150, 250], layoutAnimation: true }
    }]
  }
  chart.setOption(option, true)
}

const formatTime = (t) => {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

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

// 多选相关
const isSelectable = (row) => {
  return selectedCandidates.value.length < 5 || selectedCandidates.value.some(c => c.id === row.id)
}

const handleSelectionChange = (selection) => {
  selectedCandidates.value = selection
}

const clearSelection = () => {
  selectedCandidates.value = []
}

// 解析候选人的 AI 分析数据
const parseCandidateAnalysis = (row) => {
  if (!row.ai_analysis) return null
  try {
    let jsonStr = row.ai_analysis
    if (jsonStr.startsWith('```json')) jsonStr = jsonStr.substring(7)
    if (jsonStr.startsWith('```')) jsonStr = jsonStr.substring(3)
    if (jsonStr.endsWith('```')) jsonStr = jsonStr.substring(0, jsonStr.length - 3)
    jsonStr = jsonStr.trim()
    // 修复AI返回的JSON中常见的尾随逗号问题
    jsonStr = jsonStr.replace(/,\s*([}\]])/g, '$1')
    const parsed = JSON.parse(jsonStr)
    // 兼容后端 dimensions 格式
    const totalScore = parsed.total_score || 0
    const scoreLevel = parsed.score_level || '-'
    const potentialDim = parsed.dimensions?.potential?.score
    const experienceDim = parsed.dimensions?.experience?.score
    let experienceLevel = '-'
    if (experienceDim !== undefined) {
      const ratio = experienceDim / (parsed.dimensions.experience.max || 30)
      experienceLevel = ratio >= 0.8 ? '高级' : ratio >= 0.5 ? '中级' : ratio >= 0.2 ? '初级' : '实习'
    }
    const potentialScore = totalScore || (potentialDim !== undefined ? Math.round(potentialDim / (parsed.dimensions.potential.max || 10) * 100) : 0)
    const basicInfo = parsed.basic_info || { skills: [], experience_level: experienceLevel, potential_score: potentialScore, potential_level: scoreLevel }

    return {
      basic_info: basicInfo,
      strengths: parsed.strengths || [],
      weaknesses: parsed.weaknesses || [],
      career_suggestions: parsed.career_suggestions || {},
      overall_comment: parsed.overall_comment || '-',
    }
  } catch {
    return null
  }
}

// 计算候选人综合评分（基于 AI 分析）
const calcOverallScore = (row) => {
  const analysis = parseCandidateAnalysis(row)
  if (!analysis) return null
  const potential = analysis.basic_info?.potential_score || 0
  // 综合评分 = 潜力评分（可后续扩展为多维度加权）
  return potential
}

// 获取技能数组
const getSkillsArray = (row) => {
  if (!row.resume_skills) return []
  try {
    return JSON.parse(row.resume_skills)
  } catch {
    return row.resume_skills.split(/[,，、;；\s]+/).filter(s => s.trim())
  }
}

// 打开对比对话框
const openCompareDialog = async () => {
  if (selectedCandidates.value.length < 2) {
    ElMessage.warning('请至少选择 2 位候选人进行对比')
    return
  }
  if (selectedCandidates.value.length > 5) {
    ElMessage.warning('最多选择 5 位候选人进行对比')
    return
  }

  compareVisible.value = true
  compareLoading.value = true
  compareData.value = []

  try {
    // 并行加载每个候选人的详细数据
    const promises = selectedCandidates.value.map(async (candidate) => {
      try {
        const res = await request.get(`/resume/${candidate.resume_id}`)
        return { ...candidate, ...res }
      } catch {
        return candidate
      }
    })
    compareData.value = await Promise.all(promises)
  } catch (error) {
    console.error('加载对比数据失败', error)
    compareData.value = selectedCandidates.value
  } finally {
    compareLoading.value = false
  }
}

// 获取评分颜色
const getScoreColor = (score) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

// 获取最高分候选人
const topCandidate = computed(() => {
  if (compareData.value.length === 0) return null
  const scored = compareData.value.map(c => ({
    candidate: c,
    score: calcOverallScore(c) || 0,
  }))
  scored.sort((a, b) => b.score - a.score)
  return scored[0]?.candidate || null
})

const batchAdmit = async () => {
  try {
    await ElMessageBox.confirm(`确定批量录用选中的 ${selectedCandidates.value.length} 位候选人吗？`, '批量录用', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'success'
    })
    const promises = selectedCandidates.value.map(c => request.put(`/application/${c.id}/status`, { status: 3 }))
    await Promise.all(promises)
    ElMessage.success(`已录用 ${selectedCandidates.value.length} 位候选人`)
    clearSelection()
    fetchCandidates()
  } catch { /* cancel */ }
}

const batchReject = async () => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因（可选）', '批量拒绝', {
      confirmButtonText: '确定', cancelButtonText: '取消',
      inputPlaceholder: '例如：技能不匹配、经验不足等'
    })
    const promises = selectedCandidates.value.map(c =>
      request.put(`/application/${c.id}/status`, { status: 4, rejectReason: reason || '未说明' })
    )
    await Promise.all(promises)
    ElMessage.success(`已拒绝 ${selectedCandidates.value.length} 位候选人`)
    clearSelection()
    fetchCandidates()
  } catch { /* cancel */ }
}

onMounted(() => {
  loadJobList()
  fetchCandidates()
})
</script>

<style scoped>
.table-card { padding: var(--space-3); margin-bottom: 0; }

/* ── 表格候选人信息 ─────────────────────────── */
.table-candidate-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.table-candidate-name {
  font-weight: 600;
  color: var(--gray-800);
  font-size: var(--text-sm);
  transition: color var(--duration-fast);
}
.table-candidate-name:hover {
  color: var(--primary-500);
}
.candidate-age {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

/* ── 表格单元格样式 ─────────────────────────── */
.skill-tags-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}
.skill-mini-tag {
  font-size: 11px;
}
.salary-cell {
  font-weight: 600;
  color: var(--gray-700);
  font-variant-numeric: tabular-nums;
}
.info-cell {
  color: var(--gray-600);
  font-size: var(--text-sm);
}

/* ── 状态圆点 ────────────────────────────────── */
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
.dot-status-0 { background: var(--gray-400); }
.dot-status-1 { background: var(--primary-500, #409EFF); }
.dot-status-2 { background: var(--warning-500, #E6A23C); }
.dot-status-3 { background: var(--success-500, #67c23a); }
.dot-status-4 { background: var(--danger-500, #F56C6C); }

/* ── 操作按钮布局 ────────────────────────────── */
.action-cell {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

/* ── 候选人详情弹窗 ─────────────────────────── */
.detail-header {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.detail-avatar {
  background: linear-gradient(135deg, #409eff, #67c23a);
  color: #fff;
  font-size: 20px;
  font-weight: 600;
  flex-shrink: 0;
}

.detail-header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--gray-900);
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.candidate-detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

/* 信息卡片网格 */
.detail-info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
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

.info-card-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.info-card-content {
  display: flex;
  flex-direction: column;
}

.info-card-label {
  font-size: 11px;
  color: var(--color-text-muted);
}

.info-card-value {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--gray-800);
}

/* 技能标签区 */
.detail-skills-section {
  padding: var(--space-3) var(--space-4);
  background: var(--gray-50);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--gray-700);
  margin-bottom: var(--space-2);
}

.detail-skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.detail-skill-tag {
  font-size: 12px;
}

/* 各内容区 */
.detail-section {
  border-radius: var(--radius-md);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
}

.detail-cover {
  background: #f0f9ff;
  border-color: #bae6fd;
}

.detail-reject {
  background: #fef2f2;
  border-color: #fecaca;
}

.detail-ai {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.detail-section p {
  font-size: var(--text-sm);
  color: var(--gray-700);
  line-height: 1.6;
  margin: 0;
}

/* ── AI 分析内容 ────────────────────────────── */
.analysis-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.analysis-row {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.analysis-card {
  flex: 1;
  min-width: 120px;
  padding: var(--space-3);
  background: var(--color-surface);
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  text-align: center;
}

.analysis-card-header {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-bottom: var(--space-2);
}

.analysis-skills {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.ai-skill-tag {
  font-size: 11px;
}

.analysis-sections {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3);
}

.analysis-block {
  /* default */
}

.block-title {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 var(--space-2);
  padding-bottom: var(--space-1);
  border-bottom: 1px solid var(--color-border);
}

.block-strength { color: #67c23a; border-color: #67c23a; }
.block-weakness { color: #f56c6c; border-color: #f56c6c; }
.block-career { color: #e6a23c; border-color: #e6a23c; }

.career-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.strength-list,
.weakness-list {
  margin: 0;
  padding-left: var(--space-5);
  font-size: var(--text-sm);
  line-height: 1.6;
}

.strength-list li { color: #555; }
.weakness-list li { color: #555; }

.comment,
.dev-path,
.raw-json {
  font-size: var(--text-sm);
  color: var(--gray-700);
  line-height: 1.6;
  margin: 0;
  background: var(--color-surface);
  padding: var(--space-3);
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
}

.dev-path { margin-top: var(--space-2); }

.raw-json {
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 400px;
  overflow-y: auto;
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.graph-chart-container {
  width: 100%;
  height: 600px;
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-md);
}

/* ── 对比操作栏 ─────────────────────────────── */
.compare-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3) var(--space-4);
  margin-bottom: var(--space-4);
  background: var(--primary-50);
  border: 1px solid var(--primary-200);
  border-radius: var(--radius-md);
}

.compare-bar-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--gray-700);
}

.compare-bar-left .el-icon {
  color: var(--primary-500);
  font-size: 16px;
}

.compare-bar-left strong {
  color: var(--primary-600);
  font-size: var(--text-base);
}

.compare-hint {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.compare-bar-right {
  display: flex;
  gap: var(--space-2);
}

/* ── 对比对话框 ─────────────────────────────── */
.compare-container {
  display: flex;
  flex-direction: column;
}

.compare-header {
  display: grid;
  grid-template-columns: 120px repeat(auto-fit, minmax(140px, 1fr));
  gap: var(--space-3);
  padding: var(--space-4);
  background: var(--gray-50);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-4);
}

.compare-label-col {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.compare-candidate-col {
  text-align: center;
  padding: var(--space-3);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  position: relative;
  transition: all var(--duration-fast) var(--ease-out);
}

.compare-candidate-col.top {
  border-color: var(--warning-500);
  background: var(--warning-50);
}

.candidate-avatar {
  position: relative;
  display: inline-block;
}

.top-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: var(--color-surface);
  border-radius: var(--radius-full);
  padding: 2px;
  box-shadow: var(--shadow-sm);
}

.candidate-name {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  margin-top: var(--space-2);
}

.candidate-job {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  margin: var(--space-1) 0;
}

/* ── 对比行 ─────────────────────────────────── */
.compare-row {
  display: grid;
  grid-template-columns: 120px repeat(auto-fit, minmax(140px, 1fr));
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--gray-100);
  align-items: center;
}

.compare-row:last-child { border-bottom: none; }
.compare-row-block { align-items: flex-start; }

.compare-row-label {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--gray-600);
}

.compare-row-cell {
  font-size: var(--text-sm);
  color: var(--gray-800);
  text-align: center;
}

.compare-row-cell.compare-row-block { text-align: left; }

/* ── 评分显示 ───────────────────────────────── */
.score-display {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  justify-content: center;
}

.score-num {
  font-weight: var(--weight-semibold);
  font-size: var(--text-sm);
  min-width: 50px;
  text-align: left;
}

/* ── 技能标签 ───────────────────────────────── */
.skill-tags-compare {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  justify-content: center;
}

.skill-tag-compare { font-size: 11px; }

/* ── 列表 ───────────────────────────────────── */
.compare-list {
  margin: 0;
  padding-left: var(--space-5);
  font-size: var(--text-xs);
  line-height: var(--leading-relaxed);
  color: var(--gray-700);
}

.compare-list li { color: var(--success-600); }
.compare-list.weakness li { color: var(--danger-600); }

.no-data {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  font-style: italic;
}

/* ── 邀请面试弹窗 ─────────────────────────── */
.interview-banner {
  margin: -20px -20px 0;
  padding: var(--space-5) var(--space-5);
  background: linear-gradient(135deg, #6366f1, #818cf8);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  position: relative;
  border-radius: 8px 8px 0 0;
}

.dialog-close {
  position: absolute;
  top: 12px;
  right: 16px;
  background: rgba(255,255,255,0.25);
  border: none;
  color: #fff;
  font-size: 20px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}
.dialog-close:hover { background: rgba(255,255,255,0.4); }

.banner-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.banner-title {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

.banner-subtitle {
  font-size: 13px;
  color: rgba(255,255,255,0.8);
}

.interview-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding-top: var(--space-3);
}

.candidate-card {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: var(--gray-50);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.candidate-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #818cf8);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}

.candidate-info {
  flex: 1;
  min-width: 0;
}

.candidate-name {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.candidate-job {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  margin-top: 2px;
}

.interview-form {
  margin-top: var(--space-2);
}

.type-radio {
  display: flex;
  gap: var(--space-2);
}

.type-radio .el-radio-button {
  flex: 1;
}

.quick-locations {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.location-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.location-tag:hover {
  background: var(--primary-50);
  border-color: var(--primary-400);
  color: var(--primary-600);
}

.interview-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}
</style>
