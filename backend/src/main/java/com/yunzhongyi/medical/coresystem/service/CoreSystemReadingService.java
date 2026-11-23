package com.yunzhongyi.medical.coresystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.coresystem.dto.CoreSystemReadingQuery;
import com.yunzhongyi.medical.coresystem.entity.CoreSystemReading;

import java.util.List;

/**
 * 核心制度查阅与学习记录服务
 */
public interface CoreSystemReadingService extends IService<CoreSystemReading> {

    /** 多条件组合分页查询 */
    PageResult<CoreSystemReading> pageQuery(CoreSystemReadingQuery query);

    /** 查某项制度的全部查阅学习记录（按查阅日期降序） */
    List<CoreSystemReading> listByCoreSystem(Long coreSystemId);

    /**
     * 登记查阅学习：只有已发布的制度才能登记；查阅日期默认为当天；
     * 同一人同一制度同一天只登记一次，重复登记拒绝。
     */
    CoreSystemReading create(CoreSystemReading reading);

    /** 标记已学习：已查阅 → 已学习 */
    CoreSystemReading markLearned(Long id);

    /** 删除一条查阅学习记录 */
    boolean deleteReading(Long id);
}
