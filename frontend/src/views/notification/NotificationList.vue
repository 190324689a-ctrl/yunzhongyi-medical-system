<template>
  <div>
    <p class="page-note">口径：需求清单第 29 项“消息提醒”。站内消息收发与已读、待办提醒（由各业务待审批事项汇总生成或手工创建）、消息推送记录、短信提醒记录；推送目标支持按角色、按人员、全部人员三种接收范围。短信网关与推送通道本机没有真实服务：发送与重推为预留占位，调用明确返回失败、如实记录，不伪造发送成功；与 28 号提醒设置联动（可按其配置生成待办），不另造配置表。</p>

    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.messages || 0 }}</div><div class="label">站内消息</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num warn">{{ overview.unreadMessages || 0 }}</div><div class="label">未读消息</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.todos || 0 }}</div><div class="label">待办提醒</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num warn">{{ overview.pendingTodos || 0 }}</div><div class="label">待办中</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num warn">{{ overview.overdueTodos || 0 }}</div><div class="label">已逾期待办</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.pushRecords || 0 }}</div><div class="label">推送记录</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.smsRecords || 0 }}</div><div class="label">短信记录</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.noticeSettings || 0 }}</div><div class="label">提醒设置（28 号）</div></div></el-card></el-col>
    </el-row>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 站内消息 -->
      <el-tab-pane label="站内消息" name="message">
        <el-card>
          <el-form :inline="true" :model="mQuery" class="search-form">
            <el-form-item label="关键字"><el-input v-model="mQuery.keyword" placeholder="标题 / 内容 / 发送人" clearable style="width: 200px" /></el-form-item>
            <el-form-item label="分类">
              <el-select v-model="mQuery.category" placeholder="全部" clearable style="width: 130px">
                <el-option v-for="c in messageCategories" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="已读状态">
              <el-select v-model="mQuery.readStatus" placeholder="全部" clearable style="width: 110px">
                <el-option v-for="s in readStatuses" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="接收范围">
              <el-select v-model="mQuery.receiverScope" placeholder="全部" clearable style="width: 120px">
                <el-option v-for="s in receiverScopes" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item><el-button type="primary" @click="handleMSearch">查询</el-button><el-button @click="handleMReset">重置</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card style="margin-top: 16px">
          <div class="toolbar">
            <div><el-button type="primary" @click="handleMAdd">发送消息</el-button></div>
            <div>
              <el-button @click="handleMReadAll">全部标已读</el-button>
              <el-upload action="/api/site-messages/import" :show-file-list="false" accept=".xlsx,.xls" :on-success="handleMImportSuccess" :on-error="handleImportError" style="display: inline-block; margin: 0 8px"><el-button type="success">批量导入</el-button></el-upload>
              <el-button type="warning" @click="handleMExport">导出 Excel</el-button>
              <el-button @click="handleMDownloadTemplate">下载模板</el-button>
            </div>
          </div>
          <el-table :data="mData" border stripe v-loading="mLoading" style="margin-top: 12px">
            <el-table-column prop="title" label="标题" width="260" />
            <el-table-column label="分类" width="100"><template #default="{ row }">{{ categoryText(row.category) }}</template></el-table-column>
            <el-table-column prop="senderName" label="发送人" width="110" />
            <el-table-column label="接收范围" width="110"><template #default="{ row }">{{ scopeText(row.receiverScope) }}{{ row.receiverValue ? '：' + row.receiverValue : '' }}</template></el-table-column>
            <el-table-column prop="receiverName" label="接收人" width="100" />
            <el-table-column label="已读状态" width="90"><template #default="{ row }"><el-tag :type="row.readStatus === 1 ? 'success' : 'warning'">{{ row.readStatus === 1 ? '已读' : '未读' }}</el-tag></template></el-table-column>
            <el-table-column prop="sendTime" label="发送时间" width="170" />
            <el-table-column prop="content" label="内容" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" v-if="row.readStatus === 0" @click="handleMRead(row)">标已读</el-button>
                <el-button link type="primary" v-if="row.readStatus === 0" @click="handleMEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="handleMDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="mQuery.page" v-model:page-size="mQuery.size" :total="mTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchMessages" @current-change="fetchMessages" />
        </el-card>
      </el-tab-pane>

      <!-- 待办提醒 -->
      <el-tab-pane label="待办提醒" name="todo">
        <el-card>
          <el-form :inline="true" :model="tQuery" class="search-form">
            <el-form-item label="关键字"><el-input v-model="tQuery.keyword" placeholder="标题 / 内容 / 责任人" clearable style="width: 200px" /></el-form-item>
            <el-form-item label="待办类型">
              <el-select v-model="tQuery.todoType" placeholder="全部" clearable style="width: 130px">
                <el-option v-for="c in todoTypes" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="tQuery.status" placeholder="全部" clearable style="width: 110px">
                <el-option v-for="s in todoStatuses" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="来源">
              <el-select v-model="tQuery.source" placeholder="全部" clearable style="width: 140px">
                <el-option v-for="s in todoSources" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item><el-button type="primary" @click="handleTSearch">查询</el-button><el-button @click="handleTReset">重置</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card style="margin-top: 16px">
          <div class="toolbar">
            <div>
              <el-button type="primary" @click="handleTAdd">新建待办</el-button>
              <el-button @click="handleTGenerate">按提醒设置生成</el-button>
              <el-button @click="handleTOverdueScan">逾期扫描</el-button>
            </div>
            <div>
              <el-upload action="/api/todo-reminders/import" :show-file-list="false" accept=".xlsx,.xls" :on-success="handleTImportSuccess" :on-error="handleImportError" style="display: inline-block; margin: 0 8px"><el-button type="success">批量导入</el-button></el-upload>
              <el-button type="warning" @click="handleTExport">导出 Excel</el-button>
              <el-button @click="handleTDownloadTemplate">下载模板</el-button>
            </div>
          </div>
          <el-table :data="tData" border stripe v-loading="tLoading" style="margin-top: 12px">
            <el-table-column prop="title" label="标题" width="240" />
            <el-table-column label="待办类型" width="100"><template #default="{ row }">{{ todoTypeText(row.todoType) }}</template></el-table-column>
            <el-table-column prop="assigneeName" label="责任人" width="100" />
            <el-table-column prop="deptName" label="责任科室" width="110" />
            <el-table-column prop="dueDate" label="截止日期" width="120" />
            <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="todoStatusTag(row.status)">{{ todoStatusText(row.status) }}</el-tag></template></el-table-column>
            <el-table-column label="来源" width="130"><template #default="{ row }">{{ todoSourceText(row.source) }}</template></el-table-column>
            <el-table-column prop="settingKey" label="提醒设置编码" width="130" />
            <el-table-column prop="content" label="内容" />
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" v-if="row.status !== 1" @click="handleTComplete(row)">完成</el-button>
                <el-button link type="primary" v-if="row.status !== 1" @click="handleTEdit(row)">编辑</el-button>
                <el-button link type="danger" v-if="row.status !== 1" @click="handleTDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="tQuery.page" v-model:page-size="tQuery.size" :total="tTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchTodos" @current-change="fetchTodos" />
        </el-card>
      </el-tab-pane>

      <!-- 消息推送 -->
      <el-tab-pane label="消息推送" name="push">
        <p class="page-note">推送通道本机未对接真实服务：发送与重推为预留占位，调用明确返回失败、如实记录，不伪造发送成功。</p>
        <el-card>
          <el-form :inline="true" :model="pQuery" class="search-form">
            <el-form-item label="关键字"><el-input v-model="pQuery.keyword" placeholder="标题 / 内容 / 接收人" clearable style="width: 200px" /></el-form-item>
            <el-form-item label="推送渠道">
              <el-select v-model="pQuery.channel" placeholder="全部" clearable style="width: 120px">
                <el-option v-for="c in pushChannels" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="发送状态">
              <el-select v-model="pQuery.sendStatus" placeholder="全部" clearable style="width: 120px">
                <el-option v-for="s in sendStatuses" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item><el-button type="primary" @click="handlePSearch">查询</el-button><el-button @click="handlePReset">重置</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card style="margin-top: 16px">
          <div class="toolbar">
            <div><el-button type="primary" @click="handlePAdd">发送推送</el-button></div>
            <div><el-button type="warning" @click="handlePExport">导出 Excel</el-button></div>
          </div>
          <el-table :data="pData" border stripe v-loading="pLoading" style="margin-top: 12px">
            <el-table-column prop="title" label="标题" width="240" />
            <el-table-column label="推送渠道" width="100"><template #default="{ row }">{{ pushChannelText(row.channel) }}</template></el-table-column>
            <el-table-column label="接收范围" width="110"><template #default="{ row }">{{ scopeText(row.receiverScope) }}{{ row.receiverValue ? '：' + row.receiverValue : '' }}</template></el-table-column>
            <el-table-column prop="receiverName" label="接收人" width="100" />
            <el-table-column label="发送状态" width="100"><template #default="{ row }"><el-tag :type="sendStatusTag(row.sendStatus)">{{ sendStatusText(row.sendStatus) }}</el-tag></template></el-table-column>
            <el-table-column prop="sendTime" label="发送时间" width="170" />
            <el-table-column prop="failReason" label="失败原因" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button link type="warning" v-if="row.sendStatus !== 1" @click="handlePRetry(row)">重推</el-button>
                <el-button link type="danger" v-if="row.sendStatus !== 1" @click="handlePDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="pQuery.page" v-model:page-size="pQuery.size" :total="pTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchPushRecords" @current-change="fetchPushRecords" />
        </el-card>
      </el-tab-pane>

      <!-- 短信提醒 -->
      <el-tab-pane label="短信提醒" name="sms">
        <p class="page-note">短信网关本机未对接真实服务：发送与重发为预留占位，调用明确返回失败、如实记录，不伪造发送成功。</p>
        <el-card>
          <el-form :inline="true" :model="sQuery" class="search-form">
            <el-form-item label="关键字"><el-input v-model="sQuery.keyword" placeholder="手机号 / 内容 / 接收人" clearable style="width: 220px" /></el-form-item>
            <el-form-item label="发送状态">
              <el-select v-model="sQuery.sendStatus" placeholder="全部" clearable style="width: 120px">
                <el-option v-for="s in sendStatuses" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item><el-button type="primary" @click="handleSSearch">查询</el-button><el-button @click="handleSReset">重置</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card style="margin-top: 16px">
          <div class="toolbar">
            <div><el-button type="primary" @click="handleSAdd">发送短信</el-button></div>
            <div><el-button type="warning" @click="handleSExport">导出 Excel</el-button></div>
          </div>
          <el-table :data="sData" border stripe v-loading="sLoading" style="margin-top: 12px">
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column prop="receiverName" label="接收人" width="100" />
            <el-table-column prop="templateCode" label="模板编码" width="140" />
            <el-table-column label="发送状态" width="100"><template #default="{ row }"><el-tag :type="sendStatusTag(row.sendStatus)">{{ sendStatusText(row.sendStatus) }}</el-tag></template></el-table-column>
            <el-table-column prop="sendTime" label="发送时间" width="170" />
            <el-table-column prop="failReason" label="失败原因" />
            <el-table-column prop="content" label="内容" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button link type="warning" v-if="row.sendStatus !== 1" @click="handleSRetry(row)">重发</el-button>
                <el-button link type="danger" v-if="row.sendStatus !== 1" @click="handleSDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="sQuery.page" v-model:page-size="sQuery.size" :total="sTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchSmsRecords" @current-change="fetchSmsRecords" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 站内消息弹窗 -->
    <el-dialog v-model="mDialogVisible" :title="mDialogTitle" width="600px">
      <el-form :model="mForm" label-width="100px">
        <el-form-item label="标题"><el-input v-model="mForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="mForm.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="mForm.category" style="width: 100%">
            <el-option v-for="c in messageCategories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="发送人"><el-input v-model="mForm.senderName" placeholder="如 医务科" /></el-form-item>
        <el-form-item label="接收范围">
          <el-select v-model="mForm.receiverScope" style="width: 100%">
            <el-option v-for="s in receiverScopes" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收对象" v-if="mForm.receiverScope !== 'ALL'"><el-input v-model="mForm.receiverValue" :placeholder="mForm.receiverScope === 'ROLE' ? '角色编码，如 MEDICAL_ADMIN' : '用户名，如 admin'" /></el-form-item>
        <el-form-item label="接收人"><el-input v-model="mForm.receiverName" /></el-form-item>
        <el-form-item label="关联业务类型"><el-input v-model="mForm.relatedType" placeholder="如 LEAVE / AUTH" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 待办提醒弹窗 -->
    <el-dialog v-model="tDialogVisible" :title="tDialogTitle" width="600px">
      <el-form :model="tForm" label-width="100px">
        <el-form-item label="标题"><el-input v-model="tForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="tForm.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="待办类型">
          <el-select v-model="tForm.todoType" style="width: 100%">
            <el-option v-for="c in todoTypes" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任人"><el-input v-model="tForm.assignee" placeholder="用户名" /></el-form-item>
        <el-form-item label="责任人姓名"><el-input v-model="tForm.assigneeName" /></el-form-item>
        <el-form-item label="责任科室"><el-input v-model="tForm.deptName" /></el-form-item>
        <el-form-item label="截止日期"><el-date-picker v-model="tForm.dueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="关联业务类型"><el-input v-model="tForm.relatedType" placeholder="如 LEAVE / AUTH" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 按提醒设置生成待办弹窗 -->
    <el-dialog v-model="gDialogVisible" title="按 28 号提醒设置生成待办" width="600px">
      <el-form :model="gForm" label-width="110px">
        <el-form-item label="提醒设置编码"><el-input v-model="gForm.settingKey" placeholder="如 LEAVE_APPROVE（须存在且启用）" /></el-form-item>
        <el-form-item label="待办标题"><el-input v-model="gForm.title" placeholder="为空时用设置名称" /></el-form-item>
        <el-form-item label="待办内容"><el-input v-model="gForm.content" type="textarea" :rows="3" placeholder="为空时用设置模板内容" /></el-form-item>
        <el-form-item label="待办类型">
          <el-select v-model="gForm.todoType" style="width: 100%">
            <el-option v-for="c in todoTypes" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任人"><el-input v-model="gForm.assignee" placeholder="用户名，必填" /></el-form-item>
        <el-form-item label="责任人姓名"><el-input v-model="gForm.assigneeName" /></el-form-item>
        <el-form-item label="责任科室"><el-input v-model="gForm.deptName" /></el-form-item>
        <el-form-item label="截止日期"><el-date-picker v-model="gForm.dueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="gDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGSave">生成</el-button>
      </template>
    </el-dialog>

    <!-- 消息推送弹窗 -->
    <el-dialog v-model="pDialogVisible" title="发送推送" width="600px">
      <el-form :model="pForm" label-width="100px">
        <el-form-item label="标题"><el-input v-model="pForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="pForm.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="推送渠道">
          <el-select v-model="pForm.channel" style="width: 100%">
            <el-option v-for="c in pushChannels" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收范围">
          <el-select v-model="pForm.receiverScope" style="width: 100%">
            <el-option v-for="s in receiverScopes" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收对象" v-if="pForm.receiverScope !== 'ALL'"><el-input v-model="pForm.receiverValue" :placeholder="pForm.receiverScope === 'ROLE' ? '角色编码，如 MEDICAL_ADMIN' : '用户名，如 admin'" /></el-form-item>
        <el-form-item label="接收人"><el-input v-model="pForm.receiverName" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePSave">发送</el-button>
      </template>
    </el-dialog>

    <!-- 短信提醒弹窗 -->
    <el-dialog v-model="sDialogVisible" title="发送短信" width="600px">
      <el-form :model="sForm" label-width="100px">
        <el-form-item label="手机号"><el-input v-model="sForm.phone" placeholder="11 位大陆手机号" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="sForm.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="接收人"><el-input v-model="sForm.receiverName" /></el-form-item>
        <el-form-item label="模板编码"><el-input v-model="sForm.templateCode" placeholder="如 AUTH_EXPIRE" /></el-form-item>
        <el-form-item label="关联业务类型"><el-input v-model="sForm.relatedType" placeholder="如 AUTH / QUALITY" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSSave">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getNotificationOverview,
  getSiteMessages, createSiteMessage, updateSiteMessage, deleteSiteMessage,
  markMessageRead, markAllMessagesRead, exportSiteMessages, downloadSiteMessageTemplate,
  getTodoReminders, createTodoReminder, updateTodoReminder, deleteTodoReminder,
  completeTodoReminder, scanOverdueTodos, generateTodoFromSetting,
  exportTodoReminders, downloadTodoReminderTemplate,
  getPushRecords, sendPushRecord, retryPushRecord, deletePushRecord, exportPushRecords,
  getSmsRecords, sendSmsRecord, retrySmsRecord, deleteSmsRecord, exportSmsRecords
} from '../../api/notification'

// ---------- 字典 ----------
const messageCategories = [
  { value: 'NOTICE', label: '通知' },
  { value: 'ANNOUNCE', label: '公告' },
  { value: 'TODO', label: '待办提醒' },
  { value: 'SYSTEM', label: '系统消息' }
]
const readStatuses = [
  { value: 0, label: '未读' },
  { value: 1, label: '已读' }
]
const receiverScopes = [
  { value: 'ALL', label: '全部人员' },
  { value: 'ROLE', label: '按角色' },
  { value: 'USER', label: '按人员' }
]
const todoTypes = [
  { value: 'APPROVAL', label: '审批待办' },
  { value: 'EXPIRE', label: '到期提醒' },
  { value: 'RECTIFY', label: '整改待办' },
  { value: 'OTHER', label: '其他待办' }
]
const todoStatuses = [
  { value: 0, label: '待办' },
  { value: 1, label: '已完成' },
  { value: 2, label: '已逾期' }
]
const todoSources = [
  { value: 'MANUAL', label: '手工创建' },
  { value: 'AUTO', label: '系统生成' },
  { value: 'SETTING', label: '按提醒设置生成' }
]
const pushChannels = [
  { value: 'WECHAT', label: '微信' },
  { value: 'APP', label: '移动端' },
  { value: 'EMAIL', label: '邮件' }
]
const sendStatuses = [
  { value: 0, label: '待发送' },
  { value: 1, label: '已发送' },
  { value: 2, label: '发送失败' }
]

function labelOf(list, v) {
  return (list.find((c) => c.value === v) || {}).label || v || ''
}
function categoryText(v) { return labelOf(messageCategories, v) }
function scopeText(v) { return labelOf(receiverScopes, v) }
function todoTypeText(v) { return labelOf(todoTypes, v) }
function todoStatusText(v) { return labelOf(todoStatuses, v) }
function todoStatusTag(v) { return { 0: 'warning', 1: 'success', 2: 'danger' }[v] || 'info' }
function todoSourceText(v) { return labelOf(todoSources, v) }
function pushChannelText(v) { return labelOf(pushChannels, v) }
function sendStatusText(v) { return labelOf(sendStatuses, v) }
function sendStatusTag(v) { return { 0: '', 1: 'success', 2: 'danger' }[v] || 'info' }

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
function checkOk(res, action) {
  if (res.code !== 0) {
    ElMessage.error((action || '操作') + '失败：' + (res.msg || '未知错误'))
    return false
  }
  return true
}

// ---------- 公共 ----------
const activeTab = ref('message')
const overview = ref({})

async function fetchOverview() {
  try {
    const res = await getNotificationOverview()
    overview.value = res.data || {}
  } catch (e) {
    ElMessage.error('加载总览失败：' + e.message)
  }
}

function handleTabChange() {
  if (activeTab.value === 'message') fetchMessages()
  else if (activeTab.value === 'todo') fetchTodos()
  else if (activeTab.value === 'push') fetchPushRecords()
  else fetchSmsRecords()
}

onMounted(() => {
  fetchOverview()
  fetchMessages()
})

// ---------- 站内消息 ----------
const mData = ref([])
const mTotal = ref(0)
const mLoading = ref(false)
const mQuery = reactive({ keyword: '', category: '', readStatus: '', receiverScope: '', page: 1, size: 10 })

async function fetchMessages() {
  mLoading.value = true
  try {
    const res = await getSiteMessages({ ...mQuery })
    mData.value = res.data.records || []
    mTotal.value = res.data.total || 0
    fetchOverview()
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    mLoading.value = false
  }
}
function handleMSearch() { mQuery.page = 1; fetchMessages() }
function handleMReset() {
  Object.assign(mQuery, { keyword: '', category: '', readStatus: '', receiverScope: '', page: 1, size: 10 })
  fetchMessages()
}

const mDialogVisible = ref(false)
const mDialogTitle = ref('发送消息')
const mForm = reactive({ id: null, title: '', content: '', category: 'NOTICE', senderName: '医务科', receiverScope: 'ALL', receiverValue: '', receiverName: '', relatedType: '' })

function handleMAdd() {
  mDialogTitle.value = '发送消息'
  Object.assign(mForm, { id: null, title: '', content: '', category: 'NOTICE', senderName: '医务科', receiverScope: 'ALL', receiverValue: '', receiverName: '', relatedType: '' })
  mDialogVisible.value = true
}
function handleMEdit(row) {
  mDialogTitle.value = '编辑消息'
  Object.assign(mForm, { ...row })
  mDialogVisible.value = true
}
async function handleMSave() {
  try {
    const res = mForm.id
      ? await updateSiteMessage(mForm.id, { ...mForm })
      : await createSiteMessage({ ...mForm })
    if (!checkOk(res, '保存')) return
    ElMessage.success('保存成功')
    mDialogVisible.value = false
    fetchMessages()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}
async function handleMRead(row) {
  try {
    const res = await markMessageRead(row.id)
    if (!checkOk(res, '标已读')) return
    ElMessage.success('已标为已读')
    fetchMessages()
  } catch (e) {
    ElMessage.error('标已读失败：' + e.message)
  }
}
async function handleMReadAll() {
  try {
    const res = await markAllMessagesRead({})
    if (!checkOk(res, '全部标已读')) return
    ElMessage.success('已标已读 ' + (res.data.read || 0) + ' 条')
    fetchMessages()
  } catch (e) {
    ElMessage.error('全部标已读失败：' + e.message)
  }
}
async function handleMDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该消息吗？', '提示', { type: 'warning' })
    const res = await deleteSiteMessage(row.id)
    if (!checkOk(res, '删除')) return
    ElMessage.success('删除成功')
    fetchMessages()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + (e.message || e))
  }
}
async function handleMExport() {
  try {
    const res = await exportSiteMessages({ ...mQuery })
    downloadBlob(res, '站内消息.xlsx')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}
function handleMImportSuccess(res) {
  if (!checkOk(res, '导入')) return
  ElMessage.success('导入成功：新增 ' + (res.data.created || 0) + ' 条，跳过 ' + (res.data.skipped || 0) + ' 条')
  fetchMessages()
}
async function handleMDownloadTemplate() {
  try {
    const res = await downloadSiteMessageTemplate()
    downloadBlob(res, '站内消息导入模板.xlsx')
  } catch (e) {
    ElMessage.error('下载模板失败：' + e.message)
  }
}

// ---------- 待办提醒 ----------
const tData = ref([])
const tTotal = ref(0)
const tLoading = ref(false)
const tQuery = reactive({ keyword: '', todoType: '', status: '', source: '', page: 1, size: 10 })

async function fetchTodos() {
  tLoading.value = true
  try {
    const res = await getTodoReminders({ ...tQuery })
    tData.value = res.data.records || []
    tTotal.value = res.data.total || 0
    fetchOverview()
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    tLoading.value = false
  }
}
function handleTSearch() { tQuery.page = 1; fetchTodos() }
function handleTReset() {
  Object.assign(tQuery, { keyword: '', todoType: '', status: '', source: '', page: 1, size: 10 })
  fetchTodos()
}

const tDialogVisible = ref(false)
const tDialogTitle = ref('新建待办')
const tForm = reactive({ id: null, title: '', content: '', todoType: 'APPROVAL', assignee: '', assigneeName: '', deptName: '', dueDate: '', relatedType: '' })

function handleTAdd() {
  tDialogTitle.value = '新建待办'
  Object.assign(tForm, { id: null, title: '', content: '', todoType: 'APPROVAL', assignee: '', assigneeName: '', deptName: '', dueDate: '', relatedType: '' })
  tDialogVisible.value = true
}
function handleTEdit(row) {
  tDialogTitle.value = '编辑待办'
  Object.assign(tForm, { ...row })
  tDialogVisible.value = true
}
async function handleTSave() {
  try {
    const res = tForm.id
      ? await updateTodoReminder(tForm.id, { ...tForm })
      : await createTodoReminder({ ...tForm })
    if (!checkOk(res, '保存')) return
    ElMessage.success('保存成功')
    tDialogVisible.value = false
    fetchTodos()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}
async function handleTComplete(row) {
  try {
    const res = await completeTodoReminder(row.id)
    if (!checkOk(res, '完成')) return
    ElMessage.success('待办已完成')
    fetchTodos()
  } catch (e) {
    ElMessage.error('完成失败：' + e.message)
  }
}
async function handleTOverdueScan() {
  try {
    const res = await scanOverdueTodos()
    if (!checkOk(res, '逾期扫描')) return
    ElMessage.success('逾期扫描完成：' + (res.data.overdue || 0) + ' 条改为已逾期')
    fetchTodos()
  } catch (e) {
    ElMessage.error('逾期扫描失败：' + e.message)
  }
}
async function handleTDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该待办吗？', '提示', { type: 'warning' })
    const res = await deleteTodoReminder(row.id)
    if (!checkOk(res, '删除')) return
    ElMessage.success('删除成功')
    fetchTodos()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + (e.message || e))
  }
}
async function handleTExport() {
  try {
    const res = await exportTodoReminders({ ...tQuery })
    downloadBlob(res, '待办提醒.xlsx')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}
function handleTImportSuccess(res) {
  if (!checkOk(res, '导入')) return
  ElMessage.success('导入成功：新增 ' + (res.data.created || 0) + ' 条，跳过 ' + (res.data.skipped || 0) + ' 条')
  fetchTodos()
}
async function handleTDownloadTemplate() {
  try {
    const res = await downloadTodoReminderTemplate()
    downloadBlob(res, '待办提醒导入模板.xlsx')
  } catch (e) {
    ElMessage.error('下载模板失败：' + e.message)
  }
}

const gDialogVisible = ref(false)
const gForm = reactive({ settingKey: '', title: '', content: '', todoType: 'APPROVAL', assignee: '', assigneeName: '', deptName: '', dueDate: '' })
function handleTGenerate() {
  Object.assign(gForm, { settingKey: '', title: '', content: '', todoType: 'APPROVAL', assignee: '', assigneeName: '', deptName: '', dueDate: '' })
  gDialogVisible.value = true
}
async function handleGSave() {
  try {
    const res = await generateTodoFromSetting({ ...gForm })
    if (!checkOk(res, '生成')) return
    ElMessage.success('已按提醒设置生成待办')
    gDialogVisible.value = false
    fetchTodos()
  } catch (e) {
    ElMessage.error('生成失败：' + e.message)
  }
}

// ---------- 消息推送 ----------
const pData = ref([])
const pTotal = ref(0)
const pLoading = ref(false)
const pQuery = reactive({ keyword: '', channel: '', sendStatus: '', page: 1, size: 10 })

async function fetchPushRecords() {
  pLoading.value = true
  try {
    const res = await getPushRecords({ ...pQuery })
    pData.value = res.data.records || []
    pTotal.value = res.data.total || 0
    fetchOverview()
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    pLoading.value = false
  }
}
function handlePSearch() { pQuery.page = 1; fetchPushRecords() }
function handlePReset() {
  Object.assign(pQuery, { keyword: '', channel: '', sendStatus: '', page: 1, size: 10 })
  fetchPushRecords()
}

const pDialogVisible = ref(false)
const pForm = reactive({ title: '', content: '', channel: 'WECHAT', receiverScope: 'ALL', receiverValue: '', receiverName: '' })
function handlePAdd() {
  Object.assign(pForm, { title: '', content: '', channel: 'WECHAT', receiverScope: 'ALL', receiverValue: '', receiverName: '' })
  pDialogVisible.value = true
}
async function handlePSave() {
  try {
    const res = await sendPushRecord({ ...pForm })
    if (!checkOk(res, '发送')) return
    if (res.data && res.data.success === false) {
      ElMessage.warning('推送未发出（预留占位）：' + (res.data.message || '通道未对接'))
    } else {
      ElMessage.success('推送成功')
    }
    pDialogVisible.value = false
    fetchPushRecords()
  } catch (e) {
    ElMessage.error('发送失败：' + e.message)
  }
}
async function handlePRetry(row) {
  try {
    const res = await retryPushRecord(row.id)
    if (!checkOk(res, '重推')) return
    if (res.data && res.data.success === false) {
      ElMessage.warning('重推未发出（预留占位）：' + (res.data.message || '通道未对接'))
    } else {
      ElMessage.success('重推成功')
    }
    fetchPushRecords()
  } catch (e) {
    ElMessage.error('重推失败：' + e.message)
  }
}
async function handlePDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该推送记录吗？', '提示', { type: 'warning' })
    const res = await deletePushRecord(row.id)
    if (!checkOk(res, '删除')) return
    ElMessage.success('删除成功')
    fetchPushRecords()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + (e.message || e))
  }
}
async function handlePExport() {
  try {
    const res = await exportPushRecords({ ...pQuery })
    downloadBlob(res, '消息推送记录.xlsx')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

// ---------- 短信提醒 ----------
const sData = ref([])
const sTotal = ref(0)
const sLoading = ref(false)
const sQuery = reactive({ keyword: '', sendStatus: '', page: 1, size: 10 })

async function fetchSmsRecords() {
  sLoading.value = true
  try {
    const res = await getSmsRecords({ ...sQuery })
    sData.value = res.data.records || []
    sTotal.value = res.data.total || 0
    fetchOverview()
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    sLoading.value = false
  }
}
function handleSSearch() { sQuery.page = 1; fetchSmsRecords() }
function handleSReset() {
  Object.assign(sQuery, { keyword: '', sendStatus: '', page: 1, size: 10 })
  fetchSmsRecords()
}

const sDialogVisible = ref(false)
const sForm = reactive({ phone: '', content: '', receiverName: '', templateCode: '', relatedType: '' })
function handleSAdd() {
  Object.assign(sForm, { phone: '', content: '', receiverName: '', templateCode: '', relatedType: '' })
  sDialogVisible.value = true
}
async function handleSSave() {
  try {
    const res = await sendSmsRecord({ ...sForm })
    if (!checkOk(res, '发送')) return
    if (res.data && res.data.success === false) {
      ElMessage.warning('短信未发出（预留占位）：' + (res.data.message || '网关未对接'))
    } else {
      ElMessage.success('短信发送成功')
    }
    sDialogVisible.value = false
    fetchSmsRecords()
  } catch (e) {
    ElMessage.error('发送失败：' + e.message)
  }
}
async function handleSRetry(row) {
  try {
    const res = await retrySmsRecord(row.id)
    if (!checkOk(res, '重发')) return
    if (res.data && res.data.success === false) {
      ElMessage.warning('重发未发出（预留占位）：' + (res.data.message || '网关未对接'))
    } else {
      ElMessage.success('重发成功')
    }
    fetchSmsRecords()
  } catch (e) {
    ElMessage.error('重发失败：' + e.message)
  }
}
async function handleSDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该短信记录吗？', '提示', { type: 'warning' })
    const res = await deleteSmsRecord(row.id)
    if (!checkOk(res, '删除')) return
    ElMessage.success('删除成功')
    fetchSmsRecords()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + (e.message || e))
  }
}
async function handleSExport() {
  try {
    const res = await exportSmsRecords({ ...sQuery })
    downloadBlob(res, '短信提醒记录.xlsx')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}
</script>
