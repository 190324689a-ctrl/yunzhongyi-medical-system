<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="5"><el-card><div class="stat"><div class="num">{{ stats.total || 0 }}</div><div class="label">检查计划总数</div></div></el-card></el-col>
      <el-col :span="5"><el-card><div class="stat"><div class="num warn">{{ stats.inProgress || 0 }}</div><div class="label">进行中</div></div></el-card></el-col>
      <el-col :span="5"><el-card><div class="stat"><div class="num ok">{{ stats.completed || 0 }}</div><div class="label">已完成</div></div></el-card></el-col>
      <el-col :span="5"><el-card><div class="stat"><div class="num">{{ stats.rectificationRate ?? 0 }}%</div><div class="label">整改完成率</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num bad">{{ stats.overdueCount || 0 }}</div><div class="label">逾期整改</div></div></el-card></el-col>
    </el-row>

    <!-- 搜索区 -->
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="模板 / 被查科室 / 检查人" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="被查科室">
          <el-select v-model="query.deptId" placeholder="全部科室" clearable style="width: 140px">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="检查类型">
          <el-select v-model="query.inspectType" placeholder="全部类型" clearable style="width: 130px">
            <el-option v-for="t in inspectTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="s in planStatuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划检查日期">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作区 + 列表 -->
    <el-card style="margin-top: 16px">
      <div class="toolbar">
        <div>
          <el-button type="primary" @click="handleAdd">新增检查计划</el-button>
          <el-button @click="templateVisible = true">检查模板管理</el-button>
        </div>
        <div>
          <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
          <el-upload
            action="/api/quality-inspections/import"
            :show-file-list="false"
            accept=".xlsx,.xls"
            :on-success="handleImportSuccess"
            :on-error="handleImportError"
            style="display: inline-block; margin: 0 8px"
          >
            <el-button type="success">批量导入</el-button>
          </el-upload>
          <el-button type="warning" @click="handleExport">导出检查记录</el-button>
          <el-button @click="handleDownloadTemplate">下载模板</el-button>
        </div>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading" @selection-change="selected = $event" style="margin-top: 12px">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="templateName" label="检查模板" width="170" />
        <el-table-column prop="inspectType" label="检查类型" width="100">
          <template #default="{ row }">{{ typeText(row.inspectType) }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="被查科室" width="110" />
        <el-table-column prop="inspector" label="检查人/检查组" width="130" />
        <el-table-column prop="planDate" label="计划检查日期" width="120" />
        <el-table-column prop="status" label="计划状态" width="100">
          <template #default="{ row }">
            <el-tag :type="planStatusTag(row.status)">{{ planStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情/打分</el-button>
            <el-button v-if="row.status === 0" link type="success" @click="handleStart(row)">开始</el-button>
            <el-button v-if="row.status === 1" link type="success" @click="handleComplete(row)">完成</el-button>
            <el-button v-if="row.status === 0 || row.status === 1" link type="warning" @click="handleCancel(row)">取消</el-button>
            <el-button v-if="row.status === 0" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>

    <!-- 逾期整改提醒 -->
    <el-card style="margin-top: 16px" v-if="overdueList.length">
      <template #header><span style="color: #f56c6c">逾期整改任务（{{ overdueList.length }} 条）</span></template>
      <el-table :data="overdueList" border stripe>
        <el-table-column prop="measure" label="整改措施" />
        <el-table-column prop="respPerson" label="整改责任人" width="110" />
        <el-table-column prop="deadline" label="整改期限" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><el-tag type="danger">{{ rectifyStatusText(row.status) }}（逾期）</el-tag></template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 按检查类型统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按检查类型统计</span></template>
      <el-table :data="typeStats" border stripe>
        <el-table-column prop="label" label="检查类型" />
        <el-table-column prop="count" label="计划数" />
      </el-table>
    </el-card>

    <!-- 按科室统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按科室统计</span></template>
      <el-table :data="deptStats" border stripe>
        <el-table-column prop="deptName" label="被查科室" />
        <el-table-column prop="count" label="计划数" />
        <el-table-column prop="pendingCount" label="待执行" />
        <el-table-column prop="inProgressCount" label="进行中" />
        <el-table-column prop="completedCount" label="已完成" />
        <el-table-column prop="recordCount" label="检查记录数" />
        <el-table-column prop="avgScore" label="平均分" />
      </el-table>
    </el-card>

    <!-- 按月趋势 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按月趋势</span></template>
      <el-table :data="monthStats" border stripe>
        <el-table-column prop="month" label="计划月份" />
        <el-table-column prop="count" label="计划数" />
        <el-table-column prop="completedCount" label="已完成" />
        <el-table-column prop="inProgressCount" label="进行中" />
      </el-table>
    </el-card>

    <!-- 新增 / 编辑检查计划弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="680px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="检查模板" prop="templateId">
              <el-select v-model="form.templateId" placeholder="请选择模板" style="width: 100%" @change="onTemplateChange">
                <el-option v-for="t in enabledTemplates" :key="t.id" :label="`${t.templateName}（${typeText(t.inspectType)}）`" :value="t.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检查类型">
              <el-input v-model="formInspectTypeText" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="被查科室" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择科室" style="width: 100%" @change="onDeptChange">
                <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划检查日期" prop="planDate">
              <el-date-picker v-model="form.planDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="检查人/检查组" prop="inspector"><el-input v-model="form.inspector" placeholder="多个检查人用顿号分隔，如：吴文斌、陈国强" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </el-row>
        <el-alert title="新建后为待执行；开始检查后才能逐项打分，至少一条检查记录才能完成；只有待执行的计划能修改。" type="info" :closable="false" show-icon />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 检查模板管理弹窗 -->
    <el-dialog title="检查模板管理（权重之和必须为 100）" v-model="templateVisible" width="860px">
      <el-table :data="templates" border stripe>
        <el-table-column prop="templateName" label="模板名称" width="180" />
        <el-table-column prop="inspectType" label="检查类型" width="100">
          <template #default="{ row }">{{ typeText(row.inspectType) }}</template>
        </el-table-column>
        <el-table-column prop="checkItems" label="检查项（权重）">
          <template #default="{ row }">{{ checkItemsText(row.checkItems) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleTemplateEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 1" link type="warning" @click="handleTemplateDisable(row)">停用</el-button>
            <el-button v-else link type="success" @click="handleTemplateEnable(row)">启用</el-button>
            <el-button link type="danger" @click="handleTemplateDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 12px"><el-button type="primary" @click="handleTemplateAdd">新增模板</el-button></div>

      <el-form :model="templateForm" :rules="templateRules" ref="templateFormRef" label-width="100px" style="margin-top: 16px" v-if="templateFormVisible">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模板名称" prop="templateName"><el-input v-model="templateForm.templateName" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检查类型" prop="inspectType">
              <el-select v-model="templateForm.inspectType" placeholder="请选择" style="width: 100%">
                <el-option v-for="t in inspectTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <div style="margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center">
          <strong>检查项（权重之和：{{ templateWeightSum }} / 100）</strong>
          <el-button size="small" @click="addCheckItem">添加检查项</el-button>
        </div>
        <el-row :gutter="8" v-for="(item, idx) in templateItems" :key="idx" style="margin-bottom: 8px">
          <el-col :span="8"><el-input v-model="item.name" placeholder="检查项名称" /></el-col>
          <el-col :span="5"><el-input-number v-model="item.weight" :min="1" :max="100" placeholder="权重" style="width: 100%" /></el-col>
          <el-col :span="9"><el-input v-model="item.standard" placeholder="评分标准（可选）" /></el-col>
          <el-col :span="2"><el-button link type="danger" @click="templateItems.splice(idx, 1)">删除</el-button></el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="templateForm.remark" type="textarea" :rows="2" /></el-form-item>
        <el-button type="primary" @click="handleTemplateSubmit">保存模板</el-button>
        <el-button @click="templateFormVisible = false">取消</el-button>
      </el-form>
    </el-dialog>

    <!-- 详情 / 打分 / 问题 / 整改弹窗 -->
    <el-dialog title="检查详情与打分" v-model="detailVisible" width="960px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="检查模板">{{ detail.plan?.templateName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="检查类型">{{ typeText(detail.plan?.inspectType) }}</el-descriptions-item>
        <el-descriptions-item label="被查科室">{{ detail.plan?.deptName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="检查人/检查组">{{ detail.plan?.inspector || '—' }}</el-descriptions-item>
        <el-descriptions-item label="计划检查日期">{{ detail.plan?.planDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="计划状态">
          <el-tag :type="planStatusTag(detail.plan?.status)">{{ planStatusText(detail.plan?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.plan?.remark || '—' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 检查报告摘要 -->
      <el-card style="margin-top: 16px" v-if="report">
        <template #header><span>检查报告摘要</span></template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="检查记录数">{{ report.recordCount }} 条</el-descriptions-item>
          <el-descriptions-item label="平均分">{{ report.avgScore }}</el-descriptions-item>
          <el-descriptions-item label="发现问题">{{ report.issueCount }} 个</el-descriptions-item>
          <el-descriptions-item label="整改任务">{{ report.rectificationTotal }} 个</el-descriptions-item>
          <el-descriptions-item label="已复查通过">{{ report.rectificationReviewed }} 个</el-descriptions-item>
          <el-descriptions-item label="整改完成率">{{ report.rectificationRate }}%</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <div style="margin: 16px 0 8px; display: flex; justify-content: space-between; align-items: center">
        <strong>检查记录（{{ (detail.records || []).length }} 条，逐项打分，总分与等级自动算）</strong>
        <el-button v-if="detail.plan?.status === 1" type="primary" size="small" @click="openRecordForm">添加检查记录</el-button>
      </div>
      <el-table :data="detail.records || []" border stripe>
        <el-table-column prop="checkDate" label="检查日期" width="110" />
        <el-table-column prop="inspector" label="检查人" width="120" />
        <el-table-column prop="itemScores" label="各项得分" width="260">
          <template #default="{ row }">{{ itemScoresText(row.itemScores) }}</template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column prop="grade" label="等级" width="90">
          <template #default="{ row }"><el-tag :type="gradeTag(row.grade)">{{ gradeText(row.grade) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="result" label="检查结果" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openIssueForm(row)">发现问题</el-button>
            <el-button link type="danger" @click="handleDeleteRecord(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!(detail.records || []).length" description="暂无检查记录：开始检查后可逐项打分" />

      <!-- 添加检查记录表单（按模板动态生成评分项） -->
      <el-form :model="recordForm" :rules="recordRules" ref="recordFormRef" label-width="110px" style="margin-top: 16px" v-if="recordFormVisible">
        <el-alert :title="`按模板「${detail.plan?.templateName}」逐项打分（每项不超过权重），总分 ${recordTotal} 分，等级：${gradeText(gradeOf(recordTotal))}`" type="info" :closable="false" show-icon style="margin-bottom: 12px" />
        <el-row :gutter="16">
          <el-col :span="12" v-for="item in recordItems" :key="item.name">
            <el-form-item :label="`${item.name}（${item.weight}分）`">
              <el-input-number v-model="recordScores[item.name]" :min="0" :max="item.weight" style="width: 100%" />
              <div style="color: #909399; font-size: 12px">{{ item.standard || '' }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检查日期" prop="checkDate">
              <el-date-picker v-model="recordForm.checkDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检查人" prop="inspector"><el-input v-model="recordForm.inspector" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="检查结果"><el-input v-model="recordForm.result" type="textarea" :rows="2" placeholder="检查结论，如：合格，发现 2 个问题需整改" /></el-form-item>
          </el-col>
        </el-row>
        <el-button type="primary" @click="handleRecordSubmit">保存记录</el-button>
        <el-button @click="recordFormVisible = false">取消</el-button>
      </el-form>

      <!-- 添加问题表单 -->
      <el-form :model="issueForm" :rules="issueRules" ref="issueFormRef" label-width="110px" style="margin-top: 16px" v-if="issueFormVisible">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="问题描述" prop="description"><el-input v-model="issueForm.description" type="textarea" :rows="2" placeholder="发现的问题" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="严重程度" prop="severity">
              <el-select v-model="issueForm.severity" style="width: 100%">
                <el-option v-for="s in severities" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="责任科室"><el-input v-model="issueForm.respDeptName" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="责任人"><el-input v-model="issueForm.respPerson" placeholder="责任人姓名" /></el-form-item>
          </el-col>
        </el-row>
        <el-button type="primary" @click="handleIssueSubmit">保存问题</el-button>
        <el-button @click="issueFormVisible = false">取消</el-button>
      </el-form>

      <!-- 问题清单 -->
      <div style="margin: 16px 0 8px"><strong>发现的问题（{{ (detail.issues || []).length }} 个）</strong></div>
      <el-table :data="detail.issues || []" border stripe>
        <el-table-column prop="description" label="问题描述" />
        <el-table-column prop="severity" label="严重程度" width="90">
          <template #default="{ row }"><el-tag :type="severityTag(row.severity)">{{ severityText(row.severity) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="respDeptName" label="责任科室" width="110" />
        <el-table-column prop="respPerson" label="责任人" width="90" />
        <el-table-column prop="foundDate" label="发现日期" width="110" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRectificationForm(row)">发整改任务</el-button>
            <el-button link type="danger" @click="handleDeleteIssue(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!(detail.issues || []).length" description="暂无发现的问题" />

      <!-- 发整改任务表单 -->
      <el-form :model="rectificationForm" :rules="rectificationRules" ref="rectificationFormRef" label-width="110px" style="margin-top: 16px" v-if="rectificationFormVisible">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="整改措施" prop="measure"><el-input v-model="rectificationForm.measure" type="textarea" :rows="2" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="整改期限" prop="deadline">
              <el-date-picker v-model="rectificationForm.deadline" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="整改责任人" prop="respPerson"><el-input v-model="rectificationForm.respPerson" /></el-form-item>
          </el-col>
        </el-row>
        <el-button type="primary" @click="handleRectificationSubmit">发送整改任务</el-button>
        <el-button @click="rectificationFormVisible = false">取消</el-button>
      </el-form>

      <!-- 整改跟踪 -->
      <div style="margin: 16px 0 8px"><strong>整改跟踪（{{ (detail.rectifications || []).length }} 个任务）</strong></div>
      <el-table :data="detail.rectifications || []" border stripe>
        <el-table-column prop="measure" label="整改措施" />
        <el-table-column prop="respPerson" label="整改责任人" width="100" />
        <el-table-column prop="deadline" label="整改期限" width="110" />
        <el-table-column prop="status" label="整改状态" width="130">
          <template #default="{ row }">
            <el-tag :type="rectifyStatusTag(row)">{{ rectifyStatusText(row.status) }}{{ row.overdue ? '（逾期）' : '' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewer" label="复查人" width="90" />
        <el-table-column prop="reviewOpinion" label="复查意见" width="160" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" link type="success" @click="handleRectificationSubmit2(row)">提交整改</el-button>
            <el-button v-if="row.status === 1" link type="primary" @click="openReviewForm(row)">复查</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!(detail.rectifications || []).length" description="暂无整改任务：针对问题发整改任务后在此跟踪" />
    </el-dialog>

    <!-- 复查弹窗 -->
    <el-dialog title="整改复查" v-model="reviewVisible" width="560px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="整改措施">{{ reviewRow.measure || '—' }}</el-descriptions-item>
        <el-descriptions-item label="整改责任人">{{ reviewRow.respPerson || '—' }} · 期限 {{ reviewRow.deadline || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="reviewForm" label-width="90px" style="margin-top: 16px">
        <el-form-item label="复查人">
          <el-input v-model="reviewForm.reviewer" placeholder="复查人姓名" />
        </el-form-item>
        <el-form-item label="复查意见">
          <el-input v-model="reviewForm.opinion" type="textarea" :rows="2" placeholder="复查意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReviewReject">复查打回</el-button>
        <el-button type="primary" @click="handleReviewPass">复查通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getQualityPlans, getQualityStats, getQualityStatsByDept, getQualityStatsByMonth,
  getQualityPlan, createQualityPlan, updateQualityPlan, deleteQualityPlan, batchDeleteQualityPlans,
  startQualityPlan, completeQualityPlan, cancelQualityPlan, getQualityReport,
  addQualityRecord, deleteQualityRecord,
  addQualityIssue, deleteQualityIssue,
  addQualityRectification, submitRectification, reviewPassRectification, reviewRejectRectification,
  getRectificationPage, exportQualityRecords, downloadQualityTemplate,
  getQualityTemplates, getQualityTemplate, getEnabledQualityTemplates,
  createQualityTemplate, updateQualityTemplate, deleteQualityTemplate,
  enableQualityTemplate, disableQualityTemplate
} from '../../api/quality'
import { getEnabledDepts } from '../../api/system'

const inspectTypes = [
  { value: 'SELF', label: '科室自查' },
  { value: 'HOSPITAL', label: '院级检查' },
  { value: 'SPECIAL', label: '专项检查' }
]
const planStatuses = [
  { value: 0, label: '待执行' },
  { value: 1, label: '进行中' },
  { value: 2, label: '已完成' },
  { value: 3, label: '已取消' }
]
const severities = [
  { value: 'MAJOR', label: '严重' },
  { value: 'MINOR', label: '一般' },
  { value: 'MILD', label: '轻微' }
]
const grades = [
  { value: 'EXCELLENT', label: '优秀' },
  { value: 'QUALIFIED', label: '合格' },
  { value: 'BASIC', label: '基本合格' },
  { value: 'UNQUALIFIED', label: '不合格' }
]
const typeText = (v) => inspectTypes.find((t) => t.value === v)?.label || v || ''
const planStatusText = (v) => planStatuses.find((s) => s.value === v)?.label || ''
const planStatusTag = (v) => ({ 0: 'info', 1: 'warning', 2: 'success', 3: '' }[v] || 'info')
const severityText = (v) => severities.find((s) => s.value === v)?.label || v || ''
const severityTag = (v) => ({ MAJOR: 'danger', MINOR: 'warning', MILD: '' }[v] || 'info')
const gradeText = (v) => grades.find((g) => g.value === v)?.label || v || ''
const gradeTag = (v) => ({ EXCELLENT: 'success', QUALIFIED: '', BASIC: 'warning', UNQUALIFIED: 'danger' }[v] || 'info')
const gradeOf = (total) => (total >= 90 ? 'EXCELLENT' : total >= 80 ? 'QUALIFIED' : total >= 60 ? 'BASIC' : 'UNQUALIFIED')
const rectifyStatuses = [
  { value: 0, label: '待整改' },
  { value: 1, label: '已整改' },
  { value: 2, label: '已复查通过' }
]
const rectifyStatusText = (v) => rectifyStatuses.find((s) => s.value === v)?.label || ''
const rectifyStatusTag = (row) => (row.overdue ? 'danger' : ({ 0: 'warning', 1: '', 2: 'success' }[row.status] || 'info'))
const checkItemsText = (json) => {
  try {
    return (JSON.parse(json || '[]') || []).map((i) => `${i.name}${i.weight}`).join('、')
  } catch (e) {
    return ''
  }
}
const itemScoresText = (json) => {
  try {
    return Object.entries(JSON.parse(json || '{}')).map(([k, v]) => `${k} ${v}`).join('；')
  } catch (e) {
    return ''
  }
}

const query = reactive({ keyword: '', deptId: null, inspectType: '', status: null, startDate: null, endDate: null, page: 1, size: 10 })
const dateRange = ref(null)
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])
const depts = ref([])
const enabledTemplates = ref([])
const templates = ref([])
const stats = ref({})
const deptStats = ref([])
const monthStats = ref([])
const overdueList = ref([])
const typeStats = computed(() =>
  inspectTypes.map((t) => ({ label: t.label, count: stats.value.byType?.[t.value] || 0 }))
)

const dialogVisible = ref(false)
const dialogTitle = ref('新增检查计划')
const formRef = ref(null)
const form = reactive({
  id: null, templateId: null, templateName: '', inspectType: '', deptId: null, deptName: '',
  inspector: '', planDate: '', remark: ''
})
const formInspectTypeText = computed(() => typeText(form.inspectType))
const rules = {
  templateId: [{ required: true, message: '请选择检查模板', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择被查科室', trigger: 'change' }],
  inspector: [{ required: true, message: '请填写检查人/检查组', trigger: 'blur' }],
  planDate: [{ required: true, message: '请选择计划检查日期', trigger: 'change' }]
}

const templateVisible = ref(false)
const templateFormVisible = ref(false)
const templateFormRef = ref(null)
const templateForm = reactive({ id: null, templateName: '', inspectType: 'SELF', remark: '' })
const templateItems = ref([{ name: '', weight: 25, standard: '' }])
const templateWeightSum = computed(() => templateItems.value.reduce((s, i) => s + (Number(i.weight) || 0), 0))
const templateRules = {
  templateName: [{ required: true, message: '请填写模板名称', trigger: 'blur' }],
  inspectType: [{ required: true, message: '请选择检查类型', trigger: 'change' }]
}

const detailVisible = ref(false)
const detail = ref({ plan: null, records: [], issues: [], rectifications: [] })
const report = ref(null)

const recordFormVisible = ref(false)
const recordFormRef = ref(null)
const recordForm = reactive({ inspector: '', checkDate: '', result: '' })
const recordItems = ref([])
const recordScores = reactive({})
const recordTotal = computed(() => Object.values(recordScores).reduce((s, v) => s + (Number(v) || 0), 0))
const recordRules = {
  inspector: [{ required: true, message: '请填写检查人', trigger: 'blur' }],
  checkDate: [{ required: true, message: '请选择检查日期', trigger: 'change' }]
}

const issueFormVisible = ref(false)
const issueFormRef = ref(null)
const issueRecord = ref(null)
const issueForm = reactive({ description: '', severity: 'MINOR', respDeptName: '', respPerson: '' })
const issueRules = {
  description: [{ required: true, message: '请填写问题描述', trigger: 'blur' }],
  severity: [{ required: true, message: '请选择严重程度', trigger: 'change' }]
}

const rectificationFormVisible = ref(false)
const rectificationFormRef = ref(null)
const rectificationIssue = ref(null)
const rectificationForm = reactive({ measure: '', deadline: '', respPerson: '' })
const rectificationRules = {
  measure: [{ required: true, message: '请填写整改措施', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择整改期限', trigger: 'change' }],
  respPerson: [{ required: true, message: '请填写整改责任人', trigger: 'blur' }]
}

const reviewVisible = ref(false)
const reviewRow = ref({})
const reviewForm = reactive({ reviewer: '吴文斌', opinion: '' })

async function fetchList() {
  loading.value = true
  try {
    const data = await getQualityPlans({ ...query })
    tableData.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  try {
    stats.value = await getQualityStats()
    deptStats.value = await getQualityStatsByDept()
    monthStats.value = await getQualityStatsByMonth()
    const overdue = await getRectificationPage({ overdueOnly: true, page: 1, size: 100 })
    overdueList.value = overdue.records || []
  } catch (e) {
    console.warn('统计加载失败', e.message)
  }
}

async function fetchTemplates() {
  try {
    const page = await getQualityTemplates({ page: 1, size: 100 })
    templates.value = page.records || []
    enabledTemplates.value = await getEnabledQualityTemplates()
  } catch (e) {
    console.warn('模板加载失败', e.message)
  }
}

function refresh() {
  fetchList()
  fetchStats()
  fetchTemplates()
}

function handleSearch() {
  query.startDate = dateRange.value ? dateRange.value[0] : null
  query.endDate = dateRange.value ? dateRange.value[1] : null
  query.page = 1
  fetchList()
}

function handleReset() {
  Object.assign(query, { keyword: '', deptId: null, inspectType: '', status: null, startDate: null, endDate: null, page: 1, size: 10 })
  dateRange.value = null
  fetchList()
}

function resetForm() {
  Object.assign(form, {
    id: null, templateId: null, templateName: '', inspectType: '', deptId: null, deptName: '',
    inspector: '', planDate: '', remark: ''
  })
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增检查计划'
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  Object.assign(form, row)
  dialogTitle.value = '编辑检查计划'
  dialogVisible.value = true
}

function onTemplateChange(id) {
  const t = enabledTemplates.value.find((x) => x.id === id)
  if (t) {
    form.templateName = t.templateName
    form.inspectType = t.inspectType
  }
}

function onDeptChange(id) {
  const d = depts.value.find((x) => x.id === id)
  if (d) {
    form.deptName = d.name
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    if (form.id) {
      await updateQualityPlan(form.id, { ...form })
      ElMessage.success('检查计划修改成功')
    } else {
      await createQualityPlan({ ...form })
      ElMessage.success('检查计划新增成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.templateName} / ${row.deptName}」的检查计划吗？`, '提示', { type: 'warning' })
  await deleteQualityPlan(row.id)
  ElMessage.success('删除成功')
  refresh()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selected.value.length} 个检查计划吗？`, '提示', { type: 'warning' })
  await batchDeleteQualityPlans(selected.value.map((r) => r.id))
  ElMessage.success('批量删除成功')
  refresh()
}

async function handleStart(row) {
  await startQualityPlan(row.id)
  ElMessage.success('已开始检查，可以逐项打分了')
  refresh()
}

async function handleComplete(row) {
  await completeQualityPlan(row.id)
  ElMessage.success('检查已完成')
  refresh()
}

async function handleCancel(row) {
  await ElMessageBox.confirm('确定取消这个检查计划吗？', '提示', { type: 'warning' })
  await cancelQualityPlan(row.id)
  ElMessage.success('已取消')
  refresh()
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

async function handleExport() {
  try {
    const res = await exportQualityRecords({ ...query, page: 1, size: 10000 })
    downloadBlob(res.data, '质量检查记录.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleDownloadTemplate() {
  const res = await downloadQualityTemplate()
  downloadBlob(res.data, '质量检查计划导入模板.xlsx')
}

function handleImportSuccess(res) {
  if (res.code === 0) {
    const d = res.data || {}
    ElMessage.success(`导入完成：成功 ${d.imported || 0} 条，跳过 ${d.skipped || 0} 条`)
    if ((d.details || []).length) {
      ElMessageBox.alert((d.details || []).join('\n'), '导入说明', { type: 'info' })
    }
    refresh()
  } else {
    ElMessage.error('导入失败：' + (res.msg || '未知错误'))
  }
}

function handleImportError() {
  ElMessage.error('导入失败，请检查文件格式')
}

// ---- 检查模板管理 ----
function resetTemplateForm() {
  Object.assign(templateForm, { id: null, templateName: '', inspectType: 'SELF', remark: '' })
  templateItems.value = [{ name: '', weight: 25, standard: '' }]
}

function handleTemplateAdd() {
  resetTemplateForm()
  templateFormVisible.value = true
}

function handleTemplateEdit(row) {
  resetTemplateForm()
  Object.assign(templateForm, row)
  try {
    templateItems.value = JSON.parse(row.checkItems || '[]')
  } catch (e) {
    templateItems.value = []
  }
  templateFormVisible.value = true
}

function addCheckItem() {
  templateItems.value.push({ name: '', weight: 10, standard: '' })
}

async function handleTemplateSubmit() {
  await templateFormRef.value.validate()
  if (templateWeightSum.value !== 100) {
    ElMessage.error(`检查项权重之和必须为 100，当前为 ${templateWeightSum.value}`)
    return
  }
  try {
    const data = { ...templateForm, checkItems: JSON.stringify(templateItems.value) }
    if (templateForm.id) {
      await updateQualityTemplate(templateForm.id, data)
      ElMessage.success('模板修改成功')
    } else {
      await createQualityTemplate(data)
      ElMessage.success('模板新增成功')
    }
    templateFormVisible.value = false
    fetchTemplates()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleTemplateEnable(row) {
  await enableQualityTemplate(row.id)
  ElMessage.success('模板已启用')
  fetchTemplates()
}

async function handleTemplateDisable(row) {
  await disableQualityTemplate(row.id)
  ElMessage.success('模板已停用')
  fetchTemplates()
}

async function handleTemplateDelete(row) {
  await ElMessageBox.confirm(`确定删除模板「${row.templateName}」吗？`, '提示', { type: 'warning' })
  await deleteQualityTemplate(row.id)
  ElMessage.success('删除成功')
  fetchTemplates()
}

// ---- 详情 / 打分 / 问题 / 整改 ----
async function reloadDetail() {
  detail.value = await getQualityPlan(detail.value.plan.id)
  report.value = await getQualityReport(detail.value.plan.id)
}

async function handleDetail(row) {
  try {
    detail.value = await getQualityPlan(row.id)
    report.value = await getQualityReport(row.id)
    recordFormVisible.value = false
    issueFormVisible.value = false
    rectificationFormVisible.value = false
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败：' + e.message)
  }
}

async function openRecordForm() {
  try {
    const template = await getQualityTemplate(detail.value.plan.templateId)
    recordItems.value = JSON.parse(template.checkItems || '[]')
    Object.keys(recordScores).forEach((k) => delete recordScores[k])
    recordItems.value.forEach((i) => { recordScores[i.name] = 0 })
    Object.assign(recordForm, { inspector: detail.value.plan.inspector || '', checkDate: '', result: '' })
    recordFormVisible.value = true
  } catch (e) {
    ElMessage.error('加载模板检查项失败：' + e.message)
  }
}

async function handleRecordSubmit() {
  await recordFormRef.value.validate()
  try {
    const itemScores = {}
    recordItems.value.forEach((i) => { itemScores[i.name] = Number(recordScores[i.name]) || 0 })
    await addQualityRecord(detail.value.plan.id, {
      ...recordForm, itemScores: JSON.stringify(itemScores)
    })
    ElMessage.success('检查记录添加成功')
    recordFormVisible.value = false
    await reloadDetail()
    fetchStats()
  } catch (e) {
    ElMessage.error('添加失败：' + e.message)
  }
}

async function handleDeleteRecord(row) {
  await ElMessageBox.confirm('确定删除这条检查记录吗？该记录下的问题与整改任务会一并删除。', '提示', { type: 'warning' })
  await deleteQualityRecord(detail.value.plan.id, row.id)
  ElMessage.success('删除成功')
  await reloadDetail()
  fetchStats()
}

function openIssueForm(record) {
  issueRecord.value = record
  Object.assign(issueForm, { description: '', severity: 'MINOR', respDeptName: record.deptName || '', respPerson: '' })
  issueFormVisible.value = true
}

async function handleIssueSubmit() {
  await issueFormRef.value.validate()
  try {
    await addQualityIssue(issueRecord.value.id, { ...issueForm })
    ElMessage.success('问题添加成功')
    issueFormVisible.value = false
    await reloadDetail()
    fetchStats()
  } catch (e) {
    ElMessage.error('添加失败：' + e.message)
  }
}

async function handleDeleteIssue(row) {
  await ElMessageBox.confirm('确定删除这个问题吗？该问题下的整改任务会一并删除。', '提示', { type: 'warning' })
  await deleteQualityIssue(row.recordId, row.id)
  ElMessage.success('删除成功')
  await reloadDetail()
  fetchStats()
}

function openRectificationForm(issue) {
  rectificationIssue.value = issue
  Object.assign(rectificationForm, { measure: '', deadline: '', respPerson: issue.respPerson || '' })
  rectificationFormVisible.value = true
}

async function handleRectificationSubmit() {
  await rectificationFormRef.value.validate()
  try {
    await addQualityRectification(rectificationIssue.value.id, { ...rectificationForm })
    ElMessage.success('整改任务发送成功')
    rectificationFormVisible.value = false
    await reloadDetail()
    fetchStats()
  } catch (e) {
    ElMessage.error('发送失败：' + e.message)
  }
}

async function handleRectificationSubmit2(row) {
  await submitRectification(row.id)
  ElMessage.success('整改已提交，待复查')
  await reloadDetail()
  fetchStats()
}

function openReviewForm(row) {
  reviewRow.value = row
  Object.assign(reviewForm, { reviewer: '吴文斌', opinion: '' })
  reviewVisible.value = true
}

async function handleReviewPass() {
  try {
    await reviewPassRectification(reviewRow.value.id, { reviewer: reviewForm.reviewer, opinion: reviewForm.opinion })
    ElMessage.success('复查通过')
    reviewVisible.value = false
    await reloadDetail()
    fetchStats()
  } catch (e) {
    ElMessage.error('复查失败：' + e.message)
  }
}

async function handleReviewReject() {
  try {
    await reviewRejectRectification(reviewRow.value.id, { reviewer: reviewForm.reviewer, opinion: reviewForm.opinion })
    ElMessage.success('已打回待整改')
    reviewVisible.value = false
    await reloadDetail()
    fetchStats()
  } catch (e) {
    ElMessage.error('打回失败：' + e.message)
  }
}

onMounted(async () => {
  try {
    depts.value = await getEnabledDepts()
  } catch (e) {
    console.warn('基础数据加载失败（后端未启动？）', e.message)
  }
  refresh()
})
</script>

<style scoped>
.search-form {
  margin-bottom: -12px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
}
.stat {
  text-align: center;
}
.stat .num {
  font-size: 28px;
  font-weight: bold;
}
.stat .num.warn { color: #e6a23c; }
.stat .num.ok { color: #67c23a; }
.stat .num.bad { color: #f56c6c; }
.stat .label {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}
</style>
