<template>
  <div>
    <p class="page-note">{{ kindNote }}</p>

    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6" v-for="s in statCards" :key="s.label">
        <el-card><div class="stat"><div class="num">{{ s.value }}</div><div class="label">{{ s.label }}</div></div></el-card>
      </el-col>
    </el-row>

    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键字" v-if="kind !== 'log'">
          <el-input v-model="query.keyword" :placeholder="keywordPlaceholder" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="科室" v-if="kind === 'user'">
          <el-select v-model="query.deptId" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="d in enabledDepts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" v-if="kind === 'dept'">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="t in deptTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="字典" v-if="kind === 'dict'">
          <el-select v-model="query.dictCode" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="t in dictTypes" :key="t.code" :label="t.name" :value="t.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="模块" v-if="kind === 'log'">
          <el-input v-model="query.module" placeholder="如 physician" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="操作人" v-if="kind === 'log'">
          <el-input v-model="query.username" placeholder="操作人" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="结果" v-if="kind === 'log'">
          <el-select v-model="query.result" placeholder="全部" clearable style="width: 110px">
            <el-option :value="1" label="成功" /><el-option :value="0" label="失败" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" v-if="kind === 'user' || kind === 'role' || kind === 'dept'">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <div class="toolbar">
        <div><el-button type="primary" v-if="kind !== 'log'" @click="handleAdd">{{ addButtonText }}</el-button></div>
        <div>
          <el-upload v-if="kind === 'user' || kind === 'dept' || kind === 'dict'" :action="importUrl" :show-file-list="false" accept=".xlsx,.xls" :on-success="handleImportSuccess" :on-error="handleImportError" style="display: inline-block; margin: 0 8px">
            <el-button type="success">批量导入</el-button>
          </el-upload>
          <el-button type="warning" v-if="kind !== 'role' && kind !== 'permission'" @click="handleExport">导出 Excel</el-button>
          <el-button v-if="kind === 'user' || kind === 'dept' || kind === 'dict'" @click="handleDownloadTemplate">下载模板</el-button>
        </div>
      </div>

      <!-- 用户表 -->
      <el-table v-if="kind === 'user'" :data="tableData" border stripe v-loading="loading" style="margin-top: 12px">
        <el-table-column prop="username" label="登录账号" width="140" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="所属科室" width="130"><template #default="{ row }">{{ deptNameOf(row.deptId) }}</template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" v-if="row.status !== 1" @click="toggleStatus(row, 1)">启用</el-button>
            <el-button link type="warning" v-if="row.status === 1" @click="toggleStatus(row, 0)">停用</el-button>
            <el-button link type="primary" @click="handleAssignRoles(row)">分配角色</el-button>
            <el-button link type="warning" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 角色表 -->
      <el-table v-if="kind === 'role'" :data="tableData" border stripe v-loading="loading" style="margin-top: 12px">
        <el-table-column prop="code" label="角色编码" width="180" />
        <el-table-column prop="name" label="角色名称" width="160" />
        <el-table-column label="绑定科室" width="220"><template #default="{ row }">{{ roleDeptNames[row.id] || '未绑定' }}</template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" v-if="row.status !== 1" @click="toggleStatus(row, 1)">启用</el-button>
            <el-button link type="warning" v-if="row.status === 1" @click="toggleStatus(row, 0)">停用</el-button>
            <el-button link type="primary" @click="handleAssignPermissions(row)">分配权限</el-button>
            <el-button link type="primary" @click="handleAssignDepts(row)">绑定部门</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 权限表 -->
      <el-table v-if="kind === 'permission'" :data="tableData" border stripe v-loading="loading" style="margin-top: 12px">
        <el-table-column prop="name" label="权限名称" width="180" />
        <el-table-column prop="code" label="权限标识" width="200" />
        <el-table-column label="类型" width="90"><template #default="{ row }">{{ permissionTypeText(row.type) }}</template></el-table-column>
        <el-table-column prop="path" label="路由路径" width="200" />
        <el-table-column prop="component" label="组件路径" width="240" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" v-if="row.status !== 1" @click="toggleStatus(row, 1)">启用</el-button>
            <el-button link type="warning" v-if="row.status === 1" @click="toggleStatus(row, 0)">停用</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 科室表（树形：按 parentId 嵌套展示层级） -->
      <el-table v-if="kind === 'dept'" :data="tableData" row-key="id" :tree-props="{ children: 'children' }" default-expand-all border stripe v-loading="loading" style="margin-top: 12px">
        <el-table-column prop="name" label="科室名称" width="200" />
        <el-table-column prop="code" label="科室编码" width="110" />
        <el-table-column label="科室类型" width="100"><template #default="{ row }">{{ deptTypeText(row.type) }}</template></el-table-column>
        <el-table-column prop="director" label="科主任" width="110" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" v-if="row.status !== 1" @click="toggleStatus(row, 1)">启用</el-button>
            <el-button link type="warning" v-if="row.status === 1" @click="toggleStatus(row, 0)">停用</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 字典表 -->
      <el-table v-if="kind === 'dict'" :data="tableData" border stripe v-loading="loading" style="margin-top: 12px">
        <el-table-column prop="dictCode" label="字典编码" width="170" />
        <el-table-column prop="dictValue" label="数据值" width="150" />
        <el-table-column prop="label" label="显示标签" width="150" />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 日志表 -->
      <el-table v-if="kind === 'log'" :data="tableData" border stripe v-loading="loading" style="margin-top: 12px">
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="130" />
        <el-table-column prop="operation" label="操作" width="110" />
        <el-table-column prop="method" label="请求方法" width="220" />
        <el-table-column prop="params" label="参数摘要" width="260" />
        <el-table-column label="结果" width="90"><template #default="{ row }"><el-tag :type="row.result === 1 ? 'success' : 'danger'">{{ row.result === 1 ? '成功' : '失败' }}</el-tag></template></el-table-column>
        <el-table-column prop="costMs" label="耗时（毫秒）" width="120" />
        <el-table-column prop="errorMsg" label="错误信息" />
        <el-table-column prop="createTime" label="操作时间" width="180" />
      </el-table>

      <el-pagination v-if="kind !== 'permission' && kind !== 'dept'" style="margin-top: 16px; justify-content: flex-end" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchList" @current-change="fetchList" />
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form :model="form" label-width="110px">
        <template v-if="kind === 'user'">
          <el-form-item label="登录账号"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
          <el-form-item label="真实姓名"><el-input v-model="form.realName" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="所属科室">
            <el-select v-model="form.deptId" placeholder="请选择" clearable style="width: 100%">
              <el-option v-for="d in enabledDepts" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="密码" v-if="!form.id"><el-input v-model="form.password" placeholder="不填默认 123456" show-password /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
        </template>
        <template v-if="kind === 'role'">
          <el-form-item label="角色编码"><el-input v-model="form.code" :disabled="!!form.id" placeholder="如 DOCTOR" /></el-form-item>
          <el-form-item label="角色名称"><el-input v-model="form.name" /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
        </template>
        <template v-if="kind === 'permission'">
          <el-form-item label="权限名称"><el-input v-model="form.name" /></el-form-item>
          <el-form-item label="权限标识"><el-input v-model="form.code" :disabled="!!form.id" placeholder="如 physician:list" /></el-form-item>
          <el-form-item label="类型">
            <el-select v-model="form.type" style="width: 100%">
              <el-option :value="1" label="目录" /><el-option :value="2" label="菜单" /><el-option :value="3" label="按钮" />
            </el-select>
          </el-form-item>
          <el-form-item label="父级 id"><el-input-number v-model="form.parentId" :min="0" style="width: 100%" /></el-form-item>
          <el-form-item label="路由路径"><el-input v-model="form.path" /></el-form-item>
          <el-form-item label="组件路径"><el-input v-model="form.component" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
            </el-select>
          </el-form-item>
        </template>
        <template v-if="kind === 'dept'">
          <el-form-item label="科室名称"><el-input v-model="form.name" /></el-form-item>
          <el-form-item label="科室编码"><el-input v-model="form.code" placeholder="如 ZY-NK" /></el-form-item>
          <el-form-item label="上级科室">
            <el-select v-model="form.parentId" placeholder="顶级科室" clearable style="width: 100%">
              <el-option :value="0" label="顶级科室（无上级）" />
              <el-option v-for="d in parentDeptOptions" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="科室类型">
            <el-select v-model="form.type" style="width: 100%">
              <el-option v-for="t in deptTypes" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="科主任"><el-input v-model="form.director" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
            </el-select>
          </el-form-item>
        </template>
        <template v-if="kind === 'dict'">
          <el-form-item label="字典编码"><el-input v-model="form.dictCode" :disabled="!!form.id" placeholder="如 title" /></el-form-item>
          <el-form-item label="数据值"><el-input v-model="form.dictValue" /></el-form-item>
          <el-form-item label="显示标签"><el-input v-model="form.label" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
            </el-select>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色弹窗 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="480px">
      <el-checkbox-group v-model="selectedRoleIds">
        <el-checkbox v-for="r in enabledRoles" :key="r.id" :label="r.id" style="display: block; margin: 6px 0">{{ r.name }}（{{ r.code }}）</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignRoles">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限弹窗（树形勾选：目录勾选即含全部下级菜单） -->
    <el-dialog v-model="permDialogVisible" title="分配权限" width="480px">
      <el-tree
        v-if="permDialogVisible"
        ref="permTreeRef"
        :data="permTree"
        :props="{ children: 'children', label: 'label' }"
        node-key="id"
        show-checkbox
        default-expand-all
        :default-checked-keys="selectedPermIds"
        style="max-height: 420px; overflow-y: auto"
      />
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignPermissions">保存</el-button>
      </template>
    </el-dialog>

    <!-- 绑定部门弹窗（树形勾选：角色绑定的医院科室范围） -->
    <el-dialog v-model="deptDialogVisible" title="绑定部门" width="480px">
      <p style="color: #909399; font-size: 13px; margin: 0 0 12px">勾选该角色归属的科室；登录后可在「我的」信息里看到绑定范围。</p>
      <el-tree
        v-if="deptDialogVisible"
        ref="deptTreeRef"
        :data="deptTree"
        :props="{ children: 'children', label: 'name' }"
        node-key="id"
        show-checkbox
        default-expand-all
        :default-checked-keys="selectedDeptIds"
        style="max-height: 420px; overflow-y: auto"
      />
      <template #footer>
        <el-button @click="deptDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignDepts">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getEnabledDepts, getDeptTree,
  getUsers, getUserStats, createUser, updateUser, deleteUser, enableUser, disableUser,
  resetUserPassword, getUserRoles, assignUserRoles, exportUsers, downloadUserTemplate,
  getRoles, getRoleStats, getEnabledRoles, createRole, updateRole, deleteRole, enableRole,
  disableRole, getRolePermissions, assignRolePermissions, getRoleDepts, assignRoleDepts,
  getPermissions, getPermissionStats, createPermission, updatePermission, deletePermission,
  enablePermission, disablePermission,
  getDepts, getDeptStats, createDept, updateDept, deleteDept, enableDept, disableDept,
  exportDepts, downloadDeptTemplate,
  getDictData, getDictStats, getDictTypes, createDictData, updateDictData, deleteDictData,
  exportDictData, downloadDictDataTemplate,
  getOperLogs, getOperLogStats, exportOperLogs
} from '../../api/system'

const route = useRoute()
const kind = computed(() => route.meta.kind || 'user')

const deptTypes = [
  { value: 'clinical', label: '临床' },
  { value: 'medical-technical', label: '医技' },
  { value: 'admin', label: '行政' },
  { value: 'outpatient', label: '门诊' }
]

const kindNote = computed(() => ({
  user: '用户管理：登录账号全院唯一；新增用户不填密码时默认 123456；改密码请用“重置密码”；删除用户时同时删除其角色分配。',
  role: '角色管理：角色编码全院唯一；分配权限用树形勾选（目录勾选即含下级菜单），登录后只显示有权限的页面；绑定部门表示角色归属的医院科室范围；删除角色时同时删除其权限分配、科室绑定与用户分配。',
  permission: '权限管理：权限标识全院唯一；有子权限的不可直接删除。',
  dept: '科室管理：科室编码全院唯一；按上级科室树形展示层级；有下级科室的不可停用、不可直接删除，改编码时须无下级科室。',
  dict: '字典管理：同一字典下数据值唯一；字典类型见下拉框。',
  log: '操作日志：只读，可按模块 / 操作人 / 结果筛选，可导出 Excel，不提供导入。'
}[kind.value] || ''))

const keywordPlaceholder = computed(() => ({
  user: '登录账号 / 真实姓名',
  role: '角色名称 / 编码',
  permission: '权限名称 / 标识',
  dept: '科室名称 / 编码',
  dict: '显示标签 / 数据值'
}[kind.value] || ''))

const addButtonText = computed(() => ({
  user: '新增用户',
  role: '新增角色',
  permission: '新增权限',
  dept: '新增科室',
  dict: '新增字典数据项'
}[kind.value] || '新增'))

const importUrl = computed(() => ({
  user: '/api/users/import',
  dept: '/api/depts/import',
  dict: '/api/dicts/import'
}[kind.value] || ''))

function permissionTypeText(v) {
  return { 1: '目录', 2: '菜单', 3: '按钮' }[v] || ''
}
function deptTypeText(v) {
  return (deptTypes.find((t) => t.value === v) || {}).label || v || ''
}
/** 按 parentId 把平铺列表组装成树（父级缺失或自指时当顶级，避免死循环） */
function buildTree(list, labelKey = 'name') {
  const map = new Map()
  list.forEach((d) => map.set(d.id, { ...d, label: d[labelKey], children: [] }))
  const roots = []
  list.forEach((d) => {
    const node = map.get(d.id)
    if (d.parentId && d.parentId !== d.id && map.has(d.parentId)) {
      map.get(d.parentId).children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}
/** 科室 id → 名称（用户表所属科室列用） */
function deptNameOf(deptId) {
  if (!deptId) return ''
  return (enabledDepts.value.find((d) => d.id === deptId) || {}).name || `科室 ${deptId}`
}
/** 新增/编辑科室时上级科室下拉：排除自己，避免自指 */
const parentDeptOptions = computed(() =>
  enabledDepts.value.filter((d) => d.id !== form.id)
)
function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
function handleImportError() {
  ElMessage.error('导入失败，请检查文件格式')
}

// ---------- 统计卡片 ----------
const stats = ref({})
const statCards = computed(() => {
  if (kind.value === 'user') {
    return [
      { label: '用户总数', value: stats.value.total || 0 },
      { label: '已启用', value: stats.value.enabled || 0 },
      { label: '已停用', value: stats.value.disabled || 0 }
    ]
  }
  if (kind.value === 'role') {
    return [
      { label: '角色总数', value: stats.value.total || 0 },
      { label: '已启用', value: stats.value.enabled || 0 },
      { label: '已停用', value: stats.value.disabled || 0 }
    ]
  }
  if (kind.value === 'permission') {
    return [
      { label: '权限总数', value: stats.value.total || 0 },
      { label: '已启用', value: stats.value.enabled || 0 },
      { label: '目录', value: (stats.value.byType || {}).目录 || 0 },
      { label: '菜单', value: (stats.value.byType || {}).菜单 || 0 },
      { label: '按钮', value: (stats.value.byType || {}).按钮 || 0 }
    ]
  }
  if (kind.value === 'dept') {
    return [
      { label: '科室总数', value: stats.value.total || 0 },
      { label: '已启用', value: stats.value.enabled || 0 },
      { label: '已停用', value: stats.value.disabled || 0 }
    ]
  }
  if (kind.value === 'dict') {
    return [
      { label: '字典类型数', value: stats.value.dictTypes || 0 },
      { label: '数据项总数', value: stats.value.total || 0 },
      { label: '已启用', value: stats.value.enabled || 0 },
      { label: '已停用', value: stats.value.disabled || 0 }
    ]
  }
  return [
    { label: '日志总数', value: stats.value.total || 0 },
    { label: '成功', value: stats.value.success || 0 },
    { label: '失败', value: stats.value.failed || 0 }
  ]
})

async function fetchStats() {
  try {
    if (kind.value === 'user') stats.value = await getUserStats()
    else if (kind.value === 'role') stats.value = await getRoleStats()
    else if (kind.value === 'permission') stats.value = await getPermissionStats()
    else if (kind.value === 'dept') stats.value = await getDeptStats()
    else if (kind.value === 'dict') stats.value = await getDictStats()
    else if (kind.value === 'log') stats.value = await getOperLogStats()
  } catch (e) {
    ElMessage.error('加载统计失败：' + e.message)
  }
}

// ---------- 列表 ----------
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', deptId: '', type: '', dictCode: '', module: '', username: '', result: '', status: '', page: 1, size: 10 })
const enabledDepts = ref([])
const dictTypes = ref([])

async function fetchList() {
  loading.value = true
  try {
    if (kind.value === 'user') {
      const data = await getUsers({ ...query })
      tableData.value = data.records || []
      total.value = data.total || 0
    } else if (kind.value === 'role') {
      const data = await getRoles({ ...query })
      tableData.value = data.records || []
      total.value = data.total || 0
      // 角色列表附带各角色绑定的科室名
      const names = {}
      await Promise.all(
        tableData.value.map(async (r) => {
          try {
            names[r.id] = (await getRoleDepts(r.id)).map((d) => d.name).join('、')
          } catch {
            names[r.id] = ''
          }
        })
      )
      roleDeptNames.value = names
    } else if (kind.value === 'permission') {
      tableData.value = await getPermissions()
      total.value = tableData.value.length
    } else if (kind.value === 'dept') {
      // 树形展示需一次取全部科室（当前 11 个，取 200 条足够）
      const data = await getDepts({ ...query, page: 1, size: 200 })
      tableData.value = buildTree(data.records || [])
      total.value = data.total || 0
    } else if (kind.value === 'dict') {
      const data = await getDictData({ ...query })
      tableData.value = data.records || []
      total.value = data.total || 0
    } else if (kind.value === 'log') {
      const data = await getOperLogs({ ...query })
      tableData.value = data.records || []
      total.value = data.total || 0
    }
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    loading.value = false
  }
}
function handleReset() {
  query.keyword = ''
  query.deptId = ''
  query.type = ''
  query.dictCode = ''
  query.module = ''
  query.username = ''
  query.result = ''
  query.status = ''
  query.page = 1
  fetchList()
}

// ---------- 新增 / 编辑 ----------
const dialogVisible = ref(false)
const dialogTitle = ref('新增')
const form = reactive({})

function defaultForm() {
  if (kind.value === 'user') return { id: null, username: '', realName: '', phone: '', deptId: null, password: '', status: 1, remark: '' }
  if (kind.value === 'role') return { id: null, code: '', name: '', status: 1, remark: '' }
  if (kind.value === 'permission') return { id: null, parentId: 0, name: '', code: '', type: 2, path: '', component: '', sortOrder: 0, status: 1 }
  if (kind.value === 'dept') return { id: null, parentId: 0, name: '', code: '', type: 'clinical', director: '', phone: '', sortOrder: 0, status: 1 }
  if (kind.value === 'dict') return { id: null, dictCode: '', dictValue: '', label: '', sortOrder: 0, status: 1 }
  return { id: null }
}
function resetForm() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, defaultForm())
}
function handleAdd() {
  resetForm()
  dialogTitle.value = addButtonText.value
  dialogVisible.value = true
}
function handleEdit(row) {
  resetForm()
  Object.assign(form, row)
  dialogTitle.value = '编辑'
  dialogVisible.value = true
}
async function handleSubmit() {
  try {
    if (kind.value === 'user') {
      if (form.id) await updateUser(form.id, { ...form })
      else await createUser({ ...form })
    } else if (kind.value === 'role') {
      if (form.id) await updateRole(form.id, { ...form })
      else await createRole({ ...form })
    } else if (kind.value === 'permission') {
      if (form.id) await updatePermission(form.id, { ...form })
      else await createPermission({ ...form })
    } else if (kind.value === 'dept') {
      if (form.id) await updateDept(form.id, { ...form })
      else await createDept({ ...form })
    } else if (kind.value === 'dict') {
      if (form.id) await updateDictData(form.id, { ...form })
      else await createDictData({ ...form })
    }
    ElMessage.success(form.id ? '修改成功' : '新增成功')
    dialogVisible.value = false
    fetchList()
    fetchStats()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

// ---------- 启用 / 停用 / 删除 ----------
async function toggleStatus(row, status) {
  try {
    if (kind.value === 'user') {
      if (status === 1) await enableUser(row.id)
      else await disableUser(row.id)
    } else if (kind.value === 'role') {
      if (status === 1) await enableRole(row.id)
      else await disableRole(row.id)
    } else if (kind.value === 'permission') {
      if (status === 1) await enablePermission(row.id)
      else await disablePermission(row.id)
    } else if (kind.value === 'dept') {
      if (status === 1) await enableDept(row.id)
      else await disableDept(row.id)
    }
    ElMessage.success(status === 1 ? '已启用' : '已停用')
    fetchList()
    fetchStats()
  } catch (e) {
    ElMessage.error('操作失败：' + e.message)
  }
}
function rowName(row) {
  if (kind.value === 'user') return row.realName || row.username
  if (kind.value === 'role') return row.name
  if (kind.value === 'permission') return row.name
  if (kind.value === 'dept') return row.name
  if (kind.value === 'dict') return row.label
  return ''
}
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${rowName(row)}」吗？`, '删除', { type: 'warning' })
    if (kind.value === 'user') await deleteUser(row.id)
    else if (kind.value === 'role') await deleteRole(row.id)
    else if (kind.value === 'permission') await deletePermission(row.id)
    else if (kind.value === 'dept') await deleteDept(row.id)
    else if (kind.value === 'dict') await deleteDictData(row.id)
    ElMessage.success('删除成功')
    fetchList()
    fetchStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + e.message)
  }
}

// ---------- 用户：重置密码 / 分配角色 ----------
async function handleResetPassword(row) {
  try {
    await ElMessageBox.confirm(`确定把「${row.realName || row.username}」的密码重置为默认密码 123456 吗？`, '重置密码', { type: 'warning' })
    await resetUserPassword(row.id)
    ElMessage.success('密码已重置为默认密码 123456')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('重置失败：' + e.message)
  }
}
const roleDialogVisible = ref(false)
const enabledRoles = ref([])
const selectedRoleIds = ref([])
const currentUserId = ref(null)
async function handleAssignRoles(row) {
  try {
    enabledRoles.value = await getEnabledRoles()
    const roles = await getUserRoles(row.id)
    selectedRoleIds.value = roles.map((r) => r.id)
    currentUserId.value = row.id
    roleDialogVisible.value = true
  } catch (e) {
    ElMessage.error('加载角色失败：' + e.message)
  }
}
async function submitAssignRoles() {
  try {
    await assignUserRoles(currentUserId.value, selectedRoleIds.value)
    ElMessage.success('分配角色成功')
    roleDialogVisible.value = false
    fetchStats()
  } catch (e) {
    ElMessage.error('分配失败：' + e.message)
  }
}

// ---------- 角色：分配权限（树形勾选） ----------
const permDialogVisible = ref(false)
const permTree = ref([])
const permTreeRef = ref(null)
const selectedPermIds = ref([])
const currentRoleId = ref(null)
const roleDeptNames = ref({})
async function handleAssignPermissions(row) {
  try {
    const all = await getPermissions()
    permTree.value = buildTree(all)
    const perms = await getRolePermissions(row.id)
    selectedPermIds.value = perms.map((p) => p.id)
    currentRoleId.value = row.id
    permDialogVisible.value = true
  } catch (e) {
    ElMessage.error('加载权限失败：' + e.message)
  }
}
async function submitAssignPermissions() {
  try {
    // 勾选含半选（父目录半选时也要把目录权限授予角色，菜单树才能正常嵌套）
    const checked = permTreeRef.value
      ? [...permTreeRef.value.getCheckedKeys(false), ...permTreeRef.value.getHalfCheckedKeys()]
      : selectedPermIds.value
    await assignRolePermissions(currentRoleId.value, [...new Set(checked)])
    ElMessage.success('分配权限成功，相关账号重新登录后菜单即按新权限显示')
    permDialogVisible.value = false
  } catch (e) {
    ElMessage.error('分配失败：' + e.message)
  }
}

// ---------- 角色：绑定部门（树形勾选） ----------
const deptDialogVisible = ref(false)
const deptTree = ref([])
const deptTreeRef = ref(null)
const selectedDeptIds = ref([])
async function handleAssignDepts(row) {
  try {
    deptTree.value = await getDeptTree()
    const depts = await getRoleDepts(row.id)
    selectedDeptIds.value = depts.map((d) => d.id)
    currentRoleId.value = row.id
    deptDialogVisible.value = true
  } catch (e) {
    ElMessage.error('加载科室失败：' + e.message)
  }
}
async function submitAssignDepts() {
  try {
    const checked = deptTreeRef.value
      ? [...deptTreeRef.value.getCheckedKeys(false), ...deptTreeRef.value.getHalfCheckedKeys()]
      : selectedDeptIds.value
    await assignRoleDepts(currentRoleId.value, [...new Set(checked)])
    ElMessage.success('绑定部门成功')
    deptDialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error('绑定失败：' + e.message)
  }
}

// ---------- 导入导出 ----------
function handleImportSuccess(res) {
  if (res && res.code === 0) {
    ElMessage.success(`导入完成：新增 ${res.data.created} 条，跳过 ${res.data.skipped} 条`)
    fetchList()
    fetchStats()
  } else {
    ElMessage.error('导入失败：' + (res ? res.message : '未知错误'))
  }
}
async function handleExport() {
  try {
    let res
    let filename
    if (kind.value === 'user') { res = await exportUsers({ ...query }); filename = '用户.xlsx' }
    else if (kind.value === 'dept') { res = await exportDepts({ ...query }); filename = '科室.xlsx' }
    else if (kind.value === 'dict') { res = await exportDictData({ ...query }); filename = '字典数据.xlsx' }
    else if (kind.value === 'log') { res = await exportOperLogs({ ...query }); filename = '操作日志.xlsx' }
    if (res) downloadBlob(res.data, filename)
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}
async function handleDownloadTemplate() {
  try {
    let res
    let filename
    if (kind.value === 'user') { res = await downloadUserTemplate(); filename = '用户导入模板.xlsx' }
    else if (kind.value === 'dept') { res = await downloadDeptTemplate(); filename = '科室导入模板.xlsx' }
    else if (kind.value === 'dict') { res = await downloadDictDataTemplate(); filename = '字典数据导入模板.xlsx' }
    if (res) downloadBlob(res.data, filename)
  } catch (e) {
    ElMessage.error('下载失败：' + e.message)
  }
}

async function loadDropdowns() {
  try {
    enabledDepts.value = await getEnabledDepts()
  } catch (e) { /* 下拉加载失败不阻断列表 */ }
  try {
    dictTypes.value = await getDictTypes()
  } catch (e) { /* 下拉加载失败不阻断列表 */ }
}

watch(kind, () => {
  handleReset()
  fetchStats()
})

onMounted(() => {
  resetForm()
  loadDropdowns()
  fetchStats()
  fetchList()
})
</script>

<style scoped>
.stat { text-align: center; }
.stat .num { font-size: 26px; font-weight: bold; }
.stat .label { color: #909399; margin-top: 4px; }
.page-note { color: #909399; font-size: 13px; margin: 0 0 12px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.search-form { margin-bottom: 0; }
</style>
