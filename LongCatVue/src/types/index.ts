/* ============================================
   智能招聘匹配平台 — 全局类型定义
   ============================================ */

// ── 用户类型 ─────────────────────────────────
export type UserType = 'EMPLOYEE' | 'EMPLOYER' | 'ADMIN'

export interface UserInfo {
  id: number
  username: string
  email: string
  userType: UserType
  avatar?: string
  createdAt?: string
}

// ── 简历类型 ─────────────────────────────────
export interface Resume {
  id: number
  name: string
  phone: string
  email: string
  age: number
  education: string
  categoryId: number | null
  skills: string
  experience: string
  expectedSalary: number | null
  workExperience: WorkExperienceItem[]
  selfIntroduction: string
  fileUrl?: string
  aiAnalysis?: string
  resumeName?: string
  isDefault?: number
  note?: string
  userId?: number
  createdAt?: string
  updatedAt?: string
}

export interface WorkExperienceItem {
  company: string
  position: string
  startDate: string
  endDate: string
  current: boolean
  description: string
}

// ── 简历上传相关 ─────────────────────────────
export interface ResumeUploadResult {
  id: number
  name: string
  phone: string
  email: string
  age: number
  education: string
  categoryId: number | null
  skills: string
  experience: string
  expectedSalary: number | null
  workExperience: string
  selfIntroduction: string
  fileUrl: string
  aiAnalysis?: string
  message?: string
}

export interface ResumeParseResult {
  name: string
  phone: string
  email: string
  age: number
  education: string
  categoryId: number | null
  skills: string
  experience: string
  expectedSalary: number | null
  workExperience: WorkExperienceItem[]
  selfIntroduction: string
}

export type UploadPhase =
  | 'idle'
  | 'uploading'
  | 'parsing'
  | 'analyzing'
  | 'success'
  | 'error'

export interface UploadProgress {
  phase: UploadPhase
  percent: number
  message: string
}

// ── AI 分析结果 ──────────────────────────────
export interface AiAnalysisResult {
  total_score: number
  score_level: string
  radar_data: RadarData
  dimensions: Record<string, AnalysisDimension>
  strengths: string[]
  weaknesses: string[]
  competitive_analysis: CompetitiveAnalysis
  career_suggestions: CareerSuggestions
  learning_plan: LearningPlan
  interview_tips: InterviewTips
  overall_comment: string
}

export interface RadarData {
  labels: string[]
  values: number[]
  max_values: number[]
}

export interface AnalysisDimension {
  score: number
  max: number
  comment?: string
  sub_scores?: SubScore[]
}

export interface SubScore {
  item: string
  score: number
  max: number
  comment: string
}

export interface CompetitiveAnalysis {
  market_position: string
  core_competitiveness: string
  improvement_potential: string
}

export interface CareerSuggestions {
  recommended_positions: string[]
  salary_range: string
  development_path: string
}

export interface LearningPlan {
  short_term: PlanItem[]
  long_term: PlanItem[]
}

export interface PlanItem {
  skill: string
  description: string
  priority: string
}

export interface InterviewTips {
  focus_areas: string[]
  suggested_questions: string[]
}

// ── 简历列表查询参数 ─────────────────────────
export interface ResumeListParams {
  page?: number
  size?: number
  keyword?: string
}

// ── 通用分页结果 ─────────────────────────────
export interface PageResult<T> {
  data: T[]
  total: number
  page: number
  size: number
  pages: number
}

// ── 通用 API 响应 ────────────────────────────
export interface ApiResponse<T = unknown> {
  code: number
  data: T
  message: string
}

// ── 数据分析相关（已有） ─────────────────────
export interface StatsData {
  totalUsers: number
  totalResumes: number
  totalJobs: number
  totalMatches: number
  todayUsers: number
  todayResumes: number
  todayJobs: number
  todayMatches: number
  activeUsers7d: number
  userTypeDistribution?: {
    admin: number
    employer: number
    employee: number
  }
}

export interface TrendData {
  trends: TrendItem[]
  period: string
}

export interface TrendItem {
  date: string
  users: number
  resumes: number
  jobs: number
  matches: number
}

export interface SkillData {
  skill: string
  count: number
}

export interface JobData {
  id: number
  title: string
  company: string
  matchCount?: number
}
