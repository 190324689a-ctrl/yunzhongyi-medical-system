package com.yunzhongyi.medical.mobile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.mobile.dto.MobileDeviceQuery;
import com.yunzhongyi.medical.mobile.entity.MobileDevice;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/** 移动端设备登记服务：设备登记 / 绑定 / 解绑 / 统计 / 导入导出 */
public interface MobileDeviceService extends IService<MobileDevice> {

    PageResult<MobileDevice> pageQuery(MobileDeviceQuery query);

    MobileDevice detail(Long id);

    MobileDevice register(MobileDevice device);

    MobileDevice update(MobileDevice device);

    void delete(Long id);

    /** 绑定设备（已绑定拒绝重复绑定，解绑后可重新绑定） */
    MobileDevice bind(Long id);

    /** 解绑设备（已解绑拒绝重复解绑） */
    MobileDevice unbind(Long id);

    Map<String, Object> stats();

    void exportExcel(MobileDeviceQuery query, OutputStream out);

    Map<String, Object> importExcel(InputStream in);

    void writeImportTemplate(OutputStream out);
}
