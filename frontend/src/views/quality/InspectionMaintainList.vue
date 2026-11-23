<template>
  <div>
    <el-tabs v-model="activeTab" type="border-card">
      <!-- ============ 检查项目 ============ -->
      <el-tab-pane label="检查项目" name="item">
        <el-row :gutter="16" style="margin-bottom: 16px">
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ itemStats.total || 0 }}</div><div class="label">项目总数</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ itemStats.enabled || 0 }}</div><div class="label">已启用</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ itemStats.disabled || 0 }}</div><div class="label">已停用</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ itemStats.linked || 0 }}</div><div class="label">已组装进模板</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ itemStats.unlinked || 0 }}</div><div class="label">未组装进模板</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ templateStats.totalLinks || 0 }}</div><div class="label">模板检查项总数</div></div></el-card></el-col>
        </el-row>
        <el-card>
          <el-form :inline="true" :model="itemQuery" class="search-form">
            <el-form-item label="关键字">
              <el-input v-model="itemQuery.keyword" placeholder="项目名称 / 项目编号" clearable style="width: 200px" />
            </el-form-item>
            <el-form-item label="项目分类">
              <el-select v-model="itemQuery.category" placeholder="全部分类" clearable style="width: 130px">
                <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="检查方式">
              <el-select v-model="itemQuery.checkMethod" placeholder="全部方式" clearable style="width: 130px">
                <el-option v-for="m in checkMethods" :key="m.value" :label="m.label" :value="m.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="适用检查类型">
              <el-select v-model="itemQuery.applicableType" placeholder="全部类型" clearable style="width: 130px">
                <el-option v-for="t in applicableTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="依据标准">
              <el-select v-model="itemQuery.standardId" placeholder="全部标准" clearable style="width: 180px">
                <el-option v-for="s in standardOptions" :key="s.id" :label="`${s.standardName}（${s.standardNo}）`" :value="s.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="项目状态">
              <el-select v-model="itemQuery.status" placeholder="全部状态" clearable style="width: 110px">
                <el-option v-for="s in itemStatuses" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="itemQuery.linked">只看已组装进模板</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="itemQuery.unlinked">只看未组装进模板</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleItemSearch">查询</el-button>
              <el-button @click="handleItemReset">重置</el-button>
            </el-form-item>
          </el-form>
          <div class="toolbar">
            <div>
              <el-button type="primary" @click="handleItemAdd">新增检查项目</el-button>
            </div>
            <div>
              <el-button type="danger" :disabled="!itemSelected.length" @click="handleItemBatchDelete">批量删除</el-button>
              <el-upload
                action="/api/inspection-items/import"
                :show-file-list="false"
                accept=".xlsx,.xls"
                :on-success="handleItemImportSuccess"
                :on-error="handleImportError"
                style="display: inline-block; margin: 0 8px"
              >
                <el-button type="success">批量导入</el-button>
              </el-upload>
              <el-button type="warning" @click="handleItemExport">导出 Excel</el-button>
              <el-button @click="handleItemDownloadTemplate">下载模板</el-button>
            </div>
          </div>

          <el-table :data="itemData" border stripe v-loading="itemLoading" @selection-change="itemSelected = $event" style="margin-top: 12px">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="itemNo" label="项目编号" width="110" />
            <el-table-column prop="itemName" label="项目名称" min-width="140" />
            <el-table-column prop="category" label="项目分类" width="110">
              <template #default="{ row }">{{ categoryText(row.category) }}</template>
            </el-table-column>
            <el-table-column prop="checkMethod" label="检查方式" width="110">
              <template #default="{ row }">{{ checkMethodText(row.checkMethod) }}</template>
            </el-table-column>
            <el-table-column prop="standardNo" label="依据标准" width="130" />
            <el-table-column prop="standardText" label="评分标准" min-width="180" show-overflow-tooltip />
            <el-table-column prop="maxScore" label="满分" width="70" />
            <el-table-column prop="weight" label="默认权重" width="90" />
            <el-table-column prop="applicableType" label="适用检查类型" width="120">
              <template #default="{ row }">{{ applicableTypeText(row.applicableType) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ itemStatusText(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序" width="70" />
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleItemEdit(row)">编辑</el-button>
                <el-button link type="warning" :disabled="row.status !== 1" @click="handleItemDisable(row)">停用</el-button>
                <el-button link type="success" :disabled="row.status !== 0" @click="handleItemEnable(row)">启用</el-button>
                <el-button link type="danger" @click="handleItemDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            style="margin-top: 16px; justify-content: flex-end"
            v-model:current-page="itemQuery.page"
            v-model:page-size="itemQuery.size"
            :total="itemTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchItemList"
            @current-change="fetchItemList"
          />
        </el-card>
        <el-card style="margin-top: 16px">
          <template #header><span>按项目分类统计</span></template>
          <el-table :data="itemCategoryStats" border stripe>
            <el-table-column prop="categoryText" label="项目分类" />
            <el-table-column prop="count" label="项目数" width="120" />
            <el-table-column prop="enabledCount" label="已启用数" width="120" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ============ 检查标准 ============ -->
      <el-tab-pane label="检查标准" name="standard">
        <el-row :gutter="16" style="margin-bottom: 16px">
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ standardStats.total || 0 }}</div><div class="label">标准总数</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ standardStats.current || 0 }}</div><div class="label">现行</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ standardStats.abolished || 0 }}</div><div class="label">已废止</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ standardStats.expiringSoon || 0 }}</div><div class="label">30天内到期</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num bad">{{ standardStats.expired || 0 }}</div><div class="label">已过期</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ standardStats.referenced || 0 }}</div><div class="label">被检查项目引用</div></div></el-card></el-col>
        </el-row>
        <el-card>
          <el-form :inline="true" :model="standardQuery" class="search-form">
            <el-form-item label="关键字">
              <el-input v-model="standardQuery.keyword" placeholder="标准名称 / 标准编号 / 依据文件" clearable style="width: 230px" />
            </el-form-item>
            <el-form-item label="标准级别">
              <el-select v-model="standardQuery.level" placeholder="全部级别" clearable style="width: 110px">
                <el-option v-for="l in levels" :key="l.value" :label="l.label" :value="l.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="标准状态">
              <el-select v-model="standardQuery.status" placeholder="全部状态" clearable style="width: 110px">
                <el-option v-for="s in standardStatuses" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="standardQuery.expiringSoon">只看30天内到期</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="standardQuery.expired">只看已过期</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleStandardSearch">查询</el-button>
              <el-button @click="handleStandardReset">重置</el-button>
            </el-form-item>
          </el-form>
          <div class="toolbar">
            <div>
              <el-button type="primary" @click="handleStandardAdd">新增检查标准</el-button>
            </div>
            <div>
              <el-button type="danger" :disabled="!standardSelected.length" @click="handleStandardBatchDelete">批量删除</el-button>
              <el-upload
                action="/api/inspection-standards/import"
                :show-file-list="false"
                accept=".xlsx,.xls"
                :on-success="handleStandardImportSuccess"
                :on-error="handleImportError"
                style="display: inline-block; margin: 0 8px"
              >
                <el-button type="success">批量导入</el-button>
              </el-upload>
              <el-button type="warning" @click="handleStandardExport">导出 Excel</el-button>
              <el-button @click="handleStandardDownloadTemplate">下载模板</el-button>
            </div>
          </div>

          <el-table :data="standardData" border stripe v-loading="standardLoading" @selection-change="standardSelected = $event" style="margin-top: 12px">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="standardNo" label="标准编号" width="110" />
            <el-table-column prop="standardName" label="标准名称" min-width="170" />
            <el-table-column prop="level" label="标准级别" width="90">
              <template #default="{ row }">{{ levelText(row.level) }}</template>
            </el-table-column>
            <el-table-column prop="sourceDoc" label="依据文件" min-width="180" show-overflow-tooltip />
            <el-table-column prop="content" label="标准内容" min-width="180" show-overflow-tooltip />
            <el-table-column prop="applicableScope" label="适用范围" width="100" />
            <el-table-column prop="effectiveDate" label="生效日期" width="110" />
            <el-table-column prop="expiryDate" label="有效期" width="110">
              <template #default="{ row }"><span :class="{ expired: isStandardExpired(row) }">{{ row.expiryDate }}</span></template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ standardStatusText(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="abolishReason" label="废止原因" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleStandardEdit(row)">编辑</el-button>
                <el-button link type="warning" :disabled="row.status !== 1" @click="handleStandardAbolish(row)">废止</el-button>
                <el-button link type="success" :disabled="row.status !== 0" @click="handleStandardRestore(row)">恢复现行</el-button>
                <el-button link type="danger" @click="handleStandardDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            style="margin-top: 16px; justify-content: flex-end"
            v-model:current-page="standardQuery.page"
            v-model:page-size="standardQuery.size"
            :total="standardTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchStandardList"
            @current-change="fetchStandardList"
          />
        </el-card>
        <el-card style="margin-top: 16px">
          <template #header><span>按标准级别统计</span></template>
          <el-table :data="standardLevelStats" border stripe>
            <el-table-column prop="levelText" label="标准级别" />
            <el-table-column prop="count" label="标准数" width="120" />
            <el-table-column prop="currentCount" label="现行数" width="120" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ============ 模板检查项配置 ============ -->
      <el-tab-pane label="模板检查项配置" name="template">
        <el-row :gutter="16" style="margin-bottom: 16px">
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ templateStats.templatesConfigured || 0 }}</div><div class="label">已配置模板数</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ templateStats.totalLinks || 0 }}</div><div class="label">配置记录总数</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ templateStats.avgItemsPerTemplate || 0 }}</div><div class="label">平均每模板检查项数</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ templateStats.uncoveredTemplates || 0 }}</div><div class="label">未配置检查项的模板</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ templateStats.unlinkedItems || 0 }}</div><div class="label">未组装进任何模板的项目</div></div></el-card></el-col>
          <el-col :span="4"><el-card><div class="stat"><div class="num">{{ templateStats.templateTotal || 0 }}</div><div class="label">检查模板总数</div></div></el-card></el-col>
        </el-row>
        <el-card>
          <el-form :inline="true" class="search-form">
            <el-form-item label="检查模板">
              <el-select v-model="selectedTemplateId" placeholder="请选择检查模板" clearable style="width: 240px" @change="fetchTemplateLinks">
                <el-option v-for="t in templateOptions" :key="t.id" :label="`${t.templateName}（${inspectTypeText(t.inspectType)}）`" :value="t.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :disabled="!selectedTemplateId" @click="handleTemplateConfigure">配置检查项</el-button>
            </el-form-item>
          </el-form>
          <div class="toolbar">
            <div>
              <span style="color: #909399; font-size: 13px">从检查项目库里挑选检查项组装进模板，保存后同步回写模板的检查项（名称、权重、评分标准），权重之和须为 100。</span>
            </div>
            <div>
              <el-upload
                :action="templateImportAction"
                :disabled="!selectedTemplateId"
                :show-file-list="false"
                accept=".xlsx,.xls"
                :on-success="handleTemplateImportSuccess"
                :on-error="handleImportError"
                style="display: inline-block; margin: 0 8px"
              >
                <el-button type="success" :disabled="!selectedTemplateId">批量导入</el-button>
              </el-upload>
              <el-button type="warning" :disabled="!selectedTemplateId" @click="handleTemplateExport">导出 Excel</el-button>
              <el-button :disabled="!selectedTemplateId" @click="handleTemplateDownloadTemplate">下载模板</el-button>
            </div>
          </div>

          <el-table :data="templateLinkData" border stripe v-loading="templateLinkLoading" style="margin-top: 12px">
            <el-table-column prop="templateName" label="检查模板" width="180" />
            <el-table-column prop="itemNo" label="项目编号" width="110" />
            <el-table-column prop="itemName" label="项目名称" min-width="140" />
            <el-table-column prop="weight" label="权重" width="90" />
            <el-table-column prop="sortOrder" label="排序" width="70" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="danger" @click="handleTemplateLinkDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 12px; color: #909399; font-size: 13px">
            当前模板检查项权重之和：{{ templateWeightSum }}（须为 100）
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 检查项目新增/编辑弹窗 -->
    <el-dialog v-model="itemDialogVisible" :title="itemDialogTitle" width="640px">
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="110px">
        <el-form-item label="项目编号" prop="itemNo">
          <el-input v-model="itemForm.itemNo" placeholder="如：JCXM-001" />
        </el-form-item>
        <el-form-item label="项目名称" prop="itemName">
          <el-input v-model="itemForm.itemName" placeholder="如：病历书写规范" />
        </el-form-item>
        <el-form-item label="项目分类" prop="category">
          <el-select v-model="itemForm.category" placeholder="请选择" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="检查方式">
          <el-select v-model="itemForm.checkMethod" placeholder="请选择" clearable style="width: 100%">
            <el-option v-for="m in checkMethods" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="依据标准">
          <el-select v-model="itemForm.standardId" placeholder="请选择检查标准" clearable style="width: 100%">
            <el-option v-for="s in standardOptions" :key="s.id" :label="`${s.standardName}（${s.standardNo}）`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="评分标准">
          <el-input v-model="itemForm.standardText" type="textarea" :rows="2" placeholder="如：运行病历书写及时、完整、规范" />
        </el-form-item>
        <el-form-item label="满分">
          <el-input-number v-model="itemForm.maxScore" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="默认权重">
          <el-input-number v-model="itemForm.weight" :min="0.01" :step="5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="适用检查类型">
          <el-select v-model="itemForm.applicableType" placeholder="请选择" clearable style="width: 100%">
            <el-option v-for="t in applicableTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="itemForm.sortOrder" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="itemForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleItemSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 检查标准新增/编辑弹窗 -->
    <el-dialog v-model="standardDialogVisible" :title="standardDialogTitle" width="640px">
      <el-form ref="standardFormRef" :model="standardForm" :rules="standardRules" label-width="110px">
        <el-form-item label="标准编号" prop="standardNo">
          <el-input v-model="standardForm.standardNo" placeholder="如：JCBZ-001" />
        </el-form-item>
        <el-form-item label="标准名称" prop="standardName">
          <el-input v-model="standardForm.standardName" placeholder="如：三级医院评审标准（2020年版）" />
        </el-form-item>
        <el-form-item label="标准级别" prop="level">
          <el-select v-model="standardForm.level" placeholder="请选择" style="width: 100%">
            <el-option v-for="l in levels" :key="l.value" :label="l.label" :value="l.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="依据文件">
          <el-input v-model="standardForm.sourceDoc" placeholder="如：《三级医院评审标准（2020年版）》" />
        </el-form-item>
        <el-form-item label="标准内容">
          <el-input v-model="standardForm.content" type="textarea" :rows="3" placeholder="标准要点摘要" />
        </el-form-item>
        <el-form-item label="适用范围">
          <el-input v-model="standardForm.applicableScope" placeholder="如：全院" />
        </el-form-item>
        <el-form-item label="生效日期">
          <el-date-picker v-model="standardForm.effectiveDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker v-model="standardForm.expiryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="standardForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="standardDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStandardSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 检查标准废止弹窗 -->
    <el-dialog v-model="abolishVisible" title="废止检查标准" width="480px">
      <el-form label-width="90px">
        <el-form-item label="标准名称"><span>{{ abolishRow.standardName }}</span></el-form-item>
        <el-form-item label="废止原因">
          <el-input v-model="abolishForm.reason" type="textarea" :rows="3" placeholder="必填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="abolishVisible = false">取消</el-button>
        <el-button type="warning" @click="handleAbolishSubmit">废止</el-button>
      </template>
    </el-dialog>

    <!-- 模板检查项配置弹窗 -->
    <el-dialog v-model="configDialogVisible" :title="`配置检查项（${configTemplateName}）`" width="800px">
      <div style="margin-bottom: 12px; color: #909399; font-size: 13px">
        勾选要组装进模板的检查项目（仅显示已启用的项目），逐项填写权重，权重之和须为 100，保存后同步回写模板的检查项。
      </div>
      <el-table ref="configTableRef" :data="configItems" border stripe max-height="420" @selection-change="configSelected = $event">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="itemNo" label="项目编号" width="110" />
        <el-table-column prop="itemName" label="项目名称" min-width="140" />
        <el-table-column prop="category" label="项目分类" width="110">
          <template #default="{ row }">{{ categoryText(row.category) }}</template>
        </el-table-column>
        <el-table-column prop="standardText" label="评分标准" min-width="160" show-overflow-tooltip />
        <el-table-column label="权重" width="130">
          <template #default="{ row }">
            <el-input-number v-model="configWeights[row.id]" :min="0" :step="5" size="small" style="width: 110px" />
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 12px; font-size: 14px">
        已选 {{ configSelected.length }} 项，权重之和：<b :style="{ color: configWeightSum === 100 ? '#67c23a' : '#f56c6c' }">{{ configWeightSum }}</b>（须为 100）
      </div>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfigSubmit">保存配置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getInspectionItems, getInspectionItemStats, getInspectionItemStatsByCategory,
  createInspectionItem, updateInspectionItem, deleteInspectionItem,
  batchDeleteInspectionItems, enableInspectionItem, disableInspectionItem,
  exportInspectionItems, downloadInspectionItemTemplate,
  getInspectionStandards, getInspectionStandardStats, getInspectionStandardStatsByLevel,
  createInspectionStandard, updateInspectionStandard, deleteInspectionStandard,
  batchDeleteInspectionStandards, abolishInspectionStandard, restoreInspectionStandard,
  exportInspectionStandards, downloadInspectionStandardTemplate,
  getInspectionTemplateItemStats, getInspectionTemplateItemsByTemplate,
  configureInspectionTemplateItems, deleteInspectionTemplateItem,
  exportInspectionTemplateItems, downloadInspectionTemplateItemTemplate
} from '../../api/inspection'
import { getQualityTemplates } from '../../api/quality'

const activeTab = ref('item')

// ---------- 字典 ----------
const categories = [
  { value: 'MEDICAL_RECORD', label: '病历质量' },
  { value: 'CORE_SYSTEM', label: '核心制度' },
  { value: 'INFECTION', label: '医院感染' },
  { value: 'PHARMACY', label: '药事管理' },
  { value: 'NURSING', label: '护理质量' },
  { value: 'SAFETY', label: '医疗安全' },
  { value: 'SERVICE', label: '服务质量' },
  { value: 'TCM', label: '中医特色' },
  { value: 'MGMT', label: '科室管理' },
  { value: 'OTHER', label: '其他' }
]
const checkMethods = [
  { value: 'ON_SITE', label: '现场查看' },
  { value: 'RECORD_REVIEW', label: '抽查病历' },
  { value: 'INTERVIEW', label: '访谈' },
  { value: 'DOC_REVIEW', label: '资料查阅' }
]
const applicableTypes = [
  { value: 'ALL', label: '通用' },
  { value: 'SELF', label: '科室自查' },
  { value: 'HOSPITAL', label: '院级检查' },
  { value: 'SPECIAL', label: '专项检查' }
]
const levels = [
  { value: 'NATIONAL', label: '国家' },
  { value: 'PROVINCE', label: '省' },
  { value: 'CITY', label: '市' },
  { value: 'HOSPITAL', label: '院内' }
]
const itemStatuses = [
  { value: 1, label: '启用' },
  { value: 0, label: '停用' }
]
const standardStatuses = [
  { value: 1, label: '现行' },
  { value: 0, label: '已废止' }
]

function categoryText(v) {
  return (categories.find((c) => c.value === v) || {}).label || v || ''
}
function checkMethodText(v) {
  return (checkMethods.find((c) => c.value === v) || {}).label || v || ''
}
function applicableTypeText(v) {
  return (applicableTypes.find((c) => c.value === v) || {}).label || v || ''
}
function levelText(v) {
  return (levels.find((c) => c.value === v) || {}).label || v || ''
}
function itemStatusText(v) {
  return (itemStatuses.find((c) => c.value === v) || {}).label || ''
}
function standardStatusText(v) {
  return (standardStatuses.find((c) => c.value === v) || {}).label || ''
}
function inspectTypeText(v) {
  return (applicableTypes.find((c) => c.value === v) || {}).label || v || ''
}
function isStandardExpired(row) {
  if (!row.expiryDate || row.status !== 1) return false
  return new Date(row.expiryDate) < new Date(new Date().toDateString())
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

// ---------- 统计 ----------
const itemStats = ref({})
const itemCategoryStats = ref([])
const standardStats = ref({})
const standardLevelStats = ref([])
const templateStats = ref({})

async function fetchStats() {
  try {
    itemStats.value = await getInspectionItemStats()
    itemCategoryStats.value = await getInspectionItemStatsByCategory()
    standardStats.value = await getInspectionStandardStats()
    standardLevelStats.value = await getInspectionStandardStatsByLevel()
    templateStats.value = await getInspectionTemplateItemStats()
  } catch (e) {
    console.warn('统计加载失败', e.message)
  }
}

// ---------- 检查项目 ----------
const itemQuery = reactive({ keyword: '', category: '', checkMethod: '', applicableType: '', standardId: null, status: null, linked: false, unlinked: false, page: 1, size: 10 })
const itemData = ref([])
const itemTotal = ref(0)
const itemLoading = ref(false)
const itemSelected = ref([])
const standardOptions = ref([])

async function fetchItemList() {
  itemLoading.value = true
  try {
    const data = await getInspectionItems({ ...itemQuery })
    itemData.value = data.records
    itemTotal.value = data.total
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    itemLoading.value = false
  }
}

async function fetchStandardOptions() {
  try {
    const data = await getInspectionStandards({ page: 1, size: 10000 })
    standardOptions.value = data.records || []
  } catch (e) {
    console.warn('标准下拉加载失败', e.message)
  }
}

function handleItemSearch() {
  itemQuery.page = 1
  fetchItemList()
}

function handleItemReset() {
  Object.assign(itemQuery, { keyword: '', category: '', checkMethod: '', applicableType: '', standardId: null, status: null, linked: false, unlinked: false, page: 1, size: 10 })
  fetchItemList()
}

const itemDialogVisible = ref(false)
const itemDialogTitle = ref('新增检查项目')
const itemFormRef = ref(null)
const itemForm = reactive({
  id: null, itemNo: '', itemName: '', category: 'MEDICAL_RECORD', checkMethod: 'RECORD_REVIEW',
  standardId: null, standardText: '', maxScore: 100, weight: 10, applicableType: 'ALL',
  sortOrder: 1, remark: ''
})
const itemRules = {
  itemNo: [{ required: true, message: '请填写项目编号', trigger: 'blur' }],
  itemName: [{ required: true, message: '请填写项目名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择项目分类', trigger: 'change' }]
}

function resetItemForm() {
  Object.assign(itemForm, {
    id: null, itemNo: '', itemName: '', category: 'MEDICAL_RECORD', checkMethod: 'RECORD_REVIEW',
    standardId: null, standardText: '', maxScore: 100, weight: 10, applicableType: 'ALL',
    sortOrder: 1, remark: ''
  })
}

function handleItemAdd() {
  resetItemForm()
  itemDialogTitle.value = '新增检查项目'
  itemDialogVisible.value = true
}

function handleItemEdit(row) {
  resetItemForm()
  Object.assign(itemForm, row)
  itemDialogTitle.value = '编辑检查项目'
  itemDialogVisible.value = true
}

async function handleItemSubmit() {
  await itemFormRef.value.validate()
  try {
    if (itemForm.id) {
      await updateInspectionItem(itemForm.id, { ...itemForm })
      ElMessage.success('检查项目修改成功')
    } else {
      await createInspectionItem({ ...itemForm })
      ElMessage.success('检查项目新增成功')
    }
    itemDialogVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleItemDelete(row) {
  await ElMessageBox.confirm(`确定删除检查项目「${row.itemName}」（${row.itemNo}）吗？已被模板引用的项目不能删除。`, '提示', { type: 'warning' })
  try {
    await deleteInspectionItem(row.id)
    ElMessage.success('删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

async function handleItemBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${itemSelected.value.length} 个检查项目吗？已被模板引用的项目不能删除。`, '提示', { type: 'warning' })
  try {
    await batchDeleteInspectionItems(itemSelected.value.map((r) => r.id))
    ElMessage.success('批量删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('批量删除失败：' + e.message)
  }
}

async function handleItemDisable(row) {
  await ElMessageBox.confirm(`确定停用检查项目「${row.itemName}」吗？已被模板引用的项目不可停用。`, '提示', { type: 'warning' })
  try {
    await disableInspectionItem(row.id)
    ElMessage.success('项目已停用')
    refreshAll()
  } catch (e) {
    ElMessage.error('停用失败：' + e.message)
  }
}

async function handleItemEnable(row) {
  try {
    await enableInspectionItem(row.id)
    ElMessage.success('项目已启用')
    refreshAll()
  } catch (e) {
    ElMessage.error('启用失败：' + e.message)
  }
}

async function handleItemExport() {
  try {
    const res = await exportInspectionItems({ ...itemQuery, page: 1, size: 10000 })
    downloadBlob(res.data, '检查项目.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleItemDownloadTemplate() {
  const res = await downloadInspectionItemTemplate()
  downloadBlob(res.data, '检查项目导入模板.xlsx')
}

function handleItemImportSuccess(res) {
  if (res.code === 0) {
    ElMessage.success(`导入成功，共导入 ${res.data.imported} 条`)
    refreshAll()
  } else {
    ElMessage.error('导入失败：' + (res.msg || '未知错误'))
  }
}

// ---------- 检查标准 ----------
const standardQuery = reactive({ keyword: '', level: '', status: null, expiringSoon: false, expired: false, page: 1, size: 10 })
const standardData = ref([])
const standardTotal = ref(0)
const standardLoading = ref(false)
const standardSelected = ref([])

async function fetchStandardList() {
  standardLoading.value = true
  try {
    const data = await getInspectionStandards({ ...standardQuery })
    standardData.value = data.records
    standardTotal.value = data.total
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    standardLoading.value = false
  }
}

function handleStandardSearch() {
  standardQuery.page = 1
  fetchStandardList()
}

function handleStandardReset() {
  Object.assign(standardQuery, { keyword: '', level: '', status: null, expiringSoon: false, expired: false, page: 1, size: 10 })
  fetchStandardList()
}

const standardDialogVisible = ref(false)
const standardDialogTitle = ref('新增检查标准')
const standardFormRef = ref(null)
const standardForm = reactive({
  id: null, standardNo: '', standardName: '', level: 'NATIONAL', sourceDoc: '',
  content: '', applicableScope: '全院', effectiveDate: '', expiryDate: '', remark: ''
})
const standardRules = {
  standardNo: [{ required: true, message: '请填写标准编号', trigger: 'blur' }],
  standardName: [{ required: true, message: '请填写标准名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择标准级别', trigger: 'change' }]
}

function resetStandardForm() {
  Object.assign(standardForm, {
    id: null, standardNo: '', standardName: '', level: 'NATIONAL', sourceDoc: '',
    content: '', applicableScope: '全院', effectiveDate: '', expiryDate: '', remark: ''
  })
}

function handleStandardAdd() {
  resetStandardForm()
  standardDialogTitle.value = '新增检查标准'
  standardDialogVisible.value = true
}

function handleStandardEdit(row) {
  resetStandardForm()
  Object.assign(standardForm, row)
  standardDialogTitle.value = '编辑检查标准'
  standardDialogVisible.value = true
}

async function handleStandardSubmit() {
  await standardFormRef.value.validate()
  try {
    if (standardForm.id) {
      await updateInspectionStandard(standardForm.id, { ...standardForm })
      ElMessage.success('检查标准修改成功')
    } else {
      await createInspectionStandard({ ...standardForm })
      ElMessage.success('检查标准新增成功')
    }
    standardDialogVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleStandardDelete(row) {
  await ElMessageBox.confirm(`确定删除检查标准「${row.standardName}」（${row.standardNo}）吗？已被检查项目引用的标准不能删除。`, '提示', { type: 'warning' })
  try {
    await deleteInspectionStandard(row.id)
    ElMessage.success('删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

async function handleStandardBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${standardSelected.value.length} 条检查标准吗？已被检查项目引用的标准不能删除。`, '提示', { type: 'warning' })
  try {
    await batchDeleteInspectionStandards(standardSelected.value.map((r) => r.id))
    ElMessage.success('批量删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('批量删除失败：' + e.message)
  }
}

const abolishVisible = ref(false)
const abolishRow = ref({})
const abolishForm = reactive({ reason: '' })

function handleStandardAbolish(row) {
  abolishRow.value = row
  Object.assign(abolishForm, { reason: '' })
  abolishVisible.value = true
}

async function handleAbolishSubmit() {
  if (!abolishForm.reason) {
    ElMessage.warning('请填写废止原因')
    return
  }
  try {
    await abolishInspectionStandard(abolishRow.value.id, { ...abolishForm })
    ElMessage.success('标准已废止')
    abolishVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('废止失败：' + e.message)
  }
}

async function handleStandardRestore(row) {
  await ElMessageBox.confirm(`确定将检查标准「${row.standardName}」恢复为现行吗？`, '提示', { type: 'info' })
  try {
    await restoreInspectionStandard(row.id)
    ElMessage.success('标准已恢复现行')
    refreshAll()
  } catch (e) {
    ElMessage.error('恢复失败：' + e.message)
  }
}

async function handleStandardExport() {
  try {
    const res = await exportInspectionStandards({ ...standardQuery, page: 1, size: 10000 })
    downloadBlob(res.data, '检查标准.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleStandardDownloadTemplate() {
  const res = await downloadInspectionStandardTemplate()
  downloadBlob(res.data, '检查标准导入模板.xlsx')
}

function handleStandardImportSuccess(res) {
  if (res.code === 0) {
    ElMessage.success(`导入成功，共导入 ${res.data.imported} 条`)
    refreshAll()
  } else {
    ElMessage.error('导入失败：' + (res.msg || '未知错误'))
  }
}

// ---------- 模板检查项配置 ----------
const templateOptions = ref([])
const selectedTemplateId = ref(null)
const templateLinkData = ref([])
const templateLinkLoading = ref(false)
const templateWeightSum = computed(() =>
  templateLinkData.value.reduce((s, r) => s + Number(r.weight || 0), 0))
const templateImportAction = computed(() =>
  selectedTemplateId.value
    ? `/api/inspection-template-items/import?templateId=${selectedTemplateId.value}`
    : '/api/inspection-template-items/import')

async function fetchTemplateOptions() {
  try {
    const data = await getQualityTemplates({ page: 1, size: 10000 })
    templateOptions.value = data.records || []
    if (!selectedTemplateId.value && templateOptions.value.length) {
      selectedTemplateId.value = templateOptions.value[0].id
      fetchTemplateLinks()
    }
  } catch (e) {
    console.warn('模板下拉加载失败', e.message)
  }
}

async function fetchTemplateLinks() {
  if (!selectedTemplateId.value) {
    templateLinkData.value = []
    return
  }
  templateLinkLoading.value = true
  try {
    templateLinkData.value = await getInspectionTemplateItemsByTemplate(selectedTemplateId.value)
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    templateLinkLoading.value = false
  }
}

const configDialogVisible = ref(false)
const configTemplateName = ref('')
const configTableRef = ref(null)
const configItems = ref([])
const configSelected = ref([])
const configWeights = reactive({})

const configWeightSum = computed(() =>
  configSelected.value.reduce((s, r) => s + Number(configWeights[r.id] || 0), 0))

async function handleTemplateConfigure() {
  if (!selectedTemplateId.value) return
  const t = templateOptions.value.find((x) => x.id === selectedTemplateId.value)
  configTemplateName.value = t ? t.templateName : ''
  try {
    const data = await getInspectionItems({ status: 1, page: 1, size: 10000 })
    configItems.value = data.records || []
    const current = templateLinkData.value || []
    const currentIds = new Set(current.map((r) => r.itemId))
    configSelected.value = configItems.value.filter((r) => currentIds.has(r.id))
    Object.keys(configWeights).forEach((k) => delete configWeights[k])
    configItems.value.forEach((r) => {
      const link = current.find((x) => x.itemId === r.id)
      configWeights[r.id] = link ? Number(link.weight) : Number(r.weight || 10)
    })
    configDialogVisible.value = true
    await nextTick()
    configSelected.value.forEach((r) => configTableRef.value?.toggleRowSelection(r, true))
  } catch (e) {
    ElMessage.error('加载检查项目失败：' + e.message)
  }
}

async function handleConfigSubmit() {
  if (!configSelected.value.length) {
    ElMessage.warning('请至少选择一项检查项目')
    return
  }
  if (configWeightSum.value !== 100) {
    ElMessage.warning(`检查项权重之和必须为 100，当前为 ${configWeightSum.value}`)
    return
  }
  try {
    await configureInspectionTemplateItems({
      templateId: selectedTemplateId.value,
      items: configSelected.value.map((r, i) => ({
        itemId: r.id,
        weight: configWeights[r.id],
        sortOrder: i + 1
      }))
    })
    ElMessage.success('模板检查项配置成功，模板检查项已同步更新')
    configDialogVisible.value = false
    fetchTemplateLinks()
    fetchStats()
    fetchItemList()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleTemplateLinkDelete(row) {
  await ElMessageBox.confirm(`确定从模板里移除检查项「${row.itemName}」吗？一个模板至少保留一项。`, '提示', { type: 'warning' })
  try {
    await deleteInspectionTemplateItem(row.id)
    ElMessage.success('删除成功，模板检查项已同步更新')
    fetchTemplateLinks()
    fetchStats()
    fetchItemList()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

async function handleTemplateExport() {
  try {
    const res = await exportInspectionTemplateItems({ templateId: selectedTemplateId.value })
    downloadBlob(res.data, '模板检查项.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleTemplateDownloadTemplate() {
  const res = await downloadInspectionTemplateItemTemplate(selectedTemplateId.value)
  downloadBlob(res.data, '模板检查项导入模板.xlsx')
}

function handleTemplateImportSuccess(res) {
  if (res.code === 0) {
    ElMessage.success(`导入成功，共导入 ${res.data.imported} 条，模板检查项已同步更新`)
    fetchTemplateLinks()
    fetchStats()
    fetchItemList()
  } else {
    ElMessage.error('导入失败：' + (res.msg || '未知错误'))
  }
}

function refreshAll() {
  fetchItemList()
  fetchStandardList()
  fetchStats()
  fetchStandardOptions()
  fetchTemplateOptions()
  fetchTemplateLinks()
}

onMounted(() => {
  refreshAll()
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
.expired {
  color: #f56c6c;
  font-weight: bold;
}
</style>
