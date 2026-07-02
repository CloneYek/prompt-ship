package com.xiaoyu.promptship.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 继续对话请求（基于已有应用）
 *
 * @author xiaoyu
 * @since 1.0
 */
@Data
public class AppChatContinueRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 已有应用的 id
     */
    @NotNull(message = "应用 id 不能为空")
    private Long appId;

    /**
     * 用户新消息
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;

}
