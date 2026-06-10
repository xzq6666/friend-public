<template>
  <div class="my-resume-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-icon">
          <el-icon :size="28" style="color: var(--primary-500)"><Briefcase /></el-icon>
        </div>
        <div class="header-text">
          <h2>{{ isViewMode ? '简历详情' : '我的简历' }}</h2>
          <p class="header-desc">{{ isViewMode ? '查看候选人简历信息' : '管理简历、上传文件、智能导入，一站式完成' }}</p>
        </div>
      </div>
      <div class="header-actions">
        <el-button v-if="isViewMode" @click="$router.back()" class="action-btn">
          <el-icon><Back /></el-icon> 返回
        </el-button>
        <template v-else>
          <el-button @click="createNewResume" class="action-btn secondary">
            <el-icon><Plus /></el-icon> 新建简历
          </el-button>
          <el-button type="primary" plain @click="showTemplateDialog" class="action-btn">
            <el-icon><Document /></el-icon> 使用模板
          </el-button>
          <el-button type="primary" @click="handleSave" :loading="saving" class="action-btn primary">
            <el-icon><EditPen /></el-icon> {{ resumeId ? '更新简历' : '保存简历' }}
          </el-button>
          <el-button v-if="resumeId" @click="aiAnalyze" :loading="aiLoading" class="action-btn ai-btn">
            <el-icon><MagicStick /></el-icon> AI 分析
          </el-button>
        </template>
      </div>
    </div>

    <!-- 简历列表（仅非查看模式显示） -->
    <div v-if="!isViewMode" class="resume-switcher">
      <div class="switcher-header">
        <el-icon :size="16" style="color: var(--gray-500)"><Files /></el-icon>
        <span class="switcher-title">我的简历</span>
        <span class="switcher-count">{{ resumeList.length }}</span>
      </div>
      <div class="switcher-list">
        <div
          v-for="resume in resumeList"
          :key="resume.id"
          class="resume-card"
          :class="{ active: resumeId === resume.id, default: resume.isDefault === 1 }"
          @click="switchResume(resume)"
        >
          <div class="card-main">
            <div class="card-title-row">
              <span class="card-name">{{ resume.resumeName || (resume.name || '未命名') + '的简历' }}</span>
              <el-tag v-if="resume.isDefault === 1" type="success" size="mini" effect="plain">默认</el-tag>
            </div>
            <div class="card-meta">{{ resume.name || '-' }} · {{ resume.education || '-' }}</div>
          </div>
          <div class="card-actions">
            <el-button v-if="resume.isDefault !== 1" type="text" size="small" @click.stop="handleSetDefault(resume)" class="card-action-btn">设为默认</el-button>
            <el-button type="text" size="small" @click.stop="handleCopyResume(resume)" class="card-action-btn">复制</el-button>
            <el-button v-if="resumeList.length > 1" type="text" size="small" @click.stop="handleDeleteResume(resume)" class="card-action-btn delete">删除</el-button>
          </div>
        </div>
      </div>
      <el-button class="add-resume-btn" @click="createNewResume" :icon="Plus" circle size="small" />
    </div>

    <!-- 简历模板选择对话框 -->
    <el-dialog v-model="templateDialogVisible" title="选择简历模板" width="700px">
      <el-alert
        title="选择模板后，表单将自动填充标准化的内容结构，您只需修改个人信息即可"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />
      <div class="template-list">
        <div
          v-for="template in resumeTemplates"
          :key="template.id"
          class="template-item"
          :class="{ active: selectedTemplate === template.id }"
          @click="selectedTemplate = template.id"
        >
          <div class="template-icon">
            <el-icon :size="32" :color="template.color"><component :is="template.icon" /></el-icon>
          </div>
          <div class="template-info">
            <h4>{{ template.name }}</h4>
            <p>{{ template.description }}</p>
          </div>
          <el-icon v-if="selectedTemplate === template.id" class="check-icon" color="#67c23a"><CircleCheck /></el-icon>
        </div>
      </div>

      <!-- 模板预览 -->
      <div v-if="selectedTemplate" class="template-preview">
        <h4>模板预览</h4>
        <div class="preview-content">
          <div class="preview-item">
            <span class="preview-label">技能：</span>
            <span class="preview-value">{{ getTemplatePreview('skills') }}</span>
          </div>
          <div class="preview-item">
            <span class="preview-label">经验描述：</span>
            <span class="preview-value">{{ getTemplatePreview('experience') }}</span>
          </div>
          <div class="preview-item">
            <span class="preview-label">工作经历：</span>
            <span class="preview-value">{{ getTemplatePreview('workExperience') }}</span>
          </div>
          <div class="preview-item">
            <span class="preview-label">自我评价：</span>
            <span class="preview-value">{{ getTemplatePreview('selfIntroduction') }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="applyTemplate" :disabled="!selectedTemplate">
          使用此模板
        </el-button>
      </template>
    </el-dialog>

    <!-- 简历表单 -->
    <el-row :gutter="32">
      <!-- 左侧：简历表单 (占 17/24) -->
      <el-col :span="17">
        <el-card shadow="hover" class="form-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">基本信息</span>
            </div>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" label-position="top" :disabled="isViewMode">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="姓名" prop="name">
                  <el-input v-model="form.name" placeholder="请输入姓名" maxlength="20" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="年龄" prop="age">
                  <el-input-number v-model="form.age" :min="16" :max="70" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="联系电话" prop="phone">
                  <el-input v-model="form.phone" placeholder="请输入手机号码" maxlength="11" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="联系邮箱" prop="email">
                  <el-input v-model="form.email" placeholder="请输入邮箱地址" type="email" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="学历" prop="education">
                  <el-select v-model="form.education" placeholder="请选择学历" style="width: 100%">
                    <el-option label="高中" value="高中" />
                    <el-option label="大专" value="大专" />
                    <el-option label="本科" value="本科" />
                    <el-option label="硕士" value="硕士" />
                    <el-option label="博士" value="博士" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="行业分类" prop="categoryId">
                  <el-select v-model="form.categoryId" placeholder="请选择求职意向分类" style="width: 100%">
                    <el-option
                      v-for="cat in categories"
                      :key="cat.id"
                      :label="cat.name"
                      :value="cat.id"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="期望薪资" prop="expectedSalary">
                  <el-input-number
                    v-model="form.expectedSalary"
                    :min="0"
                    :step="1000"
                    :max="1000000"
                    style="width: 100%"
                  >
                    <template #suffix>
                      <span style="color: #78726a; font-size: 13px;">元/月</span>
                    </template>
                  </el-input-number>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="技能" prop="skills">
              <el-input
                v-model="form.skills"
                type="textarea"
                :rows="2"
                placeholder="请输入技能，用逗号分隔，如：Java, Spring Boot, Vue.js, MySQL"
              />
              <div class="form-tip">多个技能用逗号分隔，系统会自动识别并生成JSON格式</div>
            </el-form-item>

            <el-form-item label="工作年限" prop="experience">
              <el-input
                v-model="form.experience"
                type="textarea"
                :rows="2"
                placeholder="请描述您的工作经验，如：5年后端开发经验，熟悉微服务架构"
              />
            </el-form-item>

            <el-form-item label="教育背景" prop="educationDetail">
              <el-input
                v-model="form.education"
                placeholder="如：XX大学 计算机科学与技术 本科"
              />
            </el-form-item>

            <el-divider content-position="left" class="section-divider">
              <span class="divider-title">工作经历（详细）</span>
            </el-divider>
            <div class="section-tip">
              <el-icon><InfoFilled /></el-icon>
              <span>详细填写工作经历有助于AI更准确地分析您的简历</span>
            </div>

            <div v-for="(item, index) in form.workExperience" :key="index" class="work-exp-item">
              <div class="work-exp-header">
                <span class="work-exp-number">#{{ index + 1 }}</span>
                <el-popconfirm
                  title="确定要删除这段工作经历吗？"
                  confirm-button-text="确定"
                  cancel-button-text="取消"
                  @confirm="removeWorkExp(index)"
                >
                  <template #reference>
                    <el-button type="danger" link size="small" class="delete-btn">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="公司名称">
                    <el-input v-model="item.company" placeholder="如：XX科技有限公司" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="职位">
                    <el-input v-model="item.position" placeholder="如：Java开发工程师" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="7">
                  <el-form-item label="开始时间" label-width="70px">
                    <el-date-picker
                      v-model="item.startDate"
                      type="month"
                      placeholder="选择年月"
                      format="YYYY-MM"
                      value-format="YYYY-MM"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="7">
                  <el-form-item label="结束时间" label-width="70px">
                    <el-date-picker
                      v-if="!item.current"
                      v-model="item.endDate"
                      type="month"
                      placeholder="选择年月"
                      format="YYYY-MM"
                      value-format="YYYY-MM"
                      style="width: 100%"
                    />
                    <el-tag v-else type="success" effect="plain" style="width: 100%; justify-content: center;">
                      至今
                    </el-tag>
                  </el-form-item>
                </el-col>
                <el-col :span="10">
                  <el-form-item label="&nbsp;" label-width="10px">
                    <el-checkbox v-model="item.current">至今（仍在职）</el-checkbox>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="工作内容">
                <el-input v-model="item.description" type="textarea" :rows="3" placeholder="请详细描述工作内容与职责" />
              </el-form-item>
              <el-divider v-if="index < form.workExperience.length - 1" style="margin: 12px 0;" />
            </div>

            <el-form-item>
              <el-button type="primary" plain @click="addWorkExp" class="add-work-btn">+ 添加工作经历</el-button>
            </el-form-item>

            <el-divider content-position="left" class="section-divider">
              <span class="divider-title">自我评价</span>
            </el-divider>

            <el-form-item label="自我评价">
              <el-input
                v-model="form.selfIntroduction"
                type="textarea"
                :rows="4"
                placeholder="请简要介绍自己的职业特点、核心优势和职业目标"
                maxlength="500"
                show-word-limit
              />
              <div class="form-tip">一段好的自我评价能让HR快速了解您的核心竞争力</div>
            </el-form-item>

            <el-form-item v-if="!isViewMode">
              <div class="form-actions">
                <el-button type="primary" size="large" style="width: 160px" @click="handleSave" :loading="saving">
                  {{ resumeId ? '更新简历' : '保存简历' }}
                </el-button>
                <el-button v-if="resumeId" size="large" @click="handleReset">重置</el-button>
                <el-button v-if="resumeId" size="large" @click="handleExportPdf" :loading="exporting">
                  <el-icon><Download /></el-icon> 导出PDF
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：上传与AI分析 (占 7/24) -->
      <el-col :span="7">
        <el-card v-if="!isViewMode" shadow="hover" class="upload-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">上传附件简历</span>
            </div>
          </template>
          <el-upload
            class="resume-upload"
            drag
            :http-request="handleDragUpload"
            :before-upload="beforeUpload"
            :show-file-list="false"
            accept=".pdf,.doc,.docx"
          >
            <el-icon class="upload-icon" :size="40"><UploadFilled /></el-icon>
            <div class="upload-text">
              <span class="upload-main">将简历文件拖到此处</span>
              <span class="upload-hint">或点击选择文件</span>
            </div>
            <template #tip>
              <div class="upload-tip">支持 PDF、DOC、DOCX 格式，不超过 10MB</div>
            </template>
          </el-upload>
          <div v-if="uploadedFile" class="uploaded-file">
            <el-tag type="success" closable @close="uploadedFile = null">
              {{ uploadedFile }}
            </el-tag>
          </div>

          <el-divider content-position="left">
            <span class="divider-subtitle">智能导入</span>
          </el-divider>

          <el-upload
            class="import-upload"
            :http-request="handleImportUpload"
            :before-upload="beforeImportUpload"
            :show-file-list="false"
            accept=".pdf,.doc,.docx"
          >
            <el-button type="primary" :loading="importing" class="import-btn">
              <el-icon><Document /></el-icon>
              {{ importing ? '正在解析...' : '智能导入简历' }}
            </el-button>
          </el-upload>
          <div class="import-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>自动解析简历内容并填充表单</span>
          </div>
        </el-card>

        <el-card shadow="hover" class="ai-cards-container">
          <div class="ai-header">
            <div class="ai-header-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>
            </div>
            <span>AI 智能分析报告</span>
          </div>

          <!-- 总分展示 -->
          <div class="score-banner">
            <div class="score-circle" :style="{ borderColor: getScoreColor(parsedAnalysis?.total_score) }">
              <span class="score-num" :style="{ color: getScoreColor(parsedAnalysis?.total_score) }">{{ parsedAnalysis?.total_score || 0 }}</span>
              <span class="score-unit">分</span>
            </div>
            <div class="score-info">
              <el-tag :type="getScoreTagType(parsedAnalysis?.total_score)" size="large" effect="dark">
                {{ parsedAnalysis?.score_level || '-' }}
              </el-tag>
              <p class="score-desc">综合评分基于5个维度的深度分析</p>
            </div>
          </div>

          <!-- 雷达图 -->
          <div class="radar-section">
            <div ref="radarChartRef" class="radar-chart"></div>
          </div>

          <!-- 维度评分明细 -->
          <div class="dim-details">
            <div v-for="(dim, key) in parsedAnalysis?.dimensions" :key="key" class="dim-card">
              <div class="dim-header">
                <span class="dim-name">{{ getDimLabel(key) }}</span>
                <span class="dim-score">{{ dim.score }}/{{ dim.max }}</span>
              </div>
              <el-progress :percentage="Math.round(dim.score / dim.max * 100)" :color="getDimColor(key)" :stroke-width="6" />
              <div v-if="dim.sub_scores?.length" class="sub-scores">
                <div v-for="sub in dim.sub_scores" :key="sub.item" class="sub-row">
                  <span class="sub-name">{{ sub.item }}</span>
                  <span class="sub-score">{{ sub.score }}/{{ sub.max }}</span>
                  <span class="sub-comment">{{ sub.comment }}</span>
                </div>
              </div>
            </div>
          </div>

          <el-collapse v-model="activeCollapse" class="ai-collapse">
            <!-- 优势与不足 -->
            <el-collapse-item name="strengths">
              <template #title>
                <span class="collapse-title">优势与不足</span>
              </template>
              <div class="sw-grid">
                <div class="sw-col strengths-col">
                  <div class="sw-col-header">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                    <span>核心优势</span>
                  </div>
                  <ul class="sw-list">
                    <li v-for="item in parsedAnalysis?.strengths" :key="item">{{ item }}</li>
                  </ul>
                </div>
                <div class="sw-col weaknesses-col" v-if="parsedAnalysis?.weaknesses?.length">
                  <div class="sw-col-header">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                    <span>待提升</span>
                  </div>
                  <ul class="sw-list">
                    <li v-for="item in parsedAnalysis?.weaknesses" :key="item">{{ item }}</li>
                  </ul>
                </div>
              </div>
            </el-collapse-item>

            <!-- 竞争力分析 -->
            <el-collapse-item name="competitive" v-if="parsedAnalysis?.competitive_analysis?.market_position !== '-'">
              <template #title>
                <span class="collapse-title">竞争力分析</span>
              </template>
              <div class="competitive-grid">
                <div class="comp-item">
                  <span class="comp-label">市场定位</span>
                  <span class="comp-value">{{ parsedAnalysis?.competitive_analysis?.market_position || '-' }}</span>
                </div>
                <div class="comp-item">
                  <span class="comp-label">核心竞争力</span>
                  <span class="comp-value">{{ parsedAnalysis?.competitive_analysis?.core_competitiveness || '-' }}</span>
                </div>
                <div class="comp-item">
                  <span class="comp-label">提升潜力</span>
                  <span class="comp-value">{{ parsedAnalysis?.competitive_analysis?.improvement_potential || '-' }}</span>
                </div>
              </div>
            </el-collapse-item>

            <!-- 职业建议 -->
            <el-collapse-item name="career">
              <template #title>
                <span class="collapse-title">职业建议</span>
              </template>
              <div class="career-section">
                <div class="career-tags" v-if="parsedAnalysis?.career_suggestions?.recommended_positions?.length">
                  <span class="career-tag" v-for="pos in parsedAnalysis?.career_suggestions?.recommended_positions" :key="pos">{{ pos }}</span>
                </div>
                <div class="career-info">
                  <div class="career-row" v-if="parsedAnalysis?.career_suggestions?.salary_range !== '-'">
                    <span class="career-label">建议薪资</span>
                    <span class="career-value">{{ parsedAnalysis?.career_suggestions?.salary_range }}</span>
                  </div>
                  <div class="career-row" v-if="parsedAnalysis?.career_suggestions?.development_path !== '-'">
                    <span class="career-label">发展路径</span>
                    <span class="career-value">{{ parsedAnalysis?.career_suggestions?.development_path }}</span>
                  </div>
                </div>
              </div>
            </el-collapse-item>

            <!-- 学习计划 -->
            <el-collapse-item name="learning">
              <template #title>
                <span class="collapse-title">学习计划</span>
              </template>
              <div class="learning-section">
                <div class="learning-group" v-if="parsedAnalysis?.learning_plan?.short_term?.length">
                  <div class="learning-group-title">短期目标（1-3个月）</div>
                  <div v-for="item in parsedAnalysis?.learning_plan?.short_term" :key="item.skill" class="learning-card">
                    <div class="learning-card-head">
                      <span class="learning-skill">{{ item.skill }}</span>
                      <span class="learning-priority" :class="item.priority === '高' ? 'high' : 'medium'">{{ item.priority }}</span>
                    </div>
                    <p class="learning-desc">{{ item.description }}</p>
                  </div>
                </div>
                <div class="learning-group" v-if="parsedAnalysis?.learning_plan?.long_term?.length">
                  <div class="learning-group-title">长期目标（3-6个月）</div>
                  <div v-for="item in parsedAnalysis?.learning_plan?.long_term" :key="item.skill" class="learning-card">
                    <div class="learning-card-head">
                      <span class="learning-skill">{{ item.skill }}</span>
                      <span class="learning-priority" :class="item.priority === '高' ? 'high' : 'medium'">{{ item.priority }}</span>
                    </div>
                    <p class="learning-desc">{{ item.description }}</p>
                  </div>
                </div>
              </div>
            </el-collapse-item>

            <!-- 面试建议 -->
            <el-collapse-item name="interview" v-if="parsedAnalysis?.interview_tips?.focus_areas?.length">
              <template #title>
                <span class="collapse-title">面试建议</span>
              </template>
              <div class="interview-section">
                <div class="interview-group">
                  <div class="interview-title">考察重点</div>
                  <div class="interview-chips">
                    <span class="interview-chip" v-for="item in parsedAnalysis?.interview_tips?.focus_areas" :key="item">{{ item }}</span>
                  </div>
                </div>
                <div class="interview-group" v-if="parsedAnalysis?.interview_tips?.suggested_questions?.length">
                  <div class="interview-title">建议提问</div>
                  <div class="interview-list">
                    <div class="interview-q" v-for="(item, idx) in parsedAnalysis?.interview_tips?.suggested_questions" :key="item">
                      <span class="q-num">{{ idx + 1 }}</span>
                      <span class="q-text">{{ item }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-collapse-item>

            <!-- 综合评价 -->
            <el-collapse-item name="comment">
              <template #title>
                <span class="collapse-title">综合评价</span>
              </template>
              <p class="overall-comment">{{ parsedAnalysis?.overall_comment }}</p>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, UploadFilled, Document, InfoFilled, CircleCheck, User, Briefcase, DataAnalysis, Monitor, Money, Setting, FirstAidKit, House, VideoCamera, ShoppingCart, Van, UserFilled, Wallet, Sell, TrendCharts, Platform, Operation, Brush, Histogram, Ticket, Reading, School, Postcard, BrushFilled, Shop, EditPen, Promotion, Goods, Grid, Download, Back, Plus, Files } from '@element-plus/icons-vue'
import { exportResumeToPdf } from '../utils/pdfExport'

const route = useRoute()
const router = useRouter()

const formRef = ref()
const saving = ref(false)
const aiLoading = ref(false)
const importing = ref(false)
const exporting = ref(false)
const resumeId = ref(null)
const aiResult = ref(null)
const uploadedFile = ref(null)
const activeCollapse = ref(['strengths'])
const radarChartRef = ref(null)
let radarChart = null

// 简历列表
const resumeList = ref([])
const resumeListLoading = ref(false)

const fetchResumeList = async () => {
  resumeListLoading.value = true
  try {
    const res = await request.get('/resume/my/list', { skipErrorNotification: true })
    resumeList.value = res || []
  } catch {
    resumeList.value = []
  } finally {
    resumeListLoading.value = false
  }
}

const switchResume = async (resume) => {
  if (resumeId.value === resume.id) return
  // 先保存当前编辑的内容
  await saveCurrentIfNeeded()
  // 加载新简历
  resumeId.value = resume.id
  aiResult.value = null
  await loadResumeData(resume.id)
}

const createNewResume = async () => {
  try {
    // 先创建一个空简历，获取 resumeId
    const payload = {
      name: '',
      phone: '',
      email: '',
      age: 25,
      education: '',
      categoryId: null,
      skills: '[]',
      experience: '',
      expectedSalary: 0,
      workExperience: null,
      selfIntroduction: ''
    }
    
    const res = await request.post('/resume', payload)
    resumeId.value = res.id
    
    // 清空表单
    aiResult.value = null
    uploadedFile.value = null
    form.name = ''
    form.phone = ''
    form.email = ''
    form.age = 25
    form.education = ''
    form.categoryId = null
    form.skills = ''
    form.experience = ''
    form.expectedSalary = null
    form.workExperience = []
    form.selfIntroduction = ''
    clearAiCache()
    
    // 刷新简历列表
    await fetchResumeList()
    
    ElMessage.success('已创建新简历，请上传附件或填写信息')
  } catch (error) {
    console.error('[新建简历] 失败:', error)
    ElMessage.error('创建简历失败')
  }
}

const handleSetDefault = async (resume) => {
  try {
    await ElMessageBox.confirm('确定将此简历设为默认吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await request.put(`/resume/${resume.id}/set-default`)
    ElMessage.success('设置成功')
    await fetchResumeList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('设置失败')
  }
}

const handleCopyResume = async (resume) => {
  try {
    await ElMessageBox.confirm(`确定要复制简历"${resume.resumeName || resume.name}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await request.post(`/resume/${resume.id}/copy`)
    ElMessage.success('复制成功')
    await fetchResumeList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('复制失败')
  }
}

const handleDeleteResume = async (resume) => {
  try {
    console.log('[删除简历] 准备删除:', resume)
    await ElMessageBox.confirm(`确定要删除简历"${resume.resumeName || resume.name}"吗？`, '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    console.log('[删除简历] 发送删除请求, ID:', resume.id)
    
    // 如果删除的是当前编辑的简历，先清除 resumeId 避免尝试保存已删除的简历
    const isCurrentResume = resumeId.value === resume.id
    if (isCurrentResume) {
      resumeId.value = null
    }
    
    await request.delete(`/resume/${resume.id}`)
    ElMessage.success('删除成功')
    await fetchResumeList()
    
    // 如果删除的是当前编辑的简历，切换到第一份
    if (isCurrentResume) {
      if (resumeList.value.length > 0) {
        // 直接加载，不调用 switchResume（避免保存操作）
        resumeId.value = resumeList.value[0].id
        aiResult.value = null
        await loadResumeData(resumeList.value[0].id)
      } else {
        await createNewResume()
      }
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('[删除简历] 失败:', e)
      const errorMsg = e.response?.data?.error || e.message || '删除失败'
      ElMessage.error(`删除失败：${errorMsg}`)
    }
  }
}

const saveCurrentIfNeeded = async () => {
  // 如果有编辑中的简历且表单有内容，自动保存
  if (resumeId.value && form.name) {
    try {
      const payload = buildPayload()
      await request.put(`/resume/${resumeId.value}`, payload)
    } catch { /* 静默保存失败不阻塞切换 */ }
  }
}

const buildPayload = () => {
  const payload = { ...form }
  payload.expectedSalary = payload.expectedSalary || 0
  if (typeof payload.skills === 'string') {
    const arr = payload.skills.split(/[,，、\s]+/).filter(s => s.trim())
    payload.skills = arr.length > 0 ? JSON.stringify(arr) : '[]'
  }
  payload.workExperience = form.workExperience.length > 0
    ? JSON.stringify(form.workExperience.map(exp => ({
        ...exp,
        endDate: exp.current ? '至今' : exp.endDate
      }))) : null
  return payload
}

const loadResumeData = async (id) => {
  try {
    const resumeData = await request.get(`/resume/${id}`)
    if (resumeData) {
      fillFormFromResume(resumeData)
    }
  } catch {
    ElMessage.error('加载简历失败')
  }
}

const fillFormFromResume = (resumeData) => {
  form.name = resumeData.name || ''
  form.phone = resumeData.phone || ''
  form.email = resumeData.email || ''
  form.age = resumeData.age || 25
  form.education = resumeData.education || ''
  form.categoryId = resumeData.categoryId || null
  if (resumeData.skills) {
    try {
      const arr = JSON.parse(resumeData.skills)
      form.skills = Array.isArray(arr) ? arr.join(', ') : resumeData.skills
    } catch {
      form.skills = resumeData.skills
    }
  } else {
    form.skills = ''
  }
  form.experience = resumeData.experience || ''
  form.expectedSalary = resumeData.expectedSalary || null
  if (resumeData.workExperience) {
    try {
      const arr = JSON.parse(resumeData.workExperience)
      form.workExperience = Array.isArray(arr) ? arr.map(exp => ({
        ...exp,
        startDate: normalizeDate(exp.startDate),
        endDate: exp.endDate === '至今' || exp.current ? '' : normalizeDate(exp.endDate),
        current: exp.current === true || exp.endDate === '至今' || !exp.endDate
      })) : []
    } catch {
      form.workExperience = []
    }
  } else {
    form.workExperience = []
  }
  if (resumeData.aiAnalysis) {
    aiResult.value = resumeData.aiAnalysis
    saveAiToCache(resumeData.id, resumeData.aiAnalysis)
  } else {
    const cached = loadAiFromCache(resumeData.id)
    aiResult.value = cached
  }
  form.selfIntroduction = resumeData.selfIntroduction || ''
  if (resumeData.fileUrl) {
    const parts = resumeData.fileUrl.split('/')
    uploadedFile.value = parts[parts.length - 1] || '已上传附件'
  }
}

// 是否为查看模式（查看其他用户的简历）
const isViewMode = computed(() => {
  // 检查是否通过新路由访问（/r/resume/:id）
  return route.name === 'ResumeDetail' || route.query.view === 'true'
})

// localStorage key for caching AI analysis
const AI_CACHE_KEY = 'resume_ai_analysis_cache'

// 从缓存加载AI分析结果
const loadAiFromCache = (id) => {
  try {
    const cache = localStorage.getItem(AI_CACHE_KEY)
    if (cache) {
      const data = JSON.parse(cache)
      if (data.resumeId === id && data.aiAnalysis) {
        return data.aiAnalysis
      }
    }
  } catch { /* ignore */ }
  return null
}

// 保存AI分析结果到缓存
const saveAiToCache = (id, aiAnalysis) => {
  try {
    localStorage.setItem(AI_CACHE_KEY, JSON.stringify({
      resumeId: id,
      aiAnalysis,
      savedAt: Date.now()
    }))
  } catch { /* ignore */ }
}

// 清除AI分析缓存
const clearAiCache = () => {
  try {
    localStorage.removeItem(AI_CACHE_KEY)
  } catch { /* ignore */ }
}

// 模板相关
const templateDialogVisible = ref(false)
const selectedTemplate = ref(null)
const activeTemplateCategory = ref('all') // 当前选中的模板分类

const templateCategories = [
  { id: 'all', name: '全部', icon: 'Grid' },
  { id: 'general', name: '通用职能', icon: 'Briefcase' },
  { id: 'internet', name: '互联网/IT', icon: 'Monitor' },
  { id: 'finance', name: '金融', icon: 'Money' },
  { id: 'manufacturing', name: '制造业', icon: 'Setting' },
  { id: 'education', name: '教育', icon: 'Reading' },
  { id: 'medical', name: '医疗', icon: 'FirstAidKit' },
  { id: 'construction', name: '房地产/建筑', icon: 'House' },
  { id: 'media', name: '文化传媒', icon: 'VideoCamera' },
  { id: 'retail', name: '零售/电商', icon: 'ShoppingCart' },
  { id: 'logistics', name: '物流/供应链', icon: 'Van' }
]

const resumeTemplates = [
  // 通用职能类
  {
    id: 'fresher',
    category: 'general',
    name: '应届生通用模板',
    description: '适合应届毕业生，突出教育背景和实习经历',
    icon: 'User',
    color: '#409eff'
  },
  {
    id: 'hr',
    category: 'general',
    name: '人力资源模板',
    description: '适合HRBP、招聘经理、薪酬绩效等人力资源岗位',
    icon: 'UserFilled',
    color: '#67c23a'
  },
  {
    id: 'finance_account',
    category: 'general',
    name: '财务/会计模板',
    description: '适合会计、出纳、财务分析、审计等财务岗位',
    icon: 'Wallet',
    color: '#e6a23c'
  },
  {
    id: 'sales',
    category: 'general',
    name: '销售/商务模板',
    description: '适合销售代表、大客户经理、商务拓展等销售岗位',
    icon: 'Sell',
    color: '#f56c6c'
  },
  {
    id: 'marketing',
    category: 'general',
    name: '市场营销模板',
    description: '适合品牌经理、市场推广、公关策划等市场岗位',
    icon: 'TrendCharts',
    color: '#909399'
  },
  {
    id: 'manager',
    category: 'general',
    name: '管理岗位模板',
    description: '适合总监、经理等管理岗位，突出团队规模和业绩成果',
    icon: 'Briefcase',
    color: '#6b7280'
  },
  // 互联网/IT类
  {
    id: 'developer',
    category: 'internet',
    name: '后端开发模板',
    description: '适合Java、Python、Go等后端开发工程师',
    icon: 'Monitor',
    color: '#409eff'
  },
  {
    id: 'frontend',
    category: 'internet',
    name: '前端开发模板',
    description: '适合Web前端、移动端开发工程师',
    icon: 'Platform',
    color: '#67c23a'
  },
  {
    id: 'product',
    category: 'internet',
    name: '产品经理模板',
    description: '适合产品经理、产品总监、UI/UX设计师',
    icon: 'DataAnalysis',
    color: '#e6a23c'
  },
  {
    id: 'operation',
    category: 'internet',
    name: '互联网运营模板',
    description: '适合内容运营、用户运营、活动运营、数据运营',
    icon: 'Operation',
    color: '#f56c6c'
  },
  {
    id: 'designer',
    category: 'internet',
    name: '设计/产品模板',
    description: '适合UI设计师、交互设计师、视觉设计师',
    icon: 'Brush',
    color: '#722ed1'
  },
  // 金融类
  {
    id: 'bank',
    category: 'finance',
    name: '银行/金融模板',
    description: '适合银行客户经理、理财经理、信贷审批等岗位',
    icon: 'Money',
    color: '#f56c6c'
  },
  {
    id: 'securities',
    category: 'finance',
    name: '证券/基金模板',
    description: '适合证券分析师、基金经理、投行经理等岗位',
    icon: 'Histogram',
    color: '#e6a23c'
  },
  {
    id: 'insurance',
    category: 'finance',
    name: '保险/精算模板',
    description: '适合保险精算师、核保师、理赔师等岗位',
    icon: 'Ticket',
    color: '#67c23a'
  },
  // 制造业类
  {
    id: 'manufacturing_eng',
    category: 'manufacturing',
    name: '制造工程师模板',
    description: '适合机械工程师、电气工程师、自动化工程师',
    icon: 'Setting',
    color: '#409eff'
  },
  {
    id: 'supply_chain',
    category: 'manufacturing',
    name: '供应链/采购模板',
    description: '适合供应链经理、采购专员、仓储物流等岗位',
    icon: 'Van',
    color: '#909399'
  },
  {
    id: 'quality',
    category: 'manufacturing',
    name: '质量管控模板',
    description: '适合QC/QA工程师、质量检验员、体系工程师',
    icon: 'CircleCheck',
    color: '#67c23a'
  },
  // 教育类
  {
    id: 'teacher',
    category: 'education',
    name: '教师/教研模板',
    description: '适合学科教师、教研组长、培训机构讲师',
    icon: 'Reading',
    color: '#409eff'
  },
  {
    id: 'education_ops',
    category: 'education',
    name: '教育运营模板',
    description: '适合校区运营、课程顾问、招生老师、教务专员',
    icon: 'School',
    color: '#e6a23c'
  },
  // 医疗类
  {
    id: 'doctor',
    category: 'medical',
    name: '临床医师模板',
    description: '适合内科、外科、妇产科、儿科等临床医师',
    icon: 'FirstAidKit',
    color: '#f56c6c'
  },
  {
    id: 'nurse',
    category: 'medical',
    name: '护理/医技模板',
    description: '适合护士、检验师、影像师、药剂师等岗位',
    icon: 'Postcard',
    color: '#67c23a'
  },
  {
    id: 'pharma',
    category: 'medical',
    name: '医药/器械模板',
    description: '适合医药代表、产品经理、药品研发工程师',
    icon: 'FirstAidKit',
    color: '#e6a23c'
  },
  // 房地产/建筑类
  {
    id: 'construction_eng',
    category: 'construction',
    name: '建筑工程模板',
    description: '适合土建工程师、结构工程师、造价工程师',
    icon: 'House',
    color: '#409eff'
  },
  {
    id: 'architect',
    category: 'construction',
    name: '建筑/室内设计模板',
    description: '适合建筑设计师、室内设计师、景观设计师',
    icon: 'BrushFilled',
    color: '#722ed1'
  },
  {
    id: 'property',
    category: 'construction',
    name: '物业/招商模板',
    description: '适合物业经理、商业运营、招商经理等岗位',
    icon: 'Shop',
    color: '#909399'
  },
  // 文化传媒类
  {
    id: 'content',
    category: 'media',
    name: '内容创作模板',
    description: '适合编辑、记者、文案策划、自媒体博主',
    icon: 'EditPen',
    color: '#409eff'
  },
  {
    id: 'newmedia',
    category: 'media',
    name: '新媒体/短视频模板',
    description: '适合新媒体运营、短视频编导、直播运营',
    icon: 'VideoCamera',
    color: '#f56c6c'
  },
  {
    id: 'advertising',
    category: 'media',
    name: '广告/公关模板',
    description: '适合广告策划、客户执行、公关专员、媒介经理',
    icon: 'Promotion',
    color: '#e6a23c'
  },
  // 零售/电商类
  {
    id: 'retail_store',
    category: 'retail',
    name: '零售门店模板',
    description: '适合店长、门店主管、导购员、营业员',
    icon: 'ShoppingCart',
    color: '#409eff'
  },
  {
    id: 'ecommerce',
    category: 'retail',
    name: '电商运营模板',
    description: '适合天猫/京东运营、跨境电商、电商客服',
    icon: 'Goods',
    color: '#f56c6c'
  },
  // 物流/供应链类
  {
    id: 'logistics',
    category: 'logistics',
    name: '物流管理模板',
    description: '适合物流经理、仓储主管、运输调度、配送员',
    icon: 'Van',
    color: '#67c23a'
  }
]

const filteredTemplates = computed(() => {
  if (activeTemplateCategory.value === 'all') return resumeTemplates
  return resumeTemplates.filter(t => t.category === activeTemplateCategory.value)
})

const showTemplateDialog = () => {
  selectedTemplate.value = null
  activeTemplateCategory.value = 'all'
  templateDialogVisible.value = true
}

const getTemplatePreview = (field) => {
  const previews = {
    fresher: {
      skills: 'Python, Java, SQL, Git, Office',
      experience: '应届毕业生，具备扎实的专业基础和良好的学习能力',
      workExperience: 'XX科技有限公司 - 实习生 (2024.06-2024.09)',
      selfIntroduction: '应届毕业生，计算机科学与技术专业，具备扎实的编程基础...'
    },
    developer: {
      skills: 'Java, Spring Boot, Vue.js, MySQL, Redis, Docker, Git, Linux',
      experience: '5年全栈开发经验，熟悉微服务架构，有大型项目开发经验',
      workExperience: 'XX科技有限公司 - 高级开发工程师 (2021.03-至今)',
      selfIntroduction: '5年全栈开发经验，精通Java/Spring Boot后端开发...'
    },
    manager: {
      skills: '项目管理, 团队管理, 敏捷开发, Scrum, OKR, 数据分析',
      experience: '10年互联网行业经验，5年管理经验，带领过20人团队',
      workExperience: 'XX集团 - 技术总监 (2020.01-至今)',
      selfIntroduction: '10年互联网行业从业经验，其中5年技术管理经验...'
    },
    designer: {
      skills: 'Figma, Sketch, Adobe XD, Photoshop, Illustrator, 用户研究, 原型设计',
      experience: '4年产品设计经验，擅长用户研究和交互设计',
      workExperience: 'XX互联网公司 - 高级UI设计师 (2022.01-至今)',
      selfIntroduction: '4年产品设计经验，擅长用户研究、交互设计和视觉设计...'
    },
    hr: {
      skills: '招聘管理, 薪酬绩效, 员工关系, HRBP, 培训体系, 劳动法',
      experience: '6年人力资源全流程管理经验，擅长招聘与员工关系维护',
      workExperience: 'XX集团 - 人力资源经理 (2020.01-至今)',
      selfIntroduction: '6年人力资源从业经验，精通招聘配置与薪酬绩效管理...'
    },
    finance_account: {
      skills: '会计准则, 税务申报, 财务分析, ERP系统, Excel高级应用, 审计配合',
      experience: '5年总账会计经验，熟悉制造业成本核算与税务筹划',
      workExperience: 'XX制造有限公司 - 会计主管 (2019.07-至今)',
      selfIntroduction: '5年财务会计工作经验，持有中级会计师职称...'
    },
    sales: {
      skills: '商务谈判, 客户拓展, 渠道管理, CRM系统, 市场分析, 销售技巧',
      experience: '4年大客户销售经验，累计完成销售额超3000万',
      workExperience: 'XX软件公司 - 大客户经理 (2021.03-至今)',
      selfIntroduction: '4年B端销售经验，擅长挖掘客户需求与长期关系维护...'
    },
    bank: {
      skills: '信贷审批, 风险控制, 理财规划, 银行业务, 客户服务, 合规管理',
      experience: '3年银行对公业务经验，熟悉企业信贷流程',
      workExperience: 'XX银行 - 对公客户经理 (2022.01-至今)',
      selfIntroduction: '3年银行从业经验，专注于中小企业金融服务...'
    },
    manufacturing_eng: {
      skills: '机械设计, AutoCAD, SolidWorks, 工艺优化, 生产现场管理, 质量控制',
      experience: '7年机械工程师经验，负责自动化产线设计与改造',
      workExperience: 'XX重工 - 机械工程师 (2018.06-至今)',
      selfIntroduction: '7年机械制造行业经验，精通非标自动化设备设计...'
    },
    teacher: {
      skills: '学科教学, 课程设计, 班级管理, 学生心理辅导, 家校沟通, 教研活动',
      experience: '5年高中数学教学经验，带出多名重点大学学生',
      workExperience: 'XX高级中学 - 数学教师 (2019.09-至今)',
      selfIntroduction: '5年一线教学经验，注重启发式教学与学生思维培养...'
    },
    doctor: {
      skills: '临床诊断, 病历书写, 医患沟通, 急救处理, 专科手术, 医学科研',
      experience: '8年内科临床经验，擅长慢性病管理与疑难杂症诊治',
      workExperience: 'XX市人民医院 - 主治医师 (2017.07-至今)',
      selfIntroduction: '8年临床医师经验，始终秉持医者仁心，专注患者健康...'
    },
    construction_eng: {
      skills: '土建施工, 工程管理, 造价预算, 招投标, 质量验收, 安全规范',
      experience: '6年建筑工程管理经验，负责过多个大型住宅项目',
      workExperience: 'XX建设集团 - 项目经理 (2019.03-至今)',
      selfIntroduction: '6年建筑施工管理经验，持有一级建造师证书...'
    },
    content: {
      skills: '文案写作, 内容策划, 选题规划, 编辑校对, 新媒体排版, 热点追踪',
      experience: '4年媒体编辑经验，擅长深度报道与专题策划',
      workExperience: 'XX传媒 - 资深编辑 (2021.01-至今)',
      selfIntroduction: '4年内容创作经验，文字功底扎实，对热点敏感...'
    },
    retail_store: {
      skills: '门店运营, 商品陈列, 库存管理, 销售技巧, 团队激励, 客户服务',
      experience: '3年连锁零售店长经验，单店业绩常年排名区域前三',
      workExperience: 'XX品牌服饰 - 店长 (2022.05-至今)',
      selfIntroduction: '3年零售门店管理经验，擅长通过精细化运营提升业绩...'
    },
    logistics: {
      skills: '仓储管理, 物流配送, 供应链协调, 路线规划, 成本控制, 异常处理',
      experience: '5年物流调度经验，熟悉全国干线运输与末端配送网络',
      workExperience: 'XX物流 - 调度主管 (2020.08-至今)',
      selfIntroduction: '5年物流行业经验，致力于提升物流时效与服务体验...'
    }
  }
  return previews[selectedTemplate.value]?.[field] || ''
}

const applyTemplate = () => {
  const template = resumeTemplates.find(t => t.id === selectedTemplate.value)
  if (!template) return

  const tpl = (age, edu, skills, exp, salary, intro, jobs) => ({
    name: '', age, education: edu, skills, experience: exp,
    expectedSalary: salary, selfIntroduction: intro,
    workExperience: jobs
  })

  const templates = {
    fresher: tpl(22, '本科', 'Python, Java, SQL, Git, Office', '应届毕业生，具备扎实的专业基础和良好的学习能力', 8000,
      '应届毕业生，计算机科学与技术专业，在校期间成绩优异，多次获得奖学金。具备扎实的编程基础和良好的学习能力，有实习经历，熟悉软件开发流程。',
      [{ company: 'XX科技有限公司', position: '实习生', startDate: '2024-06', endDate: '2024-09', current: false, description: '参与公司项目的开发与测试工作' }]),
    developer: tpl(28, '本科', 'Java, Spring Boot, Vue.js, MySQL, Redis, Docker, Git, Linux', '5年全栈开发经验，熟悉微服务架构，有大型项目开发经验', 25000,
      '5年全栈开发经验，精通Java/Spring Boot后端开发和Vue.js前端开发。熟悉微服务架构设计，有千万级用户系统的开发和优化经验。',
      [{ company: 'XX科技有限公司', position: '高级开发工程师', startDate: '2021-03', endDate: '', current: true, description: '负责核心系统的架构设计和开发，带领3人小组完成多个重要项目' },
       { company: 'YY互联网公司', position: 'Java开发工程师', startDate: '2019-07', endDate: '2021-02', current: false, description: '负责后端API开发和数据库设计' }]),
    frontend: tpl(26, '本科', 'Vue.js, React, TypeScript, Webpack, Vite, HTML5, CSS3, JavaScript', '4年前端开发经验，擅长响应式设计和组件化开发', 20000,
      '4年前端开发经验，精通Vue/React框架，擅长响应式设计和组件化开发。熟悉Webpack/Vite等构建工具，注重用户体验和性能优化。',
      [{ company: 'XX互联网公司', position: '前端开发工程师', startDate: '2022-01', endDate: '', current: true, description: '负责核心产品的前端开发，使用Vue3+TypeScript重构旧系统，页面性能提升40%' }]),
    hr: tpl(30, '本科', '招聘管理, 薪酬绩效, 员工关系, HRBP, 培训体系, 劳动法', '6年人力资源经验，3年HRBP经验，擅长招聘和绩效管理', 18000,
      '6年人力资源从业经验，熟悉招聘、培训、绩效、员工关系等模块。擅长人才盘点和组织发展，具备较强的沟通协调能力。',
      [{ company: 'XX集团', position: 'HRBP', startDate: '2020-03', endDate: '', current: true, description: '负责业务部门的人力资源支持，年度招聘完成率95%以上' }]),
    finance_account: tpl(30, '本科', '会计核算, 税务筹划, 财务分析, Excel, 用友, 金蝶, CPA', '5年财务经验，熟悉全盘账务处理和税务申报', 15000,
      '5年财务工作经验，具备注册会计师资格，熟悉企业全盘账务处理和税务申报。擅长财务分析和成本控制。',
      [{ company: 'XX有限公司', position: '总账会计', startDate: '2020-01', endDate: '', current: true, description: '负责公司全盘账务处理、税务申报和财务报表编制' }]),
    sales: tpl(30, '本科', '客户开发, 商务谈判, 合同管理, CRM系统, 市场分析', '7年销售经验，累计完成销售额5000万+，擅长B2B大客户销售', 20000,
      '7年B2B销售经验，擅长客户关系维护和商务谈判。累计完成销售额5000万+，客户续约率90%以上。',
      [{ company: 'XX科技有限公司', position: '大客户经理', startDate: '2019-06', endDate: '', current: true, description: '负责大客户的开发与维护，年度销售额目标完成率120%' }]),
    marketing: tpl(28, '本科', '品牌策划, 市场推广, 新媒体运营, 数据分析, 活动策划, SEO/SEM', '4年市场营销经验，主导过多个品牌推广项目', 18000,
      '4年市场营销经验，擅长品牌策划和市场推广。主导过多个大型品牌推广项目，累计触达用户超百万。',
      [{ company: 'XX品牌公司', position: '品牌经理', startDate: '2021-03', endDate: '', current: true, description: '负责品牌战略规划与执行，品牌知名度提升35%' }]),
    manager: tpl(35, '硕士', '项目管理, 团队管理, 敏捷开发, Scrum, OKR, 数据分析', '10年互联网行业经验，5年管理经验，带领过20人团队', 40000,
      '10年互联网行业从业经验，其中5年技术管理经验。曾带领20人技术团队完成多个千万级项目的从0到1落地。',
      [{ company: 'XX集团', position: '技术总监', startDate: '2020-01', endDate: '', current: true, description: '负责技术团队管理和产品技术规划，团队规模20人' },
       { company: 'YY科技', position: '项目经理', startDate: '2017-06', endDate: '2019-12', current: false, description: '负责多个项目的整体管理，客户满意度95%以上' }]),
    product: tpl(28, '本科', '需求分析, 原型设计, Axure, Figma, 用户研究, 数据分析', '5年产品经验，主导过3个从0到1的产品', 25000,
      '5年互联网产品经验，擅长用户需求分析和产品规划。主导过3个从0到1的产品，累计用户超百万。',
      [{ company: 'XX互联网公司', position: '高级产品经理', startDate: '2021-01', endDate: '', current: true, description: '负责产品规划和迭代管理，DAU提升50%' }]),
    operation: tpl(27, '本科', '用户运营, 内容运营, 活动策划, 数据分析, 社群运营, 增长黑客', '4年互联网运营经验，擅长用户增长和留存', 18000,
      '4年互联网运营经验，累计操盘过多个用户增长项目。擅长数据分析驱动的精细化运营。',
      [{ company: 'XX互联网公司', position: '高级用户运营', startDate: '2021-06', endDate: '', current: true, description: '负责用户增长和留存策略，月活用户增长80%' }]),
    designer: tpl(26, '本科', 'Figma, Sketch, Adobe XD, Photoshop, Illustrator, 用户研究, 原型设计', '4年产品设计经验，擅长用户研究和交互设计', 20000,
      '4年产品设计经验，擅长用户研究、交互设计和视觉设计。曾主导3个产品的改版设计，用户活跃度提升30%。',
      [{ company: 'XX互联网公司', position: '高级UI设计师', startDate: '2022-01', endDate: '', current: true, description: '负责APP和Web端的UI设计，用户活跃度提升30%' },
       { company: 'YY设计工作室', position: 'UI设计师', startDate: '2020-07', endDate: '2021-12', current: false, description: '为多个客户完成品牌设计和界面设计项目' }]),
    bank: tpl(28, '本科', '信贷审批, 理财销售, 风险控制, 客户管理, 金融产品, 银行合规', '5年银行经验，累计管理资产规模2亿+', 18000,
      '5年银行从业经验，持有AFP/CFP证书，擅长理财规划和客户管理。累计管理资产规模2亿+。',
      [{ company: 'XX银行', position: '理财经理', startDate: '2020-01', endDate: '', current: true, description: '负责高净值客户的理财规划和资产配置，年度AUM增长30%' }]),
    securities: tpl(30, '硕士', '证券投资, 行业研究, 财务分析, 风险管理, 基金从业', '6年证券研究经验，覆盖TMT行业', 30000,
      '6年证券研究经验，擅长行业分析和个股估值。覆盖TMT行业，累计发布深度研究报告50+篇。',
      [{ company: 'XX证券', position: '行业研究员', startDate: '2019-07', endDate: '', current: true, description: '负责TMT行业研究分析，撰写深度研究报告' }]),
    insurance: tpl(28, '硕士', '精算建模, 产品设计, 风险评估, 核保理赔, Excel, Python', '4年保险精算经验，熟悉寿险/财险产品定价', 25000,
      '4年保险精算经验，中国精算师协会会员，擅长产品定价和准备金评估。熟练使用Python进行精算建模。',
      [{ company: 'XX保险', position: '精算师', startDate: '2021-03', endDate: '', current: true, description: '负责保险产品的定价和准备金评估' }]),
    manufacturing_eng: tpl(32, '本科', '机械设计, AutoCAD, SolidWorks, PLC编程, 工艺优化, 设备管理', '8年机械制造经验，5年自动化产线改造经验', 20000,
      '8年机械制造行业经验，擅长自动化产线设计和工艺优化。主导过多个大型自动化改造项目，生产效率提升40%。',
      [{ company: 'XX制造集团', position: '高级机械工程师', startDate: '2018-03', endDate: '', current: true, description: '负责自动化产线设计和改造，效率提升40%' }]),
    supply_chain: tpl(32, '本科', '供应链管理, 采购谈判, 供应商管理, ERP系统, 库存控制, 物流规划', '6年供应链经验，管理过50+供应商', 20000,
      '6年供应链管理经验，擅长供应商评估和采购成本控制。管理过50+供应商，年度采购成本降低15%。',
      [{ company: 'XX集团', position: '供应链经理', startDate: '2019-01', endDate: '', current: true, description: '负责供应链全流程管理，年度采购成本降低15%' }]),
    quality: tpl(30, '本科', 'ISO9001, QC七大手法, SPC, MSA, FMEA, 8D报告', '5年质量管理经验，熟悉IATF16949体系', 15000,
      '5年质量管理经验，内审员资格，擅长质量体系建设和问题解决。产品不良率降低50%。',
      [{ company: 'XX汽车零部件公司', position: 'QA主管', startDate: '2020-06', endDate: '', current: true, description: '负责质量体系建设和问题解决，产品不良率降低50%' }]),
    teacher: tpl(30, '本科', '教学设计, 课堂管理, 课件制作, 教育心理学, 课程开发', '5年教学经验，带过3届毕业班', 12000,
      '5年一线教学经验，擅长激发学生学习兴趣，注重因材施教。带过3届毕业班，升学率95%以上。',
      [{ company: 'XX中学', position: '高级教师', startDate: '2019-09', endDate: '', current: true, description: '负责学科教学和班级管理，升学率连续3年保持在95%以上' }]),
    education_ops: tpl(30, '本科', '校区运营, 招生推广, 课程咨询, 团队管理, 数据分析', '4年教育行业运营经验，管理过200人校区', 15000,
      '4年教育行业管理经验，擅长校区运营和团队建设。管理过200人规模的校区，年度招生目标完成率110%。',
      [{ company: 'XX教育集团', position: '校区校长', startDate: '2021-01', endDate: '', current: true, description: '负责校区整体运营管理，年度招生目标完成率110%' }]),
    doctor: tpl(35, '硕士', '临床诊断, 病历书写, 医患沟通, 医疗规范, 专科技术', '8年临床经验，主治医师，发表SCI论文3篇', 25000,
      '8年临床经验，擅长常见病和多发病的诊治，注重循证医学。发表SCI论文3篇，参与多项临床研究。',
      [{ company: 'XX三甲医院', position: '主治医师', startDate: '2018-07', endDate: '', current: true, description: '负责临床诊断和治疗，年接诊量3000+' }]),
    nurse: tpl(28, '本科', '临床护理, 急救技能, 病房管理, 护理文书, 院感防控', '6年护理经验，主管护师，ICU工作经验', 12000,
      '6年临床护理经验，ICU专科护士，擅长危重患者护理。持有主管护师资格，参与多项护理质量改进项目。',
      [{ company: 'XX医院', position: '主管护师', startDate: '2019-03', endDate: '', current: true, description: '负责ICU危重患者护理和病房管理' }]),
    pharma: tpl(28, '本科', '医药推广, 学术营销, 产品知识, 客户管理, 市场分析', '5年医药代表经验，负责心内科产品线', 18000,
      '5年医药推广经验，熟悉心内科产品线，擅长学术推广和专家维护。年度销售目标完成率120%以上。',
      [{ company: 'XX制药', position: '高级医药代表', startDate: '2020-01', endDate: '', current: true, description: '负责心内科产品线推广，覆盖20+核心医院' }]),
    construction_eng: tpl(32, '本科', '土建施工, 项目管理, 造价管理, 工程预算, 安全规范', '7年建筑施工经验，5年项目管理经验', 20000,
      '7年建筑施工经验，一级建造师，擅长项目全过程管理。主导过多个大型工程项目，累计施工面积超10万平。',
      [{ company: 'XX建设集团', position: '项目经理', startDate: '2018-06', endDate: '', current: true, description: '负责工程项目全过程管理，累计完成5个大型项目' }]),
    architect: tpl(29, '本科', '建筑设计, AutoCAD, SketchUp, Revit, 方案汇报, 施工图', '5年建筑设计经验，参与过10+商业综合体项目', 18000,
      '5年建筑设计经验，擅长商业综合体和住宅设计。参与过10+大型项目，其中3个项目获省级优秀设计奖。',
      [{ company: 'XX设计院', position: '建筑设计师', startDate: '2020-03', endDate: '', current: true, description: '负责建筑方案设计和施工图深化' }]),
    property: tpl(32, '本科', '物业管理, 客户服务, 设施管理, 招商运营, 团队管理', '6年物业经验，管理过3个商业综合体', 15000,
      '6年物业管理经验，擅长商业体运营和客户服务管理。管理过3个商业综合体，客户满意度95%以上。',
      [{ company: 'XX物业集团', position: '物业经理', startDate: '2019-06', endDate: '', current: true, description: '负责商业综合体物业管理，团队规模50人' }]),
    content: tpl(28, '本科', '文案撰写, 内容策划, 编辑排版, 选题策划, SEO优化', '4年内容创作经验，累计发表原创文章200+篇', 15000,
      '4年内容创作经验，擅长深度报道和专题策划。累计发表原创文章200+篇，多篇阅读量10万+。',
      [{ company: 'XX传媒公司', position: '资深编辑', startDate: '2021-03', endDate: '', current: true, description: '负责专题策划和内容生产，多篇阅读量10万+' }]),
    newmedia: tpl(26, '本科', '短视频运营, 抖音/快手, 直播策划, 数据分析, 社群运营', '3年新媒体运营经验，操盘过百万粉账号', 15000,
      '3年新媒体运营经验，擅长短视频内容策划和账号孵化。操盘过多个百万粉账号，累计粉丝量超500万。',
      [{ company: 'XXMCN机构', position: '新媒体运营主管', startDate: '2022-01', endDate: '', current: true, description: '负责短视频账号运营和孵化，累计粉丝量超500万' }]),
    advertising: tpl(30, '本科', '广告策划, 创意设计, 客户提案, 媒介投放, 品牌管理', '5年广告行业经验，服务过多个知名品牌', 20000,
      '5年广告策划经验，擅长品牌传播策略和创意提案。服务过多个知名品牌，主导过多个千万级广告项目。',
      [{ company: 'XX广告公司', position: '策划总监', startDate: '2020-06', endDate: '', current: true, description: '负责品牌广告策划和创意提案，客户续约率90%' }]),
    retail_store: tpl(30, '大专', '门店管理, 销售管理, 库存控制, 客户服务, 陈列规划', '6年零售经验，3年店长管理经验', 10000,
      '6年零售行业经验，擅长门店运营管理和团队建设。管理过3家门店，年度销售目标完成率均在100%以上。',
      [{ company: 'XX连锁零售', position: '店长', startDate: '2019-03', endDate: '', current: true, description: '负责门店日常运营和团队管理，年度销售目标完成率110%' }]),
    ecommerce: tpl(28, '本科', '店铺运营, 直通车/钻展, 数据分析, 爆款打造, 客户服务', '4年电商运营经验，年GMV破千万', 18000,
      '4年电商运营经验，擅长店铺规划和爆款打造。操盘过多个天猫/京东店铺，年GMV破千万。',
      [{ company: 'XX电商公司', position: '天猫运营主管', startDate: '2021-06', endDate: '', current: true, description: '负责天猫店铺整体运营，年GMV超1000万' }]),
    logistics: tpl(32, '本科', '仓储管理, 运输调度, 配送管理, 路线优化, ERP系统', '6年物流经验，管理过5000平仓库', 15000,
      '6年物流管理经验，擅长仓储运营和配送效率优化。管理过5000平仓库，库存准确率达99.5%。',
      [{ company: 'XX物流集团', position: '仓储经理', startDate: '2019-01', endDate: '', current: true, description: '负责仓储运营管理，库存准确率达99.5%' }])
  }

  const templateData = templates[selectedTemplate.value]
  if (templateData) {
    Object.assign(form, templateData)
    templateDialogVisible.value = false
    ElMessage.success(`已应用"${template.name}"模板，请填写个人信息`)
  }
}

// 解析AI分析结果 - 多维度增强版 v3
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

  while (values.length < 5) values.push(0)
  values = values.slice(0, 5)

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

  values = values.map((v, i) => Math.min(Math.max(0, v), max_values[i]))

  return { labels, values, max_values }
}

const parsedAnalysis = computed(() => {
  if (!aiResult.value) return null
  try {
    let jsonStr = aiResult.value.trim()

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
    // 容错：修复未闭合的数组/对象（AI输出截断）
    jsonStr = fixUnclosedBrackets(jsonStr)

    const parsed = JSON.parse(jsonStr)
    const dimensions = parsed.dimensions || {}

    // 兼容新旧格式
    return {
      total_score: parsed.total_score || 0,
      score_level: parsed.score_level || '-',
      radar_data: normalizeRadarData(parsed.radar_data, dimensions),
      dimensions: dimensions,
      strengths: parsed.strengths || [],
      weaknesses: parsed.weaknesses || [],
      competitive_analysis: parsed.competitive_analysis || { market_position: '-', core_competitiveness: '-', improvement_potential: '-' },
      career_suggestions: parsed.career_suggestions || {
        recommended_positions: [],
        salary_range: '-',
        development_path: '-'
      },
      learning_plan: parsed.learning_plan || {
        short_term: [],
        long_term: []
      },
      interview_tips: parsed.interview_tips || { focus_areas: [], suggested_questions: [] },
      overall_comment: parsed.overall_comment || '-',
      // 兼容旧格式
      basic_info: parsed.basic_info || {
        skills: [],
        experience_level: '-',
        education_level: '-',
        potential_score: 0,
        potential_level: '-'
      }
    }
  } catch {
    // 最后手段：用正则提取关键字段
    try {
      const raw = aiResult.value
      const scoreMatch = raw.match(/"?total_score"?\s*[:：]\s*(\d+)/)
      const levelMatch = raw.match(/"?score_level"?\s*[:：]\s*"([^"]+)"/)
      const commentMatch = raw.match(/"?overall_comment"?\s*[:：]\s*"([^"]*?)"/)
      if (scoreMatch) {
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

// 评分颜色
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

const dimLabelMap = {
  basic_info: '基本信息', skill: '技能水平', experience: '工作经历',
  education: '教育背景', potential: '职业潜力'
}
const getDimLabel = (key) => dimLabelMap[key] || key

const dimColorMap = {
  basic_info: '#6366f1', skill: '#10b981', experience: '#f59e0b',
  education: '#3b82f6', potential: '#ec4899'
}
const getDimColor = (key) => dimColorMap[key] || '#6366f1'
const getDimGradient = (key) => {
  const c = getDimColor(key)
  return `linear-gradient(90deg, ${c}, ${c}88)`
}

// 渲染雷达图
const renderRadarChart = () => {
  if (!radarChartRef.value || !parsedAnalysis.value?.radar_data) return
  if (radarChart) radarChart.dispose()
  radarChart = echarts.init(radarChartRef.value)

  const { labels, values, max_values } = parsedAnalysis.value.radar_data
  const shortLabels = labels.map(label => {
    const map = { '基本信息': '基础', '技能水平': '技能', '工作经历': '经验', '教育背景': '学历', '职业潜力': '潜力' }
    return map[label] || label
  })
  const indicator = shortLabels.map((label, i) => {
    return { name: label, max: max_values[i] || 100 }
  })

  radarChart.setOption({
    radar: {
      indicator,
      shape: 'circle',
      radius: '70%',
      splitNumber: 4,
      axisName: { color: '#64748b', fontSize: 12, padding: [3, 5] },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
      splitArea: { show: true, areaStyle: { color: ['rgba(99,102,241,0.02)', 'rgba(99,102,241,0.05)'] } },
      axisLine: { lineStyle: { color: '#e2e8f0' } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        areaStyle: { color: 'rgba(99, 102, 241, 0.15)' },
        lineStyle: { color: '#6366f1', width: 2 },
        itemStyle: { color: '#6366f1', borderWidth: 2 },
        symbol: 'circle',
        symbolSize: 6
      }]
    }]
  })
}

const uploadHeaders = {
  Authorization: `Bearer ${localStorage.getItem('token')}`
}

const form = reactive({
  name: '',
  phone: '',
  email: '',
  age: 25,
  education: '',
  categoryId: null,
  skills: '',
  experience: '',
  expectedSalary: null,
  workExperience: [],
  selfIntroduction: ''
})

const categories = ref([])

const fetchCategories = async () => {
  try {
    const res = await request.get('/category/list')
    categories.value = res.data || []
  } catch (error) {
    console.error('获取分类失败', error)
  }
}

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

const beforeUpload = (file) => {
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  return true
}

const beforeImportUpload = (file) => {
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  importing.value = true
  return true
}

// 拖拽上传处理
const handleDragUpload = async ({ file }) => {
  try {
    const formData = new FormData()
    formData.append('file', file)
    // 传递当前编辑的简历ID
    if (resumeId.value) {
      formData.append('resumeId', resumeId.value)
      console.log('[拖拽上传] 传递 resumeId:', resumeId.value)
    } else {
      console.log('[拖拽上传] 无 resumeId，将创建新简历')
    }
    
    const response = await request.post('/resume/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    console.log('[拖拽上传] 响应数据:', response)
    console.log('[拖拽上传] workExperience:', response.workExperience)
    
    // 直接处理响应数据
    if (response && response.id) {
      resumeId.value = response.id
      if (response.name) form.name = response.name
      if (response.phone) form.phone = response.phone
      if (response.email) form.email = response.email
      if (response.age) form.age = response.age
      if (response.education) form.education = response.education
      if (response.experience) form.experience = response.experience
      if (response.selfIntroduction) form.selfIntroduction = response.selfIntroduction
      
      if (response.skills) {
        try {
          const arr = JSON.parse(response.skills)
          form.skills = Array.isArray(arr) ? arr.join(', ') : response.skills
        } catch {
          form.skills = response.skills
        }
      }
      
      if (response.workExperience) {
        try {
          const arr = JSON.parse(response.workExperience)
          form.workExperience = Array.isArray(arr) ? arr.map(exp => ({
            ...exp,
            startDate: normalizeDate(exp.startDate),
            endDate: exp.endDate === '至今' || exp.current ? '' : normalizeDate(exp.endDate),
            current: exp.current === true || exp.endDate === '至今' || !exp.endDate
          })) : []
        } catch {
          form.workExperience = []
        }
      }
      
      if (response.fileUrl) {
        const parts = response.fileUrl.split('/')
        uploadedFile.value = parts[parts.length - 1] || '已上传附件'
      }
      
      ElMessage.success(response.message || '上传成功')
      
      if (response.aiAnalysis) {
        aiResult.value = response.aiAnalysis
      }
      fetchResumeList()
    } else {
      console.error('[拖拽上传] response.id 不存在', response)
      ElMessage.error('上传失败：未获取到简历ID')
    }
  } catch (error) {
    console.error('[拖拽上传] 失败:', error)
    ElMessage.error('上传失败，请重试')
  }
}

// 自定义上传处理
const handleImportUpload = async ({ file }) => {
  try {
    importing.value = true
    const formData = new FormData()
    formData.append('file', file)
    // 传递当前编辑的简历ID
    if (resumeId.value) {
      formData.append('resumeId', resumeId.value)
      console.log('[智能导入] 传递 resumeId:', resumeId.value)
    } else {
      console.log('[智能导入] 无 resumeId，将创建新简历')
    }
    
    const response = await request.post('/resume/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    importing.value = false
    
    if (response && response.id) {
      resumeId.value = response.id
      
      form.name = response.name || ''
      form.phone = response.phone || ''
      form.email = response.email || ''
      form.age = response.age || 25
      form.education = response.education || ''
      form.categoryId = response.categoryId || null
      
      if (response.skills) {
        try {
          const arr = JSON.parse(response.skills)
          form.skills = Array.isArray(arr) ? arr.join(', ') : response.skills
        } catch {
          form.skills = response.skills
        }
      } else {
        form.skills = ''
      }
      
      form.experience = response.experience || ''
      form.expectedSalary = response.expectedSalary || null
      
      if (response.workExperience) {
        try {
          const arr = JSON.parse(response.workExperience)
          form.workExperience = Array.isArray(arr) ? arr.map(exp => ({
            ...exp,
            startDate: normalizeDate(exp.startDate),
            endDate: exp.endDate === '至今' || exp.current ? '' : normalizeDate(exp.endDate),
            current: exp.current === true || exp.endDate === '至今' || !exp.endDate
          })) : []
        } catch {
          form.workExperience = []
        }
      } else {
        form.workExperience = []
      }
      
      if (response.fileUrl) {
        const parts = response.fileUrl.split('/')
        uploadedFile.value = parts[parts.length - 1] || '已导入附件'
      }
      
      if (response.aiAnalysis) {
        aiResult.value = response.aiAnalysis
      }

      form.selfIntroduction = response.selfIntroduction || ''
      
      ElMessage.success('简历智能导入成功，请检查并完善信息')
      fetchResumeList()
    } else {
      ElMessage.warning('导入成功，但未获取到解析数据')
    }
  } catch (error) {
    importing.value = false
    console.error('导入失败:', error)
    ElMessage.error('导入失败，请检查文件格式或重试')
  }
}

const onUploadSuccess = (response, file, fileList) => {
  console.log('[上传成功] response:', response)
  
  if (!response) {
    console.error('[上传成功] response 为空')
    ElMessage.error('上传失败：未获取到响应数据')
    return
  }
  
  if (response.id) {
    resumeId.value = response.id
    if (response.name) form.name = response.name
    if (response.phone) form.phone = response.phone
    if (response.email) form.email = response.email
    if (response.age) form.age = response.age
    if (response.education) form.education = response.education
    if (response.experience) form.experience = response.experience
    if (response.selfIntroduction) form.selfIntroduction = response.selfIntroduction
    
    if (response.skills) {
      try {
        const arr = JSON.parse(response.skills)
        form.skills = Array.isArray(arr) ? arr.join(', ') : response.skills
      } catch {
        form.skills = response.skills
      }
    }
    
    if (response.workExperience) {
      try {
        const arr = JSON.parse(response.workExperience)
        form.workExperience = Array.isArray(arr) ? arr.map(exp => ({
          ...exp,
          startDate: normalizeDate(exp.startDate),
          endDate: exp.endDate === '至今' || exp.current ? '' : normalizeDate(exp.endDate),
          current: exp.current === true || exp.endDate === '至今' || !exp.endDate
        })) : []
      } catch {
        form.workExperience = []
      }
    }
    
    if (response.fileUrl) {
      const parts = response.fileUrl.split('/')
      uploadedFile.value = parts[parts.length - 1] || '已上传附件'
    }
    
    ElMessage.success(response.message || '上传成功')

    if (response.aiAnalysis) {
      aiResult.value = response.aiAnalysis
    }
    fetchResumeList()
  } else {
    console.error('[上传成功] response.id 不存在', response)
    ElMessage.error('上传失败：未获取到简历ID')
  }
}

const onUploadError = () => {
  ElMessage.error('上传失败，请重试')
}

const onImportSuccess = (response) => {
  importing.value = false
  if (response && response.id) {
    resumeId.value = response.id
    form.name = response.name || ''
    form.age = response.age || 25
    form.education = response.education || ''
    form.categoryId = response.categoryId || null
    if (response.skills) {
      try {
        const arr = JSON.parse(response.skills)
        form.skills = Array.isArray(arr) ? arr.join(', ') : response.skills
      } catch {
        form.skills = response.skills
      }
    }
    form.experience = response.experience || ''
    form.expectedSalary = response.expectedSalary || null
    if (response.workExperience) {
      try {
        const arr = JSON.parse(response.workExperience)
        form.workExperience = Array.isArray(arr) ? arr : []
      } catch {
        form.workExperience = []
      }
    }
    form.selfIntroduction = response.selfIntroduction || ''
    if (response.fileUrl) {
      const parts = response.fileUrl.split('/')
      uploadedFile.value = parts[parts.length - 1] || '已导入附件'
    }
    ElMessage.success('简历智能导入成功，请检查并完善信息')
  }
}

const onImportError = () => {
  importing.value = false
  ElMessage.error('导入失败，请检查文件格式或重试')
}

const fetchMyResume = async () => {
  try {
    // 如果是查看模式，通过ID获取简历
    const targetId = route.params.id || route.query.id

    let resumeData = null
    if (isViewMode.value && targetId) {
      try {
        // 先尝试按简历ID获取
        resumeData = await request.get(`/resume/${targetId}`, { skipNotFoundNotification: true })
      } catch (e) {
        // 404时尝试按用户ID获取（从聊天页跳转时可能是用户ID）
        if (e.response?.status === 404) {
          try {
            resumeData = await request.get(`/resume/user/${targetId}`)
          } catch { /* ignore */ }
        }
      }
    } else {
      resumeData = await request.get('/resume/my')
    }

    if (resumeData) {
      resumeId.value = resumeData.id
      fillFormFromResume(resumeData)
    } else if (isViewMode.value) {
      ElMessage.error('无法加载简历详情：简历不存在或已被删除')
    }
  } catch (error) {
    if (isViewMode.value) {
      ElMessage.error('无法加载简历详情：' + (error.response?.data?.error || error.message || '权限不足或简历不存在'))
    }
    // 404 — 还没有简历，保持空表单
  }
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = buildPayload()

    if (resumeId.value) {
      await request.put(`/resume/${resumeId.value}`, payload)
      ElMessage.success('简历已更新')
    } else {
      const res = await request.post('/resume', payload)
      resumeId.value = res.id
      ElMessage.success('简历已保存')
    }
    await fetchResumeList()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}

const addWorkExp = () => {
  form.workExperience.push({
    company: '',
    position: '',
    startDate: '',
    endDate: '',
    current: false,
    description: ''
  })
}

const removeWorkExp = (index) => {
  form.workExperience.splice(index, 1)
}

const aiAnalyze = async () => {
  if (!resumeId.value) {
    ElMessage.warning('请先保存简历')
    return
  }
  aiLoading.value = true
  try {
    const res = await request.post(`/resume/${resumeId.value}/ai-analyze`)
    const analysis = res.aiAnalysis || JSON.stringify(res, null, 2)
    aiResult.value = analysis
    saveAiToCache(resumeId.value, analysis)
    ElMessage.success('AI 分析完成')
    await nextTick()
    renderRadarChart()
  } catch {
    ElMessage.error('AI 分析失败')
  } finally {
    aiLoading.value = false
  }
}

// 监听分析结果变化，渲染雷达图
watch(aiResult, async (val) => {
  if (val) {
    // 等待DOM更新完成，再渲染雷达图
    await nextTick()
    await nextTick()
    setTimeout(() => {
      renderRadarChart()
      setTimeout(() => {
        if (radarChart) radarChart.resize()
      }, 100)
    }, 100)
  }
})

const normalizeDate = (dateStr) => {
  if (!dateStr) return ''
  const str = String(dateStr)
  const match = str.match(/(\d{4})-(\d{1,2})/)
  if (match) {
    const year = match[1]
    const month = match[2].padStart(2, '0')
    return `${year}-${month}`
  }
  return str
}

const handleReset = () => {
  form.name = ''
  form.phone = ''
  form.email = ''
  form.age = 25
  form.education = ''
  form.skills = ''
  form.experience = ''
  form.expectedSalary = null
  form.workExperience = []
  form.selfIntroduction = ''
}

// 导出PDF
const handleExportPdf = async () => {
  exporting.value = true
  try {
    await exportResumeToPdf({
      name: form.name,
      age: form.age,
      phone: form.phone,
      email: form.email,
      education: form.education,
      skills: form.skills,
      experience: form.experience,
      expectedSalary: form.expectedSalary,
      workExperience: form.workExperience,
      selfIntroduction: form.selfIntroduction
    })
    ElMessage.success('PDF导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('PDF导出失败，请重试')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  fetchCategories()
  fetchMyResume()
  if (!isViewMode.value) {
    fetchResumeList()
  }
})

// 监听路由变化，重新加载简历数据
watch(() => route.params.id, (newId) => {
  if (newId && isViewMode.value) {
    console.log('[MyResume] 路由参数变化，重新加载简历:', newId)
    fetchMyResume()
  }
})

watch(() => route.query.id, (newId) => {
  if (newId && isViewMode.value) {
    console.log('[MyResume] 路由Query变化，重新加载简历:', newId)
    fetchMyResume()
  }
})
</script>

<style scoped>
/* ── 页面容器 ─────────────────────────────── */
.my-resume-page {
  padding: var(--space-6);
  background: var(--color-bg);
  min-height: 100vh;
  max-width: 1600px;
  margin: 0 auto;
}

/* ── 页面头部 ─────────────────────────────── */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
  padding: var(--space-5);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
}

.header-content {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.header-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-lg);
  background: var(--primary-50);
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-text h2 {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--gray-900);
  margin: 0 0 var(--space-1);
}

.header-desc {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: var(--space-2);
}

.action-btn {
  min-width: 90px;
}

.action-btn.secondary {
  border-color: var(--color-border);
  color: var(--gray-700);
}

.action-btn.primary {
  font-weight: var(--weight-medium);
}

.action-btn.ai-btn {
  background: linear-gradient(135deg, #8b5cf6 0%, #6366f1 100%);
  border-color: transparent;
}

.action-btn.ai-btn:hover {
  background: linear-gradient(135deg, #a78bfa 0%, #818cf8 100%);
}

/* ── 简历切换器 ───────────────────────────── */
.resume-switcher {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-6);
  padding: var(--space-4);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.switcher-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding-right: var(--space-4);
  border-right: 1px solid var(--color-border);
  flex-shrink: 0;
}

.switcher-title {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--gray-700);
}

.switcher-count {
  font-size: var(--text-xs);
  color: var(--gray-500);
  background: var(--gray-100);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}

.switcher-list {
  display: flex;
  gap: var(--space-3);
  overflow-x: auto;
  flex: 1;
  padding-bottom: 4px;
}

.switcher-list::-webkit-scrollbar {
  height: 4px;
}

.switcher-list::-webkit-scrollbar-thumb {
  background: var(--gray-300);
  border-radius: 2px;
}

.resume-card {
  flex-shrink: 0;
  min-width: 220px;
  padding: var(--space-3);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-fast);
  background: var(--gray-50);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.resume-card:hover {
  border-color: var(--primary-300);
  background: var(--primary-50);
}

.resume-card.active {
  border-color: var(--primary-500);
  background: var(--primary-50);
  box-shadow: var(--shadow-sm);
}

.resume-card.default {
  border-color: var(--success-400);
  background: var(--success-50);
}

.resume-card.default.active {
  border-color: var(--primary-500);
  background: var(--primary-50);
}

.card-main {
  flex: 1;
  min-width: 0;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: 4px;
}

.card-name {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-left: var(--space-2);
}

.card-action-btn {
  font-size: var(--text-xs);
  padding: 2px 6px;
  color: var(--gray-500);
}

.card-action-btn:hover {
  color: var(--primary-600);
}

.card-action-btn.delete:hover {
  color: var(--danger-600);
}

.add-resume-btn {
  flex-shrink: 0;
  background: var(--primary-500);
  color: #fff;
}

.add-resume-btn:hover {
  background: var(--primary-600);
}

/* ── 卡片样式 ─────────────────────────────── */
.form-card,
.upload-card,
.ai-cards-container {
  border-radius: 8px;
  margin-bottom: 24px;
  border: 1px solid #e8e8e8;
  background: #ffffff;
}

.card-header {
  display: flex;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.form-card :deep(.el-card__header),
.upload-card :deep(.el-card__header) {
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  padding: 16px 24px;
}

.form-card :deep(.el-card__body) {
  padding: 24px;
}

.upload-card :deep(.el-card__body) {
  padding: 24px;
}

/* ── AI 分析卡片 ─────────────────────────────── */
.ai-cards-container :deep(.el-card__body) {
  padding: 0;
}

.ai-cards-container :deep(.el-card__header) {
  padding: 0;
  border: none;
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 24px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a855f7 100%);
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  border-radius: 16px 16px 0 0;
}

.ai-header-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ── 分数横幅 ─────────────────────────────── */
.score-banner {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #fafafe 0%, #ffffff 100%);
  border-bottom: 1px solid #f0f0f5;
}

.score-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 3px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fff;
  flex-shrink: 0;
  box-shadow: 0 2px 12px rgba(99,102,241,0.1);
}

.score-num {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.score-unit {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.score-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

/* ── 雷达图 ─────────────────────────────── */
.radar-section {
  padding: 16px 24px;
  border-bottom: 1px solid #f1f5f9;
}

.radar-chart {
  width: 100%;
  height: 260px;
}

/* ── 维度评分明细 ─────────────────────────────── */
.dim-details {
  padding: 16px 24px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dim-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #f1f5f9;
}

.dim-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.dim-name {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.dim-score {
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
}

.sub-scores {
  margin-top: 8px;
}

.sub-row {
  display: grid;
  grid-template-columns: 70px 40px 1fr;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.sub-row:last-child { border-bottom: none; }
.sub-name { color: #475569; font-weight: 500; }
.sub-score { color: #64748b; text-align: center; }
.sub-comment { color: #94a3b8; text-align: right; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ── 折叠面板 ─────────────────────────────── */
.ai-collapse {
  border: none;
}

.ai-collapse :deep(.el-collapse-item__header) {
  background: #ffffff;
  padding: 0 24px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
  height: 44px;
  line-height: 44px;
}

.ai-collapse :deep(.el-collapse-item__header:hover) {
  background: #fafafe;
  color: #6366f1;
}

.ai-collapse :deep(.el-collapse-item__content) {
  padding: 16px 24px;
  background: #ffffff;
}

.ai-collapse :deep(.el-collapse-item__wrap) {
  border-bottom: 1px solid #f1f5f9;
}

.collapse-title {
  font-size: 13px;
  font-weight: 600;
}

/* ── 优势与不足 ─────────────────────────────── */
.sw-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.sw-col-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 10px;
}

.strengths-col .sw-col-header {
  color: #10b981;
}

.weaknesses-col .sw-col-header {
  color: #f59e0b;
}

.sw-list {
  margin: 0;
  padding-left: 16px;
  font-size: 13px;
  color: #475569;
  line-height: 1.8;
}

.sw-list li {
  margin-bottom: 4px;
}

.strengths-col .sw-list {
  list-style: none;
  padding-left: 0;
}

.strengths-col .sw-list li::before {
  content: '✓';
  color: #10b981;
  font-weight: 700;
  margin-right: 6px;
}

.weaknesses-col .sw-list {
  list-style: none;
  padding-left: 0;
}

.weaknesses-col .sw-list li::before {
  content: '!';
  color: #f59e0b;
  font-weight: 700;
  margin-right: 6px;
}

/* ── 竞争力分析 ─────────────────────────────── */
.competitive-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comp-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.comp-label {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.comp-value {
  font-size: 13px;
  color: #334155;
  line-height: 1.5;
}

/* ── 职业建议 ─────────────────────────────── */
.career-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.career-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.career-tag {
  padding: 4px 12px;
  background: linear-gradient(135deg, #ede9fe 0%, #e0e7ff 100%);
  color: #6366f1;
  font-size: 12px;
  font-weight: 500;
  border-radius: 16px;
}

.career-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.career-row {
  display: flex;
  gap: 8px;
  font-size: 13px;
}

.career-label {
  color: #94a3b8;
  flex-shrink: 0;
  min-width: 60px;
}

.career-value {
  color: #334155;
}

/* ── 学习计划 ─────────────────────────────── */
.learning-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.learning-group-title {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.learning-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
  border-left: 3px solid #6366f1;
}

.learning-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.learning-skill {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.learning-priority {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.learning-priority.high {
  background: #fef2f2;
  color: #ef4444;
}

.learning-priority.medium {
  background: #fffbeb;
  color: #f59e0b;
}

.learning-desc {
  font-size: 12px;
  color: #64748b;
  margin: 0;
  line-height: 1.5;
}

/* ── 面试建议 ─────────────────────────────── */
.interview-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.interview-title {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.interview-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.interview-chip {
  padding: 4px 10px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  border-radius: 12px;
}

.interview-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.interview-q {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}

.q-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #6366f1;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 1px;
}

.q-text {
  flex: 1;
}

/* ── 综合评价 ─────────────────────────────── */
.overall-comment {
  font-size: 13px;
  color: #475569;
  line-height: 1.7;
  margin: 0;
  background: linear-gradient(135deg, #fafafe 0%, #f8fafc 100%);
  padding: 14px 16px;
  border-radius: 8px;
  border-left: 3px solid #6366f1;
}

/* ── 上传区域 ─────────────────────────────── */
.resume-upload {
  width: 100%;
}

.resume-upload :deep(.el-upload) {
  width: 100%;
}

.resume-upload :deep(.el-upload-dragger) {
  padding: 40px 32px;
  border: 2px dashed #cbd5e1;
  border-radius: 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.resume-upload :deep(.el-upload-dragger:hover) {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #ffffff 100%);
}

.upload-icon {
  color: #94a3b8;
  margin-bottom: 16px;
}

.upload-text {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upload-main {
  font-size: 16px;
  font-weight: 500;
  color: #334155;
}

.upload-hint {
  font-size: 14px;
  color: #94a3b8;
}

.upload-tip {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 16px;
  text-align: center;
}

.uploaded-file {
  margin-top: 20px;
}

.import-upload {
  width: 100%;
}

.import-btn {
  width: 100%;
  border-radius: 12px;
}

.import-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 13px;
  color: #94a3b8;
}

/* ── 工作经历 ─────────────────────────────── */
.work-exp-item {
  background: linear-gradient(135deg, #fafbfc 0%, #ffffff 100%);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
}

.work-exp-item:hover {
  border-color: #cbd5e1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.work-exp-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.work-exp-number {
  font-weight: 600;
  color: #64748b;
  font-size: 14px;
}

.delete-btn {
  color: #ef4444;
}

.delete-btn:hover {
  color: #dc2626;
}

.add-work-btn {
  width: 100%;
  border-radius: 12px;
  border-style: dashed;
  border-width: 2px;
  color: #64748b !important;
  font-weight: 500;
  transition: all 0.3s ease;
}

.add-work-btn:hover {
  color: #0f172a !important;
  border-color: #cbd5e1 !important;
  background: #f8fafc !important;
}

/* ── 表单操作 ────────────────────────────── */
.form-actions {
  display: flex;
  gap: 12px;
  padding-top: 20px;
  align-items: center;
}

.form-actions .el-button {
  border-radius: 12px;
  font-weight: 500;
  padding: 12px 24px;
}

/* ── 模板选择 ─────────────────────────────── */
.template-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.template-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  background: #ffffff;
  transition: all 0.3s ease;
}

.template-item:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
  transform: translateX(4px);
}

.template-item.active {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #ffffff 100%);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
}

.template-icon {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  border-radius: 12px;
}

.template-info {
  flex: 1;
  min-width: 0;
}

.template-info h4 {
  margin: 0 0 4px 0;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.template-info p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.check-icon {
  flex-shrink: 0;
  font-size: 20px;
}

/* ── 表单提示 ─────────────────────────────── */
.form-tip {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 8px;
  line-height: 1.5;
}

/* ── 分隔线 ─────────────────────────────── */
.divider-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.divider-subtitle {
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
}

.section-divider :deep(.el-divider__text) {
  background: #ffffff;
  padding: 0 16px;
  color: #334155;
  font-weight: 600;
}

.section-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
  border-radius: 8px;
  font-size: 13px;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.section-tip .el-icon {
  font-size: 16px;
  flex-shrink: 0;
  color: #94a3b8;
}

/* ── 模板预览 ─────────────────────────────── */
.template-preview {
  margin-top: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.template-preview h4 {
  margin: 0 0 12px 0;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.preview-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-item {
  display: flex;
  gap: 8px;
  font-size: 13px;
}

.preview-label {
  color: #64748b;
  font-weight: 500;
  min-width: 70px;
  flex-shrink: 0;
}

.preview-value {
  color: #475569;
  flex: 1;
  line-height: 1.5;
}

/* ── Element UI 覆盖 ─────────────────────────────── */
.upload-card :deep(.el-divider__text) {
  background: #ffffff;
  padding: 0 12px;
}

/* ── 滚动条美化 ─────────────────────────────── */
.template-list::-webkit-scrollbar {
  width: 6px;
}

.template-list::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.template-list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.template-list::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* ── 表单控件样式 ─────────────────────────────── */
.form-card :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  margin-bottom: 8px;
}

.form-card :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  transition: all 0.3s ease;
}

.form-card :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #cbd5e1 inset;
}

.form-card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #3b82f6 inset;
}

.form-card :deep(.el-textarea__inner) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  transition: all 0.3s ease;
}

.form-card :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #cbd5e1 inset;
}

.form-card :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #3b82f6 inset;
}

.form-card :deep(.el-select .el-input__wrapper) {
  border-radius: 10px;
}

.form-card :deep(.el-input-number__wrapper) {
  border-radius: 10px;
}

.form-card :deep(.el-date-editor .el-input__wrapper) {
  border-radius: 10px;
}

.form-card :deep(.el-checkbox__inner) {
  border-radius: 4px;
}

/* ── 页面过渡动画 ─────────────────────────────── */
.page-enter-active,
.page-leave-active {
  transition: all 0.3s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}
</style>
