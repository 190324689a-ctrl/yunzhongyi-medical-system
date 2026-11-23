package com.yunzhongyi.medical.physician.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.physician.dto.PhysicianQuery;
import com.yunzhongyi.medical.physician.entity.Physician;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 医师档案服务
 */
public interface PhysicianService extends IService<Physician> {

    /** 多条件组合分页查询 */
    PageResult<Physician> pageQuery(PhysicianQuery query);

    /** 按同样条件查全部（导出用） */
    List<Physician> listByQuery(PhysicianQuery query);

    /** 导出 Excel */
    void exportExcel(PhysicianQuery query, OutputStream out);

    /**
     * 导入 Excel
     * @return 成功导入条数；非法行跳过并计入失败数
     */
    int importExcel(InputStream in);
}
