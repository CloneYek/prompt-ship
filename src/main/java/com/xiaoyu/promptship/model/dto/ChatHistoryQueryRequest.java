package com.xiaoyu.promptship.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对话历史游标查询请求。
 * <p>
 * 首次请求只传 appId + pageSize，返回第一页数据 + nextCursor。
 * 后续请求将 nextCursor 作为 cursor 参数传入，即可加载下一页。
 * </p>
 *
 * @author xiaoyu
 * @since 1.0
 */
@Data
public class ChatHistoryQueryRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用 id（必填，按应用隔离查询）
     */
    @NotNull(message = "应用 id 不能为空")
    private Long appId;

    /**
     * 游标：上一页最后一条记录的 createTime（ISO 格式，如 "2026-07-02T12:00:00"）。
     * 首次请求传 null。
     */
    private String cursor;

    /**
     * 每页大小，默认 20
     */
    private int pageSize = 20;

}
