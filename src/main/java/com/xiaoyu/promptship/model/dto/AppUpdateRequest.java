package com.xiaoyu.promptship.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 应用更新请求（管理员用）
 *
 * @author xiaoyu
 * @since 1.0
 */
@Data
public class AppUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用 id
     */
    @NotNull(message = "应用 id 不能为空")
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 优先级
     */
    private Integer priority;

}
