<template>
  <div>
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" style="margin-bottom: 4px">
      <el-tab-pane label="工作统计" name="work" />
      <el-tab-pane label="三级医院医疗质量指标统计" name="tertiary" />
      <el-tab-pane label="医疗质量统计" name="quality" />
    </el-tabs>
    <p class="page-note">统计数据全部取自库内已有业务数据现算：工作量登记、排班记录、质量检查、医师考核、MDT、新技术新项目；排班人次与诊疗量是两个口径，分开列示、不混算。</p>

    <!-- ============ 工作统计 ============ -->
    <div v-show="activeTab === 'work'">
      <el-row :gutter="16" style="margin-bottom: 16px">
        <el-col :span="6"><el-card><div class="stat"><div class="num">{{ workStats.totalOutpatient || 0 }}</div><div class="label">门诊量（人次）</div></div></el-card></el-col>
        <el-col :span="6"><el-card><div class="stat"><div class="num">{{ workStats.totalInpatient || 0 }}</div><div class="label">住院量（人次）</div></div></el-card></el-col>
        <el-col :span="6"><el-card><div class="stat"><div class="num">{{ workStats.totalSurgery || 0 }}</div><div class="label">手术量（台次）</div></div></el-card></el-col>
        <el-col :span="6"><el-card><div class="stat"><div class="num ok">{{ workStats.totalWorkload || 0 }}</div><div class="label">工作总量</div></div></el-card></el-col>
      </el-row>

      <el-card>
        <el-form :inline="true" :model="workQuery" class="search-form">
          <el-form-item label="关键字">
            <el-input v-model="workQuery.keyword" placeholder="医师 / 科室" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="统计月份">
            <el-date-picker v-model="workQuery.statMonth" type="month" placeholder="选择月份" value-format="YYYY-MM" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleWorkSearch">查询</el-button>
            <el-button @click="handleWorkReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card style="margin-top: 16px">
        <div class="toolbar">
          <div><el-button type="primary" @click="handleWorkAdd">新增工作量登记</el-button></div>
          <div>
            <el-button type="danger" :disabled="!workSelected.length" @click="handleWorkBatchDelete">批量删除</el-button>
            <el-upload action="/api/workloads/import" :show-file-list="false" accept=".xlsx,.xls" :on-success="handleWorkImportSuccess" :on-error="handleImportError" style="display: inline-block; margin: 0 8px">
              <el-button type="success">批量导入</el-button>
            </el-upload>
            <el-button type="warning" @click="handleWorkExport">导出 Excel</el-button>
            <el-button @click="handleWorkDownloadTemplate">下载模板</el-button>
          </div>
        </div>
        <el-table :data="workTableData" border stripe v-loading="workLoading" @selection-change="workSelected = $event" style="margin-top: 12px">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="physicianName" label="医师" width="90" />
          <el-table-column prop="deptName" label="科室" width="110" />
          <el-table-column prop="statMonth" label="统计月份" width="100" />
          <el-table-column prop="outpatientCount" label="门诊量（人次）" width="120" />
          <el-table-column prop="inpatientCount" label="住院量（人次）" width="120" />
          <el-table-column prop="surgeryCount" label="手术量（台次）" width="120" />
          <el-table-column label="合计" width="90">
            <template #default="{ row }">{{ (row.outpatientCount || 0) + (row.inpatientCount || 0) + (row.surgeryCount || 0) }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleWorkEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="handleWorkDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="workQuery.page" v-model:page-size="workQuery.size" :total="workTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchWorkList" @current-change="fetchWorkList" />
      </el-card>

      <el-card style="margin-top: 16px">
        <template #header><span>按科室统计</span></template>
        <el-table :data="workStats.byDept || []" border stripe>
          <el-table-column prop="deptName" label="科室" />
          <el-table-column prop="outpatientCount" label="门诊量（人次）" />
          <el-table-column prop="inpatientCount" label="住院量（人次）" />
          <el-table-column prop="surgeryCount" label="手术量（台次）" />
          <el-table-column prop="totalWorkload" label="合计" />
          <el-table-column prop="physicianCount" label="填报人数" />
        </el-table>
      </el-card>

      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="12">
          <el-card>
            <template #header><span>按人员统计</span></template>
            <el-table :data="workStats.byPhysician || []" border stripe>
              <el-table-column prop="physicianName" label="医师" />
              <el-table-column prop="deptName" label="科室" />
              <el-table-column prop="outpatientCount" label="门诊量" />
              <el-table-column prop="inpatientCount" label="住院量" />
              <el-table-column prop="surgeryCount" label="手术量" />
              <el-table-column prop="totalWorkload" label="合计" />
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header><span>按时间统计</span></template>
            <el-table :data="workStats.byMonth || []" border stripe>
              <el-table-column prop="month" label="月份" />
              <el-table-column prop="outpatientCount" label="门诊量" />
              <el-table-column prop="inpatientCount" label="住院量" />
              <el-table-column prop="surgeryCount" label="手术量" />
              <el-table-column prop="totalWorkload" label="合计" />
            </el-table>
            <p class="page-note" style="margin-top: 12px">排班人次（另一口径）：排班 {{ scheduleStats.total || 0 }} 条，其中夜班 {{ scheduleStats.nightCount || 0 }} 人次、待审批调班代班 {{ scheduleStats.pendingChange || 0 }} 条。排班人次不是诊疗量，不与上表合计混算。</p>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- ============ 三级医院医疗质量指标统计 ============ -->
    <div v-show="activeTab === 'tertiary'">
      <el-row :gutter="16" style="margin-bottom: 16px">
        <el-col :span="6"><el-card><div class="stat"><div class="num">{{ indicatorStats.total || 0 }}</div><div class="label">指标总数</div></div></el-card></el-col>
        <el-col :span="6"><el-card><div class="stat"><div class="num">{{ indicatorStats.reportedCount || 0 }}</div><div class="label">已填报</div></div></el-card></el-col>
        <el-col :span="6"><el-card><div class="stat"><div class="num ok">{{ indicatorStats.compliantCount || 0 }}</div><div class="label">达标数</div></div></el-card></el-col>
        <el-col :span="6"><el-card><div class="stat"><div class="num warn">{{ indicatorStats.complianceRate || 0 }}%</div><div class="label">达标率</div></div></el-card></el-col>
      </el-row>

      <el-card>
        <el-form :inline="true" :model="indicatorQuery" class="search-form">
          <el-form-item label="关键字">
            <el-input v-model="indicatorQuery.keyword" placeholder="指标编码 / 指标名称" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item label="标准级别">
            <el-select v-model="indicatorQuery.standardLevel" placeholder="全部级别" clearable style="width: 130px">
              <el-option v-for="l in levels" :key="l.value" :label="l.label" :value="l.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="指标分类">
            <el-select v-model="indicatorQuery.category" placeholder="全部分类" clearable style="width: 130px">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleIndicatorSearch">查询</el-button>
            <el-button @click="handleIndicatorReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card style="margin-top: 16px">
        <div class="toolbar">
          <div><el-button type="primary" @click="handleIndicatorAdd">新增指标</el-button></div>
          <div>
            <el-button type="danger" :disabled="!indicatorSelected.length" @click="handleIndicatorBatchDelete">批量删除</el-button>
            <el-upload action="/api/quality-indicators/import" :show-file-list="false" accept=".xlsx,.xls" :on-success="handleIndicatorImportSuccess" :on-error="handleImportError" style="display: inline-block; margin: 0 8px">
              <el-button type="success">批量导入</el-button>
            </el-upload>
            <el-button type="warning" @click="handleIndicatorExport">导出 Excel</el-button>
            <el-button @click="handleIndicatorDownloadTemplate">下载模板</el-button>
          </div>
        </div>
        <el-table :data="indicatorTableData" border stripe v-loading="indicatorLoading" @selection-change="indicatorSelected = $event" style="margin-top: 12px">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="indicatorCode" label="指标编码" width="90" />
          <el-table-column prop="indicatorName" label="指标名称" width="150" />
          <el-table-column prop="category" label="分类" width="90" />
          <el-table-column prop="standardLevel" label="标准级别" width="90">
            <template #default="{ row }">{{ levelText(row.standardLevel) }}</template>
          </el-table-column>
          <el-table-column prop="standardName" label="标准名称" width="160" />
          <el-table-column prop="targetValue" label="目标值" width="90" />
          <el-table-column prop="actualValue" label="实际值" width="90" />
          <el-table-column prop="unit" label="单位" width="60" />
          <el-table-column prop="direction" label="比较方向" width="90">
            <template #default="{ row }">{{ directionText(row.direction) }}</template>
          </el-table-column>
          <el-table-column prop="complianceText" label="达标情况" width="90">
            <template #default="{ row }">
              <el-tag :type="complianceTag(row.complianceText)">{{ row.complianceText || '未填报' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="statPeriod" label="统计周期" width="90" />
          <el-table-column prop="dataSource" label="数据来源" width="220" />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleIndicatorEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="handleIndicatorDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="indicatorQuery.page" v-model:page-size="indicatorQuery.size" :total="indicatorTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchIndicatorList" @current-change="fetchIndicatorList" />
      </el-card>

      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="12">
          <el-card>
            <template #header><span>按国家 / 省 / 市标准统计</span></template>
            <el-table :data="indicatorStats.byLevel || []" border stripe>
              <el-table-column prop="levelText" label="标准级别" />
              <el-table-column prop="total" label="指标数" />
              <el-table-column prop="reportedCount" label="已填报" />
              <el-table-column prop="compliantCount" label="达标数" />
              <el-table-column prop="complianceRate" label="达标率" width="220">
                <template #default="{ row }">
                  <el-progress :percentage="Number(row.complianceRate) || 0" :status="Number(row.complianceRate) >= 80 ? 'success' : Number(row.complianceRate) >= 50 ? 'warning' : 'exception'" />
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header><span>按指标分类统计</span></template>
            <el-table :data="indicatorStats.byCategory || []" border stripe>
              <el-table-column prop="category" label="指标分类" />
              <el-table-column prop="total" label="指标数" />
              <el-table-column prop="reportedCount" label="已填报" />
              <el-table-column prop="compliantCount" label="达标数" />
              <el-table-column prop="complianceRate" label="达标率" width="220">
                <template #default="{ row }">
                  <el-progress :percentage="Number(row.complianceRate) || 0" :status="Number(row.complianceRate) >= 80 ? 'success' : Number(row.complianceRate) >= 50 ? 'warning' : 'exception'" />
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- ============ 医疗质量统计 ============ -->
    <div v-show="activeTab === 'quality'">
      <el-row :gutter="16" style="margin-bottom: 16px">
        <el-col :span="4"><el-card><div class="stat"><div class="num">{{ qualityOverview.quality?.assessmentAvgScore ?? '-' }}</div><div class="label">医师考核平均分</div></div></el-card></el-col>
        <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ qualityOverview.quality?.assessmentExcellentRate ?? '-' }}%</div><div class="label">考核优秀率</div></div></el-card></el-col>
        <el-col :span="4"><el-card><div class="stat"><div class="num">{{ qualityOverview.quality?.checkAvgScore ?? '-' }}</div><div class="label">质量检查平均分</div></div></el-card></el-col>
        <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ qualityOverview.safety?.rectificationRate ?? '-' }}%</div><div class="label">整改完成率</div></div></el-card></el-col>
        <el-col :span="4"><el-card><div class="stat"><div class="num bad">{{ qualityOverview.safety?.overdueCount || 0 }}</div><div class="label">逾期整改</div></div></el-card></el-col>
        <el-col :span="4"><el-card><div class="stat"><div class="num bad">{{ qualityOverview.patientSafety?.urgentMdtCount || 0 }}</div><div class="label">紧急 MDT 会诊</div></div></el-card></el-col>
      </el-row>

      <el-card style="margin-bottom: 16px">
        <div class="toolbar">
          <div><span>医疗质量、医疗安全、患者安全三组指标，取自质量检查、医师考核、MDT、新技术新项目四个模块的实时数据。</span></div>
          <div><el-button type="warning" @click="handleExportReport">下载统计报表</el-button></div>
        </div>
      </el-card>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-card>
            <template #header><span>医疗质量指标</span></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="医师考核总数">{{ qualityOverview.quality?.assessmentTotal || 0 }} 条</el-descriptions-item>
              <el-descriptions-item label="医师考核平均分">{{ qualityOverview.quality?.assessmentAvgScore ?? '-' }} 分</el-descriptions-item>
              <el-descriptions-item label="考核优秀人数">{{ qualityOverview.quality?.assessmentExcellentCount || 0 }} 人</el-descriptions-item>
              <el-descriptions-item label="质量检查计划">{{ qualityOverview.quality?.checkPlanTotal || 0 }} 个</el-descriptions-item>
              <el-descriptions-item label="质量检查记录">{{ qualityOverview.quality?.checkRecordCount || 0 }} 条</el-descriptions-item>
              <el-descriptions-item label="质量检查平均分">{{ qualityOverview.quality?.checkAvgScore ?? '-' }} 分</el-descriptions-item>
            </el-descriptions>
            <p style="margin: 12px 0 6px; font-weight: bold">考核等级分布</p>
            <div v-for="(count, grade) in qualityOverview.quality?.assessmentByGrade || {}" :key="grade" style="margin-bottom: 8px">
              <span style="display: inline-block; width: 80px">{{ gradeText(grade) }}</span>
              <el-progress :percentage="gradePercent(count, qualityOverview.quality?.assessmentTotal)" style="display: inline-block; width: 70%" />
              <span style="margin-left: 8px">{{ count }} 人</span>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header><span>医疗安全指标</span></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="检查问题总数">{{ qualityOverview.safety?.issueCount || 0 }} 个</el-descriptions-item>
              <el-descriptions-item label="严重问题数">{{ qualityOverview.safety?.majorIssueCount || 0 }} 个</el-descriptions-item>
              <el-descriptions-item label="整改任务总数">{{ qualityOverview.safety?.rectificationTotal || 0 }} 条</el-descriptions-item>
              <el-descriptions-item label="整改完成率">{{ qualityOverview.safety?.rectificationRate ?? '-' }}%</el-descriptions-item>
              <el-descriptions-item label="逾期整改数">{{ qualityOverview.safety?.overdueCount || 0 }} 条</el-descriptions-item>
            </el-descriptions>
            <p style="margin: 12px 0 6px; font-weight: bold">问题严重程度分布</p>
            <div v-for="(count, severity) in qualityOverview.safety?.bySeverity || {}" :key="severity" style="margin-bottom: 8px">
              <span style="display: inline-block; width: 80px">{{ severityText(severity) }}</span>
              <el-progress :percentage="gradePercent(count, qualityOverview.safety?.issueCount)" :status="severity === 'MAJOR' ? 'exception' : ''" style="display: inline-block; width: 70%" />
              <span style="margin-left: 8px">{{ count }} 个</span>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="12">
          <el-card>
            <template #header><span>患者安全指标</span></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="MDT 会诊总数">{{ qualityOverview.patientSafety?.mdtTotal || 0 }} 条</el-descriptions-item>
              <el-descriptions-item label="MDT 已通过">{{ qualityOverview.patientSafety?.mdtApproved || 0 }} 条</el-descriptions-item>
              <el-descriptions-item label="MDT 待审批">{{ qualityOverview.patientSafety?.mdtPending || 0 }} 条</el-descriptions-item>
              <el-descriptions-item label="紧急 MDT 会诊">{{ qualityOverview.patientSafety?.urgentMdtCount || 0 }} 条</el-descriptions-item>
              <el-descriptions-item label="严重问题数">{{ qualityOverview.patientSafety?.majorIssueCount || 0 }} 个</el-descriptions-item>
              <el-descriptions-item label="逾期整改数">{{ qualityOverview.patientSafety?.overdueRectificationCount || 0 }} 条</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header><span>新技术新项目（质量改进）</span></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="项目总数">{{ qualityOverview.newTech?.total || 0 }} 个</el-descriptions-item>
              <el-descriptions-item label="已立项（含验收 / 推广）">{{ qualityOverview.newTech?.establishedCount || 0 }} 个</el-descriptions-item>
              <el-descriptions-item label="立项率">{{ qualityOverview.newTech?.establishedRate ?? '-' }}%</el-descriptions-item>
              <el-descriptions-item label="待伦理审查">{{ qualityOverview.newTech?.ethicsPending || 0 }} 个</el-descriptions-item>
              <el-descriptions-item label="已推广">{{ qualityOverview.newTech?.promoted || 0 }} 个</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>

      <el-card style="margin-top: 16px">
        <template #header><span>质量检查按科室统计</span></template>
        <el-table :data="qualityOverview.quality?.checkByDept || []" border stripe>
          <el-table-column prop="deptName" label="科室" />
          <el-table-column prop="count" label="计划数" />
          <el-table-column prop="completedCount" label="已完成" />
          <el-table-column prop="inProgressCount" label="进行中" />
          <el-table-column prop="pendingCount" label="待执行" />
          <el-table-column prop="recordCount" label="检查记录数" />
          <el-table-column prop="avgScore" label="平均分" />
        </el-table>
      </el-card>
    </div>

    <!-- 工作量登记弹窗 -->
    <el-dialog v-model="workDialogVisible" :title="workDialogTitle" width="520px">
      <el-form ref="workFormRef" :model="workForm" :rules="workRules" label-width="110px">
        <el-form-item label="医师" prop="physicianName">
          <el-select v-model="workForm.physicianId" placeholder="选择医师" filterable style="width: 100%" @change="handleWorkPhysicianChange">
            <el-option v-for="p in physicians" :key="p.id" :label="`${p.name}（${p.deptName}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="科室" prop="deptName">
          <el-input v-model="workForm.deptName" placeholder="选择医师后自动带出" />
        </el-form-item>
        <el-form-item label="统计月份" prop="statMonth">
          <el-date-picker v-model="workForm.statMonth" type="month" placeholder="选择月份" value-format="YYYY-MM" style="width: 100%" />
        </el-form-item>
        <el-form-item label="门诊量（人次）" prop="outpatientCount">
          <el-input-number v-model="workForm.outpatientCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="住院量（人次）" prop="inpatientCount">
          <el-input-number v-model="workForm.inpatientCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="手术量（台次）" prop="surgeryCount">
          <el-input-number v-model="workForm.surgeryCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="workForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="workDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleWorkSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 指标弹窗 -->
    <el-dialog v-model="indicatorDialogVisible" :title="indicatorDialogTitle" width="560px">
      <el-form ref="indicatorFormRef" :model="indicatorForm" :rules="indicatorRules" label-width="110px">
        <el-form-item label="指标编码" prop="indicatorCode">
          <el-input v-model="indicatorForm.indicatorCode" placeholder="如 QI-001" />
        </el-form-item>
        <el-form-item label="指标名称" prop="indicatorName">
          <el-input v-model="indicatorForm.indicatorName" placeholder="如 医师考核优秀率" />
        </el-form-item>
        <el-form-item label="指标分类" prop="category">
          <el-select v-model="indicatorForm.category" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="标准级别" prop="standardLevel">
          <el-select v-model="indicatorForm.standardLevel" placeholder="选择级别" style="width: 100%">
            <el-option v-for="l in levels" :key="l.value" :label="l.label" :value="l.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标准名称">
          <el-input v-model="indicatorForm.standardName" placeholder="如 三级中医医院评审标准" />
        </el-form-item>
        <el-form-item label="目标值">
          <el-input-number v-model="indicatorForm.targetValue" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="实际值">
          <el-input-number v-model="indicatorForm.actualValue" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="indicatorForm.unit" placeholder="如 % / 分 / 人次 / 台" />
        </el-form-item>
        <el-form-item label="比较方向">
          <el-select v-model="indicatorForm.direction" style="width: 100%">
            <el-option value="HIGHER_BETTER" label="越高越好" />
            <el-option value="LOWER_BETTER" label="越低越好" />
          </el-select>
        </el-form-item>
        <el-form-item label="统计周期">
          <el-input v-model="indicatorForm.statPeriod" placeholder="如 2026-11 / 2026年" />
        </el-form-item>
        <el-form-item label="数据来源">
          <el-input v-model="indicatorForm.dataSource" type="textarea" :rows="2" placeholder="注明实际值取自哪个模块的哪些数据" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="indicatorForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="indicatorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleIndicatorSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getWorkloads, getWorkloadStats, createWorkload, updateWorkload, deleteWorkload,
  batchDeleteWorkloads, exportWorkloads, downloadWorkloadTemplate,
  getQualityIndicators, getQualityIndicatorStats, createQualityIndicator,
  updateQualityIndicator, deleteQualityIndicator, batchDeleteQualityIndicators,
  exportQualityIndicators, downloadQualityIndicatorTemplate,
  getStatisticsWork, getStatisticsQuality, exportStatisticsReport
} from '../../api/statistics'
import { getPhysicians } from '../../api/physician'

const route = useRoute()
const router = useRouter()

const levels = [
  { value: 'NATIONAL', label: '国家' },
  { value: 'PROVINCE', label: '省' },
  { value: 'CITY', label: '市' }
]
const categories = ['医疗质量', '医疗安全', '患者安全', '工作效率']
const levelText = (v) => levels.find((l) => l.value === v)?.label || v || ''
const directionText = (v) => ({ HIGHER_BETTER: '越高越好', LOWER_BETTER: '越低越好' }[v] || v || '')
const complianceTag = (v) => ({ '达标': 'success', '未达标': 'danger', '未填报': 'info' }[v] || 'info')
const gradeText = (v) => ({ EXCELLENT: '优秀', QUALIFIED: '合格', BASIC: '基本合格', UNQUALIFIED: '不合格' }[v] || v || '')
const severityText = (v) => ({ MAJOR: '严重', GENERAL: '一般', MINOR: '一般', MILD: '轻微' }[v] || v || '')
const gradePercent = (count, total) => {
  const t = Number(total) || 0
  if (!t) return 0
  return Math.round((Number(count) || 0) * 100 / t)
}

const tabOfPath = (path) => {
  if (path === '/stats/tertiary') return 'tertiary'
  if (path === '/stats/quality') return 'quality'
  return 'work'
}
const activeTab = ref(tabOfPath(route.path))
const handleTabChange = (tab) => {
  const path = { work: '/stats/work', tertiary: '/stats/tertiary', quality: '/stats/quality' }[tab]
  router.push(path)
}
watch(() => route.path, (path) => {
  activeTab.value = tabOfPath(path)
  loadTabData(activeTab.value)
})

const physicians = ref([])

// ---------- 工作统计 ----------
const workStats = ref({})
const scheduleStats = ref({})
const workQuery = reactive({ keyword: '', statMonth: '', page: 1, size: 10 })
const workTableData = ref([])
const workTotal = ref(0)
const workLoading = ref(false)
const workSelected = ref([])

const workDialogVisible = ref(false)
const workDialogTitle = ref('新增工作量登记')
const workFormRef = ref(null)
const workForm = reactive({
  id: null, physicianId: null, physicianName: '', deptId: null, deptName: '',
  statMonth: '', outpatientCount: 0, inpatientCount: 0, surgeryCount: 0, remark: ''
})
const workRules = {
  physicianName: [{ required: true, message: '请选择医师', trigger: 'change' }],
  statMonth: [{ required: true, message: '请选择统计月份', trigger: 'change' }]
}

async function fetchWorkList() {
  workLoading.value = true
  try {
    const res = await getWorkloads({ ...workQuery })
    workTableData.value = res.records
    workTotal.value = res.total
  } finally {
    workLoading.value = false
  }
}

async function fetchWorkStats() {
  const res = await getWorkloadStats()
  workStats.value = res
  const work = await getStatisticsWork()
  scheduleStats.value = work.schedule || {}
}

function handleWorkSearch() {
  workQuery.page = 1
  fetchWorkList()
}

function handleWorkReset() {
  workQuery.keyword = ''
  workQuery.statMonth = ''
  workQuery.page = 1
  fetchWorkList()
}

function handleWorkPhysicianChange(physicianId) {
  const p = physicians.value.find((x) => x.id === physicianId)
  if (p) {
    workForm.physicianName = p.name
    workForm.deptId = p.deptId
    workForm.deptName = p.deptName
  }
}

function handleWorkAdd() {
  Object.assign(workForm, {
    id: null, physicianId: null, physicianName: '', deptId: null, deptName: '',
    statMonth: '', outpatientCount: 0, inpatientCount: 0, surgeryCount: 0, remark: ''
  })
  workDialogTitle.value = '新增工作量登记'
  workDialogVisible.value = true
}

function handleWorkEdit(row) {
  Object.assign(workForm, { ...row })
  workDialogTitle.value = '编辑工作量登记'
  workDialogVisible.value = true
}

async function handleWorkSubmit() {
  await workFormRef.value.validate()
  try {
    if (workForm.id) {
      await updateWorkload(workForm.id, { ...workForm })
    } else {
      await createWorkload({ ...workForm })
    }
    ElMessage.success('保存成功')
    workDialogVisible.value = false
    fetchWorkList()
    fetchWorkStats()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleWorkDelete(row) {
  await ElMessageBox.confirm(`确定删除 ${row.physicianName} ${row.statMonth} 的工作量登记吗？`, '提示', { type: 'warning' })
  await deleteWorkload(row.id)
  ElMessage.success('删除成功')
  fetchWorkList()
  fetchWorkStats()
}

async function handleWorkBatchDelete() {
  await ElMessageBox.confirm(`确定删除选中的 ${workSelected.value.length} 条工作量登记吗？`, '提示', { type: 'warning' })
  await batchDeleteWorkloads(workSelected.value.map((r) => r.id))
  ElMessage.success('删除成功')
  fetchWorkList()
  fetchWorkStats()
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

async function handleWorkExport() {
  const res = await exportWorkloads({ ...workQuery, page: 1, size: 10000 })
  downloadBlob(res.data, '工作量登记.xlsx')
}

async function handleWorkDownloadTemplate() {
  const res = await downloadWorkloadTemplate()
  downloadBlob(res.data, '工作量登记导入模板.xlsx')
}

function handleWorkImportSuccess(res) {
  if (res && res.code === 0) {
    ElMessage.success(`导入成功，共导入 ${res.data.imported} 条`)
  } else {
    ElMessage.success('导入成功')
  }
  fetchWorkList()
  fetchWorkStats()
}

// ---------- 三级医院医疗质量指标统计 ----------
const indicatorStats = ref({})
const indicatorQuery = reactive({ keyword: '', standardLevel: '', category: '', page: 1, size: 10 })
const indicatorTableData = ref([])
const indicatorTotal = ref(0)
const indicatorLoading = ref(false)
const indicatorSelected = ref([])

const indicatorDialogVisible = ref(false)
const indicatorDialogTitle = ref('新增指标')
const indicatorFormRef = ref(null)
const indicatorForm = reactive({
  id: null, indicatorCode: '', indicatorName: '', category: '医疗质量', standardLevel: 'NATIONAL',
  standardName: '', targetValue: null, actualValue: null, unit: '%', direction: 'HIGHER_BETTER',
  statPeriod: '', deptName: '', dataSource: '', remark: ''
})
const indicatorRules = {
  indicatorCode: [{ required: true, message: '请填写指标编码', trigger: 'blur' }],
  indicatorName: [{ required: true, message: '请填写指标名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择指标分类', trigger: 'change' }],
  standardLevel: [{ required: true, message: '请选择标准级别', trigger: 'change' }]
}

async function fetchIndicatorList() {
  indicatorLoading.value = true
  try {
    const res = await getQualityIndicators({ ...indicatorQuery })
    indicatorTableData.value = res.records
    indicatorTotal.value = res.total
  } finally {
    indicatorLoading.value = false
  }
}

async function fetchIndicatorStats() {
  indicatorStats.value = await getQualityIndicatorStats()
}

function handleIndicatorSearch() {
  indicatorQuery.page = 1
  fetchIndicatorList()
}

function handleIndicatorReset() {
  indicatorQuery.keyword = ''
  indicatorQuery.standardLevel = ''
  indicatorQuery.category = ''
  indicatorQuery.page = 1
  fetchIndicatorList()
}

function handleIndicatorAdd() {
  Object.assign(indicatorForm, {
    id: null, indicatorCode: '', indicatorName: '', category: '医疗质量', standardLevel: 'NATIONAL',
    standardName: '', targetValue: null, actualValue: null, unit: '%', direction: 'HIGHER_BETTER',
    statPeriod: '', deptName: '', dataSource: '', remark: ''
  })
  indicatorDialogTitle.value = '新增指标'
  indicatorDialogVisible.value = true
}

function handleIndicatorEdit(row) {
  Object.assign(indicatorForm, { ...row })
  indicatorDialogTitle.value = '编辑指标'
  indicatorDialogVisible.value = true
}

async function handleIndicatorSubmit() {
  await indicatorFormRef.value.validate()
  try {
    if (indicatorForm.id) {
      await updateQualityIndicator(indicatorForm.id, { ...indicatorForm })
    } else {
      await createQualityIndicator({ ...indicatorForm })
    }
    ElMessage.success('保存成功')
    indicatorDialogVisible.value = false
    fetchIndicatorList()
    fetchIndicatorStats()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleIndicatorDelete(row) {
  await ElMessageBox.confirm(`确定删除指标 ${row.indicatorName}（${row.indicatorCode}）吗？`, '提示', { type: 'warning' })
  await deleteQualityIndicator(row.id)
  ElMessage.success('删除成功')
  fetchIndicatorList()
  fetchIndicatorStats()
}

async function handleIndicatorBatchDelete() {
  await ElMessageBox.confirm(`确定删除选中的 ${indicatorSelected.value.length} 条指标吗？`, '提示', { type: 'warning' })
  await batchDeleteQualityIndicators(indicatorSelected.value.map((r) => r.id))
  ElMessage.success('删除成功')
  fetchIndicatorList()
  fetchIndicatorStats()
}

async function handleIndicatorExport() {
  const res = await exportQualityIndicators({ ...indicatorQuery, page: 1, size: 10000 })
  downloadBlob(res.data, '三级医院医疗质量指标.xlsx')
}

async function handleIndicatorDownloadTemplate() {
  const res = await downloadQualityIndicatorTemplate()
  downloadBlob(res.data, '三级医院医疗质量指标导入模板.xlsx')
}

function handleIndicatorImportSuccess(res) {
  if (res && res.code === 0) {
    ElMessage.success(`导入成功，共导入 ${res.data.imported} 条`)
  } else {
    ElMessage.success('导入成功')
  }
  fetchIndicatorList()
  fetchIndicatorStats()
}

function handleImportError() {
  ElMessage.error('导入失败，请检查文件格式')
}

// ---------- 医疗质量统计 ----------
const qualityOverview = ref({})

async function fetchQualityOverview() {
  qualityOverview.value = await getStatisticsQuality()
}

async function handleExportReport() {
  const res = await exportStatisticsReport()
  downloadBlob(res.data, '医疗统计报表.xlsx')
  ElMessage.success('统计报表已导出')
}

// ---------- 加载 ----------
function loadTabData(tab) {
  if (tab === 'work') {
    fetchWorkList()
    fetchWorkStats()
  } else if (tab === 'tertiary') {
    fetchIndicatorList()
    fetchIndicatorStats()
  } else if (tab === 'quality') {
    fetchQualityOverview()
  }
}

onMounted(async () => {
  const res = await getPhysicians({ page: 1, size: 100 })
  physicians.value = res.records || []
  loadTabData(activeTab.value)
})
</script>

<style scoped>
.stat {
  text-align: center;
  padding: 8px 0;
}
.stat .num {
  font-size: 28px;
  font-weight: bold;
}
.stat .num.ok {
  color: #67c23a;
}
.stat .num.warn {
  color: #e6a23c;
}
.stat .num.bad {
  color: #f56c6c;
}
.stat .label {
  color: #909399;
  margin-top: 4px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.page-note {
  color: #909399;
  font-size: 13px;
  margin: 0 0 12px;
}
</style>
