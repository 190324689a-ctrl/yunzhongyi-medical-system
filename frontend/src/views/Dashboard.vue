<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card class="stat"><div class="num">{{ stats.physicians }}</div><div class="label">在册医师</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat"><div class="num">{{ stats.depts }}</div><div class="label">科室</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat"><div class="num">{{ stats.auth }}</div><div class="label">手术分级已授权</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat"><div class="num">1 / 31</div><div class="label">模块已实现</div></el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header><span>快捷入口</span></template>
      <el-button type="primary" @click="$router.push('/physician')">医师档案管理</el-button>
      <el-button @click="$router.push('/leave')">休假执业管理</el-button>
      <el-button @click="$router.push('/schedule')">排班管理</el-button>
      <el-button @click="$router.push('/stats/work')">工作统计</el-button>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header><span>实现进度</span></template>
      <el-table :data="progress" style="width: 100%">
        <el-table-column prop="module" label="模块" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.done ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="说明" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import request from '../utils/request'
import { getPhysicians } from '../api/physician'

const stats = reactive({ physicians: 0, depts: 0, auth: 0 })

const progress = [
  { module: '医师档案管理', status: '已实现', done: true, note: '增删改查、分页、多条件查询、导入导出 Excel、手术分级与处方权' },
  { module: '休假执业管理', status: '待开发', done: false, note: '下一步：在线申请、多级审批、打印预览' },
  { module: '重点授权管理', status: '待开发', done: false, note: '手术/麻醉/抗肿瘤/精神类授权申请审批' },
  { module: '医师考核管理', status: '待开发', done: false, note: '定期考核、医德考评、自定义模板' },
  { module: '其余 27 个模块', status: '待开发', done: false, note: '见 README 模块清单与实现进度表' }
]

onMounted(async () => {
  try {
    const page = await getPhysicians({ page: 1, size: 1 })
    stats.physicians = page.total
    const authPage = await getPhysicians({ page: 1, size: 1, surgeryAuth: 1 })
    stats.auth = authPage.total
    const depts = await request.get('/depts/enabled')
    stats.depts = depts.length
  } catch (e) {
    // 后端未启动时工作台显示 0，不阻塞页面
    console.warn('工作台统计加载失败（后端未启动？）', e.message)
  }
})
</script>

<style scoped>
.stat {
  text-align: center;
}
.stat .num {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
}
.stat .label {
  color: #909399;
  margin-top: 4px;
}
</style>
