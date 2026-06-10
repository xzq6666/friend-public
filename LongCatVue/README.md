# 智能招聘管理系统 - 前端项目

## 项目概述
基于 Vue 3 + Element Plus 的智能招聘管理系统前端，提供后台管理界面，支持简历管理、用户管理、数据统计等功能。

## 技术栈
- **框架**：Vue 3
- **UI组件库**：Element Plus
- **图标库**：@element-plus/icons-vue
- **状态管理**：Pinia
- **路由**：Vue Router（嵌套布局路由）
- **HTTP客户端**：Axios（统一拦截器）
- **构建工具**：Vite

## 项目结构
```
LongCatVue/
├── src/
│   ├── assets/            # 静态资源
│   ├── components/        # 通用组件（Sidebar侧边栏）
│   ├── layouts/           # 布局组件（AdminLayout后台布局）
│   ├── views/             # 页面组件
│   │   ├── Login.vue         # 登录页
│   │   ├── Register.vue      # 注册页（竖排表单 + 右侧宣传区）
│   │   ├── Dashboard.vue     # 仪表盘
│   │   ├── MyResume.vue      # 我的简历（求职者上传/编辑）
│   │   ├── ManageResumes.vue # 简历管理
│   │   ├── Users.vue         # 用户管理
│   │   ├── Profile.vue       # 个人中心
│   │   ├── Jobs.vue          # 职位浏览
│   │   └── Match.vue         # 智能匹配
│   ├── router/            # 路由配置（嵌套路由+路由守卫）
│   ├── stores/            # 状态管理（user）
│   ├── utils/             # 工具函数（request请求封装）
│   ├── App.vue            # 根组件
│   └── main.js            # 入口文件
├── vite.config.js         # Vite配置（API代理）
└── package.json           # 项目配置
```

## 路由结构

| 路径 | 页面 | 说明 |
|------|------|------|
| `/login` | Login | 独立登录页（无侧边栏） |
| `/register` | Register | 独立注册页（无侧边栏） |
| `/dashboard` | Dashboard | 管理仪表盘 |
| `/my-resume` | MyResume | 我的简历（求职者专用） |
| `/resumes` | ManageResumes | 简历管理 |
| `/users` | Users | 用户管理 |
| `/profile` | Profile | 个人中心 |

所有管理页面使用 `AdminLayout` 布局，包含左侧深色导航栏 + 顶部用户栏。

## 核心功能

### 1. 用户认证
- 用户登录（JWT认证）
- 路由守卫（未登录自动跳转登录页）
- Token 自动携带（请求拦截器）

### 2. 仪表盘
- 用户/简历/职位/匹配统计概览
- 最近注册用户列表
- 最近上传简历列表

### 3. 我的简历（求职者）
- 简历基本信息表单（姓名/年龄/学历/技能/经验/薪资）
- 附件简历上传（PDF/DOC）
- AI 智能分析
- 侧边栏仅对 EMPLOYEE 用户可见

### 4. 简历管理（管理员）
- 简历列表（分页、搜索）
- 简历详情查看
- AI智能分析
- 简历删除

### 5. 用户管理
- 用户列表（分页、搜索）
- 用户详情查看
- 启用/禁用用户

### 5. 个人中心
- 个人信息展示
- 数据概览统计

## 快速开始

### 环境要求
- Node.js 16+
- npm 7+

### 安装依赖
```bash
npm install
```

### 开发环境启动
```bash
npm run dev
```
默认端口 3000，已配置 `/api` 代理到 `http://localhost:8080`

### 生产环境构建
```bash
npm run build
```

## API对接

### 配置代理
在 `vite.config.js` 中：
```js
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 请求封装
使用 `src/utils/request.js` 进行统一的API请求处理：
- 自动从 localStorage 读取 token 并添加到 Authorization 头
- 401 响应自动清除 token 并跳转登录页
- 统一错误提示

## 后端 API 接口

### 认证
- `POST /api/auth/login` - 登录
- `POST /api/auth/register` - 注册

### 用户管理
- `GET /api/user/profile` - 获取当前用户信息
- `GET /api/user/list?page=&size=&keyword=` - 用户列表（分页）
- `GET /api/user/{id}` - 用户详情
- `PUT /api/user/{id}/status` - 更新用户状态

### 简历管理
- `GET /api/resume/my` - 获取当前用户的简历
- `POST /api/resume` - 创建简历（自动关联当前用户）
- `GET /api/resume/page?page=&size=&keyword=` - 简历列表（分页）
- `GET /api/resume/{id}` - 简历详情
- `PUT /api/resume/{id}` - 更新简历
- `DELETE /api/resume/{id}` - 删除简历
- `POST /api/resume/{id}/ai-analyze` - AI分析

### 统计
- `GET /api/admin/stats` - 仪表盘统计数据

## 浏览器支持
- Chrome 80+
- Firefox 75+
- Safari 13+
- Edge 80+
