package com.yunzhongyi.medical.committee.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.committee.dto.CommitteeQuery;
import com.yunzhongyi.medical.committee.dto.CommitteeRenewRequest;
import com.yunzhongyi.medical.committee.entity.Committee;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 委员会管理服务
 */
public interface CommitteeService extends IService<Committee> {

    /** 多条件组合分页查询 */
    PageResult<Committee> pageQuery(CommitteeQuery query);

    /** 按同样条件查全部 */
    List<Committee> listByQuery(CommitteeQuery query);

    /**
     * 委员会统计：委员会总数 / 在任 / 已换届 / 已解散 / 按类别分组 /
     * 任期 90 天内到期数 / 会议总数 / 决议总数 / 决议各状态数 /
     * 决议完成率 / 逾期决议数
     */
    Map<String, Object> stats();

    /** 按委员会类别汇总统计 */
    List<Map<String, Object>> statsByType();

    /** 按月会议趋势统计（会议月份、会议数，按月份升序） */
    List<Map<String, Object>> statsByMonth();

    /** 详情 */
    Committee detail(Long id);

    /**
     * 换届：只有在任的委员会能换届。生成一条新记录（届次 +1、新任期、
     * 新一届人员），原记录状态改为已换届，返回新一届记录。
     */
    Committee renew(Long id, CommitteeRenewRequest request);

    /** 解散：只有在任的委员会能解散 */
    Committee dissolve(Long id);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(CommitteeQuery query, OutputStream out);

    /** 批量导入 Excel，返回成功导入条数 */
    int importExcel(InputStream in);
}
