import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'
import Placeholder from '../views/Placeholder.vue'
import Dashboard from '../views/Dashboard.vue'
import PhysicianList from '../views/physician/PhysicianList.vue'
import LeaveList from '../views/leave/LeaveList.vue'
import AuthorizationList from '../views/authorization/AuthorizationList.vue'
import AssessmentList from '../views/assessment/AssessmentList.vue'
import ScheduleList from '../views/schedule/ScheduleList.vue'
import MdtList from '../views/mdt/MdtList.vue'
import QualityInspectionList from '../views/quality/QualityInspectionList.vue'
import NewTechList from '../views/newtech/NewTechList.vue'
import PersonnelChangeList from '../views/personnel/PersonnelChangeList.vue'
import RadiationList from '../views/radiation/RadiationList.vue'
import CommitteeList from '../views/committee/CommitteeList.vue'
import DocumentList from '../views/document/DocumentList.vue'
import PolicyList from '../views/policy/PolicyList.vue'
import EthicsList from '../views/ethics/EthicsList.vue'
import CoreSystemList from '../views/quality/CoreSystemList.vue'
import RectificationSummaryList from '../views/quality/RectificationSummaryList.vue'
import TechGradingList from '../views/quality/TechGradingList.vue'
import InspectionMaintainList from '../views/quality/InspectionMaintainList.vue'
import InspectionCategoryList from '../views/quality/InspectionCategoryList.vue'
import TemplateLibraryList from '../views/quality/TemplateLibraryList.vue'
import MedGradingList from '../views/quality/MedGradingList.vue'
import CompletionList from '../views/statistics/CompletionList.vue'
import StatisticsList from '../views/statistics/StatisticsList.vue'
import BriefingList from '../views/briefing/BriefingList.vue'
import NotificationList from '../views/notification/NotificationList.vue'
import MobileList from '../views/mobile/MobileList.vue'
import SystemSettingsList from '../views/system/SystemSettingsList.vue'
import SystemBaseList from '../views/system/SystemBaseList.vue'
import Login from '../views/Login.vue'
import { getToken, hasPermission } from '../utils/auth'

/**
 * 路由与左侧菜单：覆盖需求清单全部 31 个模块。
 * 已实现：工作台、医师档案管理、休假执业管理、重点授权管理、医师考核管理、医疗人员动态管理、放射人员管理、委员会管理、文件制度管理、政策法规管理、医德医风管理、核心制度管理、检查整改汇总、医疗技术分级管理、检查维护、检查分类、检查模板管理、医疗分级管理、排班管理、MDT 管理、质量检查管理、新技术新项目管理、工作统计、三级医院医疗质量指标统计、医疗质量统计、医务系统完成统计、医务系统设置、消息提醒、医疗工作简报、医务管理移动端（后四者共用统计分析页，按路由切换页签；完成统计为独立页；系统设置为总览 + 五组配置页签，底座六项共用通用管理页，按路由切换 kind；消息提醒为站内消息 / 待办提醒 / 消息推送 / 短信提醒四个页签；医疗工作简报为简报 / 简报模板两个页签；移动端为待审批 / 移动端设备 / 审批记录 / 移动端推送 / 移动端查看五个页签）；其余显示占位页，按 README 的模块顺序逐个实现。
 */
export const menuGroups = [
  {
    title: '医师管理',
    children: [
      { path: '/physician', name: '医师档案管理', component: PhysicianList, done: true , permission: 'physician:list' },
      { path: '/leave', name: '休假执业管理', component: LeaveList, done: true , permission: 'leave:list' },
      { path: '/authorization', name: '重点授权管理', component: AuthorizationList, done: true , permission: 'authorization:list' },
      { path: '/assessment', name: '医师考核管理', component: AssessmentList, done: true , permission: 'assessment:list' },
      { path: '/staff-dynamic', name: '医疗人员动态管理', component: PersonnelChangeList, done: true , permission: 'personnel-change:list' },
      { path: '/radiation', name: '放射人员管理', component: RadiationList, done: true , permission: 'radiation:list' }
    ]
  },
  {
    title: '排班与会诊',
    children: [
      { path: '/schedule', name: '排班管理', component: ScheduleList, done: true , permission: 'schedule:list' },
      { path: '/mdt', name: 'MDT 管理', component: MdtList, done: true , permission: 'mdt:list' }
    ]
  },
  {
    title: '质量管理',
    children: [
      { path: '/quality-inspection', name: '质量检查管理', component: QualityInspectionList, done: true , permission: 'quality:list' },
      { path: '/quality/self-check', name: '质量检查与自查', component: Placeholder , permission: 'quality:list' },
      { path: '/quality/hospital', name: '院级质量检查', component: Placeholder , permission: 'quality:list' },
      { path: '/quality/special', name: '专项质量检查', component: Placeholder , permission: 'quality:list' },
      { path: '/quality/core', name: '核心制度', component: CoreSystemList, done: true , permission: 'core-system:list' },
      { path: '/quality/rectification', name: '检查整改汇总', component: RectificationSummaryList, done: true , permission: 'rectification:list' },
      { path: '/quality/tech-grading', name: '医疗技术分级管理', component: TechGradingList, done: true , permission: 'tech-grading:list' },
      { path: '/quality/maintain', name: '检查维护', component: InspectionMaintainList, done: true , permission: 'inspection:list' },
      { path: '/quality/category', name: '检查分类', component: InspectionCategoryList, done: true , permission: 'inspection-category:list' },
      { path: '/quality/template', name: '检查模板', component: TemplateLibraryList, done: true , permission: 'template-library:list' },
      { path: '/quality/grading', name: '医疗分级管理', component: MedGradingList, done: true , permission: 'med-grading:list' }
    ]
  },
  {
    title: '组织与制度',
    children: [
      { path: '/committee', name: '委员会管理', component: CommitteeList, done: true , permission: 'committee:list' },
      { path: '/document', name: '文件制度管理', component: DocumentList, done: true , permission: 'document:list' },
      { path: '/policy', name: '政策法规管理', component: PolicyList, done: true , permission: 'policy:list' },
      { path: '/ethics', name: '医德医风管理', component: EthicsList, done: true , permission: 'ethics:list' }
    ]
  },
  {
    title: '新技术',
    children: [
      { path: '/new-tech', name: '新技术、新项目管理', component: NewTechList, done: true , permission: 'newtech:list' }
    ]
  },
  {
    title: '统计分析',
    children: [
      { path: '/stats/work', name: '工作统计', component: StatisticsList, done: true , permission: 'statistics:list' },
      { path: '/stats/tertiary', name: '三级医院医疗质量指标统计', component: StatisticsList, done: true , permission: 'statistics:list' },
      { path: '/stats/quality', name: '医疗质量统计', component: StatisticsList, done: true , permission: 'statistics:list' },
      { path: '/stats/completion', name: '医务系统完成统计', component: CompletionList, done: true , permission: 'completion:list' },
      { path: '/briefing', name: '医疗工作简报', component: BriefingList, done: true , permission: 'briefing:list' }
    ]
  },
  {
    title: '系统',
    children: [
      { path: '/notice', name: '消息提醒', component: NotificationList, done: true , permission: 'notification:list' },
      { path: '/mobile', name: '医务管理移动端', component: MobileList, done: true , permission: 'mobile:list' },
      { path: '/system/settings', name: '医务系统设置', component: SystemSettingsList, done: true , permission: 'system-settings:list' },
      { path: '/system/user', name: '用户管理', component: SystemBaseList, kind: 'user', done: true , permission: 'user:list' },
      { path: '/system/role', name: '角色管理', component: SystemBaseList, kind: 'role', done: true , permission: 'role:list' },
      { path: '/system/permission', name: '权限管理', component: SystemBaseList, kind: 'permission', done: true , permission: 'system:setting' },
      { path: '/system/dept', name: '科室管理', component: SystemBaseList, kind: 'dept', done: true , permission: 'dept:list' },
      { path: '/system/dict', name: '字典管理', component: SystemBaseList, kind: 'dict', done: true , permission: 'dict:list' },
      { path: '/system/log', name: '操作日志', component: SystemBaseList, kind: 'log', done: true , permission: 'log:list' }
    ]
  }
]

const routes = [
  { path: '/login', name: '登录', component: Login },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: '/dashboard', name: '工作台', component: Dashboard },
      ...menuGroups.flatMap((g) =>
        g.children.map((m) => ({
          path: m.path,
          name: m.name,
          component: m.component,
          meta: { title: m.name, done: !!m.done, kind: m.kind, permission: m.permission }
        }))
      )
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录只能进登录页；已登录访问登录页时回工作台；
// 无权限的页面（meta.permission 不在当前用户权限里）直接回工作台，进不去
router.beforeEach((to) => {
  const token = getToken()
  if (to.path === '/login') {
    return token ? '/dashboard' : true
  }
  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.permission && !hasPermission(to.meta.permission)) {
    return '/dashboard'
  }
  return true
})

export default router
