package com.xiaoyu.promptship.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 应用创建请求
 *
 * @author xiaoyu
 * @since 1.0
 */
@Data
public class AppCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用名称
     */
    @NotBlank(message = "应用名称不能为空")
    @Size(max = 256, message = "应用名称长度不能超过 256 位")
    private String appName;

    /**
     * 应用初始化的 prompt
     */
    @NotBlank(message = "初始化 prompt 不能为空")
    private String initPrompt;

}
