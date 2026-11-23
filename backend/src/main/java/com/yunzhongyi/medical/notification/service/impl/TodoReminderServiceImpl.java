package com.yunzhongyi.medical.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.notification.dto.GenerateTodoRequest;
import com.yunzhongyi.medical.notification.dto.TodoReminderQuery;
import com.yunzhongyi.medical.notification.entity.TodoReminder;
import com.yunzhongyi.medical.notification.excel.TodoReminderExcelUtil;
import com.yunzhongyi.medical.notification.mapper.TodoReminderMapper;
import com.yunzhongyi.medical.notification.service.NotificationTypeUtil;
import com.yunzhongyi.medical.notification.service.TodoReminderService;
import com.yunzhongyi.medical.sysconfig.entity.SysNoticeSetting;
import com.yunzhongyi.medical.sysconfig.service.SysConfigTypeUtil;
import com.yunzhongyi.medical.sysconfig.service.SysNoticeSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 待办提醒服务实现。
 * <p>
 * 关键守卫：标题必填、类型与来源须合法；已完成的待办不可修改、不可直接删除、
 * 不可重复完成；按 28 号提醒设置生成时设置须存在且启用。
 */
@Service
@RequiredArgsConstructor
public class TodoReminderServiceImpl
        extends ServiceImpl<TodoReminderMapper, TodoReminder>
        implements TodoReminderService {

    private final SysNoticeSettingService sysNoticeSettingService;

    @Override
    public PageResult<TodoReminder> pageQuery(TodoReminderQuery query) {
        Page<TodoReminder> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public TodoReminder detail(Long id) {
        TodoReminder t = this.getById(id);
        if (t == null) {
            throw new IllegalArgumentException("待办提醒不存在：" + id);
        }
        return t;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoReminder create(TodoReminder todo) {
        validateTodo(todo);
        applyDefaults(todo);
        todo.setStatus(NotificationTypeUtil.TODO_PENDING);
        todo.setCompleteTime(null);
        this.save(todo);
        return todo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoReminder update(TodoReminder todo) {
        TodoReminder old = detail(todo.getId());
        if (Integer.valueOf(NotificationTypeUtil.TODO_DONE).equals(old.getStatus())) {
            throw new IllegalArgumentException("已完成的待办不可修改");
        }
        validateTodo(todo);
        applyDefaults(todo);
        todo.setStatus(old.getStatus());
        todo.setCompleteTime(old.getCompleteTime());
        this.updateById(todo);
        return todo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        TodoReminder old = detail(id);
        if (Integer.valueOf(NotificationTypeUtil.TODO_DONE).equals(old.getStatus())) {
            throw new IllegalArgumentException("已完成的待办不可直接删除");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoReminder complete(Long id) {
        TodoReminder t = detail(id);
        if (Integer.valueOf(NotificationTypeUtil.TODO_DONE).equals(t.getStatus())) {
            throw new IllegalArgumentException("待办已完成，不可重复完成");
        }
        t.setStatus(NotificationTypeUtil.TODO_DONE);
        t.setCompleteTime(LocalDateTime.now());
        this.updateById(t);
        return t;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long overdueScan() {
        List<TodoReminder> overdue = this.list(new LambdaQueryWrapper<TodoReminder>()
                .eq(TodoReminder::getStatus, NotificationTypeUtil.TODO_PENDING)
                .isNotNull(TodoReminder::getDueDate)
                .lt(TodoReminder::getDueDate, LocalDate.now()));
        for (TodoReminder t : overdue) {
            t.setStatus(NotificationTypeUtil.TODO_OVERDUE);
            this.updateById(t);
        }
        return overdue.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoReminder generateFromSetting(GenerateTodoRequest request) {
        if (!StringUtils.hasText(request.getSettingKey())) {
            throw new IllegalArgumentException("提醒设置编码不能为空");
        }
        SysNoticeSetting setting = sysNoticeSettingService.getOne(
                new LambdaQueryWrapper<SysNoticeSetting>()
                        .eq(SysNoticeSetting::getSettingKey, request.getSettingKey().trim()));
        if (setting == null) {
            throw new IllegalArgumentException("提醒设置不存在：" + request.getSettingKey());
        }
        if (!Integer.valueOf(SysConfigTypeUtil.STATUS_ENABLED).equals(setting.getEnabled())) {
            throw new IllegalArgumentException("提醒设置已停用：" + request.getSettingKey());
        }
        if (!StringUtils.hasText(request.getAssignee())) {
            throw new IllegalArgumentException("责任人不能为空");
        }
        TodoReminder todo = new TodoReminder();
        todo.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : setting.getSettingName());
        todo.setContent(StringUtils.hasText(request.getContent()) ? request.getContent()
                : setting.getTemplateContent());
        todo.setTodoType(request.getTodoType());
        todo.setRelatedType(request.getRelatedType());
        todo.setRelatedId(request.getRelatedId());
        todo.setAssignee(request.getAssignee());
        todo.setAssigneeName(request.getAssigneeName());
        todo.setDeptName(request.getDeptName());
        todo.setDueDate(request.getDueDate());
        todo.setSource(NotificationTypeUtil.SOURCE_SETTING);
        todo.setSettingKey(setting.getSettingKey());
        validateTodo(todo);
        applyDefaults(todo);
        todo.setStatus(NotificationTypeUtil.TODO_PENDING);
        this.save(todo);
        return todo;
    }

    @Override
    public Map<String, Object> stats() {
        List<TodoReminder> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("pending", all.stream()
                .filter(t -> Integer.valueOf(NotificationTypeUtil.TODO_PENDING).equals(t.getStatus())).count());
        r.put("completed", all.stream()
                .filter(t -> Integer.valueOf(NotificationTypeUtil.TODO_DONE).equals(t.getStatus())).count());
        r.put("overdue", all.stream()
                .filter(t -> Integer.valueOf(NotificationTypeUtil.TODO_OVERDUE).equals(t.getStatus())).count());
        r.put("dueToday", all.stream()
                .filter(t -> Integer.valueOf(NotificationTypeUtil.TODO_PENDING).equals(t.getStatus())
                        && LocalDate.now().equals(t.getDueDate())).count());
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String type : List.of(NotificationTypeUtil.TODO_APPROVAL, NotificationTypeUtil.TODO_EXPIRE,
                NotificationTypeUtil.TODO_RECTIFY, NotificationTypeUtil.TODO_OTHER)) {
            byType.put(type, all.stream().filter(t -> type.equals(t.getTodoType())).count());
        }
        r.put("byTodoType", byType);
        Map<String, Long> bySource = new LinkedHashMap<>();
        for (String source : List.of(NotificationTypeUtil.SOURCE_MANUAL, NotificationTypeUtil.SOURCE_AUTO,
                NotificationTypeUtil.SOURCE_SETTING)) {
            bySource.put(source, all.stream().filter(t -> source.equals(t.getSource())).count());
        }
        r.put("bySource", bySource);
        return r;
    }

    @Override
    public void exportExcel(TodoReminderQuery query, OutputStream out) {
        TodoReminderExcelUtil.write(this.list(buildWrapper(query)), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(InputStream in) {
        List<TodoReminder> rows = TodoReminderExcelUtil.read(in);
        int created = 0;
        int skipped = 0;
        for (TodoReminder row : rows) {
            try {
                validateTodo(row);
            } catch (IllegalArgumentException e) {
                skipped++;
                continue;
            }
            long dup = this.count(new LambdaQueryWrapper<TodoReminder>()
                    .eq(TodoReminder::getTitle, row.getTitle())
                    .eq(TodoReminder::getContent, row.getContent() == null ? "" : row.getContent()));
            if (dup > 0) {
                skipped++;
                continue;
            }
            applyDefaults(row);
            if (row.getStatus() == null) {
                row.setStatus(NotificationTypeUtil.TODO_PENDING);
            }
            row.setCompleteTime(null);
            this.save(row);
            created++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("total", rows.size());
        return result;
    }

    @Override
    public void writeImportTemplate(OutputStream out) {
        TodoReminderExcelUtil.writeTemplate(out);
    }

    // ---------- 内部方法 ----------

    private LambdaQueryWrapper<TodoReminder> buildWrapper(TodoReminderQuery query) {
        LambdaQueryWrapper<TodoReminder> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like(TodoReminder::getTitle, kw)
                    .or().like(TodoReminder::getContent, kw)
                    .or().like(TodoReminder::getAssigneeName, kw));
        }
        if (StringUtils.hasText(query.getTodoType())) {
            w.eq(TodoReminder::getTodoType, query.getTodoType());
        }
        if (query.getStatus() != null) {
            w.eq(TodoReminder::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getAssignee())) {
            w.eq(TodoReminder::getAssignee, query.getAssignee());
        }
        if (StringUtils.hasText(query.getSource())) {
            w.eq(TodoReminder::getSource, query.getSource());
        }
        w.orderByAsc(TodoReminder::getDueDate).orderByDesc(TodoReminder::getId);
        return w;
    }

    private void applyDefaults(TodoReminder t) {
        if (t.getTodoType() == null) {
            t.setTodoType(NotificationTypeUtil.TODO_OTHER);
        }
        if (t.getSource() == null) {
            t.setSource(NotificationTypeUtil.SOURCE_MANUAL);
        }
    }

    private void validateTodo(TodoReminder t) {
        if (!StringUtils.hasText(t.getTitle())) {
            throw new IllegalArgumentException("待办标题不能为空");
        }
        if (t.getTodoType() != null && !NotificationTypeUtil.TODO_TYPES.contains(t.getTodoType())) {
            throw new IllegalArgumentException("待办类型不合法：" + t.getTodoType());
        }
        if (t.getSource() != null && !NotificationTypeUtil.TODO_SOURCES.contains(t.getSource())) {
            throw new IllegalArgumentException("待办来源不合法：" + t.getSource());
        }
        if (NotificationTypeUtil.SOURCE_SETTING.equals(t.getSource())
                && !StringUtils.hasText(t.getSettingKey())) {
            throw new IllegalArgumentException("按提醒设置生成时须填写提醒设置编码");
        }
    }
}
