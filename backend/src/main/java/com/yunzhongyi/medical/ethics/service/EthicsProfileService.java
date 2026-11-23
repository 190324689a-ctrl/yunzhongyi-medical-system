package com.yunzhongyi.medical.ethics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.ethics.dto.EthicsProfileQuery;
import com.yunzhongyi.medical.ethics.entity.EthicsProfile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/** 医德医风档案服务：档案 CRUD + 统计 + Excel 批量导入导出 */
public interface EthicsProfileService extends IService<EthicsProfile> {

    PageResult<EthicsProfile> pageQuery(EthicsProfileQuery query);

    List<EthicsProfile> listByQuery(EthicsProfileQuery query);

    /** 档案统计：总数 / 在档 / 封存 / 按科室汇总 / 已通过表彰数 / 投诉总数 / 考评平均分与优秀率 */
    Map<String, Object> stats();

    /** 按科室汇总统计（档案数、在档数、最近考评优秀人数） */
    List<Map<String, Object>> statsByDept();

    EthicsProfile detail(Long id);

    void exportExcel(EthicsProfileQuery query, OutputStream out);

    int importExcel(InputStream in);
}
