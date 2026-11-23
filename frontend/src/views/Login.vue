<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <h2 class="title">云中医医务系统</h2>
      <p class="subtitle">请登录后使用</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="登录账号" clearable @keyup.enter="onSubmit" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            clearable
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="error" />
        <el-form-item>
          <el-button type="primary" :loading="loading" class="submit" @click="onSubmit">登 录</el-button>
        </el-form-item>
      </el-form>
      <p class="hint">演示账号（密码均为 123456）：admin 系统管理员 / wuwenbin 医务科 / chen 科主任 / sunxiaohong 普通医师</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyMenus, login } from '../api/auth'
import { setMenus, setPermissions, setRoles, setToken, setUser } from '../utils/auth'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const error = ref('')

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  error.value = ''
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const data = await login(form)
    setToken(data.token)
    setUser(data.user)
    setRoles(data.roles)
    setPermissions(data.permissions)
    try {
      setMenus(await getMyMenus())
    } catch {
      // 菜单拉取失败不阻塞登录，Layout 会在挂载时用 /api/auth/me 补一次
    }
    ElMessage.success(`欢迎回来，${data.user?.realName || data.user?.username || ''}`)
    router.push(route.query.redirect || '/dashboard')
  } catch (e) {
    // 账号或密码错误、账号停用、锁定等均由后端返回中文提示
    error.value = e?.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
.login-card {
  width: 380px;
  padding: 8px 12px;
}
.title {
  text-align: center;
  margin: 8px 0 4px;
  color: #304156;
}
.subtitle {
  text-align: center;
  color: #909399;
  margin: 0 0 20px;
  font-size: 14px;
}
.submit {
  width: 100%;
}
.error {
  margin-bottom: 12px;
}
.hint {
  text-align: center;
  color: #909399;
  font-size: 13px;
  margin: 4px 0 0;
}
</style>
