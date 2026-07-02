package com.xiaoyu.promptship.common;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 游标分页响应（适用于聊天记录等无限滚动场景）。
 * <p>
 * 与 {@link PageRequest} 传统分页的区别：
 * <ul>
 *   <li>不使用 pageNum/offset，而是用上一次返回的 cursor 作为起点继续查询</li>
 *   <li>查询性能不受翻页深度影响（无需跳过行）</li>
 *   <li>前端拿到 nextCursor 后下次请求传 cursor=nextCursor 即可加载下一页</li>
 * </ul>
 * </p>
 *
 * @param <T> 数据记录类型
 * @author xiaoyu
 * @since 1.0
 */
@Data
public class CursorPage<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页数据列表
     */
    private List<T> records;

    /**
     * 下一页游标，前端下次请求时传入 cursor 参数。
     * 为 null 表示没有更多数据。
     */
    private String nextCursor;

    /**
     * 是否还有下一页
     */
    private Boolean hasMore;

    /**
     * 每页大小
     */
    private Integer pageSize;

    public CursorPage() {
        this.records = Collections.emptyList();
        this.hasMore = false;
    }

    public static <T> CursorPage<T> of(List<T> records, String nextCursor, boolean hasMore, int pageSize) {
        CursorPage<T> page = new CursorPage<>();
        page.setRecords(records);
        page.setNextCursor(nextCursor);
        page.setHasMore(hasMore);
        page.setPageSize(pageSize);
        return page;
    }

}
