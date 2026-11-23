package com.yunzhongyi.medical.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private long total;
    private long page;
    private long size;
    private List<T> records;

    public static <T> PageResult<T> of(long total, long page, long size, List<T> records) {
        PageResult<T> r = new PageResult<>();
        r.setTotal(total);
        r.setPage(page);
        r.setSize(size);
        r.setRecords(records);
        return r;
    }
}
