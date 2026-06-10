<template>
  <div class="match-page">
    <!-- 标签页 + 排序 -->
    <el-tabs v-model="activeTab" class="match-tabs">
      <template #nav-right>
        <div class="sort-bar" v-if="(activeTab === 'jobseeker' && matchResults.length > 0) || (activeTab === 'employer' && candidateResults.length > 0)">
          <el-radio-group v-model="sortMode" size="small">
            <el-radio-button label="composite">综合推荐</el-radio-button>
            <el-radio-button label="score">匹配得分</el-radio-button>
            <el-radio-button label="freshness">最新发布</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <!-- 求职匹配 -->
      <el-tab-pane label="求职匹配" name="jobseeker" v-if="userStore.isEmployee">
        <div class="match-content">
          <!-- 简历概要 -->
          <div class="resume-section">
            <div v-if="selectedResume" class="resume-summary">
              <div class="resume-info">
                <div class="resume-name">{{ selectedResume.name }}</div>
                <div class="resume-meta">
                  <span v-if="selectedResume.age">{{ selectedResume.age }}岁</span>
                  <span v-if="selectedResume.education">{{ selectedResume.education }}</span>
                  <span v-if="selectedResume.expectedSalary">期望 ¥{{ selectedResume.expectedSalary?.toLocaleString() }}</span>
                </div>
                <div class="resume-skills">
                  <el-tag v-for="skill in parseSkills(selectedResume.skills).slice(0, 8)" :key="skill" size="small" effect="plain">
                    {{ skill }}
                  </el-tag>
                  <span v-if="parseSkills(selectedResume.skills).length > 8" class="more-skills">+{{ parseSkills(selectedResume.skills).length - 8 }}</span>
                </div>
              </div>
              <div class="resume-actions">
                <el-button type="primary" @click="startJobMatching(false)" :loading="matching">
                  <el-icon v-if="!matching"><MagicStick /></el-icon>
                  <el-icon v-else class="is-loading"><Loading /></el-icon>
                  {{ matching ? '匹配中...' : '开始匹配' }}
                </el-button>
                <el-button @click="startJobMatching(true)" :loading="matching" plain>
                  <el-icon><Refresh /></el-icon>强制刷新
                </el-button>
                <el-button text size="small" @click="loadMyRefresh">
                  <el-icon><Refresh /></el-icon>刷新简历
                </el-button>
                <span v-if="matchFromCache" class="cache-tip">
                  <el-icon><Clock /></el-icon>来自缓存
                </span>
              </div>
            </div>
            <el-empty v-else description="暂无简历，请先创建简历" :image-size="80">
              <el-button type="primary" @click="$router.push('/my-resume')">创建简历</el-button>
            </el-empty>
          </div>

          <!-- 匹配结果 -->
          <div v-if="matchResults.length > 0" class="results-section">
            <div class="results-header">
              <span class="results-title">推荐职位（{{ matchResults.length }}）</span>
              <el-button text size="small" @click="openBlockedDialog">
                <el-icon><Hide /></el-icon> 已屏蔽（{{ blockedJobs.length }}）
              </el-button>
            </div>
            <div class="results-list">
              <div v-for="result in sortedResults" :key="result.job?.id" class="result-item"
                   :class="{ 'low-match': result.matchScore < 50, 'hard-filtered': result.matchDetail?.hardFilterBlocked }">
                <div v-if="result.matchDetail?.hardFilterBlocked" class="hard-filter-badge">
                  <el-icon><Warning /></el-icon> 存在硬性差距
                </div>
                <div class="result-body">
                  <div class="result-score">
                    <el-progress
                      type="circle"
                      :percentage="Number(result.matchScore) || 0"
                      :color="getScoreColor(Number(result.matchScore))"
                      :width="56"
                      :stroke-width="5"
                    />
                  </div>
                  <div class="result-info">
                    <div class="result-title-row">
                      <h4 @click="viewJobDetail(result.job)">{{ result.job?.title }}</h4>
                      <el-tag v-if="isFreshJob(result.job)" type="success" size="small" effect="plain">新鲜</el-tag>
                      <el-tag v-if="result.matchDetail?.hardFilterBlocked" type="danger" size="small" effect="plain">硬性不符</el-tag>
                    </div>
                    <div class="result-meta">
                      <span><el-icon><Location /></el-icon>{{ result.job?.location }}</span>
                      <span><el-icon><Money /></el-icon>¥{{ result.job?.salaryMin?.toLocaleString() }} - ¥{{ result.job?.salaryMax?.toLocaleString() }}</span>
                      <span>{{ result.job?.experienceRequired }}</span>
                      <span>{{ result.job?.educationRequired }}</span>
                    </div>
                    <p class="result-suggestion" v-if="result.aiSuggestion">
                      <el-icon><Promotion /></el-icon> {{ result.aiSuggestion }}
                    </p>
                    <div v-if="result.matchDetail" class="dim-scores">
                      <div class="dim-item" v-for="dim in dimensionLabels" :key="dim.key">
                        <span class="dim-label">{{ dim.label }}</span>
                        <el-progress :percentage="Math.round((result.matchDetail[dim.key] || 0) / dim.max * 100)" :stroke-width="3" :show-text="false" :color="getDimColor(result.matchDetail[dim.key] || 0, dim.max)" />
                        <span class="dim-val">{{ result.matchDetail[dim.key] || 0 }}/{{ dim.max }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="result-actions">
                    <el-button size="small" @click="viewJobComparison(result)">对照</el-button>
                    <el-button type="primary" size="small" @click="viewJobDetail(result.job)">详情</el-button>
                    <el-button size="small" type="danger" plain @click="blockJob(result.job)">屏蔽</el-button>
                    <el-button size="small" type="warning" plain @click="openReportDialog(4, result.job.id, result.job.title)"><el-icon><WarnTriangleFilled /></el-icon> 举报</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 招聘匹配 -->
      <el-tab-pane label="招聘匹配" name="employer" v-if="userStore.isEmployer || userStore.isAdmin">
        <div class="match-content">
          <!-- 职位选择 -->
          <div class="job-select-section">
            <el-select
              v-model="selectedJobId"
              placeholder="选择职位开始匹配候选人..."
              filterable
              style="width: 100%"
              :loading="matching"
              @change="loadJobMatch"
              size="large"
            >
              <el-option
                v-for="job in jobList"
                :key="job.id"
                :label="job.title + ' — ' + job.location"
                :value="job.id"
              />
            </el-select>
            <div v-if="matching" class="matching-tip">
              <el-icon class="is-loading"><Loading /></el-icon>
              正在匹配候选人，请稍候...
            </div>
            <div v-if="candidateFromCache && candidateResults.length > 0" class="cache-line">
              <span class="cache-tip"><el-icon><Clock /></el-icon> 来自缓存</span>
              <el-button size="small" text @click="loadJobMatch(selectedJobId, true)">
                <el-icon><Refresh /></el-icon>强制刷新
              </el-button>
            </div>
          </div>

          <!-- 候选人结果 -->
          <div v-if="candidateResults.length > 0" class="results-section">
            <div class="results-header">
              <span class="results-title">推荐候选人（{{ candidateResults.length }}）</span>
            </div>
            <div class="results-list">
              <div v-for="result in sortedCandidateResults" :key="result.resume?.id" class="result-item">
                <div class="result-body">
                  <div class="result-score">
                    <el-progress
                      type="circle"
                      :percentage="Number(result.matchScore) || 0"
                      :color="getScoreColor(Number(result.matchScore))"
                      :width="56"
                      :stroke-width="5"
                    />
                  </div>
                  <div class="result-info">
                    <h4>{{ result.resume?.name }}</h4>
                    <div class="result-meta">
                      <span>{{ result.resume?.age }}岁</span>
                      <span>{{ result.resume?.education }}</span>
                      <span>期望 ¥{{ result.resume?.expectedSalary?.toLocaleString() }}</span>
                    </div>
                    <div class="result-skills">
                      <el-tag v-for="skill in parseSkills(result.resume?.skills).slice(0, 6)" :key="skill" size="small" effect="plain">{{ skill }}</el-tag>
                    </div>
                    <p class="result-suggestion" v-if="result.aiSuggestion">
                      <el-icon><Promotion /></el-icon> {{ result.aiSuggestion }}
                    </p>
                    <div v-if="result.matchDetail" class="dim-scores">
                      <div class="dim-item" v-for="dim in dimensionLabels" :key="dim.key">
                        <span class="dim-label">{{ dim.label }}</span>
                        <el-progress :percentage="Math.round((result.matchDetail[dim.key] || 0) / dim.max * 100)" :stroke-width="3" :show-text="false" :color="getDimColor(result.matchDetail[dim.key] || 0, dim.max)" />
                        <span class="dim-val">{{ result.matchDetail[dim.key] || 0 }}/{{ dim.max }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="result-actions">
                    <el-button size="small" @click="viewComparison(result)">对比</el-button>
                    <el-button type="primary" size="small" @click="inviteCandidateDirect(result.resume)">邀请面试</el-button>
                    <el-button size="small" type="warning" plain @click="openReportDialog(3, result.resume?.userId, result.resume?.name)"><el-icon><WarnTriangleFilled /></el-icon> 举报</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <el-empty v-if="selectedJobId && candidateResults.length === 0 && !matching" description="暂无匹配结果" :image-size="80">
            <p style="color: var(--gray-400); font-size: 13px;">AI 将从简历库中为您推荐合适的候选人</p>
          </el-empty>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Job detail dialog -->
    <el-dialog v-model="jobDetailVisible" title="职位详情" width="600px">
      <el-descriptions :column="1" border v-if="currentJob">
        <el-descriptions-item label="职位名称">{{ currentJob.title }}</el-descriptions-item>
        <el-descriptions-item label="工作地点">{{ currentJob.location }}</el-descriptions-item>
        <el-descriptions-item label="薪资范围">
          ¥{{ currentJob.salaryMin?.toLocaleString() }} - ¥{{ currentJob.salaryMax?.toLocaleString() }}
        </el-descriptions-item>
        <el-descriptions-item label="经验要求">{{ currentJob.experienceRequired }}</el-descriptions-item>
        <el-descriptions-item label="学历要求">{{ currentJob.educationRequired }}</el-descriptions-item>
        <el-descriptions-item label="职位描述">{{ currentJob.description }}</el-descriptions-item>
        <el-descriptions-item label="任职要求">{{ currentJob.requirements }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- Interview invitation dialog -->
    <el-dialog v-model="interviewVisible" title="邀请候选人面试" width="500px" destroy-on-close>
      <div v-if="invitingCandidate" class="invite-info">
        <p><strong>候选人：</strong>{{ invitingCandidate.name }}</p>
        <p><strong>应聘职位：</strong>{{ currentJob?.title }}</p>
      </div>
      <el-form :model="interviewForm" label-width="100px" style="margin-top: 16px">
        <el-form-item label="面试时间">
          <el-date-picker
            v-model="interviewForm.interviewTime"
            type="datetime"
            placeholder="选择面试时间"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="面试地点">
          <el-input v-model="interviewForm.location" placeholder="请输入面试地点" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="interviewForm.remark"
            type="textarea"
            :rows="3"
            placeholder="可选：面试注意事项等"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="interviewVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmInvite" :loading="inviting">确认邀请</el-button>
      </template>
    </el-dialog>

    <!-- 投递对话框 -->
    <el-dialog v-model="applyDialogVisible" title="投递简历" width="500px">
      <div class="apply-job-info">
        <h4>{{ comparisonData?.job?.title }}</h4>
        <p>{{ comparisonData?.job?.location }} | ¥{{ comparisonData?.job?.salaryMin?.toLocaleString() }} - ¥{{ comparisonData?.job?.salaryMax?.toLocaleString() }}</p>
      </div>
      <el-form label-width="80px">
        <el-form-item label="选择简历" required>
          <el-select v-model="selectedResumeIdForApply" placeholder="请选择要投递的简历" style="width: 100%" :loading="loadingResumes">
            <el-option
              v-for="resume in myResumes"
              :key="resume.id"
              :label="resume.resumeName || resume.name + '的简历'"
              :value="resume.id"
            >
              <span>{{ resume.resumeName || resume.name + '的简历' }}</span>
              <el-tag v-if="resume.isDefault === 1" type="success" size="small" style="margin-left: 8px">默认</el-tag>
            </el-option>
          </el-select>
          <div class="form-tip" v-if="myResumes.length === 0">
            暂无简历，请先<router-link to="/my-resume">创建简历</router-link>
          </div>
        </el-form-item>
        <el-form-item label="求职信">
          <el-input v-model="coverLetter" type="textarea" :rows="3" placeholder="请输入求职信（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmApply" :loading="applying" :disabled="!selectedResumeIdForApply">确认投递</el-button>
      </template>
    </el-dialog>

    <!-- Comparison dialog -->
    <el-dialog v-model="comparisonVisible" :title="userStore.isEmployee ? '简历与岗位对照分析' : '候选人对照分析'" width="880px" destroy-on-close>
      <div v-if="comparisonData && comparisonDetail" class="comparison-content">
        <!-- Score overview -->
        <div class="score-overview">
          <div class="score-circle-large" :style="{ borderColor: getScoreColor(comparisonDetail.total || 0) }">
            <span class="score-num-large" :style="{ color: getScoreColor(comparisonDetail.total || 0) }">{{ comparisonDetail.total ?? comparisonData.matchScore }}</span>
            <span class="score-unit-large">/ 100</span>
          </div>
          <div class="score-breakdown">
            <div class="breakdown-item"><span class="break-label">技能</span><span class="break-value">{{ comparisonDetail.skill ?? 0 }}/50</span></div>
            <div class="breakdown-item"><span class="break-label">行业</span><span class="break-value">{{ comparisonDetail.industryScore ?? 0 }}/15</span></div>
            <div class="breakdown-item"><span class="break-label">经验</span><span class="break-value">{{ comparisonDetail.experienceScore ?? 0 }}/20</span></div>
            <div class="breakdown-item"><span class="break-label">学历</span><span class="break-value">{{ comparisonDetail.educationScore ?? 0 }}/10</span></div>
            <div class="breakdown-item" v-if="comparisonDetail.salaryAdjustment"><span class="break-label">薪资微调</span><span class="break-value" :class="comparisonDetail.salaryAdjustment > 0 ? 'bonus-value' : 'penalty-value'">{{ comparisonDetail.salaryAdjustment > 0 ? '+' : '' }}{{ comparisonDetail.salaryAdjustment }}</span></div>
            <div class="breakdown-item" v-if="comparisonDetail.locationAdjustment"><span class="break-label">地点微调</span><span class="break-value" :class="comparisonDetail.locationAdjustment > 0 ? 'bonus-value' : 'penalty-value'">{{ comparisonDetail.locationAdjustment > 0 ? '+' : '' }}{{ comparisonDetail.locationAdjustment }}</span></div>
            <div class="breakdown-item" v-if="comparisonDetail.bonusScore > 0"><span class="break-label">加分项</span><span class="break-value bonus-value">+{{ comparisonDetail.bonusScore }}/10</span></div>
          </div>
        </div>

        <!-- Hard filter warning -->
        <el-alert v-if="comparisonDetail.hardFilterBlocked"
          :title="'存在硬性差距：' + (comparisonDetail.hardFilterReasons?.join('；') || '')"
          type="warning" :closable="false" show-icon style="margin-bottom: 16px" />

        <!-- Comparison table -->
        <div class="comparison-table">
          <div class="comp-row header-row">
            <div class="comp-col dim-col">维度</div>
            <div class="comp-col job-col">职位要求</div>
            <div class="comp-col resume-col">{{ userStore.isEmployee ? '您的简历' : '候选人' }}</div>
            <div class="comp-col score-col">得分</div>
          </div>

          <!-- Skills -->
          <div class="comp-row skills-row">
            <div class="comp-col dim-col">
              <span class="dim-icon">🎯</span>技能
            </div>
            <div class="comp-col job-col">
              <div class="skills-list">
                <el-tag v-for="skill in comparisonDetail.skillDetail?.jobSkills" :key="skill" size="small"
                  :type="isCandidateHasSkill(skill) ? 'success' : 'danger'"
                  :effect="isCandidateHasSkill(skill) ? 'dark' : 'plain'">{{ skill }}</el-tag>
              </div>
            </div>
            <div class="comp-col resume-col">
              <div class="skills-list">
                <el-tag v-for="skill in candidateSkills" :key="skill" size="small"
                  :type="isSkillMatch(skill) ? 'success' : 'info'"
                  :effect="isSkillMatch(skill) ? 'dark' : 'plain'">{{ skill }}</el-tag>
              </div>
              <div v-if="getMissingSkills().length > 0" class="missing-skills">
                <span class="missing-label">缺失：</span>
                <el-tag v-for="missing in getMissingSkills()" :key="missing" size="small" type="danger" effect="plain" class="missing-tag">{{ missing }}</el-tag>
              </div>
              <div class="skill-coverage-bar">
                <el-progress :percentage="comparisonDetail.skillDetail?.coverage || 0" :color="getCoverageColor(comparisonDetail.skillDetail?.coverage)" :stroke-width="6" />
                <span class="coverage-text">覆盖率 {{ comparisonDetail.skillDetail?.coverage }}%</span>
              </div>
            </div>
            <div class="comp-col score-col">
              <span class="score-pill" :style="{ background: getScoreColor(comparisonDetail.skill ?? 0) }">{{ comparisonDetail.skill ?? 0 }}/50</span>
            </div>
          </div>

          <!-- Industry -->
          <div class="comp-row" :class="{ 'row-matched': comparisonDetail.comparison?.industry?.sameIndustry, 'row-mismatched': comparisonDetail.comparison?.industry?.crossIndustry }">
            <div class="comp-col dim-col">
              <span class="dim-icon">🏢</span>行业
            </div>
            <div class="comp-col job-col">{{ categoryNameMap[comparisonData.job?.categoryId] || '未指定' }}</div>
            <div class="comp-col resume-col">
              {{ categoryNameMap[comparisonData.resume?.categoryId] || '未指定' }}
              <el-tag v-if="comparisonDetail.comparison?.industry?.sameIndustry" type="success" size="small">同行业</el-tag>
              <el-tag v-else-if="comparisonDetail.comparison?.industry?.relatedIndustry" type="warning" size="small">相关行业</el-tag>
              <el-tag v-else-if="comparisonDetail.comparison?.industry?.crossIndustry" type="danger" size="small">跨行业</el-tag>
            </div>
            <div class="comp-col score-col"><span class="score-pill" :style="{ background: getScoreColor(comparisonDetail.industryScore ?? 0) }">{{ comparisonDetail.industryScore ?? 0 }}/15</span></div>
          </div>

          <!-- Experience -->
          <div class="comp-row" :class="{ 'row-matched': comparisonDetail.comparison?.experience?.matched, 'row-mismatched': !comparisonDetail.comparison?.experience?.matched }">
            <div class="comp-col dim-col">
              <span class="dim-icon">💼</span>经验
            </div>
            <div class="comp-col job-col">{{ comparisonDetail.comparison?.experience?.requirement || '不限' }}</div>
            <div class="comp-col resume-col">
              {{ comparisonDetail.comparison?.experience?.actual || '未填写' }}
              <el-icon v-if="comparisonDetail.comparison?.experience?.matched" class="match-icon" color="#67c23a"><CircleCheck /></el-icon>
              <el-icon v-else class="match-icon" color="#f56c6c"><Close /></el-icon>
            </div>
            <div class="comp-col score-col"><span class="score-pill" :style="{ background: getScoreColor(comparisonDetail.experienceScore ?? 0) }">{{ comparisonDetail.experienceScore ?? 0 }}/20</span></div>
          </div>

          <!-- Education -->
          <div class="comp-row" :class="{ 'row-matched': comparisonDetail.comparison?.education?.matched, 'row-mismatched': !comparisonDetail.comparison?.education?.matched }">
            <div class="comp-col dim-col">
              <span class="dim-icon">🎓</span>学历
            </div>
            <div class="comp-col job-col">{{ comparisonDetail.comparison?.education?.requirement || '不限' }}</div>
            <div class="comp-col resume-col">
              {{ comparisonDetail.comparison?.education?.actual || '未填写' }}
              <el-icon v-if="comparisonDetail.comparison?.education?.matched" class="match-icon" color="#67c23a"><CircleCheck /></el-icon>
              <el-icon v-else class="match-icon" color="#f56c6c"><Close /></el-icon>
            </div>
            <div class="comp-col score-col"><span class="score-pill" :style="{ background: getScoreColor(comparisonDetail.educationScore ?? 0) }">{{ comparisonDetail.educationScore ?? 0 }}/10</span></div>
          </div>

          <!-- Salary -->
          <div class="comp-row" :class="{ 'row-matched': comparisonDetail.comparison?.salary?.matched, 'row-mismatched': !comparisonDetail.comparison?.salary?.matched }">
            <div class="comp-col dim-col">
              <span class="dim-icon">💰</span>薪资
            </div>
            <div class="comp-col job-col">{{ comparisonDetail.comparison?.salary?.range || '面议' }}</div>
            <div class="comp-col resume-col">
              {{ comparisonDetail.comparison?.salary?.expected || '未填写' }}
              <el-icon v-if="comparisonDetail.comparison?.salary?.matched" class="match-icon" color="#67c23a"><CircleCheck /></el-icon>
              <el-icon v-else class="match-icon" color="#f56c6c"><Close /></el-icon>
            </div>
            <div class="comp-col score-col">
              <span class="score-pill" :class="(comparisonDetail.salaryAdjustment ?? 0) >= 0 ? 'pill-success' : 'pill-danger'">
                {{ (comparisonDetail.salaryAdjustment ?? 0) >= 0 ? '+' : '' }}{{ comparisonDetail.salaryAdjustment ?? 0 }}
              </span>
            </div>
          </div>
        </div>

        <!-- Bonus breakdown -->
        <div class="bonus-section" v-if="comparisonDetail.bonusScore > 0 && comparisonDetail.bonus">
          <h4>加分项明细（{{ comparisonDetail.bonusScore }}/10）</h4>
          <div class="bonus-grid">
            <div class="bonus-item" v-for="(value, key) in comparisonDetail.bonus" :key="key"
              :class="{ 'bonus-active': typeof value === 'number' && value > 0 && key !== 'total' && key !== 'maxBonus' }">
              <span class="bonus-label" v-if="typeof value === 'number' && value > 0 && key !== 'total' && key !== 'maxBonus'">
                {{ bonusLabelMap[key] || key }}
              </span>
              <span class="bonus-val" v-if="typeof value === 'number' && value > 0 && key !== 'total' && key !== 'maxBonus'">
                +{{ value }}
              </span>
            </div>
          </div>
        </div>

        <!-- Suggestions -->
        <div class="suggestions-section" v-if="comparisonDetail.suggestions?.length > 0">
          <h4>{{ userStore.isEmployee ? '简历优化建议' : '候选人评估' }}</h4>
          <div class="suggestion-list">
            <div v-for="(suggestion, idx) in comparisonDetail.suggestions" :key="idx" class="suggestion-item">
              <span class="suggestion-index">{{ idx + 1 }}</span>
              <span class="suggestion-text">{{ suggestion }}</span>
            </div>
          </div>
        </div>

        <!-- AI suggestion -->
        <div class="ai-suggestion-box" v-if="comparisonData.aiSuggestion">
          <div class="ai-suggestion-header">
            <el-icon><Promotion /></el-icon>
            <span>AI 综合评估</span>
          </div>
          <p>{{ comparisonData.aiSuggestion }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="comparisonVisible = false">关闭</el-button>
        <el-button v-if="!userStore.isEmployee" type="primary" @click="inviteCandidateDirect(comparisonData?.resume)" :disabled="!comparisonData?.resume">
          邀请面试
        </el-button>
        <el-button v-if="userStore.isEmployee" type="success" @click="applyJobDirect" :disabled="!comparisonData?.job">
          一键投递
        </el-button>
      </template>
    </el-dialog>

    <!-- 已屏蔽职位对话框 -->
    <el-dialog v-model="blockedDialogVisible" title="已屏蔽的职位" width="520px">
      <div v-loading="loadingBlocked">
        <el-empty v-if="!loadingBlocked && blockedJobs.length === 0" description="暂无屏蔽职位" :image-size="60" />
        <div v-else class="blocked-list">
          <div v-for="item in blockedJobs" :key="item.id" class="blocked-item">
            <div class="blocked-info">
              <div class="blocked-title">{{ item.jobTitle || '职位 #' + item.jobId }}</div>
              <div class="blocked-meta">屏蔽于 {{ formatTime(item.createTime) }}</div>
            </div>
            <el-button size="small" type="primary" plain @click="unblockJob(item)">取消屏蔽</el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="blockedDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 举报对话框 -->
    <el-dialog v-model="reportDialogVisible" title="举报" width="500px">
      <el-form ref="reportFormRef" :model="reportForm" :rules="reportRules" label-width="100px">
        <el-form-item label="被举报对象">
          <el-input :value="reportTargetName" disabled />
        </el-form-item>
        <el-form-item label="举报原因" prop="reason">
          <el-select v-model="reportForm.reason" placeholder="请选择举报原因" style="width: 100%">
            <el-option label="虚假信息" value="虚假信息" />
            <el-option label="欺诈行为" value="欺诈行为" />
            <el-option label="违规内容" value="违规内容" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细描述" prop="description">
          <el-input
            v-model="reportForm.description"
            type="textarea"
            :rows="4"
            placeholder="请详细描述举报原因（选填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReport" :loading="submittingReport">
          <el-icon><WarnTriangleFilled /></el-icon> 提交举报
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Location, Money, Briefcase, School, User, Loading, Calendar, CircleCheck, Warning, Promotion, Document, Close, Clock, Refresh, Hide, WarnTriangleFilled } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const matching = ref(false)
const sortMode = ref('composite')

// 根据用户角色设置默认标签页
const activeTab = ref(userStore.isEmployer || userStore.isAdmin ? 'employer' : 'jobseeker')

// 求职者匹配
const selectedResume = ref(null)
const matchResults = ref([])
const matchFromCache = ref(false)

// 招聘者匹配
const selectedJobId = ref(null)
const jobList = ref([])
const candidateResults = ref([])
const candidateFromCache = ref(false)

// 职位详情
const jobDetailVisible = ref(false)
const currentJob = ref(null)

// 面试邀请
const interviewVisible = ref(false)
const invitingCandidate = ref(null)
const inviting = ref(false)
const interviewForm = ref({
  interviewTime: '',
  location: '',
  remark: ''
})

// 需求对比
const comparisonVisible = ref(false)
const comparisonData = ref(null)
const comparisonDetail = ref(null)

// 投递相关
const applyDialogVisible = ref(false)
const myResumes = ref([])
const selectedResumeIdForApply = ref(null)
const loadingResumes = ref(false)
const coverLetter = ref('')
const applying = ref(false)

// 匹配分数本地缓存（sessionStorage），对照详情后保存最新分数，刷新后仍可用
const SCORE_CACHE_KEY = 'match_score_cache'
const SCORE_CACHE_TTL = 3600000 // 1小时过期

const saveScoreToCache = (jobId, score, detail) => {
  try {
    const cache = JSON.parse(sessionStorage.getItem(SCORE_CACHE_KEY) || '{}')
    cache[jobId] = { score, detail, time: Date.now() }
    sessionStorage.setItem(SCORE_CACHE_KEY, JSON.stringify(cache))
  } catch {}
}

const loadScoreCache = () => {
  try {
    const cache = JSON.parse(sessionStorage.getItem(SCORE_CACHE_KEY) || '{}')
    const now = Date.now()
    // 清理过期条目
    for (const key of Object.keys(cache)) {
      if (now - cache[key].time > SCORE_CACHE_TTL) delete cache[key]
    }
    return cache
  } catch { return {} }
}

const applyCachedScores = (results) => {
  const cache = loadScoreCache()
  for (const r of results) {
    const key = r.job?.id || r.resume?.id
    if (key && cache[key]) {
      r.matchScore = cache[key].score
      r.matchDetail = cache[key].detail || r.matchDetail
    }
  }
}

const clearScoreCache = () => {
  try { sessionStorage.removeItem(SCORE_CACHE_KEY) } catch {}
}

// 维度标签（v5 新权重）
const dimensionLabels = [
  { key: 'skill', label: '技能匹配', max: 50 },
  { key: 'industryScore', label: '行业匹配', max: 15 },
  { key: 'experienceScore', label: '经验匹配', max: 20 },
  { key: 'educationScore', label: '学历匹配', max: 10 },
  { key: 'bonusScore', label: '加分项', max: 10 }
]

// 行业分类名称映射
const categoryNameMap = {
  1: 'IT/互联网', 2: '金融/会计', 3: '教育', 4: '医疗', 5: '制造',
  6: '销售', 7: '行政', 8: '建筑', 9: '传媒', 10: '服务'
}

// 加分项标签映射
const bonusLabelMap = {
  allSkillsMatched: '全技能匹配',
  detailedExperience: '详细工作经历',
  moderateExperience: '较详细经历',
  basicExperience: '基础经历',
  achievementKeywords: '项目成果突出',
  someAchievements: '部分项目成果',
  educationExceed: '学历超标',
  technicalDepth: '技术深度',
  someDepth: '一定技术深度',
  selfIntroMatch: '自我介绍匹配',
  highCompleteness: '简历完整度高',
  quickResponse: '快速响应'
}

// 原始匹配结果（用于筛选）
const originalMatchResults = ref([])
const originalCandidateResults = ref([])

// 当前筛选条件（从导航栏搜索组件同步）
const currentFilters = ref({
  keyword: '',
  location: [],
  salaryRange: '',
  experience: '',
  education: '',
  skills: [],
  minScore: ''
})

// ==================== 搜索和筛选逻辑 ====================
const applyFiltersAndSearch = (params) => {
  if (activeTab.value === 'jobseeker') {
    applyJobseekerSearch(params)
  } else {
    applyEmployerSearch(params)
  }
}

// 求职者搜索筛选
const applyJobseekerSearch = (params) => {
  let results = [...originalMatchResults.value]
  const { keyword, location, salaryRange, experience, education, skills, minScore } = params

  // 关键词搜索
  if (keyword) {
    const kw = keyword.toLowerCase()
    results = results.filter(r => {
      const title = (r.job?.title || '').toLowerCase()
      const desc = (r.job?.description || '').toLowerCase()
      const req = (r.job?.requirements || '').toLowerCase()
      return title.includes(kw) || desc.includes(kw) || req.includes(kw)
    })
  }

  // 地点筛选
  if (location?.length > 0) {
    const city = location[0]
    const district = location[1]
    results = results.filter(r => {
      const loc = r.job?.location || ''
      if (district) return loc.includes(district)
      return loc.includes(city)
    })
  }

  // 薪资筛选
  if (salaryRange) {
    const [min, max] = salaryRange.split('-').map(v => parseInt(v) * 1000)
    results = results.filter(r => {
      const jobMin = r.job?.salaryMin || 0
      if (max) return jobMin >= min && jobMin <= max
      return jobMin >= min
    })
  }

  // 经验筛选
  if (experience) {
    results = results.filter(r => (r.job?.experienceRequired || '').includes(experience))
  }

  // 学历筛选
  if (education) {
    results = results.filter(r => (r.job?.educationRequired || '').includes(education))
  }

  // 技能筛选
  if (skills?.length > 0) {
    results = results.filter(r => {
      const jobReq = ((r.job?.requirements || '') + ' ' + (r.job?.description || '') + ' ' + (r.job?.title || '')).toLowerCase()
      return skills.some(skill => jobReq.includes(skill.toLowerCase()))
    })
  }

  // 最低匹配度筛选
  if (minScore) {
    results = results.filter(r => Number(r.matchScore) >= parseInt(minScore))
  }

  matchResults.value = results
}

// 企业端搜索筛选
const applyEmployerSearch = (params) => {
  let results = [...originalCandidateResults.value]
  const { keyword, education, experience, salaryRange, skills, minScore } = params

  // 关键词搜索
  if (keyword) {
    const kw = keyword.toLowerCase()
    results = results.filter(r => {
      const name = (r.resume?.name || '').toLowerCase()
      const skillsStr = (r.resume?.skills || '').toLowerCase()
      return name.includes(kw) || skillsStr.includes(kw)
    })
  }

  // 学历筛选
  if (education) {
    const eduLevels = ['大专', '本科', '硕士', '博士']
    const requireIndex = eduLevels.indexOf(education)
    results = results.filter(r => {
      const edu = (r.resume?.education || '').toLowerCase()
      const candidateIndex = eduLevels.findIndex(l => edu.includes(l))
      if (candidateIndex === -1) return true
      return candidateIndex >= requireIndex
    })
  }

  // 经验筛选
  if (experience) {
    const [min, max] = experience.split('-').map(v => parseInt(v))
    results = results.filter(r => {
      const exp = r.resume?.experience || ''
      const match = exp.match(/(\d+)/)
      if (!match) return true
      const years = parseInt(match[1])
      if (max) return years >= min && years <= max
      return years >= min
    })
  }

  // 薪资筛选
  if (salaryRange) {
    const [min, max] = salaryRange.split('-').map(v => parseInt(v) * 1000)
    results = results.filter(r => {
      const expected = r.resume?.expectedSalary || 0
      if (max) return expected >= min && expected <= max
      return expected >= min
    })
  }

  // 技能筛选
  if (skills?.length > 0) {
    results = results.filter(r => {
      const resumeSkills = parseSkills(r.resume?.skills || '').map(s => s.toLowerCase())
      return skills.some(skill => resumeSkills.includes(skill.toLowerCase()))
    })
  }

  // 最低匹配度筛选
  if (minScore) {
    results = results.filter(r => Number(r.matchScore) >= parseInt(minScore))
  }

  candidateResults.value = results
}

// 监听导航栏搜索事件
const handleMatchSearch = (e) => {
  const params = e.detail
  currentFilters.value = { ...currentFilters.value, keyword: params.keyword }
  applyFiltersAndSearch(currentFilters.value)
}

const handleMatchFilter = (e) => {
  const filters = e.detail
  currentFilters.value = { ...currentFilters.value, ...filters }
  applyFiltersAndSearch(currentFilters.value)
}

// ==================== 排序 ====================
const sortedResults = computed(() => {
  const results = [...matchResults.value]
  if (sortMode.value === 'score') {
    results.sort((a, b) => Number(b.matchScore) - Number(a.matchScore))
  } else if (sortMode.value === 'freshness') {
    results.sort((a, b) => new Date(b.job?.createTime || 0) - new Date(a.job?.createTime || 0))
  }
  return results
})

const sortedCandidateResults = computed(() => {
  const results = [...candidateResults.value]
  if (sortMode.value === 'score') {
    results.sort((a, b) => Number(b.matchScore) - Number(a.matchScore))
  }
  return results
})

// ==================== 新辅助方法 ====================
const isFreshJob = (job) => {
  if (!job?.createTime) return false
  const days = (Date.now() - new Date(job.createTime).getTime()) / (1000 * 60 * 60 * 24)
  return days <= 7
}

const formatJobTime = (time) => {
  if (!time) return '未知'
  const days = Math.floor((Date.now() - new Date(time).getTime()) / (1000 * 60 * 60 * 24))
  if (days === 0) return '今天发布'
  if (days === 1) return '昨天发布'
  if (days < 30) return `${days}天前发布`
  return `${Math.floor(days / 30)}个月前发布`
}

// 屏蔽职位
const blockJob = async (job) => {
  if (!job) return
  try {
    await ElMessageBox.confirm(`确定要屏蔽「${job.title}」吗？屏蔽后该职位将不再出现在推荐列表中。`, '确认屏蔽', {
      confirmButtonText: '确定屏蔽',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.post('/match/block', { blockType: 1, jobId: job.id, reason: '用户手动屏蔽' })
    // 从结果中移除
    matchResults.value = matchResults.value.filter(r => r.job?.id !== job.id)
    ElMessage.success('已屏蔽该职位')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

// 已屏蔽职位
const blockedDialogVisible = ref(false)
const blockedJobs = ref([])
const loadingBlocked = ref(false)

const openBlockedDialog = async () => {
  blockedDialogVisible.value = true
  await fetchBlockedJobs()
}

const fetchBlockedJobs = async () => {
  loadingBlocked.value = true
  try {
    const res = await request.get('/match/blocks', { params: { blockType: 1 } })
    blockedJobs.value = Array.isArray(res) ? res : []
  } catch {
    blockedJobs.value = []
  } finally {
    loadingBlocked.value = false
  }
}

const unblockJob = async (item) => {
  try {
    await ElMessageBox.confirm(`确定取消屏蔽「${item.jobTitle || '该职位'}」吗？`, '取消屏蔽', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    await request.delete('/match/block', { params: { blockType: 1, targetId: item.jobId } })
    blockedJobs.value = blockedJobs.value.filter(b => b.id !== item.id)
    ElMessage.success('已取消屏蔽')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

// 举报相关
const reportDialogVisible = ref(false)
const reportFormRef = ref()
const reportTargetId = ref(null)
const reportTargetName = ref('')
const reportTargetType = ref(4)
const submittingReport = ref(false)
const reportForm = reactive({
  reason: '',
  description: ''
})
const reportRules = {
  reason: [{ required: true, message: '请选择举报原因', trigger: 'change' }]
}

const openReportDialog = (type, id, name) => {
  reportTargetType.value = type
  reportTargetId.value = id
  reportTargetName.value = name || ''
  reportForm.reason = ''
  reportForm.description = ''
  reportDialogVisible.value = true
}

const submitReport = async () => {
  const valid = await reportFormRef.value.validate().catch(() => false)
  if (!valid) return
  submittingReport.value = true
  try {
    await request.post('/report', {
      reportedType: reportTargetType.value,
      reportedId: reportTargetId.value,
      reason: reportForm.reason,
      description: reportForm.description || null
    })
    ElMessage.success('举报已提交，我们会尽快处理')
    reportDialogVisible.value = false
  } catch (error) {
    ElMessage.error('提交失败：' + (error.response?.data?.error || '未知错误'))
  } finally {
    submittingReport.value = false
  }
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 172800000) return '昨天'
  return d.toLocaleDateString('zh-CN')
}

const getDimColor = (score, max) => {
  const ratio = score / max
  if (ratio >= 0.8) return '#67c23a'
  if (ratio >= 0.5) return '#e6a23c'
  return '#f56c6c'
}

// 解析技能JSON (增强版)
const parseSkills = (skills) => {
  if (!skills) return []
  try {
    const arr = JSON.parse(skills)
    const skillList = Array.isArray(arr) ? arr : skills.split(/[,，、;；\s]+/).filter(s => s.trim())
    // 去重并清理
    return [...new Set(skillList.map(s => s.trim()).filter(s => s))]
  } catch {
    const skillList = skills.split(/[,，、;；\s]+/).filter(s => s.trim())
    return [...new Set(skillList)]
  }
}

// 获取分数颜色
const getScoreColor = (score) => {
  if (score >= 90) return '#67c23a'
  if (score >= 80) return '#409eff'
  if (score >= 70) return '#e6a23c'
  return '#f56c6c'
}

// 职位核心技能列表 (去重版)
const jobSkills = computed(() => {
  if (!currentJob.value) return []
  const req = (currentJob.value.requirements || '') + ' ' + (currentJob.value.description || '') + ' ' + (currentJob.value.title || '')
  
  // 扩充关键词库，覆盖更多行业和技术栈
  const coreTech = [
    'Java', 'Python', 'Go', 'Golang', 'C++', 'C#', 'JavaScript', 'TypeScript',
    'Vue', 'React', 'Angular', 'Node.js', 'Spring', 'Spring Boot', 'Spring Cloud',
    'Django', 'Flask', 'FastAPI', '.NET', 'PHP', 'Ruby', 'Scala', 'Rust', 'Swift', 'Kotlin'
  ]
  
  const middleware = [
    'MySQL', 'PostgreSQL', 'Oracle', 'MongoDB', 'Redis', 'Elasticsearch',
    'RabbitMQ', 'Kafka', 'RocketMQ', 'Nginx', 'Apache', 'Tomcat', 'Zookeeper'
  ]
  
  const devops = [
    'Docker', 'K8s', 'Kubernetes', 'Jenkins', 'Git', 'CI/CD', 'DevOps',
    'Linux', 'Shell', 'AWS', 'Azure', 'GCP', '阿里云', '腾讯云'
  ]
  
  const concepts = [
    '微服务', '分布式', '高并发', '网络编程', '容器编排', '架构设计',
    'gRPC', 'Protobuf', 'RESTful', 'GraphQL', 'WebSocket',
    '区块链', 'Solidity', '智能合约', '以太坊', 'Hyperledger', 'Fabric', 'DeFi', '密码学',
    'AI', '人工智能', '机器学习', '深度学习', 'NLP', '计算机视觉', '大模型', 'LLM',
    '大数据', 'Hadoop', 'Spark', 'Flink', 'Hive', '数据仓库',
    '移动端', 'iOS', 'Android', 'Flutter', 'React Native', '小程序',
    '游戏开发', 'Unity', 'Unreal', 'Cocos',
    '网络安全', '渗透测试', '防火墙', '信息安全',
    '财务', '审计', '税务', '风控', '合规', 'ERP', 'CRM',
    '市场营销', 'SEO', 'SEM', '社交媒体', '品牌策划', '数据分析', '用户增长'
  ]
  
  // 教育/教师行业关键词
  const education = [
    '教学', '授课', '备课', '教案', '课件', '教研', '辅导',
    '物理', '化学', '生物', '数学', '语文', '英语', '历史', '地理', '政治',
    '教师资格证', '教师资格证', '教师编制', '班主任',
    '课程设计', '教材编写', '学生管理', '家校沟通', '升学指导',
    '初中', '高中', '小学', '幼儿园', '大学', '研究生', '博士', '硕士',
    '物理实验', '化学实验', '生物实验', '实验室管理',
    '教育技术', '在线教学', '直播教学', '录播课程', 'MOOC'
  ]
  
  // 金融/会计关键词
  const finance = [
    '会计核算', '全盘账', '账务处理', '总账', '明细账', '记账',
    '税务申报', '报税', '税务筹划', '个税申报', '企业所得税', '增值税',
    '财务报表', '财务分析', '经营分析', '报表分析', '财务预算',
    '审计', '内控', '合规', '风控', '尽职调查',
    '用友', '金蝶', 'SAP', 'ERP', '财务软件',
    'CPA', '注册会计师', 'ACCA', 'CFA', '税务师', '中级会计师', '初级会计',
    'Excel', 'VBA', '数据透视表', '函数公式'
  ]
  
  // 产品/运营关键词
  const productOps = [
    '产品设计', '需求分析', '原型设计', 'Axure', '墨刀', 'Figma',
    '用户研究', '用户画像', '用户旅程', 'A/B测试', '数据埋点',
    '产品规划', '版本管理', '迭代管理', '敏捷开发', 'Scrum',
    '运营策略', '活动策划', '内容运营', '社群运营', '用户运营',
    '拉新', '留存', '转化', 'GMV', 'DAU', 'MAU', 'ROI'
  ]
  
  // 设计关键词
  const design = [
    'UI设计', 'UX设计', '交互设计', '视觉设计', '平面设计',
    'PS', 'Photoshop', 'AI', 'Illustrator', 'Sketch', 'Figma',
    '动效设计', 'AE', 'After Effects', 'C4D', '3D设计',
    '品牌设计', 'Logo设计', '海报设计', '画册设计'
  ]
  
  // 销售/商务关键词
  const sales = [
    '客户开发', '客户维护', '商务谈判', '合同签订', '渠道拓展',
    '大客户', 'KA', 'B2B', 'B2C', 'O2O',
    '销售目标', '业绩指标', '转化率', '客单价', '复购率'
  ]
  
  // 人力/行政关键词
  const hr = [
    '招聘', '面试', '绩效考核', 'KPI', 'OKR', '薪酬管理', '社保', '公积金',
    '培训', '员工关系', '企业文化', '组织架构', '人才发展',
    'HRBP', 'COE', 'SSC', 'HR三支柱'
  ]
  
  const allKeywords = [
    ...coreTech, ...middleware, ...devops, ...concepts,
    ...education, ...finance, ...productOps, ...design, ...sales, ...hr
  ]
  
  // 提取匹配的技能并去重
  const matchedSkills = allKeywords.filter(kw => 
    req.toLowerCase().includes(kw.toLowerCase())
  )
  
  return [...new Set(matchedSkills)]
})

// 候选人技能列表 (去重版)
const candidateSkills = computed(() => {
  return parseSkills(comparisonData.value?.resume?.skills)
})

// 解析经验
const parseExperience = (resume) => {
  if (!resume) return '未知'
  const exp = resume.experience || resume.workExperience || ''
  const match = exp.match(/(\d+[年以个])?/) 
  return match ? exp.substring(0, 50) + (exp.length > 50 ? '...' : '') : '无经验'
}

// 判断技能是否匹配 (智能匹配)
const isSkillMatch = (skill) => {
  if (!currentJob.value || !skill) return false
  const req = (currentJob.value.requirements || '').toLowerCase() + 
              (currentJob.value.description || '').toLowerCase()
  const skillLower = skill.toLowerCase()
  
  // 直接匹配
  if (req.includes(skillLower)) return true
  
  // 别名映射匹配
  const aliasMap = {
    'k8s': 'kubernetes',
    'kubernetes': 'k8s',
    'golang': 'go',
    'go': 'golang',
    'spring boot': 'springboot',
    'springboot': 'spring boot',
    'spring cloud': 'springcloud',
    'springcloud': 'spring cloud',
    'ci/cd': 'cicd',
    'cicd': 'ci/cd',
    'node.js': 'node',
    'node': 'node.js'
  }
  
  const alias = aliasMap[skillLower]
  if (alias && req.includes(alias)) return true
  
  return false
}

// 判断候选人是否具备某项技能 (智能匹配)
const isCandidateHasSkill = (skill) => {
  const skills = candidateSkills.value
  const skillLower = skill.toLowerCase()
  
  // 直接匹配
  if (skills.some(s => s.toLowerCase() === skillLower)) return true
  
  // 别名映射匹配
  const aliasMap = {
    'k8s': 'kubernetes',
    'kubernetes': 'k8s',
    'golang': 'go',
    'go': 'golang',
    'spring boot': 'springboot',
    'springboot': 'spring boot',
    'spring cloud': 'springcloud',
    'springcloud': 'spring cloud',
    'ci/cd': 'cicd',
    'cicd': 'ci/cd',
    'node.js': 'node',
    'node': 'node.js'
  }
  
  const alias = aliasMap[skillLower]
  if (alias && skills.some(s => s.toLowerCase() === alias.toLowerCase())) return true
  
  return false
}

// 获取匹配的技能数量
const getMatchedSkillCount = () => {
  const skills = candidateSkills.value
  const jobReqs = (currentJob.value?.requirements || '').toLowerCase() + 
                  (currentJob.value?.description || '').toLowerCase()
  
  return skills.filter(skill => {
    const skillLower = skill.toLowerCase()
    if (jobReqs.includes(skillLower)) return true
    
    // 别名匹配
    const aliasMap = {
      'k8s': 'kubernetes', 'kubernetes': 'k8s',
      'golang': 'go', 'go': 'golang',
      'spring boot': 'springboot', 'springboot': 'spring boot',
      'ci/cd': 'cicd', 'cicd': 'ci/cd',
      'node.js': 'node', 'node': 'node.js'
    }
    const alias = aliasMap[skillLower]
    return alias && jobReqs.includes(alias)
  }).length
}

// 计算技能覆盖率
const getSkillCoverage = () => {
  const total = jobSkills.value.length
  if (total === 0) return 100
  const matched = getMatchedSkillCount()
  return Math.round((matched / total) * 100)
}

// 获取覆盖率颜色
const getCoverageColor = () => {
  const coverage = getSkillCoverage()
  if (coverage >= 80) return '#67c23a'
  if (coverage >= 60) return '#409eff'
  if (coverage >= 40) return '#e6a23c'
  return '#f56c6c'
}

// 获取已匹配的技能列表
const getMatchedSkills = () => {
  return candidateSkills.value.filter(skill => isSkillMatch(skill))
}

// 获取缺失的技能列表 (职位要求但候选人没有的)
const getMissingSkills = () => {
  return jobSkills.value.filter(skill => !isCandidateHasSkill(skill))
}

// 判断学历是否匹配
const isEducationMatch = () => {
  if (!currentJob.value?.educationRequired || currentJob.value.educationRequired === '不限') return true
  const edu = (comparisonData.value?.resume?.education || '').toLowerCase()
  const req = currentJob.value.educationRequired.toLowerCase()
  
  const eduLevels = ['博士', '硕士', '本科', '大专', '高中']
  const candidateLevel = eduLevels.findIndex(l => edu.includes(l))
  const requireLevel = eduLevels.findIndex(l => req.includes(l))
  
  if (candidateLevel === -1 || requireLevel === -1) return edu.includes(req) || req.includes('不限')
  return candidateLevel <= requireLevel // 学历越高索引越小
}

// 判断经验是否匹配
const isExperienceMatch = () => {
  if (!currentJob.value?.experienceRequired || currentJob.value.experienceRequired === '不限') return true
  const exp = (comparisonData.value?.resume?.experience || comparisonData.value?.resume?.workExperience || '').toLowerCase()
  const req = currentJob.value.experienceRequired.toLowerCase()
  
  const reqMatch = req.match(/(\d+)/)
  if (!reqMatch) return true
  
  const reqYears = parseInt(reqMatch[1])
  const expMatch = exp.match(/(\d+)/)
  if (!expMatch) return false
  
  return parseInt(expMatch[1]) >= reqYears
}

// 判断薪资是否匹配
const isSalaryMatch = () => {
  const candidateSalary = comparisonData.value?.resume?.expectedSalary
  const minSalary = currentJob.value?.salaryMin
  const maxSalary = currentJob.value?.salaryMax
  
  if (!candidateSalary || (!minSalary && !maxSalary)) return true
  if (minSalary && candidateSalary < minSalary) return false
  if (maxSalary && candidateSalary > maxSalary * 1.2) return false // 允许20%上浮
  return true
}

// 加载我的简历
const loadMyResume = async () => {
  try {
    const res = await request({
      method: 'get',
      url: '/resume/my',
      skipNotFoundNotification: true,
      skipErrorNotification: true
    })
    selectedResume.value = res
  } catch {
    selectedResume.value = null
  }
}

// 加载我的所有简历列表
const loadMyResumes = async () => {
  loadingResumes.value = true
  try {
    const res = await request.get('/resume/my/list', { skipErrorNotification: true })
    myResumes.value = res || []
    // 默认选中默认简历
    if (myResumes.value.length > 0) {
      const defaultResume = myResumes.value.find(r => r.isDefault === 1)
      selectedResumeIdForApply.value = defaultResume?.id || myResumes.value[0].id
    }
  } catch (e) {
    console.error('加载简历列表失败', e)
    myResumes.value = []
  } finally {
    loadingResumes.value = false
  }
}

// 刷新简历
const loadMyRefresh = async () => {
  await loadMyResume()
  ElMessage.success('简历已刷新')
}

// 开始求职匹配
// forceRefresh: true 时先清除缓存再重新匹配
const startJobMatching = async (forceRefresh = false) => {
  if (!selectedResume.value) {
    ElMessage.warning('请先创建简历')
    return
  }

  // 调试信息
  const token = localStorage.getItem('token')
  console.log('=== 匹配调试信息 ===')
  console.log('Token存在:', !!token)
  console.log('Token长度:', token?.length)
  console.log('简历ID:', selectedResume.value.id)
  console.log('强制刷新:', forceRefresh)

  if (!token) {
    ElMessage.error('未找到登录凭证，请重新登录')
    return
  }

  // 检查 token 是否过期
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const expMs = payload.exp * 1000
    const now = Date.now()
    const remainMin = Math.round((expMs - now) / 60000)
    console.log('Token过期时间:', new Date(expMs).toLocaleString())
    console.log('Token剩余分钟:', remainMin)
    if (expMs < now) {
      ElMessage.error('登录已过期，请重新登录后再试')
      return
    }
  } catch (e) {
    console.warn('Token解析失败:', e.message)
  }

  matching.value = true
  matchFromCache.value = false
  try {
    // 强制刷新时先清除服务端缓存
    if (forceRefresh) {
      await request.delete('/match/cache/jobs', { skipErrorNotification: true })
      clearScoreCache()
    }
    const startTime = Date.now()
    console.log('发送匹配请求...')
    const res = await request.get('/match/recommend/jobs', {
      params: { limit: 10 },
      timeout: 180000,
      skipErrorNotification: true
    })
    const elapsed = Date.now() - startTime
    console.log('匹配成功:', res, '耗时:', elapsed + 'ms')
    // 应用本地缓存的最新分数
    applyCachedScores(res || [])
    matchResults.value = res || []
    originalMatchResults.value = [...(res || [])]
    // 如果响应很快（<2s），大概率是缓存命中
    matchFromCache.value = !forceRefresh && elapsed < 2000
    if (matchResults.value.length > 0) {
      if (matchFromCache.value) {
        ElMessage.success(`已加载 ${matchResults.value.length} 个推荐职位（来自缓存）`)
      } else {
        ElMessage.success(`匹配完成，为您找到 ${matchResults.value.length} 个推荐职位`)
      }
    } else {
      ElMessage.info('暂未找到匹配的职位，请稍后重试')
    }
  } catch (error) {
    console.error('匹配失败:', error)
    console.error('错误详情:', {
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      message: error.message,
      code: error.code
    })
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      ElMessage.error('匹配超时，计算量较大，请稍后重试（首次匹配较慢，后续将从缓存读取）')
    } else if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请刷新页面后重新登录')
    } else {
      ElMessage.error(error.response?.data?.error || '匹配失败，请稍后重试')
    }
  } finally {
    matching.value = false
  }
}

// 加载职位列表
const loadJobList = async () => {
  try {
    const res = await request({
      method: 'get',
      url: '/job',
      skipNotFoundNotification: true
    })
    jobList.value = res.records || res || []
  } catch (error) {
    console.error('加载职位列表失败', error)
  }
}

// 加载职位匹配
const loadJobMatch = async (jobId, forceRefresh = false) => {
  if (!jobId) return
  matching.value = true
  candidateResults.value = []
  candidateFromCache.value = false
  try {
    if (forceRefresh) {
      await request.delete(`/match/cache/candidates/${jobId}`, { skipErrorNotification: true })
      clearScoreCache()
    }
    const startTime = Date.now()
    // 增加超时时间到 3 分钟，适配 AI 匹配
    const res = await request.get(`/match/recommend/candidates/${jobId}`, {
      params: { limit: 10 },
      timeout: 180000,
      skipErrorNotification: true
    })
    const elapsed = Date.now() - startTime
    // 应用本地缓存的最新分数
    applyCachedScores(res || [])
    candidateResults.value = res || []
    originalCandidateResults.value = [...(res || [])]
    candidateFromCache.value = !forceRefresh && elapsed < 2000
    if (candidateResults.value.length > 0) {
      if (candidateFromCache.value) {
        ElMessage.success(`已加载 ${candidateResults.value.length} 位候选人（来自缓存）`)
      } else {
        ElMessage.success(`匹配完成，为您推荐 ${candidateResults.value.length} 位候选人`)
      }
    } else {
      ElMessage.info('暂未找到合适的候选人')
    }
  } catch (error) {
    console.error('候选人匹配失败:', error)
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      ElMessage.warning('匹配超时，计算量较大，请稍后重试（首次匹配较慢，后续将从缓存读取）')
    } else if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请刷新页面后重新登录')
    } else {
      ElMessage.error(error.response?.data?.error || '匹配失败，请重试')
    }
  } finally {
    matching.value = false
  }
}

// 查看职位详情
const viewJobDetail = (job) => {
  currentJob.value = job
  jobDetailVisible.value = true
}

// 查看简历详情
const viewResumeDetail = (resume) => {
  if (resume?.id) {
    router.push(`/resumes?id=${resume.id}`)
  }
}

// 邀请候选人面试
const inviteCandidate = (resume, job) => {
  if (!resume || !job) {
    ElMessage.warning('无法获取候选人或职位信息')
    return
  }
  invitingCandidate.value = resume
  currentJob.value = job
  interviewForm.value = {
    interviewTime: '',
    location: '',
    remark: ''
  }
  interviewVisible.value = true
}

// 确认面试邀请
const confirmInvite = async () => {
  if (!interviewForm.value.interviewTime) {
    ElMessage.warning('请选择面试时间')
    return
  }
  if (!interviewForm.value.location) {
    ElMessage.warning('请填写面试地点')
    return
  }

  inviting.value = true
  try {
    const resumeId = invitingCandidate.value?.id || invitingCandidate.value?.resume_id
    const userId = invitingCandidate.value?.userId || invitingCandidate.value?.user_id

    // 使用直接沟通模式（人才市场不需要投递记录）
    await request.post('/interview', {
      directChat: true,
      resumeId: resumeId,
      jobId: currentJob.value?.id,
      userId: userId,
      interviewTime: interviewForm.value.interviewTime,
      interviewLocation: interviewForm.value.location,
      notes: interviewForm.value.remark || 'AI智能匹配邀请面试'
    })

    ElMessage.success('面试邀请已发送')
    interviewVisible.value = false
  } catch (error) {
    console.error('邀请失败', error)
    ElMessage.error(error.response?.data?.error || '邀请失败，请重试')
  } finally {
    inviting.value = false
  }
}

// 查看需求对比（企业端）
const viewComparison = async (result) => {
  comparisonData.value = result
  currentJob.value = result.job || jobList.value.find(j => j.id === selectedJobId.value)
  // 获取详细对照数据
  if (result.resume?.id && result.job?.id) {
    try {
      const detail = await request.get('/match/detail', { params: { resumeId: result.resume.id, jobId: result.job.id } })
      comparisonDetail.value = detail
      // 用最新分数同步更新列表中的分数和详情
      if (detail?.total != null) {
        result.matchScore = detail.total
        result.matchDetail = detail
        // 企业端用 resumeId 作为缓存 key（同一职位下不同候选人）
        saveScoreToCache(result.resume.id, detail.total, detail)
      }
    } catch {
      comparisonDetail.value = result.matchDetail
    }
  } else {
    comparisonDetail.value = result.matchDetail
  }
  comparisonVisible.value = true
}

// 查看需求对比（求职者端）
const viewJobComparison = async (result) => {
  comparisonData.value = {
    ...result,
    resume: selectedResume.value,
    job: result.job
  }
  currentJob.value = result.job
  // 获取详细对照数据
  if (selectedResume.value?.id && result.job?.id) {
    try {
      const detail = await request.get('/match/detail', { params: { resumeId: selectedResume.value.id, jobId: result.job.id } })
      comparisonDetail.value = detail
      // 用最新分数同步更新列表中的分数和详情
      if (detail?.total != null) {
        result.matchScore = detail.total
        result.matchDetail = detail
        saveScoreToCache(result.job.id, detail.total, detail)
      }
    } catch {
      comparisonDetail.value = result.matchDetail
    }
  } else {
    comparisonDetail.value = result.matchDetail
  }
  comparisonVisible.value = true
}

// 直接邀请候选人
const inviteCandidateDirect = (resume) => {
  if (!resume) return
  invitingCandidate.value = resume
  // 从对比数据或当前选中职位获取 job
  currentJob.value = comparisonData.value?.job || currentJob.value || jobList.value.find(j => j.id === selectedJobId.value) || null
  interviewForm.value = { interviewTime: '', location: '', remark: '' }
  interviewVisible.value = true
}

// 一键投递（求职者端）
const applyJobDirect = async () => {
  if (!comparisonData.value?.job) {
    ElMessage.warning('职位信息缺失')
    return
  }
  
  // 先加载简历列表
  await loadMyResumes()
  
  if (myResumes.value.length === 0) {
    ElMessage.warning('暂无简历，请先创建简历')
    router.push('/my-resume')
    return
  }
  
  // 显示简历选择对话框
  applyDialogVisible.value = true
}

// 确认投递
const confirmApply = async () => {
  if (!selectedResumeIdForApply.value) {
    ElMessage.warning('请选择要投递的简历')
    return
  }
  
  applying.value = true
  try {
    await request.post('/application', {
      jobId: comparisonData.value.job.id,
      resumeId: selectedResumeIdForApply.value,
      coverLetter: coverLetter.value || '通过智能匹配功能一键投递'
    })
    
    ElMessage.success('投递成功！请等待企业处理')
    applyDialogVisible.value = false
    comparisonVisible.value = false
    // 重置表单
    selectedResumeIdForApply.value = null
    coverLetter.value = ''
  } catch (error) {
    if (error !== 'cancel') {
      console.error('投递失败', error)
      ElMessage.error(error.response?.data?.error || '投递失败，请稍后重试')
    }
  } finally {
    applying.value = false
  }
}

onMounted(() => {
  loadMyResume()
  loadJobList()
  fetchBlockedJobs()

  // 监听导航栏搜索事件
  window.addEventListener('match-search', handleMatchSearch)
  window.addEventListener('match-filter', handleMatchFilter)
})

onUnmounted(() => {
  // 移除事件监听
  window.removeEventListener('match-search', handleMatchSearch)
  window.removeEventListener('match-filter', handleMatchFilter)
})
</script>

<style scoped>
.match-page {
  padding: var(--space-6) var(--space-7);
  max-width: var(--container-xl);
  margin: 0 auto;
  min-height: calc(100vh - var(--header-height));
  animation: fadeIn 0.4s var(--ease-out);
}

/* ── 标签页 ──────────────────────────────── */
.match-tabs {
  background: transparent;
}

.match-tabs :deep(.el-tabs__header) {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: 0 var(--space-5);
  margin-bottom: var(--space-5);
}

.match-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.match-tabs :deep(.el-tabs__item) {
  font-size: var(--text-base);
  font-weight: var(--weight-medium);
  color: var(--gray-500);
  height: 48px;
  line-height: 48px;
}

.match-tabs :deep(.el-tabs__item.is-active) {
  color: var(--gray-900);
}

.match-tabs :deep(.el-tabs__active-bar) {
  background: var(--gray-900);
}

.sort-bar {
  display: flex;
  align-items: center;
}

.match-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

/* ── 简历概要 ─────────────────────────────── */
.resume-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
}

.resume-summary {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-6);
}

.resume-info {
  flex: 1;
  min-width: 0;
}

.resume-name {
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  margin-bottom: var(--space-2);
}

.resume-meta {
  display: flex;
  gap: var(--space-4);
  font-size: var(--text-sm);
  color: var(--gray-500);
  margin-bottom: var(--space-3);
}

.resume-skills {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.more-skills {
  font-size: var(--text-xs);
  color: var(--gray-400);
  padding: 2px var(--space-2);
}

.resume-actions {
  display: flex;
  gap: var(--space-2);
  align-items: center;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.cache-tip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-xs);
  color: var(--gray-400);
}

/* ── 职位选择 ─────────────────────────────── */
.job-select-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
}

.matching-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  margin-top: var(--space-3);
  padding: var(--space-3);
  background: var(--gray-50);
  border-radius: var(--radius-md);
  color: var(--gray-600);
  font-size: var(--text-sm);
}

.cache-line {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

/* ── 结果区 ──────────────────────────────── */
.results-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.results-header {
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--gray-100);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.results-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

/* ── 已屏蔽职位列表 ──────────────────────── */
.blocked-list {
  display: flex;
  flex-direction: column;
}
.blocked-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--gray-100);
}
.blocked-item:last-child { border-bottom: none; }
.blocked-info { flex: 1; min-width: 0; }
.blocked-title {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-800);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.blocked-meta {
  font-size: var(--text-xs);
  color: var(--gray-400);
  margin-top: 2px;
}

.results-list {
  display: flex;
  flex-direction: column;
}

.result-item {
  padding: var(--space-5);
  border-bottom: 1px solid var(--gray-100);
  transition: background var(--duration-fast) var(--ease-out);
  position: relative;
}

.result-item:last-child {
  border-bottom: none;
}

.result-item:hover {
  background: var(--gray-50);
}

.result-item.low-match {
  opacity: 0.7;
}

.result-item.low-match:hover {
  opacity: 1;
}

.result-item.hard-filtered {
  border-left: 3px solid var(--danger-400);
}

.hard-filter-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: var(--danger-500);
  background: var(--danger-50);
  padding: 2px var(--space-3);
  border-radius: var(--radius-sm);
  margin-bottom: var(--space-3);
  width: fit-content;
}

.result-body {
  display: flex;
  align-items: flex-start;
  gap: var(--space-5);
}

.result-score {
  flex-shrink: 0;
}

.result-info {
  flex: 1;
  min-width: 0;
}

.result-info h4 {
  margin: 0 0 var(--space-2);
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  cursor: pointer;
  transition: color var(--duration-fast) var(--ease-out);
}

.result-info h4:hover {
  color: var(--color-primary);
}

.result-title-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.result-title-row h4 {
  margin: 0;
  flex: 1;
}

.result-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  font-size: var(--text-sm);
  color: var(--gray-500);
  margin-bottom: var(--space-2);
}

.result-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.result-meta .el-icon {
  color: var(--gray-400);
  font-size: 14px;
}

.result-skills {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  margin-bottom: var(--space-2);
}

.result-suggestion {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--gray-500);
  background: var(--gray-50);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  display: flex;
  align-items: flex-start;
  gap: 4px;
  line-height: var(--leading-relaxed);
}

.result-suggestion .el-icon {
  color: var(--color-primary);
  flex-shrink: 0;
  margin-top: 2px;
}

.result-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

/* ── 维度得分 ─────────────────────────────── */
.dim-scores {
  margin-top: var(--space-3);
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px var(--space-4);
}

.dim-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.dim-label {
  font-size: var(--text-xs);
  color: var(--gray-500);
  min-width: 56px;
}

.dim-val {
  font-size: var(--text-xs);
  color: var(--gray-400);
  min-width: 36px;
  text-align: right;
  font-family: var(--font-mono);
}

/* ── 对比对话框 ───────────────────────────── */
.comparison-content {
  padding: var(--space-1) 0;
}

.score-overview {
  display: flex;
  align-items: center;
  gap: var(--space-6);
  padding: var(--space-5);
  background: var(--gray-50);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-5);
}

.score-circle-large {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 3px solid var(--gray-900);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--color-surface);
  flex-shrink: 0;
}

.score-num-large {
  font-size: 26px;
  font-weight: var(--weight-bold);
  line-height: 1;
  color: var(--gray-900);
}

.score-unit-large {
  font-size: var(--text-xs);
  color: var(--gray-400);
}

.score-breakdown {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.breakdown-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.break-label {
  font-size: var(--text-sm);
  color: var(--gray-500);
  min-width: 40px;
}

.break-value {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.bonus-value { color: var(--success-500); }
.penalty-value { color: var(--danger-500); }
.total-value { color: var(--gray-900); font-size: var(--text-lg); }

.dim-icon {
  margin-right: 4px;
  font-size: 14px;
}

.pill-success {
  background: var(--success-500) !important;
  color: #fff !important;
}

.pill-danger {
  background: var(--danger-500) !important;
  color: #fff !important;
}

.comparison-table {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.comp-row {
  display: flex;
  border-bottom: 1px solid var(--gray-100);
}

.comp-row:last-child { border-bottom: none; }

.header-row {
  background: var(--gray-50);
  font-weight: var(--weight-semibold);
}

.comp-col {
  flex: 1;
  padding: var(--space-3);
  font-size: var(--text-sm);
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 44px;
}

.comp-col.dim-col {
  flex: 0.6;
  font-weight: var(--weight-semibold);
  color: var(--gray-600);
}

.comp-col.score-col {
  flex: 0.8;
  text-align: center;
  justify-content: center;
}

.job-col {
  background: var(--gray-50);
  flex: 1.2;
}

.resume-col {
  background: var(--primary-50);
  flex: 1.2;
}

.score-pill {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: var(--weight-bold);
  color: var(--color-surface);
  background: var(--gray-700);
}

.row-matched { background: rgba(16, 185, 129, 0.03); }
.row-mismatched { background: rgba(239, 68, 68, 0.03); }
.match-icon { margin-left: var(--space-2); vertical-align: middle; }

.skills-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.missing-skills {
  margin-top: var(--space-2);
  padding: var(--space-2) var(--space-3);
  background: var(--danger-50);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.missing-label {
  font-size: var(--text-xs);
  color: var(--danger-500);
  font-weight: var(--weight-semibold);
}

.skill-coverage-bar { margin-top: var(--space-2); }

.coverage-text {
  font-size: var(--text-xs);
  color: var(--gray-400);
  margin-left: var(--space-2);
}

.suggestions-section {
  margin-top: var(--space-5);
  padding: var(--space-4);
  background: var(--gray-50);
  border-radius: var(--radius-md);
}

.suggestions-section h4 {
  margin: 0 0 var(--space-3);
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.suggestion-item {
  display: flex;
  gap: var(--space-3);
  align-items: flex-start;
  padding: var(--space-2) var(--space-3);
  background: var(--color-surface);
  border-radius: var(--radius-sm);
}

.suggestion-index {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--gray-700);
  color: var(--color-surface);
  font-size: 10px;
  font-weight: var(--weight-bold);
  display: flex;
  align-items: center;
  justify-content: center;
}

.suggestion-text {
  font-size: var(--text-sm);
  color: var(--gray-700);
  line-height: var(--leading-normal);
}

.ai-suggestion-box {
  padding: var(--space-4);
  background: var(--gray-50);
  border-radius: var(--radius-md);
  margin-top: var(--space-4);
}

.ai-suggestion-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--gray-700);
}

.ai-suggestion-header .el-icon {
  color: var(--color-primary);
}

.ai-suggestion-box p {
  margin: 0;
  line-height: var(--leading-relaxed);
  color: var(--gray-600);
  font-size: var(--text-sm);
}

/* ── 加分项明细 ────────────────────────────── */
.bonus-section {
  margin-top: var(--space-4);
  padding: var(--space-4);
  background: var(--success-50, #f0f9ff);
  border: 1px solid var(--success-200, #bbf7d0);
  border-radius: var(--radius-md);
}

.bonus-section h4 {
  margin: 0 0 var(--space-3);
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--success-700, #15803d);
}

.bonus-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.bonus-item {
  display: none;
}

.bonus-item.bonus-active {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: var(--color-surface);
  border: 1px solid var(--success-300, #86efac);
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
}

.bonus-label {
  color: var(--gray-700);
}

.bonus-val {
  color: var(--success-600, #16a34a);
  font-weight: var(--weight-semibold);
}

.invite-info {
  padding: var(--space-3);
  background: var(--gray-50);
  border-radius: var(--radius-md);
}

.invite-info p {
  margin: 4px 0;
  font-size: var(--text-sm);
  color: var(--gray-700);
}

.apply-job-info {
  padding: var(--space-3);
  background: var(--gray-50);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-4);
}

.apply-job-info h4 {
  margin: 0 0 var(--space-1);
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.apply-job-info p {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--gray-500);
}

.form-tip {
  font-size: var(--text-xs);
  color: var(--gray-400);
  margin-top: var(--space-1);
}

.form-tip a { color: var(--color-primary); text-decoration: none; }
.form-tip a:hover { text-decoration: underline; }

/* ── 响应式 ──────────────────────────────── */
@media (max-width: 768px) {
  .match-page {
    padding: var(--space-4);
  }
  .resume-summary {
    flex-direction: column;
  }
  .result-body {
    flex-direction: column;
    gap: var(--space-3);
  }
  .result-score {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }
  .result-actions {
    flex-direction: row;
    flex-wrap: wrap;
  }
  .dim-scores {
    grid-template-columns: 1fr;
  }
  .score-overview {
    flex-direction: column;
    gap: var(--space-4);
  }
}
</style>
