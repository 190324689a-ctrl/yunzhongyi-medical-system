<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">云中医医务系统</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/dashboard">
          <span>工作台</span>
        </el-menu-item>
        <el-sub-menu v-for="g in visibleMenuGroups" :key="g.title" :index="g.title">
          <template #title>
            <span>{{ g.title }}</span>
          </template>
          <el-menu-item v-for="m in g.children" :key="m.path" :index="m.path">
            <span>{{ m.name }}</span>
            <el-tag v-if="m.done" type="success" size="small" style="margin-left: 6px">已实现</el-tag>
            <el-tag v-else type="info" size="small" style="margin-left: 6px">待开发</el-tag>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ $route.meta.title || '工作台' }}</span>
        <span class="user">
          {{ currentUser?.realName || currentUser?.username || '' }}
          <span v-if="currentUser?.deptName" class="meta">（{{ currentUser.deptName }}</span><span v-if="roleNames" class="meta"> · {{ roleNames }}</span><span v-if="currentUser?.deptName" class="meta">）</span>
          <el-button type="text" @click="onLogout">退出登录</el-button>
        </span>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { menuGroups } from '../router'
import { getMe, logout } from '../api/auth'
import { clearAuth, getPermissions, getRoles, getUser, setPermissions, setRoles, setUser } from '../utils/auth'

const router = useRouter()
const currentUser = ref(getUser())
const roles = ref(getRoles())

/** 左侧菜单按当前用户权限过滤：无权限的页面不显示；未加载到权限（历史登录态）时先全显 */
const visibleMenuGroups = computed(() => {
  const permissions = getPermissions()
  const filter = (m) => !m.permission || !permissions.length || permissions.includes(m.permission)
  return menuGroups
    .map((g) => ({ ...g, children: g.children.filter(filter) }))
    .filter((g) => g.children.length > 0)
})

/** 角色中文名（ADMIN 显示为系统管理员，其余按角色编码直显） */
const roleNames = computed(() =>
  roles.value
    .map((r) => ({ ADMIN: '系统管理员', MEDICAL_ADMIN: '医务科管理员', DEPT_HEAD: '科主任', DOCTOR: '医师' })[r] || r)
    .join('、')
)

async function onLogout() {
  try {
    await logout()
  } catch {
    // 登出接口失败也不阻塞本地清登录态
  }
  clearAuth()
  router.push('/login')
}

onMounted(async () => {
  // 刷新后权限未加载时，从后端实时取一次（角色/权限/科室名），菜单随之过滤
  if (!getPermissions().length) {
    try {
      const data = await getMe()
      if (data?.user) setUser(data.user)
      if (data?.roles) {
        setRoles(data.roles)
        roles.value = data.roles
      }
      if (data?.permissions) setPermissions(data.permissions)
      currentUser.value = getUser()
    } catch {
      // 取不到也不阻塞页面
    }
  }
})
</script>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background-color: #304156;
  overflow-y: auto;
}
.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #fff;
  font-size: 17px;
  font-weight: bold;
  background-color: #2b3a4b;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
}
.header .title {
  font-size: 17px;
  font-weight: bold;
}
.header .user {
  color: #666;
  font-size: 14px;
}
.header .user .meta {
  color: #909399;
  font-size: 13px;
}
.main {
  background: #f0f2f5;
  padding: 16px;
}
</style>
