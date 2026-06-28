package com.xiaoyu.promptship.model.dto;

import com.xiaoyu.promptship.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 应用查询请求
 *
 * @author xiaoyu
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用名称（模糊查询）
     */
    private String appName;

    /**
     * 创建用户 ID
     */
    private Long userId;

    /**
     * 代码生成类型
     */
    private String codeGenType;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 部署标识
     */
    private String deployKey;

}
