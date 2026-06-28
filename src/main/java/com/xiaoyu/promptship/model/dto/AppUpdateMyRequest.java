package com.xiaoyu.promptship.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户自主更新应用请求
 *
 * @author xiaoyu
 * @since 1.0
 */
@Data
public class AppUpdateMyRequest implements Serializable {

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

}
