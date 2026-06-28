package com.xiaoyu.promptship.common;

import lombok.Getter;
import lombok.Setter;

/**
 * 分页请求封装类
 */
@Getter
@Setter
public class PageRequest {

    /**
     * 当前页号
     */
    private int pageNum = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";
}
