<template>
  <div>
    <p class="page-note">口径：需求清单第 28 项“医务系统设置”。底座（用户/角色/权限/科室/字典/操作日志）见“系统”菜单下的各管理页；本页为新增的五组配置：参数设置、数据备份、系统接口、消息提醒设置、知识库（支持 3000+ 条，分页 + 关键字检索 + 按分类筛选）。</p>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 总览 -->
      <el-tab-pane label="总览" name="overview">
        <el-row :gutter="16" style="margin-bottom: 16px">
          <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.users || 0 }}</div><div class="label">用户</div></div></el-card></el-col>
          <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.roles || 0 }}</div><div class="label">角色</div></div></el-card></el-col>
          <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.permissions || 0 }}</div><div class="label">权限</div></div></el-card></el-col>
          <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.depts || 0 }}</div><div class="label">科室</div></div></el-card></el-col>
          <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.dictData || 0 }}</div><div class="label">字典数据项</div></div></el-card></el-col>
          <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.operLogs || 0 }}</div><div class="label">操作日志</div></div></el-card></el-col>
          <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.knowledge || 0 }}</div><div class="label">知识库条目</div></div></el-card></el-col>
          <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.interfaces || 0 }}</div><div class="label">系统接口</div></div></el-card></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-card>
              <template #header><span>参数设置</span></template>
              <p>总数 {{ paramStats.total || 0 }}（启用 {{ paramStats.enabled || 0 }} / 停用 {{ paramStats.disabled || 0 }}；内置 {{ paramStats.system || 0 }} / 自定义 {{ paramStats.custom || 0 }}）</p>
              <p>系统参数 {{ (paramStats.byGroup || {}).SYSTEM || 0 }} / 业务参数 {{ (paramStats.byGroup || {}).BUSINESS || 0 }} / 消息提醒参数 {{ (paramStats.byGroup || {}).NOTIFY || 0 }}</p>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card>
              <template #header><span>数据备份</span></template>
              <p>总数 {{ backupStats.total || 0 }}（成功 {{ backupStats.success || 0 }} / 失败 {{ backupStats.failed || 0 }} / 进行中 {{ backupStats.running || 0 }}）</p>
              <p>已恢复 {{ backupStats.restored || 0 }} 个备份，累计恢复 {{ backupStats.totalRestoreCount || 0 }} 次，备份记录 {{ backupStats.totalRecords || 0 }} 条</p>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card>
              <template #header><span>系统接口</span></template>
              <p>总数 {{ interfaceStats.total || 0 }}（启用 {{ interfaceStats.enabled || 0 }} / 停用 {{ interfaceStats.disabled || 0 }}）</p>
              <p>测试成功 {{ interfaceStats.testSuccess || 0 }} / 失败 {{ interfaceStats.testFail || 0 }} / 未测试 {{ interfaceStats.untested || 0 }}</p>
            </el-card>
          </el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top: 16px">
          <el-col :span="12">
            <el-card>
              <template #header><span>消息提醒设置</span></template>
              <p>总数 {{ noticeStats.total || 0 }}（启用 {{ noticeStats.enabled || 0 }} / 停用 {{ noticeStats.disabled || 0 }}）</p>
              <p>站内 {{ (noticeStats.byChannel || {}).SITE || 0 }} / 短信 {{ (noticeStats.byChannel || {}).SMS || 0 }} / 微信 {{ (noticeStats.byChannel || {}).WECHAT || 0 }} / 邮件 {{ (noticeStats.byChannel || {}).EMAIL || 0 }}</p>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <template #header><span>知识库</span></template>
              <p>总数 {{ knowledgeStats.total || 0 }}（启用 {{ knowledgeStats.enabled || 0 }} / 停用 {{ knowledgeStats.disabled || 0 }}），总查阅 {{ knowledgeStats.totalViews || 0 }} 次</p>
              <p>制度规范 {{ (knowledgeStats.byCategory || {})['制度规范'] || 0 }} / 操作指南 {{ (knowledgeStats.byCategory || {})['操作指南'] || 0 }} / 常见问题 {{ (knowledgeStats.byCategory || {})['常见问题'] || 0 }} / 法律法规 {{ (knowledgeStats.byCategory || {})['法律法规'] || 0 }} / 培训资料 {{ (knowledgeStats.byCategory || {})['培训资料'] || 0 }} / 其他 {{ (knowledgeStats.byCategory || {})['其他'] || 0 }}</p>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 参数设置 -->
      <el-tab-pane label="参数设置" name="param">
        <el-row :gutter="16" style="margin-bottom: 16px">
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ paramStats.total || 0 }}</div><div class="label">参数总数</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ paramStats.enabled || 0 }}</div><div class="label">已启用</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ paramStats.disabled || 0 }}</div><div class="label">已停用</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ paramStats.system || 0 }}</div><div class="label">内置参数</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ paramStats.custom || 0 }}</div><div class="label">自定义参数</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ (paramStats.byGroup || {}).SYSTEM || 0 }}</div><div class="label">系统参数</div></div></el-card></el-col>
        </el-row>
        <el-card>
          <el-form :inline="true" :model="paramQuery" class="search-form">
            <el-form-item label="关键字"><el-input v-model="paramQuery.keyword" placeholder="参数名称 / 参数键" clearable style="width: 180px" /></el-form-item>
            <el-form-item label="分组">
              <el-select v-model="paramQuery.paramGroup" placeholder="全部" clearable style="width: 140px">
                <el-option v-for="g in paramGroups" :key="g.value" :label="g.label" :value="g.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="值类型">
              <el-select v-model="paramQuery.valueType" placeholder="全部" clearable style="width: 120px">
                <el-option v-for="t in valueTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="paramQuery.status" placeholder="全部" clearable style="width: 110px">
                <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
              </el-select>
            </el-form-item>
            <el-form-item><el-button type="primary" @click="fetchParams">查询</el-button><el-button @click="resetParamQuery">重置</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card style="margin-top: 16px">
          <div class="toolbar">
            <div><el-button type="primary" @click="handleParamAdd">新增参数</el-button></div>
            <div>
              <el-upload action="/api/sys-params/import" :show-file-list="false" accept=".xlsx,.xls" :on-success="handleImportSuccess('param')" :on-error="handleImportError" style="display: inline-block; margin: 0 8px">
                <el-button type="success">批量导入</el-button>
              </el-upload>
              <el-button type="warning" @click="handleParamExport">导出 Excel</el-button>
              <el-button @click="handleParamTemplate">下载模板</el-button>
            </div>
          </div>
          <el-table :data="paramData" border stripe v-loading="paramLoading" style="margin-top: 12px">
            <el-table-column prop="paramKey" label="参数键" width="200" />
            <el-table-column prop="paramName" label="参数名称" width="170" />
            <el-table-column prop="paramValue" label="参数值" width="220" />
            <el-table-column label="参数分组" width="120"><template #default="{ row }">{{ paramGroupText(row.paramGroup) }}</template></el-table-column>
            <el-table-column label="值类型" width="90"><template #default="{ row }">{{ valueTypeText(row.valueType) }}</template></el-table-column>
            <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
            <el-table-column label="内置" width="80"><template #default="{ row }">{{ row.isSystem === 1 ? '是' : '否' }}</template></el-table-column>
            <el-table-column prop="remark" label="备注" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" v-if="row.status !== 1" @click="toggleParam(row, 1)">启用</el-button>
                <el-button link type="warning" v-if="row.status === 1" @click="toggleParam(row, 0)">停用</el-button>
                <el-button link type="primary" @click="handleParamEdit(row)">编辑</el-button>
                <el-button link type="danger" :disabled="row.isSystem === 1" @click="handleParamDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="paramQuery.page" v-model:page-size="paramQuery.size" :total="paramTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchParams" @current-change="fetchParams" />
        </el-card>
      </el-tab-pane>

      <!-- 数据备份 -->
      <el-tab-pane label="数据备份" name="backup">
        <el-row :gutter="16" style="margin-bottom: 16px">
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ backupStats.total || 0 }}</div><div class="label">备份总数</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ backupStats.success || 0 }}</div><div class="label">成功</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ backupStats.failed || 0 }}</div><div class="label">失败</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ backupStats.restored || 0 }}</div><div class="label">已恢复备份</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ backupStats.totalRestoreCount || 0 }}</div><div class="label">累计恢复次数</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ backupStats.totalRecords || 0 }}</div><div class="label">备份记录条数</div></div></el-card></el-col>
        </el-row>
        <el-card>
          <el-form :inline="true" :model="backupQuery" class="search-form">
            <el-form-item label="关键字"><el-input v-model="backupQuery.keyword" placeholder="备份名称" clearable style="width: 180px" /></el-form-item>
            <el-form-item label="备份类型">
              <el-select v-model="backupQuery.backupType" placeholder="全部" clearable style="width: 140px">
                <el-option v-for="t in backupTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="backupQuery.status" placeholder="全部" clearable style="width: 110px">
                <el-option :value="1" label="成功" /><el-option :value="2" label="失败" /><el-option :value="0" label="进行中" />
              </el-select>
            </el-form-item>
            <el-form-item><el-checkbox v-model="backupQuery.restoredOnly">只看已恢复</el-checkbox></el-form-item>
            <el-form-item><el-button type="primary" @click="fetchBackups">查询</el-button><el-button @click="resetBackupQuery">重置</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card style="margin-top: 16px">
          <div class="toolbar">
            <div><el-button type="primary" @click="handleBackupAdd">执行备份</el-button></div>
            <div>
              <el-upload action="/api/sys-backups/import" :show-file-list="false" accept=".xlsx,.xls" :on-success="handleImportSuccess('backup')" :on-error="handleImportError" style="display: inline-block; margin: 0 8px">
                <el-button type="success">批量导入</el-button>
              </el-upload>
              <el-button type="warning" @click="handleBackupExport">导出 Excel</el-button>
              <el-button @click="handleBackupTemplate">下载模板</el-button>
            </div>
          </div>
          <el-table :data="backupData" border stripe v-loading="backupLoading" style="margin-top: 12px">
            <el-table-column prop="backupName" label="备份名称" width="200" />
            <el-table-column label="备份类型" width="110"><template #default="{ row }">{{ backupTypeText(row.backupType) }}</template></el-table-column>
            <el-table-column prop="backupScope" label="备份范围" width="110" />
            <el-table-column prop="recordCount" label="记录条数" width="100" />
            <el-table-column prop="fileName" label="备份文件名" width="260" />
            <el-table-column label="文件大小" width="110"><template #default="{ row }">{{ formatSize(row.fileSize) }}</template></el-table-column>
            <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="backupStatusTag(row.status)">{{ backupStatusText(row.status) }}</el-tag></template></el-table-column>
            <el-table-column prop="createdBy" label="执行人" width="100" />
            <el-table-column prop="restoreCount" label="已恢复次数" width="110" />
            <el-table-column prop="lastRestoreTime" label="最近恢复时间" width="170" />
            <el-table-column prop="remark" label="备注" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" v-if="row.status === 1" @click="handleBackupRestore(row)">恢复</el-button>
                <el-button link type="danger" :disabled="row.restoreCount > 0" @click="handleBackupDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="backupQuery.page" v-model:page-size="backupQuery.size" :total="backupTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchBackups" @current-change="fetchBackups" />
        </el-card>
      </el-tab-pane>

      <!-- 系统接口 -->
      <el-tab-pane label="系统接口" name="interface">
        <el-row :gutter="16" style="margin-bottom: 16px">
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ interfaceStats.total || 0 }}</div><div class="label">接口总数</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ interfaceStats.enabled || 0 }}</div><div class="label">已启用</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ interfaceStats.disabled || 0 }}</div><div class="label">已停用</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ interfaceStats.testSuccess || 0 }}</div><div class="label">测试成功</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ interfaceStats.testFail || 0 }}</div><div class="label">测试失败</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ interfaceStats.untested || 0 }}</div><div class="label">未测试</div></div></el-card></el-col>
        </el-row>
        <el-card>
          <el-form :inline="true" :model="interfaceQuery" class="search-form">
            <el-form-item label="关键字"><el-input v-model="interfaceQuery.keyword" placeholder="接口名称 / 编码" clearable style="width: 180px" /></el-form-item>
            <el-form-item label="接口类型">
              <el-select v-model="interfaceQuery.type" placeholder="全部" clearable style="width: 130px">
                <el-option v-for="t in interfaceTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="interfaceQuery.status" placeholder="全部" clearable style="width: 110px">
                <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
              </el-select>
            </el-form-item>
            <el-form-item label="测试结果">
              <el-select v-model="interfaceQuery.lastTestResult" placeholder="全部" clearable style="width: 120px">
                <el-option value="SUCCESS" label="成功" /><el-option value="FAIL" label="失败" /><el-option value="UNTESTED" label="未测试" />
              </el-select>
            </el-form-item>
            <el-form-item><el-button type="primary" @click="fetchInterfaces">查询</el-button><el-button @click="resetInterfaceQuery">重置</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card style="margin-top: 16px">
          <div class="toolbar">
            <div><el-button type="primary" @click="handleInterfaceAdd">新增接口</el-button></div>
            <div>
              <el-upload action="/api/sys-interfaces/import" :show-file-list="false" accept=".xlsx,.xls" :on-success="handleImportSuccess('interface')" :on-error="handleImportError" style="display: inline-block; margin: 0 8px">
                <el-button type="success">批量导入</el-button>
              </el-upload>
              <el-button type="warning" @click="handleInterfaceExport">导出 Excel</el-button>
              <el-button @click="handleInterfaceTemplate">下载模板</el-button>
            </div>
          </div>
          <el-table :data="interfaceData" border stripe v-loading="interfaceLoading" style="margin-top: 12px">
            <el-table-column prop="interfaceCode" label="接口编码" width="110" />
            <el-table-column prop="interfaceName" label="接口名称" width="150" />
            <el-table-column label="接口类型" width="90"><template #default="{ row }">{{ interfaceTypeText(row.type) }}</template></el-table-column>
            <el-table-column prop="url" label="接口地址" width="280" />
            <el-table-column label="鉴权方式" width="100"><template #default="{ row }">{{ authTypeText(row.authType) }}</template></el-table-column>
            <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
            <el-table-column prop="timeoutSeconds" label="超时（秒）" width="100" />
            <el-table-column label="最近测试结果" width="120"><template #default="{ row }"><el-tag :type="testResultTag(row.lastTestResult)">{{ testResultText(row.lastTestResult) }}</el-tag></template></el-table-column>
            <el-table-column prop="lastTestTime" label="最近测试时间" width="170" />
            <el-table-column prop="lastTestMsg" label="测试说明" width="180" />
            <el-table-column prop="remark" label="备注" />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" :disabled="row.status !== 1" @click="handleInterfaceTest(row)">测试连通性</el-button>
                <el-button link type="success" v-if="row.status !== 1" @click="toggleInterface(row, 1)">启用</el-button>
                <el-button link type="warning" v-if="row.status === 1" @click="toggleInterface(row, 0)">停用</el-button>
                <el-button link type="primary" @click="handleInterfaceEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="handleInterfaceDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="interfaceQuery.page" v-model:page-size="interfaceQuery.size" :total="interfaceTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchInterfaces" @current-change="fetchInterfaces" />
        </el-card>
      </el-tab-pane>

      <!-- 消息提醒设置 -->
      <el-tab-pane label="消息提醒设置" name="notice">
        <el-row :gutter="16" style="margin-bottom: 16px">
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ noticeStats.total || 0 }}</div><div class="label">设置总数</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ noticeStats.enabled || 0 }}</div><div class="label">已启用</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ noticeStats.disabled || 0 }}</div><div class="label">已停用</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ (noticeStats.byChannel || {}).SITE || 0 }}</div><div class="label">站内消息</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ (noticeStats.byChannel || {}).SMS || 0 }}</div><div class="label">短信</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ (noticeStats.byChannel || {}).WECHAT || 0 }}</div><div class="label">微信</div></div></el-card></el-col>
        </el-row>
        <el-card>
          <el-form :inline="true" :model="noticeQuery" class="search-form">
            <el-form-item label="关键字"><el-input v-model="noticeQuery.keyword" placeholder="设置名称 / 编码 / 触发事件" clearable style="width: 200px" /></el-form-item>
            <el-form-item label="提醒渠道">
              <el-select v-model="noticeQuery.channel" placeholder="全部" clearable style="width: 130px">
                <el-option v-for="c in channels" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="是否启用">
              <el-select v-model="noticeQuery.enabled" placeholder="全部" clearable style="width: 110px">
                <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
              </el-select>
            </el-form-item>
            <el-form-item><el-button type="primary" @click="fetchNotices">查询</el-button><el-button @click="resetNoticeQuery">重置</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card style="margin-top: 16px">
          <div class="toolbar">
            <div><el-button type="primary" @click="handleNoticeAdd">新增设置</el-button></div>
            <div>
              <el-upload action="/api/sys-notice-settings/import" :show-file-list="false" accept=".xlsx,.xls" :on-success="handleImportSuccess('notice')" :on-error="handleImportError" style="display: inline-block; margin: 0 8px">
                <el-button type="success">批量导入</el-button>
              </el-upload>
              <el-button type="warning" @click="handleNoticeExport">导出 Excel</el-button>
              <el-button @click="handleNoticeTemplate">下载模板</el-button>
            </div>
          </div>
          <el-table :data="noticeData" border stripe v-loading="noticeLoading" style="margin-top: 12px">
            <el-table-column prop="settingKey" label="设置编码" width="150" />
            <el-table-column prop="settingName" label="设置名称" width="150" />
            <el-table-column label="提醒渠道" width="100"><template #default="{ row }">{{ channelText(row.channel) }}</template></el-table-column>
            <el-table-column prop="triggerEvent" label="触发事件" width="180" />
            <el-table-column prop="templateContent" label="消息模板" width="260" />
            <el-table-column label="接收范围" width="100"><template #default="{ row }">{{ receiverScopeText(row.receiverScope) }}</template></el-table-column>
            <el-table-column prop="receiverValue" label="接收对象" width="140" />
            <el-table-column label="是否启用" width="100"><template #default="{ row }"><el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
            <el-table-column prop="remark" label="备注" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" v-if="row.enabled !== 1" @click="toggleNotice(row, 1)">启用</el-button>
                <el-button link type="warning" v-if="row.enabled === 1" @click="toggleNotice(row, 0)">停用</el-button>
                <el-button link type="primary" @click="handleNoticeEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="handleNoticeDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="noticeQuery.page" v-model:page-size="noticeQuery.size" :total="noticeTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchNotices" @current-change="fetchNotices" />
        </el-card>
      </el-tab-pane>

      <!-- 知识库 -->
      <el-tab-pane label="知识库" name="knowledge">
        <el-row :gutter="16" style="margin-bottom: 16px">
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ knowledgeStats.total || 0 }}</div><div class="label">知识总数</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ knowledgeStats.enabled || 0 }}</div><div class="label">已启用</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ knowledgeStats.disabled || 0 }}</div><div class="label">已停用</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ knowledgeStats.totalViews || 0 }}</div><div class="label">总查阅量</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ (knowledgeStats.byCategory || {})['制度规范'] || 0 }}</div><div class="label">制度规范</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ (knowledgeStats.byCategory || {})['操作指南'] || 0 }}</div><div class="label">操作指南</div></div></el-card></el-col>
        </el-row>
        <el-card>
          <el-form :inline="true" :model="knowledgeQuery" class="search-form">
            <el-form-item label="关键字"><el-input v-model="knowledgeQuery.keyword" placeholder="标题 / 正文 / 关键词" clearable style="width: 200px" /></el-form-item>
            <el-form-item label="知识分类">
              <el-select v-model="knowledgeQuery.category" placeholder="全部" clearable style="width: 140px">
                <el-option v-for="c in knowledgeCategories" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="knowledgeQuery.status" placeholder="全部" clearable style="width: 110px">
                <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
              </el-select>
            </el-form-item>
            <el-form-item><el-button type="primary" @click="fetchKnowledge">查询</el-button><el-button @click="resetKnowledgeQuery">重置</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card style="margin-top: 16px">
          <div class="toolbar">
            <div><el-button type="primary" @click="handleKnowledgeAdd">新增知识</el-button></div>
            <div>
              <el-upload action="/api/sys-knowledge/import" :show-file-list="false" accept=".xlsx,.xls" :on-success="handleImportSuccess('knowledge')" :on-error="handleImportError" style="display: inline-block; margin: 0 8px">
                <el-button type="success">批量导入</el-button>
              </el-upload>
              <el-button type="warning" @click="handleKnowledgeExport">导出 Excel</el-button>
              <el-button @click="handleKnowledgeTemplate">下载模板</el-button>
            </div>
          </div>
          <el-table :data="knowledgeData" border stripe v-loading="knowledgeLoading" style="margin-top: 12px">
            <el-table-column prop="kbCode" label="知识编码" width="110" />
            <el-table-column prop="title" label="知识标题" width="200" />
            <el-table-column prop="category" label="知识分类" width="110" />
            <el-table-column prop="keywords" label="关键词" width="200" />
            <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
            <el-table-column prop="viewCount" label="查阅量" width="90" />
            <el-table-column prop="sortOrder" label="排序值" width="90" />
            <el-table-column prop="remark" label="备注" />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleKnowledgeDetail(row)">查看</el-button>
                <el-button link type="success" v-if="row.status !== 1" @click="toggleKnowledge(row, 1)">启用</el-button>
                <el-button link type="warning" v-if="row.status === 1" @click="toggleKnowledge(row, 0)">停用</el-button>
                <el-button link type="primary" @click="handleKnowledgeEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="handleKnowledgeDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="knowledgeQuery.page" v-model:page-size="knowledgeQuery.size" :total="knowledgeTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchKnowledge" @current-change="fetchKnowledge" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 参数弹窗 -->
    <el-dialog v-model="paramDialogVisible" :title="paramDialogTitle" width="560px">
      <el-form :model="paramForm" label-width="110px">
        <el-form-item label="参数键"><el-input v-model="paramForm.paramKey" :disabled="isSystemParam" placeholder="如 sys.name，内置参数不可改" /></el-form-item>
        <el-form-item label="参数名称"><el-input v-model="paramForm.paramName" /></el-form-item>
        <el-form-item label="参数值"><el-input v-model="paramForm.paramValue" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="参数分组">
          <el-select v-model="paramForm.paramGroup" style="width: 100%">
            <el-option v-for="g in paramGroups" :key="g.value" :label="g.label" :value="g.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="值类型">
          <el-select v-model="paramForm.valueType" style="width: 100%">
            <el-option v-for="t in valueTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="paramForm.status" style="width: 100%">
            <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="paramForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="paramDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitParam">保存</el-button>
      </template>
    </el-dialog>

    <!-- 备份弹窗 -->
    <el-dialog v-model="backupDialogVisible" title="执行备份" width="560px">
      <el-form :model="backupForm" label-width="110px">
        <el-form-item label="备份名称"><el-input v-model="backupForm.backupName" placeholder="如 2026-11-22 全量备份" /></el-form-item>
        <el-form-item label="备份类型">
          <el-select v-model="backupForm.backupType" style="width: 100%">
            <el-option v-for="t in backupTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备份范围">
          <el-select v-model="backupForm.backupScope" style="width: 100%">
            <el-option value="ALL" label="全库" /><el-option value="SYSTEM" label="系统配置" /><el-option value="BUSINESS" label="业务数据" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行人"><el-input v-model="backupForm.createdBy" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="backupForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="backupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBackup">执行备份</el-button>
      </template>
    </el-dialog>

    <!-- 接口弹窗 -->
    <el-dialog v-model="interfaceDialogVisible" :title="interfaceDialogTitle" width="560px">
      <el-form :model="interfaceForm" label-width="110px">
        <el-form-item label="接口编码"><el-input v-model="interfaceForm.interfaceCode" :disabled="!!interfaceForm.id" placeholder="如 HIS-001，编码不可改" /></el-form-item>
        <el-form-item label="接口名称"><el-input v-model="interfaceForm.interfaceName" /></el-form-item>
        <el-form-item label="接口类型">
          <el-select v-model="interfaceForm.type" style="width: 100%">
            <el-option v-for="t in interfaceTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="接口地址"><el-input v-model="interfaceForm.url" placeholder="http(s)://..." /></el-form-item>
        <el-form-item label="鉴权方式">
          <el-select v-model="interfaceForm.authType" style="width: 100%">
            <el-option v-for="t in authTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="鉴权凭证"><el-input v-model="interfaceForm.authToken" placeholder="Token 或账号密码；不改请留空" show-password /></el-form-item>
        <el-form-item label="超时时间（秒）"><el-input-number v-model="interfaceForm.timeoutSeconds" :min="1" :max="60" style="width: 100%" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="interfaceForm.status" style="width: 100%">
            <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="interfaceForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="interfaceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitInterface">保存</el-button>
      </template>
    </el-dialog>

    <!-- 提醒设置弹窗 -->
    <el-dialog v-model="noticeDialogVisible" :title="noticeDialogTitle" width="560px">
      <el-form :model="noticeForm" label-width="110px">
        <el-form-item label="设置编码"><el-input v-model="noticeForm.settingKey" :disabled="!!noticeForm.id" placeholder="如 LEAVE_APPROVE" /></el-form-item>
        <el-form-item label="设置名称"><el-input v-model="noticeForm.settingName" /></el-form-item>
        <el-form-item label="提醒渠道">
          <el-select v-model="noticeForm.channel" style="width: 100%">
            <el-option v-for="c in channels" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="触发事件"><el-input v-model="noticeForm.triggerEvent" /></el-form-item>
        <el-form-item label="消息模板"><el-input v-model="noticeForm.templateContent" type="textarea" :rows="3" placeholder="支持 {姓名} {科室} 等占位符" /></el-form-item>
        <el-form-item label="接收范围">
          <el-select v-model="noticeForm.receiverScope" style="width: 100%">
            <el-option v-for="s in receiverScopes" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收对象"><el-input v-model="noticeForm.receiverValue" placeholder="按角色填角色编码，按人员填用户名，全部人员留空" /></el-form-item>
        <el-form-item label="是否启用">
          <el-select v-model="noticeForm.enabled" style="width: 100%">
            <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="noticeForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitNotice">保存</el-button>
      </template>
    </el-dialog>

    <!-- 知识弹窗 -->
    <el-dialog v-model="knowledgeDialogVisible" :title="knowledgeDialogTitle" width="640px">
      <el-form :model="knowledgeForm" label-width="110px">
        <el-form-item label="知识编码"><el-input v-model="knowledgeForm.kbCode" :disabled="!!knowledgeForm.id" placeholder="如 KB-001" /></el-form-item>
        <el-form-item label="知识标题"><el-input v-model="knowledgeForm.title" /></el-form-item>
        <el-form-item label="知识分类">
          <el-select v-model="knowledgeForm.category" style="width: 100%">
            <el-option v-for="c in knowledgeCategories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识正文"><el-input v-model="knowledgeForm.content" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="关键词"><el-input v-model="knowledgeForm.keywords" placeholder="逗号分隔，如 三级查房,查房制度" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="knowledgeForm.status" style="width: 100%">
            <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序值"><el-input-number v-model="knowledgeForm.sortOrder" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="knowledgeForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="knowledgeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitKnowledge">保存</el-button>
      </template>
    </el-dialog>

    <!-- 知识详情弹窗 -->
    <el-dialog v-model="knowledgeDetailVisible" title="知识详情" width="640px">
      <div v-if="knowledgeDetail">
        <h3 style="margin: 0 0 8px">{{ knowledgeDetail.title }}</h3>
        <p class="page-note">{{ knowledgeDetail.kbCode }} · {{ knowledgeDetail.category }} · 查阅 {{ knowledgeDetail.viewCount || 0 }} 次</p>
        <p style="white-space: pre-wrap; line-height: 1.8">{{ knowledgeDetail.content }}</p>
        <p class="page-note">关键词：{{ knowledgeDetail.keywords }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSystemSettingsOverview,
  getSysParams, getSysParamStats, createSysParam, updateSysParam, deleteSysParam,
  enableSysParam, disableSysParam, exportSysParams, downloadSysParamTemplate,
  getSysBackups, getSysBackupStats, createSysBackup, restoreSysBackup, deleteSysBackup,
  exportSysBackups, downloadSysBackupTemplate,
  getSysInterfaces, getSysInterfaceStats, createSysInterface, updateSysInterface,
  deleteSysInterface, enableSysInterface, disableSysInterface, testSysInterface,
  exportSysInterfaces, downloadSysInterfaceTemplate,
  getSysNoticeSettings, getSysNoticeSettingStats, createSysNoticeSetting,
  updateSysNoticeSetting, deleteSysNoticeSetting, enableSysNoticeSetting,
  disableSysNoticeSetting, exportSysNoticeSettings, downloadSysNoticeSettingTemplate,
  getKnowledgeItems, getKnowledgeItemStats, getKnowledgeItem, createKnowledgeItem,
  updateKnowledgeItem, deleteKnowledgeItem, enableKnowledgeItem, disableKnowledgeItem,
  exportKnowledgeItems, downloadKnowledgeItemTemplate
} from '../../api/systemSettings'

const paramGroups = [
  { value: 'SYSTEM', label: '系统参数' },
  { value: 'BUSINESS', label: '业务参数' },
  { value: 'NOTIFY', label: '消息提醒参数' }
]
const valueTypes = [
  { value: 'STRING', label: '文本' },
  { value: 'NUMBER', label: '数字' },
  { value: 'BOOLEAN', label: '开关' }
]
const backupTypes = [
  { value: 'FULL', label: '全量备份' },
  { value: 'INCREMENTAL', label: '增量备份' },
  { value: 'CONFIG', label: '配置备份' }
]
const interfaceTypes = [
  { value: 'HIS', label: 'HIS' },
  { value: 'LIS', label: 'LIS' },
  { value: 'PACS', label: 'PACS' },
  { value: 'INSURANCE', label: '医保' },
  { value: 'OTHER', label: '其他' }
]
const authTypes = [
  { value: 'NONE', label: '无鉴权' },
  { value: 'TOKEN', label: '令牌' },
  { value: 'BASIC', label: '账号密码' }
]
const channels = [
  { value: 'SITE', label: '站内消息' },
  { value: 'SMS', label: '短信' },
  { value: 'WECHAT', label: '微信' },
  { value: 'EMAIL', label: '邮件' }
]
const receiverScopes = [
  { value: 'ALL', label: '全部人员' },
  { value: 'ROLE', label: '按角色' },
  { value: 'USER', label: '按人员' }
]
const knowledgeCategories = [
  { value: '制度规范', label: '制度规范' },
  { value: '操作指南', label: '操作指南' },
  { value: '常见问题', label: '常见问题' },
  { value: '法律法规', label: '法律法规' },
  { value: '培训资料', label: '培训资料' },
  { value: '其他', label: '其他' }
]

function paramGroupText(v) {
  return (paramGroups.find((g) => g.value === v) || {}).label || v || ''
}
function valueTypeText(v) {
  return (valueTypes.find((g) => g.value === v) || {}).label || v || ''
}
function backupTypeText(v) {
  return (backupTypes.find((g) => g.value === v) || {}).label || v || ''
}
function backupStatusText(v) {
  return { 0: '进行中', 1: '成功', 2: '失败' }[v] || ''
}
function backupStatusTag(v) {
  return { 0: 'info', 1: 'success', 2: 'danger' }[v] || 'info'
}
function interfaceTypeText(v) {
  return (interfaceTypes.find((g) => g.value === v) || {}).label || v || ''
}
function authTypeText(v) {
  return (authTypes.find((g) => g.value === v) || {}).label || v || ''
}
function testResultText(v) {
  return { SUCCESS: '成功', FAIL: '失败', UNTESTED: '未测试' }[v] || v || '未测试'
}
function testResultTag(v) {
  return { SUCCESS: 'success', FAIL: 'danger', UNTESTED: 'info' }[v] || 'info'
}
function channelText(v) {
  return (channels.find((g) => g.value === v) || {}).label || v || ''
}
function receiverScopeText(v) {
  return (receiverScopes.find((g) => g.value === v) || {}).label || v || ''
}
function formatSize(bytes) {
  if (bytes == null) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}
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

const activeTab = ref('overview')

// ---------- 总览 ----------
const overview = ref({})
const paramStats = ref({})
const backupStats = ref({})
const interfaceStats = ref({})
const noticeStats = ref({})
const knowledgeStats = ref({})

async function fetchOverview() {
  try {
    overview.value = await getSystemSettingsOverview()
  } catch (e) {
    ElMessage.error('加载总览失败：' + e.message)
  }
}
async function fetchAllStats() {
  try { paramStats.value = await getSysParamStats() } catch (e) { ElMessage.error('加载参数统计失败：' + e.message) }
  try { backupStats.value = await getSysBackupStats() } catch (e) { ElMessage.error('加载备份统计失败：' + e.message) }
  try { interfaceStats.value = await getSysInterfaceStats() } catch (e) { ElMessage.error('加载接口统计失败：' + e.message) }
  try { noticeStats.value = await getSysNoticeSettingStats() } catch (e) { ElMessage.error('加载提醒设置统计失败：' + e.message) }
  try { knowledgeStats.value = await getKnowledgeItemStats() } catch (e) { ElMessage.error('加载知识库统计失败：' + e.message) }
}
function handleTabChange(tab) {
  if (tab === 'overview') { fetchOverview(); fetchAllStats() }
  if (tab === 'param') { fetchParams(); fetchAllStats() }
  if (tab === 'backup') { fetchBackups(); fetchAllStats() }
  if (tab === 'interface') { fetchInterfaces(); fetchAllStats() }
  if (tab === 'notice') { fetchNotices(); fetchAllStats() }
  if (tab === 'knowledge') { fetchKnowledge(); fetchAllStats() }
}
function handleImportSuccess(kind) {
  return (res) => {
    if (res && res.code === 0) {
      ElMessage.success(`导入完成：新增 ${res.data.created} 条，跳过 ${res.data.skipped} 条`)
      handleTabChange(kind)
    } else {
      ElMessage.error('导入失败：' + (res ? res.message : '未知错误'))
    }
  }
}

// ---------- 参数设置 ----------
const paramData = ref([])
const paramTotal = ref(0)
const paramLoading = ref(false)
const paramQuery = reactive({ keyword: '', paramGroup: '', valueType: '', status: '', page: 1, size: 10 })
const paramDialogVisible = ref(false)
const paramDialogTitle = ref('新增参数')
const paramForm = reactive({ id: null, paramKey: '', paramName: '', paramValue: '', paramGroup: 'BUSINESS', valueType: 'STRING', status: 1, isSystem: 0, remark: '' })
const isSystemParam = computed(() => paramForm.isSystem === 1)

async function fetchParams() {
  paramLoading.value = true
  try {
    const data = await getSysParams({ ...paramQuery })
    paramData.value = data.records || []
    paramTotal.value = data.total || 0
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    paramLoading.value = false
  }
}
function resetParamQuery() {
  paramQuery.keyword = ''
  paramQuery.paramGroup = ''
  paramQuery.valueType = ''
  paramQuery.status = ''
  paramQuery.page = 1
  fetchParams()
}
function resetParamForm() {
  paramForm.id = null
  paramForm.paramKey = ''
  paramForm.paramName = ''
  paramForm.paramValue = ''
  paramForm.paramGroup = 'BUSINESS'
  paramForm.valueType = 'STRING'
  paramForm.status = 1
  paramForm.isSystem = 0
  paramForm.remark = ''
}
function handleParamAdd() {
  resetParamForm()
  paramDialogTitle.value = '新增参数'
  paramDialogVisible.value = true
}
function handleParamEdit(row) {
  resetParamForm()
  Object.assign(paramForm, row)
  paramDialogTitle.value = '编辑参数'
  paramDialogVisible.value = true
}
async function submitParam() {
  try {
    if (paramForm.id) {
      await updateSysParam(paramForm.id, { ...paramForm })
      ElMessage.success('修改成功')
    } else {
      await createSysParam({ ...paramForm })
      ElMessage.success('新增成功')
    }
    paramDialogVisible.value = false
    fetchParams()
    fetchAllStats()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}
async function toggleParam(row, status) {
  try {
    if (status === 1) await enableSysParam(row.id)
    else await disableSysParam(row.id)
    ElMessage.success(status === 1 ? '已启用' : '已停用')
    fetchParams()
    fetchAllStats()
  } catch (e) {
    ElMessage.error('操作失败：' + e.message)
  }
}
async function handleParamDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除参数「${row.paramName}」吗？`, '删除', { type: 'warning' })
    await deleteSysParam(row.id)
    ElMessage.success('删除成功')
    fetchParams()
    fetchAllStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + e.message)
  }
}
async function handleParamExport() {
  try {
    const res = await exportSysParams({ ...paramQuery })
    downloadBlob(res.data, '系统参数.xlsx')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}
async function handleParamTemplate() {
  try {
    const res = await downloadSysParamTemplate()
    downloadBlob(res.data, '系统参数导入模板.xlsx')
  } catch (e) {
    ElMessage.error('下载失败：' + e.message)
  }
}

// ---------- 数据备份 ----------
const backupData = ref([])
const backupTotal = ref(0)
const backupLoading = ref(false)
const backupQuery = reactive({ keyword: '', backupType: '', status: '', restoredOnly: false, page: 1, size: 10 })
const backupDialogVisible = ref(false)
const backupForm = reactive({ backupName: '', backupType: 'FULL', backupScope: 'ALL', createdBy: 'admin', remark: '' })

async function fetchBackups() {
  backupLoading.value = true
  try {
    const data = await getSysBackups({ ...backupQuery })
    backupData.value = data.records || []
    backupTotal.value = data.total || 0
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    backupLoading.value = false
  }
}
function resetBackupQuery() {
  backupQuery.keyword = ''
  backupQuery.backupType = ''
  backupQuery.status = ''
  backupQuery.restoredOnly = false
  backupQuery.page = 1
  fetchBackups()
}
function handleBackupAdd() {
  backupForm.backupName = ''
  backupForm.backupType = 'FULL'
  backupForm.backupScope = 'ALL'
  backupForm.createdBy = 'admin'
  backupForm.remark = ''
  backupDialogVisible.value = true
}
async function submitBackup() {
  try {
    const data = await createSysBackup({ ...backupForm })
    ElMessage.success(`备份成功：${data.fileName}，共 ${data.recordCount} 条记录`)
    backupDialogVisible.value = false
    fetchBackups()
    fetchAllStats()
  } catch (e) {
    ElMessage.error('备份失败：' + e.message)
  }
}
async function handleBackupRestore(row) {
  try {
    await ElMessageBox.confirm(`确定恢复备份「${row.backupName}」吗？恢复后会记录恢复次数。`, '恢复备份', { type: 'warning' })
    await restoreSysBackup(row.id)
    ElMessage.success('恢复成功')
    fetchBackups()
    fetchAllStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('恢复失败：' + e.message)
  }
}
async function handleBackupDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除备份「${row.backupName}」吗？`, '删除', { type: 'warning' })
    await deleteSysBackup(row.id)
    ElMessage.success('删除成功')
    fetchBackups()
    fetchAllStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + e.message)
  }
}
async function handleBackupExport() {
  try {
    const res = await exportSysBackups({ ...backupQuery })
    downloadBlob(res.data, '数据备份.xlsx')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}
async function handleBackupTemplate() {
  try {
    const res = await downloadSysBackupTemplate()
    downloadBlob(res.data, '数据备份导入模板.xlsx')
  } catch (e) {
    ElMessage.error('下载失败：' + e.message)
  }
}

// ---------- 系统接口 ----------
const interfaceData = ref([])
const interfaceTotal = ref(0)
const interfaceLoading = ref(false)
const interfaceQuery = reactive({ keyword: '', type: '', status: '', lastTestResult: '', page: 1, size: 10 })
const interfaceDialogVisible = ref(false)
const interfaceDialogTitle = ref('新增接口')
const interfaceForm = reactive({ id: null, interfaceCode: '', interfaceName: '', type: 'OTHER', url: '', authType: 'NONE', authToken: '', timeoutSeconds: 5, status: 1, remark: '' })

async function fetchInterfaces() {
  interfaceLoading.value = true
  try {
    const data = await getSysInterfaces({ ...interfaceQuery })
    interfaceData.value = data.records || []
    interfaceTotal.value = data.total || 0
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    interfaceLoading.value = false
  }
}
function resetInterfaceQuery() {
  interfaceQuery.keyword = ''
  interfaceQuery.type = ''
  interfaceQuery.status = ''
  interfaceQuery.lastTestResult = ''
  interfaceQuery.page = 1
  fetchInterfaces()
}
function resetInterfaceForm() {
  interfaceForm.id = null
  interfaceForm.interfaceCode = ''
  interfaceForm.interfaceName = ''
  interfaceForm.type = 'OTHER'
  interfaceForm.url = ''
  interfaceForm.authType = 'NONE'
  interfaceForm.authToken = ''
  interfaceForm.timeoutSeconds = 5
  interfaceForm.status = 1
  interfaceForm.remark = ''
}
function handleInterfaceAdd() {
  resetInterfaceForm()
  interfaceDialogTitle.value = '新增接口'
  interfaceDialogVisible.value = true
}
function handleInterfaceEdit(row) {
  resetInterfaceForm()
  Object.assign(interfaceForm, row)
  interfaceForm.authToken = ''
  interfaceDialogTitle.value = '编辑接口'
  interfaceDialogVisible.value = true
}
async function submitInterface() {
  try {
    if (interfaceForm.id) {
      await updateSysInterface(interfaceForm.id, { ...interfaceForm })
      ElMessage.success('修改成功')
    } else {
      await createSysInterface({ ...interfaceForm })
      ElMessage.success('新增成功')
    }
    interfaceDialogVisible.value = false
    fetchInterfaces()
    fetchAllStats()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}
async function toggleInterface(row, status) {
  try {
    if (status === 1) await enableSysInterface(row.id)
    else await disableSysInterface(row.id)
    ElMessage.success(status === 1 ? '已启用' : '已停用')
    fetchInterfaces()
    fetchAllStats()
  } catch (e) {
    ElMessage.error('操作失败：' + e.message)
  }
}
async function handleInterfaceDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除接口「${row.interfaceName}」吗？`, '删除', { type: 'warning' })
    await deleteSysInterface(row.id)
    ElMessage.success('删除成功')
    fetchInterfaces()
    fetchAllStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + e.message)
  }
}
async function handleInterfaceTest(row) {
  try {
    const data = await testSysInterface(row.id)
    if (data.success) ElMessage.success('连通性测试成功：' + data.message)
    else ElMessage.warning('连通性测试失败：' + data.message)
    fetchInterfaces()
    fetchAllStats()
  } catch (e) {
    ElMessage.error('测试失败：' + e.message)
  }
}
async function handleInterfaceExport() {
  try {
    const res = await exportSysInterfaces({ ...interfaceQuery })
    downloadBlob(res.data, '系统接口.xlsx')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}
async function handleInterfaceTemplate() {
  try {
    const res = await downloadSysInterfaceTemplate()
    downloadBlob(res.data, '系统接口导入模板.xlsx')
  } catch (e) {
    ElMessage.error('下载失败：' + e.message)
  }
}

// ---------- 消息提醒设置 ----------
const noticeData = ref([])
const noticeTotal = ref(0)
const noticeLoading = ref(false)
const noticeQuery = reactive({ keyword: '', channel: '', enabled: '', page: 1, size: 10 })
const noticeDialogVisible = ref(false)
const noticeDialogTitle = ref('新增设置')
const noticeForm = reactive({ id: null, settingKey: '', settingName: '', channel: 'SITE', triggerEvent: '', templateContent: '', receiverScope: 'ALL', receiverValue: '', enabled: 1, remark: '' })

async function fetchNotices() {
  noticeLoading.value = true
  try {
    const data = await getSysNoticeSettings({ ...noticeQuery })
    noticeData.value = data.records || []
    noticeTotal.value = data.total || 0
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    noticeLoading.value = false
  }
}
function resetNoticeQuery() {
  noticeQuery.keyword = ''
  noticeQuery.channel = ''
  noticeQuery.enabled = ''
  noticeQuery.page = 1
  fetchNotices()
}
function resetNoticeForm() {
  noticeForm.id = null
  noticeForm.settingKey = ''
  noticeForm.settingName = ''
  noticeForm.channel = 'SITE'
  noticeForm.triggerEvent = ''
  noticeForm.templateContent = ''
  noticeForm.receiverScope = 'ALL'
  noticeForm.receiverValue = ''
  noticeForm.enabled = 1
  noticeForm.remark = ''
}
function handleNoticeAdd() {
  resetNoticeForm()
  noticeDialogTitle.value = '新增设置'
  noticeDialogVisible.value = true
}
function handleNoticeEdit(row) {
  resetNoticeForm()
  Object.assign(noticeForm, row)
  noticeDialogTitle.value = '编辑设置'
  noticeDialogVisible.value = true
}
async function submitNotice() {
  try {
    if (noticeForm.id) {
      await updateSysNoticeSetting(noticeForm.id, { ...noticeForm })
      ElMessage.success('修改成功')
    } else {
      await createSysNoticeSetting({ ...noticeForm })
      ElMessage.success('新增成功')
    }
    noticeDialogVisible.value = false
    fetchNotices()
    fetchAllStats()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}
async function toggleNotice(row, enabled) {
  try {
    if (enabled === 1) await enableSysNoticeSetting(row.id)
    else await disableSysNoticeSetting(row.id)
    ElMessage.success(enabled === 1 ? '已启用' : '已停用')
    fetchNotices()
    fetchAllStats()
  } catch (e) {
    ElMessage.error('操作失败：' + e.message)
  }
}
async function handleNoticeDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除设置「${row.settingName}」吗？`, '删除', { type: 'warning' })
    await deleteSysNoticeSetting(row.id)
    ElMessage.success('删除成功')
    fetchNotices()
    fetchAllStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + e.message)
  }
}
async function handleNoticeExport() {
  try {
    const res = await exportSysNoticeSettings({ ...noticeQuery })
    downloadBlob(res.data, '消息提醒设置.xlsx')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}
async function handleNoticeTemplate() {
  try {
    const res = await downloadSysNoticeSettingTemplate()
    downloadBlob(res.data, '消息提醒设置导入模板.xlsx')
  } catch (e) {
    ElMessage.error('下载失败：' + e.message)
  }
}

// ---------- 知识库 ----------
const knowledgeData = ref([])
const knowledgeTotal = ref(0)
const knowledgeLoading = ref(false)
const knowledgeQuery = reactive({ keyword: '', category: '', status: '', page: 1, size: 10 })
const knowledgeDialogVisible = ref(false)
const knowledgeDialogTitle = ref('新增知识')
const knowledgeForm = reactive({ id: null, kbCode: '', title: '', category: '其他', content: '', keywords: '', status: 1, sortOrder: 0, remark: '' })
const knowledgeDetailVisible = ref(false)
const knowledgeDetail = ref(null)

async function fetchKnowledge() {
  knowledgeLoading.value = true
  try {
    const data = await getKnowledgeItems({ ...knowledgeQuery })
    knowledgeData.value = data.records || []
    knowledgeTotal.value = data.total || 0
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    knowledgeLoading.value = false
  }
}
function resetKnowledgeQuery() {
  knowledgeQuery.keyword = ''
  knowledgeQuery.category = ''
  knowledgeQuery.status = ''
  knowledgeQuery.page = 1
  fetchKnowledge()
}
function resetKnowledgeForm() {
  knowledgeForm.id = null
  knowledgeForm.kbCode = ''
  knowledgeForm.title = ''
  knowledgeForm.category = '其他'
  knowledgeForm.content = ''
  knowledgeForm.keywords = ''
  knowledgeForm.status = 1
  knowledgeForm.sortOrder = 0
  knowledgeForm.remark = ''
}
function handleKnowledgeAdd() {
  resetKnowledgeForm()
  knowledgeDialogTitle.value = '新增知识'
  knowledgeDialogVisible.value = true
}
function handleKnowledgeEdit(row) {
  resetKnowledgeForm()
  Object.assign(knowledgeForm, row)
  knowledgeDialogTitle.value = '编辑知识'
  knowledgeDialogVisible.value = true
}
async function submitKnowledge() {
  try {
    if (knowledgeForm.id) {
      await updateKnowledgeItem(knowledgeForm.id, { ...knowledgeForm })
      ElMessage.success('修改成功')
    } else {
      await createKnowledgeItem({ ...knowledgeForm })
      ElMessage.success('新增成功')
    }
    knowledgeDialogVisible.value = false
    fetchKnowledge()
    fetchAllStats()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}
async function toggleKnowledge(row, status) {
  try {
    if (status === 1) await enableKnowledgeItem(row.id)
    else await disableKnowledgeItem(row.id)
    ElMessage.success(status === 1 ? '已启用' : '已停用')
    fetchKnowledge()
    fetchAllStats()
  } catch (e) {
    ElMessage.error('操作失败：' + e.message)
  }
}
async function handleKnowledgeDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除知识「${row.title}」吗？`, '删除', { type: 'warning' })
    await deleteKnowledgeItem(row.id)
    ElMessage.success('删除成功')
    fetchKnowledge()
    fetchAllStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + e.message)
  }
}
async function handleKnowledgeDetail(row) {
  try {
    knowledgeDetail.value = await getKnowledgeItem(row.id)
    knowledgeDetailVisible.value = true
    fetchKnowledge()
    fetchAllStats()
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  }
}
async function handleKnowledgeExport() {
  try {
    const res = await exportKnowledgeItems({ ...knowledgeQuery })
    downloadBlob(res.data, '知识库.xlsx')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}
async function handleKnowledgeTemplate() {
  try {
    const res = await downloadKnowledgeItemTemplate()
    downloadBlob(res.data, '知识库导入模板.xlsx')
  } catch (e) {
    ElMessage.error('下载失败：' + e.message)
  }
}

onMounted(() => {
  fetchOverview()
  fetchAllStats()
})
</script>

<style scoped>
.stat { text-align: center; }
.stat .num { font-size: 26px; font-weight: bold; }
.stat .num.ok { color: #67c23a; }
.stat .num.warn { color: #e6a23c; }
.stat .label { color: #909399; margin-top: 4px; }
.page-note { color: #909399; font-size: 13px; margin: 0 0 12px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.search-form { margin-bottom: 0; }
</style>
