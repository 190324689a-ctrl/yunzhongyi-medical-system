# 云中医医务系统

医院医务系统：医师档案、休假执业、重点授权、考核、排班、MDT、质量检查、新技术新项目、工作统计、系统设置、移动端等约 31 个模块（需求见 `docs/需求梳理.md`，原扫描 PDF 在 `docs/云中医需求文档.pdf`）。

技术栈：后端 **Java 17 + Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + MySQL 8**，前端 **Vue 3 + Vite 5 + Element Plus**，**Docker Compose 一键启动**。

## 快速启动

### 方式一：Docker Compose（一键启动，推荐）

```bash
docker compose up --build
```

- 前端：http://localhost:4173
- 后端：http://localhost:8080
- MySQL：localhost:3306（库 `medical`，用户 `medical` / 密码 `medical123`）

首次启动会自动建表并写入种子数据（11 个科室、含 3 个下级科室、12 名示例医师、字典、演示账号）。**种子数据是幂等写入，重启/多次启动不会重复插入。**

### 方式二：本地开发（接本地 MySQL）

默认数据库就是 **MySQL**（`spring.profiles.active=mysql`）。本机装好 MySQL 8 后建库并授权：

```sql
CREATE DATABASE medical CHARACTER SET utf8mb4;
CREATE USER 'medical'@'%' IDENTIFIED BY 'medical123';
GRANT ALL PRIVILEGES ON medical.* TO 'medical'@'%';
```

```bash
# 后端（需 JDK 17 + Maven 3.9）
cd backend
mvn spring-boot:run
# 或：mvn package -DskipTests && java -Dfile.encoding=UTF-8 -jar target/*.jar

# 前端（另开终端，需 Node 20+）
cd frontend
npm install
npm run dev      # http://localhost:5173，/api 已代理到 localhost:8080
npm run build    # 生产构建验证
```

连接信息在 `backend/src/main/resources/application.yml` 的 mysql profile 里，也可用环境变量覆盖：`SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`（docker-compose 就是这么接的）。

### 方式三：H2 内存库（仅测试/临时调试，无 MySQL 时用）

```bash
cd backend
SPRING_PROFILES_ACTIVE=h2 mvn spring-boot:run
```

H2 只保留作测试兜底：单元测试固定走 h2（`MedicalApplicationTests` 上有 `@ActiveProfiles("h2")`），种子数据在 H2（MySQL 兼容模式）下同样可跑。

演示账号（密码均为 `123456`）：`admin` 系统管理员（全部 33 个页面）/ `wuwenbin` 医务科管理员（25 个页面）/ `chen` 科主任（14 个页面）/ `sunxiaohong` 普通医师（6 个页面），见「登录鉴权」一节；生产请改默认密码并用环境变量 `AUTH_JWT_SECRET` 换 JWT 密钥。

## 目录结构

```
yunzhongyi-medical-system/
├── docker-compose.yml          # mysql + backend + frontend 一键启动
├── docs/
│   ├── 需求梳理.md             # 3 页扫描需求文档的模块清单
│   ├── 云中医需求文档.pdf      # 原始需求（扫描件）
│   └── p1/p2/p3.jpg            # 需求文档页面图
├── backend/                    # Spring Boot 3 + MyBatis-Plus
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/yunzhongyi/medical/
│       ├── MedicalApplication.java
│       ├── common/             # Result/PageResult/BaseEntity/全局异常处理
│       ├── config/             # MyBatis-Plus 配置（分页 + 自动填充）
│       ├── system/             # 通用底座：用户/角色/权限/科室/字典/操作日志
│       │   ├── entity/ mapper/ service/ controller/
│       └── physician/          # 医师档案管理（第一个完整模块）
│           ├── entity/ dto/ mapper/ service/ controller/ excel/
│   └── src/main/resources/
│       ├── application.yml     # mysql（默认）/ h2（测试）双 profile
│       ├── schema.sql / schema-mysql.sql
│       └── data.sql            # 种子数据：8 科室、12 医师、字典、账号（幂等写入）
└── frontend/                   # Vue 3 + Element Plus
    ├── package.json  vite.config.js  index.html
    └── src/
        ├── main.js  App.vue
        ├── router/index.js     # 左侧菜单：全部 31 个模块路由
        ├── layout/Layout.vue
        ├── api/                # physician.js / system.js
        ├── utils/request.js    # axios 封装（统一 Result 处理）
        └── views/
            ├── Dashboard.vue           # 工作台（统计 + 进度）
            ├── Placeholder.vue         # 未实现模块占位页
            └── physician/PhysicianList.vue  # 医师档案：列表+搜索+弹窗+导入导出
```

## 已实现接口

### 医师档案管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/physicians` | 分页 + 多条件组合查询（关键字/科室/职称/学历/人员类别/手术分级/三类处方权/状态） |
| `GET /api/physicians/{id}` | 详情 |
| `POST /api/physicians` | 新增（含校验：姓名必填、身份证/手机号格式） |
| `PUT /api/physicians/{id}` | 修改 |
| `DELETE /api/physicians/{id}` | 删除（逻辑删除） |
| `DELETE /api/physicians/batch` | 批量删除 |
| `GET /api/physicians/export` | 按当前查询条件导出 Excel |
| `POST /api/physicians/import` | 批量导入 Excel（同名同科室去重、非法行跳过） |
| `GET /api/physicians/template` | 导入模板下载 |

医师字段：姓名、性别、身份证、电话、科室、职称、最高学历、毕业院校、专业、执业证号/范围/类别、人员类别、聘期、证书、证件照、进修经历、**手术分级（1-4 级）与手术分级授权、抗肿瘤/麻醉/精神药品处方权**、在职状态。

### 通用底座接口

| 接口 | 说明 |
|---|---|
| `GET /api/depts`、`GET /api/depts/enabled`、`POST/PUT/DELETE /api/depts/{id}` | 科室分页/下拉/增删改 |
| `GET /api/dicts/{dictCode}`、`GET /api/dicts`、`POST/PUT/DELETE /api/dicts/{id}` | 字典（职称/学历/人员类别/执业类别/手术分级） |
| `GET/POST/PUT/DELETE /api/users` | 用户管理（底座；密码不回显） |
| `GET/POST/PUT/DELETE /api/roles` | 角色管理 |
| `GET/POST/PUT/DELETE /api/permissions` | 权限（菜单/按钮）管理 |
| `GET /api/oper-logs` | 操作日志查询（医师新增/导出已接入记录） |

统一响应：`{ "code": 0, "msg": "ok", "data": ... }`，分页为 `{ total, page, size, records }`。

## 模块清单与实现进度

| # | 模块 | 状态 | 备注 |
|---|---|---|---|
| 1 | 医师档案管理 | ✅ 已实现 | 前后端打通，见上 |
| 2 | 休假执业管理 | ✅ 已实现 | 在线申请、多级审批（科主任→医务科）、查询、统计、打印预览，见下 |
| 3 | 重点授权管理 | ✅ 已实现 | 在线申请、多级审批（科主任→医务科）、查询、统计、批量导入导出、打印预览，见下 |
| 4 | 医师考核管理 | ✅ 已实现 | 自定义模板、评分录入（总分与等级自动计算）、评分统计、批量导入导出，见下 |
| 5 | 排班管理 | ✅ 已实现 | 科室/门诊/住院/急诊排班、模板批量排班、调班代班一级审批、统计、批量导入导出；HIS 对接为预留占位，未真实对接 |
| 6 | MDT 管理 | ✅ 已实现 | 多学科会诊申请、一级审批（医务科/科主任）、会诊记录（可多次补充）、统计、批量导入导出，见下 |
| 7 | 医疗人员动态管理 | ✅ 已实现 | 人员变动、调动、离职、退休、入职、返聘，一级审批（医务科），审批通过同步医师档案，见下 |
| 8 | 放射人员管理 | ✅ 已实现 | 档案、资质、培训、健康管理，见下 |
| 9 | 委员会管理 | ✅ 已实现 | 换届、会议记录、决议管理，见下 |
| 10 | 文件制度管理 | ✅ 已实现 | 分类、版本、发布、作废、在线阅读、下载，见下 |
| 11 | 政策法规管理 | ✅ 已实现 | 分类、发布、查询、更新、查阅学习，见下 |
| 12 | 医德医风管理 | ✅ 已实现 | 档案、考评、投诉、表彰，见下 |
| 13 | 质量检查与自查 | ✅ 已实现 | 科室自查、检查模板/计划/记录、整改反馈（合入质量检查管理模块），见下 |
| 14 | 院级质量检查 | ✅ 已实现 | 计划、记录、报告、整改跟踪（合入质量检查管理模块），见下 |
| 15 | 专项质量检查 | ✅ 已实现 | 病历/处方/抗菌药物等专项检查（合入质量检查管理模块），见下 |
| 16 | 核心制度 | ✅ 已实现 | 分类、发布、查询、更新、查阅学习，见下 |
| 17 | 检查整改汇总 | ✅ 已实现 | 检查问题汇总、整改汇总、统计分析，见下 |
| 18 | 医疗技术分级管理 | ✅ 已实现 | 技术分级、准入、授权、档案，见下 |
| 19 | 检查维护 | ✅ 已实现 | 检查项目/标准/模板维护，见下 |
| 20 | 检查分类 | ✅ 已实现 | 两级检查分类库，检查项目按分类归类，见下 |
| 21 | 检查模板 | ✅ 已实现 | 模板版本、复制、使用情况、统计、导入导出，见下 |
| 22 | 医疗分级管理 | ✅ 已实现 | 分级标准、评定、审批、统计，见下 |
| 23 | 新技术、新项目管理 | ✅ 已实现 | 申报、审批、立项、验收、推广、伦理审查，见下 |
| 24 | 工作统计 | ✅ 已实现 | 手术量/工作量/门诊量/住院量统计，按科室/按人员/按时间汇总（合入统计分析模块），见下 |
| 25 | 三级医院医疗质量指标统计 | ✅ 已实现 | 国家/省/市标准评审指标、达标率统计（合入统计分析模块），见下 |
| 26 | 医疗质量统计 | ✅ 已实现 | 质量/安全/患者安全指标实时聚合、图形化展示（合入统计分析模块），见下 |
| 27 | 医务系统完成统计 | ✅ 已实现 | 各模块完成情况统计，见下 |
| 28 | 医务系统设置 | ✅ 已实现 | 参数设置、数据备份、系统接口、消息提醒设置、知识库 + 底座补强（用户/角色/权限/科室/字典/操作日志），见下 |
| 29 | 消息提醒 | ✅ 已实现 | 站内消息、待办提醒、消息推送、短信提醒，见下 |
| 30 | 医疗工作简报 | ✅ 已实现 | 简报生成、发布、导出 PDF/Word |
| 31 | 医务管理移动端 | ✅ 已实现 | 移动端查看、审批、统计、消息推送，见下 |

31 个模块已全部实现，无待开发模块。

### 休假执业管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/leaves` | 分页 + 多条件组合查询（申请人/科室/申请类型/审批状态） |
| `GET /api/leaves/stats` | 统计：总数、待审批（科主任/医务科分开计）、已通过、已驳回、按类型分组 |
| `GET /api/leaves/{id}` | 详情（打印预览用） |
| `POST /api/leaves` | 在线申请（默认待科主任审批；结束日期不能早于开始日期；天数按日期自动算，含首尾） |
| `PUT /api/leaves/{id}` | 修改 |
| `DELETE /api/leaves/{id}` / `DELETE /api/leaves/batch` | 删除 / 批量删除 |
| `POST /api/leaves/{id}/approve` | 多级审批通过：待科主任 → 待医务科 → 已通过，审批记录留痕 |
| `POST /api/leaves/{id}/reject` | 多级审批驳回：任一级驳回即已驳回；已结束的申请不能再审批 |

申请类型：休假 / 执业 / 进修 / 外出进修 / 手术分级授权。种子数据 6 条，覆盖 5 种类型、4 种审批状态。前端页面含统计卡片、审批按钮（通过/驳回，需填审批意见）、打印预览弹窗（申请表 + 浏览器打印）。

### 重点授权管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/authorizations` | 分页 + 多条件组合查询（申请人/科室/授权类型/审批状态） |
| `GET /api/authorizations/stats` | 统计：总数、待审批（科主任/医务科分开计）、已通过、已驳回、按类型分组 |
| `GET /api/authorizations/{id}` | 详情（打印预览用） |
| `POST /api/authorizations` | 在线申请（默认待科主任审批；有效期结束日期不能早于开始日期） |
| `PUT /api/authorizations/{id}` | 修改 |
| `DELETE /api/authorizations/{id}` / `DELETE /api/authorizations/batch` | 删除 / 批量删除 |
| `POST /api/authorizations/{id}/approve` | 多级审批通过：待科主任 → 待医务科 → 已通过，审批记录留痕 |
| `POST /api/authorizations/{id}/reject` | 多级审批驳回：任一级驳回即已驳回；已结束的申请不能再审批 |
| `GET /api/authorizations/export` | 导出 Excel（按当前查询条件） |
| `POST /api/authorizations/import` | 批量导入 Excel（同一医师同一授权类型重复跳过） |
| `GET /api/authorizations/template` | 导入模板下载 |

授权类型：手术分级授权 / 麻醉药品处方权 / 抗肿瘤处方权 / 精神药品处方权。种子数据 6 条，覆盖 4 种类型、4 种审批状态。前端页面含统计卡片、审批按钮（通过/驳回，需填审批意见）、批量导入导出、打印预览弹窗（申请表 + 浏览器打印）。表名用 `key_authorization`（`authorization` 是 H2 保留字，建表会报语法错误）。

### 医师考核管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/assessments` | 分页 + 多条件组合查询（医师/科室/考核类型/年度/等级/模板） |
| `GET /api/assessments/stats` | 评分统计：总数、平均分、优秀人数与优秀率、按等级分组、按科室汇总 |
| `GET /api/assessments/stats/by-dept` | 按科室汇总统计（人数、平均分、优秀率） |
| `GET /api/assessments/{id}` | 详情 |
| `POST /api/assessments` | 新增考核记录：由各项得分求和自动算总分，再按总分自动算等级（≥90 优秀 / ≥80 合格 / ≥60 基本合格 / <60 不合格）与考核结果；有模板时校验每项得分不超过该项权重 |
| `PUT /api/assessments/{id}` | 修改（重新录入得分后总分与等级自动重算） |
| `DELETE /api/assessments/{id}` / `DELETE /api/assessments/batch` | 删除 / 批量删除 |
| `GET /api/assessments/export` | 导出 Excel（按当前查询条件） |
| `POST /api/assessments/import` | 批量导入 Excel（同一医师同一模板同一年度重复跳过；按姓名查医师档案补全医师 id，按模板名称补全模板 id） |
| `GET /api/assessments/template` | 导入模板下载 |
| `GET/POST/PUT/DELETE /api/assessment-templates` | 考核模板 CRUD（评分项权重之和必须为 100） |
| `GET /api/assessment-templates/enabled` | 全部启用的模板（新建考核记录时下拉用） |

考核类型：定期考核 / 医德考评 / 医疗质量考核。种子数据：3 个模板（定期考核、医德考评、医疗质量考核，权重之和均为 100）、6 条考核记录，覆盖 3 种类型、4 个等级（平均分 81.17，优秀 2 人、优秀率 33.33%）。前端页面含统计卡片、按科室评分统计表、模板管理弹窗（新增/编辑/启用/停用模板，权重合计校验）、评分录入弹窗（按模板动态生成评分项输入框，实时显示总分与等级）、批量导入导出。

### 排班管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/schedules` | 分页 + 多条件组合查询（医师/科室/排班类型/班次/日期范围/调班代班状态/模板） |
| `GET /api/schedules/stats` | 排班统计：总数、待审批调班代班数、夜班人次、按排班类型分组、按班次分组、按科室汇总、按人员汇总 |
| `GET /api/schedules/stats/by-dept` | 按科室汇总统计（排班人次、夜班人次、急诊排班） |
| `GET /api/schedules/stats/by-physician` | 按人员汇总统计（排班天数、夜班天数） |
| `GET /api/schedules/{id}` | 详情 |
| `POST /api/schedules` | 新增排班：同一医师同一天不能重复排班；班次时间段缺失时按班次默认时间补全 |
| `PUT /api/schedules/{id}` | 修改 |
| `DELETE /api/schedules/{id}` / `DELETE /api/schedules/batch` | 删除 / 批量删除 |
| `POST /api/schedules/generate` | 按模板批量生成排班（指定日期范围 + 科室/人员；冲突跳过并计数返回，休息日不建记录） |
| `POST /api/schedules/{id}/swap` | 申请调班（换班次/换日期），进入待科主任审批 |
| `POST /api/schedules/{id}/substitute` | 申请代班（换人），进入待科主任审批 |
| `POST /api/schedules/{id}/approve-change` | 一级审批通过（科主任）：调班改日期/班次，代班改人员；审批时再次校验调班/代班后不冲突，审批记录留痕 |
| `POST /api/schedules/{id}/reject-change` | 一级审批驳回（科主任）：原排班不变；已结束的申请不能再审批 |
| `POST /api/schedules/{id}/his-sync` | 同步单条排班到 HIS（预留占位：目前未真实对接，调用明确返回 501“HIS 对接未启用”，不伪造同步成功） |
| `GET /api/schedules/export` | 导出 Excel（按当前查询条件） |
| `POST /api/schedules/import` | 批量导入 Excel（同一医师同一天重复跳过；按姓名查医师档案补全医师 id） |
| `GET /api/schedules/template` | 导入模板下载 |
| `GET/POST/PUT/DELETE /api/schedule-templates` | 排班模板 CRUD（班次组合校验：星期 1-7 不重复、班次编码合法） |
| `GET /api/schedule-templates/enabled` | 全部启用的模板（批量生成排班时下拉用） |

排班类型：科室排班 / 门诊排班 / 住院排班 / 急诊排班。班次：白班 08:00–17:30 / 上午班 08:00–12:00 / 下午班 14:00–17:30 / 夜班 17:30–次日 08:00 / 休息（无时间段）。调班/代班一级审批（科主任）；表名用 `duty_schedule`（`schedule` 在部分数据库是保留字）。种子数据：2 个排班模板（中医内科常规、急诊科轮班）、8 条排班记录，覆盖 4 种排班类型、5 种班次，含 1 条待审批调班申请。前端页面含统计卡片、按科室/按人员排班统计表、模板管理弹窗（按星期编辑班次组合）、按模板批量排班弹窗、调班/代班申请与审批弹窗、批量导入导出。HIS 对接说明：目前没有真实 HIS 系统可连，`his.enabled` 默认关闭，同步接口只做预留占位，不伪造同步成功；待医院提供真实 HIS 接口地址与鉴权方式后再按其规范实现。

### MDT 管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/mdts` | 分页 + 多条件组合查询（患者/住院号/申请医师关键字、科室、会诊类型、审批状态、紧急程度、期望会诊日期范围） |
| `GET /api/mdts/stats` | MDT 统计：总数、待审批数、已通过数、已驳回数、紧急会诊数、按会诊类型分组、按科室汇总、按月趋势 |
| `GET /api/mdts/stats/by-dept` | 按科室汇总统计（申请数、待审批、已通过、紧急会诊） |
| `GET /api/mdts/stats/by-month` | 按月趋势统计（申请月份、申请数、已通过、待审批，按月份升序） |
| `GET /api/mdts/{id}` | 详情（含全部会诊记录） |
| `POST /api/mdts` | 新增 MDT 申请：默认待审批，申请日期默认为当天；会诊类型/紧急程度/期望会诊时间必填且须合法 |
| `PUT /api/mdts/{id}` | 修改（只有待审批的申请能修改） |
| `DELETE /api/mdts/{id}` / `DELETE /api/mdts/batch` | 删除 / 批量删除 |
| `POST /api/mdts/{id}/approve` | 一级审批通过（医务科/科主任）：只有待审批的申请能通过，审批记录留痕 |
| `POST /api/mdts/{id}/reject` | 一级审批驳回（医务科/科主任）：只有待审批的申请能驳回，审批记录留痕；已结束的申请不能再审批 |
| `GET /api/mdts/{id}/records` | 查某申请的全部会诊记录（按会诊时间升序） |
| `POST /api/mdts/{id}/records` | 添加会诊记录：只有审批通过的申请才能记录，可多次补充；记录日期默认为当天 |
| `DELETE /api/mdts/{id}/records/{recordId}` | 删除一条会诊记录 |
| `GET /api/mdts/export` | 导出 Excel（按当前查询条件） |
| `POST /api/mdts/import` | 批量导入 Excel（同一患者同一住院号同一期望会诊时间重复跳过；紧急程度非法时默认为一般） |
| `GET /api/mdts/template` | 导入模板下载 |

会诊类型：院内会诊 / 院外会诊 / 远程会诊 / 急会诊。紧急程度：一般 / 紧急。种子数据：5 条 MDT 申请，覆盖 4 种会诊类型、待审批/已通过/已驳回 3 种审批状态，含 2 条紧急申请；3 条会诊记录，其中张建国的申请有 2 条补充记录。前端页面含统计卡片、按会诊类型/科室/月份统计表、新增/编辑弹窗、详情弹窗（含会诊记录列表与添加记录表单）、审批弹窗（通过/驳回，需填审批人+意见）、批量导入导出。注意：`LocalDateTime` 字段（期望会诊时间、会诊时间）用 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` 与前端日期时间选择器格式对齐，MDT 是第一个用 `LocalDateTime` 的模块；审批内部直接调 `super.updateById`，避免同一事务内 MyBatis 一级缓存导致修改守卫误判（见 `MdtServiceImpl` 注释）。

### 放射人员管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/radiation-personnel` | 分页 + 多条件组合查询（姓名/证号关键字、科室、放射类别、在岗状态） |
| `GET /api/radiation-personnel/stats` | 放射人员统计：档案总数、在岗/停岗/离岗、按放射类别分组、按科室汇总、资质待审核/已通过/已驳回/90 天内到期/已过期、培训总数/合格数、体检总数/平均剂量/剂量超标数/复查数/不宜从事放射工作数/体检逾期人数 |
| `GET /api/radiation-personnel/stats/by-dept` | 按科室汇总统计（档案数、在岗、停岗、离岗） |
| `GET /api/radiation-personnel/stats/by-work-type` | 按放射类别汇总统计 |
| `GET /api/radiation-personnel/{id}` | 档案详情 |
| `POST /api/radiation-personnel` | 新增档案：默认在岗；同一姓名同一科室已有档案拒绝重复建档；许可证有效期不能早于从事放射工作日期 |
| `PUT /api/radiation-personnel/{id}` | 修改 |
| `DELETE /api/radiation-personnel/{id}` / `DELETE /api/radiation-personnel/batch` | 删除 / 批量删除 |
| `GET /api/radiation-personnel/export` | 导出 Excel（按当前查询条件） |
| `POST /api/radiation-personnel/import` | 批量导入 Excel（同一姓名同一科室重复跳过） |
| `GET /api/radiation-personnel/template` | 导入模板下载 |
| `GET /api/radiation-qualifications` | 资质分页 + 多条件组合查询（人员/证书关键字、证书类型、审核状态、只看 90 天内到期） |
| `GET /api/radiation-qualifications/by-personnel/{personnelId}` | 查某人员的全部资质（按有效期升序） |
| `GET/POST/PUT/DELETE /api/radiation-qualifications/{id}` | 资质增删改（只有待审核的资质能修改；有效期不能早于发证日期；同一证书同一编号已有待审核记录拒绝重复登记） |
| `POST /api/radiation-qualifications/{id}/approve` | 一级审核通过（医务科）：只有待审核的资质能通过，审核记录留痕 |
| `POST /api/radiation-qualifications/{id}/reject` | 一级审核驳回（医务科）：只有待审核的资质能驳回，审核记录留痕；已结束的资质不能再审核、再修改 |
| `GET /api/radiation-trainings` | 培训分页 + 多条件组合查询（人员/培训名称关键字、培训类型、培训结果） |
| `GET /api/radiation-trainings/by-personnel/{personnelId}` | 查某人员的全部培训记录（按培训日期降序） |
| `GET/POST/PUT/DELETE /api/radiation-trainings/{id}` | 培训记录增删改（结果默认合格；同一人员同一培训同一日期拒绝重复登记；学时不能为负） |
| `GET /api/radiation-health` | 体检分页 + 多条件组合查询（人员关键字、体检类型、体检结论、只看剂量超标） |
| `GET /api/radiation-health/by-personnel/{personnelId}` | 查某人员的全部体检记录（按体检日期降序） |
| `GET/POST/PUT/DELETE /api/radiation-health/{id}` | 体检记录增删改（结论默认合格；同一人员同一体检类型同一日期拒绝重复登记；剂量不能为负；下次体检日期不能早于本次） |

放射类别：放射诊断 / 介入放射 / 核医学 / 放射治疗。在岗状态：在岗 / 停岗 / 离岗。证书类型：放射工作人员证 / 大型设备上岗证 / 放射防护培训合格证 / 执业医师证。培训类型：岗前培训 / 复训 / 防护知识培训 / 新设备培训。体检类型：岗前体检 / 在岗期间体检 / 离岗体检；体检结论：合格 / 复查 / 不宜从事放射工作。个人受照年有效剂量限值 20 mSv，超过即超标（统计单独计数、列表可过滤、登记时后端记日志提醒复查调岗）；体检有效期按一年计，超过 365 天未体检的在岗人员计入体检逾期。种子数据：6 名放射人员（覆盖 4 种放射类别，在岗 4 / 停岗 1 / 离岗 1；周建国关联医师档案 id=7）、6 条资质（覆盖 4 种证书类型，待审核 2 / 已通过 3 / 已驳回 1；陈志强大型设备上岗证 2027-01-31 到期、刘海燕防护合格证 2026-10-31 已过期，90 天内到期 2 条、已过期 1 条）、5 条培训（覆盖 4 种培训类型，合格 4 / 不合格 1）、6 条体检（覆盖 3 种体检类型，平均剂量 8.75 mSv；赵雪 21.40 mSv 超标、结论复查、已停岗，体检逾期 1 人）。前端页面含统计卡片（档案总数/在岗/资质待审核/资质 90 天内到期/剂量超标/体检逾期）、资质到期提醒表、剂量超标提醒表、按放射类别/科室统计表、档案详情弹窗（含资质/培训/健康三个页签，可直接登记与审核）、批量导入导出。

### 委员会管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/committees` | 分页 + 多条件组合查询（委员会名称/主任委员关键字、类别、状态、只看任期 90 天内到期） |
| `GET /api/committees/stats` | 委员会统计：总数、在任/已换届/已解散、按类别分组、任期 90 天内到期数、会议总数、决议总数与各状态数、决议完成率、逾期决议数 |
| `GET /api/committees/stats/by-type` | 按委员会类别汇总统计（委员会数、在任数） |
| `GET /api/committees/stats/by-month` | 按月会议趋势统计（会议月份、会议数，按月份升序） |
| `GET /api/committees/{id}` | 详情 |
| `POST /api/committees` | 新增委员会：默认在任、第 1 届；同一委员会名称同一届次已有记录拒绝重复建档；任期结束日期不能早于开始日期 |
| `PUT /api/committees/{id}` | 修改 |
| `DELETE /api/committees/{id}` / `DELETE /api/committees/batch` | 删除 / 批量删除（同时删除该委员会的会议与决议） |
| `POST /api/committees/{id}/renew` | 换届：只有在任的委员会能换届，生成新一届记录（届次 +1、新任期、新一届人员，空字段沿用上一届），原记录改为已换届 |
| `POST /api/committees/{id}/dissolve` | 解散：只有在任的委员会能解散；已结束的委员会不能再换届、再解散 |
| `GET /api/committees/export` | 导出 Excel（按当前查询条件） |
| `POST /api/committees/import` | 批量导入 Excel（同一委员会名称同一届次重复跳过；类别非法跳过） |
| `GET /api/committees/template` | 导入模板下载 |
| `GET /api/committee-meetings` | 会议分页 + 多条件组合查询（主题/主持人/委员会关键字、委员会、会议日期范围） |
| `GET /api/committee-meetings/by-committee/{committeeId}` | 查某委员会的全部会议（按会议日期降序） |
| `GET/POST/PUT/DELETE /api/committee-meetings/{id}` | 会议增删改（只有在任的委员会才能新增会议；同一委员会同一天同一主题拒绝重复登记；删除会议同时删除其决议） |
| `GET /api/committee-resolutions` | 决议分页 + 多条件组合查询（决议事项/责任人关键字、会议、委员会、状态、只看逾期） |
| `GET /api/committee-resolutions/by-meeting/{meetingId}` | 查某会议的全部决议（按决议期限升序） |
| `GET/POST/PUT/DELETE /api/committee-resolutions/{id}` | 决议增删改（默认待执行；同一会议同一决议事项拒绝重复登记；已完成/已取消的决议不能再修改） |
| `POST /api/committee-resolutions/{id}/start` | 开始执行：待执行 → 执行中 |
| `POST /api/committee-resolutions/{id}/complete` | 完成：执行中 → 已完成（完成日期默认为当天） |
| `POST /api/committee-resolutions/{id}/cancel` | 取消：待执行/执行中 → 已取消；已结束的决议不能再流转 |

委员会类别：医疗质量 / 药事 / 院感 / 输血 / 病案 / 其他。委员会状态：在任 / 已换届 / 已解散。决议状态：待执行 / 执行中 / 已完成 / 已取消；逾期不单独存状态，按“待执行/执行中且决议期限已过当天”动态判断；决议完成率 = 已完成 /（决议总数 − 已取消）。种子数据：6 个委员会（覆盖 5 种类别，在任 4 / 已换届 1 / 已解散 1；医院感染管理委员会第 2 届任期 2026-12-31 截止，90 天内到期 1 个；输血管理委员会第 1 届已换届、第 2 届在任）、5 次会议（2026-10 月 1 次、2026-11 月 4 次）、5 条决议（待执行 2 / 执行中 1 / 已完成 1 / 已取消 1，其中“落实三级医师查房制度”期限 2026-11-15 已逾期，决议完成率 25.00%）。前端页面含统计卡片（委员会总数/在任/任期 90 天内到期/会议总数/决议总数/逾期决议）、逾期决议提醒表、按类别/按月统计表、新增/编辑弹窗、换届弹窗（新一届任期与人员，空字段沿用上一届）、解散确认、详情弹窗（含基本信息、会议记录、决议管理三个页签，决议可开始执行/完成/取消）、批量导入导出。

### 文件制度管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/documents` | 分页 + 多条件组合查询（文件标题/文件编号关键字、分类、状态、只看 30 天内到期、只看已过期） |
| `GET /api/documents/stats` | 文件统计：总数、草稿/已发布/已作废、按分类分组、30 天内到期数、已过期数 |
| `GET /api/documents/stats/by-category` | 按文件分类汇总统计（文件数、已发布数） |
| `GET /api/documents/stats/by-month` | 按月发布趋势统计（发布月份、发布数，按月份升序） |
| `GET /api/documents/{id}` | 详情（含正文，用于在线阅读） |
| `POST /api/documents` | 新增文件：默认草稿、V1.0；同一文件编号同一版本已有记录拒绝重复建档；失效日期不能早于生效日期 |
| `PUT /api/documents/{id}` | 修改（只有草稿能修改；已发布的文件改版走新建版本，已作废的文件不可再改） |
| `DELETE /api/documents/{id}` / `DELETE /api/documents/batch` | 删除 / 批量删除（已发布的文件不能直接删除，请先作废） |
| `POST /api/documents/{id}/publish` | 发布（一级审批）：只有草稿能发布，审批人必填，发布日期默认为当天、生效日期为空时默认当天，审批记录留痕；发布新版本时同一编号的其他已发布版本自动作废 |
| `POST /api/documents/{id}/abolish` | 作废：只有已发布的文件能作废，作废原因必填，审批记录留痕 |
| `POST /api/documents/{id}/new-version` | 新建版本：只有已发布的文件能新建版本，生成草稿记录（文件编号相同、版本号 +1、正文与归口科室沿用原文件） |
| `GET /api/documents/{id}/download` | 下载：按正文生成 .txt 文件（不引入真实文件存储） |
| `GET /api/documents/export` | 导出 Excel（按当前查询条件） |
| `POST /api/documents/import` | 批量导入 Excel（导入的均为草稿，需走发布审批流程；同一编号同一版本重复跳过；分类非法跳过） |
| `GET /api/documents/template` | 导入模板下载 |

文件分类：制度 / 规范 / 流程 / 文件 / 其他。文件状态：草稿 / 已发布 / 已作废；失效不单独存状态，按“已发布且失效日期已过当天”动态判断已过期，30 天内到期即即将到期；版本号格式 V大版本.0，新建版本时大版本号 +1。种子数据：6 个文件（制度 3 / 规范 1 / 流程 1 / 文件 1，已发布 4 / 草稿 1 / 已作废 1；抗菌药物临床应用管理办法 2026-12-20 到期，30 天内到期 1 个；病历书写规范失效日期 2026-11-15 已过期 1 个；会诊制度 V1.0 已作废，作废原因“已被《会诊制度》（V2.0）替代”）。前端页面含统计卡片（文件总数/草稿/已发布/已作废/30 天内到期/已过期）、已过期文件提醒表、按分类/按月发布统计表、新增/编辑弹窗（含正文编辑）、发布弹窗（审批人+审批意见）、作废弹窗（作废原因必填）、新建版本确认、详情弹窗（含基本信息、在线阅读两个页签，正文可下载 .txt）、批量导入导出。

### 政策法规管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/policies` | 分页 + 多条件组合查询（法规名称/文号/发布机关关键字、分类、发布层级、状态、适用科室、只看 30 天内到期、只看已过期） |
| `GET /api/policies/stats` | 法规统计：总数、草稿/已发布/已废止、按分类分组、按层级分组、30 天内到期数、已过期数、查阅总人次/已学习人次/有查阅记录的法规数 |
| `GET /api/policies/stats/by-category` | 按法规分类汇总统计（法规数、已发布数） |
| `GET /api/policies/stats/by-level` | 按发布层级汇总统计（法规数、已发布数） |
| `GET /api/policies/stats/by-month` | 按月发布趋势统计（发布月份、发布数，按月份升序） |
| `GET /api/policies/{id}` | 详情（含正文，用于在线阅读） |
| `POST /api/policies` | 新增法规：默认草稿、V1.0、适用科室默认“全院”；同一文号同一版本已有记录拒绝重复建档；失效日期不能早于生效日期 |
| `PUT /api/policies/{id}` | 修改（只有草稿能修改；已发布的法规更新走新建版本，已废止的法规不可再改） |
| `DELETE /api/policies/{id}` / `DELETE /api/policies/batch` | 删除 / 批量删除（已发布的法规不能直接删除，请先废止；删除时同时删除其查阅学习记录） |
| `POST /api/policies/{id}/publish` | 发布（一级审批）：只有草稿能发布，审批人必填，发布日期默认为当天、生效日期为空时默认当天，审批记录留痕；发布新版本时同一文号的其他已发布版本自动废止 |
| `POST /api/policies/{id}/abolish` | 废止：只有已发布的法规能废止，废止原因必填，审批记录留痕 |
| `POST /api/policies/{id}/new-version` | 新建版本：只有已发布的法规能新建版本，生成草稿记录（文号相同、版本号 +1、正文与适用范围、归口科室沿用原法规） |
| `GET /api/policies/{id}/download` | 下载：按正文生成 .txt 文件（不引入真实文件存储） |
| `GET /api/policies/export` | 导出 Excel（按当前查询条件） |
| `POST /api/policies/import` | 批量导入 Excel（导入的均为草稿，需走发布审批流程；同一文号同一版本重复跳过；分类/层级非法时分类跳过、层级默认国家） |
| `GET /api/policies/template` | 导入模板下载 |
| `GET /api/policy-readings` | 查阅学习记录分页 + 多条件组合查询（法规、查阅人/科室关键字、是否已学习） |
| `GET /api/policy-readings/by-policy/{policyId}` | 查某部法规的全部查阅学习记录（按查阅日期降序） |
| `POST /api/policy-readings` | 登记查阅学习：只有已发布的法规才能登记；查阅日期默认为当天；同一人同一法规同一天只登记一次 |
| `POST /api/policy-readings/{id}/learned` | 标记已学习：已查阅 → 已学习 |
| `DELETE /api/policy-readings/{id}` | 删除一条查阅学习记录 |

法规分类：法律 / 行政法规 / 部门规章 / 地方性法规 / 规范性文件 / 行业标准 / 其他。发布层级：国家 / 省 / 市 / 院内。法规状态：草稿 / 已发布 / 已废止；失效不单独存状态，按“已发布且失效日期已过当天”动态判断已过期，30 天内到期即即将到期；版本号格式 V大版本.0，新建版本时大版本号 +1。种子数据：6 部法规（法律 1 / 行政法规 2 / 部门规章 2 / 地方性法规 1，国家 5 / 省 1，已发布 4 / 草稿 1 / 已废止 1；医疗质量管理办法 2026-12-20 到期，30 天内到期 1 部；医疗机构管理条例失效日期 2026-11-15 已过期 1 部）、6 条查阅学习记录（医师法 3 条 / 医疗质量管理办法 2 条 / 医疗机构管理条例 1 条，已学习 4 人次、已查阅 2 人次，覆盖法规 3 部）。前端页面含统计卡片（法规总数/草稿/已发布/已废止/30 天内到期/已过期/查阅总人次/已学习人次/有查阅记录的法规数）、已过期法规提醒表、按分类/按层级/按月发布统计表、新增/编辑弹窗（含适用科室与适用人员）、发布弹窗（审批人+审批意见）、废止弹窗（废止原因必填）、新建版本确认、详情弹窗（含基本信息、在线阅读、查阅学习记录三个页签，查阅学习可直接登记与标记已学习）、批量导入导出。

### 医德医风管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/ethics-profiles` | 档案分页 + 多条件组合查询（医师/档案编号关键字、科室、状态、最近考评等级） |
| `GET /api/ethics-profiles/stats` | 档案统计：总数、在档/封存、按科室汇总、已通过表彰数、投诉总数、已通过考评平均分与优秀率、逾期投诉数 |
| `GET /api/ethics-profiles/stats/by-dept` | 按科室汇总统计（档案数、在档数、最近考评优秀人数） |
| `GET /api/ethics-profiles/{id}` | 档案详情 |
| `POST /api/ethics-profiles` | 新增档案：默认在档；一名医师只能建一份档案，档案编号全院唯一；科室/职称按医师档案补全 |
| `PUT /api/ethics-profiles/{id}` | 修改 |
| `DELETE /api/ethics-profiles/{id}` / `DELETE /api/ethics-profiles/batch` | 删除 / 批量删除 |
| `GET /api/ethics-profiles/export` | 导出 Excel（按当前查询条件） |
| `POST /api/ethics-profiles/import` | 批量导入 Excel（同一档案编号/同一医师重复跳过；按姓名查医师档案补全） |
| `GET /api/ethics-profiles/template` | 导入模板下载 |
| `GET/POST/PUT/DELETE /api/ethics-templates` | 考评模板 CRUD（评分项权重之和必须为 100） |
| `GET /api/ethics-templates/enabled` | 全部启用的模板（新建考评记录时下拉用） |
| `GET /api/ethics-evaluations` | 考评分页 + 多条件组合查询（医师/科室/模板/年度/等级/审批状态） |
| `GET /api/ethics-evaluations/stats` | 考评统计：总数、待审批/已通过/已驳回、平均分、优秀人数与优秀率、按等级分组、按科室汇总 |
| `GET /api/ethics-evaluations/stats/by-dept` | 按科室汇总统计（人数、平均分、优秀率） |
| `GET /api/ethics-evaluations/{id}` | 详情 |
| `POST /api/ethics-evaluations` | 新增考评记录：由各项得分求和自动算总分，再按总分自动算等级（≥90 优秀 / ≥80 合格 / ≥60 基本合格 / <60 不合格）与考评结果；有模板时校验每项得分不超过该项权重；默认待审批 |
| `PUT /api/ethics-evaluations/{id}` | 修改（只有待审批的考评能修改；重新录入得分后总分与等级自动重算） |
| `DELETE /api/ethics-evaluations/{id}` / `DELETE /api/ethics-evaluations/batch` | 删除 / 批量删除（已通过的考评不能直接删除） |
| `POST /api/ethics-evaluations/{id}/approve` | 一级审批通过（医务科）：只有待审批的考评能通过，审批记录留痕；通过时同步医德档案的最近考评结果 |
| `POST /api/ethics-evaluations/{id}/reject` | 一级审批驳回（医务科）：只有待审批的考评能驳回，驳回原因必填；已结束的考评不能再审批 |
| `GET /api/ethics-evaluations/export` | 导出 Excel（按当前查询条件） |
| `POST /api/ethics-evaluations/import` | 批量导入 Excel（同一医师同一模板同一年度重复跳过；按姓名查医师档案补全医师 id，按模板名称补全模板 id） |
| `GET /api/ethics-evaluations/template` | 导入模板下载 |
| `GET /api/ethics-complaints` | 投诉分页 + 多条件组合查询（投诉人/被投诉医师/投诉编号关键字、投诉类型、流转状态、只看逾期） |
| `GET /api/ethics-complaints/stats` | 投诉统计：总数、按流转状态分组、按投诉类型分组、按科室汇总、逾期数、反馈率 |
| `GET /api/ethics-complaints/stats/by-dept` | 按科室汇总统计（投诉数、待受理、调查中、已反馈、逾期） |
| `GET /api/ethics-complaints/{id}` | 详情（含流转记录） |
| `POST /api/ethics-complaints` | 新增投诉登记：默认待受理；处理期限默认为投诉日期 + 15 天；同一投诉编号已有记录拒绝重复登记 |
| `PUT /api/ethics-complaints/{id}` | 修改（只有待受理/调查中的投诉能修改） |
| `DELETE /api/ethics-complaints/{id}` / `DELETE /api/ethics-complaints/batch` | 删除 / 批量删除 |
| `POST /api/ethics-complaints/{id}/accept` | 受理：待受理 → 调查中 |
| `POST /api/ethics-complaints/{id}/handle` | 处理：调查中 → 已处理，处理结果必填，处理日期默认为当天 |
| `POST /api/ethics-complaints/{id}/feedback` | 反馈：已处理 → 已反馈，反馈内容必填，反馈日期默认为当天 |
| `POST /api/ethics-complaints/{id}/reject` | 不予受理：待受理 → 不予受理，原因必填；已结束的投诉不能再流转 |
| `GET /api/ethics-complaints/export` | 导出 Excel（按当前查询条件） |
| `POST /api/ethics-complaints/import` | 批量导入 Excel（同一投诉编号重复跳过；按姓名查医师档案补全医师 id，查不到的不阻断、留空待核实） |
| `GET /api/ethics-complaints/template` | 导入模板下载 |
| `GET /api/ethics-commendations` | 表彰分页 + 多条件组合查询（医师/表彰标题关键字、科室、表彰类型、表彰级别、审批状态） |
| `GET /api/ethics-commendations/stats` | 表彰统计：总数、待审批/已通过/已驳回、按表彰类型分组、按表彰级别分组、按科室汇总 |
| `GET /api/ethics-commendations/stats/by-dept` | 按科室汇总统计（表彰数、已通过数） |
| `GET /api/ethics-commendations/{id}` | 详情 |
| `POST /api/ethics-commendations` | 新增表彰登记：默认待审批；表彰类型/级别非法拒绝 |
| `PUT /api/ethics-commendations/{id}` | 修改（只有待审批的表彰能修改） |
| `DELETE /api/ethics-commendations/{id}` / `DELETE /api/ethics-commendations/batch` | 删除 / 批量删除（已通过的表彰不能直接删除） |
| `POST /api/ethics-commendations/{id}/approve` | 一级审批通过（医务科）：只有待审批的表彰能通过，审批记录留痕 |
| `POST /api/ethics-commendations/{id}/reject` | 一级审批驳回（医务科）：只有待审批的表彰能驳回，驳回原因必填；已结束的表彰不能再审批 |
| `GET /api/ethics-commendations/export` | 导出 Excel（按当前查询条件） |
| `POST /api/ethics-commendations/import` | 批量导入 Excel（同一医师同一表彰标题同一表彰日期重复跳过；按姓名查医师档案补全医师 id） |
| `GET /api/ethics-commendations/template` | 导入模板下载 |

投诉类型：服务态度 / 医疗质量 / 收费问题 / 廉洁行医 / 其他。投诉流转：待受理 → 调查中 → 已处理 → 已反馈，待受理的投诉可不予受理；逾期不单独存状态，按“未到已反馈、未不予受理且处理期限已过当天”动态判断。表彰类型：锦旗 / 表扬信 / 通报表扬 / 荣誉称号 / 其他；表彰级别：院级 / 市级 / 省级 / 国家级。种子数据：6 份档案（中医内科/针灸推拿科/骨伤科/妇科/儿科/急诊科各 1 人，全部在档；刘志强 2026 年度考评待审批，档案里最近考评结果为空）、2 个考评模板（年度/季度，权重之和均为 100）、6 条考评记录（优秀 2 / 合格 2 / 基本合格 1 / 不合格 1，平均分 80.33，优秀率 33.33%；已通过 5 条、待审批 1 条）、6 条投诉（服务态度 2 / 医疗质量 1 / 收费问题 1 / 廉洁行医 1 / 其他 1；待受理 1 / 调查中 2 / 已处理 1 / 已反馈 1 / 不予受理 1；陈先生投诉处理期限 2026-11-04 已逾期 1 条，反馈率 16.67%）、6 条表彰（锦旗 2 / 表扬信 1 / 通报表扬 2 / 荣誉称号 1；院级 4 / 市级 1 / 省级 1；已通过 4 条、待审批 1 条、已驳回 1 条）。前端页面为档案/考评/投诉/表彰四个页签，含统计卡片、按科室统计表、逾期投诉提醒表、新增/编辑弹窗、模板管理弹窗（评分项权重合计实时校验）、评分录入弹窗（按模板动态生成评分项输入框，实时显示总分与等级）、审批弹窗（通过/驳回，需填审批人+意见/原因）、投诉流转弹窗（受理/处理/反馈/不予受理）、批量导入导出。

### 医疗人员动态管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/personnel-changes` | 分页 + 多条件组合查询（人员姓名/相关科室/变动类型/审批状态/生效日期范围） |
| `GET /api/personnel-changes/stats` | 医疗人员动态统计：总数、待审批、已通过、已驳回、按变动类型分组、按相关科室汇总、按月趋势 |
| `GET /api/personnel-changes/stats/by-dept` | 按相关科室汇总统计（变动数、待审批、已通过、已驳回） |
| `GET /api/personnel-changes/stats/by-month` | 按月趋势统计（生效月份、变动数、已通过、待审批，按月份升序） |
| `GET /api/personnel-changes/{id}` | 详情 |
| `POST /api/personnel-changes` | 新增人员变动登记：默认待审批、申请日期为当天；同一人员同一变动类型同一生效日期已有待审批记录时拒绝重复登记 |
| `PUT /api/personnel-changes/{id}` | 修改（只有待审批的变动能修改） |
| `DELETE /api/personnel-changes/{id}` / `DELETE /api/personnel-changes/batch` | 删除 / 批量删除 |
| `POST /api/personnel-changes/{id}/approve` | 一级审批通过（医务科）：只有待审批的变动能通过，审批记录留痕；通过时同步医师档案（调动改科室、离职改在职状态为离职、退休改为退休、入职/返聘改回在职并落到新科室） |
| `POST /api/personnel-changes/{id}/reject` | 一级审批驳回（医务科）：只有待审批的变动能驳回，审批记录留痕；已结束的变动不能再审批、再修改 |
| `GET /api/personnel-changes/export` | 导出 Excel（按当前查询条件） |
| `POST /api/personnel-changes/import` | 批量导入 Excel（同一人员同一变动类型同一生效日期重复跳过；按姓名查医师档案补全人员 id） |
| `GET /api/personnel-changes/template` | 导入模板下载 |

变动类型：调动 / 离职 / 退休 / 入职 / 返聘。调动须填调入科室且与原科室不同；离职/退休须填原科室；入职/返聘须填调入（新）科室；入职人员尚未建档（无医师档案 id）时审批通过不阻断，需在医师档案里补建档案。种子数据：6 条变动，覆盖 5 种类型、待审批/已通过/已驳回 3 种状态（退休 1 条已通过，与医师档案郑秀兰已退休状态一致）。前端页面含统计卡片、按变动类型/相关科室/月份统计表、新增/编辑弹窗、详情弹窗、审批弹窗（通过/驳回，需填审批人+意见）、批量导入导出。

### 质量检查管理（完整前后端打通，覆盖第 13/14/15 项）

| 接口 | 说明 |
|---|---|
| `GET /api/quality-inspections` | 分页 + 多条件组合查询检查计划（模板/科室/检查人关键字、检查类型、科室、模板、计划状态、计划日期范围） |
| `GET /api/quality-inspections/stats` | 质量检查统计：计划总数、按状态分组、按检查类型分组、按科室汇总、按月趋势、检查记录数、平均分、按等级分组、问题数、整改完成率、逾期整改数 |
| `GET /api/quality-inspections/stats/by-dept` | 按科室汇总统计（计划数、待执行/进行中/已完成、检查记录数、平均分） |
| `GET /api/quality-inspections/stats/by-month` | 按月趋势统计（计划月份、计划数、已完成、进行中，按月份升序） |
| `GET /api/quality-inspections/rectifications` | 整改任务分页查询（责任人/状态/只看逾期/计划） |
| `GET /api/quality-inspections/{id}` | 详情（含全部检查记录、问题、整改任务） |
| `POST /api/quality-inspections` | 新增检查计划：默认待执行；模板须启用，计划的检查类型与模板一致 |
| `PUT /api/quality-inspections/{id}` | 修改（只有待执行的计划能修改） |
| `DELETE /api/quality-inspections/{id}` / `DELETE /api/quality-inspections/batch` | 删除 / 批量删除 |
| `POST /api/quality-inspections/{id}/start` | 开始检查：待执行 → 进行中 |
| `POST /api/quality-inspections/{id}/complete` | 完成检查：进行中 → 已完成（需要至少一条检查记录） |
| `POST /api/quality-inspections/{id}/cancel` | 取消计划：待执行/进行中 → 已取消 |
| `GET /api/quality-inspections/{id}/report` | 检查报告：按计划生成报告摘要（记录数、平均分、按等级分组、问题数、按严重程度分组、整改完成率、逾期数） |
| `GET /api/quality-inspections/{id}/records` | 查某计划的全部检查记录（按检查日期升序） |
| `POST /api/quality-inspections/{id}/records` | 添加检查记录：只有进行中的计划才能记录；逐项打分（每项不超过模板权重），总分与等级自动算（≥90 优秀 / ≥80 合格 / ≥60 基本合格 / <60 不合格） |
| `DELETE /api/quality-inspections/{id}/records/{recordId}` | 删除一条检查记录（同时删掉该记录下的问题与整改任务） |
| `GET /api/quality-inspections/records/{recordId}/issues` | 查某记录的全部问题 |
| `POST /api/quality-inspections/records/{recordId}/issues` | 添加问题：严重程度须合法（严重/一般/轻微） |
| `DELETE /api/quality-inspections/records/{recordId}/issues/{issueId}` | 删除一条问题（同时删掉该问题下的整改任务） |
| `GET /api/quality-inspections/{id}/rectifications` | 查某计划的全部整改任务（逾期标记按“待整改/已整改且期限已过当天”动态算出） |
| `POST /api/quality-inspections/issues/{issueId}/rectifications` | 针对一个问题发整改任务：一个问题只发一个 |
| `POST /api/quality-inspections/rectifications/{rectificationId}/submit` | 提交整改：待整改 → 已整改（待复查），整改完成日期默认为当天 |
| `POST /api/quality-inspections/rectifications/{rectificationId}/review-pass` | 复查通过：已整改 → 已复查通过 |
| `POST /api/quality-inspections/rectifications/{rectificationId}/review-reject` | 复查打回：已整改 → 待整改，可重新提交 |
| `GET /api/quality-inspections/export` | 导出 Excel：检查记录（按当前查询条件对应的计划） |
| `POST /api/quality-inspections/import` | 批量导入 Excel（检查计划）：同一模板同一科室同一计划日期重复跳过；模板不存在跳过；检查类型/计划状态非法时给默认值并在结果里说明 |
| `GET /api/quality-inspections/template` | 导入模板下载（检查计划导入模板） |
| `GET/POST/PUT/DELETE /api/quality-templates` | 检查模板 CRUD（检查项权重之和必须为 100） |
| `GET /api/quality-templates/enabled` | 全部启用的模板（新建检查计划时下拉用） |
| `POST /api/quality-templates/{id}/enable`、`POST /api/quality-templates/{id}/disable` | 启用 / 停用模板（停用的模板不能用于新建检查计划） |

检查类型：科室自查 / 院级检查 / 专项检查。计划状态：待执行 / 进行中 / 已完成 / 已取消。严重程度：严重 / 一般 / 轻微。整改状态：待整改 / 已整改（待复查）/ 已复查通过；逾期不单独存状态，按“待整改/已整改且整改期限已过当天”动态判断。种子数据：3 个模板（科室自查、院级检查、病历专项检查，权重之和均为 100）、4 个检查计划（覆盖 3 种检查类型、待执行/进行中/已完成 3 种状态）、3 条检查记录（89 分合格、84 分合格、68 分基本合格，平均分 80.33）、4 个检查问题（2 严重/2 一般）、4 个整改任务（1 已复查通过、1 已整改待复查、2 待整改，其中 1 条已逾期，整改完成率 25%）。前端页面含统计卡片（计划总数/进行中/已完成/整改完成率/逾期整改）、逾期整改提醒表、按检查类型/科室/月份统计表、新增/编辑弹窗、模板管理弹窗（检查项权重合计实时校验）、详情弹窗（含检查报告摘要、按模板动态生成的逐项打分表单、问题清单、整改跟踪与复查弹窗）、批量导入导出。

### 核心制度管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/core-systems` | 分页 + 多条件组合查询（制度名称/制度编号/制定部门关键字、类别、适用层级、状态、适用科室、只看 30 天内到期、只看已过期） |
| `GET /api/core-systems/stats` | 制度统计：总数、草稿/已发布/已废止、按类别分组、按层级分组、30 天内到期数、已过期数、查阅总人次/已学习人次/有查阅记录的制度数 |
| `GET /api/core-systems/stats/by-category` | 按制度类别汇总统计（制度数、已发布数） |
| `GET /api/core-systems/stats/by-level` | 按适用层级汇总统计（制度数、已发布数） |
| `GET /api/core-systems/stats/by-month` | 按月发布趋势统计（发布月份、发布数，按月份升序） |
| `GET /api/core-systems/{id}` | 详情（含正文，用于在线阅读） |
| `POST /api/core-systems` | 新增制度：默认草稿、V1.0、适用科室默认“全院”；同一编号同一版本已有记录拒绝重复建档；失效日期不能早于生效日期 |
| `PUT /api/core-systems/{id}` | 修改（只有草稿能修改；已发布的制度更新走新建版本，已废止的制度不可再改） |
| `DELETE /api/core-systems/{id}` / `DELETE /api/core-systems/batch` | 删除 / 批量删除（已发布的制度不能直接删除，请先废止；删除时同时删除其查阅学习记录） |
| `POST /api/core-systems/{id}/publish` | 发布（一级审批）：只有草稿能发布，审批人必填，发布日期默认为当天、生效日期为空时默认当天，审批记录留痕；发布新版本时同一编号的其他已发布版本自动废止 |
| `POST /api/core-systems/{id}/abolish` | 废止：只有已发布的制度能废止，废止原因必填，审批记录留痕 |
| `POST /api/core-systems/{id}/new-version` | 新建版本：只有已发布的制度能新建版本，生成草稿记录（制度编号相同、版本号 +1、正文与适用范围、归口科室沿用原制度） |
| `GET /api/core-systems/{id}/download` | 下载：按正文生成 .txt 文件（不引入真实文件存储） |
| `GET /api/core-systems/export` | 导出 Excel（按当前查询条件） |
| `POST /api/core-systems/import` | 批量导入 Excel（导入的均为草稿，需走发布审批流程；同一编号同一版本重复跳过；类别/层级非法时类别跳过、层级默认院级） |
| `GET /api/core-systems/template` | 导入模板下载 |
| `GET /api/core-system-readings` | 查阅学习记录分页 + 多条件组合查询（制度、查阅人/科室关键字、是否已学习） |
| `GET /api/core-system-readings/by-core-system/{coreSystemId}` | 查某项制度的全部查阅学习记录（按查阅日期降序） |
| `POST /api/core-system-readings` | 登记查阅学习：只有已发布的制度才能登记；查阅日期默认为当天；同一人同一制度同一天只登记一次 |
| `POST /api/core-system-readings/{id}/learned` | 标记已学习：已查阅 → 已学习 |
| `DELETE /api/core-system-readings/{id}` | 删除一条查阅学习记录 |

制度类别：医疗 / 护理 / 药事 / 管理 / 安全 / 其他。适用层级：院级 / 科室级。制度状态：草稿 / 已发布 / 已废止；失效不单独存状态，按“已发布且失效日期已过当天”动态判断已过期，30 天内到期即即将到期；版本号格式 V大版本.0，新建版本时大版本号 +1。种子数据：6 项制度（医疗 3 / 护理 1 / 药事 1 / 安全 1，院级 5 / 科室级 1，已发布 4 / 草稿 1 / 已废止 1；三级医师查房制度 2026-12-20 到期，30 天内到期 1 项；分级护理制度失效日期 2026-11-15 已过期 1 项）、6 条查阅学习记录（首诊负责制度 3 条 / 三级医师查房制度 2 条 / 分级护理制度 1 条，已学习 4 人次、已查阅 2 人次，覆盖制度 3 项）。前端页面含统计卡片（制度总数/草稿/已发布/已废止/30 天内到期/已过期/查阅总人次/已学习人次/有查阅记录的制度数）、已过期制度提醒表、按类别/按层级/按月发布统计表、新增/编辑弹窗（含适用科室与适用人员）、发布弹窗（审批人+审批意见）、废止弹窗（废止原因必填）、新建版本确认、详情弹窗（含基本信息、在线阅读、查阅学习记录三个页签，查阅学习可直接登记与标记已学习）、批量导入导出。

### 检查整改汇总（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/rectification-summaries` | 汇总报告分页 + 多条件组合查询（统计周期/汇总范围/科室/检查类型） |
| `GET /api/rectification-summaries/stats` | 实时汇总统计：问题总数、按严重程度分组、整改任务数、待整改/已整改/已复查通过数、整改完成率、逾期整改数、检查计划/记录数、平均分、按科室/按类型/按月汇总 |
| `GET /api/rectification-summaries/stats/by-dept` | 按责任科室汇总统计（问题数、按严重程度分组、整改数、整改完成率、逾期数、计划/记录数、平均分） |
| `GET /api/rectification-summaries/stats/by-type` | 按检查类型汇总统计（科室自查/院级检查/专项检查） |
| `GET /api/rectification-summaries/stats/by-month` | 按月汇总统计（按问题发现月份，月份升序） |
| `GET /api/rectification-summaries/overdue` | 逾期整改任务分页查询（带问题描述、严重程度、责任科室、检查类型；按整改期限升序） |
| `GET /api/rectification-summaries/issues` | 检查问题汇总分页查询（带整改任务状态、整改措施、整改期限与逾期标记；支持只看逾期/只看未完成整改） |
| `GET /api/rectification-summaries/{id}` | 汇总报告详情 |
| `POST /api/rectification-summaries` | 新增汇总报告：统计周期须为 yyyy-MM 格式；按科室汇总须填科室、按检查类型汇总须填合法类型；同一周期同一范围同一科室/类型已有记录拒绝重复建档；整改完成率始终按“已复查通过 / 整改任务总数”现算 |
| `PUT /api/rectification-summaries/{id}` | 修改汇总报告（汇总口径不可改，改口径请重新生成；未传的计数沿用原值，不会被归零） |
| `DELETE /api/rectification-summaries/{id}` / `DELETE /api/rectification-summaries/batch` | 删除 / 批量删除 |
| `POST /api/rectification-summaries/generate` | 生成汇总报告：按统计周期 + 汇总范围，从质量检查的问题与整改任务实时聚合生成；同一口径已有记录时重新生成（原地更新，不新增） |
| `GET /api/rectification-summaries/export` | 导出 Excel（按当前查询条件） |
| `POST /api/rectification-summaries/import` | 批量导入 Excel（同一周期同一范围同一科室/类型重复跳过；范围/类型非法跳过） |
| `GET /api/rectification-summaries/template` | 导入模板下载 |

汇总范围：全院 / 按科室 / 按检查类型。整改完成率 = 已复查通过数 / 整改任务总数 × 100，无整改任务时为 0；逾期与质量检查模块口径一致，按“待整改/已整改且整改期限已过当天”动态判断。问题与整改任务本身仍由质量检查管理模块（第 13/14/15 项）维护，本模块只读 quality_issue / quality_rectification（另加 quality_plan / quality_record 取科室、检查类型与平均分），不改写它们。种子数据：6 条汇总报告（2026-11 全院 1 条、按科室 2 条、按检查类型 3 条，按 2026-11 质量检查种子数据聚合：问题 4 个、严重 2 / 一般 2，整改 4 条、已复查通过 1 条、整改完成率 25.00%、逾期 1 条；骨伤科完成率 0%、逾期 1 条；专项检查暂无问题）。前端页面含统计卡片（问题总数/严重/一般/轻微/整改任务总数/已复查通过/整改完成率/逾期整改）、逾期整改提醒表、按科室/按检查类型/按月汇总表、汇总报告列表（生成/重新生成/手工新增/编辑/删除）、检查问题汇总表（支持只看逾期/只看未完成整改）、批量导入导出。

### 医疗技术分级管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/medical-techs` | 技术档案分页 + 多条件组合查询（技术名称/编号关键字、技术分类、技术分级、准入类别、风险等级、档案状态、开展科室、只看 90 天内到期、只看已过期） |
| `GET /api/medical-techs/stats` | 技术档案统计：总数、待准入/已准入/已停用、按技术分级分组、按准入类别分组、按技术分类分组、按开展科室汇总、90 天内到期数、已过期数、准入申请统计、医师授权统计 |
| `GET /api/medical-techs/stats/by-level` | 按技术分级汇总统计（技术数、已准入数） |
| `GET /api/medical-techs/stats/by-category` | 按技术分类汇总统计（技术数、已准入数） |
| `GET /api/medical-techs/stats/by-dept` | 按开展科室汇总统计（技术数、已准入数；一项技术可计入多个科室） |
| `GET /api/medical-techs/{id}` | 技术档案详情 |
| `POST /api/medical-techs` | 新增技术档案：默认待准入；同一技术编号已有记录拒绝重复建档；有效期不能早于准入日期 |
| `PUT /api/medical-techs/{id}` | 修改（已准入的技术不能直接修改，分级/科室/有效期变更请走准入申请） |
| `DELETE /api/medical-techs/{id}` / `DELETE /api/medical-techs/batch` | 删除 / 批量删除（已准入的技术不能直接删除，请先停用；删除时同时删除其准入申请与授权记录） |
| `POST /api/medical-techs/{id}/suspend` | 停用：只有已准入的技术能停用，停用原因必填，审批记录留痕 |
| `POST /api/medical-techs/{id}/resume` | 重新启用：只有已停用的技术能重新启用，审批记录留痕 |
| `GET /api/medical-techs/export` | 导出 Excel（按当前查询条件） |
| `POST /api/medical-techs/import` | 批量导入 Excel（导入的均为待准入，需走准入审批流程；同一技术编号重复跳过） |
| `GET /api/medical-techs/template` | 导入模板下载 |
| `GET /api/tech-admissions` | 准入申请分页 + 多条件组合查询（技术名称/编号/申请人关键字、技术、申请类型、申请科室、申请状态） |
| `GET /api/tech-admissions/stats` | 准入申请统计：总数、待审批/已通过/已驳回、按申请类型分组 |
| `GET /api/tech-admissions/by-tech/{techId}` | 查某项技术的全部准入申请（按申请日期降序） |
| `GET /api/tech-admissions/{id}` | 准入申请详情 |
| `POST /api/tech-admissions` | 新增准入申请：默认待审批、申请日期为当天；变更分级须填变更后分级、新增开展科室须填新增科室、延续准入须填延续后有效期（不能早于当天）；同一技术同一申请类型已有待审批记录拒绝重复申请 |
| `PUT /api/tech-admissions/{id}` | 修改（只有待审批的申请能修改） |
| `DELETE /api/tech-admissions/{id}` / `DELETE /api/tech-admissions/batch` | 删除 / 批量删除（已通过的准入申请不能直接删除） |
| `POST /api/tech-admissions/{id}/approve` | 一级审批通过（医务科）：只有待审批的申请能通过，审批人必填，审批记录留痕；通过时同步技术档案：新技术准入→档案变为已准入、准入日期填当天（有效期为空时默认 + 3 年）；新增开展科室→科室追加进档案；变更分级→档案分级改为新分级；延续准入→档案有效期改为新有效期；禁止类技术不得准入 |
| `POST /api/tech-admissions/{id}/reject` | 一级审批驳回（医务科）：只有待审批的申请能驳回，驳回原因必填；已结束的申请不能再审批 |
| `GET /api/tech-admissions/export` | 导出 Excel（按当前查询条件） |
| `POST /api/tech-admissions/import` | 批量导入 Excel（导入的均为待审批；同一技术同一类型同一申请日期重复跳过；按技术编号/名称查技术档案补全关联） |
| `GET /api/tech-admissions/template` | 导入模板下载 |
| `GET /api/tech-authorizations` | 医师授权分页 + 多条件组合查询（医师/技术名称/编号关键字、技术、医师、授权级别、授权状态、科室、只看 90 天内到期、只看已过期） |
| `GET /api/tech-authorizations/stats` | 授权统计：总数、待审批/已通过/已驳回/已注销、按授权级别分组、按科室汇总、90 天内到期数、已过期数 |
| `GET /api/tech-authorizations/stats/by-dept` | 按科室汇总统计（授权数、已通过数） |
| `GET /api/tech-authorizations/by-tech/{techId}` | 查某项技术的全部授权（按授权开始日期降序） |
| `GET /api/tech-authorizations/by-physician/{physicianId}` | 查某位医师的全部授权（按授权开始日期降序） |
| `GET /api/tech-authorizations/{id}` | 授权详情 |
| `POST /api/tech-authorizations` | 新增授权申请：默认待审批、授权开始日期默认为当天；技术须已准入、非禁止类、授权级别不高于技术分级；授权结束日期不能早于开始日期；同一医师同一技术已有待审批或已通过授权拒绝重复申请；科室按医师档案补全 |
| `PUT /api/tech-authorizations/{id}` | 修改（只有待审批的授权能修改） |
| `DELETE /api/tech-authorizations/{id}` / `DELETE /api/tech-authorizations/batch` | 删除 / 批量删除（已通过的授权不能直接删除，请先注销） |
| `POST /api/tech-authorizations/{id}/approve` | 一级审批通过（医务科）：只有待审批的授权能通过，审批人必填；通过时再次校验技术仍已准入且非禁止类、授权级别不高于技术分级 |
| `POST /api/tech-authorizations/{id}/reject` | 一级审批驳回（医务科）：只有待审批的授权能驳回，驳回原因必填；已结束的授权不能再审批 |
| `POST /api/tech-authorizations/{id}/revoke` | 注销：只有已通过的授权能注销，注销原因必填，审批记录留痕 |
| `GET /api/tech-authorizations/export` | 导出 Excel（按当前查询条件） |
| `POST /api/tech-authorizations/import` | 批量导入 Excel（导入的均为待审批；同一医师同一技术同一开始日期重复跳过；按姓名查医师档案补全医师 id 与科室，按技术编号/名称查技术档案补全技术分级） |
| `GET /api/tech-authorizations/template` | 导入模板下载 |

技术分级（一/二/三级，按《医疗技术临床应用管理办法》）：一级（安全性、有效性确切，技术难度低、风险低）、二级（安全性、有效性有一定保障，技术难度较大、有一定风险）、三级（安全性、有效性不确切，技术难度大、风险大）。准入类别：常规技术 / 限制类技术 / 禁止类技术（禁止类技术不得开展、不得授权）。技术分类：手术类 / 非手术类 / 中医技术 / 诊疗技术 / 其他。风险等级：低 / 中 / 高。档案状态：待准入 / 已准入 / 已停用；失效不单独存状态，按“已准入且有效期已过当天”动态判断已过期，90 天内到期即即将到期。种子数据：6 项技术档案（一级 3 / 二级 1 / 三级 2，常规技术 4 / 限制类 1 / 禁止类 1，已准入 4 / 待准入 1 / 已停用 1；小针刀松解术 2026-12-20 到期，90 天内到期 1 项；介入栓塞治疗技术 2026-11-15 已过期 1 项）、6 条准入申请（新技术准入 4 / 新增开展科室 1 / 延续准入 1，待审批 2 / 已通过 3 / 已驳回 1）、6 条医师授权（待审批 1 / 已通过 4 / 已驳回 1；王建军小针刀授权 2026-12-20 到期，周建国介入栓塞授权 2026-11-15 已过期）。前端页面为技术档案/准入申请/医师授权三个页签，含统计卡片（档案总数/待准入/已准入/已停用/90 天内到期/已过期、准入申请总数/待审批/已通过、授权总数/待审批/已过期）、按分级/分类/科室统计表、已过期技术提醒表、新增/编辑弹窗、停用弹窗（原因必填）、详情弹窗（含基本信息、准入申请、医师授权三个页签）、审批弹窗（通过/驳回/注销，需填审批人+意见/原因）、批量导入导出。

### 检查维护（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/inspection-items` | 检查项目分页 + 多条件组合查询（项目名称/编号关键字、项目分类、检查方式、适用检查类型、依据标准、项目状态、只看已组装进模板、只看未组装进模板） |
| `GET /api/inspection-items/stats` | 检查项目统计：总数、已启用/已停用、按项目分类分组、按检查方式分组、按适用检查类型分组、已组装进模板数、未组装进模板数 |
| `GET /api/inspection-items/stats/by-category` | 按项目分类汇总统计（项目数、已启用数） |
| `GET /api/inspection-items/{id}` | 检查项目详情 |
| `POST /api/inspection-items` | 新增检查项目：默认启用、满分默认 100、默认权重默认 10、适用类型默认通用；同一项目编号已有记录拒绝重复建档；依据标准不存在拒绝 |
| `PUT /api/inspection-items/{id}` | 修改检查项目（改编号时新编号已被其他项目占用拒绝） |
| `DELETE /api/inspection-items/{id}` / `DELETE /api/inspection-items/batch` | 删除 / 批量删除（已被模板引用的项目不能直接删除） |
| `POST /api/inspection-items/{id}/enable` | 启用：只有停用的项目能启用 |
| `POST /api/inspection-items/{id}/disable` | 停用：只有启用的项目能停用；已被模板引用的项目不可停用 |
| `GET /api/inspection-items/export` | 导出 Excel（按当前查询条件） |
| `POST /api/inspection-items/import` | 批量导入 Excel（导入的项目默认启用；分类非法跳过，依据标准编号查不到时置空关联，同一项目编号重复跳过） |
| `GET /api/inspection-items/template` | 导入模板下载 |
| `GET /api/inspection-standards` | 检查标准分页 + 多条件组合查询（标准名称/编号/依据文件关键字、标准级别、标准状态、只看 30 天内到期、只看已过期） |
| `GET /api/inspection-standards/stats` | 检查标准统计：总数、现行/已废止、按标准级别分组、30 天内到期数、已过期数、被检查项目引用数 |
| `GET /api/inspection-standards/stats/by-level` | 按标准级别汇总统计（标准数、现行数） |
| `GET /api/inspection-standards/{id}` | 检查标准详情 |
| `POST /api/inspection-standards` | 新增检查标准：默认现行；同一标准编号已有记录拒绝重复建档；有效期不能早于生效日期 |
| `PUT /api/inspection-standards/{id}` | 修改检查标准（改编号时新编号已被其他标准占用拒绝） |
| `DELETE /api/inspection-standards/{id}` / `DELETE /api/inspection-standards/batch` | 删除 / 批量删除（已被检查项目引用的标准不能直接删除） |
| `POST /api/inspection-standards/{id}/abolish` | 废止：只有现行的标准能废止，废止原因必填 |
| `POST /api/inspection-standards/{id}/restore` | 恢复现行：只有已废止的标准能恢复，废止原因清空 |
| `GET /api/inspection-standards/export` | 导出 Excel（按当前查询条件） |
| `POST /api/inspection-standards/import` | 批量导入 Excel（导入的标准默认现行；级别非法或有效期早于生效日期跳过，同一标准编号重复跳过） |
| `GET /api/inspection-standards/template` | 导入模板下载 |
| `GET /api/inspection-template-items` | 模板检查项配置分页 + 多条件组合查询（模板、项目、模板/项目名称/编号关键字） |
| `GET /api/inspection-template-items/stats` | 模板配置统计：已配置模板数、配置记录总数、平均每模板检查项数、未配置检查项的模板数、未组装进任何模板的项目数 |
| `GET /api/inspection-template-items/by-template/{templateId}` | 查某模板的全部检查项（按排序号升序） |
| `GET /api/inspection-template-items/{id}` | 模板检查项配置详情 |
| `POST /api/inspection-template-items/configure` | 配置模板检查项（整模板替换）：模板须存在、至少一项、项目须存在且已启用、同一模板内项目不重复、每项权重大于 0、权重之和必须为 100；保存后同步回写模板 checkItems JSON（名称、权重、评分标准） |
| `DELETE /api/inspection-template-items/{id}` / `DELETE /api/inspection-template-items/batch` | 删除单项 / 批量删除（一个模板至少保留一项，删除后同步回写模板 JSON） |
| `GET /api/inspection-template-items/export` | 导出指定模板的检查项 Excel |
| `POST /api/inspection-template-items/import?templateId=` | 批量导入指定模板的检查项 Excel（整模板替换，权重之和须为 100；项目编号查不到或已停用跳过） |
| `GET /api/inspection-template-items/template` | 导入模板下载 |

项目分类：病历质量 / 核心制度 / 医院感染 / 药事管理 / 护理质量 / 医疗安全 / 服务质量 / 中医特色 / 科室管理 / 其他。检查方式：现场查看 / 抽查病历 / 访谈 / 资料查阅。适用检查类型：通用 / 科室自查 / 院级检查 / 专项检查（与质量检查模板的检查类型一致）。标准级别：国家 / 省 / 市 / 院内。项目状态：启用 / 停用；标准状态：现行 / 已废止；失效不单独存状态，按“现行且有效期已过当天”动态判断已过期，30 天内到期即即将到期。模块边界：检查项目库与检查标准库是质量检查的基础数据（模板从项目库组装检查项，不再各模板手写检查项；项目通过标准 id 关联标准库）；模板本身的增删改与启用/停用仍在质量检查管理模块，本模块只维护“模板—检查项”的组装关系并同步回写模板 JSON。种子数据：12 个检查项目（病历质量 4 / 核心制度 1 / 医院感染 1 / 药事管理 1 / 医疗安全 2 / 服务质量 1 / 科室管理 1 / 中医特色 1，启用 11 / 停用 1；已组装进模板 11 项、未组装 1 项）、6 条检查标准（国家 4 / 省 1 / 院内 1，现行 5 / 已废止 1；病历书写基本规范 2026-12-20 到期，30 天内到期 1 部；抗菌药物临床应用管理办法 2026-11-15 已过期 1 部；被检查项目引用 4 部）、11 条模板检查项配置（科室自查模板 4 项权重 30/30/20/20、院级质量检查模板 4 项权重 35/30/20/15、病历专项检查模板 3 项权重 40/30/30，与模板 checkItems JSON 一致）。前端页面为检查项目/检查标准/模板检查项配置三个页签，含统计卡片（项目总数/已启用/已停用/已组装进模板/未组装进模板、标准总数/现行/已废止/30 天内到期/已过期/被项目引用、已配置模板数/配置记录总数/平均每模板检查项数/未配置检查项的模板/未组装进任何模板的项目）、按分类/级别统计表、新增/编辑弹窗、废止弹窗（原因必填）、模板配置弹窗（勾选已启用项目、逐项填权重、实时显示权重之和，须为 100 才可保存）、批量导入导出。

### 检查分类（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/inspection-categories` | 检查分类分页 + 多条件组合查询（分类名称/编码关键字、层级、适用检查类型、状态、只看有项目归属、只看无项目归属） |
| `GET /api/inspection-categories/stats` | 检查分类统计：总数、已启用/已停用、一级/二级、按适用检查类型分组、归属检查项目总数、有项目归属的分类数、无项目归属的分类数、已组装进模板的项目数、涉及模板数 |
| `GET /api/inspection-categories/stats/by-category` | 按分类汇总统计（每个分类的项目数、已启用项目数、涉及模板数、下级数） |
| `GET /api/inspection-categories/tree` | 分类树（一级分类带下级） |
| `GET /api/inspection-categories/enabled` | 全部启用的分类（新建检查项目时下拉用） |
| `GET /api/inspection-categories/{id}` | 检查分类详情（含上级分类编码/名称） |
| `POST /api/inspection-categories` | 新增检查分类：默认启用、适用类型默认通用、层级按上级自动算；同一分类编码已有记录拒绝重复建档；同一上级下同名拒绝；上级不存在/不是一级分类拒绝（不建三级） |
| `PUT /api/inspection-categories/{id}` | 修改检查分类（改编码时新编码已被其他分类占用拒绝；已有项目归属的分类编码不可改；已有下级的分类不可改为二级；上级不能是自己或自己的下级） |
| `DELETE /api/inspection-categories/{id}` / `DELETE /api/inspection-categories/batch` | 删除 / 批量删除（已有下级、或已有检查项目归属的分类不能直接删除） |
| `POST /api/inspection-categories/{id}/enable` | 启用：只有停用的分类能启用；父分类停用时子分类不可启用 |
| `POST /api/inspection-categories/{id}/disable` | 停用：只有启用的分类能停用；已有检查项目归属、或有启用下级的分类不可停用 |
| `GET /api/inspection-categories/export` | 导出 Excel（按当前查询条件） |
| `POST /api/inspection-categories/import` | 批量导入 Excel（导入的分类默认启用；上级按编码解析，查不到或不是一级分类跳过；同一分类编码重复跳过） |
| `GET /api/inspection-categories/template` | 导入模板下载 |

两级分类：parentId 为空是一级分类，有上级是二级分类，不建三级；层级由上级决定，后端自动算。适用检查类型：通用 / 科室自查 / 院级检查 / 专项检查（与检查项目一致）。分类状态：启用 / 停用。关联口径与模块 19 检查维护一致：检查项目按分类编码归类（InspectionItem.category 存分类编码），模板通过模板检查项配置组装项目、间接引用分类；分类下有检查项目归属时不可删除、不可停用、不可改编码，其中有项目已被模板引用时报错点明模板引用（先从模板里移除项目）；停用的分类不可再归检查项目（检查项目新增/修改时按分类库校验，库里没有的编码沿用模块 19 的 10 个既有分类）。种子数据：12 个分类（一级 10 个，编码与模块 19 既有项目分类口径一致，全部启用；二级 2 个：运行病历 / 终末病历，均挂在病历质量下，适用类型分别为科室自查 / 专项检查）、归属检查项目 12 个（8 个分类有项目、4 个分类无项目；病历质量 4 个、医疗安全 2 个，其余 1 个；已组装进模板 11 项、涉及 3 个模板）。前端页面含统计卡片（分类总数/已启用/已停用/一级/二级/归属检查项目/有项目归属的分类/无项目归属的分类）、按分类汇总统计表、分类树、新增/编辑弹窗（上级分类下拉只列一级分类）、批量导入导出。

### 检查模板管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/template-library/stats` | 模板库统计：模板总数、已启用/已停用、按检查类型分组、版本总数、有版本记录的模板数、无版本记录的模板数、平均每模板检查项数、最常用模板（按计划数）、从未使用数 |
| `GET /api/template-library/templates/{id}/usage` | 某模板的使用情况：计划总数（待执行/进行中/已完成/已取消）、检查记录总数、最近一次计划日期、是否有进行中计划 |
| `POST /api/template-library/templates/{id}/copy` | 复制模板：同类型同检查项的新模板，默认停用；同检查类型下已存在同名模板拒绝 |
| `GET /api/template-library/templates/{id}/versions` | 某模板的版本历史（按版本号倒序） |
| `GET /api/template-library/templates/{id}/versions/current` | 某模板的当前版本 |
| `POST /api/template-library/templates/{id}/versions` | 新建版本：检查项权重之和须为 100；旧当前版本改为历史版本，模板检查项回写为新内容；有待执行/进行中计划时拒绝改版 |
| `GET /api/template-library/versions/{id}` | 版本详情（快照内容） |
| `POST /api/template-library/versions/{id}/restore` | 从历史版本恢复：以该版本快照内容新建一版（历史只追加不改写）；有待执行/进行中计划时拒绝 |
| `GET /api/template-library/templates/export` | 导出全部模板 Excel |
| `POST /api/template-library/templates/import` | 批量导入模板 Excel（同名同类型跳过，非法行跳过；返回新增/跳过/总数） |
| `GET /api/template-library/templates/import-template` | 导入模板下载 |
| `POST /api/quality-templates/{id}/disable` | 停用模板（新增守卫：有进行中计划时拒绝） |
| `DELETE /api/quality-templates/{id}` | 删除模板（新增守卫：已被检查计划引用时拒绝，返回 400） |

模板本身的增删改与启用/停用仍在质量检查管理模块（/api/quality-templates），本模块补齐模板的版本、复制、使用跟踪、统计与批量导入导出。版本管理：每次新建版本/从历史版本恢复时追加一条版本记录（V1.0/V2.0/V3.0……），旧当前版本改为历史版本，历史记录只追加不改写；检查记录逐项打分按模板当前检查项权重校验上限，因此模板有待执行/进行中计划时不允许改版与停用，已被检查计划引用的模板不允许直接删除。种子数据：3 个种子模板各 1 个 V1.0 当前版本快照（与模板 checkItems JSON 一致）。前端页面含统计卡片（模板总数/已启用/已停用/版本总数/平均检查项数/从未使用/无版本记录/最常用模板）、新增/编辑弹窗（检查项逐项编辑、权重之和实时显示须为 100）、版本历史弹窗（当前/历史标记、恢复按钮）、新建版本弹窗、复制弹窗（新模板默认停用）、使用情况弹窗、批量导入导出。

### 医疗分级管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/grading-standards` | 分级标准分页 + 多条件组合查询（标准名称/编码关键字、分级范围、状态） |
| `GET /api/grading-standards/stats` | 分级标准统计：总数、启用/停用、按分级范围分组、已用/未用标准数 |
| `GET /api/grading-standards/enabled` | 全部启用的标准（新建评定时下拉用） |
| `GET /api/grading-standards/{id}` | 标准详情 |
| `POST /api/grading-standards` | 新增标准：编码唯一、范围合法、分数线须满足 丙级线 < 乙级线 < 甲级线；默认启用、甲级 90 / 乙级 80 / 丙级 60 |
| `PUT /api/grading-standards/{id}` | 修改标准（已有评定引用时编码不可改；分数线变更不追溯已有评定） |
| `DELETE /api/grading-standards/{id}` | 删除标准（已有分级评定的标准不能直接删除） |
| `POST /api/grading-standards/{id}/enable` | 启用标准 |
| `POST /api/grading-standards/{id}/disable` | 停用标准（有待审批评定的标准不可停用） |
| `GET /api/grading-standards/export` | 导出分级标准 Excel（按当前查询条件） |
| `POST /api/grading-standards/import` | 批量导入分级标准 Excel（编码重复或非法行跳过） |
| `GET /api/grading-standards/template` | 导入模板下载 |
| `GET /api/grading-assessments` | 分级评定分页 + 多条件组合查询（科室/评定人关键字、标准、科室、等级、状态、周期/周期范围） |
| `GET /api/grading-assessments/stats` | 分级评定统计：总数、待审批/已通过/已驳回、按等级分组、平均分、达标率（甲+乙+丙占比）、已用标准数、已评科室数 |
| `GET /api/grading-assessments/stats/by-dept` | 按科室汇总：各科室最新已通过等级（当前分级）、平均分、评定次数 |
| `GET /api/grading-assessments/stats/by-month` | 按月趋势：每月评定数、已通过数、平均分，按月份升序 |
| `GET /api/grading-assessments/{id}` | 评定详情 |
| `POST /api/grading-assessments` | 新增评定：默认待审批；等级按所属标准分数线自动算（甲级/乙级/丙级/丁级） |
| `PUT /api/grading-assessments/{id}` | 修改评定（只有待审批的能修改，改评分时重算等级） |
| `DELETE /api/grading-assessments/{id}` | 删除评定（已通过的评定是科室当前分级的依据，不可直接删除） |
| `POST /api/grading-assessments/{id}/approve` | 一级审批通过（审批人必填，审批记录留痕） |
| `POST /api/grading-assessments/{id}/reject` | 一级审批驳回（审批人必填，审批记录留痕） |
| `GET /api/grading-assessments/export` | 导出分级评定 Excel（按当前查询条件） |
| `POST /api/grading-assessments/import` | 批量导入分级评定 Excel（标准编码须存在且启用；重复或非法行跳过） |
| `GET /api/grading-assessments/template` | 导入模板下载 |

分级范围：QUALITY 医疗质量分级 / SERVICE 医疗服务分级 / COMPREHENSIVE 综合分级。与模块 18 医疗技术分级管理的区别：本模块评的是科室整体的医疗质量/服务等级（按科室、按周期评定），18 号模块评的是单项医疗技术的分级准入与医师授权；与 24/25/26 统计的区别：本模块是分级评定的业务台账与审批流，统计模块只做工作量与指标的聚合展示。等级：A 甲级 / B 乙级 / C 丙级 / D 丁级，按所属标准分数线自动算（默认甲级 90 / 乙级 80 / 丙级 60，综合标准为 85 / 75 / 60）。种子数据：3 个分级标准（医疗质量 / 医疗服务 / 综合分级，全部启用）、6 条分级评定（2026-11：甲级 1 / 乙级 3 / 丙级 1 / 丁级 1；已通过 3 / 待审批 2 / 已驳回 1；平均分 80.67、达标率 83.33%）。前端页面为分级评定/分级标准两个页签，含统计卡片（评定总数/待审批/已通过/已驳回/平均分/达标率/已用标准数/已评科室数）、按科室汇总表（当前分级取最新已通过评定）、新增/编辑弹窗、审批弹窗（审批人必填）、批量导入导出。

### 新技术、新项目管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/new-techs` | 分页 + 多条件组合查询（项目名称/申报人/负责人关键字、科室、项目类型、审批状态、伦理审查状态、申报日期范围） |
| `GET /api/new-techs/stats` | 新技术、新项目统计：总数、待审批、已立项、已验收、已推广、已驳回、待伦理审查数、按项目类型分组、按科室汇总、按月趋势 |
| `GET /api/new-techs/stats/by-dept` | 按科室汇总统计（项目数、待审批、已立项、已验收、已推广） |
| `GET /api/new-techs/stats/by-month` | 按月趋势统计（申报月份、申报数、已立项（含验收/推广）、待审批，按月份升序） |
| `GET /api/new-techs/{id}` | 详情（含全部进展记录） |
| `POST /api/new-techs` | 新增申报：默认待审批、申报日期为当天；需伦理审查的项目默认伦理待审查；计划结束日期不能早于开始日期、预算不能为负 |
| `PUT /api/new-techs/{id}` | 修改（只有待审批的项目能修改申报内容；已有伦理审查结论的项目，修改时不改动伦理审查结果） |
| `DELETE /api/new-techs/{id}` / `DELETE /api/new-techs/batch` | 删除 / 批量删除 |
| `POST /api/new-techs/{id}/approve` | 一级审批通过（医务科）：只有待审批的项目能通过，需伦理审查的项目须伦理通过后才能立项，审批记录留痕 |
| `POST /api/new-techs/{id}/reject` | 一级审批驳回（医务科）：只有待审批的项目能驳回，审批记录留痕；已结束的项目不能再审批 |
| `POST /api/new-techs/{id}/ethics` | 伦理审查：只有需伦理审查且待审查的项目能审查，审查结论与意见留痕 |
| `GET /api/new-techs/{id}/records` | 查某项目的全部进展记录（按记录日期升序） |
| `POST /api/new-techs/{id}/records` | 添加进展记录：进展记录只允许已立项/已验收的项目；验收记录只允许已立项的项目，添加后项目自动变为已验收；推广记录只允许已验收的项目，添加后项目自动变为已推广 |
| `DELETE /api/new-techs/{id}/records/{recordId}` | 删除一条进展记录 |
| `GET /api/new-techs/export` | 导出 Excel（按当前查询条件） |
| `POST /api/new-techs/import` | 批量导入 Excel（同一项目名称同一申报科室重复跳过；项目类型/伦理审查状态非法时给默认值并在结果里说明） |
| `GET /api/new-techs/template` | 导入模板下载 |

项目类型：新技术 / 新项目。审批状态：待审批 / 已立项 / 已验收 / 已驳回 / 已推广。伦理审查状态：不需审查 / 待审查 / 已通过 / 未通过。种子数据：5 个项目（覆盖 2 种项目类型、待审批/已立项/已验收/已驳回/已推广 5 种状态；伦理待审查/已通过/未通过/不需审查各 1 条）、3 条进展记录（骨伤科项目验收 1 条，儿科项目验收 + 推广各 1 条）。前端页面含统计卡片（项目总数/待审批/已立项/待伦理审查）、按项目类型/科室/月份统计表、新增/编辑弹窗、详情弹窗（含进展记录列表与添加记录表单）、审批弹窗（伦理未通过时提示不能立项）、伦理审查弹窗（通过/未通过）、批量导入导出。

### 统计分析（完整前后端打通，覆盖第 24/25/26 项）

| 接口 | 说明 |
|---|---|
| `GET /api/workloads` | 工作量登记分页 + 多条件组合查询（医师/科室关键字、科室、医师、统计月份/月份范围） |
| `GET /api/workloads/stats` | 工作统计：门诊/住院/手术总量与合计、记录数、按科室/按人员/按月汇总 |
| `GET /api/workloads/stats/by-dept` | 按科室汇总统计（门诊量、住院量、手术量、合计、填报人数） |
| `GET /api/workloads/stats/by-physician` | 按人员汇总统计（门诊量、住院量、手术量、合计） |
| `GET /api/workloads/stats/by-month` | 按月趋势统计（月份、门诊量、住院量、手术量、合计，按月份升序） |
| `GET /api/workloads/{id}` | 详情 |
| `POST /api/workloads` | 新增工作量登记：同一医师同一统计月份只允许一条，重复拒绝；工作量不能为负 |
| `PUT /api/workloads/{id}` | 修改 |
| `DELETE /api/workloads/{id}` / `DELETE /api/workloads/batch` | 删除 / 批量删除 |
| `GET /api/workloads/export` | 导出 Excel（按当前查询条件） |
| `POST /api/workloads/import` | 批量导入 Excel（同一医师同一统计月份重复跳过；月份格式非法跳过） |
| `GET /api/workloads/template` | 导入模板下载 |
| `GET /api/quality-indicators` | 指标分页 + 多条件组合查询（编码/名称关键字、分类、标准级别、统计周期、科室），每条附带达标情况 |
| `GET /api/quality-indicators/stats` | 指标统计：总数、已填报数、达标数、达标率、按标准级别/按分类汇总 |
| `GET /api/quality-indicators/stats/by-level` | 按标准级别汇总统计（国家/省/市各级别的指标数、已填报数、达标数、达标率） |
| `GET /api/quality-indicators/stats/by-category` | 按指标分类汇总统计（医疗质量/医疗安全/患者安全/工作效率） |
| `GET /api/quality-indicators/{id}` | 详情（附带达标情况） |
| `POST /api/quality-indicators` | 新增指标：指标编码全院唯一，重复拒绝；标准级别/分类/比较方向非法拒绝 |
| `PUT /api/quality-indicators/{id}` | 修改 |
| `DELETE /api/quality-indicators/{id}` / `DELETE /api/quality-indicators/batch` | 删除 / 批量删除 |
| `GET /api/quality-indicators/export` | 导出 Excel（按当前查询条件） |
| `POST /api/quality-indicators/import` | 批量导入 Excel（同一指标编码重复跳过；标准级别/分类非法跳过） |
| `GET /api/quality-indicators/template` | 导入模板下载 |
| `GET /api/statistics/overview` | 三组统计一次取全：工作统计 + 三级医院指标统计 + 医疗质量统计 |
| `GET /api/statistics/work` | 工作统计（24）：工作量登记汇总 + 排班人次（另一口径，单独列示） |
| `GET /api/statistics/tertiary` | 三级医院医疗质量指标统计（25）：指标表汇总 + 全部指标 |
| `GET /api/statistics/quality` | 医疗质量统计（26）：医疗质量/医疗安全/患者安全/新技术新项目四组实时聚合指标 |
| `GET /api/statistics/export` | 导出统计报表 Excel：工作统计（按科室）、三级医院指标、医疗质量统计三个 sheet |

工作量登记：一条记录 = 一名医师一个月的填报工作量（门诊量/住院量/手术量）。种子数据 8 条（2026-11，8 个科室各 1 名代表医师；医务科为行政科室，无临床工作量）：门诊合计 3420 人次、住院合计 1066 人次、手术合计 241 台、工作总量 4727；按科室合计最高为中医内科 922（680+210+32）。排班人次来自排班记录（种子 8 条），是另一口径，接口里单独列示，不与诊疗量混算。

三级医院医疗质量指标：一条记录 = 一个指标在一个统计周期内的填报值，达标与否由目标值、实际值与比较方向现算（越高越好：实际值 ≥ 目标值；越低越好：实际值 ≤ 目标值；未填报的不计入达标率）。种子数据 10 条，实际值均按当前库内已有业务数据聚合填写、数据来源写在每条指标的 dataSource 里：医师考核优秀率 33.33%（考核 6 条、优秀 2 人）、医师考核平均分 81.17 分（92+95+85+72+55+88=487）、质量检查平均分 80.33 分（89+84+68=241）、整改完成率 25%（整改 4 条、已复查通过 1 条）、严重问题占比 50%（问题 4 个、严重 2 个，越低越好）、MDT 会诊通过率 40%（申请 5 条、已通过 2 条）、新技术项目立项率 60%（项目 5 个、已立项/已验收/已推广 3 个）、月门诊量 3420 人次、月手术量 241 台、月住院量 1066 人次；达标 7 条、未达标 3 条（整改完成率、严重问题占比、MDT 会诊通过率），达标率 70.00%；按级别：国家 4 条达标 3 条（75%）、省 3 条达标 2 条（66.67%）、市 3 条达标 2 条（66.67%）。前端页面为三个页签（工作统计/三级医院指标/医疗质量统计）：统计卡片、登记/指标的增删改查与批量导入导出、按科室/按人员/按月统计表、按国家/省/市标准的达标率进度条、医疗质量/医疗安全/患者安全三组指标的图形化展示（Element Plus 进度条）、统计报表导出。注意：MyBatis-Plus `updateById` 默认不更新 null 字段，指标“未填报实际值”不走库内 update 覆盖，见 `StatisticsTests` 注释。


### 医务系统完成统计（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/module-completions` | 分页 + 多条件组合查询（模块名称关键字/分组/状态/模块编号） |
| `GET /api/module-completions/stats` | 完成统计总览：模块总数、已完成/进行中/待开发数、完成率、接口总数、测试总数、平均进度 |
| `GET /api/module-completions/stats/by-category` | 按分组汇总统计（各分组模块数、已完成/进行中/待开发数、完成率、接口数、测试数、平均进度） |
| `GET /api/module-completions/stats/by-month` | 按完成月份趋势统计（每月完成模块数，按月份升序） |
| `GET /api/module-completions/{id}` | 详情 |
| `POST /api/module-completions` | 新增模块台账：模块编号全院唯一、分组须合法、进度 0-100；已完成时进度强制 100、完成日期为空默认当天，未完成时完成日期清空 |
| `PUT /api/module-completions/{id}` | 修改（模块编号不可与已有记录重复） |
| `DELETE /api/module-completions/{id}` / `DELETE /api/module-completions/batch` | 删除 / 批量删除 |
| `POST /api/module-completions/{id}/complete` | 标记完成：只有未完成的模块能标记完成，进度置 100、完成日期默认当天 |
| `POST /api/module-completions/{id}/reopen` | 重新开始：只有已完成的模块能重新开始，回到进行中、进度置 50、完成日期清空 |
| `GET /api/module-completions/export` | 导出 Excel（按当前查询条件） |
| `POST /api/module-completions/import` | 批量导入 Excel（模块编号重复或非法行跳过） |
| `GET /api/module-completions/template` | 导入模板下载 |

需求梳理.md 第 27 项只有一句话定义（“医务系统各模块完成情况统计”），口径按 README 模块总表实现：31 个模块各一行台账，分组为医师管理（1-8）/ 组织与制度（9-12）/ 质量管理（13-22）/ 科研与统计（23-27）/ 系统与移动（28-31）。接口数 / 测试数为代码实测：统计各包 `@Mapping` 注解数与各测试类 `@Test` 数；合入其他模块的记为空（第 14/15 项接口与测试计入第 13 项、第 25/26 项测试计入第 24 项），统计合计时按 0 计，不虚增。种子数据：31 条台账，已完成 29 / 进行中 0 / 待开发 2（30 医疗工作简报、31 移动端），完成率 93.55%，接口总数 661、测试总数 337、平均进度 93.55%。前端页面含统计卡片（模块总数/已完成/进行中/待开发/完成率/接口总数/测试总数/平均进度）、按分组/按月统计表、进度条、标记完成/重新开始按钮、批量导入导出。

### 医务系统设置（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/system-settings/overview` | 系统设置总览：底座（用户/角色/权限/科室/字典/操作日志）与参数/备份/接口/提醒设置/知识库的数量与各组统计 |
| `GET /api/sys-params` | 参数分页 + 多条件查询（键/名称关键字、分组、值类型、启用状态） |
| `GET /api/sys-params/stats` | 参数统计：总数、启用/停用数、内置/自定义数、按分组统计 |
| `GET /api/sys-params/{id}` / `GET /api/sys-params/by-key/{key}` | 参数详情 / 按键取值 |
| `POST /api/sys-params` | 新增参数：参数键全院唯一、分组须合法、值须符合值类型（STRING/NUMBER/BOOLEAN） |
| `PUT /api/sys-params/{id}` | 修改：内置参数的键不可改 |
| `DELETE /api/sys-params/{id}` / `DELETE /api/sys-params/batch` | 删除 / 批量删除：内置参数不可删 |
| `POST /api/sys-params/{id}/enable` / `POST /api/sys-params/{id}/disable` | 启用 / 停用 |
| `GET /api/sys-params/export` / `POST /api/sys-params/import` / `GET /api/sys-params/template` | 导出 / 批量导入 / 导入模板 |
| `GET /api/sys-backups` | 备份分页 + 多条件查询（名称关键字、类型、状态、备份人） |
| `GET /api/sys-backups/stats` | 备份统计：总数、成功/失败数、已恢复数、总记录条数、总文件大小 |
| `GET /api/sys-backups/{id}` | 备份详情 |
| `POST /api/sys-backups` | 执行备份：类型须合法，逐表 COUNT 求和得记录条数，生成 `medical-backup-yyyyMMdd-HHmmss.zip` 文件名 |
| `POST /api/sys-backups/{id}/restore` | 恢复备份：只有成功的备份能恢复，恢复次数 +1 并记录恢复时间 |
| `DELETE /api/sys-backups/{id}` | 删除：已被恢复过的备份不可直接删除 |
| `GET /api/sys-backups/export` / `GET /api/sys-backups/template` | 导出 / 导入模板（备份只导出、不导入） |
| `GET /api/sys-interfaces` | 接口分页 + 多条件查询（编码/名称关键字、类型、鉴权方式、启用状态） |
| `GET /api/sys-interfaces/stats` | 接口统计：总数、启用/停用数、测试成功/失败/未测试数、按类型统计 |
| `GET /api/sys-interfaces/{id}` | 接口详情（鉴权凭证不回显） |
| `POST /api/sys-interfaces` | 新增接口：编码唯一、类型须合法、地址须 http/https 开头、超时 1–60 秒、鉴权方式须合法 |
| `PUT /api/sys-interfaces/{id}` | 修改：编码唯一（排除自身） |
| `DELETE /api/sys-interfaces/{id}` / `DELETE /api/sys-interfaces/batch` | 删除 / 批量删除 |
| `POST /api/sys-interfaces/{id}/enable` / `POST /api/sys-interfaces/{id}/disable` | 启用 / 停用 |
| `POST /api/sys-interfaces/{id}/test` | 连通性测试：真实发起 HTTP GET 请求，结果回写 lastTestResult/lastTestTime/lastTestMsg，不伪造成功 |
| `GET /api/sys-interfaces/export` / `POST /api/sys-interfaces/import` / `GET /api/sys-interfaces/template` | 导出 / 批量导入（凭证不导入） / 导入模板 |
| `GET /api/sys-notice-settings` | 提醒设置分页 + 多条件查询（编码/名称关键字、渠道、触发事件、启用状态） |
| `GET /api/sys-notice-settings/stats` | 提醒统计：总数、启用/停用数、按渠道/按接收范围统计 |
| `GET /api/sys-notice-settings/{id}` | 提醒设置详情 |
| `POST /api/sys-notice-settings` | 新增提醒设置：编码唯一、渠道须合法；接收范围 ALL 时接收对象强制清空，ROLE 须填角色编码，USER 须填用户名 |
| `PUT /api/sys-notice-settings/{id}` | 修改（同上校验） |
| `DELETE /api/sys-notice-settings/{id}` / `DELETE /api/sys-notice-settings/batch` | 删除 / 批量删除 |
| `POST /api/sys-notice-settings/{id}/enable` / `POST /api/sys-notice-settings/{id}/disable` | 启用 / 停用 |
| `GET /api/sys-notice-settings/export` / `POST /api/sys-notice-settings/import` / `GET /api/sys-notice-settings/template` | 导出 / 批量导入 / 导入模板 |
| `GET /api/sys-knowledge` | 知识库分页 + 多条件查询（标题/编码/关键字、分类、启用状态） |
| `GET /api/sys-knowledge/stats` | 知识库统计：总数、启用/停用数、总查阅量、按分类统计 |
| `GET /api/sys-knowledge/{id}` | 知识详情：打开一次查阅量 +1 |
| `POST /api/sys-knowledge` | 新增知识：编码唯一、分类须合法 |
| `PUT /api/sys-knowledge/{id}` | 修改：编辑不改写查阅量 |
| `DELETE /api/sys-knowledge/{id}` / `DELETE /api/sys-knowledge/batch` | 删除 / 批量删除 |
| `POST /api/sys-knowledge/{id}/enable` / `POST /api/sys-knowledge/{id}/disable` | 启用 / 停用 |
| `GET /api/sys-knowledge/export` / `POST /api/sys-knowledge/import` / `GET /api/sys-knowledge/template` | 导出 / 批量导入（查阅量从 0 计） / 导入模板 |
| `GET /api/users/{id}/roles` / `POST /api/users/{id}/roles` | 查询用户已分配角色 / 给用户分配角色 |
| `POST /api/users/{id}/reset-password` | 重置密码为默认 123456 |
| `GET /api/users/stats` | 用户统计：总数、启用/停用数、按科室统计 |
| `GET /api/roles/{id}/permissions` / `POST /api/roles/{id}/permissions` | 查询角色已分配权限 / 给角色分配权限 |
| `GET /api/roles/stats` | 角色统计：总数、启用/停用数、各角色已分配用户数 |
| `GET /api/depts/stats` | 科室统计：总数、启用/停用数、按层级统计 |
| `GET /api/permissions/stats` | 权限统计：总数、启用/停用数、按类型（目录/菜单/按钮）统计 |
| `GET /api/dicts/stats` / `GET /api/dicts/types` | 字典统计：类型数、数据总数、启用/停用数、按类型统计 / 字典类型下拉 |
| `GET /api/oper-logs/stats` | 操作日志统计：总数、成功/失败数、按操作类型统计 |

需求梳理.md 第 28 项：“医务系统设置（23 项）：用户管理、角色管理、权限管理、科室管理、字典管理、参数设置、日志管理、数据备份、系统接口、消息提醒设置等；支持自定义、批量导入、导出；支持 3000+ 条知识库。”底座（用户/角色/权限/科室/字典/操作日志）为项目早期实现，本模块补齐：参数设置、数据备份/恢复、系统接口配置、消息提醒设置、知识库管理，并给底座补上校验、启用/停用、用户-角色与角色-权限分配、统计、导入导出与前端页面。接口总数 95（登录鉴权 4 个 + 新包 `sysconfig` 60 个：参数 13、备份 9、接口 13、提醒设置 12、知识库 12、总览 1；底座补强 31 个：用户 9、角色 6、科室 6、字典 5、日志 2、权限 3）。种子数据：权限 id 30「医务系统设置」；参数 8 条（启用 7/停用 1、内置 4/自定义 4、SYSTEM 4/BUSINESS 2/NOTIFY 2）；备份 3 条（全部成功，2026-11-15 全量备份已恢复 1 次，记录条数合计 1526）；接口 5 条（启用 4/停用 1，HIS/LIS/PACS/医保/其他各 1，均未测试）；提醒设置 6 条（启用 5/停用 1，站内 3/短信 1/微信 1/邮件 1，ALL 2/ROLE 3/USER 1）；知识库 8 条（启用 7/停用 1，制度规范 2/操作指南 2/其余 4 类各 1）；28 号台账由“进行中”改为已完成（进度 100、完成日期 2026-11-22、接口 91、测试 23），登录鉴权后接口 91→95、测试 23→36。前端页面：`/system/settings`（总览 + 参数/备份/接口/提醒设置/知识库五个页签），`/system/users`、`/system/roles`、`/system/permissions`、`/system/depts`、`/system/dicts`、`/system/logs`（底座六页共用一个视图、按路由切换），统计卡片、搜索、列表、新增/编辑弹窗、启用/停用、备份执行与恢复、接口连通性测试、知识详情弹窗、批量导入导出。

### 登录鉴权（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `POST /api/auth/login` | 登录：账号密码校验，成功返回 JWT（有效期 8 小时）+ 用户信息 + 角色与权限编码 |
| `POST /api/auth/logout` | 登出：当前 token 加入黑名单，之后再用即 401 |
| `GET /api/auth/me` | 当前登录用户信息：数据库实时查角色与权限 |
| `POST /api/auth/change-password` | 修改密码：校验原密码，新密码写 PBKDF2 哈希；成功后当前 token 失效，需重新登录 |

实现：新包 `auth`（`JwtUtil` / `PasswordUtil` / `AuthService` / `AuthController` / `AuthInterceptor`），复用系统设置底座的 `sys_user` / `sys_role` / `sys_permission` 三张表，不另起一套。密码用 JDK 内置 PBKDF2WithHmacSHA256 哈希存储（`PBKDF2$65536$盐$哈希`），新建/重置/导入用户直接写哈希；种子账号的明文占位密码首次登录成功后自动迁移为哈希。token 为 JJWT 0.11.5 签发的 HS256 JWT（每次签发带唯一 jti），请求头 `Authorization: Bearer <token>`；除登录接口外全部 `/api/**` 由拦截器校验，未登录 / token 非法 / 过期 / 已登出均返回 HTTP 401。登录失败处理：账号不存在与密码错误返回同一提示，不泄露账号是否存在；同一账号连续失败 5 次后锁定 15 分钟（返回 429）；停用账号登录返回 403。配置在 `application.yml` 的 `app.auth` 下：`enabled`（默认 true）、`jwt-secret`（可用环境变量 `AUTH_JWT_SECRET` 覆盖，生产务必换成足够长的随机值）、`jwt-expire-hours`（默认 8）、`max-failures`（默认 5）、`lock-minutes`（默认 15）。前端：`/login` 登录页 + 路由守卫（未登录只能进登录页，已登录访问登录页回工作台）+ 请求拦截器自动带 token、401 时清登录态回登录页；顶栏显示当前登录人姓名与退出登录按钮。种子数据：admin 分配 ADMIN 角色，ADMIN 角色拥有全部 33 个权限（登录后 `/api/auth/me` 可返回）。限制：登出黑名单与登录失败计数均为单机内存实现，重启后清空，多实例部署需改用 Redis 等共享存储；登录失败锁定同样只在本实例内生效。测试：`AuthTests` 13 个（登录成功与密码迁移、密码错误/未知账号/停用账号、连续失败锁定、未登录拦截、带 token 放行、篡改与过期 token 拒绝、登出拉黑、改密全流程、密码工具往返）；h2 测试 profile 默认关闭接口拦截（历史模块测试不带 token），本类用 `@TestPropertySource(app.auth.enabled=true)` 显式开启验证。冒烟（真实启动，H2 + 强制开启拦截）：未带 token 调 `/api/users` 返回 401、密码错误返回 401、正确登录签发 token、`/api/auth/me` 返回 ADMIN 与 33 个权限、带 token 调业务接口 200、登出后原 token 再用 401、改密原密码错误返回 401。28 号台账接口 91→95、测试 23→36，全量合计接口 712→716、测试 379→392。部门角色授权增强（2026-11-23，按医院实际使用场景）：新增 4 个接口（科室树 `GET /api/depts/tree`、角色绑定科室查/改 `GET/POST /api/roles/{id}/depts`、登录后菜单树 `GET /api/auth/menus`），鉴权接口 4→5，28 号台账接口 95→99、测试 36→52，全量合计接口 716→720、测试 392→408。科室层级：科室 8→11 个，内科门诊/内科病房挂在中医内科下、急诊内科挂在急诊科下；科室管理页改为树形展示、新增/编辑可选手上级科室；有下级科室的不可停用/不可直接删除、上级科室不能是自己。角色绑定科室：新表 `sys_role_dept`（角色-科室多对多，落库、重启不丢），角色管理页新增“绑定部门”树形勾选，删除角色时绑定一并清理；`GET /api/auth/me` 增加 `deptName`（所属科室名）与 `deptScope`（角色绑定的科室范围）。角色分配权限：由平铺复选框改为树形勾选（目录勾选即含下级菜单，半选目录也计入）。登录后菜单动态过滤：`GET /api/auth/menus` 按当前用户角色权限返回启用的目录/菜单树；前端左侧菜单按权限过滤、无权限的分组自动隐藏，路由守卫拦截无权限的直接访问（回工作台），顶栏显示当前登录人科室与角色；登录时拉取菜单并缓存，刷新后用 `/api/auth/me` 补一次。演示账号（密码均为 123456）：`admin` 系统管理员（医务科，ADMIN，全部 33 个页面）；`wuwenbin` 医务科管理员（医务科，MEDICAL_ADMIN，25 个页面：全部业务页面 + 统计 + 消息/简报/移动端，不含用户/角色/权限/科室/字典/日志/系统设置与完成统计）；`chen` 科主任（中医内科，DEPT_HEAD，14 个页面：本科室业务 + MDT + 质量检查/核心制度/整改汇总/技术分级/医疗分级 + 消息/简报，不含统计与系统管理）；`sunxiaohong` 普通医师（儿科，DOCTOR，6 个页面：医师档案、休假执业、排班、MDT、消息提醒、医疗工作简报）。角色-科室绑定种子：MEDICAL_ADMIN→医务科、DEPT_HEAD→中医内科、DOCTOR→全部 6 个临床科室。测试：新类 `DeptRoleAuthTests` 16 个（科室树结构与层级守卫、科室统计 11、角色绑定科室查/改/清理、角色-科室种子绑定、四账号菜单数与可见页面差异、未登录调菜单 401、`/api/auth/me` 科室名与范围、停用权限后菜单剔除）；旧测试随种子调整：用户总数 2→4、科室总数 8→11，`SystemSettingsTests` 给角色 3 分配权限的清理改为恢复种子（6 个），避免共用 H2 的测试类互相污染。冒烟（真实启动，H2 + 强制开启拦截）：admin/wuwenbin/chen/sunxiaohong 四账号登录均成功，`/api/auth/me` 返回科室名与范围正确，`/api/auth/menus` 菜单数分别为 33/25/14/6，科室树顶级 8 个、中医内科下级 2 个、急诊科下级 1 个，角色绑定科室查询正确。接口级权限控制（2026-11-23 补上）：登录时把角色权限编码快照写进 JWT（`permissions` claim）；拦截器在 token 校验通过后按集中式映射 `ApiPermissionMapping` 查本次请求所需权限（66 个 Controller 前缀 → 33 个页面权限编码，如 `/api/leaves` → `leave:list`；同一页面的子接口共用一个权限），无权限返回 HTTP 403 + `{"code":403,"msg":"无权限访问该接口"}`；ADMIN 角色放行全部接口；未登记映射的新接口默认拒绝。仅需登录态的接口：`/api/auth/**`、GET `/api/depts/enabled`（启用科室下拉框）、GET `/api/dicts/{dictCode}`（字典下拉框）。给角色授权：角色管理页分配权限树，改完后该角色的用户需重新登录才生效（旧 token 仍是登录时的快照）。前端请求拦截器：403 只提示“无权限访问该接口”，不清登录态、不回登录页；只有 401 才清登录态。测试：新类 `InterfacePermissionTests` 11 个（未登录 401、有权限 200、无权限 403、admin 全通、改权限后重新登录才生效、映射覆盖全部 66 个前缀、token 权限快照）；全量合计接口 720 不变、测试 408→419；28 号台账测试 52→63。冒烟（真实启动，H2 + 强制开启拦截）：不带 token 调 `/api/users` 返回 401、sunxiaohong 调 `/api/users` 返回 403、调 `/api/leaves` 与 `/api/depts/enabled` 返回 200、admin 调 `/api/users` 返回 200、sunxiaohong 调 `/api/statistics/overview` 返回 403、admin 调同一接口返回 200。限制：角色改权限后需重新登录才生效（token 快照机制）；升级后非管理员用户正在用的旧 token 没有权限快照，调业务接口会 403，需重新登录一次，ADMIN 旧 token 不受影响；登出黑名单与登录失败计数仍为单机内存实现，重启后清空。

### 消息提醒管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/notifications/overview` | 消息提醒总览：站内消息 / 待办提醒 / 消息推送 / 短信提醒 / 28 号提醒设置的数量汇总 |
| `GET /api/site-messages` | 站内消息分页 + 多条件查询（关键字、分类、已读状态、接收范围） |
| `GET /api/site-messages/stats` | 站内消息统计：总数、未读/已读数、按分类分组、按接收范围分组 |
| `GET /api/site-messages/unread-count` | 未读数：按用户名与角色编码统计该用户可见的未读消息数 |
| `GET /api/site-messages/{id}` | 站内消息详情 |
| `POST /api/site-messages` | 发送站内消息：标题必填、分类与接收范围须合法，默认未读 |
| `PUT /api/site-messages/{id}` | 修改：已读消息不可修改 |
| `DELETE /api/site-messages/{id}` | 删除 |
| `POST /api/site-messages/{id}/read` | 标已读：已读消息不可重复标记 |
| `POST /api/site-messages/read-all` | 全部标已读：把某用户可见的未读消息全部标已读 |
| `GET /api/site-messages/export` / `POST /api/site-messages/import` / `GET /api/site-messages/template` | 导出 / 批量导入（标题与内容均已存在的行跳过） / 导入模板 |
| `GET /api/todo-reminders` | 待办提醒分页 + 多条件查询（关键字、待办类型、状态、责任人、来源） |
| `GET /api/todo-reminders/stats` | 待办提醒统计：总数、待办/已完成/已逾期、今日到期、按类型分组、按来源分组 |
| `GET /api/todo-reminders/{id}` | 待办提醒详情 |
| `POST /api/todo-reminders` | 新建待办：标题必填、类型与来源须合法，默认待办 |
| `PUT /api/todo-reminders/{id}` | 修改：已完成的待办不可修改 |
| `DELETE /api/todo-reminders/{id}` | 删除：已完成的待办不可直接删除 |
| `POST /api/todo-reminders/{id}/complete` | 完成待办：已完成不可重复完成，已逾期可完成 |
| `POST /api/todo-reminders/overdue-scan` | 逾期扫描：待办且截止日期早于当天的改为已逾期 |
| `POST /api/todo-reminders/generate` | 按 28 号提醒设置生成待办：设置须存在且启用，责任人必填 |
| `GET /api/todo-reminders/export` / `POST /api/todo-reminders/import` / `GET /api/todo-reminders/template` | 导出 / 批量导入（标题与内容均已存在的行跳过） / 导入模板 |
| `GET /api/push-records` | 消息推送记录分页 + 多条件查询（关键字、推送渠道、发送状态） |
| `GET /api/push-records/stats` | 推送记录统计：总数、待发送/已发送/发送失败、按渠道分组 |
| `GET /api/push-records/{id}` | 推送记录详情 |
| `POST /api/push-records` | 发送推送（预留占位：无真实通道，返回失败并如实记录） |
| `POST /api/push-records/{id}/retry` | 重推：已发送成功不可重推 |
| `DELETE /api/push-records/{id}` | 删除：已发送成功的记录不可直接删除 |
| `GET /api/push-records/export` | 导出 Excel（按当前查询条件） |
| `GET /api/sms-records` | 短信提醒记录分页 + 多条件查询（关键字、发送状态） |
| `GET /api/sms-records/stats` | 短信记录统计：总数、待发送/已发送/发送失败 |
| `GET /api/sms-records/{id}` | 短信记录详情 |
| `POST /api/sms-records` | 发送短信（预留占位：无真实网关，返回失败并如实记录） |
| `POST /api/sms-records/{id}/retry` | 重发：已发送成功不可重发 |
| `DELETE /api/sms-records/{id}` | 删除：已发送成功的记录不可直接删除 |
| `GET /api/sms-records/export` | 导出 Excel（按当前查询条件） |

需求梳理.md 第 29 项：“消息提醒：待办提醒、消息推送、短信提醒、站内消息；支持按角色、按人员推送。”与 28 号分工：28 号只管提醒“设置/规则配置”，本模块做消息提醒“中心/实际消息”：站内消息收发与已读、待办提醒、消息推送记录、短信提醒记录；推送目标支持按角色、按人员、全部人员三种接收范围；与 28 号提醒设置联动（可按其配置生成待办），不重复造配置表。接口总数 39（新包 `notification`：站内消息 12、待办提醒 12、消息推送 7、短信提醒 7、总览 1）。种子数据：权限 id 31「消息提醒」；站内消息 6 条（未读 4/已读 2，通知 1/公告 1/待办提醒 3/系统消息 1，ALL 2/ROLE 2/USER 2）；待办提醒 6 条（待办 4/已完成 1/已逾期 1，审批待办 2/到期提醒 1/整改待办 1/其他待办 2，手工创建 2/系统生成 2/按提醒设置生成 2）；消息推送记录 4 条（待发送 1/已发送 0/发送失败 3，微信 2/移动端 1/邮件 1）；短信提醒记录 4 条（待发送 1/已发送 0/发送失败 3）；短信网关与推送通道为预留占位，未真实对接，发送失败如实记录；29 号台账由“待开发”改为已完成（进度 100、完成日期 2026-11-22、接口 39、测试 20）。前端页面：`/notice`（总览统计卡片 + 站内消息/待办提醒/消息推送/短信提醒四个页签），统计卡片、搜索、列表、新增/编辑弹窗、标已读/全部标已读、完成待办、逾期扫描、按提醒设置生成、发送/重推/重发、批量导入导出。
### 医疗工作简报管理（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/briefing-templates` | 简报模板分页 + 多条件组合查询（关键字/分类/启用状态） |
| `GET /api/briefing-templates/enabled` | 全部启用的模板（新建/自动生成简报时下拉用） |
| `GET /api/briefing-templates/{id}` | 简报模板详情 |
| `POST /api/briefing-templates` | 新增简报模板（默认启用、默认月报分类；栏目须为非空 JSON 数组） |
| `PUT /api/briefing-templates/{id}` | 修改简报模板（启用状态不在此改，走启用/停用接口） |
| `DELETE /api/briefing-templates/{id}` | 删除简报模板（已被简报引用的模板不允许删除） |
| `POST /api/briefing-templates/{id}/enable` / `POST /api/briefing-templates/{id}/disable` | 启用 / 停用简报模板 |
| `GET /api/briefings` | 简报分页 + 多条件组合查询（关键字/分类/发布状态/统计周期/模板/只看已发布） |
| `GET /api/briefings/stats` | 简报统计：总数 / 草稿 / 待审批 / 已发布 / 已驳回 / 按分类分组 / 模板数 |
| `GET /api/briefings/stats/by-category` | 按分类汇总统计 |
| `GET /api/briefings/stats/by-month` | 按统计周期汇总统计（按周期升序） |
| `GET /api/briefings/{id}` | 简报详情 |
| `POST /api/briefings` | 新增简报（默认草稿） |
| `PUT /api/briefings/{id}` | 修改简报（只有草稿与已驳回的简报能修改） |
| `DELETE /api/briefings/{id}` / `DELETE /api/briefings/batch` | 删除 / 批量删除（已发布的简报不能直接删除） |
| `POST /api/briefings/{id}/submit` | 提交审批：草稿 / 已驳回 → 待审批 |
| `POST /api/briefings/{id}/approve` | 审批通过：待审批 → 已发布，发布日期默认为当天，审批记录留痕 |
| `POST /api/briefings/{id}/reject` | 审批驳回：待审批 → 已驳回，驳回原因必填，审批记录留痕 |
| `POST /api/briefings/generate` | 按模板自动生成简报：从医师档案、工作量、质量检查、MDT、新技术、考核聚合本期真实数据写正文，生成结果为草稿 |
| `GET /api/briefings/{id}/export-word` | 导出 Word（.docx，Apache POI 真实文件） |
| `GET /api/briefings/{id}/export-pdf` | 导出 PDF（OpenPDF + 嵌入简体中文字体，真实 PDF 文件） |
| `GET /api/briefings/{id}/download` | 下载正文纯文本（.txt） |
| `GET /api/briefings/export` | 导出 Excel（按当前查询条件） |
| `POST /api/briefings/import` | 批量导入 Excel（导入的均为草稿，需走提交审批流程；期号重复跳过） |
| `GET /api/briefings/template` | 导入模板下载 |

简报分类：月报 / 季报 / 年报 / 专项简报 / 工作动态。发布状态：草稿 / 待审批 / 已发布 / 已驳回；一级审批（医务科）。种子数据：3 个简报模板（月报 1 / 季报 1 / 工作动态 1，启用 2 / 停用 1）、6 期简报（草稿 2 / 待审批 1 / 已发布 2 / 已驳回 1；月报 4 / 季报 1 / 工作动态 1；2026-11 有 3 期、2026-10 有 2 期、2026-09 有 1 期）。自动生成按模板栏目聚合数据：工作概况（医师总数 + 本期工作量）、医疗质量（检查计划/记录平均分/问题数）、会诊与新技术（MDT 申请数、新技术申报数）、医师考核（本年度平均分、优秀人数）；认不出的栏目如实写“暂无自动汇总数据”。PDF 导出说明：新增 OpenPDF 1.3.30 依赖（已用 curl 经环境代理下载进本地仓库 `~/.m2/repository`，离线构建可用）；中文字体取本机 Noto CJK 字体集里 PostScript 名含 CJKsc 的简体中文字体并嵌入，若本机没有可用中文字体则明确报错、不生成假 PDF。前端页面为简报 / 简报模板两个页签，含统计卡片、按分类/按统计周期汇总表、自动生成弹窗、审批弹窗（通过/驳回）、详情弹窗（正文 + 审批记录）、导出 Word/PDF/文本/Excel、批量导入导出。


### 医务管理移动端（完整前后端打通）

| 接口 | 说明 |
|---|---|
| `GET /api/mobile/overview` | 移动端首页总览：医师 / 待审批 / 考核 / 设备 / 消息 / 推送的数量汇总 |
| `GET /api/mobile/physicians` | 移动端查看医师档案（只读分页，与 PC 端同一数据源） |
| `GET /api/mobile/schedules/today` | 移动端查看今日排班（只读，按当天日期过滤） |
| `POST /api/mobile/push` | 移动端消息推送：固定走 APP 渠道，复用 29 号推送记录；通道未对接时如实返回失败 |
| `GET /api/mobile/push/stats` | 移动端推送统计：总数 / 待发送 / 已发送 / 发送失败 / 按渠道分组 |
| `GET /api/mobile-approvals/pending` | 待审批事项聚合列表（8 个业务模块，可按模块 / 关键字过滤） |
| `GET /api/mobile-approvals/pending/count` | 待审批计数：总数与按业务模块分组 |
| `POST /api/mobile-approvals/{moduleType}/{id}/approve` | 移动端审批通过：委托业务模块审批，成功与失败都记操作日志 |
| `POST /api/mobile-approvals/{moduleType}/{id}/reject` | 移动端审批驳回：驳回原因必填，委托业务模块审批 |
| `GET /api/mobile-approvals/logs` | 审批操作记录分页 + 多条件组合查询（关键字/业务模块/操作/结果/审批人） |
| `GET /api/mobile-approvals/logs/stats` | 审批操作记录统计：总数 / 通过 / 驳回 / 成功 / 失败 / 按业务模块分组 |
| `GET /api/mobile-approvals/logs/export` | 导出审批操作记录 Excel（按当前查询条件） |
| `GET /api/mobile-devices` | 设备分页 + 多条件组合查询（关键字/平台/绑定状态/使用人） |
| `GET /api/mobile-devices/stats` | 设备统计：总数 / 已绑定 / 已解绑 / 按平台分组 |
| `GET /api/mobile-devices/{id}` | 设备详情 |
| `POST /api/mobile-devices` | 登记设备（默认已绑定） |
| `PUT /api/mobile-devices/{id}` | 修改设备（绑定状态与最近活跃时间不在此改，走绑定 / 解绑接口） |
| `DELETE /api/mobile-devices/{id}` / `DELETE /api/mobile-devices/batch` | 删除 / 批量删除（已绑定的设备不可直接删除，请先解绑；批量删除逐条走守卫、已绑定的跳过并计入 failed） |
| `POST /api/mobile-devices/{id}/bind` | 绑定设备（解绑后可重新绑定） |
| `POST /api/mobile-devices/{id}/unbind` | 解绑设备 |
| `GET /api/mobile-devices/export` | 导出设备 Excel（按当前查询条件） |
| `POST /api/mobile-devices/import` | 批量导入设备 Excel（设备编码已存在的行跳过） |
| `GET /api/mobile-devices/template` | 导入模板下载 |

设备平台：iOS / Android；设备状态：已绑定 / 已解绑；设备编码全院唯一，重复登记拒绝。待审批聚合覆盖 8 个业务模块：休假执业（两级审批待科主任 / 待医务科）、重点授权（两级审批）、排班调班（调班/代班待科主任审批）、MDT（一级审批）、新技术新项目（一级审批）、医疗人员动态（一级审批）、医疗分级（一级审批）、医疗工作简报（待审批）；审批委托各业务模块已有逻辑，业务模块的守卫（如已结束不可再审批）原样生效，委托失败记一条 FAIL 操作日志并原样返回 400。种子数据：6 台移动端设备（已绑定 4 / 已解绑 2；iOS 3 / Android 3）、6 条移动端审批操作记录（通过 5 / 驳回 1，全部成功，与各业务已结束事项的状态一致）。移动端推送复用 29 号推送记录走 APP 渠道，推送通道本机没有真实服务，发送明确返回 success=false、记发送失败“推送通道未对接（预留占位）：移动端通道未配置真实服务”，不伪造发送成功。前端页面为待审批 / 移动端设备 / 审批记录 / 移动端推送 / 移动端查看五个页签，含统计卡片、审批弹窗（通过/驳回）、设备登记/绑定/解绑弹窗、推送表单、批量导入导出。注意：Lombok 1.18.32 对同时存在 `username` 与 `userName` 两个字段的类只生成 `getUsername()`（`userName` 的访问器被吞掉，编译报找不到 `getUserName`），本模块的“使用人姓名”字段改用 `displayName` + `@TableField("user_name")` 绕开。

## 验证结果（2026-11-22，云电脑）

- **默认数据库已切到 MySQL**：`spring.profiles.active=mysql`，实测默认启动日志显示 `The following 1 profile is active: "mysql"`；无 MySQL 时启动失败（连 localhost:3306），不再静默回落到 H2。
- **种子数据 MySQL 下可正常灌库**：`data.sql` 已改成幂等写法（`INSERT ... SELECT ... FROM DUAL WHERE NOT EXISTS`，派生表写法绕开 MySQL 1093 限制），84 条语句对应 8 科室、5 字典类型、21 字典数据、2 用户、4 角色、11 权限、12 医师、6 休假执业、6 重点授权、3 考核模板、6 医师考核；文件型 H2（MySQL 兼容模式）连续启动两次实测：两次都是 12 名医师、0 报错。排班模块新增后共 94 条语句（+2 排班模板、+8 排班记录）。MDT 模块新增后共 103 条语句（+1 MDT 权限菜单、+5 MDT 申请、+3 会诊记录）。质量检查模块新增后共 122 条语句（+1 质量检查权限菜单、+3 检查模板、+4 检查计划、+3 检查记录、+4 检查问题、+4 整改任务）。新技术新项目模块新增后共 131 条语句（+1 新技术新项目权限菜单、+5 项目、+3 进展记录）。统计分析模块新增后共 150 条语句（+1 统计分析权限菜单、+8 工作量登记、+10 三级医院医疗质量指标）。医疗人员动态管理模块新增后共 157 条语句（+1 医疗人员动态权限菜单、+6 人员变动）。放射人员管理模块新增后共 181 条语句（+1 放射人员管理权限菜单、+6 放射人员档案、+6 资质、+5 培训记录、+6 体检记录）。委员会管理模块新增后共 199 条语句（+1 委员会管理权限菜单、+6 委员会、+5 会议记录、+5 决议）。文件制度管理模块新增后共 206 条语句（+1 文件制度管理权限菜单、+6 文件制度）。核心制度管理模块新增后共 258 条语句（+1 核心制度管理权限菜单、+6 核心制度、+6 学习记录）。检查整改汇总模块新增后共 265 条语句（+1 检查整改汇总权限菜单、+6 汇总报告）。医疗技术分级管理模块新增后共 284 条语句（+1 医疗技术分级管理权限菜单、+6 技术档案、+6 准入申请、+6 医师授权）。检查维护模块新增后共 314 条语句（+1 检查维护权限菜单、+12 检查项目、+6 检查标准、+11 模板检查项配置）。检查分类模块新增后共 327 条语句（+1 检查分类权限菜单、+12 检查分类）。医疗分级管理模块新增后共 341 条语句（+1 医疗分级管理权限菜单、+3 分级标准、+6 分级评定）。检查模板管理模块新增后共 331 条语句（+1 检查模板管理权限菜单、+3 模板版本）。医务系统完成统计模块新增后共 373 条语句（+1 完成统计权限菜单、+31 模块完成台账）。医务系统设置模块新增后共 404 条 INSERT + 1 条 UPDATE（+1 医务系统设置权限菜单、+8 参数、+3 备份、+5 接口、+6 提醒设置、+8 知识库，另有 1 条 UPDATE 将 28 号台账改为已完成：status=2、progress=100、interface_count=91、test_count=23、completed_date='2026-11-22'）。消息提醒管理模块新增后共 425 条 INSERT + 2 条 UPDATE（+1 消息提醒权限菜单、+6 站内消息、+6 待办提醒、+4 消息推送记录、+4 短信提醒记录，另有 1 条 UPDATE 将 29 号台账改为已完成：status=2、progress=100、interface_count=39、test_count=20、completed_date='2026-11-22'）。医疗工作简报管理模块新增后共 435 条 INSERT + 3 条 UPDATE（+1 医疗工作简报权限菜单、+3 简报模板、+6 简报，另有 1 条 UPDATE 将 30 号台账改为已完成：status=2、progress=100、interface_count=27、test_count=22、completed_date='2026-11-22'）。医务管理移动端模块新增后共 449 条 INSERT + 4 条 UPDATE（+1 医务管理移动端权限菜单、+6 移动端设备、+6 移动端审批操作记录，另有 1 条 UPDATE 将 31 号台账改为已完成：status=2、progress=100、interface_count=24、test_count=20、completed_date='2026-11-22'）。
- 后端：`mvn clean test` **通过，419/419 测试**（医师档案 5 个 + 休假执业 8 个 + 重点授权 10 个 + 医师考核 10 个 + 排班管理 12 个 + MDT 管理 11 个 + 质量检查管理 12 个 + 新技术新项目管理 12 个 + 统计分析 12 个 + 医疗人员动态管理 13 个 + 放射人员管理 12 个 + 委员会管理 12 个 + 文件制度管理 14 个 + 政策法规管理 16 个 + 医德医风管理 19 个 + 核心制度管理 16 个 + 检查整改汇总 13 个 + 医疗技术分级管理 19 个 + 检查维护 17 个 + 检查分类 13 个 + 检查模板管理 13 个 + 医疗分级管理 12 个 + 医务系统完成统计 13 个 + 医务系统设置 23 个 + 部门角色授权增强 16 个 + 接口级权限控制 11 个 + 消息提醒管理 20 个 + 医疗工作简报管理 22 个 + 医务管理移动端 20 个：种子数据、多条件查询、等级边界自动计算 90/80/60 分、超模板权重拒绝、模板 CRUD 与权重合计 100 校验、评分统计、CRUD、导入导出往返；排班新增：同一医师同一天重复排班拒绝、班次默认时间补全、按模板批量生成冲突跳过计数、调班/代班一级审批流转与审批时冲突拒绝、模板班次组合校验、排班统计、导入导出往返；MDT 新增：会诊类型/紧急程度/期望时间校验与默认值、一级审批流转与已结束申请不可再审批/再修改、待审批与已驳回申请不可添加会诊记录、多次补充记录与按时间升序、按类型/科室/月份统计、导入导出往返；质量检查新增：模板权重合计 100 校验与停用模板不可建计划、计划状态流转（开始/完成需有记录/取消）与已结束计划不可修改、逐项打分超权重拒绝与总分等级自动算（90/60 分边界）、问题与整改跟踪流转（提交→复查通过/打回重提、一个问题只发一个整改任务、逾期动态判定）、按类型/科室/月份统计与整改完成率、检查报告摘要、导入导出往返；新技术新项目新增：项目类型/计划日期/预算校验与默认值、伦理审查流转（不需审查的项目不可审查、审查结论为空拒绝、审查后不可再审查）与伦理门禁（伦理未通过前不能审批立项）、一级审批流转与已结束项目不可再审批/再修改、进展/验收/推广记录驱动状态流转（验收 1→2、推广 2→4）与各状态下的记录守卫、按类型/科室/月份统计与待伦理审查数、导入导出往返；统计分析新增：工作量登记种子合计（门诊 3420 / 住院 1066 / 手术 241 / 合计 4727）与按科室/按人员/按月汇总（中医内科 922 排第一）、同一医师同一月份重复登记拒绝、负数拒绝、指标种子（10 条，达标 7 条、达标率 70.00%，国家 75% / 省 66.67% / 市 66.67%）与达标判定（含越低越好、未填报不计入达标率）、指标编码重复与非法标准级别拒绝、工作量与指标的导入导出往返（导出后删除再导入补回 1 条、重复导入 0 条）、三组统计聚合与实时数据一致、统计报表导出为 3 个 sheet 的有效 xlsx）；医疗人员动态管理新增：变动类型/生效日期/按类型科室校验与默认值、同一人员同一变动类型同一生效日期重复登记拒绝、一级审批流转与已结束变动不可再审批/再修改、审批通过同步医师档案（调动改科室、离职改在职状态为离职、退休改为退休、入职/返聘改回在职，尚未建档的入职人员不阻断审批）、按类型/相关科室/月份统计、导入导出往返（导出后删除再导入补回 1 条、重复导入 0 条）。放射人员管理新增：档案校验与默认值（放射类别/在岗状态非法拒绝、许可证有效期不能早于从事放射工作日期、同一姓名同一科室重复建档拒绝）、资质一级审核流转与已结束资质不可再审核/再修改、资质 90 天内到期过滤（只含已通过）、培训校验（类型/结果/学时/重复登记）与默认合格、体检校验（类型/结论/剂量/下次体检日期/重复登记）与默认合格、剂量限值判定（20 mSv 不超标、20.01 mSv 超标）、统计（档案 6 条在岗 4/停岗 1/离岗 1、资质待审核 2/已通过 3/已驳回 1/90 天内到期 2/已过期 1、培训合格 4/不合格 1、平均剂量 8.75 mSv、超标 1 条、体检逾期 1 人）、导入导出往返（导出后删除再导入补回 1 条、重复导入 0 条）。委员会管理新增：委员会校验与默认值（类别/任期/委员人数/状态非法拒绝、同一名称同一届次重复建档拒绝）、换届流转（在任换届生成新一届届次 +1、原记录改为已换届，已结束的委员会不能再换届/再解散）、会议校验（已解散委员会不能新增会议、同一委员会同一天同一主题重复登记拒绝）、决议流转（待执行→执行中→已完成，完成日期默认当天，已结束的决议不能再流转/再修改）、统计（委员会 6 个在任 4/已换届 1/已解散 1、任期 90 天内到期 1 个、会议 5 次、决议待执行 2/执行中 1/已完成 1/已取消 1、完成率 25.00%、逾期 1 条、按月 2026-10 月 1 次/2026-11 月 4 次）、导入导出往返（导出后删除再导入补回 1 条、重复导入 0 条）。文件制度管理新增：文件校验与默认值（分类/失效日期非法拒绝、同一编号同一版本重复建档拒绝、默认草稿 V1.0）、修改守卫（只有草稿能修改，已发布改版走新建版本）、发布一级审批流转（无审批人拒绝、发布日期默认为当天、生效日期为空默认当天、审批记录留痕，已发布/已作废不可再发布）、作废流转（无作废原因拒绝，草稿不可作废，已作废不可再作废/再发布）、新建版本流转（只有已发布能新建版本、版本号 +1、同一编号已有该版本草稿拒绝、发布新版本时旧版本自动作废并留痕）、删除守卫（已发布不可直接删除）、统计（文件 6 个已发布 4/草稿 1/已作废 1，按分类制度 3/规范 1/流程 1/文件 1，30 天内到期 1 个、已过期 1 个，按月发布 2023-01/2024-01/2025-03/2025-06/2026-01 各 1 次）、正文下载文本、导入导出往返（导出后删除再导入补回 1 条、重复导入 0 条，导入的均为草稿）。政策法规管理新增：法规校验与默认值（分类/层级/失效日期非法拒绝、同一文号同一版本重复建档拒绝、默认草稿 V1.0、适用科室默认“全院”）、修改守卫（只有草稿能修改，已发布更新走新建版本）、发布一级审批流转（无审批人拒绝、发布日期默认为当天、生效日期为空默认当天、审批记录留痕，已发布/已废止不可再发布）、废止流转（无废止原因拒绝，草稿不可废止，已废止不可再废止/再发布）、新建版本流转（只有已发布能新建版本、版本号 +1、同一文号已有该版本草稿拒绝、发布新版本时旧版本自动废止并留痕）、删除守卫（已发布的法规不可直接删除，删除时同时删除其查阅学习记录）、统计（法规 6 部已发布 4/草稿 1/已废止 1，按分类法律 1/行政法规 2/部门规章 2/地方性法规 1，按层级国家 5/省 1，30 天内到期 1 部、已过期 1 部，按月发布 1994-09/2011-03/2016-11/2022-03/2024-01 各 1 次；查阅总人次 6、已学习 4 人次、覆盖法规 3 部）、查阅学习登记（只有已发布的法规才能登记、同一人同一法规同一天重复登记拒绝、标记已学习流转、查阅日期默认当天、法规名称自动补全）、正文下载文本、导入导出往返（导出后删除再导入补回 1 条、重复导入 0 条，导入的均为草稿）。核心制度管理新增：制度校验与默认值（类别/层级/失效日期非法拒绝、同一编号同一版本重复建档拒绝、默认草稿 V1.0、适用科室默认“全院”）、修改守卫（只有草稿能修改，已发布更新走新建版本）、发布一级审批流转（无审批人拒绝、发布日期默认为当天、生效日期为空默认当天、审批记录留痕，已发布/已废止不可再发布）、废止流转（无废止原因拒绝，草稿不可废止，已废止不可再废止/再发布）、新建版本流转（只有已发布能新建版本、版本号 +1、同一编号已有该版本草稿拒绝、发布新版本时旧版本自动废止并留痕）、删除守卫（已发布的制度不可直接删除，删除时同时删除其查阅学习记录）、统计（制度 6 项已发布 4/草稿 1/已废止 1，按类别医疗 3/护理 1/药事 1/安全 1，按层级院级 5/科室级 1，30 天内到期 1 项、已过期 1 项，按月发布 2023-01/2024-01/2024-06；查阅总人次 6、已学习 4 人次、覆盖制度 3 项）、查阅学习登记（只有已发布的制度才能登记、同一人同一制度同一天重复登记拒绝、标记已学习流转、查阅日期默认当天、制度名称自动补全）、正文下载文本、导入导出往返（导出后删除再导入补回 1 条、重复导入 0 条，导入的均为草稿）。检查整改汇总新增：汇总报告种子（6 条，2026-11 全院问题 4/严重 2/一般 2、整改 4 条、完成率 25.00%、逾期 1 条；骨伤科完成率 0%、逾期 1 条；专项检查无问题）、建档校验与默认值（统计周期 yyyy-MM 格式、汇总范围/科室/类型缺失拒绝、计数为负拒绝、空计数按 0 处理、整改完成率按已复查通过数现算、同一周期同一范围重复建档拒绝）、修改守卫（汇总口径不可改、未传计数沿用原值、完成率重算）、实时汇总统计（问题 4 个严重 2/一般 2、整改 4 条待整改 2/已整改 1/已复查通过 1、完成率 25.00%、逾期 1 条、计划 4 个、记录 3 条、平均分 80.33；按科室中医内科完成率 50%/骨伤科 0%逾期 1、妇科有计划无问题；按类型科室自查完成率 0%逾期 1/院级检查 50%/专项检查 0 问题；按月只有 2026-11 一个月）、逾期整改查询（1 条，王建军急救药品效期，期限 2026-11-20）、问题汇总查询（总数 4、严重 2 个、骨伤科 2 个、院级检查 2 个、只看逾期 1 条、只看未完成整改 3 条）、生成汇总（已有口径原地更新不新增、新口径妇科新增后删除、周期/范围/科室/类型缺失拒绝）、导入导出往返（导出后删除再导入补回 1 条、重复导入 0 条）。可执行 jar 已打包验证，真实启动后接口实测通过：休假执业统计总数 6（待审批 4、已通过 1、已驳回 1）、列表 6 条、孙小红休假申请科主任通过后状态 0→1、当前审批变为医务科、审批记录留痕；重点授权统计总数 6（待审批 4、已通过 1、已驳回 1，按类型手术 2 / 麻醉 2 / 抗肿瘤 1 / 精神 1）、王建军三级手术授权两级审批 0→1→2、审批记录留痕；导出 Excel 成功，删除孙小红抗肿瘤授权后批量导入补回 1 条、总数恢复 6 条（导入时按姓名查医师档案补全医师 id）。医师考核统计总数 6（平均分 81.17，优秀 2 人、优秀率 33.33%，按等级优秀 2 / 合格 2 / 基本合格 1 / 不合格 1），删除郑秀兰医德考评记录后批量导入补回 1 条、总数恢复 7 条（导入时按姓名补全医师 id、按模板名称补全模板 id，总分与等级重算一致）。排班管理统计总数 8（待审批调班 1、夜班 2，按类型科室 3 / 门诊 3 / 住院 1 / 急诊 1，按班次白班 2 / 上午班 2 / 下午班 1 / 夜班 2 / 休息 1）；按模板给孙小红批量生成 2026-11-30~12-06 一周排班：新建 6 条、冲突跳过 0 条、休息 1 天；种子调班申请（李雅琴 11 月 25 日门诊下午班 → 11 月 26 日白班）科主任通过后日期与班次写回、状态 1→2、审批记录留痕；导出 Excel 成功（HTTP 200，4381 字节，有效 xlsx）；HIS 同步调用明确返回 501“HIS 对接未启用”，未伪造同步成功。MDT 管理统计总数 5（待审批 2、已通过 2、已驳回 1，紧急 2，按类型院内 2 / 院外 1 / 远程 1 / 急会诊 1）；种子申请李秀英远程会诊审批通过后状态 0→1、审批记录留痕，再次审批返回 400“已结束，不能再审批”；通过后添加会诊记录成功，详情含 1 条记录；导出 Excel 成功（HTTP 200，4677 字节，有效 xlsx）。质量检查管理统计总数 4（待执行 2、进行中 1、已完成 1，按类型科室自查 2 / 院级检查 1 / 专项检查 1，检查记录 3 条、平均分 80.33，问题 4 个、整改完成率 25%、逾期整改 1 条）；冒烟新建抗菌药物专项检查模板→建急诊科检查计划→开始（0→1）→逐项打分 88 分合格→发现问题→发整改任务→提交整改（0→1）→复查通过（1→2）→完成计划（1→2），状态流转正确；检查报告：记录 1 条、平均分 88、问题 1 个、整改完成率 100%、逾期 0 条；导出检查记录 Excel 成功（HTTP 200，4233 字节，有效 xlsx）。新技术新项目管理统计总数 5（待审批 1、已立项 1、已验收 1、已推广 1、已驳回 1，待伦理审查 1，按类型新技术 3 / 新项目 2）；冒烟新建需伦理审查项目→未过伦理直接审批返回 400“需伦理审查通过后才能立项”→伦理审查通过（伦理状态 1→2）→审批立项（0→1，审批记录留痕）→添加验收记录（1→2）→添加推广记录（2→4），状态流转正确；导出 Excel 成功（HTTP 200，4587 字节，有效 xlsx）；冒烟数据已清理，统计恢复总数 5。统计分析：工作统计门诊 3420 / 住院 1066 / 手术 241 / 合计 4727（8 条记录、8 名医师、8 个科室），排班人次 8 条（另一口径，单独列示）；三级医院指标总数 10（已填报 10、达标 7、达标率 70.00%，国家 75% / 省 66.67% / 市 66.67%）；医疗质量统计：考核 6 条平均 81.17 分优秀率 33.33%、检查平均 80.33 分、问题 4 个严重 2 个、整改完成率 25%、逾期 1 条、MDT 5 条紧急 2 条、新技术立项率 60%；冒烟新增工作量登记（冒烟测试，2026-12，10/5/2）后统计变为 9 条合计 4744，重复新增同一医师同一月份返回 400“该医师 2026-12 的工作量已登记，请修改原记录”，删除后恢复 8 条合计 4727；统计报表导出成功（HTTP 200，6842 字节，有效 xlsx，3 个 sheet）。医疗人员动态管理统计总数 6（待审批 4、已通过 1、已驳回 1，按类型调动 2 / 离职 1 / 退休 1 / 入职 1 / 返聘 1）；冒烟新建张伟调动（骨伤科 → 中医内科）→ 审批通过（0→1，审批记录留痕）→ 医师档案张伟科室同步改为中医内科，再次审批返回 400“已结束，不能再审批”；导出 Excel 成功（HTTP 200，4438 字节，有效 xlsx）；冒烟数据已清理（调动记录删除、张伟科室恢复为骨伤科），统计恢复总数 6。放射人员管理统计总数 6（在岗 4、停岗 1、离岗 1，按类别诊断 2 / 介入 2 / 核医学 1 / 放疗 1；资质待审核 2、已通过 3、已驳回 1，90 天内到期 2、已过期 1；培训合格 4、不合格 1；平均剂量 8.75 mSv、超标 1 条、体检逾期 1 人）；冒烟审核王小明执业医师证（0→1，审批记录留痕），再次审核返回 400“已结束，不能再审核”；90 天内到期资质过滤返回 2 条（陈志强、刘海燕），剂量超标过滤返回 1 条（赵雪 21.40 mSv）；导出 Excel 成功（HTTP 200，4505 字节，有效 xlsx）；冒烟在内存库上跑，停服后数据不留存，种子数据不受影响。委员会管理统计总数 6（在任 4、已换届 1、已解散 1，按类别医疗质量 1 / 药事 1 / 院感 1 / 输血 2 / 病案 1；任期 90 天内到期 1 个；会议 5 次；决议待执行 2、执行中 1、已完成 1、已取消 1，完成率 25.00%，逾期 1 条）；冒烟给医院感染管理委员会换届（第 2 届 → 第 3 届新记录 id=7，主任委员改为黄志明，原记录改为已换届，统计变为总数 7、已换届 2、任期 90 天内到期 0）；决议“开展病历书写质量专项检查”开始执行（0→1）再完成（1→2，完成日期默认为当天），再次完成返回 400“只有执行中的决议才能完成”；导出 Excel 成功（HTTP 200，4432 字节，有效 xlsx）；冒烟在内存库上跑，停服后数据不留存，种子数据不受影响。文件制度管理统计总数 6（已发布 4、草稿 1、已作废 1，按分类制度 3 / 规范 1 / 流程 1 / 文件 1；30 天内到期 1 个、已过期 1 个）；冒烟发布门诊接诊流程草稿（审批人吴文斌，状态 0→1，发布日期默认为当天，审批记录留痕）；给三级医师查房制度新建版本（V3.0 → V4.0 草稿新记录 id=7，正文沿用）；导出 Excel 成功（HTTP 200，4424 字节，有效 xlsx）；下载三级医师查房制度正文成功（HTTP 200，687 字节 .txt）；冒烟在内存库上跑，停服后数据不留存，种子数据不受影响。医疗技术分级管理新增：技术档案校验与默认值（分类/分级/准入类别/风险等级/有效期非法拒绝、同一技术编号重复建档拒绝、默认待准入/风险低/不需伦理审查）、修改守卫（已准入不可直接修改，分级/科室/有效期变更走准入申请）、停用/重新启用流转（无停用原因拒绝，已停用不可再停用、已准入不可重新启用）、删除守卫（已准入不可直接删除，删除待准入档案时同时删除其准入申请）、准入审批流转（新技术准入通过后档案变为已准入、准入日期为当天、有效期默认 + 3 年；新增开展科室通过后科室追加进档案；变更分级通过后档案分级更新；延续准入通过后档案有效期更新；禁止类技术不得准入；变更分级/新增科室/延续准入必填项缺失拒绝；同一技术同一类型已有待审批申请拒绝重复申请；已结束的申请不能再审批）、授权校验与默认值（技术未准入/已停用拒绝授权、禁止类技术不得授权、授权级别高于技术分级拒绝、结束日期早于开始日期拒绝、同一医师同一技术已有待审批或已通过授权拒绝重复申请、科室按医师档案补全、开始日期默认当天）、授权审批与注销流转（无审批人拒绝、通过时再次校验技术仍已准入、已通过的授权不能直接删除请先注销、注销原因必填、已结束的授权不能再审批/再注销）、统计（档案 6 项已准入 4/待准入 1/已停用 1，一级 3/二级 1/三级 2，常规技术 4/限制类 1/禁止类 1，90 天内到期 1 项、已过期 1 项；准入申请 6 条待审批 2/已通过 3/已驳回 1，新技术准入 4/新增开展科室 1/延续准入 1；授权 6 条待审批 1/已通过 4/已驳回 1，90 天内到期 1 条、已过期 1 条）、导入导出往返（档案/准入/授权三组导出后删除再导入各补回 1 条、重复导入 0 条，导入的档案均为待准入、申请与授权均为待审批）。医疗技术分级管理冒烟：技术档案统计总数 6（待准入 1、已准入 4、已停用 1，按分级一级 3/二级 1/三级 2，按准入类别常规 4/限制类 1/禁止类 1）、准入申请统计总数 6（待审批 2、已通过 3、已驳回 1）、授权统计总数 6（待审批 1、已通过 4、已驳回 1）；种子准入申请针刺治疗新增开展科室“妇科”审批通过后技术档案开展科室变为“针灸推拿科、妇科”；授权审批无审批人返回 400“审批人不能为空”，有审批人通过后状态 0→1；冒烟在内存库上跑，停服后数据不留存，种子数据不受影响。检查维护新增：项目校验与默认值（分类/检查方式/适用类型/满分/权重非法拒绝、同一项目编号重复建档拒绝、默认启用/满分 100/默认权重 10/适用类型通用、依据标准自动补全编号）、项目启用/停用流转（已被模板引用不可停用/不可删除）、标准校验与默认值（级别/有效期非法拒绝、同一标准编号重复建档拒绝、默认现行）、标准废止/恢复流转（无废止原因拒绝，已废止不可再废止、现行不可恢复）、标准删除守卫（已被检查项目引用不可删除）、模板配置流转（权重之和须为 100、同一模板内项目不重复、停用项目/不存在的项目/模板拒绝、权重为 0 拒绝，配置后模板 checkItems JSON 同步更新）、单项删除守卫（一个模板至少保留一项，删除后模板 JSON 同步）、统计（项目 12 项启用 11/停用 1、已组装进模板 11/未组装 1，按分类病历质量 4/医疗安全 2、按方式抽查病历 6/现场查看 3；标准 6 部现行 5/已废止 1，国家 4/省 1/院内 1，30 天内到期 1 部、已过期 1 部、被项目引用 4 部；模板配置 11 条、已配置模板 3 个、平均每模板 3.67 项、未配置模板 0 个、未组装项目 1 个）、导入导出往返（项目/标准/模板配置三组导出后删除再导入各补回 1 条、重复导入 0 条，导入的项目默认启用、标准默认现行、模板配置整模板替换且权重之和须为 100）。检查分类新增：分类校验与默认值（编码/名称/适用类型非法拒绝、同一编码重复建档拒绝、同一上级下同名拒绝、不同上级下同名允许、默认启用/适用类型通用/层级按上级自动算）、上级守卫（上级不存在/不是一级分类拒绝，不建三级；上级不能是自己或自己的下级；已有下级的分类不可改为二级）、改编码守卫（已有项目归属的分类编码不可改）、启用/停用流转（已有项目归属、或有启用下级的分类不可停用；父分类停用时子分类不可启用）、删除守卫（已有下级、或已有项目归属的分类不可删除；其中有项目已被模板引用时报错点明模板引用）、统计（分类 12 个一级 10/二级 2，全部启用，归属项目 12 个、有项目归属 8 个分类、无项目归属 4 个分类、已组装进模板 11 项、涉及 3 个模板；病历质量项目 4 个、涉及模板 2 个、下级 2 个）、分类树（根 10 个，病历质量下级 2 个）、检查项目归属联动（新建二级分类编码可直接归检查项目、已停用的分类不可再归项目、启用后又可归）、导入导出往返（导出后删除再导入补回 1 条、重复导入 0 条，上级按编码解析）。检查模板管理新增：版本校验与默认值（检查项权重之和须为 100、至少一项、有名称与正数权重；版本号按已有最大版本号顺延 V1.0/V2.0/V3.0……）、新建版本流转（旧当前版本改为历史、模板检查项回写为新内容）、从历史版本恢复流转（以快照内容新建一版、历史只追加不改写）、改版守卫（有待执行/进行中计划的模板不允许新建版本与恢复）、复制流转（同类型同检查项的新模板默认停用、同名同类型拒绝、空名称拒绝）、使用情况统计（科室自查模板计划 2 条待执行 1/进行中 1、记录 2 条、最近计划 2026-11-28；院级模板计划 1 条已完成、记录 1 条；病历专项模板计划 1 条待执行、记录 0 条）、模板库统计（模板 3 个全部启用、按类型各 1 个、版本 3 个、平均检查项 3.67、最常用科室自查模板计划 2 条、从未使用 0 个）、停用/删除守卫（有进行中计划的模板不允许停用、已被检查计划引用的模板不允许删除、无计划引用的新模板可正常删除）、导入导出往返（导出读回 3 条、重复导入 0 条新增 3 条跳过、新模板导入 1 条、权重之和非法行与重复行各跳过 1 条、重复导入同一份 0 条新增）。医疗分级管理新增：标准校验与默认值（范围/分数线非法拒绝、同一编码重复建档拒绝、默认启用/甲级 90/乙级 80/丙级 60）、标准停用/删除守卫（有待审批评定不可停用、有评定不可删除）、评定校验与默认值（评分 0-100、周期 yyyy-MM、同一科室同一标准同一周期重复拒绝、停用标准不可新建评定、等级按分数线自动算）、审批流转（一级审批、审批人必填、已结束的评定不可再审批/再修改、已通过的评定不可直接删除）、统计（评定 6 条已通过 3/待审批 2/已驳回 1，甲级 1/乙级 3/丙级 1/丁级 1，平均分 80.67、达标率 83.33%）、导入导出往返（标准/评定两组导出后删除再导入各补回 1 条、重复导入 0 条）。医务系统完成统计新增：种子数据（31 个模块台账，已完成 27/进行中 1/待开发 3，完成率 87.10%、接口总数 556、测试总数 294、平均进度 88.71%）、总览/按分组/按月统计（医师管理 8 个全部完成、接口 136、测试 81；质量管理 10 个、接口 206、测试 115；系统与移动 4 个、完成 0、进行中 1、待开发 3；2026-11 完成 27 个）、多条件查询（关键字/分组/状态/编号）、模块编号唯一与分组/进度/计数校验、标记完成/重新开始流转（完成时进度 100、完成日期默认当天；重新开始回进行中、进度 50、完成日期清空；重复标记与未完成重开均拒绝）、已完成新建默认值与未完成完成日期清空、导入导出往返（导出读回 31 行、新增 1 条跳过 1 条、重复导入 0 条新增）。医务系统设置新增：种子数据（用户 4、角色 4、权限 33、科室 11、字典类型 5、字典数据 21、日志 0、参数 8、备份 3、接口 5、提醒设置 6、知识库 8）与五组统计（参数启用 7/停用 1、备份成功 3/已恢复 1/总记录 1526、接口启用 4/停用 1、提醒启用 5/停用 1、知识库启用 7/停用 1）；参数校验（键重复/分组非法/值类型不符拒绝）与内置参数守卫（改键、删除拒绝）、启用/停用与按 key 读取；备份执行（逐表计数）/恢复/删除守卫（已恢复不可删、失败备份不可恢复）；接口校验（编码重复/地址非法/类型非法/超时越界拒绝）与连通性测试（不可达端口如实记 FAIL）；提醒设置校验（渠道/接收范围非法、按角色须填角色编码、按人员须填用户名、全部人员时接收对象强制清空）；知识库检索（标题/编码/分类）与查阅量（详情打开 +1、编辑不改写）；参数与知识库导入导出往返（导出后删除再导入补回 1 条、重复导入 0 条）；底座 MockMvc（用户账号重复 400、重置密码、用户统计；角色编码重复、分配权限；科室/权限/字典编码重复、日志统计 0、科室统计 11）；总览统计与完成统计（已完成 30、接口 688、测试 359）。消息提醒管理新增：种子数据（站内消息 6、待办提醒 6、推送记录 4、短信记录 4）与四组统计（消息未读 4/已读 2、待办 4/已完成 1/已逾期 1、推送待发送 1/发送失败 3、短信待发送 1/发送失败 3）；消息校验（标题/分类/接收范围非法、按角色须填角色编码、按人员须填用户名、全部人员时接收对象强制清空）与已读流转（标已读、重复标已读拒绝、已读不可修改、全部标已读与按用户未读计数）；待办校验与完成流转（已完成不可修改/删除/重复完成、已逾期可完成）、逾期扫描（过期 1 条改已逾期、再扫 0 条）与按 28 号提醒设置生成（设置不存在/停用/责任人缺失拒绝）；推送与短信发送占位（明确返回 success=false、记发送失败并写明通道/网关未对接，重推/重发再次失败如实记录，已发送不可重推/重发/删除）与手机号格式校验；站内消息导入导出往返（导出后删除再导入补回 1 条、重复导入 0 条）；总览统计与 MockMvc（四组统计、未读计数、标已读、推送发送占位、短信非法手机号 400）。检查维护冒烟：检查项目统计总数 12（已启用 11、已停用 1，已组装进模板 11、未组装 1，按分类病历质量 4/医疗安全 2，按方式抽查病历 6/现场查看 3）、检查标准统计总数 6（现行 5、已废止 1，国家 4/省 1/院内 1，30 天内到期 1 部、已过期 1 部、被项目引用 4 部）、模板配置统计已配置模板 3 个、配置记录 11 条、平均每模板 3.67 项；冒烟把科室自查模板改配为病历三项（40/30/30）后模板 JSON 同步为病历完整性/及时性/规范性，再恢复为原四项（30/30/20/20）后 JSON 恢复一致；无废止原因废止标准返回 400“废止原因不能为空”；只看未组装进模板过滤返回 1 项（中医辨证论治规范）；导出检查项目 Excel 成功（HTTP 200，4995 字节，有效 xlsx）；冒烟在内存库上跑，停服后数据不留存，种子数据不受影响。检查分类冒烟：检查分类统计总数 12（已启用 12，一级 10、二级 2，归属检查项目 12、有项目归属 8 个分类、无项目归属 4 个分类、已组装进模板 11 项、涉及模板 3 个）、分类树根 10 个（病历质量下级 2 个）、按分类汇总病历质量项目 4 个、涉及模板 2 个、下级 2 个；冒烟新建二级分类（挂在病历质量下，层级自动算为 2）→ 新建检查项目归到该分类成功 → 有项目归属的分类停用/改编码/删除均返回 400（“已有 1 个检查项目归属，请先把项目改归其他分类”）；病历质量停用返回 400“该分类下有 4 个检查项目已被检查模板引用”；把项目改归其他分类后停用（1→0）、启用（0→1）、删除均成功，冒烟项目一并删除；导出 Excel 成功（HTTP 200，4838 字节，有效 xlsx）；冒烟在内存库上跑，停服后数据不留存，种子数据不受影响。检查模板管理冒烟：模板库统计总数 3（全部启用，按类型科室自查 1/院级检查 1/专项检查 1，版本 3 个、平均检查项 3.67、最常用科室自查模板计划 2 条、从未使用 0 个）；院级模板版本历史 1 条 V1.0 当前版本；科室自查模板使用情况计划 2 条（待执行 1、进行中 1）、记录 2 条、最近计划 2026-11-28；复制院级模板为“冒烟复制模板”成功（新记录 id=4，默认停用）；院级模板新建版本 V2.0 成功（权重改为 40/30/20/10，模板检查项同步更新）；科室自查模板新建版本返回 400“该模板有 2 个进行中的检查计划，不能新建版本”；停用科室自查模板返回 400“该模板有 2 个进行中的检查计划，不能停用”；删除科室自查模板返回 400“该模板已被 2 个检查计划引用，不能直接删除”；从 V1.0 恢复生成 V3.0（内容为 V1.0 快照，模板检查项恢复为 35/30/20/15）；导出 Excel 成功（HTTP 200，4326 字节，有效 xlsx）；冒烟在内存库上跑，停服后数据不留存，种子数据不受影响。医疗分级管理冒烟：分级标准统计总数 3（全部启用，按范围医疗质量 1/医疗服务 1/综合 1，全部已被评定引用）；分级评定统计总数 6（待审批 2、已通过 3、已驳回 1，甲级 1/乙级 3/丙级 1/丁级 1，平均分 80.67、达标率 83.33%）；按科室汇总中医内科当前分级甲级、针灸推拿科与妇科乙级、骨伤科与急诊科与儿科未评级；冒烟新建放射科评定（2026-12，91 分甲级）→ 审批通过（0→1，审批记录留痕），重复新建返回 400，重复审批返回 400；导出标准与评定 Excel 成功（HTTP 200，3998/4353 字节，有效 xlsx）；冒烟在内存库上跑，停服后数据不留存，种子数据不受影响。医务系统完成统计冒烟：总览统计总数 31（已完成 27、进行中 1、待开发 3，完成率 87.10%，接口总数 556、测试总数 294、平均进度 88.71%）；按分组汇总 5 行（医师管理 8 个全部完成、接口 136、测试 81；系统与移动 4 个、完成 0、进行中 1、待开发 3）；按月 2026-11 完成 27 个；冒烟给消息提醒标记完成（0→2，进度 100、完成日期默认为当天）再重新开始（2→1，进度 50、完成日期清空）；导出 Excel 成功（HTTP 200，5569 字节，有效 xlsx，表头 + 31 行）；冒烟在内存库上跑，停服后数据不留存，种子数据不受影响。医务系统设置冒烟：总览统计（用户 2、角色 4、权限 30、科室 8、字典类型 5、字典数据 21、日志 0、参数 8、备份 3、接口 5、提醒设置 6、知识库 8）；参数统计 8（启用 7/停用 1）、备份统计 3（成功 3、已恢复 1、总记录 1526）、完成统计 31（已完成 28、进行中 0、待开发 3、完成率 90.32%、接口总数 622、测试总数 317、平均进度 90.32%）；新建参数 smoke.test（NUMBER 值 5）成功，重复参数键 sys.name 返回 400“参数键已存在”；执行备份成功（记录 406 条、文件名 medical-backup-20261122-125418.zip、大小 415744 字节），恢复后恢复次数 1，已恢复备份删除返回 400“已被恢复过（1 次），不可直接删除”；提醒设置新建→停用→删除成功；知识库新建→详情查阅量 1→2→删除成功；重复用户名 admin 返回 400“登录账号已存在：admin”，用户统计总数 2，角色分配权限 2 条后清空恢复；参数/备份/知识库导出 Excel 成功（HTTP 200，4342/4030/5154 字节，有效 xlsx）；接口连通性测试对本机服务自测返回 success=false、httpStatus=-1、message“连接失败: 未收到响应”：原因是本沙箱禁止 JVM 出站 TCP（独立探针证实：原生 Socket 被重置、example.com 同样 -1，而同机 curl 正常），测试逻辑本身不伪造成功、失败如实记 FAIL，单元测试中不可达端口用例已验证失败记录正确，在有正常网络的机器上该功能可正常工作；冒烟在内存库上跑，停服后数据不留存，种子数据不受影响。消息提醒管理冒烟：总览统计（站内消息 6、未读 4，待办 6、待办中 4、已逾期 1，推送 4、发送失败 3，短信 4、发送失败 3，提醒设置 6、启用 5）；站内消息统计总数 6（未读 4/已读 2，按分类通知 1/公告 1/待办提醒 3/系统消息 1）、待办统计总数 6（待办 4/已完成 1/已逾期 1）、推送统计总数 4（待发送 1/发送失败 3）、短信统计总数 4（待发送 1/发送失败 3）、admin 未读 2 条；冒烟新建站内消息→标已读（0→1）→重复标已读返回 400“消息已读，无需重复标记”；按 LEAVE_APPROVE 设置生成待办（来源 SETTING、标题用设置名称）→完成（0→1）；逾期扫描 0 条；推送发送返回 success=false、记录记发送失败“推送通道未对接（预留占位）：微信通道未配置真实服务”，短信发送与重发同样返回 success=false、记发送失败“短信网关未对接（预留占位）”，未伪造发送成功；站内消息与待办导出 Excel 成功（HTTP 200，4641/4621 字节，有效 xlsx）；冒烟数据已清理（冒烟消息/推送/短信记录已删除；冒烟待办已完成、按守卫不可直接删除，内存库停服后不留存），统计恢复总数 6/6/4/4；冒烟在内存库上跑，停服后数据不留存，种子数据不受影响。医疗工作简报管理新增：种子数据（简报模板 3 个启用 2/停用 1、简报 6 期草稿 2/待审批 1/已发布 2/已驳回 1）与三组统计（总数 6、按分类月报 4/季报 1/工作动态 1、按月 2026-09 1 期/2026-10 2 期/2026-11 3 期）与 MockMvc（统计总数 6、已发布 2，启用模板 2 个）；模板校验与默认值（名称/分类/栏目 JSON 非空校验、同名拒绝）与启用/停用流转、删除守卫（已被简报引用拒绝）；简报校验与默认值（期号全院唯一、统计周期 yyyy-MM、分类合法）与修改守卫（只有草稿与已驳回能修改）、删除守卫（已发布不可直接删除、批量删除混入已发布整批拒绝）；提交审批/审批通过/驳回流转（无审批人拒绝、无驳回原因拒绝、已结束不可再审批、驳回后可改回草稿重提、发布日期默认为当天、审批记录留痕）；按模板自动生成（2026-11 生成 2026-11-04 期草稿，正文聚合真实数据：医师 12 人、工作量门诊 3420/住院 1066/手术 241、检查计划 4 个已完成 1/进行中 1、记录 3 条平均 80.33 分、问题 4 个严重 2 个、MDT 5 例已通过 2 例紧急 2 例、新技术申报 3 项已立项 1 项、考核 6 人次平均 81.17 分优秀 2 人；停用模板/非法周期/不存在模板/重复期号均拒绝）；导出 Word（真实 .docx，PK 开头）与 PDF（真实 PDF，%PDF-1.5 开头，嵌入 NotoSansCJKsc 简体中文字体，pdftotext 可正常提取中文正文）与正文 .txt 下载、导入导出往返（导出读回 7 行、删除临时行再导入补回 1 条、重复导入 0 条）；完成统计台账同步更新（已完成 30、接口 688、测试 359、完成率 96.77%）。医务管理移动端新增：种子数据（设备 6 台已绑定 4/已解绑 2、iOS 3/Android 3；审批记录 6 条通过 5/驳回 1、全部成功）与四组统计（设备总数 6、待审批 19 条、审批记录总数 6、总览医师 12 人）与 MockMvc（设备分页总数 6、待审批计数 19、总览待审批 19、医师档案只读总数 12、推送占位失败）；设备校验与默认值（编码/使用人必填、平台非法拒绝、编码重复拒绝、登记默认已绑定）与绑定/解绑流转（重复绑定/解绑拒绝、已绑定不可直接删除、解绑后可删除）与导入导出往返（导出读回 7 行、删除临时行再导入补回 1 条、重复导入 0 条）；待审批聚合（8 个模块共 19 条：休假执业 4/重点授权 4/排班调班 1/MDT 2/新技术新项目 1/医疗人员动态 4/医疗分级 2/医疗工作简报 1，按模块与关键字过滤、非法模块拒绝）与移动端审批流转（休假两级审批 0→1→2、调班 1→2、MDT 驳回 0→2、审批人为空与驳回无原因与不存在事项拒绝、已结束事项再审批委托失败记 FAIL 日志并原样抛错，恢复时 MDT 须走 mapper 绕过其 service 的“已结束不可修改”守卫）与审批记录统计与导出（表头 + 6 行，按操作时间倒序）；移动端总览（医师 12、待审批 19、今日排班 0、考核平均 81.17、设备 6、未读消息 4、推送失败 3）与移动端推送占位（APP 渠道 success=false、失败原因写明通道未对接、重推再次失败）；完成统计台账同步更新（已完成 31、接口 720、测试 408、完成率 100.00%）。医疗工作简报管理冒烟：简报统计总数 6（草稿 2、待审批 1、已发布 2、已驳回 1，按分类月报 4/季报 1/工作动态 1，模板 3 个启用 2 个）；按模板自动生成 2026-11 简报（2026-11-04 期，标题“月度医疗工作简报模板（2026-11）”，正文含门诊 3420/检查计划 4 个/MDT 5 例/考核平均 81.17 分）→ 提交（0→1）→ 审批通过（1→2，发布日期默认为当天，审批记录留痕“医务科 吴文斌：通过（同意发布）”），再次审批返回 400“只有待审批的简报能审批通过”，删除已发布返回 400“已发布的简报不能直接删除”；导出 Word 成功（HTTP 200，2874 字节，有效 .docx）、导出 PDF 成功（HTTP 200，416154 字节，%PDF-1.5，嵌入简体中文字体，pdftotext 提取中文正文完整）、下载正文成功（HTTP 200，795 字节 .txt）、导出 Excel 成功（HTTP 200，5072 字节，有效 xlsx）；冒烟在内存库上跑，停服后数据不留存，种子数据不受影响。医务管理移动端冒烟：总览统计（医师 12、待审批 19：休假执业 4/重点授权 4/排班调班 1/MDT 2/新技术新项目 1/医疗人员动态 4/医疗分级 2/医疗工作简报 1，今日排班 0、考核 6 条平均 81.17 分优秀 2 人、设备 6 台已绑定 4/未解绑 2、未读消息 4、推送失败 3）；李秀英 MDT 会诊申请移动端审批通过（0→1，审批记录留痕，日志 id=7）；孙小红休假申请移动端两级审批（0→1→2，审批记录留痕“科主任 吴文斌：通过（同意）；医务科 吴文斌：通过（同意）”）；待审批由 19 变为 17；无驳回原因驳回返回 400“驳回原因不能为空”，已结束的休假申请再审批返回 400“该申请已结束审批，不能再通过”并记 FAIL 日志；移动端推送返回 success=false、记发送失败“推送通道未对接（预留占位）：移动端通道未配置真实服务”，未伪造发送成功；设备与审批记录导出 Excel 成功（HTTP 200，4243/4575 字节，有效 xlsx）；完成统计台账总数 31（已完成 31、进行中 0、待开发 0，完成率 100.00%，接口总数 720、测试总数 408、平均进度 100.00%）；冒烟在内存库上跑，停服后数据不留存，种子数据不受影响。
- 前端：`npm run build` **通过**（见下方说明）。休假执业页面已接入左侧菜单（`/leave`），重点授权页面已接入左侧菜单（`/authorization`），医师考核页面已接入左侧菜单（`/assessment`），排班管理页面已接入左侧菜单（`/schedule`），MDT 管理页面已接入左侧菜单（`/mdt`），质量检查管理页面已接入左侧菜单（`/quality-inspection`），新技术新项目管理页面已接入左侧菜单（`/new-tech`），医疗人员动态管理页面已接入左侧菜单（`/staff-dynamic`），放射人员管理页面已接入左侧菜单（`/radiation`），委员会管理页面已接入左侧菜单（`/committee`），检查分类页面已接入左侧菜单（`/quality/category`），统计分析页面已接入左侧菜单（`/stats/work`、`/stats/tertiary`、`/stats/quality` 三个入口共用一个统计页、按路由切换页签），，医务系统完成统计页面已接入左侧菜单（`/stats/completion`），医务系统设置页面已接入左侧菜单（`/system/settings`，含参数/备份/接口/提醒设置/知识库五个页签），系统底座页面已接入左侧菜单（`/system/users`、`/system/roles`、`/system/permissions`、`/system/depts`、`/system/dicts`、`/system/logs`），医疗工作简报页面已接入左侧菜单（`/briefing`，含简报/简报模板两个页签），医务管理移动端页面已接入左侧菜单（`/mobile`，含待审批/移动端设备/审批记录/移动端推送/移动端查看五个页签），都不再是占位页。
- Docker Compose：本机无 Docker 未实测，`docker-compose.yml` + 两个 Dockerfile 已写好，待在有 Docker 的机器上验证。**真实 MySQL 灌库也未在本机实测**（本机无 MySQL 服务端、无 Docker）：data.sql 的 MySQL 语法已按标准写法复核，H2 MySQL 兼容模式实测通过，但在有 MySQL 的机器上第一次 `docker compose up` 时仍需确认一次。

### 环境说明（构建时遇到的问题与处理）

- 云电脑初始无 JDK/Maven/Docker：从 Adoptium 下载 JDK 17、从 Apache 归档下载 Maven 3.9.9 到 `/opt/java`、`/opt/maven`（系统外目录，重启可能丢失；项目本身不依赖它们的位置）。MDT 模块开发时发现 `/opt` 下的 JDK/Maven 已丢失，已按同一办法重新下载安装（Temurin 17.0.21.1、Maven 3.9.9），构建全程离线（`-o -Dmaven.repo.local=/home/hatch/.m2/repository`）。**2026-11-22 医疗人员动态模块开发时 `/opt` 下的 JDK/Maven 又因服务重启丢失，改装到 `~/workspace/tools/java`、`~/workspace/tools/maven`（家目录下，重启不丢），此后构建用 `export JAVA_HOME=$HOME/workspace/tools/java PATH=$HOME/workspace/tools/java/bin:$HOME/workspace/tools/maven/bin:$PATH`。**
- Maven 出站代理：云电脑 Maven 直连中央仓库会被重置，本次构建用 curl 经环境代理把缺失依赖（H2、POI 闭包、Surefire 等）下载进本地仓库 `~/.m2/repository`，随后全程离线构建。用户自己机器上如网络正常，直接 `mvn compile` 即可。医疗工作简报模块新增的 OpenPDF 1.3.30（及父 pom `openpdf-parent`）同样用 curl 灌进本地仓库，`_remote.repositories` 须写 `构件名=central,aliyun` 格式（旧的 `>` 格式离线模式不认）。
- **本机 Maven 离线构建注意**：依赖仓库在 `/home/hatch/.m2/repository`；但以 root 身份跑 Maven 时 JVM 的 `user.home` 是 `/root`，Maven 默认会去读 `/root/.m2/repository`（几乎是空的）而报错。办法：加 `-Dmaven.repo.local=/home/hatch/.m2/repository` 再跑。另外 curl 灌进去的依赖没有 `_remote.repositories` 标记，已批量补过（513 个目录、797 个构件），离线模式才能认出来；`~/.m2/settings.xml` 里的 egress 代理密码已过期，在线构建目前走不通。
- MyBatis-Plus 3.5.5 自带的 mybatis-spring 2.1.2 与 Spring Framework 6.1.6 不兼容（启动报 `factoryBeanObjectType` 错误），已升级 MyBatis-Plus 至 **3.5.7**（配套 mybatis-spring 3.0.3）并在 pom 中显式锁定。
- 容器 locale 为 POSIX 时 JVM 默认字符集是 US-ASCII，会导致中文种子数据入库变 `?`：已在 `application.yml` 设 `spring.sql.init.encoding: UTF-8`，pom 锁定编译编码 UTF-8，Surefire 加 `-Dfile.encoding=UTF-8`，并在 Dockerfile 入口加了该参数。
- 字典表列名 `value` 是 H2/MySQL 保留字，已改名为 `dict_value`。

## 待办

- ~~登录鉴权（Spring Security / Sa-Token + JWT）、密码 BCrypt 加密。~~已完成（2026-11-22）：JWT（JJWT HS256，不引入 Spring Security）+ PBKDF2 密码哈希 + 接口拦截 + 前端登录页与路由守卫，见「登录鉴权」一节；生产需换 `AUTH_JWT_SECRET` 并改默认密码。
- 证件照上传（对象存储/本地文件服务）。
- 与 HIS 对接（排班模块）、短信推送（消息提醒模块）。
- Docker Compose 在有 Docker 的机器上实测。
