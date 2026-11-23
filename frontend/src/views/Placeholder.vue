<template>
  <el-card>
    <el-empty :description="`${$route.meta.title || '本模块'} 尚未开发`">
      <template #image>
        <el-icon :size="64" color="#909399"><Document /></el-icon>
      </template>
      <p style="color: #909399; font-size: 14px; margin: 8px 0">
        该模块在需求清单中，待按 README 的模块顺序实现。
      </p>
      <el-button type="primary" @click="$router.push('/physician')">先去医师档案管理</el-button>
    </el-empty>
  </el-card>
</template>

<script setup>
import { Document } from '@element-plus/icons-vue'
</script>
