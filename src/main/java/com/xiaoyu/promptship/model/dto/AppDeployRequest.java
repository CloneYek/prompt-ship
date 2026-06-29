package com.xiaoyu.promptship.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用部署请求类
 */
@Data
public class AppDeployRequest implements Serializable {

    /**
     * 应用id
     */
    private Long appId;

    private static  final long serialVersionUID = 1L;
}
