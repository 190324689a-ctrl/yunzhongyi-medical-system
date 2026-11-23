<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ stats.total || 0 }}</div><div class="label">档案总数</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ stats.active || 0 }}</div><div class="label">在岗</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ stats.qualPending || 0 }}</div><div class="label">资质待审核</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ stats.qualExpiringSoon || 0 }}</div><div class="label">资质90天内到期</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num bad">{{ stats.overLimitCount || 0 }}</div><div class="label">剂量超标（&gt;20mSv）</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num bad">{{ stats.overdueExamCount || 0 }}</div><div class="label">体检逾期人数</div></div></el-card></el-col>
    </el-row>

    <!-- 搜索区 -->
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="姓名 / 工作人员证号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="科室">
          <el-select v-model="query.deptId" placeholder="全部科室" clearable style="width: 140px">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="放射类别">
          <el-select v-model="query.workType" placeholder="全部类别" clearable style="width: 140px">
            <el-option v-for="t in workTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="在岗状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
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
          <el-button type="primary" @click="handleAdd">新增档案</el-button>
        </div>
        <div>
          <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
          <el-upload
            action="/api/radiation-personnel/import"
            :show-file-list="false"
            accept=".xlsx,.xls"
            :on-success="handleImportSuccess"
            :on-error="handleImportError"
            style="display: inline-block; margin: 0 8px"
          >
            <el-button type="success">批量导入</el-button>
          </el-upload>
          <el-button type="warning" @click="handleExport">导出 Excel</el-button>
          <el-button @click="handleDownloadTemplate">下载模板</el-button>
        </div>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading" @selection-change="selected = $event" style="margin-top: 12px">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="deptName" label="科室" width="110" />
        <el-table-column prop="title" label="职称" width="110" />
        <el-table-column prop="workType" label="放射类别" width="110">
          <template #default="{ row }">{{ workTypeText(row.workType) }}</template>
        </el-table-column>
        <el-table-column prop="licenseNo" label="工作人员证号" width="140" />
        <el-table-column prop="permitNo" label="诊疗许可证号" width="180" />
        <el-table-column prop="permitExpiry" label="许可证有效期至" width="130" />
        <el-table-column prop="status" label="在岗状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
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

    <!-- 按放射类别统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按放射类别统计</span></template>
      <el-table :data="workTypeStats" border stripe>
        <el-table-column prop="workTypeText" label="放射类别" />
        <el-table-column prop="count" label="人数" />
      </el-table>
    </el-card>

    <!-- 按科室统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按科室统计</span></template>
      <el-table :data="deptStats" border stripe>
        <el-table-column prop="deptName" label="科室" />
        <el-table-column prop="count" label="人数" />
        <el-table-column prop="activeCount" label="在岗" />
        <el-table-column prop="suspendedCount" label="停岗" />
        <el-table-column prop="leftCount" label="离岗" />
      </el-table>
    </el-card>

    <!-- 资质到期提醒 -->
    <el-card style="margin-top: 16px">
      <template #header><span>资质到期提醒（90 天内到期，含已过期）</span></template>
      <el-table :data="expiringQuals" border stripe>
        <el-table-column prop="personnelName" label="人员" width="100" />
        <el-table-column prop="certName" label="证书名称" />
        <el-table-column prop="certNo" label="证书编号" width="150" />
        <el-table-column prop="expiryDate" label="有效期至" width="120">
          <template #default="{ row }">
            <span :class="isExpired(row.expiryDate) ? 'expired' : ''">{{ row.expiryDate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="approveStatusTag(row.status)">{{ approveStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 剂量超标提醒 -->
    <el-card style="margin-top: 16px">
      <template #header><span>剂量超标提醒（年有效剂量 &gt; 20 mSv）</span></template>
      <el-table :data="overLimitHealth" border stripe>
        <el-table-column prop="personnelName" label="人员" width="100" />
        <el-table-column prop="examType" label="体检类型" width="130">
          <template #default="{ row }">{{ examTypeText(row.examType) }}</template>
        </el-table-column>
        <el-table-column prop="examDate" label="体检日期" width="120" />
        <el-table-column prop="dose" label="受照剂量（mSv）" width="140">
          <template #default="{ row }"><span class="expired">{{ row.dose }}</span></template>
        </el-table-column>
        <el-table-column prop="result" label="体检结论" width="160">
          <template #default="{ row }">{{ examResultText(row.result) }}</template>
        </el-table-column>
        <el-table-column prop="conclusion" label="结论说明" min-width="200" />
      </el-table>
    </el-card>

    <!-- 新增 / 编辑档案弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="820px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="130px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="选择医师">
              <el-select v-model="form.physicianId" placeholder="医师档案中选择（未建档可不选）" clearable style="width: 100%" @change="onPhysicianChange">
                <el-option v-for="p in physicians" :key="p.id" :label="`${p.name}（${p.deptName || ''}）`" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name"><el-input v-model="form.name" placeholder="姓名" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.gender" placeholder="请选择" clearable style="width: 100%">
                <el-option label="男" value="M" />
                <el-option label="女" value="F" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号"><el-input v-model="form.idCard" placeholder="身份证号" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话"><el-input v-model="form.phone" placeholder="联系电话" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="科室">
              <el-select v-model="form.deptId" placeholder="请选择" clearable style="width: 100%" @change="onDeptChange">
                <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称"><el-input v-model="form.title" placeholder="职称" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="放射类别" prop="workType">
              <el-select v-model="form.workType" placeholder="请选择" style="width: 100%">
                <el-option v-for="t in workTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工作人员证号"><el-input v-model="form.licenseNo" placeholder="放射工作人员证号" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="诊疗许可证号"><el-input v-model="form.permitNo" placeholder="放射诊疗许可证号" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="许可证有效期至">
              <el-date-picker v-model="form.permitExpiry" type="date" value-format="YYYY-MM-DD" placeholder="请选择" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="从事放射工作日期">
              <el-date-picker v-model="form.entryDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="在岗状态">
              <el-select v-model="form.status" placeholder="请选择" style="width: 100%">
                <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </el-row>
        <el-alert title="同一姓名同一科室已有档案时不能重复建档；许可证有效期不能早于从事放射工作日期。" type="info" :closable="false" show-icon />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗：档案 + 资质 / 培训 / 健康三个页签 -->
    <el-dialog title="放射人员详情" v-model="detailVisible" width="920px">
      <el-tabs v-model="detailTab">
        <el-tab-pane label="档案信息" name="info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ detail.gender === 'M' ? '男' : detail.gender === 'F' ? '女' : '—' }}</el-descriptions-item>
            <el-descriptions-item label="身份证号">{{ detail.idCard || '—' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detail.phone || '—' }}</el-descriptions-item>
            <el-descriptions-item label="科室">{{ detail.deptName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="职称">{{ detail.title || '—' }}</el-descriptions-item>
            <el-descriptions-item label="放射类别">{{ workTypeText(detail.workType) }}</el-descriptions-item>
            <el-descriptions-item label="工作人员证号">{{ detail.licenseNo || '—' }}</el-descriptions-item>
            <el-descriptions-item label="诊疗许可证号">{{ detail.permitNo || '—' }}</el-descriptions-item>
            <el-descriptions-item label="许可证有效期至">{{ detail.permitExpiry || '—' }}</el-descriptions-item>
            <el-descriptions-item label="从事放射工作日期">{{ detail.entryDate || '—' }}</el-descriptions-item>
            <el-descriptions-item label="在岗状态">
              <el-tag :type="statusTag(detail.status)">{{ statusText(detail.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="资质证书" name="qual">
          <el-table :data="qualList" border stripe>
            <el-table-column prop="certType" label="证书类型" width="150">
              <template #default="{ row }">{{ certTypeText(row.certType) }}</template>
            </el-table-column>
            <el-table-column prop="certName" label="证书名称" />
            <el-table-column prop="certNo" label="证书编号" width="140" />
            <el-table-column prop="issueDate" label="发证日期" width="110" />
            <el-table-column prop="expiryDate" label="有效期至" width="110">
              <template #default="{ row }">
                <span :class="isExpired(row.expiryDate) ? 'expired' : ''">{{ row.expiryDate }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="审核状态" width="90">
              <template #default="{ row }">
                <el-tag :type="approveStatusTag(row.status)">{{ approveStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button v-if="row.status === 0" link type="warning" @click="handleQualApprove(row)">审核</el-button>
                <el-button link type="danger" @click="handleQualDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-form :inline="true" :model="qualForm" class="sub-form">
            <el-form-item><el-select v-model="qualForm.certType" placeholder="证书类型" style="width: 170px">
              <el-option v-for="t in certTypes" :key="t.value" :label="t.label" :value="t.value" />
            </el-select></el-form-item>
            <el-form-item><el-input v-model="qualForm.certName" placeholder="证书名称" style="width: 160px" /></el-form-item>
            <el-form-item><el-input v-model="qualForm.certNo" placeholder="证书编号" style="width: 130px" /></el-form-item>
            <el-form-item><el-date-picker v-model="qualForm.issueDate" type="date" value-format="YYYY-MM-DD" placeholder="发证日期" style="width: 150px" /></el-form-item>
            <el-form-item><el-date-picker v-model="qualForm.expiryDate" type="date" value-format="YYYY-MM-DD" placeholder="有效期至" style="width: 150px" /></el-form-item>
            <el-form-item><el-button type="primary" @click="handleQualAdd">添加资质</el-button></el-form-item>
          </el-form>
          <el-alert title="新增资质默认待审核，需医务科一级审核通过；已结束审核的资质不能再修改、再审核。" type="info" :closable="false" show-icon />
        </el-tab-pane>

        <el-tab-pane label="培训记录" name="training">
          <el-table :data="trainingList" border stripe>
            <el-table-column prop="trainingName" label="培训名称" />
            <el-table-column prop="trainingType" label="培训类型" width="130">
              <template #default="{ row }">{{ trainingTypeText(row.trainingType) }}</template>
            </el-table-column>
            <el-table-column prop="org" label="培训机构" width="150" />
            <el-table-column prop="trainingDate" label="培训日期" width="110" />
            <el-table-column prop="hours" label="学时" width="70" />
            <el-table-column prop="result" label="结果" width="80">
              <template #default="{ row }">
                <el-tag :type="row.result === 'PASS' ? 'success' : 'danger'">{{ trainingResultText(row.result) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button link type="danger" @click="handleTrainingDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-form :inline="true" :model="trainingForm" class="sub-form">
            <el-form-item><el-input v-model="trainingForm.trainingName" placeholder="培训名称" style="width: 200px" /></el-form-item>
            <el-form-item><el-select v-model="trainingForm.trainingType" placeholder="培训类型" style="width: 150px">
              <el-option v-for="t in trainingTypes" :key="t.value" :label="t.label" :value="t.value" />
            </el-select></el-form-item>
            <el-form-item><el-input v-model="trainingForm.org" placeholder="培训机构" style="width: 140px" /></el-form-item>
            <el-form-item><el-date-picker v-model="trainingForm.trainingDate" type="date" value-format="YYYY-MM-DD" placeholder="培训日期" style="width: 150px" /></el-form-item>
            <el-form-item><el-input v-model="trainingForm.hours" placeholder="学时" type="number" style="width: 90px" /></el-form-item>
            <el-form-item><el-select v-model="trainingForm.result" placeholder="结果" style="width: 100px">
              <el-option label="合格" value="PASS" />
              <el-option label="不合格" value="FAIL" />
            </el-select></el-form-item>
            <el-form-item><el-button type="primary" @click="handleTrainingAdd">添加培训</el-button></el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="健康检查" name="health">
          <el-table :data="healthList" border stripe>
            <el-table-column prop="examType" label="体检类型" width="120">
              <template #default="{ row }">{{ examTypeText(row.examType) }}</template>
            </el-table-column>
            <el-table-column prop="examDate" label="体检日期" width="110" />
            <el-table-column prop="examOrg" label="体检机构" width="150" />
            <el-table-column prop="dose" label="受照剂量（mSv）" width="130">
              <template #default="{ row }">
                <span :class="row.dose > 20 ? 'expired' : ''">{{ row.dose }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="result" label="体检结论" width="140">
              <template #default="{ row }">
                <el-tag :type="row.result === 'FIT' ? 'success' : row.result === 'REVIEW' ? 'warning' : 'danger'">{{ examResultText(row.result) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="conclusion" label="结论说明" min-width="180" />
            <el-table-column prop="nextExamDate" label="下次体检" width="110" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button link type="danger" @click="handleHealthDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-form :inline="true" :model="healthForm" class="sub-form">
            <el-form-item><el-select v-model="healthForm.examType" placeholder="体检类型" style="width: 140px">
              <el-option v-for="t in examTypes" :key="t.value" :label="t.label" :value="t.value" />
            </el-select></el-form-item>
            <el-form-item><el-date-picker v-model="healthForm.examDate" type="date" value-format="YYYY-MM-DD" placeholder="体检日期" style="width: 150px" /></el-form-item>
            <el-form-item><el-input v-model="healthForm.examOrg" placeholder="体检机构" style="width: 140px" /></el-form-item>
            <el-form-item><el-input v-model="healthForm.dose" placeholder="受照剂量 mSv" type="number" style="width: 120px" /></el-form-item>
            <el-form-item><el-select v-model="healthForm.result" placeholder="体检结论" style="width: 150px">
              <el-option label="合格" value="FIT" />
              <el-option label="复查" value="REVIEW" />
              <el-option label="不宜从事放射工作" value="UNFIT" />
            </el-select></el-form-item>
            <el-form-item><el-input v-model="healthForm.conclusion" placeholder="结论说明" style="width: 180px" /></el-form-item>
            <el-form-item><el-button type="primary" @click="handleHealthAdd">添加体检</el-button></el-form-item>
          </el-form>
          <el-alert title="个人年受照剂量限值 20 mSv，超过限值时请安排复查调岗；体检有效期按一年计，逾期应安排在岗期间体检。" type="warning" :closable="false" show-icon />
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 资质审核弹窗 -->
    <el-dialog title="放射人员资质审核（医务科）" v-model="approveVisible" width="640px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="人员">{{ approveRow.personnelName }}</el-descriptions-item>
        <el-descriptions-item label="证书">{{ certTypeText(approveRow.certType) }} / {{ approveRow.certName }}</el-descriptions-item>
        <el-descriptions-item label="证书编号">{{ approveRow.certNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="有效期">{{ approveRow.issueDate || '—' }} 至 {{ approveRow.expiryDate || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="approveForm" label-width="90px" style="margin-top: 16px">
        <el-form-item label="审核人">
          <el-input v-model="approveForm.approver" placeholder="医务科姓名" />
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="approveForm.opinion" type="textarea" :rows="2" placeholder="审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="danger" @click="handleQualReject">驳回</el-button>
        <el-button type="primary" @click="handleQualApproveSubmit">通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRadiationPersonnel, getRadiationPersonnelStats, getRadiationPersonnelStatsByDept, getRadiationPersonnelStatsByWorkType,
  getRadiationPersonnelById, createRadiationPersonnel, updateRadiationPersonnel, deleteRadiationPersonnel,
  batchDeleteRadiationPersonnel, exportRadiationPersonnel, downloadRadiationPersonnelTemplate,
  getRadiationQualifications, getQualificationsByPersonnel, createRadiationQualification, deleteRadiationQualification,
  approveRadiationQualification, rejectRadiationQualification,
  getTrainingsByPersonnel, createRadiationTraining, deleteRadiationTraining,
  getRadiationHealth, getHealthByPersonnel, createRadiationHealth, deleteRadiationHealth
} from '../../api/radiation'
import { getEnabledDepts } from '../../api/system'
import { getPhysicians } from '../../api/physician'

const workTypes = [
  { value: 'DIAGNOSIS', label: '放射诊断' },
  { value: 'INTERVENTION', label: '介入放射' },
  { value: 'NUCLEAR_MED', label: '核医学' },
  { value: 'RADIOTHERAPY', label: '放射治疗' }
]
const statuses = [
  { value: 1, label: '在岗' },
  { value: 2, label: '停岗' },
  { value: 3, label: '离岗' }
]
const certTypes = [
  { value: 'WORKER_CERT', label: '放射工作人员证' },
  { value: 'EQUIPMENT', label: '大型设备上岗证' },
  { value: 'PROTECTION', label: '放射防护培训合格证' },
  { value: 'PRACTICE', label: '执业医师证' }
]
const trainingTypes = [
  { value: 'PRE_JOB', label: '岗前培训' },
  { value: 'RETRAINING', label: '复训' },
  { value: 'PROTECTION', label: '防护知识培训' },
  { value: 'EQUIPMENT', label: '新设备培训' }
]
const examTypes = [
  { value: 'PRE_JOB', label: '岗前体检' },
  { value: 'ON_JOB', label: '在岗期间体检' },
  { value: 'OFF_JOB', label: '离岗体检' }
]
const workTypeText = (v) => workTypes.find((t) => t.value === v)?.label || v || ''
const statusText = (v) => statuses.find((s) => s.value === v)?.label || ''
const statusTag = (v) => ({ 1: 'success', 2: 'warning', 3: 'info' }[v] || 'info')
const certTypeText = (v) => certTypes.find((t) => t.value === v)?.label || v || ''
const approveStatusText = (v) => ({ 0: '待审核', 1: '已通过', 2: '已驳回' }[v] || '')
const approveStatusTag = (v) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[v] || 'info')
const trainingTypeText = (v) => trainingTypes.find((t) => t.value === v)?.label || v || ''
const trainingResultText = (v) => ({ PASS: '合格', FAIL: '不合格' }[v] || v || '')
const examTypeText = (v) => examTypes.find((t) => t.value === v)?.label || v || ''
const examResultText = (v) => ({ FIT: '合格', REVIEW: '复查', UNFIT: '不宜从事放射工作' }[v] || v || '')
const isExpired = (d) => d && d < new Date().toISOString().slice(0, 10)

const query = reactive({ keyword: '', deptId: null, workType: '', status: null, page: 1, size: 10 })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])
const depts = ref([])
const physicians = ref([])
const stats = ref({})
const deptStats = ref([])
const workTypeStats = ref([])
const expiringQuals = ref([])
const overLimitHealth = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增档案')
const formRef = ref(null)
const form = reactive({
  id: null, physicianId: null, name: '', gender: '', idCard: '', phone: '',
  deptId: null, deptName: '', title: '', workType: 'DIAGNOSIS',
  licenseNo: '', permitNo: '', permitExpiry: '', entryDate: '', status: 1, remark: ''
})
const rules = {
  name: [{ required: true, message: '请填写姓名', trigger: 'blur' }],
  workType: [{ required: true, message: '请选择放射类别', trigger: 'change' }]
}

const detailVisible = ref(false)
const detailTab = ref('info')
const detail = ref({})
const qualList = ref([])
const trainingList = ref([])
const healthList = ref([])
const qualForm = reactive({ certType: 'WORKER_CERT', certName: '', certNo: '', issueDate: '', expiryDate: '' })
const trainingForm = reactive({ trainingName: '', trainingType: 'RETRAINING', org: '', trainingDate: '', hours: null, result: 'PASS' })
const healthForm = reactive({ examType: 'ON_JOB', examDate: '', examOrg: '', dose: null, result: 'FIT', conclusion: '' })

const approveVisible = ref(false)
const approveRow = ref({})
const approveForm = reactive({ approver: '吴文斌', opinion: '' })

async function fetchList() {
  loading.value = true
  try {
    const data = await getRadiationPersonnel({ ...query })
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
    stats.value = await getRadiationPersonnelStats()
    deptStats.value = await getRadiationPersonnelStatsByDept()
    workTypeStats.value = await getRadiationPersonnelStatsByWorkType()
    expiringQuals.value = await getRadiationQualifications({ expiringSoon: true, page: 1, size: 100 })
      .then((d) => d.records || d)
    overLimitHealth.value = await getRadiationHealth({ overLimit: true, page: 1, size: 100 })
      .then((d) => d.records || d)
  } catch (e) {
    console.warn('统计加载失败', e.message)
  }
}

function refresh() {
  fetchList()
  fetchStats()
}

function handleSearch() {
  query.page = 1
  fetchList()
}

function handleReset() {
  Object.assign(query, { keyword: '', deptId: null, workType: '', status: null, page: 1, size: 10 })
  fetchList()
}

function resetForm() {
  Object.assign(form, {
    id: null, physicianId: null, name: '', gender: '', idCard: '', phone: '',
    deptId: null, deptName: '', title: '', workType: 'DIAGNOSIS',
    licenseNo: '', permitNo: '', permitExpiry: '', entryDate: '', status: 1, remark: ''
  })
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增档案'
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  Object.assign(form, row)
  dialogTitle.value = '编辑档案'
  dialogVisible.value = true
}

function onPhysicianChange(id) {
  const p = physicians.value.find((x) => x.id === id)
  if (p) {
    form.name = p.name
    form.deptId = p.deptId
    form.deptName = p.deptName
    form.title = p.title
  }
}

function onDeptChange(id) {
  const d = depts.value.find((x) => x.id === id)
  form.deptName = d ? d.name : ''
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    if (form.id) {
      await updateRadiationPersonnel(form.id, { ...form })
      ElMessage.success('档案修改成功')
    } else {
      await createRadiationPersonnel({ ...form })
      ElMessage.success('档案新增成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除放射人员「${row.name}」的档案吗？`, '提示', { type: 'warning' })
  await deleteRadiationPersonnel(row.id)
  ElMessage.success('删除成功')
  refresh()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selected.value.length} 条档案吗？`, '提示', { type: 'warning' })
  await batchDeleteRadiationPersonnel(selected.value.map((r) => r.id))
  ElMessage.success('批量删除成功')
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
    const res = await exportRadiationPersonnel({ ...query, page: 1, size: 10000 })
    downloadBlob(res.data, '放射人员档案.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleDownloadTemplate() {
  const res = await downloadRadiationPersonnelTemplate()
  downloadBlob(res.data, '放射人员档案导入模板.xlsx')
}

function handleImportSuccess(res) {
  if (res.code === 0) {
    ElMessage.success(`导入成功，共导入 ${res.data.imported} 条`)
    refresh()
  } else {
    ElMessage.error('导入失败：' + (res.msg || '未知错误'))
  }
}

function handleImportError() {
  ElMessage.error('导入失败，请检查文件格式')
}

// ---- 详情：档案 + 资质 / 培训 / 健康 ----
async function handleDetail(row) {
  try {
    detail.value = await getRadiationPersonnelById(row.id)
    detailTab.value = 'info'
    await fetchSubRecords(row.id)
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败：' + e.message)
  }
}

async function fetchSubRecords(personnelId) {
  try {
    qualList.value = await getQualificationsByPersonnel(personnelId)
    trainingList.value = await getTrainingsByPersonnel(personnelId)
    healthList.value = await getHealthByPersonnel(personnelId)
  } catch (e) {
    ElMessage.error('加载资质/培训/健康记录失败：' + e.message)
  }
}

function resetSubForms() {
  Object.assign(qualForm, { certType: 'WORKER_CERT', certName: '', certNo: '', issueDate: '', expiryDate: '' })
  Object.assign(trainingForm, { trainingName: '', trainingType: 'RETRAINING', org: '', trainingDate: '', hours: null, result: 'PASS' })
  Object.assign(healthForm, { examType: 'ON_JOB', examDate: '', examOrg: '', dose: null, result: 'FIT', conclusion: '' })
}

async function handleQualAdd() {
  if (!qualForm.certName) {
    ElMessage.warning('请填写证书名称')
    return
  }
  try {
    await createRadiationQualification({ ...qualForm, personnelId: detail.value.id, personnelName: detail.value.name })
    ElMessage.success('资质登记成功，已进入待审核')
    await fetchSubRecords(detail.value.id)
    fetchStats()
  } catch (e) {
    ElMessage.error('添加失败：' + e.message)
  }
}

async function handleQualDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.certName}」吗？`, '提示', { type: 'warning' })
  await deleteRadiationQualification(row.id)
  ElMessage.success('删除成功')
  await fetchSubRecords(detail.value.id)
  fetchStats()
}

function handleQualApprove(row) {
  approveRow.value = row
  Object.assign(approveForm, { approver: '吴文斌', opinion: '' })
  approveVisible.value = true
}

async function handleQualApproveSubmit() {
  try {
    await approveRadiationQualification(approveRow.value.id, { approver: approveForm.approver, opinion: approveForm.opinion })
    ElMessage.success('审核通过')
    approveVisible.value = false
    await fetchSubRecords(detail.value.id)
    fetchStats()
  } catch (e) {
    ElMessage.error('审核失败：' + e.message)
  }
}

async function handleQualReject() {
  try {
    await rejectRadiationQualification(approveRow.value.id, { approver: approveForm.approver, opinion: approveForm.opinion })
    ElMessage.success('已驳回')
    approveVisible.value = false
    await fetchSubRecords(detail.value.id)
    fetchStats()
  } catch (e) {
    ElMessage.error('驳回失败：' + e.message)
  }
}

async function handleTrainingAdd() {
  if (!trainingForm.trainingName || !trainingForm.trainingDate) {
    ElMessage.warning('请填写培训名称和培训日期')
    return
  }
  try {
    await createRadiationTraining({ ...trainingForm, personnelId: detail.value.id, personnelName: detail.value.name })
    ElMessage.success('培训记录添加成功')
    await fetchSubRecords(detail.value.id)
    fetchStats()
  } catch (e) {
    ElMessage.error('添加失败：' + e.message)
  }
}

async function handleTrainingDelete(row) {
  await ElMessageBox.confirm(`确定删除培训记录「${row.trainingName}」吗？`, '提示', { type: 'warning' })
  await deleteRadiationTraining(row.id)
  ElMessage.success('删除成功')
  await fetchSubRecords(detail.value.id)
  fetchStats()
}

async function handleHealthAdd() {
  if (!healthForm.examDate) {
    ElMessage.warning('请填写体检日期')
    return
  }
  try {
    await createRadiationHealth({ ...healthForm, personnelId: detail.value.id, personnelName: detail.value.name })
    ElMessage.success('体检记录添加成功')
    await fetchSubRecords(detail.value.id)
    fetchStats()
  } catch (e) {
    ElMessage.error('添加失败：' + e.message)
  }
}

async function handleHealthDelete(row) {
  await ElMessageBox.confirm(`确定删除这条体检记录吗？`, '提示', { type: 'warning' })
  await deleteRadiationHealth(row.id)
  ElMessage.success('删除成功')
  await fetchSubRecords(detail.value.id)
  fetchStats()
}

onMounted(async () => {
  try {
    depts.value = await getEnabledDepts()
    const page = await getPhysicians({ page: 1, size: 100 })
    physicians.value = page.records || []
  } catch (e) {
    console.warn('基础数据加载失败（后端未启动？）', e.message)
  }
  resetSubForms()
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
.sub-form {
  margin-top: 16px;
}
.expired {
  color: #f56c6c;
  font-weight: bold;
}
</style>
